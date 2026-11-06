/*
 *  Copyright 2026 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.util.concurrent.locks;

import java.util.Map;
import java.util.WeakHashMap;
import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.lang.TThreadInterruptHandler;
import org.teavm.classlib.java.lang.TThreadSupport;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.interop.Platforms;
import org.teavm.interop.UnsupportedOn;
import org.teavm.runtime.EventQueue;

@UnsupportedOn(Platforms.WEBASSEMBLY_GC)
public final class TLockSupport {
    private static final Map<TThread, ParkState> states = new WeakHashMap<>();

    private TLockSupport() {
    }

    @Async
    public static native void park();

    private static void park(AsyncCallback<Void> callback) {
        park(null, 0, true, callback);
    }

    @Async
    public static native void park(Object blocker);

    private static void park(Object blocker, AsyncCallback<Void> callback) {
        park(blocker, 0, true, callback);
    }

    @Async
    public static native void parkNanos(long nanos);

    private static void parkNanos(long nanos, AsyncCallback<Void> callback) {
        park(null, nanos, false, callback);
    }

    @Async
    public static native void parkNanos(Object blocker, long nanos);

    private static void parkNanos(Object blocker, long nanos, AsyncCallback<Void> callback) {
        park(blocker, nanos, false, callback);
    }

    public static void unpark(TThread thread) {
        if (thread == null) {
            return;
        }
        ParkState state = stateFor(thread);
        ParkHandler handler = null;
        synchronized (state) {
            handler = state.handler;
            if (handler != null) {
                state.permit = false;
                state.resumePending = true;
            } else if (!state.resumePending) {
                state.permit = true;
            }
        }
        if (handler != null) {
            handler.complete();
        }
    }

    public static Object getBlocker(TThread thread) {
        if (thread == null) {
            throw new NullPointerException("thread");
        }
        ParkState state = stateFor(thread);
        synchronized (state) {
            return state.handler == null ? null : state.handler.blocker;
        }
    }

    private static void park(Object blocker, long nanos, boolean indefinite, AsyncCallback<Void> callback) {
        TThread thread = TThread.currentThread();
        ParkState state = stateFor(thread);
        ParkHandler handler = null;
        boolean resumeImmediately = false;
        synchronized (state) {
            if (thread.isInterrupted() || (!indefinite && nanos <= 0)) {
                resumeImmediately = true;
            } else if (state.permit) {
                state.permit = false;
                resumeImmediately = true;
                state.handler = null;
            } else {
                handler = new ParkHandler(thread, state, blocker, callback, indefinite ? 0 : nanos);
                state.handler = handler;
                thread.interruptHandler = handler;
            }
        }
        if (resumeImmediately) {
            callback.complete(null);
            return;
        }
        if (!indefinite && handler != null) {
            handler.schedule(nanos);
        }
    }

    private static ParkState stateFor(TThread thread) {
        synchronized (states) {
            return states.computeIfAbsent(thread, ignored -> new ParkState());
        }
    }

    private static final class ParkState {
        boolean permit;
        boolean resumePending;
        ParkHandler handler;
    }

    private static final class ParkHandler implements EventQueue.Event, TThreadInterruptHandler {
        private final TThread thread;
        private final ParkState state;
        private final Object blocker;
        private final AsyncCallback<Void> callback;
        private final long startNanos;
        private final long timeoutNanos;
        private int scheduleId = -1;

        ParkHandler(TThread thread, ParkState state, Object blocker, AsyncCallback<Void> callback, long nanos) {
            this.thread = thread;
            this.state = state;
            this.blocker = blocker;
            this.callback = callback;
            startNanos = System.nanoTime();
            timeoutNanos = nanos;
        }

        void schedule(long nanos) {
            long millis = nanos / 1_000_000L + (nanos % 1_000_000L == 0 ? 0 : 1);
            int timeout = millis >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
            int id = EventQueue.offer(this, System.currentTimeMillis() + timeout);
            synchronized (state) {
                if (state.handler == this) {
                    scheduleId = id;
                } else {
                    cancelSchedule(id);
                }
            }
        }

        @Override
        public void run() {
            long remaining = timeoutNanos - (System.nanoTime() - startNanos);
            if (remaining > 0) {
                schedule(remaining);
                return;
            }
            complete();
        }

        @Override
        public void interrupted() {
            complete();
        }

        void complete() {
            boolean resume;
            synchronized (state) {
                resume = state.handler == this;
                if (resume) {
                    state.handler = null;
                    if (thread.interruptHandler == this) {
                        thread.interruptHandler = null;
                    }
                }
            }
            if (!resume) {
                return;
            }
            if (scheduleId >= 0) {
                cancelSchedule(scheduleId);
                scheduleId = -1;
            }
            EventQueue.offer(() -> {
                synchronized (state) {
                    state.resumePending = false;
                }
                TThread previousThread = TThread.currentThread();
                try {
                    TThreadSupport.setCurrentThread(thread);
                    callback.complete(null);
                } finally {
                    TThreadSupport.setCurrentThread(previousThread);
                }
            });
        }

        private void cancelSchedule(int id) {
            EventQueue.kill(id);
        }
    }
}
