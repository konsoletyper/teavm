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
import java.util.NoSuchElementException;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.platform.Platform;

class TGenericEnumSet<E extends Enum<E>> extends TEnumSet<E> {
    Class<E> cls;
    int[] bits;

    TGenericEnumSet(Class<E> cls) {
        this.cls = cls;
        int constantCount = getConstants(cls).length;
        int bitCount = ((constantCount - 1) / 32) + 1;
        this.bits = new int[bitCount];
    }

    TGenericEnumSet(Class<E> cls, int[] bits) {
        this.cls = cls;
        this.bits = bits;
    }

    static Enum<?>[] getConstants(Class<?> cls) {
        return Platform.getEnumConstants(((TClass<?>) (Object) cls).getPlatformClass());
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int index;
            int indexToRemove = -1;
            int count = size();

            @Override
            public boolean hasNext() {
                return count > 0;
            }

            @Override
            public E next() {
                if (count == 0) {
                    throw new NoSuchElementException();
                }
                indexToRemove = index;
                while (true) {
                    int next = Integer.numberOfTrailingZeros(bits[index / 32] >>> (index % 32));
                    if (next < 32) {
                        index += next;
                        --count;
                        @SuppressWarnings("unchecked")
                        E returnValue = (E) getConstants(cls)[index++];
                        return returnValue;
                    } else {
                        index = (index / 32 + 1) * 32;
                    }
                }
            }

            @Override
            public void remove() {
                if (indexToRemove < 0) {
                    throw new IllegalStateException();
                }
                int bitNumber = indexToRemove / 32;
                bits[bitNumber] &= ~(1 << (indexToRemove % 32));
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
            return false;
        }
        TGenericEnumSet<?> other = (TGenericEnumSet<?>) o;
        return cls == other.cls && Arrays.equals(bits, other.bits);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bits);
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
        int bitNumber = n / 32;
        int bit = 1 << (n % 32);
        return (bits[bitNumber] & bit) != 0;
    }

    @Override
    void fastAdd(E t) {
        int n = t.ordinal();
        int bitNumber = n / 32;
        bits[bitNumber] |= 1 << (n % 32);
    }

    @Override
    public boolean add(E t) {
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
        int bitNumber = n / 32;
        int bit = 1 << (n % 32);
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
