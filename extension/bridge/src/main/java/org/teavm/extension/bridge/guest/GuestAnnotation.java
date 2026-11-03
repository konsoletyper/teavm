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
package org.teavm.extension.bridge.guest;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import org.teavm.extension.bridge.BridgeAnnotation;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeValue;
import org.teavm.extension.introspect.IntrospectAnnotation;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectMethod;

class GuestAnnotation<T extends Annotation> extends GuestElement implements IntrospectAnnotation<T> {
    private final BridgeAnnotation remoteAnnotation;
    private IntrospectClass<T> type;
    private Map<IntrospectMethod, Object> values = new HashMap<>();

    GuestAnnotation(GuestEnvironment env, BridgeAnnotation remote) {
        super(env, remote);
        remoteAnnotation = remote;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IntrospectClass<T> type() {
        if (type == null) {
            type = (IntrospectClass<T>) env.element(remoteAnnotation.type());
        }
        return type;
    }

    @Override
    public Object value(IntrospectMethod method) {
        if (method.declaringClass() != type()) {
            throw new IllegalArgumentException("Method " + method + " is not declared by " + type());
        }
        var result = values.get(method);
        if (result == null) {
            result = convertValue(remoteAnnotation.value(((GuestMethod) method).remoteMethod));
            values.put(method, result);
        }
        return result;
    }

    @Override
    public Object value(String attributeName) {
        var method = type().declaredMethod(attributeName);
        if (method == null) {
            throw new IllegalArgumentException("Annotation " + type().name() + " does not have attribute "
                    + attributeName);
        }
        return value(method);
    }

    private Object convertValue(BridgeValue value) {
        switch (value.kind()) {
            case BridgeKinds.VALUE_BOOLEAN:
                return value.number() != 0;
            case BridgeKinds.VALUE_BYTE:
                return (byte) value.number();
            case BridgeKinds.VALUE_SHORT:
                return (short) value.number();
            case BridgeKinds.VALUE_CHAR:
                return (char) value.number();
            case BridgeKinds.VALUE_INT:
                return (int) value.number();
            case BridgeKinds.VALUE_LONG:
                return Long.parseLong(value.string());
            case BridgeKinds.VALUE_FLOAT:
                return (float) value.number();
            case BridgeKinds.VALUE_DOUBLE:
                return value.number();
            case BridgeKinds.VALUE_STRING:
                return value.string();
            case BridgeKinds.VALUE_CLASS:
            case BridgeKinds.VALUE_ENUM:
            case BridgeKinds.VALUE_ANNOTATION:
                return env.element(value.element());
            case BridgeKinds.VALUE_ARRAY:
                return convertArray(value.itemKind(), value.items());
            default:
                throw new IllegalStateException("Unexpected value kind: " + value.kind());
        }
    }

    private Object convertArray(int itemKind, BridgeValue[] items) {
        switch (itemKind) {
            case BridgeKinds.VALUE_BOOLEAN: {
                var result = new boolean[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = items[i].number() != 0;
                }
                return result;
            }
            case BridgeKinds.VALUE_BYTE: {
                var result = new byte[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = (byte) items[i].number();
                }
                return result;
            }
            case BridgeKinds.VALUE_SHORT: {
                var result = new short[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = (short) items[i].number();
                }
                return result;
            }
            case BridgeKinds.VALUE_CHAR: {
                var result = new char[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = (char) items[i].number();
                }
                return result;
            }
            case BridgeKinds.VALUE_INT: {
                var result = new int[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = (int) items[i].number();
                }
                return result;
            }
            case BridgeKinds.VALUE_LONG: {
                var result = new long[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = Long.parseLong(items[i].string());
                }
                return result;
            }
            case BridgeKinds.VALUE_FLOAT: {
                var result = new float[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = (float) items[i].number();
                }
                return result;
            }
            case BridgeKinds.VALUE_DOUBLE: {
                var result = new double[items.length];
                for (var i = 0; i < items.length; ++i) {
                    result[i] = items[i].number();
                }
                return result;
            }
            case BridgeKinds.VALUE_STRING:
                return fillArray(new String[items.length], items);
            case BridgeKinds.VALUE_CLASS:
                return fillArray(new IntrospectClass<?>[items.length], items);
            case BridgeKinds.VALUE_ENUM:
                return fillArray(new IntrospectField[items.length], items);
            case BridgeKinds.VALUE_ANNOTATION:
                return fillArray(new IntrospectAnnotation<?>[items.length], items);
            default:
                throw new IllegalStateException("Unexpected array item kind: " + itemKind);
        }
    }

    private Object[] fillArray(Object[] array, BridgeValue[] items) {
        for (var i = 0; i < items.length; ++i) {
            array[i] = convertValue(items[i]);
        }
        return array;
    }
}
