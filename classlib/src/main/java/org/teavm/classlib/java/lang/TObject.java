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

import org.teavm.interop.Address;
import org.teavm.interop.Async;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Rename;
import org.teavm.interop.Structure;
import org.teavm.interop.Superclass;
import org.teavm.interop.Sync;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformObject;
import org.teavm.platform.PlatformQueue;
import org.teavm.platform.PlatformRunnable;
import org.teavm.platform.async.AsyncCallback;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeJavaObject;
import org.teavm.runtime.RuntimeObject;

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
        } else if (o.monitor.owner != TThread.currentThread()) {
            throw new IllegalStateException("Can't enter monitor from another thread synchronously");
        }
        o.monitor.count++;
    }

    static void monitorExitSync(TObject o) {
        if (o.isEmptyMonitor() || o.monitor.owner != TThread.currentThread()) {
            throw new TIllegalMonitorStateException();
        }
        if (--o.monitor.count == 0) {
            o.monitor.owner = null;
        }
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
        o.monitor.enteringThreads.add(() -> {
            TThread.setCurrentThread(thread);
            o.monitor.owner = thread;
            o.monitor.count += count;
            callback.complete(null);
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
            Platform.postpone(() -> {
                if (o.isEmptyMonitor() || o.monitor.owner != null) {
                    return;
                }
                if (!o.monitor.enteringThreads.isEmpty()) {
                    o.monitor.enteringThreads.remove().run();
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
    }

    @Rename("getClass")
    public final TClass<?> getClass0() {
        return TClass.getClass(Platform.getPlatformObject(this).getPlatformClass());
    }

    @Override
    public int hashCode() {
        return identity();
    }

    @Override
    public boolean equals(Object obj) {
        return equals0((TObject) obj);
    }

    @Rename("equals")
    public boolean equals0(TObject other) {
        return this == other;
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + TInteger.toHexString(identity());
    }

    @DelegateTo("identityLowLevel")
    int identity() {
        PlatformObject platformThis = Platform.getPlatformObject(this);
        if (platformThis.getId() == 0) {
            platformThis.setId(Platform.nextObjectId());
        }
        return Platform.getPlatformObject(this).getId();
    }

    @SuppressWarnings("unused")
    private static int identityLowLevel(RuntimeJavaObject object) {
        if ((object.classReference & RuntimeObject.MONITOR_EXISTS) != 0) {
            object = (RuntimeJavaObject) object.monitor;
        }
        int result = object.monitor.toAddress().toInt();
        if (result == 0) {
            result = RuntimeJavaObject.nextId++;
            if (result == 0) {
                result = RuntimeJavaObject.nextId++;
            }
            object.monitor = Address.fromInt(result).toStructure();
        }
        return result;
    }

    @Override
    @DelegateTo("cloneLowLevel")
    protected Object clone() throws TCloneNotSupportedException {
        if (!(this instanceof TCloneable) && Platform.getPlatformObject(this)
                .getPlatformClass().getMetadata().getArrayItem() == null) {
            throw new TCloneNotSupportedException();
        }
        Object result = Platform.clone(this);
        Platform.getPlatformObject(result).setId(Platform.nextObjectId());
        return result;
    }

    @SuppressWarnings("unused")
    private static RuntimeJavaObject cloneLowLevel(RuntimeJavaObject self) {
        RuntimeClass cls = RuntimeClass.getClass(self);
        int skip = Structure.sizeOf(RuntimeJavaObject.class);
        int size;
        RuntimeJavaObject copy;
        if (cls.itemType == null) {
            copy = Allocator.allocate(cls).toStructure();
            size = cls.size;
        } else {
            RuntimeArray array = (RuntimeArray) self;
            copy = Allocator.allocateArray(cls, array.size).toStructure();
            int itemSize = (cls.itemType.flags & RuntimeClass.PRIMITIVE) == 0 ? 4 : cls.itemType.size;
            Address headerSize = Address.align(Address.fromInt(Structure.sizeOf(RuntimeArray.class)), itemSize);
            size = itemSize * array.size + headerSize.toInt();
        }
        if (size > skip) {
            Allocator.moveMemoryBlock(self.toAddress().add(skip), copy.toAddress().add(skip), size - skip);
        }
        return copy;
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
            Platform.postpone(() -> {
                if (!expired()) {
                    run();
                }
            });
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
            Platform.postpone(() -> callback.error(new TInterruptedException()));
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
