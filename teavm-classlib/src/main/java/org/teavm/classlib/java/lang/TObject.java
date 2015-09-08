/*
 *  Copyright 2013 Alexey Andreev.
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

import org.teavm.javascript.spi.Async;
import org.teavm.javascript.spi.Rename;
import org.teavm.javascript.spi.Superclass;
import org.teavm.javascript.spi.Sync;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformQueue;
import org.teavm.platform.PlatformRunnable;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
@Superclass("")
public class TObject {
    Monitor monitor;

    static class Monitor {
        PlatformQueue<PlatformRunnable> enteringThreads;
        PlatformQueue<NotifyListener> notifyListeners;
        TThread owner;
        int count;

        public Monitor() {
            this.owner = TThread.currentThread();
            enteringThreads = Platform.createQueue();
            notifyListeners = Platform.createQueue();
        }
    }

    interface NotifyListener extends PlatformRunnable {
        boolean expired();
    }

    static void monitorEnterSync(TObject o) {
        if (o.monitor == null) {
            o.monitor = new Monitor();
        }
        if (o.monitor.owner == null) {
            o.monitor.owner = TThread.currentThread();
        }
        o.monitor.count++;
    }

    static void monitorExitSync(TObject o) {
        if (o.isEmptyMonitor() || o.monitor.owner != TThread.currentThread()) {
            throw new TIllegalMonitorStateException();
        }
        --o.monitor.count;
        o.isEmptyMonitor();
    }

    static void monitorEnter(TObject o) {
        monitorEnter(o, 1);
    }

    static void monitorEnter(TObject o, int count) {
        if (o.monitor == null) {
            o.monitor = new Monitor();
        }
        if (o.monitor.owner == null) {
            o.monitor.owner = TThread.currentThread();
        }
        if (o.monitor.owner != TThread.currentThread()) {
            monitorEnterWait(o, count);
        } else {
            o.monitor.count += count;
        }
    }

    @Async
    static native void monitorEnterWait(TObject o, int count);

    static void monitorEnterWait(final TObject o, final int count, final AsyncCallback<Void> callback) {
        final TThread thread = TThread.currentThread();
        if (o.monitor == null) {
            o.monitor = new Monitor();
            TThread.setCurrentThread(thread);
            o.monitor.count += count;
            callback.complete(null);
            return;
        } else if (o.monitor.owner == null) {
            o.monitor.owner = thread;
            TThread.setCurrentThread(thread);
            o.monitor.count += count;
            callback.complete(null);
            return;
        }
        o.monitor.enteringThreads.add(new PlatformRunnable() {
            @Override public void run() {
                TThread.setCurrentThread(thread);
                o.monitor.owner = thread;
                o.monitor.count += count;
                callback.complete(null);
            }
        });
    }

    @Sync
    static void monitorExit(final TObject o) {
        monitorExit(o, 1);
    }

    @Sync
    static void monitorExit(final TObject o, int count) {
        if (o.isEmptyMonitor() || o.monitor.owner != TThread.currentThread()) {
            throw new TIllegalMonitorStateException();
        }
        o.monitor.count -= count;
        if (o.monitor.count > 0) {
            return;
        }

        o.monitor.owner = null;
        if (!o.monitor.enteringThreads.isEmpty()) {
            Platform.postpone(new PlatformRunnable() {
                @Override public void run() {
                    if (o.isEmptyMonitor() || o.monitor.owner != null) {
                        return;
                    }
                    if (!o.monitor.enteringThreads.isEmpty()) {
                        o.monitor.enteringThreads.remove().run();
                    }
                }
            });
        } else {
            o.isEmptyMonitor();
        }
    }

    boolean isEmptyMonitor() {
        if (monitor == null) {
            return true;
        }
        if (monitor.owner == null && monitor.enteringThreads.isEmpty() && monitor.notifyListeners.isEmpty()) {
            monitor = null;
            return true;
        } else {
            return false;
        }
    }

    static boolean holdsLock(TObject o) {
        return o.monitor != null && o.monitor.owner == TThread.currentThread();
    }

    @Rename("fakeInit")
    public TObject() {
    }

    @Rename("<init>")
    private void init() {
        Platform.getPlatformObject(this).setId(Platform.nextObjectId());
    }

    @Rename("getClass")
    public final TClass<?> getClass0() {
        return TClass.getClass(Platform.getPlatformObject(this).getPlatformClass());
    }

    @Override
    public int hashCode() {
        return identity();
    }

    @Rename("equals")
    public boolean equals0(TObject other) {
        return this == other;
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + TInteger.toHexString(identity());
    }

    int identity() {
        return Platform.getPlatformObject(this).getId();
    }

    @Override
    protected Object clone() throws TCloneNotSupportedException {
        if (!(this instanceof TCloneable) && Platform.getPlatformObject(this)
                .getPlatformClass().getMetadata().getArrayItem() == null) {
            throw new TCloneNotSupportedException();
        }
        Object result = Platform.clone(this);
        Platform.getPlatformObject(result).setId(Platform.nextObjectId());
        return result;
    }

    @Sync
    @Rename("notify")
    public final void notify0() {
        if (!holdsLock(this)) {
            throw new TIllegalMonitorStateException();
        }
        PlatformQueue<NotifyListener> listeners = monitor.notifyListeners;
        while (!listeners.isEmpty()) {
            NotifyListener listener = listeners.remove();
            if (!listener.expired()) {
                Platform.postpone(listener);
                break;
            }
        }
    }

    @Sync
    @Rename("notifyAll")
    public final void notifyAll0() {
        if (!holdsLock(this)) {
            throw new TIllegalMonitorStateException();
        }
        PlatformQueue<NotifyListener> listeners = monitor.notifyListeners;
        while (!listeners.isEmpty()) {
            NotifyListener listener = listeners.remove();
            if (!listener.expired()) {
                Platform.postpone(listener);
            }
        }
    }

    @Rename("wait")
    public final void wait0(long timeout) throws TInterruptedException {
        try {
            wait(timeout, 0);
        } catch (InterruptedException ex) {
            throw new TInterruptedException();
        }
    }

    @Rename("wait")
    private void wait0(long timeout, int nanos) throws TInterruptedException {
        if (!holdsLock(this)) {
            throw new TIllegalMonitorStateException();
        }
        waitImpl(timeout, nanos);
    }

    @Async
    private native void waitImpl(long timeout, int nanos) throws TInterruptedException;

    public final void waitImpl(long timeout, int nanos, final AsyncCallback<Void> callback) {
        final NotifyListenerImpl listener = new NotifyListenerImpl(this, callback, monitor.count);
        monitor.notifyListeners.add(listener);
        if (timeout > 0 || nanos > 0) {
            listener.timerId = Platform.schedule(listener, timeout >= Integer.MAX_VALUE ? Integer.MAX_VALUE
                    : (int) timeout);
        }
        monitorExit(this, monitor.count);
    }

    private static class NotifyListenerImpl implements NotifyListener, TimerHandler, PlatformRunnable,
            TThreadInterruptHandler {
        final TObject obj;
        final AsyncCallback<Void> callback;
        final TThread currentThread = TThread.currentThread();
        int timerId = -1;
        boolean expired;
        boolean performed;
        int lockCount;

        public NotifyListenerImpl(TObject obj, AsyncCallback<Void> callback, int lockCount) {
            this.obj = obj;
            this.callback = callback;
            this.lockCount = lockCount;
        }

        @Override
        public boolean expired() {
            boolean result = expired;
            expired = true;
            return result;
        }

        @Override
        public void onTimer() {
            if (!expired()) {
                run();
            }
        }

        @Override
        public void run() {
            if (performed) {
                return;
            }
            performed = true;
            if (timerId >= 0) {
                Platform.killSchedule(timerId);
                timerId = -1;
            }
            TThread.setCurrentThread(currentThread);
            monitorEnterWait(obj, lockCount, callback);
        }

        @Override
        public void interrupted() {
            if (performed) {
                return;
            }
            performed = true;
            if (timerId >= 0) {
                Platform.killSchedule(timerId);
                timerId = -1;
            }
            Platform.postpone(new PlatformRunnable() {
                @Override public void run() {
                    callback.error(new TInterruptedException());
                }
            });
        }
    }

    @Rename("wait")
    public final void wait0() throws TInterruptedException {
        try {
            wait(0L);
        } catch (InterruptedException ex) {
            throw new TInterruptedException();
        }
    }

    @Override
    protected void finalize() throws TThrowable {
    }

    public static TObject wrap(Object obj) {
        return (TObject) obj;
    }
}
