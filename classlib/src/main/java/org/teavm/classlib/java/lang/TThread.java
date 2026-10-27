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

import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.Fiber;

public class TThread extends TObject implements TRunnable {
    private static TThread mainThread = new TThread("main", true);
    private static TThread currentThread = mainThread;
    private static int nextId = 1;
    private static int activeCount = 1;
    private static UncaughtExceptionHandler defaultUncaughtExceptionHandler = new TDefaultUncaughtExceptionHandler();
    private UncaughtExceptionHandler uncaughtExceptionHandler;
    private long id;
    private int priority;
    private boolean daemon;
    private long timeSliceStart;
    private int yieldCount;
    private final Object finishedLock = new Object();
    private boolean interruptedFlag;
    public TThreadInterruptHandler interruptHandler;
    public Object key;

    private String name;
    // As in the JDK, isAlive() is false before start(). started and alive give the NEW, RUNNABLE and TERMINATED
    // states.
    private boolean alive;
    /**
     * The InheritableThreadLocal values this thread takes from its creator, as pairs (instance, value), or null.
     * They are read when the thread is created, as in the JDK, and set when it starts: a ThreadLocal keys its
     * values by {@link #key}, which exists only then.
     */
    private Object[] inherited;
    private boolean started;
    // The context class loader once set (null is a valid value); until then, the system loader. A new thread
    // copies both from the thread that creates it, as in the JDK.
    private TClassLoader contextClassLoader;
    private boolean contextClassLoaderSet;
    TRunnable target;

    public TThread() {
        this(null, null);
    }

    /**
     * The main thread's constructor. The main thread runs from the start, and it has no creator, so it copies
     * nothing. It must not reach
     * the public constructor's inheritance snapshot: Thread's class initializer creates the main thread, and
     * currentThread() runs that initializer, so code reachable from here would make currentThread() suspendable.
     */
    private TThread(String name, @SuppressWarnings("unused") boolean mainThread) {
        this.name = name;
        id = nextId++;
        alive = true;
        started = true;
    }

    public TThread(String name) {
        this(null, name);
    }

    public TThread(TRunnable target) {
        this(target, null);
    }

