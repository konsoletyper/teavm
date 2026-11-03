/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.compiler.bootstrap.extension;

import org.teavm.extension.bridge.BridgeElement;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeValue;
import org.teavm.extension.introspect.IntrospectAnnotation;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;

class HostValue implements BridgeValue {
    private static final HostValue[] NO_ITEMS = new HostValue[0];

    private int kind;
    private double number;
    private String string;
    private HostElement element;
    private int itemKind;
    private HostValue[] items = NO_ITEMS;

    private HostValue(int kind) {
        this.kind = kind;
    }

    static HostValue of(HostEnvironment env, Object value) {
        if (value instanceof Boolean) {
            return number(BridgeKinds.VALUE_BOOLEAN, (Boolean) value ? 1 : 0);
        } else if (value instanceof Byte) {
            return number(BridgeKinds.VALUE_BYTE, (Byte) value);
        } else if (value instanceof Short) {
            return number(BridgeKinds.VALUE_SHORT, (Short) value);
        } else if (value instanceof Character) {
            return number(BridgeKinds.VALUE_CHAR, (Character) value);
        } else if (value instanceof Integer) {
            return number(BridgeKinds.VALUE_INT, (Integer) value);
        } else if (value instanceof Long) {
            var result = new HostValue(BridgeKinds.VALUE_LONG);
            result.string = value.toString();
            return result;
        } else if (value instanceof Float) {
            return number(BridgeKinds.VALUE_FLOAT, (Float) value);
        } else if (value instanceof Double) {
            return number(BridgeKinds.VALUE_DOUBLE, (Double) value);
        } else if (value instanceof String) {
            var result = new HostValue(BridgeKinds.VALUE_STRING);
            result.string = (String) value;
            return result;
        } else if (value instanceof IntrospectClass<?>) {
            var result = new HostValue(BridgeKinds.VALUE_CLASS);
            result.element = env.cls((IntrospectClass<?>) value);
            return result;
        } else if (value instanceof IntrospectField) {
            var result = new HostValue(BridgeKinds.VALUE_ENUM);
            result.element = env.field((IntrospectField) value);
            return result;
        } else if (value instanceof IntrospectAnnotation<?>) {
            var result = new HostValue(BridgeKinds.VALUE_ANNOTATION);
            result.element = env.annotation((IntrospectAnnotation<?>) value);
            return result;
        } else {
            return array(env, value);
        }
    }

    private static HostValue number(int kind, double value) {
        var result = new HostValue(kind);
        result.number = value;
        return result;
    }

    private static HostValue array(HostEnvironment env, Object value) {
        var result = new HostValue(BridgeKinds.VALUE_ARRAY);
        if (value instanceof boolean[]) {
            var array = (boolean[]) value;
            result.itemKind = BridgeKinds.VALUE_BOOLEAN;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = number(BridgeKinds.VALUE_BOOLEAN, array[i] ? 1 : 0);
            }
        } else if (value instanceof byte[]) {
            var array = (byte[]) value;
            result.itemKind = BridgeKinds.VALUE_BYTE;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = number(BridgeKinds.VALUE_BYTE, array[i]);
            }
        } else if (value instanceof short[]) {
            var array = (short[]) value;
            result.itemKind = BridgeKinds.VALUE_SHORT;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = number(BridgeKinds.VALUE_SHORT, array[i]);
            }
        } else if (value instanceof char[]) {
            var array = (char[]) value;
            result.itemKind = BridgeKinds.VALUE_CHAR;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = number(BridgeKinds.VALUE_CHAR, array[i]);
            }
        } else if (value instanceof int[]) {
            var array = (int[]) value;
            result.itemKind = BridgeKinds.VALUE_INT;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = number(BridgeKinds.VALUE_INT, array[i]);
            }
        } else if (value instanceof long[]) {
            var array = (long[]) value;
            result.itemKind = BridgeKinds.VALUE_LONG;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = of(env, array[i]);
            }
        } else if (value instanceof float[]) {
            var array = (float[]) value;
            result.itemKind = BridgeKinds.VALUE_FLOAT;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = number(BridgeKinds.VALUE_FLOAT, array[i]);
            }
        } else if (value instanceof double[]) {
            var array = (double[]) value;
            result.itemKind = BridgeKinds.VALUE_DOUBLE;
            result.items = new HostValue[array.length];
            for (var i = 0; i < array.length; ++i) {
                result.items[i] = number(BridgeKinds.VALUE_DOUBLE, array[i]);
            }
        } else if (value instanceof String[]) {
            result.itemKind = BridgeKinds.VALUE_STRING;
            result.items = objectItems(env, (Object[]) value);
        } else if (value instanceof IntrospectClass<?>[]) {
            result.itemKind = BridgeKinds.VALUE_CLASS;
            result.items = objectItems(env, (Object[]) value);
        } else if (value instanceof IntrospectField[]) {
            result.itemKind = BridgeKinds.VALUE_ENUM;
            result.items = objectItems(env, (Object[]) value);
        } else if (value instanceof IntrospectAnnotation<?>[]) {
            result.itemKind = BridgeKinds.VALUE_ANNOTATION;
            result.items = objectItems(env, (Object[]) value);
        } else {
            throw new IllegalArgumentException("Unsupported annotation value: " + value);
        }
        return result;
    }

    private static HostValue[] objectItems(HostEnvironment env, Object[] array) {
        var result = new HostValue[array.length];
        for (var i = 0; i < array.length; ++i) {
            result[i] = of(env, array[i]);
        }
        return result;
    }

    @Override
    public int kind() {
        return kind;
    }

    @Override
    public double number() {
        return number;
    }

    @Override
    public String string() {
        return string;
    }

    @Override
    public BridgeElement element() {
        return element;
    }

    @Override
    public int itemKind() {
        return itemKind;
    }

    @Override
    public BridgeValue[] items() {
        return items;
    }
}
