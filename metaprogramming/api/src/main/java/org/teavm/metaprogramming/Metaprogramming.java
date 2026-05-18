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
package org.teavm.metaprogramming;

import java.util.Iterator;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectMethod;

@SuppressWarnings("unused")
public final class Metaprogramming {
    private Metaprogramming() {
    }

    public static <T> Value<T> emit(Computation<T> computation) {
        throw unsupported();
    }

    public static void emit(Action action) {
        throw unsupported();
    }

    public static <T> Value<T> lazyFragment(LazyComputation<T> computation) {
        throw unsupported();
    }

    public static <T> Value<T> lazy(Computation<T> computation) {
        throw unsupported();
    }

    public static void exit(Computation<?> returnValue) {
        throw unsupported();
    }

    public static void exit() {
        throw unsupported();
    }

    public static void location(String fileName, int lineNumber) {
        throw unsupported();
    }

    public static void defaultLocation() {
        throw unsupported();
    }

    @Deprecated
    public static SourceLocation getLocation() {
        throw unsupported();
    }

    public static org.teavm.extension.diagnostics.SourceLocation currentLocation() {
        throw unsupported();
    }

    public static MetaprogrammingEnvironment environment() {
        throw unsupported();
    }

    @Deprecated
    public static ReflectClass<?> findClass(String name) {
        throw unsupported();
    }

    @Deprecated
    public static <T> ReflectClass<T> findClass(Class<T> cls) {
        throw unsupported();
    }

    @Deprecated
    public static ClassLoader getClassLoader() {
        throw unsupported();
    }

    @Deprecated
    public static <T> ReflectClass<T[]> arrayClass(ReflectClass<T> componentType) {
        throw unsupported();
    }

    @Deprecated
    public static ReflectClass<?> createClass(byte[] bytecode) {
        throw unsupported();
    }

    @Deprecated
    public static <T> Value<T> proxy(Class<T> type, InvocationHandler<T> handler)  {
        throw unsupported();
    }

    @Deprecated
    public static <T> Value<T> proxy(ReflectClass<T> type, InvocationHandler<T> handler) {
        throw unsupported();
    }

    public static <T> Value<T> proxy(IntrospectClass<T> type, ProxyHandler<T> handler) {
        throw unsupported();
    }

    @Deprecated
    public static Diagnostics getDiagnostics() {
        throw unsupported();
    }

    @Deprecated
    public static Iterator<Resource> getResources(String name) {
        throw unsupported();
    }

    public static MethodCaller caller(IntrospectMethod method) {
        throw unsupported();
    }

    public static FieldAccessor accessor(IntrospectField field) {
        throw unsupported();
    }

    public static void unsupportedCase() {
        throw unsupported();
    }

    private static RuntimeException unsupported() {
        return new UnsupportedOperationException("This operation is only supported from TeaVM compile-time "
                + "environment");
    }
}
