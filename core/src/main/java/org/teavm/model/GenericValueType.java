/*
 *  Copyright 2019 konsoletyper.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class GenericValueType {
    private static final Argument[] EMPTY_ARRAY = new Argument[0];

    public static final class Primitive extends GenericValueType {
        private final PrimitiveType kind;
        private final int hash;

        private Primitive(PrimitiveType kind) {
            this.kind = kind;
            hash = 17988782 ^ (kind.ordinal() * 31);
        }

        public PrimitiveType getKind() {
            return kind;
        }

        @Override
        void toString(StringBuilder sb) {
            switch (kind) {
                case BOOLEAN:
                    sb.append("Z");
                    break;
                case BYTE:
                    sb.append("B");
                    break;
                case SHORT:
                    sb.append("S");
                    break;
                case INTEGER:
                    sb.append("I");
                    break;
                case LONG:
                    sb.append("J");
                    break;
                case FLOAT:
                    sb.append("F");
                    break;
                case CHARACTER:
                    sb.append("C");
                    break;
                case DOUBLE:
                    sb.append("D");
                    break;
            }
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static final class Void extends GenericValueType {
        private Void() {
        }

        @Override
        void toString(StringBuilder sb) {
            sb.append("V");
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return 53604390;
        }
    }

    public static abstract class Reference extends GenericValueType {
        private Reference() {
        }
    }

    public static class Argument {
        public static final Argument ANY = new Argument(ArgumentKind.ANY, null);
        private final ArgumentKind kind;
        private final Reference value;
        private int hash;

        private Argument(ArgumentKind kind, Reference value) {
            this.kind = kind;
            this.value = value;
        }

        public ArgumentKind getKind() {
            return kind;
        }

        public Reference getValue() {
            return value;
        }

        public static Argument invariant(Reference value) {
            return new Argument(ArgumentKind.INVARIANT, value);
        }

        public static Argument covariant(Reference value) {
            return new Argument(ArgumentKind.COVARIANT, value);
        }

        public static Argument contravariant(Reference value) {
            return new Argument(ArgumentKind.CONTRAVARIANT, value);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                switch (kind) {
                    case ANY:
                        hash = 43866465;
                        break;
                    case CONTRAVARIANT:
                        hash = 6993579;
                        break;
                    case COVARIANT:
                        hash = 4379540;
                        break;
                    case INVARIANT:
                        hash = 72320251;
                        break;
                }
            }
            if (value != null) {
                hash = hash * 47 + value.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (this == obj) {
                return true;
            }
            return super.equals(obj);
        }
    }

    public enum ArgumentKind {
        COVARIANT,
        CONTRAVARIANT,
        INVARIANT,
        ANY
    }

    public static final class Object extends Reference {
        private final Object parent;
        private final String className;
        private final Argument[] arguments;
        private int hash;

        public Object(Object parent, String className, Argument[] arguments) {
            this.parent = parent;
            this.className = className;
            this.arguments = arguments != null ? arguments.clone() : EMPTY_ARRAY;
        }

        public Object getParent() {
            return parent;
        }

        public String getClassName() {
            return className;
        }

        public Argument[] getArguments() {
            return arguments.length == 0 ? EMPTY_ARRAY : arguments.clone();
        }

        @Override
        void toString(StringBuilder sb) {
            sb.append("L");
            toStringImpl(sb);
            sb.append(";");
        }

        private void toStringImpl(StringBuilder sb) {
            if (parent != null) {
                parent.toStringImpl(sb);
                sb.append(".");
            }
            sb.append(className.replace('.', '/'));
            if (arguments.length > 0) {
                sb.append("<");
                for (Argument argument : arguments) {
                    switch (argument.kind) {
                        case ANY:
                            sb.append("*");
                            break;
                        case CONTRAVARIANT:
                            sb.append("-");
                            break;
                        case COVARIANT:
                            sb.append("+");
                            break;
                        case INVARIANT:
                            break;
                    }
                    if (argument.value != null) {
                        argument.value.toString(sb);
                    }
                }
                sb.append(">");
            }
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Object)) {
                return false;
            }
            Object that = (Object) obj;
            if (that.arguments.length != arguments.length) {
                return false;
            }
            if (!that.className.equals(className)) {
                return false;
            }
            for (int i = 0; i < arguments.length; ++i) {
                if (!arguments[i].equals(that.arguments[i])) {
                    return false;
                }
            }
            return Objects.equals(parent, that.parent);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                hash = 85396296 ^ (className.hashCode() * 167);
                for (Argument arg : arguments) {
                    hash = hash * 31 + arg.hashCode();
                }
                if (parent != null) {
                    hash = 167 * hash + parent.hashCode();
                }
                if (hash == 0) {
                    ++hash;
                }
            }
            return hash;
        }
    }

    public static final class Variable extends Reference {
        private final String name;
        private int hash;

        public Variable(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Variable)) {
                return false;
            }
            Variable that = (Variable) obj;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                hash = 69257681 ^ (name.hashCode() * 173);
                if (hash == 0) {
                    ++hash;
                }
            }
            return hash;
        }

        @Override
        void toString(StringBuilder sb) {
            sb.append("T");
            sb.append(name);
            sb.append(";");
        }
    }

    public static final class Array extends Reference {
        private final GenericValueType itemType;
        private int hash;


        public Array(GenericValueType itemType) {
            this.itemType = itemType;
        }

        public GenericValueType getItemType() {
            return itemType;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Array)) {
                return false;
            }

            Array that = (Array) obj;
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

        @Override
        void toString(StringBuilder sb) {
            sb.append("[");
            itemType.toString(sb);
        }
    }


    public static final Void VOID = new Void();

    public static final Primitive BOOLEAN = new Primitive(PrimitiveType.BOOLEAN);

    public static final Primitive BYTE = new Primitive(PrimitiveType.BYTE);

    public static final Primitive SHORT = new Primitive(PrimitiveType.SHORT);

    public static final Primitive INT = new Primitive(PrimitiveType.INTEGER);

    public static final Primitive FLOAT = new Primitive(PrimitiveType.FLOAT);

    public static final Primitive LONG = new Primitive(PrimitiveType.LONG);

    public static final Primitive DOUBLE = new Primitive(PrimitiveType.DOUBLE);

    public static final Primitive CHAR = new Primitive(PrimitiveType.CHARACTER);

    private GenericValueType() {
    }

    abstract void toString(StringBuilder sb);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public static GenericValueType parse(String text, ParsePosition position) {
        int i = position.index;
        if (i >= text.length()) {
            return null;
        }
        switch (text.charAt(i++)) {
            case 'V':
                position.index = i;
                return VOID;
            case 'Z':
                position.index = i;
                return BOOLEAN;
            case 'B':
                position.index = i;
                return BYTE;
            case 'S':
                position.index = i;
                return SHORT;
            case 'C':
                position.index = i;
                return CHAR;
            case 'I':
                position.index = i;
                return INT;
            case 'J':
                position.index = i;
                return LONG;
            case 'F':
                position.index = i;
                return FLOAT;
            case 'D':
                position.index = i;
                return DOUBLE;
            case 'L':
                position.index = i;
                return parseObjectImpl(text, position);
            case 'T':
                position.index = i;
                return parseVariable(text, position);
            case '[': {
                position.index = i;
                GenericValueType itemType = parse(text, position);
                return itemType != null ? new Array(itemType) : null;
            }
            default:
                return null;
        }
    }

    private static GenericValueType parseObjectImpl(String text, ParsePosition position) {
        int i = position.index;
        int last = i;
        Object parent = null;
        while (i < text.length()) {
            char c = text.charAt(i);
            switch (c) {
                case ';': {
                    if (last == i) {
                        return null;
                    }
                    String name = text.substring(last, i).replace('/', '.');
                    i++;
                    position.index = i;
                    return new Object(parent, name, EMPTY_ARRAY);
                }
                case '.': {
                    if (i == last) {
                        return null;
                    }
                    parent = new Object(parent, text.substring(last, i).replace('/', '.'), EMPTY_ARRAY);
                    i++;
                    last = i;
                    break;
                }
                case '<': {
                    position.index = i + 1;
                    Argument[] arguments = parseArguments(text, position);
                    if (arguments == null || position.index >= text.length()) {
                        return null;
                    }
                    String name = text.substring(last, i).replace('/', '.');
                    Object result = new Object(parent, name, arguments);
                    i = position.index;
                    c = text.charAt(i++);
                    if (c == ';') {
                        position.index = i;
                        return result;
                    } else if (c == '.') {
                        parent = result;
                        last = i;
                        break;
                    } else {
                        return null;
                    }
                }
                default:
                    i++;
                    break;
            }
        }
        return null;
    }

    private static Argument[] parseArguments(String text, ParsePosition position) {
        List<Argument> arguments = new ArrayList<>();
        while (position.index < text.length()) {
            char c = text.charAt(position.index);
            switch (c) {
                case '>':
                    if (arguments.isEmpty()) {
                        return null;
                    }
                    position.index++;
                    return arguments.toArray(new Argument[0]);
                case '*':
                    position.index++;
                    arguments.add(Argument.ANY);
                    break;
                case '+': {
                    position.index++;
                    Reference constraint = parseReference(text, position);
                    if (constraint == null) {
                        return null;
                    }
                    arguments.add(Argument.covariant(constraint));
                    break;
                }
                case '-': {
                    position.index++;
                    Reference constraint = parseReference(text, position);
                    if (constraint == null) {
                        return null;
                    }
                    arguments.add(Argument.contravariant(constraint));
                    break;
                }
                default: {
                    Reference constraint = parseReference(text, position);
                    if (constraint == null) {
                        return null;
                    }
                    arguments.add(Argument.invariant(constraint));
                    break;
                }
            }
        }
        return null;
    }

    public static Reference parseReference(String text, ParsePosition position) {
        GenericValueType type = parse(text, position);
        return type instanceof Reference ? (Reference) type : null;
    }

    public static Object parseObject(String text, ParsePosition position) {
        GenericValueType type = parse(text, position);
        return type instanceof Object ? (Object) type : null;
    }

    private static GenericValueType parseVariable(String text, ParsePosition position) {
        int i = position.index;
        while (i < text.length()) {
            if (text.charAt(i) == ';') {
                if (i == position.index) {
                    return null;
                }
                Variable result = new Variable(text.substring(position.index, i));
                position.index = i + 1;
                return result;
            }
            i++;
        }
        return null;
    }

    public GenericValueType parse(String text) {
        ParsePosition position = new ParsePosition();
        GenericValueType type = parse(text, position);
        return position.index == text.length() ? type : null;
    }

    public static class ParsePosition {
        public int index;
    }
}
