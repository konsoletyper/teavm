/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import org.teavm.interop.Async;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.platform.async.AsyncCallback;

public class TThread extends TObject implements TRunnable {
    private static TThread mainThread = new TThread(TString.wrap("main"));
    private static TThread currentThread = mainThread;
    private static long nextId = 1;
    private static int activeCount = 1;
    private long id;
    private int priority;
    private long timeSliceStart;
    private int yieldCount;
    private final Object finishedLock = new Object();
    private boolean interruptedFlag;
    private TThreadInterruptHandler interruptHandler;

    private TString name;
    private boolean alive = true;
    TRunnable target;

    public TThread() {
        this(null, null);
    }

    public TThread(TString name) {
        this(null, name);
    }

    public TThread(TRunnable target) {
        this(target, null);
    }

    public TThread(TRunnable target, TString name) {
        this.name = name;
        this.target = target;
        id = nextId++;
    }

    public void start() {
        Platform.startThread(() -> {
            try {
                activeCount++;
                setCurrentThread(TThread.this);
                TThread.this.run();
            } finally {
                synchronized (finishedLock) {
                    finishedLock.notifyAll();
                }
                alive = false;
                activeCount--;
                setCurrentThread(mainThread);
            }
        });
    }

    static void setCurrentThread(TThread thread) {
        if (currentThread != thread) {
            currentThread = thread;
        }
        currentThread.timeSliceStart = System.currentTimeMillis();
    }

    static TThread getMainThread() {
        return mainThread;
    }

    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    public static TThread currentThread() {
        return currentThread;
    }

    public TString getName() {
        return name;
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        if (currentThread() == this) {
            return;
        }
        synchronized (finishedLock) {
            finishedLock.wait(millis, nanos);
        }
    }

    public final void join(long millis) throws InterruptedException {
        join(millis, 0);
    }

    public final void join() throws InterruptedException {
        join(0);
    }

    public static void yield() {
        TThread currentThread = currentThread();
        if (++currentThread.yieldCount < 30) {
            return;
        }
        currentThread().yieldCount = 0;
        if (currentThread.timeSliceStart + 100 < System.currentTimeMillis()) {
            switchContext(currentThread);
        }
    }

    @Async
    static native void switchContext(TThread thread);

    private static void switchContext(final TThread thread, final AsyncCallback<Void> callback) {
        Platform.postpone(() -> {
            setCurrentThread(thread);
            callback.complete(null);
        });
    }

    public void interrupt() {
        interruptedFlag = true;
        if (interruptHandler != null) {
            interruptHandler.interrupted();
            interruptHandler = null;
        }
    }

    public static boolean interrupted() {
        TThread thread = currentThread();
        boolean result = thread.interruptedFlag;
        thread.interruptedFlag = false;
        return result;
    }

    public boolean isInterrupted() {
        return interruptedFlag;
    }

    public boolean isAlive() {
        return alive;
    }

    public static int activeCount() {
        return activeCount;
    }

    public long getId() {
        return id;
    }

    public static boolean holdsLock(TObject obj) {
        return TObject.holdsLock(obj);
    }

    @Async
    public static native void sleep(long millis) throws TInterruptedException;

    private static void sleep(long millis, final AsyncCallback<Void> callback) {
        final TThread current = currentThread();
        int intMillis = millis < Integer.MAX_VALUE ? (int) millis : Integer.MAX_VALUE;
        SleepHandler handler = new SleepHandler(current, callback);
        handler.scheduleId = Platform.schedule(handler, intMillis);
        current.interruptHandler = handler;
    }

    private static class SleepHandler implements PlatformRunnable, TThreadInterruptHandler {
        private TThread thread;
        private AsyncCallback<Void> callback;
        private boolean isInterrupted;
        int scheduleId;

        public SleepHandler(TThread thread, AsyncCallback<Void> callback) {
            this.thread = thread;
            this.callback = callback;
        }

        @Override
        public void interrupted() {
            thread.interruptedFlag = false;
            isInterrupted = true;
            Platform.killSchedule(scheduleId);
            Platform.postpone(() -> callback.error(new TInterruptedException()));
        }

        @Override
        public void run() {
            if (!isInterrupted) {
                thread.interruptHandler = null;
                setCurrentThread(thread);
                callback.complete(null);
            }
        }
    }

    public final void setPriority(int newPriority) {
        this.priority = newPriority;
    }

    public final int getPriority() {
        return this.priority;
    }

    public TStackTraceElement[] getStackTrace() {
        return new TStackTraceElement[0];
    }
}
