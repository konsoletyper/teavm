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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.Fiber;

public class TThread extends TObject implements TRunnable {
    private static final List<TThread> activeThreads = new ArrayList<>();
    private static TThread mainThread = new TThread("main");
    private static TThread currentThread = mainThread;
    private static int nextId = 1;
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
    private boolean alive;
    private boolean started;
    private TClassLoader contextClassLoader;
    private boolean contextClassLoaderSet;
    TRunnable target;

    static {
        mainThread.alive = true;
        mainThread.started = true;
        activeThreads.add(mainThread);
    }

    public TThread() {
        this(null, null);
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
        if (currentThread != null) {
            contextClassLoader = currentThread.getContextClassLoader();
            contextClassLoaderSet = true;
        }
    }

    public void start() {
        synchronized (this) {
            if (started) {
                throw new TIllegalThreadStateException();
            }
            started = true;
            alive = true;
            synchronized (activeThreads) {
                activeThreads.add(this);
            }
        }
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
            setCurrentThread(TThread.this);
            key = new Object();
            TThread.this.run();
        } catch (Throwable t) {
            getUncaughtExceptionHandler().uncaughtException(this, t);
        } finally {
            synchronized (finishedLock) {
                alive = false;
                synchronized (activeThreads) {
                    activeThreads.remove(this);
                }
                finishedLock.notifyAll();
            }
            key = null;
            setCurrentThread(mainThread);
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
        if (currentThread() == this || !alive) {
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
        EventQueue.offer(() -> {
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
        synchronized (activeThreads) {
            return activeThreads.size();
        }
    }

    public static int enumerate(TThread[] threads) {
        Objects.requireNonNull(threads);
        synchronized (activeThreads) {
            int count = 0;
            for (TThread thread : activeThreads) {
                if (count == threads.length) {
                    break;
                }
                if (thread.alive) {
                    threads[count++] = thread;
                }
            }
            return count;
        }
    }

    public long getId() {
        return id;
    }

    public static boolean holdsLock(TObject obj) {
        return TObject.holdsLock(obj);
    }

    @Async
    public static native void sleep(long millis) throws TInterruptedException;

    private static void sleep(long millis, AsyncCallback<Void> callback) {
        TThread current = currentThread();
        SleepHandler handler = new SleepHandler(current, callback);
        if (current.interruptedFlag) {
            handler.interrupted();
        } else {
            handler.scheduleId = EventQueue.offer(handler, System.currentTimeMillis() + millis);
            current.interruptHandler = handler;
        }
    }

    static class SleepHandler implements EventQueue.Event, TThreadInterruptHandler {
        private TThread thread;
        private AsyncCallback<Void> callback;
        private boolean isInterrupted;
        int scheduleId;

        SleepHandler(TThread thread, AsyncCallback<Void> callback) {
            this.thread = thread;
            this.callback = callback;
        }

        @Override
        public void interrupted() {
            thread.interruptedFlag = false;
            isInterrupted = true;
            EventQueue.kill(scheduleId);
            EventQueue.offer(() -> callback.error(new TInterruptedException()));
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

    public void setContextClassLoader(TClassLoader loader) {
        contextClassLoader = loader;
        contextClassLoaderSet = true;
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
