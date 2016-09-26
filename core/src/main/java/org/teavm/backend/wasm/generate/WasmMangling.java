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
package org.teavm.backend.wasm.generate;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public final class WasmMangling {
    private WasmMangling() {
    }

    public static String mangleIsSupertype(ValueType type) {
        return "isSupertype$" + mangleType(type);
    }

    public static String mangleMethod(MethodReference method) {
        String className = method.getClassName().length() + mangleString(method.getClassName());
        StringBuilder sb = new StringBuilder("method$" + className + "_");
        String name = mangleString(method.getName());
        sb.append(mangleType(method.getReturnType()));
        sb.append(name.length() + "_" + name);
        sb.append(Arrays.stream(method.getParameterTypes())
                .map(WasmMangling::mangleType)
                .collect(Collectors.joining()));
        return sb.toString();
    }

    public static String mangleInitializer(String className) {
        return "clinit$" + mangleString(className);
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
