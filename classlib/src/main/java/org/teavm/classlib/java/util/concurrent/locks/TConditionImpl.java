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

import java.util.Date;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TThread;
import org.teavm.classlib.java.util.concurrent.TTimeUnit;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;
import org.teavm.runtime.EventQueue;

class TConditionImpl implements TCondition {
    private int waitCount;

    @Override
    @Async
    public native void await() throws InterruptedException;

    private void await(AsyncCallback<Void> callback) {
        waitCount++;
        if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
            EventQueue.offer(() -> {
                waitCount--;
                callback.complete(null);
            });
        } else {
            Platform.postpone(() -> {
                waitCount--;
                callback.complete(null);
            });
        }
    }

    @Override
    public void awaitUninterruptibly() {
        // In single-threaded environment, just return
    }

    @Override
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        // In single-threaded environment, just return remaining time
        return nanosTimeout;
    }

    @Override
    public boolean await(long time, TTimeUnit unit) throws InterruptedException {
        // In single-threaded environment, just return true
        return true;
    }

    @Override
    public boolean awaitUntil(Date deadline) throws InterruptedException {
        // In single-threaded environment, just return true
        return true;
    }

    @Override
    public void signal() {
        // In single-threaded environment, no actual signaling needed
    }

    @Override
    public void signalAll() {
        // In single-threaded environment, no actual signaling needed
    }

    boolean hasWaiters() {
        return waitCount > 0;
    }

    int getWaitQueueLength() {
        return waitCount;
    }
}
