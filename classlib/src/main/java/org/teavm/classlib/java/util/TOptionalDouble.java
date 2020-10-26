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
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.stream.TDoubleStream;

public class TOptionalDouble {
    private static TOptionalDouble emptyInstance;
    private final double value;

    private TOptionalDouble(double value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static TOptionalDouble empty() {
        if (emptyInstance == null) {
            emptyInstance = new TOptionalDouble(0);
        }
        return emptyInstance;
    }

    public static TOptionalDouble of(double value) {
        return new TOptionalDouble(value);
    }

    public double getAsDouble() {
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

    public void ifPresent(DoubleConsumer consumer) {
        if (this != emptyInstance) {
            consumer.accept(value);
        }
    }

    public double orElse(double other) {
        return this != emptyInstance ? value : other;
    }

    public double orElseGet(DoubleSupplier other) {
        return this != emptyInstance ? value : other.getAsDouble();
    }

    public void ifPresentOrElse(DoubleConsumer action, Runnable emptyAction) {
        if (this == emptyInstance) {
            emptyAction.run();
        } else {
            action.accept(value);
        }
    }

    public <X extends Throwable> double orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (this == emptyInstance) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public double orElseThrow() {
        if (this == emptyInstance) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public TDoubleStream stream() {
        if (this == emptyInstance) {
            return TDoubleStream.empty();
        } else {
            return TDoubleStream.of(value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this == emptyInstance || obj == emptyInstance || !(obj instanceof TOptionalDouble)) {
            return false;
        }

        return ((TOptionalDouble) obj).value == value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public String toString() {
        return isPresent() ? "OptionalDouble.of(" + value + ")" : "OptionalDouble.empty()";
    }
}
