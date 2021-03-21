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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.stream.TIntStream;

public class TOptionalInt {
    private static TOptionalInt emptyInstance;
    private final int value;

    private TOptionalInt(int value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static TOptionalInt empty() {
        if (emptyInstance == null) {
            emptyInstance = new TOptionalInt(0);
        }
        return emptyInstance;
    }

    public static TOptionalInt of(int value) {
        return new TOptionalInt(value);
    }

    public int getAsInt() {
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

    public void ifPresent(IntConsumer consumer) {
        if (this != emptyInstance) {
            consumer.accept(value);
        }
    }

    public int orElse(int other) {
        return this != emptyInstance ? value : other;
    }

    public int orElseGet(IntSupplier other) {
        return this != emptyInstance ? value : other.getAsInt();
    }

    public void ifPresentOrElse(IntConsumer action, Runnable emptyAction) {
        if (this == emptyInstance) {
            emptyAction.run();
        } else {
            action.accept(value);
        }
    }

    public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (this == emptyInstance) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public int orElseThrow() {
        if (this == emptyInstance) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public TIntStream stream() {
        if (this == emptyInstance) {
            return TIntStream.empty();
        } else {
            return TIntStream.of(value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this == emptyInstance || obj == emptyInstance || !(obj instanceof TOptionalInt)) {
            return false;
        }

        return ((TOptionalInt) obj).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return isPresent() ? "OptionalInt.of(" + value + ")" : "OptionalInt.empty()";
    }
}
