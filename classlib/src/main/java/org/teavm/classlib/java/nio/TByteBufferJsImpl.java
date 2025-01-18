/*
 *  Copyright 2015 Alexey Andreev.
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

import org.teavm.jso.typedarrays.BigInt64Array;
import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Float64Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint16Array;

class TByteBufferJsImpl extends TByteBuffer {
    private byte[] array;
    private int arrayOffset;
    private Int8Array data;
    private DataView dataView;
    private boolean direct;
    private boolean readOnly;

    TByteBufferJsImpl(byte[] array, int arrayOffset, Int8Array data, boolean direct, boolean readOnly) {
        this.array = array;
        this.arrayOffset = arrayOffset;
        this.data = data;
        this.direct = direct;
        this.readOnly = readOnly;
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
        return data.getLength();
    }

    @Override
    void getImpl(int index, byte[] dst, int offset, int length) {
        var slice = new Int8Array(data.getBuffer(), data.getByteOffset() + index, length);
        Int8Array.fromJavaArray(dst).set(slice, offset);
    }

    @Override
    void putImpl(int index, TByteBuffer src, int offset, int length) {
        if (src instanceof TByteBufferJsImpl) {
            var srcImpl = (TByteBufferJsImpl) src;
            var slice = new Int8Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + offset, length);
            data.set(slice, index);
        } else if (src.hasArray()) {
            var slice = new Int8Array(Int8Array.fromJavaArray(src.array()).getBuffer(), src.arrayOffset() + offset,
                    length);
            data.set(slice, index);
        } else {
            for (var i = 0; i < length; i++) {
                put(index + i, src.get(offset + i));
            }
        }
    }

    @Override
    void putImpl(byte[] src, int srcOffset, int destOffset, int length) {
        var slice = new Int8Array(Int8Array.fromJavaArray(src).getBuffer(), srcOffset, length);
        data.set(slice, destOffset);
    }

    @Override
    public TByteBuffer slice() {
        var newData = new Int8Array(data.getBuffer(), data.getByteOffset() + position, remaining());
        var result = new TByteBufferJsImpl(array, arrayOffset + position, newData, direct, readOnly);
        result.position = 0;
        result.limit = newData.getLength();
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public TByteBuffer duplicate() {
        var result = new TByteBufferJsImpl(array, arrayOffset + position, data, direct, readOnly);
        result.position = position;
        result.limit = limit;
        result.mark = mark;
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public TByteBuffer asReadOnlyBuffer() {
        var result = new TByteBufferJsImpl(array, arrayOffset + position, data, direct, true);
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
        return data.get(position++);
    }

    @Override
    public TByteBuffer put(byte b) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position >= limit) {
            throw new TBufferOverflowException();
        }
        data.set(position++, b);
        return this;
    }

    @Override
    public byte get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return data.get(index);
    }

    @Override
    public TByteBuffer put(int index, byte b) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException();
        }
        data.set(index, b);
        return this;
    }

    @Override
    public TByteBuffer compact() {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        int sz = remaining();
        if (position > 0) {
            data.set(new Int8Array(data.getBuffer(), data.getByteOffset() + position, sz), 0);
        }
        position = sz;
        limit = capacity();
        mark = -1;
        return this;
    }

    @Override
    public boolean isDirect() {
        return direct;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    private DataView getDataView() {
        if (dataView == null) {
            dataView = new DataView(data.getBuffer(), data.getByteOffset(), data.getByteLength());
        }
        return dataView;
    }

    @Override
    public char getChar() {
        if (position + 1 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = getDataView().getUint16(position, order == TByteOrder.LITTLE_ENDIAN);
        position += 2;
        return (char) result;
    }

    @Override
    public TByteBuffer putChar(char value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 1 >= limit) {
            throw new TBufferOverflowException();
        }
        getDataView().setUint16(position, value, order == TByteOrder.LITTLE_ENDIAN);
        position += 2;
        return this;
    }

    @Override
    public char getChar(int index) {
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return (char) getDataView().getUint16(index, order == TByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public TByteBuffer putChar(int index, char value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        getDataView().setUint16(index, value, order == TByteOrder.LITTLE_ENDIAN);
        return this;
    }

    @Override
    public TCharBuffer asCharBuffer() {
        int sz = remaining() / 2;
        if (order == TByteOrder.nativeOrder()) {
            var slice = new Uint16Array(data.getBuffer(), data.getByteOffset() + position, sz);
            return new TCharBufferOverTypedArray(0, sz, readOnly, slice, null);
        } else {
            var slice = new DataView(data.getBuffer(), data.getByteOffset() + position, sz * 2);
            return new TCharBufferOverDataView(0, sz, readOnly, slice, order == TByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public short getShort() {
        if (position + 1 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = getDataView().getInt16(position, order == TByteOrder.LITTLE_ENDIAN);
        position += 2;
        return result;
    }

    @Override
    public TByteBuffer putShort(short value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 1 >= limit) {
            throw new TBufferOverflowException();
        }
        getDataView().setInt16(position, value, order == TByteOrder.LITTLE_ENDIAN);
        position += 2;
        return this;
    }

    @Override
    public short getShort(int index) {
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return getDataView().getInt16(index, order == TByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public TByteBuffer putShort(int index, short value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        getDataView().setInt16(index, value, order == TByteOrder.LITTLE_ENDIAN);
        return this;
    }

    @Override
    public TShortBuffer asShortBuffer() {
        int sz = remaining() / 2;
        if (order == TByteOrder.nativeOrder()) {
            var slice = new Int16Array(data.getBuffer(), data.getByteOffset() + position, sz);
            return new TShortBufferOverTypedArray(0, sz, readOnly, slice, null);
        } else {
            var slice = new DataView(data.getBuffer(), data.getByteOffset() + position, sz * 2);
            return new TShortBufferOverDataView(0, sz, readOnly, slice, order == TByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public int getInt() {
        if (position + 3 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = getDataView().getInt32(position, order == TByteOrder.LITTLE_ENDIAN);
        position += 4;
        return result;
    }

    @Override
    public TByteBuffer putInt(int value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 3 >= limit) {
            throw new TBufferOverflowException();
        }
        getDataView().setInt32(position, value, order == TByteOrder.LITTLE_ENDIAN);
        position += 4;
        return this;
    }

    @Override
    public int getInt(int index) {
        if (index < 0 || index + 3 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        return getDataView().getInt32(index, order == TByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public TByteBuffer putInt(int index, int value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 3 >= limit) {
            throw new IndexOutOfBoundsException();
        }
        getDataView().setInt32(index, value, order == TByteOrder.LITTLE_ENDIAN);
        return this;
    }

    @Override
    public TIntBuffer asIntBuffer() {
        int sz = remaining() / 4;
        if (order == TByteOrder.nativeOrder()) {
            var slice = new Int32Array(data.getBuffer(), data.getByteOffset() + position, sz);
            return new TIntBufferOverTypedArray(0, sz, readOnly, slice, null);
        } else {
            var slice = new DataView(data.getBuffer(), data.getByteOffset() + position, sz * 4);
            return new TIntBufferOverDataView(0, sz, readOnly, slice, order == TByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public float getFloat() {
        if (position + 3 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = getDataView().getFloat32(position, order == TByteOrder.LITTLE_ENDIAN);
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
        getDataView().setFloat32(position, value, order == TByteOrder.LITTLE_ENDIAN);
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
        getDataView().setFloat32(index, value, order == TByteOrder.LITTLE_ENDIAN);
        return this;
    }

    @Override
    public float getFloat(int index) {
        if (index + 3 >= limit) {
            throw new TBufferUnderflowException();
        }
        return getDataView().getFloat32(index, order == TByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public double getDouble() {
        if (position + 7 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = getDataView().getFloat64(position, order == TByteOrder.LITTLE_ENDIAN);
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
        getDataView().setFloat64(position, value, order == TByteOrder.LITTLE_ENDIAN);
        position += 8;
        return this;
    }

    @Override
    public double getDouble(int index) {
        if (index + 7 >= limit) {
            throw new TBufferUnderflowException();
        }
        return getDataView().getFloat64(index, order == TByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public TByteBuffer putDouble(int index, double value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index + 7 >= limit) {
            throw new TBufferOverflowException();
        }
        getDataView().setFloat64(index, value, order == TByteOrder.LITTLE_ENDIAN);
        return this;
    }

    @Override
    public long getLong() {
        if (position + 7 >= limit) {
            throw new TBufferUnderflowException();
        }
        var result = getDataView().getBigInt64(position, order == TByteOrder.LITTLE_ENDIAN);
        position += 8;
        return result;
    }

    @Override
    public TByteBuffer putLong(long value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 7 >= limit) {
            throw new TBufferOverflowException();
        }
        getDataView().setBigInt64(position, value, order == TByteOrder.LITTLE_ENDIAN);
        position += 8;
        return this;
    }

    @Override
    public long getLong(int index) {
        if (index < 0 || index + 7 >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + (limit - 7) + ")");
        }
        return getDataView().getBigInt64(index, order == TByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public TByteBuffer putLong(int index, long value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 3 >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + (limit - 3) + ")");
        }
        getDataView().setBigInt64(index, value, order == TByteOrder.LITTLE_ENDIAN);
        return this;
    }

    @Override
    public TLongBuffer asLongBuffer() {
        int sz = remaining() / 8;
        if (order == TByteOrder.nativeOrder()) {
            var slice = new BigInt64Array(data.getBuffer(), data.getByteOffset() + position, sz);
            return new TLongBufferOverTypedArray(0, sz, readOnly, slice, null);
        } else {
            var slice = new DataView(data.getBuffer(), data.getByteOffset() + position, sz * 8);
            return new TLongBufferOverDataView(0, sz, readOnly, slice, order == TByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public TFloatBuffer asFloatBuffer() {
        int sz = remaining() / 4;
        if (order == TByteOrder.nativeOrder()) {
            var slice = new Float32Array(data.getBuffer(), data.getByteOffset() + position, sz);
            return new TFloatBufferOverTypedArray(0, sz, readOnly, slice, null);
        } else {
            var slice = new DataView(data.getBuffer(), data.getByteOffset() + position, sz * 4);
            return new TFloatBufferOverDataView(0, sz, readOnly, slice, order == TByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public TDoubleBuffer asDoubleBuffer() {
        int sz = remaining() / 8;
        if (order == TByteOrder.nativeOrder()) {
            var slice = new Float64Array(data.getBuffer(), data.getByteOffset() + position, sz);
            return new TDoubleBufferOverTypedArray(0, sz, readOnly, slice, null);
        } else {
            var slice = new DataView(data.getBuffer(), data.getByteOffset() + position, sz * 8);
            return new TDoubleBufferOverDataView(0, sz, readOnly, slice, order == TByteOrder.LITTLE_ENDIAN);
        }
    }
}
