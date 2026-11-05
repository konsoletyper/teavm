/*
 *  Copyright 2026 TeaVM contributors.
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
package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TRunnable;

public interface TScheduledExecutorService extends TExecutorService {
    TScheduledFuture<TObject> schedule(TRunnable command, long delay, TTimeUnit unit);

    <V extends TObject> TScheduledFuture<V> schedule(TCallable<V> callable, long delay, TTimeUnit unit);

    TScheduledFuture<?> scheduleAtFixedRate(TRunnable command, long initialDelay, long period, TTimeUnit unit);

    TScheduledFuture<?> scheduleWithFixedDelay(TRunnable command, long initialDelay, long delay, TTimeUnit unit);
}
