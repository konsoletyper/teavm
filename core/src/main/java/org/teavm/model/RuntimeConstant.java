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
package org.teavm.model;

public final class RuntimeConstant {
    public static final byte INT = 0;
    public static final byte LONG = 1;
    public static final byte FLOAT = 2;
    public static final byte DOUBLE = 3;
    public static final byte STRING = 4;
    public static final byte TYPE = 5;
    public static final byte METHOD = 6;
    public static final byte METHOD_HANDLE = 7;

    private byte kind;
    private Object value;

    RuntimeConstant(byte kind, Object value) {
        super();
        this.kind = kind;
        this.value = value;
    }

    public RuntimeConstant(int value) {
        this(INT, value);
    }

    public RuntimeConstant(long value) {
        this(LONG, value);
    }

    public RuntimeConstant(float value) {
        this(FLOAT, value);
    }

    public RuntimeConstant(double value) {
        this(FLOAT, value);
    }

    public RuntimeConstant(String value) {
        this(STRING, value);
    }

    public RuntimeConstant(ValueType value) {
        this(TYPE, value);
    }

    public RuntimeConstant(ValueType[] methodType) {
        this(METHOD, methodType.clone());
    }

    public RuntimeConstant(MethodHandle value) {
        this(METHOD_HANDLE, value);
    }

    public byte getKind() {
        return kind;
    }

    public int getInt() {
        return (Integer) value;
    }

    public long getLong() {
        return (Long) value;
    }

    public float getFloat() {
        return (Float) value;
    }

    public double getDouble() {
        return (Double) value;
    }

    public String getString() {
        return (String) value;
    }

    public ValueType getValueType() {
        return (ValueType) value;
    }

    public ValueType[] getMethodType() {
        return ((ValueType[]) value).clone();
    }

    public MethodHandle getMethodHandle() {
        return (MethodHandle) value;
    }
}
