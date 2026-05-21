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
package org.teavm.extension.introspect;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

public class IntrospectClassImpl<T> extends IntrospectAnnotatedElementImpl implements IntrospectClass<T> {
    private static final int FLAG_RESOLVED = 1;
    private static final int FLAG_SUPERCLASS_RESOLVED = 2;

    public final ValueType type;
    public ClassReader classReader;
    private List<? extends IntrospectClass<T>> interfaces;
    private IntrospectClass<T> superclass;
    private List<? extends IntrospectField> declaredFields;
    private Map<String, IntrospectFieldImpl> declaredFieldByName;
    private List<? extends IntrospectField> fieldsCache;
    private Map<MethodDescriptor, IntrospectMethodImpl> methods;
    private List<? extends IntrospectMethod> declaredMethods;
    private Map<String, IntrospectMethodImpl> declaredMethodsBySignature;
    private List<? extends IntrospectMethod> methodsCache;
    private List<? extends IntrospectField> enumConstants;
    private List<? extends IntrospectTypeVariable> typeParameters;
    private int flags;

    IntrospectClassImpl(ValueType type, Introspection introspection) {
        super(introspection);
        this.type = type;
    }

    public Introspection introspection() {
        return introspection;
    }

    @Override
    public boolean isPrimitive() {
        return type instanceof ValueType.Primitive || type == ValueType.VOID;
    }

    @Override
    public boolean isInterface() {
        resolve();
        return classReader != null && classReader.readModifiers().contains(ElementModifier.INTERFACE);
    }

    @Override
    public boolean isArray() {
        return type instanceof ValueType.Array;
    }

    @Override
    public boolean isAnnotation() {
        resolve();
        return classReader != null && classReader.readModifiers().contains(ElementModifier.ANNOTATION);
    }

    @Override
    public boolean isEnum() {
        resolve();
        return classReader != null && classReader.readModifiers().contains(ElementModifier.ENUM);
    }

    @Override
    public boolean isRecord() {
        resolve();
        return classReader != null && classReader.readModifiers().contains(ElementModifier.RECORD);
    }

    @Override
    public List<? extends IntrospectField> enumConstants() {
        var result = enumConstants;
        if (result == null) {
            resolve();
            if (classReader == null || !classReader.hasModifier(ElementModifier.ENUM)) {
                result = Collections.emptyList();
            } else {
                var constants = new ArrayList<IntrospectField>();
                for (var field : declaredFields()) {
                    if (field.isEnumConstant()) {
                        constants.add(field);
                    }
                }
                result = Collections.unmodifiableList(constants);
            }
            enumConstants = result;
        }
        return result;
    }

    @Override
    public int modifiers() {
        resolve();
        if (classReader == null) {
            return 0;
        }
        return Introspection.getModifiers(classReader);
    }

    @Override
    public IntrospectClass<?> componentType() {
        if (!(type instanceof ValueType.Array)) {
            return null;
        }
        ValueType componentType = ((ValueType.Array) type).getItemType();
        return introspection.getClass(componentType);
    }

