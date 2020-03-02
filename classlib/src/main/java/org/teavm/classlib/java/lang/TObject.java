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

import org.teavm.classlib.PlatformDetector;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.Address;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Rename;
import org.teavm.interop.Structure;
import org.teavm.interop.Superclass;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformObject;
import org.teavm.platform.PlatformQueue;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

@Superclass("")
public class TObject {
    Monitor monitor;

    static class Monitor {
        static final int MASK = 0x80000000;

        PlatformQueue<PlatformRunnable> enteringThreads;
        PlatformQueue<NotifyListener> notifyListeners;
        TThread owner;
        int count;
        int id;

        Monitor() {
            this.owner = TThread.currentThread();
        }
    }

    interface NotifyListener extends PlatformRunnable, EventQueue.Event {
        boolean expired();
    }

    static void monitorEnterSync(TObject o) {
        if (o.monitor == null) {
            createMonitor(o);
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
           createMonitor(o);
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

    private static void createMonitor(TObject o) {
        if (PlatformDetector.isLowLevel()) {
            int hashCode = hashCodeLowLevel(o);
            o.monitor = new Monitor();
            o.monitor.id = hashCode;
        } else {
            o.monitor = new Monitor();
        }
    }

    @Async
    static native void monitorEnterWait(TObject o, int count);

    static void monitorEnterWait(TObject o, int count, AsyncCallback<Void> callback) {
        TThread thread = TThread.currentThread();
        if (o.monitor == null) {
            createMonitor(o);
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

        Monitor monitor = o.monitor;
        if (monitor.enteringThreads == null) {
            monitor.enteringThreads = Platform.createQueue();
        }
        monitor.enteringThreads.add(() -> {
            TThread.setCurrentThread(thread);
            o.monitor.owner = thread;
            o.monitor.count += count;
            callback.complete(null);
        });
    }

    static void monitorExit(TObject o) {
        monitorExit(o, 1);
    }

    static void monitorExit(TObject o, int count) {
        if (o.isEmptyMonitor() || o.monitor.owner != TThread.currentThread()) {
            throw new TIllegalMonitorStateException();
        }

        Monitor monitor = o.monitor;
        monitor.count -= count;
        if (monitor.count > 0) {
            return;
        }

        monitor.owner = null;
        if (monitor.enteringThreads != null && !monitor.enteringThreads.isEmpty()) {
            if (PlatformDetector.isLowLevel()) {
                EventQueue.offer(() -> waitForOtherThreads(o));
            } else {
                Platform.postpone(() -> waitForOtherThreads(o));
            }
        } else {
            o.isEmptyMonitor();
        }
    }

    private static void waitForOtherThreads(TObject o) {
        if (o.isEmptyMonitor() || o.monitor.owner != null) {
            return;
        }
        Monitor monitor = o.monitor;
        if (monitor.enteringThreads != null && !monitor.enteringThreads.isEmpty()) {
            PlatformQueue<PlatformRunnable> enteringThreads = monitor.enteringThreads;
            PlatformRunnable r = enteringThreads.remove();
            monitor.enteringThreads = null;
            r.run();
        }
    }

    final boolean isEmptyMonitor() {
        Monitor monitor = this.monitor;
        if (monitor == null) {
            return true;
        }
        if (monitor.owner == null
                && (monitor.enteringThreads == null || monitor.enteringThreads.isEmpty())
                && (monitor.notifyListeners == null || monitor.notifyListeners.isEmpty())) {
            deleteMonitor();
            return true;
        } else {
            return false;
        }
    }

    private void deleteMonitor() {
        if (PlatformDetector.isLowLevel()) {
            int id = monitor.id;
            setHashCodeLowLevel(this, id);
        } else {
            monitor = null;
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

    private String obfuscatedToString() {
        return "<java_object>@" + Integer.toHexString(identity());
    }

    final int identity() {
        if (PlatformDetector.isLowLevel()) {
            Monitor monitor = this.monitor;
            if (monitor == null) {
                int hashCode = hashCodeLowLevel(this);
                if (hashCode == 0) {
                    hashCode = identityLowLevel();
                    setHashCodeLowLevel(this, hashCode);
                }
                return hashCode;
            } else {
                int hashCode = monitor.id;
                if (hashCode == 0) {
                    hashCode = identityLowLevel();
                    monitor.id = hashCode;
                }
                return hashCode;
            }
        }
        PlatformObject platformThis = Platform.getPlatformObject(this);
        if (platformThis.getId() == 0) {
            platformThis.setId(Platform.nextObjectId());
        }
        return Platform.getPlatformObject(this).getId();
    }

    @DelegateTo("hashCodeLowLevelImpl")
    @NoSideEffects
    private static native int hashCodeLowLevel(TObject obj);

    @Unmanaged
    private static int hashCodeLowLevelImpl(RuntimeObject obj) {
        return obj.hashCode;
    }

    @DelegateTo("setHashCodeLowLevelImpl")
    @NoSideEffects
    private static native void setHashCodeLowLevel(TObject obj, int value);

    @Unmanaged
    private static void setHashCodeLowLevelImpl(RuntimeObject obj, int value) {
        obj.hashCode = value;
    }

    @Unmanaged
    private static int identityLowLevel() {
        int result = RuntimeObject.nextId++;
        if (result == 0) {
            result = RuntimeObject.nextId++;
            if (result == Monitor.MASK) {
                result = 1;
            }
        }
        return result;
    }

    @DelegateTo("identityOrMonitorLowLevel")
    @NoSideEffects
    private native int identityOrMonitor();

    private static int identityOrMonitorLowLevel(RuntimeObject object) {
        return object.hashCode;
    }

    @DelegateTo("setIdentityLowLevel")
    @NoSideEffects
    native void setIdentity(int id);

    private static void setIdentityLowLevel(RuntimeObject object, int id) {
        object.hashCode = id;
    }

    @Override
    @DelegateTo("cloneLowLevel")
    @PluggableDependency(ObjectDependencyPlugin.class)
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
    private static RuntimeObject cloneLowLevel(RuntimeObject self) {
        RuntimeClass cls = RuntimeClass.getClass(self);
        int skip = Structure.sizeOf(RuntimeObject.class);
        int size;
        RuntimeObject copy;
        if (cls.itemType == null) {
            copy = Allocator.allocate(cls).toStructure();
            size = cls.size;
        } else {
            RuntimeArray array = (RuntimeArray) self;
            copy = Allocator.allocateArray(cls, array.size).toStructure();
            int itemSize = (cls.itemType.flags & RuntimeClass.PRIMITIVE) == 0 ? Address.sizeOf() : cls.itemType.size;
            Address headerSize = Address.align(Address.fromInt(Structure.sizeOf(RuntimeArray.class)), itemSize);
            size = itemSize * array.size + headerSize.toInt();
        }
        if (size > skip) {
            Allocator.moveMemoryBlock(self.toAddress().add(skip), copy.toAddress().add(skip), size - skip);
        }
        return copy;
    }

    @Rename("notify")
    public final void notify0() {
        if (!holdsLock(this)) {
            throw new TIllegalMonitorStateException();
        }
        PlatformQueue<NotifyListener> listeners = monitor.notifyListeners;
        if (listeners == null) {
            return;
        }
        while (!listeners.isEmpty()) {
            NotifyListener listener = listeners.remove();
            if (!listener.expired()) {
                if (PlatformDetector.isLowLevel()) {
                    EventQueue.offer(listener);
                } else {
                    Platform.postpone(listener);
                }
                break;
            }
        }
        if (listeners.isEmpty()) {
            monitor.notifyListeners = null;
        }
    }

    @Rename("notifyAll")
    public final void notifyAll0() {
        if (!holdsLock(this)) {
            throw new TIllegalMonitorStateException();
        }
        PlatformQueue<NotifyListener> listeners = monitor.notifyListeners;
        if (listeners == null) {
            return;
        }
        while (!listeners.isEmpty()) {
            NotifyListener listener = listeners.remove();
            if (!listener.expired()) {
                if (PlatformDetector.isLowLevel()) {
                    EventQueue.offer(listener);
                } else {
                    Platform.postpone(listener);
                }
            }
        }
        monitor.notifyListeners = null;
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

    public final void waitImpl(long timeout, int nanos, AsyncCallback<Void> callback) {
        Monitor monitor = this.monitor;
        final NotifyListenerImpl listener = new NotifyListenerImpl(this, callback, monitor.count);
        if (monitor.notifyListeners == null) {
            monitor.notifyListeners = Platform.createQueue();
        }
        monitor.notifyListeners.add(listener);
        TThread.currentThread().interruptHandler = listener;
        if (timeout > 0 || nanos > 0) {
            int timeoutToSchedule = timeout >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) timeout;
            listener.timerId = PlatformDetector.isLowLevel()
                    ? EventQueue.offer(listener, timeoutToSchedule + System.currentTimeMillis())
                    : Platform.schedule(listener, timeoutToSchedule);
        }
        monitorExit(this, monitor.count);
    }

    static class NotifyListenerImpl implements NotifyListener, TimerHandler, PlatformRunnable,
            TThreadInterruptHandler {
        final TObject obj;
        final AsyncCallback<Void> callback;
        final TThread currentThread = TThread.currentThread();
        int timerId = -1;
        boolean expired;
        boolean performed;
        int lockCount;

        NotifyListenerImpl(TObject obj, AsyncCallback<Void> callback, int lockCount) {
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
            if (PlatformDetector.isLowLevel()) {
                EventQueue.offer(() -> {
                    if (!expired()) {
                        run();
                    }
                });
            } else {
                Platform.postpone(() -> {
                    if (!expired()) {
                        run();
                    }
                });
            }
        }

        @Override
        public void run() {
            if (performed) {
                return;
            }
            performed = true;
            if (timerId >= 0) {
                if (PlatformDetector.isLowLevel()) {
                    EventQueue.kill(timerId);
                } else {
                    Platform.killSchedule(timerId);
                }
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
                if (PlatformDetector.isLowLevel()) {
                    EventQueue.kill(timerId);
                } else {
                    Platform.killSchedule(timerId);
                }
                timerId = -1;
            }
            if (PlatformDetector.isLowLevel()) {
                EventQueue.offer(() -> callback.error(new TInterruptedException()));
            } else {
                Platform.postpone(() -> callback.error(new TInterruptedException()));
            }
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
}
