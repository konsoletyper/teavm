/*
 *  Copyright 2025 konsoletyper and other contributors.
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

import org.teavm.classlib.java.util.concurrent.TTimeUnit;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;

public interface TLock {
    void lock();

    @Async
    void lockInterruptibly() throws InterruptedException;

    boolean tryLock();

    boolean tryLock(long time, TTimeUnit unit) throws InterruptedException;

    void unlock();

    TCondition newCondition();

    static void lockInterruptibly(TLock lock, AsyncCallback<Void> callback) {
        lock.lock();
        callback.complete(null);
    }
}