    @Override
    public String name() {
        if (type instanceof ValueType.Object) {
            return ((ValueType.Object) type).getClassName();
        } else if (type instanceof ValueType.Void) {
            return "void";
        } else if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return "boolean";
                case BYTE:
                    return "byte";
                case SHORT:
                    return "short";
                case CHARACTER:
                    return "char";
                case INTEGER:
                    return "int";
                case LONG:
                    return "long";
                case FLOAT:
                    return "float";
                case DOUBLE:
                    return "double";
                default:
                    return "";
            }
        } else if (type instanceof ValueType.Array) {
            return type.toString().replace('/', '.');
        } else {
            return "";
        }
    }

    @Override
    public String simpleName() {
        if (type instanceof ValueType.Object) {
            resolve();
            if (classReader != null) {
                if (classReader.getSimpleName() != null) {
                    return classReader.getSimpleName();
                }
            }
            var name = ((ValueType.Object) type).getClassName();
            var lastIndex = name.lastIndexOf('$');
            if (lastIndex < 0) {
                lastIndex = name.lastIndexOf('.');
            }
            return name.substring(lastIndex + 1);
        } else if (type instanceof ValueType.Void) {
            return "void";
        } else if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return "boolean";
                case BYTE:
                    return "byte";
                case SHORT:
                    return "short";
                case CHARACTER:
                    return "char";
                case INTEGER:
                    return "int";
                case LONG:
                    return "long";
                case FLOAT:
                    return "float";
                case DOUBLE:
                    return "double";
                default:
                    return "";
            }
        } else if (type instanceof ValueType.Array) {
            return introspection.getClass(((ValueType.Array) type).getItemType()).simpleName() + "[]";
        } else {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public IntrospectClass<? super T> superclass() {
        resolve();
        var result = superclass;
        if ((flags & FLAG_SUPERCLASS_RESOLVED) == 0) {
            if (classReader == null || classReader.getParent() == null
                    || classReader.getName().equals(classReader.getParent())) {
                result = null;
            } else {
                result = (IntrospectClass<T>) introspection.getClass(new ValueType.Object(classReader.getParent()));
            }
            superclass = result;
            flags |= FLAG_SUPERCLASS_RESOLVED;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends IntrospectClass<? super T>> interfaces() {
        resolve();
        var result = interfaces;
        if (result == null) {
            if (classReader == null) {
                result = Collections.emptyList();
            } else {
                result = classReader.getInterfaces().stream()
                        .map(iface -> (IntrospectClass<T>) introspection.getClass(new ValueType.Object(iface)))
                        .collect(Collectors.toUnmodifiableList());
            }
            interfaces = result;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> IntrospectClass<U> asSubclass(IntrospectClass<U> cls) {
        if (!cls.isAssignableFrom(this)) {
            throw new IllegalArgumentException(cls.name() + " is not subclass of " + name());
        }
        return (IntrospectClass<U>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> IntrospectClass<U> asSubclass(Class<U> cls) {
        var reflectClass = introspection.findClass(cls);
        if (!reflectClass.isAssignableFrom(this)) {
            throw new IllegalArgumentException(cls.getName() + " is not subclass of " + name());
        }
        return (IntrospectClass<U>) this;
    }

    @Override
    public boolean isAssignableFrom(IntrospectClass<?> cls) {
        return cls == this
                || cls.superclass() != null && this.isAssignableFrom(cls.superclass())
                || cls.interfaces().stream().anyMatch(this::isAssignableFrom);
    }

    @Override
    public boolean isAssignableFrom(Class<?> cls) {
        return isAssignableFrom(introspection.findClass(cls));
    }

    @Override
    public List<? extends IntrospectMethod> declaredMethods() {
        resolve();
        var result = declaredMethods;
        if (result == null) {
            if (classReader == null) {
                result = Collections.emptyList();
            } else {
                result = classReader.getMethods().stream()
                        .filter(method -> !method.getName().equals("<clinit>"))
                        .map(method -> declaredMethod(method.getDescriptor()))
                        .collect(Collectors.toUnmodifiableList());
            }
            declaredMethods = result;
        }
        return result;
    }

    @Override
    public List<? extends IntrospectMethod> methods() {
        resolve();
        var result = methodsCache;
        if (result == null) {
            if (classReader == null) {
                result = Collections.emptyList();
            } else {
                Set<String> visited = new HashSet<>();
                result = introspection.getClassSource().getAncestors(classReader.getName())
                        .flatMap(cls -> cls.getMethods().stream())
                        .filter(method -> !method.getName().equals("<clinit>"))
                        .filter(method -> visited.add(method.getDescriptor().toString()))
                        .map(method -> introspection.getClass(ValueType.object(method.getOwnerName()))
                                .declaredMethod(method.getDescriptor()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableList());
            }
            methodsCache = result;
        }
        return result;
    }

    @Override
    public IntrospectMethod declaredMethod(String name, IntrospectClass<?>... parameterTypes) {
        resolve();
        if (classReader == null) {
            return null;
        }

        ValueType[] internalParameterTypes = Arrays.stream(parameterTypes)
                .map(type -> ((IntrospectClassImpl<?>) type).type)
                .toArray(ValueType[]::new);
        String key = name + "(" + ValueType.manyToString(internalParameterTypes) + ")";
        if (declaredMethods == null) {
            declaredMethodsBySignature = new LinkedHashMap<>();
        }
        return declaredMethodsBySignature.computeIfAbsent(key, k -> {
            MethodReader candidate = null;
            for (var method : classReader.getMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (!Arrays.equals(method.getParameterTypes(), internalParameterTypes)) {
                    continue;
                }
                if (candidate == null) {
                    candidate = method;
                } else {
                    boolean moreSpecial = introspection.getHierarchy().isSuperType(candidate.getResultType(),
                            method.getResultType(), false);
                    if (moreSpecial) {
                        candidate = method;
                    }
                }
            }

            return candidate != null ? declaredMethod(candidate.getDescriptor()) : null;
        });
    }

    public IntrospectMethodImpl declaredMethod(MethodDescriptor method) {
        resolve();
        if (methods == null) {
            methods = new HashMap<>();
        }
        return methods.computeIfAbsent(method, m -> {
            MethodReader methodReader = classReader.getMethod(m);
            return methodReader != null ? new IntrospectMethodImpl(this, methodReader) : null;
        });
    }

    @Override
    public IntrospectMethod declaredJavaMethod(String name, Class<?>... parameterTypes) {
        var mappedParamTypes = Arrays.stream(parameterTypes)
                .map(introspection::findClass)
                .toArray(IntrospectClass[]::new);
        return declaredMethod(name, mappedParamTypes);
    }

    @Override
    public IntrospectMethod javaMethod(String name, Class<?>... parameterTypes) {
        var mappedParamTypes = Arrays.stream(parameterTypes)
                .map(introspection::findClass)
                .toArray(IntrospectClass[]::new);
        return method(name, mappedParamTypes);
    }

    @Override
    public IntrospectMethod method(String name, IntrospectClass<?>... parameterTypes) {
        resolve();
        if (classReader == null) {
            return null;
        }

        Iterable<ClassReader> ancestors = () -> introspection.getClassSource().getAncestors(classReader.getName())
                .iterator();
        for (var cls : ancestors) {
            var reflectClass = introspection.getClass(ValueType.object(cls.getName()));
            var method = reflectClass.declaredMethod(name, parameterTypes);
            if (method != null && Modifier.isPublic(method.modifiers())) {
                return method;
            }
        }
        return null;
    }

    @Override
    public List<? extends IntrospectField> declaredFields() {
        resolve();
        var result = declaredFields;
        if (result == null) {
            if (classReader == null) {
                result = Collections.emptyList();
            } else {
                result = classReader.getFields().stream()
                        .map(fld -> declaredField(fld.getName()))
                        .collect(Collectors.toUnmodifiableList());
            }
            declaredFields = result;
        }
        return result;
    }

    @Override
    public List<? extends IntrospectField> fields() {
        resolve();
        var result = fieldsCache;
        if (fieldsCache == null) {
            if (classReader == null) {
                result = Collections.emptyList();
            } else {
                Set<String> visited = new HashSet<>();
                result = introspection
                    .getClassSource()
                    .getAncestors(classReader.getName())
                    .flatMap(cls -> cls.getFields().stream().filter(fld -> fld.getLevel() == AccessLevel.PUBLIC))
                    .filter(fld -> visited.add(fld.getName()))
                    .map(fld -> introspection.getClass(ValueType.object(fld.getOwnerName()))
                            .declaredField(fld.getName()))
                    .collect(Collectors.toUnmodifiableList());
            }
            fieldsCache = result;
        }
        return result;
    }


    @Override
    public IntrospectField declaredField(String name) {
        resolve();
        if (declaredFieldByName == null) {
            declaredFieldByName = new HashMap<>();
        }
        return declaredFieldByName.computeIfAbsent(name, n -> {
            FieldReader fld = classReader.getField(n);
            return fld != null ? new IntrospectFieldImpl(this, fld) : null;
        });
    }

    @Override
    public IntrospectField field(String name) {
        resolve();
        if (classReader == null) {
            return null;
        }
        var cls = classReader;
        while (cls != null) {
            FieldReader fieldReader = classReader.getField(name);
            if (fieldReader != null && fieldReader.getLevel() == AccessLevel.PUBLIC) {
                return declaredField(name);
            }
            cls = cls.getParent() != null ? introspection.getClassSource().get(cls.getParent()) : null;
        }
        return null;
    }

    @Override
    protected AnnotationContainerReader annotationContainer() {
        resolve();
        return classReader != null ? classReader.getAnnotations() : null;
    }

    public void resolve() {
        if ((flags & FLAG_RESOLVED) != 0) {
            return;
        }
        flags |= FLAG_RESOLVED;
        if (!(type instanceof ValueType.Object)) {
            return;
        }

        String className = ((ValueType.Object) type).getClassName();
        classReader = introspection.getClassSource().get(className);
    }

    @Override
    public String toString() {
        if (isArray()) {
            return componentType().toString() + "[]";
        } else {
            return name();
        }
    }

    @Override
    public List<? extends IntrospectTypeVariable> typeParameters() {
        if (typeParameters == null) {
            resolve();
            if (classReader == null) {
                typeParameters = Collections.emptyList();
            } else {
                GenericTypeParameter[] params = classReader.getGenericParameters();
                if (params == null || params.length == 0) {
                    typeParameters = Collections.emptyList();
                } else {
                    var result = new ArrayList<IntrospectTypeVariableImpl>(params.length);
                    for (var param : params) {
                        result.add(new IntrospectTypeVariableImpl(param, this, null, introspection));
                    }
                    typeParameters = Collections.unmodifiableList(result);
                }
            }
        }
        return typeParameters;
    }

    IntrospectTypeVariableImpl typeParameter(String name) {
        for (var tp : typeParameters()) {
            if (tp.name().equals(name)) {
                return (IntrospectTypeVariableImpl) tp;
            }
        }
        return null;
    }

    @Override
    public Class<T> asJavaClass() {
        throw new IllegalStateException("Don't call this method from compile domain");
    }
}
