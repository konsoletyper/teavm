/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.extension.introspect;

import java.util.List;

public interface IntrospectClass<T> extends IntrospectAnnotatedElement, IntrospectType, IntrospectGenericDeclaration,
        IntrospectElement {
    boolean isPrimitive();

    boolean isInterface();

    boolean isArray();

    boolean isAnnotation();

    boolean isEnum();

    boolean isRecord();

    List<? extends IntrospectField> enumConstants();

    IntrospectClass<?> componentType();

    IntrospectClass<T[]> arrayType();

    String simpleName();

    IntrospectClass<? super T> superclass();

    List<? extends IntrospectClass<? super T>> interfaces();

    <U> IntrospectClass<U> asSubclass(Class<U> cls);

    <U> IntrospectClass<U> asSubclass(IntrospectClass<U> cls);

    @SuppressWarnings("unchecked")
    default <U> IntrospectClass<U> asSubclassUnchecked() {
        return (IntrospectClass<U>) this;
    }

    boolean isAssignableFrom(IntrospectClass<?> cls);

    boolean isAssignableFrom(Class<?> cls);

    List<? extends IntrospectMethod> declaredMethods();

    List<? extends IntrospectMethod> methods();

    IntrospectMethod declaredMethod(String name, IntrospectClass<?>... parameterTypes);

    IntrospectMethod declaredJavaMethod(String name, Class<?>... parameterTypes);

    IntrospectMethod method(String name, IntrospectClass<?>... parameterTypes);

    IntrospectMethod javaMethod(String name, Class<?>... parameterTypes);

    List<? extends IntrospectField> declaredFields();

    List<? extends IntrospectField> fields();

    IntrospectField declaredField(String name);

    IntrospectField field(String name);

    Class<T> asJavaClass();
}
