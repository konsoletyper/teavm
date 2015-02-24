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

import org.teavm.dom.browser.TimerHandler;
import org.teavm.javascript.spi.Async;
import org.teavm.javascript.spi.Rename;
import org.teavm.javascript.spi.Superclass;
import org.teavm.javascript.spi.Sync;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformQueue;
import org.teavm.platform.PlatformRunnable;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
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

    static void monitorEnter(TObject o) {
        monitorEnter(o, 1);
    }

    @Async
    static native void monitorEnter(TObject o, int count);

    static void monitorEnter(final TObject o, final int count, final AsyncCallback<Void> callback) {
        if (o.monitor == null) {
            o.monitor = new Monitor();
        }
        if (o.monitor.owner == null) {
            o.monitor.owner = TThread.currentThread();
        }
        if (o.monitor.owner != TThread.currentThread()) {
            final TThread thread = TThread.currentThread();
            o.monitor.enteringThreads.add(new PlatformRunnable() {
                @Override public void run() {
                    TThread.setCurrentThread(thread);
                    o.monitor.owner = thread;
                    o.monitor.count += count;
                    callback.complete(null);
                }
            });
        } else {
            o.monitor.count += count;
            callback.complete(null);
        }
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
        Platform.startThread(new PlatformRunnable() {
            @Override public void run() {
                if (o.isEmptyMonitor() || o.monitor.owner != null) {
                    return;
                }
                if (!o.monitor.enteringThreads.isEmpty()) {
                    o.monitor.enteringThreads.remove().run();
                }
            }
        });
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
        TThread thread = TThread.currentThread();
        PlatformQueue<NotifyListener> listeners = monitor.notifyListeners;
        while (!listeners.isEmpty()) {
            NotifyListener listener = listeners.remove();
            if (!listener.expired()) {
                Platform.startThread(listener);
                break;
            }
        }
        TThread.setCurrentThread(thread);
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
                Platform.startThread(listener);
            }
        }
    }

    @Rename("wait")
    public final void wait0(long timeout) throws TInterruptedException{
        try {
            wait(timeout, 0);
        } catch (InterruptedException ex) {
            throw new TInterruptedException();
        }
    }

    @Async
    @Rename("wait")
    private native final void wait0(long timeout, int nanos) throws TInterruptedException;

    @Rename("wait")
    public final void wait0(long timeout, int nanos, final AsyncCallback<Void> callback) {
        if (!holdsLock(this)) {
            throw new TIllegalMonitorStateException();
        }
        final NotifyListenerImpl listener = new NotifyListenerImpl(this, callback, monitor.count);
        monitor.notifyListeners.add(listener);
        if (timeout > 0 || nanos > 0) {
            listener.timerId = Platform.schedule(listener, timeout >= Integer.MAX_VALUE ? Integer.MAX_VALUE :
                    (int)timeout);
        }
        monitorExit(this, monitor.count);
    }

    private static class NotifyListenerImpl implements NotifyListener, TimerHandler, PlatformRunnable {
        final TObject obj;
        final AsyncCallback<Void> callback;
        final TThread currentThread = TThread.currentThread();
        int timerId = -1;
        boolean expired;
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
                Platform.startThread(this);
            }
        }

        @Override
        public void run() {
            if (timerId >= 0) {
                Platform.killSchedule(timerId);
                timerId = -1;
            }
            TThread.setCurrentThread(currentThread);
            monitorEnter(obj, lockCount, callback);
        }
    }

    @Rename("wait")
    public final void wait0() throws TInterruptedException {
        try {
            wait(0l);
        } catch (InterruptedException ex) {
            throw new TInterruptedException();
        }
    }

    @Override
    protected void finalize() throws TThrowable {
    }

    public static TObject wrap(Object obj) {
        return (TObject)obj;
    }
}
