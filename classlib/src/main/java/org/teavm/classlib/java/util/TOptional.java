/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class TOptional<T> {
    private static TOptional<?> emptyInstance;
    private final T value;

    private TOptional(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <S> TOptional<S> empty() {
        if (emptyInstance == null) {
            emptyInstance = new TOptional<>(null);
        }
        return (TOptional<S>) emptyInstance;
    }

    public static <T> TOptional<T> of(T value) {
        return new TOptional<>(Objects.requireNonNull(value));
    }

    public static <T> TOptional<T> ofNullable(T value) {
        return value != null ? of(value) : empty();
    }

    public T get() {
        if (value == null) {
            throw new TNoSuchElementException();
        }
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public void ifPresent(Consumer<? super T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public TOptional<T> filter(Predicate<? super T> predicate) {
        return value == null || predicate.test(value) ? this : empty();
    }

    @SuppressWarnings("unchecked")
    public <U> TOptional<U> map(Function<? super T, ? extends U> mapper) {
        return value != null ? ofNullable(mapper.apply(value)) : (TOptional<U>) this;
    }

    @SuppressWarnings("unchecked")
    public <U> TOptional<U> flatMap(Function<? super T, TOptional<U>> mapper) {
        return value != null ? mapper.apply(value) : (TOptional<U>) this;
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    public T orElseGet(Supplier<? extends T> other) {
        return value != null ? value : other.get();
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TOptional<?>)) {
            return false;
        }

        return Objects.equals(((TOptional<?>) obj).value, value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return value != null ? "Optional.of(" + value + ")" : "Optional.empty()";
    }
}
