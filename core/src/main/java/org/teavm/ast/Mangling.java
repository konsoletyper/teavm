/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.ast;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public final class Mangling {
    private Mangling() {
    }

    public static String mangleIsSupertype(ValueType type) {
        return "isSupertype$" + mangleType(type);
    }

    public static String mangleMethod(MethodReference method) {
        String className = mangleClassBase(method.getClassName());
        StringBuilder sb = new StringBuilder("method$" + className + "_");
        mangleSignature(method.getDescriptor(), sb);
        return sb.toString();
    }

    public static String mangleVTableEntry(MethodDescriptor method) {
        StringBuilder sb = new StringBuilder("m_");
        mangleSignature(method, sb);
        return sb.toString();
    }

    private static void mangleSignature(MethodDescriptor method, StringBuilder sb) {
        sb.append(mangleType(method.getResultType()));
        sb.append(mangleString(method.getName()));
        sb.append(Arrays.stream(method.getParameterTypes())
                .map(Mangling::mangleType)
                .collect(Collectors.joining()));
    }

    public static String mangleField(FieldReference field) {
        String className = mangleClassBase(field.getClassName());
        return "field$" + className + "_" + field.getFieldName().length() + mangleString(field.getFieldName());
    }

    public static String mangleClass(String className) {
        return "class$" + mangleClassBase(className);
    }

    public static String mangleClassObject(ValueType type) {
        return "tobject$" + mangleType(type);
    }

    public static String mangleVtable(String className) {
        return "vt$" + mangleClassBase(className);
    }

    private static String mangleClassBase(String className) {
        return className.length() + mangleString(className);
    }

    public static String mangleInitializer(String className) {
        return "clinit$" + mangleString(className);
    }

    public static String mangleInstanceOf(ValueType type) {
        return "instanceof$" + mangleType(type);
    }

    private static String mangleString(String string) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            switch (c) {
                case '$':
                    sb.append(c);
                    break;
                case '.':
                    sb.append("_g");
                    break;
                case '<':
                    sb.append("_h");
                    break;
                case '>':
                    sb.append("_i");
                    break;
                case '_':
                    sb.append("__");
                    break;
                default:
                    if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                        sb.append(c);
                    } else {
                        sb.append('_')
                                .append(Character.forDigit(c >>> 12, 16))
                                .append(Character.forDigit((c >>> 8) & 0xF, 16))
                                .append(Character.forDigit((c >>> 4) & 0xF, 16))
                                .append(Character.forDigit(c & 0xF, 16));
                    }
                    break;
            }
        }
        return sb.toString();
    }

    public static String mangleType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return "Z";
                case BYTE:
                    return "B";
                case SHORT:
                    return "S";
                case CHARACTER:
                    return "C";
                case INTEGER:
                    return "I";
                case LONG:
                    return "L";
                case FLOAT:
                    return "F";
                case DOUBLE:
                    return "D";
            }
        } else if (type instanceof ValueType.Void) {
            return "V";
        } else if (type instanceof ValueType.Array) {
            return "A" + mangleType(((ValueType.Array) type).getItemType());
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            return className.length() + "_" + mangleString(className);
        }
        throw new IllegalArgumentException("Don't know how to mangle " + type);
    }
}
