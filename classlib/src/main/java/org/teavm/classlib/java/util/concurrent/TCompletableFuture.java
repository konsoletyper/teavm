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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TInterruptedException;
import org.teavm.platform.Platform;
import org.teavm.runtime.EventQueue;

public class TCompletableFuture<T> implements TFuture<T>, TCompletionStage<T> {
    private static final int NEW = 0;
    private static final int COMPLETING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;

    private volatile int state = NEW;
    private Object result;
    private java.util.List<Completion> stack;

    public TCompletableFuture() {
    }

    public TCompletableFuture(boolean minimal) {
    }

    public static <U> TCompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, null);
    }

    public static <U> TCompletableFuture<U> supplyAsync(Supplier<U> supplier, TExecutor executor) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<U> f = new TCompletableFuture<>();
        execute(executor, () -> {
            try {
                f.complete(supplier.get());
            } catch (Throwable e) {
                f.completeExceptionally(e);
            }
        });
        return f;
    }

    public static TCompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, null);
    }

    public static TCompletableFuture<Void> runAsync(Runnable runnable, TExecutor executor) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<Void> f = new TCompletableFuture<>();
        execute(executor, () -> {
            try {
                runnable.run();
                f.complete(null);
            } catch (Throwable e) {
                f.completeExceptionally(e);
            }
        });
        return f;
    }

    public static <U> TCompletableFuture<U> completedFuture(U value) {
        TCompletableFuture<U> f = new TCompletableFuture<>();
        f.complete(value);
        return f;
    }

    public static <U> TCompletableFuture<U> failedFuture(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<U> f = new TCompletableFuture<>();
        f.completeExceptionally(ex);
        return f;
    }

    public boolean complete(T value) {
        if (state != NEW) {
            return false;
        }
        synchronized (this) {
            if (state != NEW) {
                return false;
            }
            result = value;
            state = NORMAL;
            postComplete();
        }
        return true;
    }

    public boolean completeExceptionally(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        if (state != NEW) {
            return false;
        }
        synchronized (this) {
            if (state != NEW) {
                return false;
            }
            result = ex;
            state = EXCEPTIONAL;
            postComplete();
        }
        return true;
    }

    public T join() {
        T r = getNow(null);
        if (r != null && state == NORMAL) {
            return r;
        }
        return waitingGet();
    }

    public T getNow(T valueIfAbsent) {
        if (state == NORMAL) {
            @SuppressWarnings("unchecked")
            T r = (T) result;
            return r;
        }
        return valueIfAbsent;
    }

    public boolean isCompletedExceptionally() {
        return state == EXCEPTIONAL || state == CANCELLED;
    }

    public void obtrudeValue(T value) {
        result = value;
        state = NORMAL;
        postComplete();
    }

    public void obtrudeException(Throwable ex) {
        result = ex;
        state = EXCEPTIONAL;
        postComplete();
    }

    public int getNumberOfDependents() {
        return stack != null ? stack.size() : 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state != NEW) {
            return false;
        }
        synchronized (this) {
            if (state != NEW) {
                return false;
            }
            state = CANCELLED;
            postComplete();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return state == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != NEW;
    }

    @Override
    public T get() throws TInterruptedException, TExecutionException {
        int s = state;
        if (s <= COMPLETING) {
            waitingGet();
            s = state;
        }
        return reportGet(s);
    }

    @Override
    public T get(long timeout, TTimeUnit unit) throws TInterruptedException, TExecutionException, TTimeoutException {
        Objects.requireNonNull(unit);
        int s = state;
        if (s <= COMPLETING) {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            waitingGet();
            s = state;
            if (s <= COMPLETING) {
                throw new TTimeoutException();
            }
        }
        return reportGet(s);
    }

    @SuppressWarnings("unchecked")
    private T reportGet(int s) throws TExecutionException {
        if (s == NORMAL) {
            return (T) result;
        }
        if (s == CANCELLED) {
            throw new TCancellationException();
        }
        throw new TCompletionException((Throwable) result);
    }

    private T waitingGet() {
        while (state <= COMPLETING) {
            if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                EventQueue.offer(() -> { });
            } else {
                Platform.postpone(() -> { });
            }
        }
        @SuppressWarnings("unchecked")
        T r = (T) result;
        return r;
    }

    private void postComplete() {
        if (stack == null) {
            return;
        }
        for (Completion c : stack) {
            c.run();
        }
        stack = null;
    }

    private void push(Completion c) {
        if (stack == null) {
            stack = new java.util.ArrayList<>();
        }
        stack.add(c);
        if (state > COMPLETING) {
            c.run();
        }
    }

    private static void execute(TExecutor executor, Runnable task) {
        if (executor != null) {
            executor.execute(task::run);
        } else {
            if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
                EventQueue.offer(task::run);
            } else {
                Platform.postpone(task::run);
            }
        }
    }

    // -- CompletionStage methods --

    @Override
    public <U> TCompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, null);
    }

    @Override
    public <U> TCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, null);
    }

    @Override
    public <U> TCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, TExecutor executor) {
        if (fn == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<U> dep = new TCompletableFuture<>();
        push(new ApplyCompletion<>(this, fn, dep, executor));
        return dep;
    }

    @Override
    public TCompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return thenAcceptAsync(action, null);
    }

    @Override
    public TCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenAcceptAsync(action, null);
    }

    @Override
    public TCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, TExecutor executor) {
        if (action == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<Void> dep = new TCompletableFuture<>();
        push(new AcceptCompletion<>(this, action, dep, executor));
        return dep;
    }

    @Override
    public TCompletableFuture<Void> thenRun(Runnable action) {
        return thenRunAsync(action, null);
    }

    @Override
    public TCompletableFuture<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, null);
    }

    @Override
    public TCompletableFuture<Void> thenRunAsync(Runnable action, TExecutor executor) {
        if (action == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<Void> dep = new TCompletableFuture<>();
        push(new RunCompletion<>(this, action, dep, executor));
        return dep;
    }

    @Override
    public <U, V> TCompletableFuture<V> thenCombine(TCompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return thenCombineAsync(other, fn, null);
    }

    @Override
    public <U, V> TCompletableFuture<V> thenCombineAsync(TCompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return thenCombineAsync(other, fn, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U, V> TCompletableFuture<V> thenCombineAsync(TCompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, TExecutor executor) {
        if (other == null || fn == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<V> dep = new TCompletableFuture<>();
        TCompletableFuture<? extends U> otherFuture = other.toCompletableFuture();
        BiFunCompletion<T, U, V> c = new BiFunCompletion<>(this, otherFuture, fn, dep, executor);
        push(c);
        otherFuture.push(c);
        return dep;
    }

    @Override
    public <U> TCompletableFuture<Void> thenAcceptBoth(TCompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, null);
    }

    @Override
    public <U> TCompletableFuture<Void> thenAcceptBothAsync(TCompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> TCompletableFuture<Void> thenAcceptBothAsync(TCompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, TExecutor executor) {
        if (other == null || action == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<Void> dep = new TCompletableFuture<>();
        TCompletableFuture<? extends U> otherFuture = other.toCompletableFuture();
        BiAcceptCompletion<T, U> c = new BiAcceptCompletion<>(this, otherFuture, action, dep, executor);
        push(c);
        otherFuture.push(c);
        return dep;
    }

    @Override
    public TCompletableFuture<Void> runAfterBoth(TCompletionStage<?> other, Runnable action) {
        return runAfterBothAsync(other, action, null);
    }

    @Override
    public TCompletableFuture<Void> runAfterBothAsync(TCompletionStage<?> other, Runnable action) {
        return runAfterBothAsync(other, action, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TCompletableFuture<Void> runAfterBothAsync(TCompletionStage<?> other, Runnable action,
            TExecutor executor) {
        if (other == null || action == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<Void> dep = new TCompletableFuture<>();
        TCompletableFuture<?> otherFuture = other.toCompletableFuture();
        BiRunCompletion<T> c = new BiRunCompletion<>(this, otherFuture, action, dep, executor);
        push(c);
        otherFuture.push(c);
        return dep;
    }

    @Override
    public <U> TCompletableFuture<U> applyToEither(TCompletionStage<? extends T> other, Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, null);
    }

    @Override
    public <U> TCompletableFuture<U> applyToEitherAsync(TCompletionStage<? extends T> other,
            Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> TCompletableFuture<U> applyToEitherAsync(TCompletionStage<? extends T> other,
            Function<? super T, U> fn, TExecutor executor) {
        if (other == null || fn == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<U> dep = new TCompletableFuture<>();
        TCompletableFuture<? extends T> otherFuture = other.toCompletableFuture();
        ApplyCompletion<T, U> c1 = new ApplyCompletion<>(this, fn, dep, executor);
        ApplyCompletion<T, U> c2 = new ApplyCompletion<>(otherFuture, fn, dep, executor);
        push(c1);
        otherFuture.push(c2);
        return dep;
    }

    @Override
    public TCompletableFuture<Void> acceptEither(TCompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, null);
    }

    @Override
    public TCompletableFuture<Void> acceptEitherAsync(TCompletionStage<? extends T> other,
            Consumer<? super T> action) {
        return acceptEitherAsync(other, action, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TCompletableFuture<Void> acceptEitherAsync(TCompletionStage<? extends T> other,
            Consumer<? super T> action, TExecutor executor) {
        if (other == null || action == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<Void> dep = new TCompletableFuture<>();
        TCompletableFuture<? extends T> otherFuture = other.toCompletableFuture();
        AcceptCompletion<T> c1 = new AcceptCompletion<>(this, action, dep, executor);
        AcceptCompletion<T> c2 = new AcceptCompletion<>(otherFuture, action, dep, executor);
        push(c1);
        otherFuture.push(c2);
        return dep;
    }

    @Override
    public TCompletableFuture<Void> runAfterEither(TCompletionStage<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, null);
    }

    @Override
    public TCompletableFuture<Void> runAfterEitherAsync(TCompletionStage<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TCompletableFuture<Void> runAfterEitherAsync(TCompletionStage<?> other, Runnable action,
            TExecutor executor) {
        if (other == null || action == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<Void> dep = new TCompletableFuture<>();
        TCompletableFuture<?> otherFuture = other.toCompletableFuture();
        RunCompletion<T> c1 = new RunCompletion<>(this, action, dep, executor);
        RunCompletion<T> c2 = new RunCompletion<>(otherFuture, action, dep, executor);
        push(c1);
        otherFuture.push(c2);
        return dep;
    }

    @Override
    public <U> TCompletableFuture<U> thenCompose(Function<? super T, ? extends TCompletionStage<U>> fn) {
        return thenComposeAsync(fn, null);
    }

    @Override
    public <U> TCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn) {
        return thenComposeAsync(fn, null);
    }

    @Override
    public <U> TCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn,
            TExecutor executor) {
        if (fn == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<U> dep = new TCompletableFuture<>();
        push(new ComposeCompletion<>(this, fn, dep, executor));
        return dep;
    }

    @Override
    public TCompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<T> dep = new TCompletableFuture<>();
        push(new ExceptionCompletion<>(this, fn, dep));
        return dep;
    }

    @Override
    public TCompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, null);
    }

    @Override
    public TCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, null);
    }

    @Override
    public TCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
            TExecutor executor) {
        if (action == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<T> dep = new TCompletableFuture<>();
        push(new WhenCompleteCompletion<>(this, action, dep, executor));
        return dep;
    }

    @Override
    public <U> TCompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, null);
    }

    @Override
    public <U> TCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handleAsync(fn, null);
    }

    @Override
    public <U> TCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
            TExecutor executor) {
        if (fn == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<U> dep = new TCompletableFuture<>();
        push(new HandleCompletion<>(this, fn, dep, executor));
        return dep;
    }

    @Override
    public TCompletableFuture<T> toCompletableFuture() {
        return this;
    }

    // -- Static utility methods --

    public static TCompletableFuture<Void> allOf(TCompletableFuture<?>... cfs) {
        if (cfs.length == 0) {
            return completedFuture(null);
        }
        TCompletableFuture<Void> result = new TCompletableFuture<>();
        TCountDownLatch latch = new TCountDownLatch(cfs.length);
        for (TCompletableFuture<?> cf : cfs) {
            cf.whenComplete((v, ex) -> latch.countDown());
        }
        latch.countDown(); // Start the countdown
        // Simplified: just check if all are done
        for (TCompletableFuture<?> cf : cfs) {
            cf.whenComplete((v, ex) -> {
                if (allDone(cfs)) {
                    result.complete(null);
                }
            });
        }
        if (allDone(cfs)) {
            result.complete(null);
        }
        return result;
    }

    private static boolean allDone(TCompletableFuture<?>... cfs) {
        for (TCompletableFuture<?> cf : cfs) {
            if (!cf.isDone()) {
                return false;
            }
        }
        return true;
    }

    public static TCompletableFuture<Object> anyOf(TCompletableFuture<?>... cfs) {
        if (cfs.length == 0) {
            TCompletableFuture<Object> result = new TCompletableFuture<>();
            return result;
        }
        TCompletableFuture<Object> result = new TCompletableFuture<>();
        for (TCompletableFuture<?> cf : cfs) {
            cf.whenComplete((v, ex) -> {
                if (ex != null) {
                    result.completeExceptionally(ex);
                } else {
                    @SuppressWarnings("unchecked")
                    Object val = v;
                    result.complete(val);
                }
            });
        }
        return result;
    }

    public TCompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        return exceptionallyAsync(fn, null);
    }

    public TCompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn, TExecutor executor) {
        if (fn == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<T> dep = new TCompletableFuture<>();
        push(new ExceptionCompletion<>(this, fn, dep, executor));
        return dep;
    }

    public TCompletableFuture<T> copy() {
        return thenApply(Function.identity());
    }

    public TCompletableFuture<T> newIncompleteFuture() {
        return new TCompletableFuture<>();
    }

    public TCompletableFuture<T> orTimeout(long timeout, TTimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<T> result = new TCompletableFuture<>();
        whenComplete((v, ex) -> result.complete(v != null ? v : (T) result.result));
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
            EventQueue.offer(() -> {
                if (!result.isDone()) {
                    result.completeExceptionally(new TTimeoutException());
                }
            }, deadline);
        } else {
            int millis = (int) Math.min(unit.toMillis(timeout), Integer.MAX_VALUE);
            Platform.schedule(() -> {
                if (!result.isDone()) {
                    result.completeExceptionally(new TTimeoutException());
                }
            }, millis);
        }
        whenComplete((v, ex) -> {
            if (ex != null) {
                result.completeExceptionally(ex);
            } else {
                @SuppressWarnings("unchecked")
                T val = (T) v;
                result.complete(val);
            }
        });
        return result;
    }

    public TCompletableFuture<T> completeOnTimeout(T value, long timeout, TTimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        TCompletableFuture<T> result = new TCompletableFuture<>();
        whenComplete((v, ex) -> {
            if (ex != null) {
                result.completeExceptionally(ex);
            } else {
                @SuppressWarnings("unchecked")
                T val = (T) v;
                result.complete(val);
            }
        });
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        if (PlatformDetector.isLowLevel() || PlatformDetector.isWebAssemblyGC()) {
            EventQueue.offer(() -> {
                if (!result.isDone()) {
                    result.complete(value);
                }
            }, deadline);
        } else {
            int millis = (int) Math.min(unit.toMillis(timeout), Integer.MAX_VALUE);
            Platform.schedule(() -> {
                if (!result.isDone()) {
                    result.complete(value);
                }
            }, millis);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        switch (state) {
            case NORMAL:
                sb.append("[Completed normally]");
                break;
            case EXCEPTIONAL:
                sb.append("[Completed exceptionally: ").append(result).append("]");
                break;
            case CANCELLED:
                sb.append("[Cancelled]");
                break;
            default:
                sb.append("[Not completed]");
                break;
        }
        return sb.toString();
    }

    // -- Completion nodes --

    abstract static class Completion implements Runnable {
        abstract void run();
    }

    static class ApplyCompletion<T, U> extends Completion {
        final TCompletableFuture<T> src;
        final Function<? super T, ? extends U> fn;
        final TCompletableFuture<U> dep;
        final TExecutor executor;
        volatile boolean claimed;

        ApplyCompletion(TCompletableFuture<T> src, Function<? super T, ? extends U> fn,
                TCompletableFuture<U> dep, TExecutor executor) {
            this.src = src;
            this.fn = fn;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            claimed = true;
            if (src.state == NORMAL) {
                @SuppressWarnings("unchecked")
                T val = (T) src.result;
                execute(executor, () -> {
                    try {
                        U result = fn.apply(val);
                        dep.complete(result);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            } else if (src.state == EXCEPTIONAL || src.state == CANCELLED) {
                dep.completeExceptionally((Throwable) src.result);
            }
        }
    }

    static class AcceptCompletion<T> extends Completion {
        final TCompletableFuture<T> src;
        final Consumer<? super T> action;
        final TCompletableFuture<Void> dep;
        final TExecutor executor;
        volatile boolean claimed;

        AcceptCompletion(TCompletableFuture<T> src, Consumer<? super T> action,
                TCompletableFuture<Void> dep, TExecutor executor) {
            this.src = src;
            this.action = action;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            claimed = true;
            if (src.state == NORMAL) {
                @SuppressWarnings("unchecked")
                T val = (T) src.result;
                execute(executor, () -> {
                    try {
                        action.accept(val);
                        dep.complete(null);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            } else if (src.state == EXCEPTIONAL || src.state == CANCELLED) {
                dep.completeExceptionally((Throwable) src.result);
            }
        }
    }

    static class RunCompletion<T> extends Completion {
        final TCompletableFuture<T> src;
        final Runnable action;
        final TCompletableFuture<Void> dep;
        final TExecutor executor;
        volatile boolean claimed;

        RunCompletion(TCompletableFuture<T> src, Runnable action,
                TCompletableFuture<Void> dep, TExecutor executor) {
            this.src = src;
            this.action = action;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            claimed = true;
            if (src.state == NORMAL || src.state == EXCEPTIONAL || src.state == CANCELLED) {
                execute(executor, () -> {
                    try {
                        action.run();
                        dep.complete(null);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            }
        }
    }

    static class BiFunCompletion<T, U, V> extends Completion {
        final TCompletableFuture<T> src1;
        final TCompletableFuture<U> src2;
        final BiFunction<? super T, ? super U, ? extends V> fn;
        final TCompletableFuture<V> dep;
        final TExecutor executor;
        volatile boolean claimed;

        BiFunCompletion(TCompletableFuture<T> src1, TCompletableFuture<U> src2,
                BiFunction<? super T, ? super U, ? extends V> fn,
                TCompletableFuture<V> dep, TExecutor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.fn = fn;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            if (src1.isDone() && src2.isDone()) {
                claimed = true;
                if (src1.state == NORMAL && src2.state == NORMAL) {
                    @SuppressWarnings("unchecked")
                    T v1 = (T) src1.result;
                    @SuppressWarnings("unchecked")
                    U v2 = (U) src2.result;
                    execute(executor, () -> {
                        try {
                            V result = fn.apply(v1, v2);
                            dep.complete(result);
                        } catch (Throwable e) {
                            dep.completeExceptionally(e);
                        }
                    });
                } else {
                    Throwable ex = src1.state != NORMAL ? (Throwable) src1.result : (Throwable) src2.result;
                    dep.completeExceptionally(ex);
                }
            }
        }
    }

    static class BiAcceptCompletion<T, U> extends Completion {
        final TCompletableFuture<T> src1;
        final TCompletableFuture<U> src2;
        final BiConsumer<? super T, ? super U> action;
        final TCompletableFuture<Void> dep;
        final TExecutor executor;
        volatile boolean claimed;

        BiAcceptCompletion(TCompletableFuture<T> src1, TCompletableFuture<U> src2,
                BiConsumer<? super T, ? super U> action,
                TCompletableFuture<Void> dep, TExecutor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.action = action;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            if (src1.isDone() && src2.isDone()) {
                claimed = true;
                if (src1.state == NORMAL && src2.state == NORMAL) {
                    @SuppressWarnings("unchecked")
                    T v1 = (T) src1.result;
                    @SuppressWarnings("unchecked")
                    U v2 = (U) src2.result;
                    execute(executor, () -> {
                        try {
                            action.accept(v1, v2);
                            dep.complete(null);
                        } catch (Throwable e) {
                            dep.completeExceptionally(e);
                        }
                    });
                } else {
                    Throwable ex = src1.state != NORMAL ? (Throwable) src1.result : (Throwable) src2.result;
                    dep.completeExceptionally(ex);
                }
            }
        }
    }

    static class BiRunCompletion<T> extends Completion {
        final TCompletableFuture<T> src1;
        final TCompletableFuture<?> src2;
        final Runnable action;
        final TCompletableFuture<Void> dep;
        final TExecutor executor;
        volatile boolean claimed;

        BiRunCompletion(TCompletableFuture<T> src1, TCompletableFuture<?> src2,
                Runnable action, TCompletableFuture<Void> dep, TExecutor executor) {
            this.src1 = src1;
            this.src2 = src2;
            this.action = action;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            if (src1.isDone() && src2.isDone()) {
                claimed = true;
                execute(executor, () -> {
                    try {
                        action.run();
                        dep.complete(null);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            }
        }
    }

    static class ComposeCompletion<T, U> extends Completion {
        final TCompletableFuture<T> src;
        final Function<? super T, ? extends TCompletionStage<U>> fn;
        final TCompletableFuture<U> dep;
        final TExecutor executor;
        volatile boolean claimed;

        ComposeCompletion(TCompletableFuture<T> src, Function<? super T, ? extends TCompletionStage<U>> fn,
                TCompletableFuture<U> dep, TExecutor executor) {
            this.src = src;
            this.fn = fn;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            claimed = true;
            if (src.state == NORMAL) {
                @SuppressWarnings("unchecked")
                T val = (T) src.result;
                execute(executor, () -> {
                    try {
                        TCompletionStage<U> stage = fn.apply(val);
                        stage.whenComplete((v, ex) -> {
                            if (ex != null) {
                                dep.completeExceptionally(ex);
                            } else {
                                dep.complete(v);
                            }
                        });
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            } else if (src.state == EXCEPTIONAL || src.state == CANCELLED) {
                dep.completeExceptionally((Throwable) src.result);
            }
        }
    }

    static class ExceptionCompletion<T> extends Completion {
        final TCompletableFuture<T> src;
        final Function<Throwable, ? extends T> fn;
        final TCompletableFuture<T> dep;
        final TExecutor executor;
        volatile boolean claimed;

        ExceptionCompletion(TCompletableFuture<T> src, Function<Throwable, ? extends T> fn,
                TCompletableFuture<T> dep) {
            this(src, fn, dep, null);
        }

        ExceptionCompletion(TCompletableFuture<T> src, Function<Throwable, ? extends T> fn,
                TCompletableFuture<T> dep, TExecutor executor) {
            this.src = src;
            this.fn = fn;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            claimed = true;
            if (src.state == NORMAL) {
                @SuppressWarnings("unchecked")
                T val = (T) src.result;
                dep.complete(val);
            } else if (src.state == EXCEPTIONAL || src.state == CANCELLED) {
                Throwable ex = (Throwable) src.result;
                execute(executor, () -> {
                    try {
                        T result = fn.apply(ex);
                        dep.complete(result);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            }
        }
    }

    static class WhenCompleteCompletion<T> extends Completion {
        final TCompletableFuture<T> src;
        final BiConsumer<? super T, ? super Throwable> action;
        final TCompletableFuture<T> dep;
        final TExecutor executor;
        volatile boolean claimed;

        WhenCompleteCompletion(TCompletableFuture<T> src, BiConsumer<? super T, ? super Throwable> action,
                TCompletableFuture<T> dep, TExecutor executor) {
            this.src = src;
            this.action = action;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            claimed = true;
            if (src.state == NORMAL) {
                @SuppressWarnings("unchecked")
                T val = (T) src.result;
                execute(executor, () -> {
                    try {
                        action.accept(val, null);
                        dep.complete(val);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            } else if (src.state == EXCEPTIONAL || src.state == CANCELLED) {
                Throwable ex = (Throwable) src.result;
                execute(executor, () -> {
                    try {
                        action.accept(null, ex);
                        dep.completeExceptionally(ex);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            }
        }
    }

    static class HandleCompletion<T, U> extends Completion {
        final TCompletableFuture<T> src;
        final BiFunction<? super T, Throwable, ? extends U> fn;
        final TCompletableFuture<U> dep;
        final TExecutor executor;
        volatile boolean claimed;

        HandleCompletion(TCompletableFuture<T> src, BiFunction<? super T, Throwable, ? extends U> fn,
                TCompletableFuture<U> dep, TExecutor executor) {
            this.src = src;
            this.fn = fn;
            this.dep = dep;
            this.executor = executor;
        }

        @Override
        void run() {
            if (claimed) {
                return;
            }
            claimed = true;
            if (src.state == NORMAL) {
                @SuppressWarnings("unchecked")
                T val = (T) src.result;
                execute(executor, () -> {
                    try {
                        U result = fn.apply(val, null);
                        dep.complete(result);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            } else if (src.state == EXCEPTIONAL || src.state == CANCELLED) {
                Throwable ex = (Throwable) src.result;
                execute(executor, () -> {
                    try {
                        U result = fn.apply(null, ex);
                        dep.complete(result);
                    } catch (Throwable e) {
                        dep.completeExceptionally(e);
                    }
                });
            }
        }
    }
}
