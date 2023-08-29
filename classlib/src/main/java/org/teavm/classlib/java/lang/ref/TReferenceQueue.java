/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.lang.ref;

import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.lang.TThreadInterruptHandler;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;

public class TReferenceQueue<T> {
    private RemoveCallback firstCallback;
    private RemoveCallback lastCallback;

    public TReference<T> poll() {
        return null;
    }

    public TReference<T> remove() throws InterruptedException {
        return remove(0);
    }

    @Async
    public native TReference<T> remove(long timeout) throws InterruptedException;

    public void remove(long timeout, AsyncCallback<TReference<T>> callback) {
        var ref = poll();
        if (ref != null) {
            callback.complete(ref);
        } else {
            var callbackWrapper = new RemoveCallback(callback);
            if (timeout != 0) {
                callbackWrapper.id = PlatformDetector.isLowLevel()
                        ? EventQueue.offer(callbackWrapper, timeout + System.currentTimeMillis())
                        : Platform.schedule(callbackWrapper, (int) timeout);
            }
            TThread.currentThread().interruptHandler = callbackWrapper;
            registerCallback(callbackWrapper);
        }
    }

    private void registerCallback(RemoveCallback callback) {
        callback.prev = lastCallback;
        if (lastCallback != null) {
            lastCallback.next = callback;
        } else {
            firstCallback = callback;
        }
        lastCallback = callback;
    }

    protected boolean reportNext(TReference<T> ref) {
        if (firstCallback == null) {
            return false;
        }
        var callback = firstCallback;
        callback.complete(ref);
        return true;
    }

    private class RemoveCallback implements EventQueue.Event, PlatformRunnable, AsyncCallback<TReference<T>>,
            TThreadInterruptHandler {
        RemoveCallback next;
        RemoveCallback prev;
        int id;
        AsyncCallback<TReference<T>> callback;

        RemoveCallback(AsyncCallback<TReference<T>> callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            if (PlatformDetector.isLowLevel()) {
                EventQueue.kill(id);
            } else {
                Platform.killSchedule(id);
            }
            complete(null);
        }

        @Override
        public void complete(TReference<T> result) {
            var callback = this.callback;
            if (callback != null) {
                remove();
                callback.complete(result);
            }
        }

        @Override
        public void error(Throwable e) {
            var callback = this.callback;
            if (callback != null) {
                remove();
                callback.error(e);
            }
        }

        @Override
        public void interrupted() {
            var callback = this.callback;
            if (callback != null) {
                remove();
                callback.error(new InterruptedException());
            }
        }

        private void remove() {
            TThread.currentThread().interruptHandler = null;
            callback = null;
            if (prev != null) {
                prev.next = next;
            } else {
                firstCallback = next;
            }
            if (next != null) {
                next.prev = prev;
            } else {
                lastCallback = prev;
            }
            next = null;
            prev = null;
        }
    }
}
