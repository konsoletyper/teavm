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

public abstract sealed class ValueType implements Serializable
        permits ValueType.Object, ValueType.Array, ValueType.Primitive, ValueType.Void {
    private static final Map<Class<?>, ValueType> primitiveMap = new HashMap<>();

    private ValueType() {
    }

    public static final class Object extends ValueType {
        private String className;
        private transient int hash;

        public Object(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        @Override
        public String toString() {
            return "L" + className.replace('.', '/') + ";";
        }

        @Override
        public boolean isObject(String className) {
            return this.className.equals(className);
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Object that)) {
                return false;
            }
            return that.className.equals(className);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                hash = 85396296 ^ (className.hashCode() * 167);
                if (hash == 0) {
                    ++hash;
                }
            }
            return hash;
        }
    }

    public static final class Primitive extends ValueType {
        private PrimitiveType kind;
        private final ValueType.Object boxedType;
        private int hash;

        private Primitive(PrimitiveType kind, ValueType.Object boxedType) {
            this.kind = kind;
            this.boxedType = boxedType;
            hash = 17988782 ^ (kind.ordinal() * 31);
        }

        public PrimitiveType getKind() {
            return kind;
        }

        public ValueType.Object getBoxedType() {
            return boxedType;
        }

        @Override
        public String toString() {
            return switch (kind) {
                case BOOLEAN -> "Z";
                case BYTE -> "B";
                case SHORT -> "S";
                case INTEGER -> "I";
                case LONG -> "J";
                case FLOAT -> "F";
                case CHARACTER -> "C";
                case DOUBLE -> "D";
            };
        }

        @Override
        public boolean isObject(String cls) {
            return false;
        }


        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static final class Array extends ValueType {
        private ValueType itemType;
        private transient int hash;

        public Array(ValueType itemType) {
            this.itemType = itemType;
        }

        public ValueType getItemType() {
            return itemType;
        }

        @Override
        public String toString() {
            return "[" + itemType;
        }

        @Override
        public boolean isObject(String cls) {
            return false;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Array that)) {
                return false;
            }

            return itemType.equals(that.itemType);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                hash = 27039876 ^ (itemType.hashCode() * 193);
                if (hash == 0) {
                    ++hash;
                }
            }
            return hash;
        }
    }

    public static final class Void extends ValueType {
        private Void() {
        }

        @Override
        public String toString() {
            return "V";
        }

        @Override
        public boolean isObject(String cls) {
            return false;
        }

        @Override
        public int hashCode() {
            return 53604390;
        }
    }

    public static final Void VOID = new Void();

    public static final Primitive BOOLEAN =
            new Primitive(PrimitiveType.BOOLEAN, ValueType.object(Boolean.class.getName()));

    public static final Primitive BYTE = new Primitive(PrimitiveType.BYTE, ValueType.object(Byte.class.getName()));

    public static final Primitive SHORT = new Primitive(PrimitiveType.SHORT, ValueType.object(Short.class.getName()));

    public static final Primitive INTEGER =
            new Primitive(PrimitiveType.INTEGER, ValueType.object(Integer.class.getName()));

    public static final Primitive FLOAT = new Primitive(PrimitiveType.FLOAT, ValueType.object(Float.class.getName()));

    public static final Primitive LONG = new Primitive(PrimitiveType.LONG, ValueType.object(Long.class.getName()));

    public static final Primitive DOUBLE =
            new Primitive(PrimitiveType.DOUBLE, ValueType.object(Double.class.getName()));

    public static final Primitive CHARACTER =
            new Primitive(PrimitiveType.CHARACTER, ValueType.object(Character.class.getName()));

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

    public static ValueType.Object object(String cls) {
        return new Object(cls);
    }

    public static ValueType.Array arrayOf(ValueType type) {
        return new Array(type);
    }

    public static ValueType primitive(PrimitiveType type) {
        return switch (type) {
            case BOOLEAN -> BOOLEAN;
            case BYTE -> BYTE;
            case CHARACTER -> CHARACTER;
            case SHORT -> SHORT;
            case INTEGER -> INTEGER;
            case LONG -> LONG;
            case FLOAT -> FLOAT;
            case DOUBLE -> DOUBLE;
        };
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
            ValueType type = parseIfPossible(text.substring(index, nextIndex));
            if (type == null) {
                return null;
            }
            types.add(type);
            index = nextIndex;
        }
        return types.toArray(new ValueType[0]);
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
        return switch (string.charAt(0)) {
            case 'Z' -> primitive(PrimitiveType.BOOLEAN);
            case 'B' -> primitive(PrimitiveType.BYTE);
            case 'S' -> primitive(PrimitiveType.SHORT);
            case 'I' -> primitive(PrimitiveType.INTEGER);
            case 'J' -> primitive(PrimitiveType.LONG);
            case 'F' -> primitive(PrimitiveType.FLOAT);
            case 'D' -> primitive(PrimitiveType.DOUBLE);
            case 'C' -> primitive(PrimitiveType.CHARACTER);
            case 'V' -> VOID;
            case 'L' -> {
                if (!string.endsWith(";")) {
                    yield null;
                }
                yield object(string.substring(1, string.length() - 1).replace('/', '.'));
            }
            default -> null;
        };
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
}
