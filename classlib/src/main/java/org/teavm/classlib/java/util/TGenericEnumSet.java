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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformClass;

class TGenericEnumSet<E extends Enum<E>> extends TEnumSet<E> {
    Class<E> cls;
    int[] bits;

    TGenericEnumSet(Class<E> cls) {
        this.cls = cls;
        Enum<?>[] constants = getConstants(cls);
        if (constants == null) {
            throw new ClassCastException();
        }
        int constantCount = constants.length;
        int bitCount = constantCount == 0 ? 0 : ((constantCount - 1) / Integer.SIZE) + 1;
        this.bits = new int[bitCount];
    }

    TGenericEnumSet(Class<E> cls, int[] bits) {
        this.cls = cls;
        this.bits = bits;
    }

    static Enum<?>[] getConstants(Class<?> cls) {
        PlatformClass platformClass = ((TClass<?>) (Object) cls).getPlatformClass();
        Platform.initClass(platformClass);
        return Platform.getEnumConstants(platformClass);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private int index = find();
            private int indexToRemove = -1;

            private int find() {
                int overflow = bits.length * Integer.SIZE;
                while (index < overflow) {
                    int next = Integer.numberOfTrailingZeros(bits[index / Integer.SIZE] >>> (index % Integer.SIZE));
                    if (next < Integer.SIZE) {
                        index += next;
                        return index;
                    } else {
                        index = (index / Integer.SIZE + 1) * Integer.SIZE;
                    }
                }
                return index;
            }

            @Override
            public boolean hasNext() {
                return index < bits.length * Integer.SIZE;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new TNoSuchElementException();
                }
                indexToRemove = index;
                @SuppressWarnings("unchecked")
                E returnValue = (E) getConstants(cls)[index++];
                index = find();
                return returnValue;
            }

            @Override
            public void remove() {
                if (indexToRemove < 0) {
                    throw new IllegalStateException();
                }
                int bitNumber = indexToRemove / Integer.SIZE;
                bits[bitNumber] &= ~(1 << (indexToRemove % Integer.SIZE));
                indexToRemove = -1;
            }
        };
    }

    @Override
    public int size() {
        int result = 0;
        for (int bit : bits) {
            result += Integer.bitCount(bit);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TGenericEnumSet)) {
            return super.equals(o);
        }
        TGenericEnumSet<?> other = (TGenericEnumSet<?>) o;
        if (this.cls != other.cls) {
            return this.size() == 0 && other.size() == 0;
        }
        return Arrays.equals(bits, other.bits);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c instanceof TGenericEnumSet<?>) {
            TGenericEnumSet<?> other = (TGenericEnumSet<?>) c;
            if (cls == other.cls) {
                boolean changed = false;
                for (int i = 0; i < bits.length; ++i) {
                    int inv = ~other.bits[i];
                    if ((bits[i] & inv) != bits[i]) {
                        changed = true;
                        bits[i] &= inv;
                    }
                }
                return changed;
            }
        }
        return super.removeAll(c);
    }

    @Override
    public boolean contains(Object o) {
        if (!cls.isInstance(o)) {
            return false;
        }
        int n = ((Enum<?>) o).ordinal();
        int bitNumber = n / Integer.SIZE;
        int bit = 1 << (n % Integer.SIZE);
        return (bits[bitNumber] & bit) != 0;
    }

    @Override
    void fastAdd(int n) {
        int bitNumber = n / Integer.SIZE;
        bits[bitNumber] |= 1 << (n % Integer.SIZE);
    }

    @Override
    public boolean add(E t) {
        Class<?> tCls = t.getClass();
        if (tCls != cls && tCls.getSuperclass() != cls) {
            throw new ClassCastException();
        }
        int n = t.ordinal();
        int bitNumber = n / 32;
        int bit = 1 << (n % 32);
        if ((bits[bitNumber] & bit) == 0) {
            bits[bitNumber] |= bit;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!cls.isInstance(o)) {
            return false;
        }

        int n = ((Enum<?>) o).ordinal();
        int bitNumber = n / Integer.SIZE;
        int bit = 1 << (n % Integer.SIZE);
        if ((bits[bitNumber] & bit) != 0) {
            bits[bitNumber] &= ~bit;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c instanceof TGenericEnumSet<?>) {
            TGenericEnumSet<?> other = (TGenericEnumSet<?>) c;
            if (cls == other.cls) {
                for (int i = 0; i < bits.length; ++i) {
                    if ((bits[i] | other.bits[i]) != bits[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return super.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c instanceof TGenericEnumSet<?>) {
            TGenericEnumSet<?> other = (TGenericEnumSet<?>) c;
            if (cls == other.cls) {
                boolean added = false;
                for (int i = 0; i < bits.length; ++i) {
                    if ((bits[i] | other.bits[i]) != bits[i]) {
                        added = true;
                        bits[i] |= other.bits[i];
                    }
                }
                return added;
            }
        }
        return super.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c instanceof TGenericEnumSet<?>) {
            TGenericEnumSet<?> other = (TGenericEnumSet<?>) c;
            if (cls == other.cls) {
                boolean changed = false;
                for (int i = 0; i < bits.length; ++i) {
                    if ((bits[i] & other.bits[i]) != bits[i]) {
                        changed = true;
                        bits[i] &= other.bits[i];
                    }
                }
                return changed;
            }
        }
        return super.retainAll(c);
    }

    @Override
    public void clear() {
        Arrays.fill(bits, 0);
    }
}
