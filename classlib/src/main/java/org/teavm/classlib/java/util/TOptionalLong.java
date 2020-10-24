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

import java.util.NoSuchElementException;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.stream.TLongStream;

public class TOptionalLong {
    private static TOptionalLong emptyInstance;
    private final long value;

    private TOptionalLong(long value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static TOptionalLong empty() {
        if (emptyInstance == null) {
            emptyInstance = new TOptionalLong(0);
        }
        return emptyInstance;
    }

    public static TOptionalLong of(long value) {
        return new TOptionalLong(value);
    }

    public long getAsLong() {
        if (this == emptyInstance) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public boolean isPresent() {
        return this != emptyInstance;
    }

    public boolean isEmpty() {
        return this == emptyInstance;
    }

    public void ifPresent(LongConsumer consumer) {
        if (this != emptyInstance) {
            consumer.accept(value);
        }
    }

    public long orElse(long other) {
        return this != emptyInstance ? value : other;
    }

    public long orElseGet(LongSupplier other) {
        return this != emptyInstance ? value : other.getAsLong();
    }

    public void ifPresentOrElse(LongConsumer action, Runnable emptyAction) {
        if (this == emptyInstance) {
            emptyAction.run();
        } else {
            action.accept(value);
        }
    }

    public <X extends Throwable> long orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (this == emptyInstance) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public long orElseThrow() {
        if (this == emptyInstance) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public TLongStream stream() {
        if (this == emptyInstance) {
            return TLongStream.empty();
        } else {
            return TLongStream.of(value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this == emptyInstance || obj == emptyInstance || !(obj instanceof TOptionalLong)) {
            return false;
        }

        return ((TOptionalLong) obj).value == value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return isPresent() ? "OptionalLong.of(" + value + ")" : "OptionalLong.empty()";
    }
}
