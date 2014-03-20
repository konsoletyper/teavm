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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
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
        data = new int[TMath.max(1, (nbits + TInteger.SIZE - 1) / TInteger.SIZE)];
    }

    public static TBitSet valueOf(long[] longs) {
        int[] ints = new int[longs.length * 2];
        for (int i = 0; i < longs.length; ++i) {
            ints[i * 2 + 1] = (int)longs[i];
            ints[i * 2 + 1] = (int)(longs[i] >>> TInteger.SIZE);
        }
        return new TBitSet(ints);
    }

    public static TBitSet valueOf(byte[] bytes) {
        int[] ints = new int[(bytes.length + 3) / 4];
        int fullInts = bytes.length / 4;
        for (int i = 0; i < fullInts; ++i) {
            ints[i] = bytes[i * 4] | (bytes[i * 4 + 1] << 8) | (bytes[i * 4 + 2] << 16) | (bytes[i * 4 + 3] << 24);
        }
        int lastInt = ints.length - 1;
        int lastByte = bytes[lastInt * 4];
        switch (bytes.length % 4) {
            case 3:
                ints[lastInt] = bytes[lastByte] | (bytes[lastByte + 1] << 8) | (bytes[lastByte + 2] << 16);
                break;
            case 2:
                ints[lastInt] = bytes[lastByte] | (bytes[lastByte + 1] << 8);
                break;
            case 1:
                ints[lastInt] = bytes[lastByte];
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
            bytes[j++] = (byte)data[i];
            bytes[j++] = (byte)(data[i] >>> 8);
            bytes[j++] = (byte)(data[i] >>> 16);
            bytes[j++] = (byte)(data[i] >>> 24);
        }
        switch (bytes.length % 4) {
            case 3:
                bytes[j++] = (byte)data[i];
                bytes[j++] = (byte)(data[i] >>> 8);
                bytes[j++] = (byte)(data[i] >>> 16);
                break;
            case 2:
                bytes[j++] = (byte)data[i];
                bytes[j++] = (byte)(data[i] >>> 8);
                break;
            case 1:
                bytes[j++] = (byte)data[i];
                break;
        }
        return bytes;
    }

    public long[] toLongArray() {
        long[] longs = new long[(length + 63) / 64];
        int fullLongs = length / 64;
        int i = 0;
        for (; i < fullLongs; ++i) {
            longs[i] = data[i * 2] | (data[i * 2 + 1] << 32);
        }
        if ((length / 32) % 2 == 1) {
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
            data[fromDataIndex] |= (0xFFFFFFFF << (fromIndex % 32)) & (0xFFFFFFFF >>> (32 - toIndex % 32));
        } else {
            data[fromDataIndex] |= 0xFFFFFFFF << (fromIndex % 32);
            for (int i = fromDataIndex + 1; i < toDataIndex; ++i) {
                data[i] = 0xFFFFFFFF;
            }
            data[toDataIndex] |= 0xFFFFFFFF >>> (32 - toIndex % 32);
        }
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
            data[index] &= TInteger.rotateLeft(bitIndex, 0xFFFFFFFE);
            recalculateLength();
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
            data[fromDataIndex] &= (0xFFFFFFFF >>> (32 - fromIndex % 32)) | (0xFFFFFFFF << (toIndex % 32));
        } else {
            data[fromDataIndex] &= 0xFFFFFFFF >>> (32 - fromIndex % 32);
            for (int i = fromDataIndex + 1; i < toDataIndex; ++i) {
                data[i] = 0;
            }
            data[toDataIndex] &= 0xFFFFFFFF << (toIndex % 32);
        }
        recalculateLength();
    }

    public boolean get(int bitIndex) {
        int index = bitIndex / 32;
        return index < data.length && (data[index] & (1 << (bitIndex % 32))) != 0;
    }

    public void clear() {
        length = 0;
        TArrays.fill(data, 0);
    }

    private void ensureCapacity(int capacity) {
        if (data.length >= capacity) {
            return;
        }
        int newArrayLength = TMath.max(capacity * 3 / 2, data.length * 2 + 1);
        data = TArrays.copyOf(data, newArrayLength);
    }

    private void recalculateLength() {
        length = (1 + length / 32) * 32;
        for (int i = data.length - 1; i >= 0; --i, length -= TInteger.SIZE) {
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

    public boolean isEmpty() {
        return length == 0;
    }

    public int size() {
        return data.length * 32;
    }
}
