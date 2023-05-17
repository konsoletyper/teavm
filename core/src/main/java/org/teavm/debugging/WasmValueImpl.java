/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.debugging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.backend.wasm.debug.info.ArrayLayout;
import org.teavm.backend.wasm.debug.info.ClassLayout;
import org.teavm.backend.wasm.debug.info.DebugInfo;
import org.teavm.backend.wasm.debug.info.FieldType;
import org.teavm.backend.wasm.debug.info.InterfaceLayout;
import org.teavm.backend.wasm.debug.info.PrimitiveLayout;
import org.teavm.backend.wasm.debug.info.TypeLayout;
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.model.PrimitiveType;

class WasmValueImpl extends Value {
    private static final String CLASS_PROP = "__class";
    private static final String ADDRESS_PROP = "__address";
    private DebugInfo debugInfo;
    private FieldType type;
    private JavaScriptCallFrame callFrame;
    private long longValue;
    private Promise<TypeLayout> calculatedType;

    WasmValueImpl(Debugger debugger, DebugInfo debugInfo, FieldType type, JavaScriptCallFrame callFrame,
            long longValue) {
        super(debugger);
        this.debugInfo = debugInfo;
        this.type = type;
        this.callFrame = callFrame;
        this.longValue = longValue;
    }

    @Override
    public Promise<String> getRepresentation() {
        switch (type) {
            case BOOLEAN:
                return Promise.of(longValue != 0 ? "true" : "false");
            case BYTE:
                return Promise.of(Byte.toString((byte) longValue));
            case SHORT:
                return Promise.of(Short.toString((short) longValue));
            case CHAR: {
                var sb = new StringBuilder("'");
                appendChar(sb, (char) longValue);
                sb.append("'");
                return Promise.of(sb.toString());
            }
            case INT:
                return Promise.of(Integer.toString((int) longValue));
            case LONG:
                return Promise.of(Long.toString(longValue));
            case FLOAT:
                return Promise.of(Float.toString(Float.intBitsToFloat((int) longValue)));
            case DOUBLE:
                return Promise.of(Double.toString(Double.longBitsToDouble(longValue)));
            case OBJECT:
                return buildObjectRepresentation();
            case ADDRESS:
                return Promise.of("0x" + Integer.toHexString((int) longValue));
            default:
                return Promise.of("undefined");
        }
    }

    private void appendChar(StringBuilder sb, char c) {
        switch (c) {
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\'':
                sb.append("\\\'");
                break;
            case '\"':
                sb.append("\\\"");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            default:
                if (c < 32) {
                    sb.append("\\u00").append(Character.forDigit(c / 16, 16))
                            .append(Character.forDigit(c % 16, 16));
                } else {
                    sb.append(c);
                }
                break;
        }
    }

    private Promise<String> buildObjectRepresentation() {
        if (longValue == 0) {
            return Promise.of("null");
        }
        return getCalculatedType().thenAsync(cls -> {
            if (cls == null) {
                return Promise.of("error");
            }
            return typeRepresentation(cls, (int) longValue);
        });
    }

    private Promise<String> typeRepresentation(TypeLayout type, int address) {
        switch (type.kind()) {
            case CLASS:
                return objectRepresentation((ClassLayout) type, address);
            case ARRAY:
                return arrayRepresentation((ArrayLayout) type, address);
            default:
                break;
        }
        return Promise.of(classToString(type));
    }

    private Promise<String> objectRepresentation(ClassLayout cls, int address) {
        if (cls.classRef().fullName().equals("java.lang.String")) {
            var stringRepr = decodeString(cls, address);
            if (stringRepr != null) {
                return stringRepr.then(result -> result != null ? result : classToString(cls));
            }
        } else if (cls.classRef().fullName().equals("java.lang.Class")) {
            var stringRepr = decodeClass(address);
            if (stringRepr != null) {
                return Promise.of(stringRepr);
            }
        }
        return Promise.of(classToString(cls));
    }

    private Promise<String> arrayRepresentation(ArrayLayout arrayType, int address) {
        return callFrame.getMemory(address + 8, 4).then(data -> {
            if (data == null) {
                return classToString(arrayType);
            }
            var length = readInt(data, 0);
            return classToString(arrayType.elementType()) + "[" + length + "]";
        });
    }

    private Promise<String> decodeString(ClassLayout cls, int address) {
        for (var field : cls.instanceFields()) {
            if (field.name().equals("characters") && field.type() == FieldType.OBJECT) {
                return callFrame.getMemory(address + field.address(), 4).thenAsync(data -> {
                    var charsAddress = readInt(data, 0);
                    return decodeChars(charsAddress);
                });
            }
        }
        return null;
    }

