/*
 *  Copyright 2025 konsoletyper.
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
package org.teavm.classlib.java.lang.invoke;

/**
 * ConstantBootstraps provides bootstrap methods for dynamically computing
 * constants. These are used by the JVM's {@code invokedynamic} instruction
 * for constant resolution.
 *
 * <p>In TeaVM, these methods provide the API surface for compatibility.
 * Actual constant bootstrap resolution is handled by TeaVM's metaprogramming
 * infrastructure at compile time.</p>
 */
public class TConstantBootstraps {
    private TConstantBootstraps() {
    }

    public static Object nullConstant(MethodHandles.Lookup lookup, String name, Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return (char) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0;
        }
        return null;
    }

    public static Class<?> primitiveClass(MethodHandles.Lookup lookup, String name, Class<?> type) {
        switch (name) {
            case "boolean": return boolean.class;
            case "byte": return byte.class;
            case "char": return char.class;
            case "short": return short.class;
            case "int": return int.class;
            case "long": return long.class;
            case "float": return float.class;
            case "double": return double.class;
            case "void": return void.class;
            default: throw new IllegalArgumentException("Not a primitive type: " + name);
        }
    }

    public static <T> T explicitCast(MethodHandles.Lookup lookup, String name, Class<T> dstType, Object value) {
        return dstType.cast(value);
    }

    public static Object getStaticFinal(MethodHandles.Lookup lookup, String name, Class<?> type) {
        try {
            return lookup.findStaticGetter(type, name, type).invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getStaticFinal(MethodHandles.Lookup lookup, String name, Class<?> type,
            Class<?> declaringClass) {
        try {
            return lookup.findStaticGetter(declaringClass, name, type).invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object enumConstant(MethodHandles.Lookup lookup, String name, Class<?> type) {
        @SuppressWarnings("unchecked")
        Enum<?>[] constants = ((Class<Enum<?>>) type).getEnumConstants();
        for (Enum<?> constant : constants) {
            if (constant.name().equals(name)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("No enum constant " + type.getName() + "." + name);
    }
}
