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
import org.teavm.dom.browser.Window;
import org.teavm.javascript.spi.Async;
import org.teavm.javascript.spi.Rename;
import org.teavm.javascript.spi.Superclass;
import org.teavm.jso.JS;
import org.teavm.jso.JSArray;
import org.teavm.jso.JSObject;
import org.teavm.platform.Platform;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@Superclass("")
public class TObject {
    private static final Window window = (Window)JS.getGlobal();
    private JSArray<NotifyListener> notifyListeners;
    private Lock lock;

    private static class Lock {
        TThread owner;
        int count;

        public Lock() {
            this.owner = TThread.currentThread();
            count = 1;
        }
    }

    interface NotifyListener extends JSObject {
        boolean handleNotify();
    }

    static void monitorEnter(TObject o) {
        if (o.lock == null) {
            o.lock = new Lock();
            return;
        }
        if (o.lock.owner != TThread.currentThread()) {
            while (o.lock != null) {
                try {
                    o.lock.wait();
                } catch (InterruptedException ex) {
                }
            }
            o.lock = new Lock();
        } else {
            o.lock.count++;
        }
    }

    static void monitorExit(TObject o){
        if (o.lock != null && o.lock.count-- == 0) {
            o.lock.notifyAll();
            o.lock = null;
        }
    }

    static boolean holdsLock(TObject o){
        return o.lock != null && o.lock.owner == TThread.currentThread();
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

    @Rename("notify")
    public final void notify0() {
        TThread thread = TThread.currentThread();
        if (notifyListeners != null) {
            while (notifyListeners.getLength() > 0 && notifyListeners.shift().handleNotify()) {
                // repeat loop
            }
            if (notifyListeners.getLength() == 0) {
                notifyListeners = null;
            }
        }
        TThread.setCurrentThread(thread);
    }

    @Rename("notifyAll")
    public final void notifyAll0(){
        if (notifyListeners != null){
            JSArray<NotifyListener> listeners = window.newArray();
            while (notifyListeners.getLength() > 0) {
                listeners.push(notifyListeners.shift());
            }
            notifyListeners = null;
            while (listeners.getLength() > 0) {
                listeners.shift().handleNotify();
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
        if (notifyListeners == null) {
            notifyListeners = window.newArray();
        }
        final NotifyListenerImpl listener = new NotifyListenerImpl(callback);
        notifyListeners.push(listener);
        if (timeout == 0 && nanos == 0) {
            return;
        }
        listener.timerId = window.setTimeout(listener, timeout);
    }

    private static class NotifyListenerImpl implements NotifyListener, TimerHandler {
        final AsyncCallback<Void> callback;
        final TThread currentThread = TThread.currentThread();
        int timerId = -1;
        boolean finished;

        public NotifyListenerImpl(AsyncCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public boolean handleNotify() {
            if (finished) {
                return false;
            }
            TThread.setCurrentThread(currentThread);
            if (timerId >= 0) {
                window.clearTimeout(timerId);
                timerId = -1;
            }
            finished = true;
            try {
                callback.complete(null);
            } finally {
                TThread.setCurrentThread(TThread.getMainThread());
            }
            return true;
        }

        @Override
        public void onTimer() {
            handleNotify();
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
