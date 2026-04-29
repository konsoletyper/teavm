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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TCompletionStage<T> {
    <U> TCompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

    <U> TCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    <U> TCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, TExecutor executor);

    TCompletionStage<Void> thenAccept(Consumer<? super T> action);

    TCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);

    TCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, TExecutor executor);

    TCompletionStage<Void> thenRun(Runnable action);

    TCompletionStage<Void> thenRunAsync(Runnable action);

    TCompletionStage<Void> thenRunAsync(Runnable action, TExecutor executor);

    <U, V> TCompletionStage<V> thenCombine(TCompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> TCompletionStage<V> thenCombineAsync(TCompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> TCompletionStage<V> thenCombineAsync(TCompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, TExecutor executor);

    <U> TCompletionStage<Void> thenAcceptBoth(TCompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action);

    <U> TCompletionStage<Void> thenAcceptBothAsync(TCompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action);

    <U> TCompletionStage<Void> thenAcceptBothAsync(TCompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, TExecutor executor);

    TCompletionStage<Void> runAfterBoth(TCompletionStage<?> other, Runnable action);

    TCompletionStage<Void> runAfterBothAsync(TCompletionStage<?> other, Runnable action);

    TCompletionStage<Void> runAfterBothAsync(TCompletionStage<?> other, Runnable action, TExecutor executor);

    <U> TCompletionStage<U> applyToEither(TCompletionStage<? extends T> other, Function<? super T, U> fn);

    <U> TCompletionStage<U> applyToEitherAsync(TCompletionStage<? extends T> other, Function<? super T, U> fn);

    <U> TCompletionStage<U> applyToEitherAsync(TCompletionStage<? extends T> other, Function<? super T, U> fn,
            TExecutor executor);

    TCompletionStage<Void> acceptEither(TCompletionStage<? extends T> other, Consumer<? super T> action);

    TCompletionStage<Void> acceptEitherAsync(TCompletionStage<? extends T> other, Consumer<? super T> action);

    TCompletionStage<Void> acceptEitherAsync(TCompletionStage<? extends T> other, Consumer<? super T> action,
            TExecutor executor);

    TCompletionStage<Void> runAfterEither(TCompletionStage<?> other, Runnable action);

    TCompletionStage<Void> runAfterEitherAsync(TCompletionStage<?> other, Runnable action);

    TCompletionStage<Void> runAfterEitherAsync(TCompletionStage<?> other, Runnable action, TExecutor executor);

    <U> TCompletionStage<U> thenCompose(Function<? super T, ? extends TCompletionStage<U>> fn);

    <U> TCompletionStage<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn);

    <U> TCompletionStage<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn,
            TExecutor executor);

    TCompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);

    TCompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    TCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action);

    TCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, TExecutor executor);

    <U> TCompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    <U> TCompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn);

    <U> TCompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, TExecutor executor);

    TCompletableFuture<T> toCompletableFuture();
}
