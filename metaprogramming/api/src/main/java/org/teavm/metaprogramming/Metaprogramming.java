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

@SuppressWarnings("unused")
public final class Metaprogramming {
    private Metaprogramming() {
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

    public static void exit(Computation<?> returnValue) {
        unsupported();
    }

    public static void exit() {
        unsupported();
    }

    public static void location(String fileName, int lineNumber) {
        unsupported();
    }

    public static void defaultLocation() {
        unsupported();
    }

    public static SourceLocation getLocation() {
        unsupported();
        return null;
    }

    public static ReflectClass<?> findClass(String name) {
        unsupported();
        return null;
    }

    public static <T> ReflectClass<T> findClass(Class<T> cls) {
        unsupported();
        return null;
    }

    public static ClassLoader getClassLoader() {
        unsupported();
        return null;
    }

    public static <T> ReflectClass<T[]> arrayClass(ReflectClass<T> componentType) {
        throw new UnsupportedOperationException();
    }

    public static ReflectClass<?> createClass(byte[] bytecode) {
        unsupported();
        return null;
    }

    public static <T> Value<T> proxy(Class<T> type, InvocationHandler<T> handler)  {
        unsupported();
        return null;
    }

    public static <T> Value<T> proxy(ReflectClass<T> type, InvocationHandler<T> handler) {
        unsupported();
        return null;
    }

    public static Diagnostics getDiagnostics() {
        unsupported();
        return null;
    }

    public static void unsupportedCase() {
        unsupported();
    }

    private static void unsupported() {
        throw new UnsupportedOperationException("This operation is only supported from TeaVM compile-time "
                + "environment");
    }
}
