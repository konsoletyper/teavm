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
package org.teavm.metaprogramming.impl;

import org.teavm.metaprogramming.Action;
import org.teavm.metaprogramming.Computation;
import org.teavm.metaprogramming.InvocationHandler;
import org.teavm.metaprogramming.LazyComputation;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Scope;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.impl.reflect.ReflectClassImpl;
import org.teavm.metaprogramming.impl.reflect.ReflectContext;
import org.teavm.model.ValueType;

public final class MetaprogrammingImpl {
    static ClassLoader classLoader;
    static ReflectContext reflectContext;

    private MetaprogrammingImpl() {
    }

    public static <T> Value<T> emit(Computation<T> computation) {
        unsupported();
        return null;
    }

    public static void emit(Action action) {
        unsupported();
    }

    public static <T> Value<T> lazyFragment(LazyComputation<T> computation) {
        unsupported();
        return null;
    }

    public static <T> Value<T> lazy(Computation<T> computation) {
        unsupported();
        return null;
    }

    public static Scope currentScope() {
        unsupported();
        return null;
    }

    public static void location(String fileName, int lineNumber) {
        unsupported();
    }

    public static void defaultLocation() {
        unsupported();
    }

    public static ReflectClass<?> findClass(String name) {
        return reflectContext.findClass(name);
    }

    public static <T> ReflectClass<T> findClass(Class<T> cls) {
        return reflectContext.findClass(cls);
    }

    static ReflectClass<?> findClass(ValueType type) {
        return reflectContext.getClass(type);
    }

    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    @SuppressWarnings("unchecked")
    public static <T> ReflectClass<T[]> arrayClass(ReflectClass<T> componentType) {
        ReflectClassImpl<T> componentTypeImpl = (ReflectClassImpl<T>) componentType;
        return (ReflectClassImpl<T[]>) reflectContext.getClass(ValueType.arrayOf(componentTypeImpl.type));
    }

    public static ReflectClass<?> createClass(byte[] bytecode) {
        unsupported();
        return null;
    }

    public static <T> Value<T> proxy(Class<T> type, InvocationHandler<T> handler)  {
        return proxy(findClass(type), handler);
    }

    public static <T> Value<T> proxy(ReflectClass<T> type, InvocationHandler<T> handler) {
        unsupported();
        return null;
    }

    private static void unsupported() {
        throw new UnsupportedOperationException("This operation is only supported from TeaVM compile-time "
                + "environment");
    }
}
