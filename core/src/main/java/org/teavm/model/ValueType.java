/*
 *  Copyright 2012 Alexey Andreev.
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

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ValueType implements Serializable {
    volatile String reprCache;
    private static final Map<Class<?>, ValueType> primitiveMap = new HashMap<>();

    private ValueType() {
    }

    public static class Object extends ValueType {
        private String className;

        public Object(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        @Override
        public String toString() {
            if (reprCache == null) {
                reprCache = "L" + className.replace('.', '/') + ";";
            }
            return reprCache;
        }

        @Override
        public boolean isObject(String className) {
            return this.className.equals(className);
        }
    }

    public static class Primitive extends ValueType {
        private PrimitiveType kind;

        Primitive(PrimitiveType kind) {
            this.kind = kind;
        }

        public PrimitiveType getKind() {
            return kind;
        }

        @Override
        public String toString() {
            if (reprCache == null) {
                reprCache = createString();
            }
            return reprCache;
        }

        private String createString() {
            switch (kind) {
                case BOOLEAN:
                    return "Z";
                case BYTE:
                    return "B";
                case SHORT:
                    return "S";
                case INTEGER:
                    return "I";
                case LONG:
                    return "J";
                case FLOAT:
                    return "F";
                case CHARACTER:
                    return "C";
                case DOUBLE:
                    return "D";
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public boolean isObject(String cls) {
            return false;
        }
    }

    public static class Array extends ValueType {
        private ValueType itemType;

        public Array(ValueType itemType) {
            this.itemType = itemType;
        }

        public ValueType getItemType() {
            return itemType;
        }

        @Override
        public String toString() {
            if (reprCache == null) {
                reprCache = "[" + itemType.toString();
            }
            return reprCache;
        }

        @Override
        public boolean isObject(String cls) {
            return false;
        }
    }

    public static class Void extends ValueType {
        @Override
        public String toString() {
            return "V";
        }

        @Override
        public boolean isObject(String cls) {
            return false;
        }
    }

    public static class Null extends ValueType {
        @Override
        public boolean isObject(String cls) {
            return false;
        }
    }

    public static final Void VOID = new Void();

    public static final Primitive BOOLEAN = new Primitive(PrimitiveType.BOOLEAN);

    public static final Primitive BYTE = new Primitive(PrimitiveType.BYTE);

    public static final Primitive SHORT = new Primitive(PrimitiveType.SHORT);

    public static final Primitive INTEGER = new Primitive(PrimitiveType.INTEGER);

    public static final Primitive FLOAT = new Primitive(PrimitiveType.FLOAT);

    public static final Primitive LONG = new Primitive(PrimitiveType.LONG);

    public static final Primitive DOUBLE = new Primitive(PrimitiveType.DOUBLE);

    public static final Primitive CHARACTER = new Primitive(PrimitiveType.CHARACTER);

    public static final Null NULL = new Null();


    static {
        primitiveMap.put(boolean.class, BOOLEAN);
        primitiveMap.put(char.class, CHARACTER);
        primitiveMap.put(byte.class, BYTE);
        primitiveMap.put(short.class, SHORT);
        primitiveMap.put(int.class, INTEGER);
        primitiveMap.put(long.class, LONG);
        primitiveMap.put(float.class, FLOAT);
        primitiveMap.put(double.class, DOUBLE);
        primitiveMap.put(void.class, VOID);
    }

    public static ValueType object(String cls) {
        return new Object(cls);
    }

    public static ValueType arrayOf(ValueType type) {
        return new Array(type);
    }

    public static ValueType primitive(PrimitiveType type) {
        switch (type) {
            case BOOLEAN:
                return BOOLEAN;
            case BYTE:
                return BYTE;
            case CHARACTER:
                return CHARACTER;
            case SHORT:
                return SHORT;
            case INTEGER:
                return INTEGER;
            case LONG:
                return LONG;
            case FLOAT:
                return FLOAT;
            case DOUBLE:
                return DOUBLE;
            default:
                throw new AssertionError("Unknown primitive type " + type);
        }
    }

    public static ValueType[] parseMany(String text) {
        ValueType[] types = parseManyIfPossible(text);
        if (types == null) {
            throw new IllegalArgumentException("Illegal method type signature: " + text);
        }
        return types;
    }

    public static ValueType[] parseManyIfPossible(String text) {
        List<ValueType> types = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int nextIndex = cut(text, index);
            ValueType type = parse(text.substring(index, nextIndex));
            if (type == null) {
                return null;
            }
            types.add(type);
            index = nextIndex;
        }
        return types.toArray(new ValueType[types.size()]);
    }

    private static int cut(String text, int index) {
        while (text.charAt(index) == '[') {
            if (++index >= text.length()) {
                return index;
            }
        }
        if (text.charAt(index) != 'L') {
            return index + 1;
        }
        while (text.charAt(index) != ';') {
            if (++index >= text.length()) {
                return index;
            }
        }
        return index + 1;
    }

    public static ValueType parse(String string) {
        ValueType type = parseIfPossible(string);
        if (type == null) {
            throw new IllegalArgumentException("Illegal type signature: " + string);
        }
        return type;
    }

    public static ValueType parseIfPossible(String string) {
        int arrayDegree = 0;
        int left = 0;
        while (string.charAt(left) == '[') {
            ++arrayDegree;
            ++left;
        }
        string = string.substring(left);
        if (string.isEmpty()) {
            return null;
        }
        ValueType type = parseImpl(string);
        if (type == null) {
            return null;
        }
        while (arrayDegree-- > 0) {
            type = arrayOf(type);
        }
        return type;
    }

    public static String manyToString(ValueType[] types) {
        StringBuilder sb = new StringBuilder();
        for (ValueType type : types) {
            sb.append(type);
        }
        return sb.toString();
    }

    public static String methodTypeToString(ValueType[] types) {
        return "(" + Arrays.stream(types, 0, types.length - 1).map(ValueType::toString)
                .collect(Collectors.joining()) + ")" + types[types.length - 1];
    }

    public abstract boolean isObject(String cls);

    public boolean isObject(Class<?> cls) {
        return isObject(cls.getName());
    }

    private static ValueType parseImpl(String string) {
        switch (string.charAt(0)) {
            case 'Z':
                return primitive(PrimitiveType.BOOLEAN);
            case 'B':
                return primitive(PrimitiveType.BYTE);
            case 'S':
                return primitive(PrimitiveType.SHORT);
            case 'I':
                return primitive(PrimitiveType.INTEGER);
            case 'J':
                return primitive(PrimitiveType.LONG);
            case 'F':
                return primitive(PrimitiveType.FLOAT);
            case 'D':
                return primitive(PrimitiveType.DOUBLE);
            case 'C':
                return primitive(PrimitiveType.CHARACTER);
            case 'V':
                return VOID;
            case 'L':
                if (!string.endsWith(";")) {
                    return null;
                }
                return object(string.substring(1, string.length() - 1).replace('/', '.'));
            default:
                return null;
        }
    }

    public boolean isSubtypeOf(ValueType supertype) {
        if (supertype instanceof ValueType.Object) {
            return !(this instanceof Primitive);
        } else if (supertype instanceof Array && this instanceof Array) {
            return ((Array) this).getItemType().isSubtypeOf(((Array) supertype).getItemType());
        } else {
            return false;
        }
    }

    public static ValueType parse(Class<?> cls) {
        if (cls.isPrimitive()) {
            return primitiveMap.get(cls);
        } else if (cls.getComponentType() != null) {
            return ValueType.arrayOf(ValueType.parse(cls.getComponentType()));
        } else {
            return ValueType.object(cls.getName());
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) {
           return true;
        }
        if (!(obj instanceof ValueType)) {
            return false;
        }
        return toString().equals(obj.toString());
    }
}
