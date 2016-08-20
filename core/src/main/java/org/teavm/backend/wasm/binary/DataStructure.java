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

public class DataStructure extends DataType {
    private byte alignment;
    private DataType[] components;

    public DataStructure(byte alignment, DataType... components) {
        this.alignment = alignment;
        this.components = components.clone();
    }

    public byte getAlignment() {
        return alignment;
    }

    public DataType[] getComponents() {
        return components.clone();
    }

    @Override
    public DataValue createValue() {
        return new StructureValue(this);
    }

    static class StructureValue extends DataValue {
        private DataValue[] components;

        StructureValue(DataStructure type) {
            super(type);
            components = new DataValue[type.components.length];
            for (int i = 0; i < components.length; ++i) {
                components[i] = type.components[i].createValue();
            }
        }

        @Override
        public void setByte(int index, byte value) {
            components[index].setByte(0, value);
        }

        @Override
        public void setShort(int index, short value) {
            components[index].setShort(0, value);
        }

        @Override
        public void setInt(int index, int value) {
            components[index].setInt(0, value);
        }

        @Override
        public void setLong(int index, long value) {
            components[index].setLong(0, value);
        }

        @Override
        public void setAddress(int index, long value) {
            components[index].setAddress(0, value);
        }

        @Override
        public void setFloat(int index, float value) {
            components[index].setFloat(0, value);
        }

        @Override
        public void setDouble(int index, double value) {
            components[index].setDouble(0, value);
        }

        @Override
        public DataValue getValue(int index) {
            return components[index];
        }
    }
}
