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
package org.teavm.compiler.bootstrap.extension;

import org.teavm.extension.bridge.BridgeClass;
import org.teavm.extension.bridge.BridgeField;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeMethod;
import org.teavm.extension.bridge.BridgeTypeVariable;
import org.teavm.extension.introspect.IntrospectClass;

class HostClass extends HostType implements BridgeClass {
    final IntrospectClass<?> cls;

    HostClass(HostEnvironment env, IntrospectClass<?> cls) {
        super(env, cls, BridgeKinds.CLASS);
        this.cls = cls;
    }

    @Override
    public String name() {
        return cls.name();
    }

    @Override
    public String simpleName() {
        return cls.simpleName();
    }

    @Override
    public int modifiers() {
        return cls.modifiers();
    }

    @Override
    public int flags() {
        var result = 0;
        if (cls.isPrimitive()) {
            result |= BridgeKinds.CLASS_PRIMITIVE;
        }
        if (cls.isInterface()) {
            result |= BridgeKinds.CLASS_INTERFACE;
        }
        if (cls.isArray()) {
            result |= BridgeKinds.CLASS_ARRAY;
        }
        if (cls.isAnnotation()) {
            result |= BridgeKinds.CLASS_ANNOTATION;
        }
        if (cls.isEnum()) {
            result |= BridgeKinds.CLASS_ENUM;
        }
        if (cls.isRecord()) {
            result |= BridgeKinds.CLASS_RECORD;
        }
        return result;
    }

    @Override
    public BridgeClass componentType() {
        return env.cls(cls.componentType());
    }

    @Override
    public BridgeClass arrayType() {
        return env.cls(cls.arrayType());
    }

    @Override
    public BridgeClass superclass() {
        return env.cls(cls.superclass());
    }

    @Override
    public BridgeClass[] interfaces() {
        return env.classes(cls.interfaces());
    }

    @Override
    public BridgeField[] enumConstants() {
        return env.fields(cls.enumConstants());
    }

    @Override
    public boolean isAssignableFrom(BridgeClass other) {
        return cls.isAssignableFrom(env.unwrapClass(other));
    }

    @Override
    public BridgeMethod[] declaredMethods() {
        return env.methods(cls.declaredMethods());
    }

    @Override
    public BridgeMethod[] methods() {
        return env.methods(cls.methods());
    }

    @Override
    public BridgeMethod declaredMethod(String name, BridgeClass[] parameterTypes) {
        return env.method(cls.declaredMethod(name, env.unwrapClasses(parameterTypes)));
    }

    @Override
    public BridgeMethod method(String name, BridgeClass[] parameterTypes) {
        return env.method(cls.method(name, env.unwrapClasses(parameterTypes)));
    }

    @Override
    public BridgeField[] declaredFields() {
        return env.fields(cls.declaredFields());
    }

    @Override
    public BridgeField[] fields() {
        return env.fields(cls.fields());
    }

    @Override
    public BridgeField declaredField(String name) {
        return env.field(cls.declaredField(name));
    }

    @Override
    public BridgeField field(String name) {
        return env.field(cls.field(name));
    }

    @Override
    public BridgeTypeVariable[] typeParameters() {
        return env.typeVariables(cls.typeParameters());
    }
}
