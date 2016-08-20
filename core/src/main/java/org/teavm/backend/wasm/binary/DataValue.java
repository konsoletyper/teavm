/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.binary;

public abstract class DataValue {
    private DataType type;

    DataValue(DataType type) {
        this.type = type;
    }

    public DataType getType() {
        return type;
    }

    public byte getByte(int index) {
        throw new UnsupportedOperationException();
    }

    public void setByte(int index, byte value) {
        throw new UnsupportedOperationException();
    }

    public short getShort(int index) {
        throw new UnsupportedOperationException();
    }

    public void setShort(int index, short value) {
        throw new UnsupportedOperationException();
    }

    public int getInt(int index) {
        throw new UnsupportedOperationException();
    }

    public void setInt(int index, int value) {
        throw new UnsupportedOperationException();
    }

    public long getLong(int index) {
        throw new UnsupportedOperationException();
    }

    public void setLong(int index, long value) {
        throw new UnsupportedOperationException();
    }

    public long getAddress(int index) {
        throw new UnsupportedOperationException();
    }

    public void setAddress(int index, long value) {
        throw new UnsupportedOperationException();
    }

    public float getFloat(int index) {
        throw new UnsupportedOperationException();
    }

    public void setFloat(int index, float value) {
        throw new UnsupportedOperationException();
    }

    public double getDouble(int index) {
        throw new UnsupportedOperationException();
    }

    public void setDouble(int index, double value) {
        throw new UnsupportedOperationException();
    }

    public DataValue getValue(int index) {
        throw new UnsupportedOperationException();
    }
}

