/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.nio;

import org.teavm.interop.Address;
import org.teavm.runtime.Allocator;

class TByteBufferNative extends TByteBuffer {
    private byte[] array;
    private int arrayOffset;
    @TNativeBufferObjectMarker
    private Object base;
    private Address address;
    private int capacity;
    private boolean readOnly;
    private boolean swap;

    TByteBufferNative(byte[] array, int arrayOffset, Object base, Address address, int capacity, boolean readOnly) {
        this.array = array;
        this.arrayOffset = arrayOffset;
        this.base = base;
        this.address = address;
        this.capacity = capacity;
        this.readOnly = readOnly;
        updateSwap();
    }

    @Override
    void onOrderChanged() {
        updateSwap();
    }

    private void updateSwap() {
        swap = order != TByteOrder.nativeOrder();
    }

    @Override
    byte[] arrayImpl() {
        if (array == null) {
            throw new UnsupportedOperationException();
        }
        return array;
    }

    @Override
    int arrayOffsetImpl() {
        if (array == null) {
            throw new UnsupportedOperationException();
        }
        return arrayOffset;
    }

    @Override
    boolean hasArrayImpl() {
        return array != null;
    }

    @Override
    int capacityImpl() {
        return capacity;
    }

    @Override
    void getImpl(int index, byte[] dst, int offset, int length) {
        copy(address.add(index), Address.ofData(dst).add(offset), length);
    }

    @Override
    void putImpl(int index, TByteBuffer src, int offset, int length) {
        if (src instanceof TByteBufferNative) {
            var srcImpl = (TByteBufferNative) src;
            copy(srcImpl.address.add(offset), address.add(index), length);
        } else if (src.hasArray()) {
            copy(Address.ofData(src.array()).add(offset + src.arrayOffset() + offset),
                    address.add(index), length);
        } else {
            var addr = address.add(index);
            while (length-- > 0) {
                addr.putByte(src.get(offset++));
                addr = addr.add(1);
            }
        }
    }

    @Override
    void putImpl(byte[] src, int srcOffset, int destOffset, int length) {
        copy(Address.ofData(src).add(srcOffset), address.add(destOffset), length);
    }

    @Override
    public TByteBuffer slice() {
        var newData = address.add(position);
        var result = new TByteBufferNative(array, arrayOffset + position, base, newData, remaining(), readOnly);
        result.position = 0;
        result.limit = result.capacity();
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public TByteBuffer duplicate() {
        var result = new TByteBufferNative(array, arrayOffset + position, base, address, capacity, readOnly);
        result.position = position;
        result.limit = limit;
        result.mark = mark;
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public TByteBuffer asReadOnlyBuffer() {
        var result = new TByteBufferNative(array, arrayOffset + position, base, address, capacity, true);
        result.position = position;
        result.limit = limit;
        result.mark = mark;
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public byte get() {
        if (position >= limit) {
            throw new TBufferUnderflowException();
        }
        return address.add(position++).getByte();
    }

    @Override
    public TByteBuffer put(byte b) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position >= limit) {
            throw new TBufferOverflowException();
        }
        address.add(position++).putByte(b);
        return this;
    }

    @Override
    public byte get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return address.add(index).getByte();
    }

