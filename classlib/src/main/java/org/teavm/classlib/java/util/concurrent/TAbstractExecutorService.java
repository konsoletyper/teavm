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

import java.util.Objects;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TRunnable;

public abstract class TAbstractExecutorService implements TExecutorService {
    @Override
    public TFuture<TObject> submit(TRunnable task) {
        return submit(task, null);
    }

    @Override
    public <V extends TObject> TFuture<V> submit(TRunnable task, V result) {
        Objects.requireNonNull(task);
        TFutureTask<V> future = new TFutureTask<>(() -> {
            task.run();
            return result;
        });
        execute(future);
        return future;
    }

    @Override
    public <V extends TObject> TFuture<V> submit(TCallable<V> task) {
        TFutureTask<V> future = new TFutureTask<>(Objects.requireNonNull(task));
        execute(future);
        return future;
    }
}
