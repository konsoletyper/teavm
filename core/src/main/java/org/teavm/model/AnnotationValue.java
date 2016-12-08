/*
 *  Copyright 2013 Alexey Andreev.
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

import java.util.Collections;
import java.util.List;

public class AnnotationValue {
    public static final byte BOOLEAN = 0;
    public static final byte BYTE = 1;
    public static final byte SHORT = 2;
    public static final byte INT = 3;
    public static final byte LONG = 4;
    public static final byte FLOAT = 5;
    public static final byte DOUBLE = 6;
    public static final byte STRING = 7;
    public static final byte CLASS = 8;
    public static final byte LIST = 9;
    public static final byte ENUM = 10;
    public static final byte ANNOTATION = 11;
    private byte type;
    private Object value;

    public AnnotationValue(boolean value) {
        this.type = BOOLEAN;
        this.value = value;
    }

    public AnnotationValue(byte value) {
        this.type = BYTE;
        this.value = value;
    }

    public AnnotationValue(short value) {
        this.type = SHORT;
        this.value = value;
    }

    public AnnotationValue(int value) {
        this.type = INT;
        this.value = value;
    }

    public AnnotationValue(long value) {
        this.type = LONG;
        this.value = value;
    }

    public AnnotationValue(float value) {
        this.type = FLOAT;
        this.value = value;
    }

    public AnnotationValue(double value) {
        this.type = DOUBLE;
        this.value = value;
    }

    public AnnotationValue(String value) {
        this.type = STRING;
        this.value = value;
    }

    public AnnotationValue(ValueType value) {
        this.type = CLASS;
        this.value = value;
    }

    public AnnotationValue(List<AnnotationValue> value) {
        this.type = LIST;
        this.value = value;
    }

    public AnnotationValue(AnnotationReader value) {
        this.type = ANNOTATION;
        this.value = value;
    }

    public AnnotationValue(FieldReference value) {
        this.type = ENUM;
        this.value = value;
    }

    public boolean getBoolean() {
        if (type != BOOLEAN) {
            throw new IllegalStateException("There is no boolean value");
        }
        return (Boolean) value;
    }

    public byte getByte() {
        if (type != BYTE) {
            throw new IllegalStateException("There is no byte value");
        }
        return (Byte) value;
    }

    public short getShort() {
        if (type != SHORT) {
            throw new IllegalStateException("There is no short value");
        }
        return (Short) value;
    }

    public int getInt() {
        if (type != INT) {
            throw new IllegalStateException("There is no int value");
        }
        return (Integer) value;
    }

    public long getLong() {
        if (type != LONG) {
            throw new IllegalStateException("There is no long value");
        }
        return (Long) value;
    }

    public float getFloat() {
        if (type != FLOAT) {
            throw new IllegalStateException("There is no float value");
        }
        return (Float) value;
    }

    public double getDouble() {
        if (type != DOUBLE) {
            throw new IllegalStateException("There is no double value");
        }
        return (Double) value;
    }

    public String getString() {
        if (type != STRING) {
            throw new IllegalStateException("There is no String value");
        }
        return (String) value;
    }

    public ValueType getJavaClass() {
        if (type != CLASS) {
            throw new IllegalStateException("There is no ValueType value");
        }
        return (ValueType) value;
    }

    @SuppressWarnings("unchecked")
    public List<AnnotationValue> getList() {
        if (type != LIST) {
            throw new IllegalStateException("There is no List value");
        }
        return Collections.unmodifiableList((List<AnnotationValue>) value);
    }

    public FieldReference getEnumValue() {
        if (type != ENUM) {
            throw new IllegalStateException("There is no enum value");
        }
        return (FieldReference) value;
    }

    public AnnotationReader getAnnotation() {
        if (type != ANNOTATION) {
            throw new IllegalStateException("There is no annotation value");
        }
        return (AnnotationReader) value;
    }

    public byte getType() {
        return type;
    }
}
