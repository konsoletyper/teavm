/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.gc;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.HashMap;
import java.util.Map;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCNameProvider {
    private ObjectIntMap<String> occupiedTopLevelIndexes = new ObjectIntHashMap<>();
    private ObjectIntMap<String> occupiedStructIndexes = new ObjectIntHashMap<>();

    private Map<MethodDescriptor, String> virtualMethodNames = new HashMap<>();
    private Map<FieldReference, String> memberFieldNames = new HashMap<>();

    public String topLevel(String name) {
        return pickUnoccupied(name);
    }

    public String structureField(String name) {
        return pickUnoccupied(name, occupiedStructIndexes);
    }

    public String forVirtualMethod(MethodDescriptor method) {
        return virtualMethodNames.computeIfAbsent(method,
                k -> pickUnoccupied(sanitize(k.getName()), occupiedStructIndexes));
    }

    public String forMemberField(FieldReference field) {
        return memberFieldNames.computeIfAbsent(field,
                k -> pickUnoccupied(sanitize(field.getFieldName()), occupiedStructIndexes));
    }

    public String suggestForMethod(MethodReference method) {
        StringBuilder sb = new StringBuilder();
        sb.append(sanitize(method.getClassName()));
        sb.append("::");
        sb.append(sanitize(method.getName()));
        return sb.toString();
    }

    public String suggestForStaticField(FieldReference field) {
        StringBuilder sb = new StringBuilder();
        sb.append(sanitize(field.getClassName()));
        sb.append('#');
        sb.append(sanitize(field.getFieldName()));
        return sb.toString();
    }

    public String suggestForClass(String className) {
        return sanitize(className);
    }

    public String suggestForType(ValueType type) {
        StringBuilder sb = new StringBuilder();
        suggestForType(type, sb);
        return sb.toString();
    }

    private void suggestForType(ValueType type, StringBuilder sb) {
        if (type instanceof ValueType.Object) {
            sb.append(suggestForClass(((ValueType.Object) type).getClassName()));
        } else if (type instanceof ValueType.Array) {
            sb.append("Array<");
            suggestForType(((ValueType.Array) type).getItemType(), sb);
            sb.append(">");
        } else {
            sb.append("&").append(type.toString());
        }
    }

    public static String sanitize(String name) {
        char[] chars = null;
        var i = 0;
        for (; i < name.length(); ++i) {
            var c = name.charAt(i);
            if (!isIdentifierPart(c)) {
                chars = new char[name.length()];
                name.getChars(0, i, chars, 0);
                chars[i++] = '_';
                break;
            }
        }
        if (chars == null) {
            return name;
        }
        for (; i < name.length(); ++i) {
            var c = name.charAt(i);
            chars[i] = isIdentifierPart(c) ? c : '_';
        }
        return new String(chars);
    }

    public static boolean isIdentifierPart(char c) {
        switch (c) {
            case '!':
            case '#':
            case '$':
            case '%':
            case '&':
            case '`':
            case '*':
            case '+':
            case '-':
            case '.':
            case '/':
            case ':':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case '\\':
            case '^':
            case '_':
            case '\'':
            case '|':
            case '~':
                return true;
            default:
                return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
        }

    }

    private String pickUnoccupied(String name) {
        return pickUnoccupied(name, occupiedTopLevelIndexes);
    }

    private String pickUnoccupied(String name, ObjectIntMap<String> occupiedIndexes) {
        String result = name;
        int index = occupiedIndexes.getOrDefault(name, -1);
        if (index >= 0) {
            result = name + "_" + index;
        }
        occupiedIndexes.put(name, ++index);

        return result;
    }
}
