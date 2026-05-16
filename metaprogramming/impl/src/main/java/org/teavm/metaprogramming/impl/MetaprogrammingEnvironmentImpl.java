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
package org.teavm.metaprogramming.impl;

import java.util.Iterator;
import org.teavm.dependency.DependencyAgent;
import org.teavm.extension.ExtensionEnvironmentImpl;
import org.teavm.extension.diagnostics.Diagnostics;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectClassImpl;
import org.teavm.extension.resource.Resource;
import org.teavm.metaprogramming.MetaprogrammingEnvironment;
import org.teavm.model.ValueType;

public class MetaprogrammingEnvironmentImpl implements MetaprogrammingEnvironment {
    public final ExtensionEnvironmentImpl underlyingEnv;
    private DependencyAgent dependency;

    public MetaprogrammingEnvironmentImpl(ExtensionEnvironmentImpl underlyingEnv, DependencyAgent dependency) {
        this.underlyingEnv = underlyingEnv;
        this.dependency = dependency;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IntrospectClass<T[]> arrayClass(IntrospectClass<T> componentType) {
        var componentTypeImpl = (IntrospectClassImpl<T>) componentType;
        return (IntrospectClass<T[]>) underlyingEnv.introspection().getClass(
                ValueType.arrayOf(componentTypeImpl.type));
    }

    @Override
    public IntrospectClass<?> createClass(byte[] bytecode) {
        return findClass(dependency.submitClassFile(bytecode).replace('/', '.'));
    }

    @Override
    public Iterator<Resource> resources(String name) {
        return underlyingEnv.resources(name);
    }

    @Override
    public Diagnostics diagnostics() {
        return underlyingEnv.diagnostics();
    }

    @Override
    public ClassLoader classLoader() {
        return underlyingEnv.classLoader();
    }

    @Override
    public IntrospectClass<?> findClass(String name) {
        return underlyingEnv.findClass(name);
    }

    @Override
    public <T> IntrospectClass<T> findClass(Class<T> cls) {
        return underlyingEnv.findClass(cls);
    }
}
