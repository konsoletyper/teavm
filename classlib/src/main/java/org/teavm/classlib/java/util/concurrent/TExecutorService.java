/*
 *  Copyright 2025 konsoletyper.
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.teavm.classlib.java.lang.TRunnable;

public interface TExecutorService extends TExecutor {
    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TTimeUnit unit) throws TInterruptedException;

    <T> TFuture<T> submit(Callable<T> task);

    <T> TFuture<T> submit(Runnable task, T result);

    TFuture<?> submit(Runnable task);

    <T> List<TFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws TInterruptedException;

    <T> List<TFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TTimeUnit unit)
            throws TInterruptedException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws TInterruptedException, TExecutionException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TTimeUnit unit)
            throws TInterruptedException, TExecutionException, TTimeoutException;
}