    public TThread(TRunnable target, String name) {
        this.name = name;
        this.target = target;
        id = nextId++;
        // As in the JDK, a new thread takes its creator's context class loader and inheritable thread locals.
        TThread creator = currentThread;
        if (creator != null) {
            contextClassLoader = creator.contextClassLoader;
            contextClassLoaderSet = creator.contextClassLoaderSet;
            inherited = TInheritableThreadLocal.snapshot();
        }
    }

    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED
    }

    public State getState() {
        if (!started) {
            return State.NEW;
        }
        return alive ? State.RUNNABLE : State.TERMINATED;
    }

    public void setContextClassLoader(TClassLoader loader) {
        contextClassLoader = loader;
        contextClassLoaderSet = true;
    }

    public static void onSpinWait() {
        // A spin loop waits for another thread; with green threads that thread runs only if this one yields.
        TThread current = currentThread();
        current.yieldCount = 0;
        switchContext(current);
    }

    public void start() {
        started = true;
        alive = true;
        if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
            boolean daemon = this.daemon;
            if (!daemon) {
                Fiber.userThreadCount++;
            }
            EventQueue.offer(() -> Fiber.start(this::runThread, daemon));
        } else {
            Platform.startThread(this::runThread);
        }
    }

    private void runThread() {
        try {
            activeCount++;
            setCurrentThread(TThread.this);
            key = new Object();
            applyInherited();
            TThread.this.run();
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        } finally {
            // Clear alive under finishedLock, before the notify, so join() sees both together.
            synchronized (finishedLock) {
                alive = false;
                finishedLock.notifyAll();
            }
            activeCount--;
            key = null;
            setCurrentThread(mainThread);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyInherited() {
        Object[] pairs = inherited;
        inherited = null;
        if (pairs != null) {
            for (int i = 0; i < pairs.length && pairs[i] != null; i += 2) {
                ((TThreadLocal<Object>) pairs[i]).set(pairs[i + 1]);
            }
        }
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public final boolean isDaemon() {
        return daemon;
    }

    public final void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        if (millis < 0 || nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException();
        }
        // A thread that was never started has nothing to wait for.
        if (currentThread() == this || !started) {
            return;
        }
        // Check and wait under the lock, in a loop. monitorEnter can switch threads, so a check outside the lock can
        // miss the thread's end, and a wake with time left can come while the thread is still alive. started never
        // goes back to false, so the early return above cannot race with the thread's end.
        synchronized (finishedLock) {
            // A timeout over about 292 years (Long.MAX_VALUE ns) waits without a deadline, because
            // millis * 1_000_000 overflows for it.
            if (millis == 0 && nanos == 0 || millis >= (Long.MAX_VALUE - nanos) / 1_000_000L) {
                while (alive) {
                    finishedLock.wait();
                }
            } else {
                long deadline = System.nanoTime() + millis * 1_000_000L + nanos;
                while (alive) {
                    long left = deadline - System.nanoTime();
                    if (left <= 0) {
                        break;
                    }
                    finishedLock.wait(left / 1_000_000L, (int) (left % 1_000_000L));
                }
            }
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
        if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
            EventQueue.offer(() -> {
                setCurrentThread(thread);
                callback.complete(null);
            });
        } else {
            Platform.postpone(() -> {
                setCurrentThread(thread);
                callback.complete(null);
            });
        }
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

    // The JDK's sleep(millis, nanos) and sleep(Duration). The clock has millisecond resolution, so a part of a
    // millisecond rounds up, as in the JDK before 21.
    public static void sleep(long millis, int nanos) throws TInterruptedException {
        if (millis < 0 || nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException();
        }
        sleep(nanos > 0 && millis < Long.MAX_VALUE ? millis + 1 : millis);
    }

    public static void sleep(java.time.Duration duration) throws TInterruptedException {
        // As in the JDK, a negative duration returns at once, and one too long for a long of milliseconds
        // saturates instead of throwing.
        long seconds = duration.getSeconds();
        if (seconds < 0) {
            return;
        }
        int nanos = duration.getNano();
        long millis = seconds >= Long.MAX_VALUE / 1000 - 1 ? Long.MAX_VALUE : seconds * 1000 + nanos / 1_000_000;
        sleep(millis, nanos % 1_000_000);
    }

    private static void sleep(long millis, AsyncCallback<Void> callback) {
        TThread current = currentThread();
        SleepHandler handler = new SleepHandler(current, callback);
        // A thread interrupted before it sleeps throws at once, on every backend.
        if (current.interruptedFlag) {
            handler.interrupted();
            return;
        }
        // Host timers take an int delay (a larger one fires at once), so a longer sleep wakes after
        // Integer.MAX_VALUE ms, about 24.8 days. This also keeps the deadline below from overflowing.
        int intMillis = millis < Integer.MAX_VALUE ? (int) millis : Integer.MAX_VALUE;
        if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
            handler.scheduleId = EventQueue.offer(handler, System.currentTimeMillis() + intMillis);
        } else {
            handler.scheduleId = Platform.schedule(handler, intMillis);
        }
        current.interruptHandler = handler;
    }

    static class SleepHandler implements PlatformRunnable, EventQueue.Event, TThreadInterruptHandler {
        private TThread thread;
        private AsyncCallback<Void> callback;
        private boolean isInterrupted;
        // -1 until the timer is scheduled. An interrupt before that must not kill another event: on C, event ids
        // start at 0.
        int scheduleId = -1;

        SleepHandler(TThread thread, AsyncCallback<Void> callback) {
            this.thread = thread;
            this.callback = callback;
        }

        @Override
        public void interrupted() {
            thread.interruptedFlag = false;
            isInterrupted = true;
            if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                if (scheduleId >= 0) {
                    EventQueue.kill(scheduleId);
                }
                EventQueue.offer(() -> callback.error(new TInterruptedException()));
            } else {
                if (scheduleId >= 0) {
                    Platform.killSchedule(scheduleId);
                }
                Platform.postpone(() -> callback.error(new TInterruptedException()));
            }
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

    public TClassLoader getContextClassLoader() {
        return contextClassLoaderSet ? contextClassLoader : TClassLoader.getSystemClassLoader();
    }
    
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        if (this.uncaughtExceptionHandler != null) {
            return this.uncaughtExceptionHandler;
        }
        return defaultUncaughtExceptionHandler;
    }
    
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }
    
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }
    
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        defaultUncaughtExceptionHandler = handler;
    }
    
    public interface UncaughtExceptionHandler {
        void uncaughtException(TThread t, Throwable e);
    }
}
