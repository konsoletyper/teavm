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

public class DataArray extends DataType {
    private DataType componentType;
    private int size;

    public DataArray(DataType componentType, int size) {
        this.componentType = componentType;
        this.size = size;
    }

    public DataType getComponentType() {
        return componentType;
    }

    public int getSize() {
        return size;
    }

    @Override
    public DataValue createValue() {
        if (componentType == DataPrimitives.BYTE) {
            return new ByteArrayValue(this);
        } else if (componentType == DataPrimitives.SHORT) {
            return new ShortArrayValue(this);
        } else if (componentType == DataPrimitives.INT) {
            return new IntArrayValue(this);
        } else if (componentType == DataPrimitives.LONG) {
            return new LongArrayValue(this);
        } else if (componentType == DataPrimitives.ADDRESS) {
            return new AddressArrayValue(this);
        } else if (componentType == DataPrimitives.FLOAT) {
            return new FloatArrayValue(this);
        } else if (componentType == DataPrimitives.DOUBLE) {
            return new DoubleArrayValue(this);
        }

        return new ArrayValue(this);
    }

    static class ByteArrayValue extends DataValue {
        byte[] data;

        ByteArrayValue(DataArray type) {
            super(type);
            data = new byte[type.size];
        }

        @Override
        public byte getByte(int index) {
            return data[index];
        }

        @Override
        public void setByte(int index, byte value) {
            data[index] = value;
        }

        @Override
        public DataValue getValue(int index) {
            int outerIndex = index;
            return new DataValue(DataPrimitives.BYTE) {
                @Override
                public byte getByte(int index) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    return data[outerIndex];
                }

                @Override
                public void setByte(int index, byte value) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    data[outerIndex] = value;
                }
            };
        }
    }

    static class ShortArrayValue extends DataValue {
        short[] data;

        ShortArrayValue(DataArray type) {
            super(type);
            data = new short[type.size];
        }

        @Override
        public short getShort(int index) {
            return data[index];
        }

        @Override
        public void setShort(int index, short value) {
            data[index] = value;
        }

        @Override
        public DataValue getValue(int index) {
            int outerIndex = index;
            return new DataValue(DataPrimitives.SHORT) {
                @Override
                public short getShort(int index) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    return data[outerIndex];
                }

                @Override
                public void setShort(int index, short value) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    data[outerIndex] = value;
                }
            };
        }
    }

    static class IntArrayValue extends DataValue {
        int[] data;

        IntArrayValue(DataArray type) {
            super(type);
            data = new int[type.size];
        }

        @Override
        public int getInt(int index) {
            return data[index];
        }

        @Override
        public void setInt(int index, int value) {
            data[index] = value;
        }

        @Override
        public DataValue getValue(int index) {
            int outerIndex = index;
            return new DataValue(DataPrimitives.INT) {
                @Override
                public int getInt(int index) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    return data[outerIndex];
                }

                @Override
                public void setInt(int index, int value) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    data[outerIndex] = value;
                }
            };
        }
    }

    static class LongArrayValue extends DataValue {
        long[] data;

        LongArrayValue(DataArray type) {
            super(type);
            data = new long[type.size];
        }

        @Override
        public long getLong(int index) {
            return data[index];
        }

        @Override
        public void setLong(int index, long value) {
            data[index] = value;
        }

        @Override
        public DataValue getValue(int index) {
            int outerIndex = index;
            return new DataValue(DataPrimitives.LONG) {
                @Override
                public long getLong(int index) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    return data[outerIndex];
                }

                @Override
                public void setLong(int index, long value) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    data[outerIndex] = value;
                }
            };
        }
    }

    static class AddressArrayValue extends DataValue {
        long[] data;

        AddressArrayValue(DataArray type) {
            super(type);
            data = new long[type.size];
        }

        @Override
        public long getAddress(int index) {
            return data[index];
        }

        @Override
        public void setAddress(int index, long value) {
            data[index] = value;
        }

        @Override
        public DataValue getValue(int index) {
            int outerIndex = index;
            return new DataValue(DataPrimitives.ADDRESS) {
                @Override
                public long getAddress(int index) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    return data[outerIndex];
                }

                @Override
                public void setAddress(int index, long value) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    data[outerIndex] = value;
                }
            };
        }
    }

    static class FloatArrayValue extends DataValue {
        float[] data;

        FloatArrayValue(DataArray type) {
            super(type);
            data = new float[type.size];
        }

        @Override
        public float getFloat(int index) {
            return data[index];
        }

        @Override
        public void setFloat(int index, float value) {
            data[index] = value;
        }

        @Override
        public DataValue getValue(int index) {
            int outerIndex = index;
            return new DataValue(DataPrimitives.FLOAT) {
                @Override
                public float getFloat(int index) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    return data[outerIndex];
                }

                @Override
                public void setFloat(int index, float value) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    data[outerIndex] = value;
                }
            };
        }
    }

    static class DoubleArrayValue extends DataValue {
        double[] data;

        DoubleArrayValue(DataArray type) {
            super(type);
            data = new double[type.size];
        }

        @Override
        public double getDouble(int index) {
            return data[index];
        }

        @Override
        public void setDouble(int index, double value) {
            data[index] = value;
        }

        @Override
        public DataValue getValue(int index) {
            int outerIndex = index;
            return new DataValue(DataPrimitives.DOUBLE) {
                @Override
                public double getDouble(int index) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    return data[outerIndex];
                }

                @Override
                public void setDouble(int index, double value) {
                    if (index != 0) {
                        throw new IllegalArgumentException("Index should be 0");
                    }
                    data[outerIndex] = value;
                }
            };
        }
    }

    static class ArrayValue extends DataValue {
        DataValue[] data;

        ArrayValue(DataArray type) {
            super(type);
            data = new DataValue[type.size];
            for (int i = 0; i < data.length; ++i) {
                data[i] = type.componentType.createValue();
            }
        }

        @Override
        public byte getByte(int index) {
            return data[index].getByte(0);
        }

        @Override
        public void setByte(int index, byte value) {
            data[index].setByte(0, value);
        }

        @Override
        public short getShort(int index) {
            return data[index].getShort(0);
        }

        @Override
        public void setShort(int index, short value) {
            data[index].setShort(0, value);
        }

        @Override
        public int getInt(int index) {
            return data[index].getInt(0);
        }

        @Override
        public void setInt(int index, int value) {
            data[index].setInt(0, value);
        }

        @Override
        public long getLong(int index) {
            return data[index].getLong(0);
        }

        @Override
        public void setLong(int index, long value) {
            data[index].setLong(0, value);
        }

        @Override
        public long getAddress(int index) {
            return data[index].getAddress(0);
        }

        @Override
        public void setAddress(int index, long value) {
            data[index].setAddress(0, value);
        }

        @Override
        public float getFloat(int index) {
            return data[index].getFloat(0);
        }

        @Override
        public void setFloat(int index, float value) {
            data[index].setFloat(0, value);
        }

        @Override
        public double getDouble(int index) {
            return data[index].getDouble(0);
        }

        @Override
        public void setDouble(int index, double value) {
            data[index].setDouble(0, value);
        }

        @Override
        public DataValue getValue(int index) {
            return data[index];
        }
    }
}
