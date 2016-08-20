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

public final class DataPrimitives {
    private DataPrimitives() {
    }

    public static final DataType BYTE = new DataType() {
        @Override
        public DataValue createValue() {
            return new ByteDataValue();
        }
    };

    public static final DataType SHORT = new DataType() {
        @Override
        public DataValue createValue() {
            return new ShortDataValue();
        }
    };

    public static final DataType INT = new DataType() {
        @Override
        public DataValue createValue() {
            return new IntDataValue();
        }
    };

    public static final DataType LONG = new DataType() {
        @Override
        public DataValue createValue() {
            return new LongDataValue();
        }
    };

    public static final DataType ADDRESS = new DataType() {
        @Override
        public DataValue createValue() {
            return new AddressDataValue();
        }
    };

    public static final DataType FLOAT = new DataType() {
        @Override
        public DataValue createValue() {
            return new FloatDataValue();
        }
    };

    public static final DataType DOUBLE = new DataType() {
        @Override
        public DataValue createValue() {
            return new DoubleDataValue();
        }
    };

    static class ByteDataValue extends DataValue {
        byte value;

        ByteDataValue() {
            super(BYTE);
        }

        @Override
        public byte getByte(int index) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            return value;
        }

        @Override
        public void setByte(int index, byte value) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            this.value = value;
        }
    }

    static class ShortDataValue extends DataValue {
        short value;

        ShortDataValue() {
            super(SHORT);
        }

        @Override
        public short getShort(int index) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            return value;
        }

        @Override
        public void setShort(int index, short value) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            this.value = value;
        }
    }

    static class IntDataValue extends DataValue {
        int value;

        IntDataValue() {
            super(INT);
        }

        @Override
        public int getInt(int index) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            return value;
        }

        @Override
        public void setInt(int index, int value) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            this.value = value;
        }
    }

    static class LongDataValue extends DataValue {
        long value;

        LongDataValue() {
            super(LONG);
        }

        @Override
        public long getLong(int index) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            return value;
        }

        @Override
        public void setLong(int index, long value) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            this.value = value;
        }
    }

    static class AddressDataValue extends DataValue {
        long value;

        AddressDataValue() {
            super(ADDRESS);
        }

        @Override
        public long getAddress(int index) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            return value;
        }

        @Override
        public void setAddress(int index, long value) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            this.value = value;
        }
    }

    static class FloatDataValue extends DataValue {
        float value;

        FloatDataValue() {
            super(FLOAT);
        }

        @Override
        public float getFloat(int index) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            return value;
        }

        @Override
        public void setFloat(int index, float value) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            this.value = value;
        }
    }

    static class DoubleDataValue extends DataValue {
        double value;

        DoubleDataValue() {
            super(DOUBLE);
        }

        @Override
        public double getDouble(int index) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            return value;
        }

        @Override
        public void setDouble(int index, double value) {
            if (index != 0) {
                throw new IllegalArgumentException("Index should be 0 for primitive values");
            }
            this.value = value;
        }
    }
}