    @Override
    public TByteBuffer put(int index, byte b) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        address.add(index).putByte(b);
        return this;
    }

    @Override
    public TByteBuffer compact() {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        int sz = remaining();
        if (position > 0) {
            copy(address.add(position), address, sz);
        }
        position = sz;
        limit = capacity();
        mark = -1;
        return this;
    }

    @Override
    public boolean isDirect() {
        return base == null;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public char getChar() {
        if (position + 1 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = address.add(position).getChar();
        position += 2;
        return swap ? Character.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putChar(char value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 1 >= limit) {
            throw new TBufferOverflowException();
        }
        address.add(position).putChar(swap ? Character.reverseBytes(value) : value);
        position += 2;
        return this;
    }

    @Override
    public char getChar(int index) {
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        var result = address.add(index).getChar();
        return swap ? Character.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putChar(int index, char value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        address.add(index).putChar(swap ? Character.reverseBytes(value) : value);
        return this;
    }

    @Override
    public TCharBuffer asCharBuffer() {
        int sz = remaining() / 2;
        return new TCharBufferNative(null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public short getShort() {
        if (position + 1 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = address.add(position).getShort();
        position += 2;
        return swap ? Short.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putShort(short value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 1 >= limit) {
            throw new TBufferOverflowException();
        }
        address.add(position).putShort(swap ? Short.reverseBytes(value) : value);
        position += 2;
        return this;
    }

    @Override
    public short getShort(int index) {
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        var result = address.add(index).getShort();
        return swap ? Short.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putShort(int index, short value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        address.add(index).putShort(swap ? Short.reverseBytes(value) : value);
        return this;
    }

    @Override
    public TShortBuffer asShortBuffer() {
        int sz = remaining() / 2;
        return new TShortBufferNative(null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public int getInt() {
        if (position + 3 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = address.add(position).getInt();
        position += 4;
        return swap ? Integer.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putInt(int value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 3 >= limit) {
            throw new TBufferOverflowException();
        }
        address.add(position).putInt(swap ? Integer.reverseBytes(value) : value);
        position += 4;
        return this;
    }

    @Override
    public int getInt(int index) {
        if (index < 0 || index + 3 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        var result = address.add(index).getInt();
        return swap ? Integer.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putInt(int index, int value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 3 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        address.add(index).putInt(swap ? Integer.reverseBytes(value) : value);
        return this;
    }

    @Override
    public TIntBuffer asIntBuffer() {
        int sz = remaining() / 4;
        return new TIntBufferNative(null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public float getFloat() {
        if (position + 3 >= limit) {
            throw new TBufferUnderflowException();
        }
        var address = this.address.add(position);
        var result = swap
                ? Float.intBitsToFloat(Integer.reverseBytes(address.getInt()))
                : address.getFloat();
        position += 4;
        return result;
    }

    @Override
    public TByteBuffer putFloat(float value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 3 >= limit) {
            throw new TBufferOverflowException();
        }
        var address = this.address.add(position);
        if (swap) {
            address.putInt(Integer.reverseBytes(Float.floatToRawIntBits(value)));
        } else {
            address.putFloat(value);
        }
        position += 4;
        return this;
    }

    @Override
    public TByteBuffer putFloat(int index, float value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index + 3 >= limit) {
            throw new TBufferOverflowException();
        }
        var address = this.address.add(index);
        if (swap) {
            address.putInt(Integer.reverseBytes(Float.floatToRawIntBits(value)));
        } else {
            address.putFloat(value);
        }
        return this;
    }

    @Override
    public float getFloat(int index) {
        if (index + 3 >= limit) {
            throw new TBufferUnderflowException();
        }
        var address = this.address.add(index);
        return swap
                ? Float.intBitsToFloat(Integer.reverseBytes(address.getInt()))
                : address.getFloat();
    }

    @Override
    public double getDouble() {
        if (position + 7 >= limit) {
            throw new TBufferUnderflowException();
        }
        var address = this.address.add(position);
        var result = swap
                ? Double.longBitsToDouble(Long.reverseBytes(address.getLong()))
                : address.getDouble();
        position += 8;
        return result;
    }

    @Override
    public TByteBuffer putDouble(double value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 7 >= limit) {
            throw new TBufferOverflowException();
        }
        var address = this.address.add(position);
        if (swap) {
            address.putLong(Long.reverseBytes(Double.doubleToRawLongBits(value)));
        } else {
            address.putDouble(value);
        }
        position += 8;
        return this;
    }

    @Override
    public double getDouble(int index) {
        if (index + 7 >= limit) {
            throw new TBufferUnderflowException();
        }
        var address = this.address.add(index);
        return swap
                ? Double.longBitsToDouble(Long.reverseBytes(address.getLong()))
                : address.getDouble();
    }

    @Override
    public TByteBuffer putDouble(int index, double value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index + 7 >= limit) {
            throw new TBufferOverflowException();
        }
        var address = this.address.add(index);
        if (swap) {
            address.putLong(Long.reverseBytes(Double.doubleToRawLongBits(value)));
        } else {
            address.putDouble(value);
        }
        return this;
    }

    @Override
    public long getLong() {
        if (position + 7 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = address.add(position).getLong();
        position += 8;
        return swap ? Long.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putLong(long value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 7 >= limit) {
            throw new TBufferOverflowException();
        }
        address.add(position).putLong(swap ? Long.reverseBytes(value) : value);
        position += 8;
        return this;
    }

    @Override
    public long getLong(int index) {
        if (index < 0 || index + 7 >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + (limit - 7) + ")");
        }
        var result = address.add(index).getLong();
        position += 8;
        return swap ? Long.reverseBytes(result) : result;
    }

    @Override
    public TByteBuffer putLong(int index, long value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 3 >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + (limit - 3) + ")");
        }
        address.add(index).putLong(swap ? Long.reverseBytes(value) : value);
        return this;
    }

    @Override
    public TLongBuffer asLongBuffer() {
        int sz = remaining() / 8;
        return new TLongBufferNative(null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public TFloatBuffer asFloatBuffer() {
        int sz = remaining() / 4;
        return new TFloatBufferNative(null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public TDoubleBuffer asDoubleBuffer() {
        int sz = remaining() / 8;
        return new TDoubleBufferNative(null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    static void copy(Address from, Address to, int count) {
        Allocator.moveMemoryBlock(from, to, count);
    }

    static TByteOrder oppositeOrder(TByteOrder order) {
        return order == TByteOrder.BIG_ENDIAN ? TByteOrder.LITTLE_ENDIAN : TByteOrder.BIG_ENDIAN;
    }
}
