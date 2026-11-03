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
package org.teavm.extension.bridge.guest;

import java.util.List;
import org.teavm.extension.bridge.BridgeClass;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.introspect.IntrospectTypeVariable;

class GuestClass<T> extends GuestAnnotatedElement implements IntrospectClass<T> {
    private static final int FLAGS_RESOLVED = 1 << 30;
    private static final int SUPERCLASS_RESOLVED = 1 << 29;

    final BridgeClass remoteClass;
    private int flags;
    private String name;
    private String simpleName;
    private int modifiers = -1;
    private IntrospectClass<? super T> superclass;
    private List<? extends IntrospectClass<? super T>> interfaces;
    private List<? extends IntrospectField> enumConstants;
    private List<? extends IntrospectMethod> declaredMethods;
    private List<? extends IntrospectMethod> methods;
    private List<? extends IntrospectField> declaredFields;
    private List<? extends IntrospectField> fields;
    private List<? extends IntrospectTypeVariable> typeParameters;

    GuestClass(GuestEnvironment env, BridgeClass remote) {
        super(env, remote);
        remoteClass = remote;
    }

    private boolean hasFlag(int flag) {
        if ((flags & FLAGS_RESOLVED) == 0) {
            flags |= remoteClass.flags() | FLAGS_RESOLVED;
        }
        return (flags & flag) != 0;
    }

    @Override
    public boolean isPrimitive() {
        return hasFlag(BridgeKinds.CLASS_PRIMITIVE);
    }

    @Override
    public boolean isInterface() {
        return hasFlag(BridgeKinds.CLASS_INTERFACE);
    }

    @Override
    public boolean isArray() {
        return hasFlag(BridgeKinds.CLASS_ARRAY);
    }

    @Override
    public boolean isAnnotation() {
        return hasFlag(BridgeKinds.CLASS_ANNOTATION);
    }

    @Override
    public boolean isEnum() {
        return hasFlag(BridgeKinds.CLASS_ENUM);
    }

    @Override
    public boolean isRecord() {
        return hasFlag(BridgeKinds.CLASS_RECORD);
    }

    @Override
    public List<? extends IntrospectField> enumConstants() {
        if (enumConstants == null) {
            enumConstants = env.elements(remoteClass.enumConstants());
        }
        return enumConstants;
    }

    @Override
    public IntrospectClass<?> componentType() {
        return (IntrospectClass<?>) env.element(remoteClass.componentType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public IntrospectClass<T[]> arrayType() {
        return (IntrospectClass<T[]>) env.element(remoteClass.arrayType());
    }

    @Override
    public String name() {
        if (name == null) {
            name = remoteClass.name();
        }
        return name;
    }

    @Override
    public int modifiers() {
        if (modifiers < 0) {
            modifiers = remoteClass.modifiers();
        }
        return modifiers;
    }

    @Override
    public String simpleName() {
        if (simpleName == null) {
            simpleName = remoteClass.simpleName();
        }
        return simpleName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IntrospectClass<? super T> superclass() {
        if ((flags & SUPERCLASS_RESOLVED) == 0) {
            superclass = (IntrospectClass<? super T>) env.element(remoteClass.superclass());
            flags |= SUPERCLASS_RESOLVED;
        }
        return superclass;
    }

    @Override
    public List<? extends IntrospectClass<? super T>> interfaces() {
        if (interfaces == null) {
            interfaces = env.elements(remoteClass.interfaces());
        }
        return interfaces;
    }

    @Override
    public <U> IntrospectClass<U> asSubclass(Class<U> cls) {
        var other = env.findClass(cls);
        if (other == null) {
            throw new IllegalArgumentException(cls.getName() + " is not subclass of " + name());
        }
        return asSubclass(other);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> IntrospectClass<U> asSubclass(IntrospectClass<U> cls) {
        if (!cls.isAssignableFrom(this)) {
            throw new IllegalArgumentException(cls.name() + " is not subclass of " + name());
        }
        return (IntrospectClass<U>) this;
    }

    @Override
    public boolean isAssignableFrom(IntrospectClass<?> cls) {
        if (cls == this) {
            return true;
        }
        var classes = env.remoteClasses(new IntrospectClass<?>[] { cls });
        return remoteClass.isAssignableFrom(classes[0]);
    }

    @Override
    public boolean isAssignableFrom(Class<?> cls) {
        var other = env.findClass(cls);
        return other != null && isAssignableFrom(other);
    }

    @Override
    public List<? extends IntrospectMethod> declaredMethods() {
        if (declaredMethods == null) {
            declaredMethods = env.elements(remoteClass.declaredMethods());
        }
        return declaredMethods;
    }

    @Override
    public List<? extends IntrospectMethod> methods() {
        if (methods == null) {
            methods = env.elements(remoteClass.methods());
        }
        return methods;
    }

    @Override
    public IntrospectMethod declaredMethod(String name, IntrospectClass<?>... parameterTypes) {
        return (IntrospectMethod) env.element(remoteClass.declaredMethod(name,
                env.remoteClasses(parameterTypes)));
    }

    @Override
    public IntrospectMethod declaredJavaMethod(String name, Class<?>... parameterTypes) {
        var types = mapClasses(parameterTypes);
        return types != null ? declaredMethod(name, types) : null;
    }

    @Override
    public IntrospectMethod method(String name, IntrospectClass<?>... parameterTypes) {
        return (IntrospectMethod) env.element(remoteClass.method(name, env.remoteClasses(parameterTypes)));
    }

    @Override
    public IntrospectMethod javaMethod(String name, Class<?>... parameterTypes) {
        var types = mapClasses(parameterTypes);
        return types != null ? method(name, types) : null;
    }

    private IntrospectClass<?>[] mapClasses(Class<?>[] classes) {
        var result = new IntrospectClass<?>[classes.length];
        for (var i = 0; i < classes.length; ++i) {
            result[i] = env.findClass(classes[i]);
            if (result[i] == null) {
                return null;
            }
        }
        return result;
    }

    @Override
    public List<? extends IntrospectField> declaredFields() {
        if (declaredFields == null) {
            declaredFields = env.elements(remoteClass.declaredFields());
        }
        return declaredFields;
    }

    @Override
    public List<? extends IntrospectField> fields() {
        if (fields == null) {
            fields = env.elements(remoteClass.fields());
        }
        return fields;
    }

    @Override
    public IntrospectField declaredField(String name) {
        return (IntrospectField) env.element(remoteClass.declaredField(name));
    }

    @Override
    public IntrospectField field(String name) {
        return (IntrospectField) env.element(remoteClass.field(name));
    }

    @Override
    public Class<T> asJavaClass() {
        throw new IllegalStateException("Don't call this method from compile domain");
    }

    @Override
    public List<? extends IntrospectTypeVariable> typeParameters() {
        if (typeParameters == null) {
            typeParameters = env.elements(remoteClass.typeParameters());
        }
        return typeParameters;
    }
}
