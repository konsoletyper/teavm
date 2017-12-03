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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public abstract class TEnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, Serializable {
    public static <E extends Enum<E>> TEnumSet<E> noneOf(Class<E> elementType) {
        return new TGenericEnumSet<>(elementType);
    }

    public static <E extends Enum<E>> TEnumSet<E> allOf(Class<E> elementType) {
        int count = TGenericEnumSet.getConstants(elementType).length;
        int[] bits = new int[((count - 1) / 32) + 1];
        for (int i = 0; i < bits.length; ++i) {
            bits[i] = ~0;
        }
        zeroHighBits(bits, count);
        return new TGenericEnumSet<>(elementType, bits);
    }

    public static <E extends Enum<E>> TEnumSet<E> copyOf(TEnumSet<E> s) {
        TGenericEnumSet<E> other = (TGenericEnumSet<E>) s;
        return new TGenericEnumSet<>(other.cls, other.bits.clone());
    }

    public static <E extends Enum<E>> TEnumSet<E> copyOf(Collection<E> c) {
        if (c instanceof TEnumSet<?>) {
            return copyOf((TEnumSet<E>) c);
        } else {
            Iterator<E> iter = c.iterator();
            if (!iter.hasNext()) {
                throw new IllegalArgumentException();
            }
            E first = iter.next();
            @SuppressWarnings("unchecked")
            TEnumSet<E> result = noneOf(first.getDeclaringClass());
            result.add(first);
            while (iter.hasNext()) {
                result.add(iter.next());
            }
            return result;
        }
    }

    public static <E extends Enum<E>> TEnumSet<E> complementOf(TEnumSet<E> s) {
        TGenericEnumSet<E> other = (TGenericEnumSet<E>) s;
        int count = TGenericEnumSet.getConstants(other.cls).length;
        int[] bits = new int[other.bits.length];
        for (int i = 0; i < bits.length - 1; ++i) {
            bits[i] = ~other.bits[i];
        }
        zeroHighBits(bits, count);
        return new TGenericEnumSet<>(other.cls, bits);
    }

    public static <E extends Enum<E>> TEnumSet<E> of(E e) {
        TEnumSet<E> result = TEnumSet.noneOf(e.getDeclaringClass());
        result.fastAdd(e);
        return result;
    }

    public static <E extends Enum<E>> TEnumSet<E> of(E e1, E e2) {
        TEnumSet<E> result = TEnumSet.noneOf(e1.getDeclaringClass());
        result.fastAdd(e1);
        result.fastAdd(e2);
        return result;
    }

    public static <E extends Enum<E>> TEnumSet<E> of(E e1, E e2, E e3) {
        TEnumSet<E> result = TEnumSet.noneOf(e1.getDeclaringClass());
        result.fastAdd(e1);
        result.fastAdd(e2);
        result.fastAdd(e3);
        return result;
    }

    public static <E extends Enum<E>> TEnumSet<E> of(E e1, E e2, E e3, E e4) {
        TEnumSet<E> result = TEnumSet.noneOf(e1.getDeclaringClass());
        result.fastAdd(e1);
        result.fastAdd(e2);
        result.fastAdd(e3);
        result.fastAdd(e4);
        return result;
    }

    public static <E extends Enum<E>> TEnumSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        TEnumSet<E> result = TEnumSet.noneOf(e1.getDeclaringClass());
        result.fastAdd(e1);
        result.fastAdd(e2);
        result.fastAdd(e3);
        result.fastAdd(e4);
        result.fastAdd(e5);
        return result;
    }

    @SafeVarargs
    public static <E extends Enum<E>> TEnumSet<E> of(E first, E... rest) {
        TEnumSet<E> result = TEnumSet.noneOf(first.getDeclaringClass());
        result.fastAdd(first);
        for (E e : rest) {
            result.fastAdd(e);
        }
        return result;
    }

    @Override
    public TEnumSet<E> clone() {
        return copyOf(this);
    }

    abstract void fastAdd(E t);

    private static void zeroHighBits(int[] bits, int count) {
        bits[bits.length - 1] &= (~0) >>> (32 - count % 32);
    }
}