    private Promise<String> decodeChars(int address) {
        return callFrame.getMemory(address, 12).thenAsync(data -> {
            if (data == null) {
                return null;
            }
            var classPtr = readInt(data, 0) << 3;
            var type = debugInfo.classLayoutInfo().find(classPtr);
            if (!(type instanceof ArrayLayout)) {
                return null;
            }
            var elementType = ((ArrayLayout) type).elementType();
            if (!(elementType instanceof PrimitiveLayout)) {
                return null;
            }
            var primitiveType = ((PrimitiveLayout) elementType).primitiveType();
            if (primitiveType != PrimitiveType.CHARACTER) {
                return null;
            }
            var length = readInt(data, 8);
            return callFrame.getMemory(address + 12, length * 2).then(charsData -> {
                if (charsData == null) {
                    return null;
                }
                var sb = new StringBuilder("\"");
                for (var i = 0; i < length; ++i) {
                    appendChar(sb, (char) readShort(charsData, i * 2));
                }
                sb.append("\"");
                return sb.toString();
            });
        });
    }

    private String decodeClass(int address) {
        var type = debugInfo.classLayoutInfo().find(address);
        return type != null ? classToString(type) : null;
    }

    @Override
    Promise<String> prepareType() {
        switch (type) {
            case BOOLEAN:
                return Promise.of("boolean");
            case BYTE:
                return Promise.of("byte");
            case SHORT:
                return Promise.of("short");
            case CHAR:
                return Promise.of("char");
            case INT:
                return Promise.of("int");
            case LONG:
                return Promise.of("long");
            case FLOAT:
                return Promise.of("float");
            case DOUBLE:
                return Promise.of("double");
            case ADDRESS:
                return Promise.of("address");
            case OBJECT:
                return fetchObjectType();
            default:
                return Promise.of("undefined");
        }
    }

    private Promise<TypeLayout> getCalculatedType() {
        if (calculatedType == null) {
            calculatedType = callFrame.getMemory((int) longValue, 4).then(data -> {
                if (data == null) {
                    return null;
                }
                var header = readInt(data, 0);
                var classPtr = header << 3;
                var classes = debugInfo.classLayoutInfo();
                if (classes == null) {
                    return null;
                }
                return classes.find(classPtr);
            });
        }
        return calculatedType;
    }

    private Promise<String> fetchObjectType() {
        if (longValue == 0) {
            return Promise.of("null");
        }
        return getCalculatedType().then(cls -> {
            if (cls == null) {
                return "error";
            }
            return classToString(cls);
        });
    }

    private String classToString(TypeLayout type) {
        switch (type.kind()) {
            case PRIMITIVE:
                switch (((PrimitiveLayout) type).primitiveType()) {
                    case BOOLEAN:
                        return "boolean";
                    case BYTE:
                        return "byte";
                    case SHORT:
                        return "short";
                    case CHARACTER:
                        return "char";
                    case INTEGER:
                        return "int";
                    case LONG:
                        return "long";
                    case FLOAT:
                        return "float";
                    case DOUBLE:
                        return "double";
                    default:
                        break;
                }
                break;
            case CLASS:
                return ((ClassLayout) type).classRef().fullName();
            case INTERFACE:
                return ((InterfaceLayout) type).classRef().fullName();
            case ARRAY:
                return classToString(((ArrayLayout) type).elementType()) + "[]";
            default:
                break;
        }
        return "unknown";
    }

    @Override
    Promise<Map<String, Variable>> prepareProperties() {
        return getCalculatedType().thenAsync(cls -> {
            if (cls != null) {
                switch (cls.kind()) {
                    case CLASS:
                        return fetchObjectProperties((ClassLayout) cls);
                    case ARRAY:
                        return fetchArrayProperties((ArrayLayout) cls);
                    default:
                        break;
                }
            }
            return Promise.of(Collections.emptyMap());
        });
    }

