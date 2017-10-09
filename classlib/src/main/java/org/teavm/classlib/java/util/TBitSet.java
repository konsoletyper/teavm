/*
 *  Copyright 2014 Alexey Andreev.
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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.*;
import org.teavm.interop.Rename;

public class TBitSet extends TObject implements TCloneable, TSerializable {
    private int[] data;
    private int length;

    private TBitSet(int[] data) {
        this.data = data;
        length = data.length * Integer.SIZE;
        recalculateLength();
    }

    public TBitSet() {
        data = new int[0];
    }

    public TBitSet(int nbits) {
        data = new int[(nbits + TInteger.SIZE - 1) / TInteger.SIZE];
    }

    public static TBitSet valueOf(long[] longs) {
        int[] ints = new int[longs.length * 2];
        for (int i = 0; i < longs.length; ++i) {
            ints[i * 2 + 1] = (int) longs[i];
            ints[i * 2 + 1] = (int) (longs[i] >>> TInteger.SIZE);
        }
        return new TBitSet(ints);
    }

    public static TBitSet valueOf(byte[] bytes) {
        int[] ints = new int[(bytes.length + 3) / 4];
        int fullInts = bytes.length / 4;
        for (int i = 0; i < fullInts; ++i) {
            ints[i] = (bytes[i * 4] & 0xFF) | ((bytes[i * 4 + 1] & 0xFF) << 8) | ((bytes[i * 4 + 2] & 0xFF) << 16)
                    | ((bytes[i * 4 + 3] & 0xFF) << 24);
        }
        int lastInt = ints.length - 1;
        int lastByte = lastInt * 4;
        switch (bytes.length % 4) {
            case 3:
                ints[lastInt] = (bytes[lastByte] & 0xFF) | ((bytes[lastByte + 1] & 0xFF) << 8)
                        | ((bytes[lastByte + 2] & 0xFF) << 16);
                break;
            case 2:
                ints[lastInt] = (bytes[lastByte] & 0xFF) | ((bytes[lastByte + 1] & 0xFF) << 8);
                break;
            case 1:
                ints[lastInt] = bytes[lastByte] & 0xFF;
                break;
        }
        return new TBitSet(ints);
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[(length + 7) / 8];
        int fullInts = length / TInteger.SIZE;
        int j = 0;
        int i = 0;
        for (; i < fullInts; i += 4) {
            bytes[j++] = (byte) data[i];
            bytes[j++] = (byte) (data[i] >>> 8);
            bytes[j++] = (byte) (data[i] >>> 16);
            bytes[j++] = (byte) (data[i] >>> 24);
        }
        switch (bytes.length % 4) {
            case 3:
                bytes[j++] = (byte) data[i];
                bytes[j++] = (byte) (data[i] >>> 8);
                bytes[j++] = (byte) (data[i] >>> 16);
                break;
            case 2:
                bytes[j++] = (byte) data[i];
                bytes[j++] = (byte) (data[i] >>> 8);
                break;
            case 1:
                bytes[j++] = (byte) data[i];
                break;
        }
        return bytes;
    }

    public long[] toLongArray() {
        long[] longs = new long[(length + 63) / 64];
        int fullLongs = length / 64;
        int i = 0;
        for (; i < fullLongs; ++i) {
            longs[i] = data[i * 2] | ((long) data[i * 2 + 1] << 32);
        }
        if ((((31 + length) / 32) & 1) == 1) {
            longs[i] = data[i * 2];
        }
        return longs;
    }

    public void flip(int bitIndex) {
        if (get(bitIndex)) {
            clear(bitIndex);
        } else {
            set(bitIndex);
        }
    }

    public void flip(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new TIndexOutOfBoundsException();
        }
        int fromDataIndex = fromIndex / 32;
        int toDataIndex = toIndex / 32;
        if (toIndex > length) {
            ensureCapacity(toDataIndex + 1);
            length = toIndex;
        }
        if (fromDataIndex == toDataIndex) {
            data[fromDataIndex] ^= trailingZeroBits(fromIndex) & trailingOneBits(toIndex);
        } else {
            data[fromDataIndex] ^= trailingZeroBits(fromIndex);
            for (int i = fromDataIndex + 1; i < toDataIndex; ++i) {
                data[i] ^= 0xFFFFFFFF;
            }
            data[toDataIndex] ^= trailingOneBits(toIndex);
        }
        if (toIndex == length) {
            recalculateLength();
        }
    }

    public void set(int bitIndex) {
        int index = bitIndex / 32;
        if (bitIndex >= length) {
            ensureCapacity(index + 1);
            length = bitIndex + 1;
        }
        data[index] |= 1 << (bitIndex % 32);
    }

    public void set(int bitIndex, boolean value) {
        if (value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }

    public void set(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new TIndexOutOfBoundsException();
        }
        int fromDataIndex = fromIndex / 32;
        int toDataIndex = toIndex / 32;
        if (toIndex > length) {
            ensureCapacity(toDataIndex + 1);
            length = toIndex;
        }
        if (fromDataIndex == toDataIndex) {
            data[fromDataIndex] |= trailingZeroBits(fromIndex) & trailingOneBits(toIndex);
        } else {
            data[fromDataIndex] |= trailingZeroBits(fromIndex);
            for (int i = fromDataIndex + 1; i < toDataIndex; ++i) {
                data[i] = 0xFFFFFFFF;
            }
            data[toDataIndex] |= trailingOneBits(toIndex);
        }
    }

    private int trailingZeroBits(int num) {
        num %= 32;
        return 0xFFFFFFFF << num;
    }

    private int trailingOneBits(int num) {
        num %= 32;
        return num != 0 ? 0xFFFFFFFF >>> (32 - num) : 0;
    }

    public void set(int fromIndex, int toIndex, boolean value) {
        if (value) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }

    public void clear(int bitIndex) {
        int index = bitIndex / 32;
        if (index < data.length) {
            data[index] &= TInteger.rotateLeft(0xFFFFFFFE, bitIndex % 32);
            if (bitIndex == length - 1) {
                recalculateLength();
            }
        }
    }

    public void clear(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new TIndexOutOfBoundsException();
        }
        if (fromIndex >= length) {
            return;
        }
        toIndex = TMath.min(length, toIndex);
        int fromDataIndex = fromIndex / 32;
        int toDataIndex = toIndex / 32;
        if (fromDataIndex == toDataIndex) {
            data[fromDataIndex] &= trailingOneBits(fromIndex) | trailingZeroBits(toIndex);
        } else {
            data[fromDataIndex] &= trailingOneBits(fromIndex);
            for (int i = fromDataIndex + 1; i < toDataIndex; ++i) {
                data[i] = 0;
            }
            data[toDataIndex] &= trailingZeroBits(toIndex);
        }
        recalculateLength();
    }

    public void clear() {
        length = 0;
        TArrays.fill(data, 0);
    }

    public boolean get(int bitIndex) {
        int index = bitIndex / 32;
        return index < data.length && (data[index] & (1 << (bitIndex % 32))) != 0;
    }

    public TBitSet get(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new TIndexOutOfBoundsException();
        }
        if (toIndex > length) {
            if (fromIndex > length) {
                return new TBitSet();
            }
            toIndex = length;
        }
        if (toIndex == fromIndex) {
            return new TBitSet();
        }
        int newBitSize = toIndex - fromIndex;
        int newArraySize = (newBitSize + 31) / 32;
        int[] newData = new int[newArraySize];
        int shift = fromIndex % 32;
        int offset = fromIndex / 32;
        if (shift != 0) {
            for (int i = 0; i < newData.length; ++i) {
                newData[i] = data[offset++] >>> shift;
                if (offset < data.length) {
                    newData[i] |= data[offset] << (32 - shift);
                }
            }
        } else {
            for (int i = 0; i < newData.length; ++i, ++offset) {
                newData[i] = data[offset];
            }
        }
        TBitSet result = new TBitSet(newData);
        result.clear(newBitSize, result.size());
        return result;
    }

    public int nextSetBit(int fromIndex) {
        if (fromIndex >= length) {
            return -1;
        }
        int index = fromIndex / 32;
        int val = data[index];
        val >>>= fromIndex % 32;
        if (val != 0) {
            return TInteger.numberOfTrailingZeros(val) + fromIndex;
        }
        int top = (length + 31) / 32;
        for (int i = index + 1; i < top; ++i) {
            if (data[i] != 0) {
                return i * 32 + TInteger.numberOfTrailingZeros(data[i]);
            }
        }
        return -1;
    }

    public int nextClearBit(int fromIndex) {
        if (fromIndex >= length) {
            return fromIndex;
        }
        int index = fromIndex / 32;
        int val = ~data[index];
        val >>>= fromIndex % 32;
        if (val != 0) {
            return TInteger.numberOfTrailingZeros(val) + fromIndex;
        }
        int top = (length + 31) / 32;
        for (int i = index + 1; i < top; ++i) {
            if (data[i] != 0xFFFFFFFF) {
                return i * 32 + TInteger.numberOfTrailingZeros(~data[i]);
            }
        }
        return length;
    }

    public int previousSetBit(int fromIndex) {
        if (fromIndex == -1) {
            return -1;
        }
        if (fromIndex >= length) {
            fromIndex = length;
        }
        int index = fromIndex / 32;
        int val = data[index];
        val <<= 31 - (fromIndex % 32);
        if (val != 0) {
            return fromIndex - TInteger.numberOfLeadingZeros(val);
        }
        for (int i = index - 1; i >= 0; ++i) {
            if (data[i] != 0) {
                return (i + 1) * 32 - TInteger.numberOfLeadingZeros(data[i]) - 1;
            }
        }
        return -1;
    }

    public int previousClearBit(int fromIndex) {
        if (fromIndex == -1) {
            return -1;
        }
        if (fromIndex >= length) {
            return fromIndex;
        }
        int index = fromIndex / 32;
        int val = ~data[index];
        val <<= 31 - (fromIndex % 32);
        if (val != 0) {
            return fromIndex - TInteger.numberOfLeadingZeros(val);
        }
        for (int i = index - 1; i >= 0; ++i) {
            if (data[i] != 0xFFFFFFFF) {
                return (i + 1) * 32 - TInteger.numberOfLeadingZeros(~data[i]) - 1;
            }
        }
        return -1;
    }

    private void ensureCapacity(int capacity) {
        if (data.length >= capacity) {
            return;
        }
        int newArrayLength = TMath.max(capacity * 3 / 2, data.length * 2 + 1);
        data = TArrays.copyOf(data, newArrayLength);
    }

    private void recalculateLength() {
        int top = (length + 31) / 32;
        length = top * 32;
        for (int i = top - 1; i >= 0; --i, length -= TInteger.SIZE) {
            int sz = TInteger.numberOfLeadingZeros(data[i]);
            if (sz < TInteger.SIZE) {
                length -= sz;
                break;
            }
        }
    }

    public int length() {
        return length;
    }

    public boolean intersects(TBitSet set) {
        int sz = TMath.min(data.length, set.data.length);
        for (int i = 0; i < sz; ++i) {
            if ((data[i] & set.data[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    public int cardinality() {
        int result = 0;
        int sz = 1 + length / 32;
        for (int i = 0; i < sz; ++i) {
            result += TInteger.bitCount(data[i]);
        }
        return result;
    }

    public void and(TBitSet set) {
        int sz = TMath.min(data.length, set.data.length);
        for (int i = 0; i < sz; ++i) {
            data[i] &= set.data[i];
        }
        for (int i = sz; i < data.length; ++i) {
            data[i] = 0;
        }
        length = TMath.min(length, set.length);
        recalculateLength();
    }

    public void andNot(TBitSet set) {
        int sz = TMath.min(data.length, set.data.length);
        for (int i = 0; i < sz; ++i) {
            data[i] &= ~set.data[i];
        }
        recalculateLength();
    }

    public void or(TBitSet set) {
        length = TMath.max(length, set.length);
        ensureCapacity((length + 31) / 32);
        int sz = TMath.min(data.length, set.length);
        for (int i = 0; i < sz; ++i) {
            data[i] |= set.data[i];
        }
    }

    public void xor(TBitSet set) {
        length = TMath.max(length, set.length);
        ensureCapacity((length + 31) / 32);
        int sz = TMath.min(data.length, set.length);
        for (int i = 0; i < sz; ++i) {
            data[i] ^= set.data[i];
        }
        recalculateLength();
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public int size() {
        return data.length * 32;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TBitSet)) {
            return false;
        }
        TBitSet set = (TBitSet) other;
        if (set.length != length) {
            return false;
        }
        int sz = TMath.min(data.length, set.data.length);
        for (int i = 0; i < sz; ++i) {
            if (data[i] != set.data[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        long h = 1234;
        long[] words = toLongArray();
        for (int i = words.length; --i >= 0;) {
            h ^= words[i] * (i + 1);
        }
        return (int) ((h >> 32) ^ h);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (int i = 0; i < data.length; ++i) {
            int bit = i * 32;
            if (bit > length) {
                break;
            }
            int val = data[i];
            while (val != 0) {
                int numZeros = TInteger.numberOfTrailingZeros(val);
                bit += numZeros;
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(bit++);
                val >>>= numZeros;
                val >>>= 1;
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Rename("clone")
    public TObject clone0() {
        return new TBitSet(TArrays.copyOf(data, data.length));
    }
}