    private Promise<Map<String, Variable>> fetchObjectProperties(ClassLayout cls) {
        if (longValue == 0) {
            return Promise.of(Collections.emptyMap());
        }
        return callFrame.getMemory((int) longValue, cls.size()).then(data -> {
            if (data == null) {
                return Collections.emptyMap();
            }
            var properties = new LinkedHashMap<String, Variable>();
            var ancestorCls = cls;
            while (ancestorCls != null) {
                for (var field : ancestorCls.instanceFields()) {
                    long longValue;
                    switch (field.type()) {
                        case BOOLEAN:
                        case BYTE:
                            longValue = data[field.address()];
                            break;
                        case SHORT:
                        case CHAR:
                            longValue = readShort(data, field.address());
                            break;
                        case INT:
                        case FLOAT:
                        case ADDRESS:
                        case OBJECT:
                            longValue = readInt(data, field.address());
                            break;
                        case LONG:
                        case DOUBLE:
                            longValue = readLong(data, field.address());
                            break;
                        default:
                            longValue = 0;
                            break;
                    }
                    var value = new WasmValueImpl(debugger, debugInfo, field.type(), callFrame, longValue);
                    properties.put(field.name(), new Variable(field.name(), value));
                }
                ancestorCls = ancestorCls.superclass();
            }
            addCommonProperties(properties, cls);
            return properties;
        });
    }

    private Promise<Map<String, Variable>> fetchArrayProperties(ArrayLayout type) {
        if (longValue == 0) {
            return Promise.of(Collections.emptyMap());
        }
        return callFrame.getMemory((int) longValue + 8, 4).thenAsync(data -> {
            if (data == null) {
                return Promise.of(Collections.emptyMap());
            }
            var length = readInt(data, 0);
            var offset = 12;
            int itemSize;
            FieldType elementType;
            if (type.elementType() instanceof PrimitiveLayout) {
                switch (((PrimitiveLayout) type.elementType()).primitiveType()) {
                    case BOOLEAN:
                        elementType = FieldType.BOOLEAN;
                        itemSize = 0;
                        break;
                    case BYTE:
                        elementType = FieldType.BYTE;
                        itemSize = 0;
                        break;
                    case SHORT:
                        elementType = FieldType.SHORT;
                        itemSize = 1;
                        break;
                    case CHARACTER:
                        elementType = FieldType.CHAR;
                        itemSize = 1;
                        break;
                    case INTEGER:
                        elementType = FieldType.INT;
                        itemSize = 2;
                        break;
                    case LONG:
                        elementType = FieldType.LONG;
                        itemSize = 3;
                        break;
                    case FLOAT:
                        elementType = FieldType.FLOAT;
                        itemSize = 2;
                        break;
                    case DOUBLE:
                        elementType = FieldType.DOUBLE;
                        offset = 16;
                        itemSize = 3;
                        break;
                    default:
                        itemSize = 1;
                        elementType = FieldType.UNDEFINED;
                        break;
                }
            } else {
                elementType = FieldType.OBJECT;
                itemSize = 2;
            }
            return callFrame.getMemory((int) longValue + offset, length << itemSize).then(arrayData -> {
                var properties = new LinkedHashMap<String, Variable>();
                for (var i = 0; i < length; ++i) {
                    var name = String.valueOf(i);
                    long longValue;
                    switch (itemSize) {
                        case 0:
                            longValue = arrayData[i];
                            break;
                        case 1:
                            longValue = readShort(arrayData, i * 2);
                            break;
                        case 2:
                            longValue = readInt(arrayData, i * 4);
                            break;
                        default:
                            longValue = readLong(arrayData, i * 8);
                            break;
                    }
                    var value = new WasmValueImpl(debugger, debugInfo, elementType, callFrame, longValue);
                    properties.put(name, new Variable(name, value));
                }
                properties.put("length", new Variable("length", new WasmValueImpl(debugger, debugInfo,
                        FieldType.INT, callFrame, length)));
                addCommonProperties(properties, type);
                return properties;
            });
        });
    }

    private void addCommonProperties(Map<String, Variable> properties, TypeLayout cls) {
        properties.put(CLASS_PROP, new Variable(CLASS_PROP, new WasmValueImpl(debugger, debugInfo,
                FieldType.OBJECT, callFrame, cls.address())));
        properties.put(ADDRESS_PROP, new Variable(ADDRESS_PROP, new WasmValueImpl(debugger, debugInfo,
                FieldType.ADDRESS, callFrame, longValue)));
    }

    private int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);
    }

    private int readShort(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private long readLong(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)
                | ((data[offset + 4] & 0xFFL) << 32) | ((data[offset + 5] & 0xFF) << 40)
                | ((data[offset + 6] & 0xFFL) << 48) | ((data[offset + 7] & 0xFF) << 56);
    }

    @Override
    public Promise<Boolean> hasInnerStructure() {
        return Promise.of(type == FieldType.OBJECT && longValue != 0);
    }

    @Override
    public Promise<String> getInstanceId() {
        return Promise.of(null);
    }

    @Override
    public JavaScriptValue getOriginalValue() {
        return null;
    }
}