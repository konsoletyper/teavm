/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.classlib.java.lang.annotation.TAnnotation;
import org.teavm.classlib.java.lang.reflect.TConstructor;
import org.teavm.classlib.java.lang.reflect.TField;
import org.teavm.classlib.java.lang.reflect.TGenericDeclaration;
import org.teavm.classlib.java.lang.reflect.TMethod;
import org.teavm.classlib.java.lang.reflect.TModifier;
import org.teavm.classlib.java.lang.reflect.TType;
import org.teavm.classlib.java.lang.reflect.TTypeVariable;
import org.teavm.classlib.java.lang.reflect.TTypeVariableImpl;
import org.teavm.dependency.PluggableDependency;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ModifiersInfo;

public final class TClass<T> extends TObject implements TGenericDeclaration, TType {
    static class StatFlags {
        private static final int NAME_INITIALIZED = 1;
        private static final int SIMPLE_NAME_INITIALIZED = 1 << 1;
        private static final int CANONICAL_NAME_INITIALIZED = 1 << 2;
    }

    private static Map<String, TClass<?>> nameMap;
    private int flags;
    private ClassInfo classInfo;
    private String name;
    private String simpleName;
    private String canonicalName;
    private TClass<?>[] interfaces;
    private TAnnotation[] annotations;
    private TAnnotation[] declaredAnnotations;
    private Map<TClass<?>, TAnnotation> annotationsByType;
    private TField[] declaredFields;
    private TField[] fields;
    private TConstructor<T>[] declaredConstructors;
    private TMethod[] declaredMethods;
    private TTypeVariable<?>[] typeParameters;

    private TClass(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    static TClass<?> createClass(ClassInfo classInfo) {
        return new TClass<>(classInfo);
    }

    @Override
    public String toString() {
        return (isInterface() ? "interface " : (isPrimitive() ? "" : "class ")) + getName();
    }

    private String obfuscatedToString() {
        return "javaClass@" + identity();
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public boolean isInstance(TObject obj) {
        return obj != null && classInfo.isSuperTypeOf(obj.getClass0().getClassInfo());
    }

    public boolean isAssignableFrom(TClass<?> obj) {
        return classInfo.isSuperTypeOf(obj.classInfo);
    }

    public String getName() {
        if ((flags & StatFlags.NAME_INITIALIZED) == 0) {
            flags |= StatFlags.NAME_INITIALIZED;
            var metadataName = classInfo.name();
            var result = metadataName != null ? metadataName.getStringObject() : null;
            if (result == null) {
                var itemType = classInfo.itemType();
                if (itemType != null) {
                    var itemName = itemType.classObject().getName();
                    if (itemName != null) {
                        result = itemType.itemType() != null ? "[" + itemName : "[L" + itemName + ";";
                    }
                }
            }
            name = result;
        }
        return name;
    }

    public String getSimpleName() {
        if ((flags & StatFlags.SIMPLE_NAME_INITIALIZED) != 0) {
            flags |= StatFlags.SIMPLE_NAME_INITIALIZED;
            var metadataName = classInfo.simpleName();
            var result = metadataName != null ? metadataName.getStringObject() : null;
            if (result == null) {
                if (classInfo.itemType() != null) {
                    result = classInfo.itemType().classObject().getSimpleName() + "[]";
                } else if (classInfo.enclosingClass() == null) {
                    var name = getName();
                    int lastDollar = name.lastIndexOf('$');
                    if (lastDollar != -1) {
                        name = name.substring(lastDollar + 1);
                        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
                            name = "";
                        }
                    } else {
                        int lastDot = name.lastIndexOf('.');
                        if (lastDot != -1) {
                            name = name.substring(lastDot + 1);
                        }
                    }
                    result = name;
                }
            }
            simpleName = result;
        }
        return simpleName;
    }

    public String getCanonicalName() {
        if ((flags & StatFlags.CANONICAL_NAME_INITIALIZED) != 0) {
            flags |= StatFlags.CANONICAL_NAME_INITIALIZED;
            if (classInfo.itemType() != null) {
                String componentName = classInfo.itemType().classObject().getCanonicalName();
                if (componentName != null) {
                    canonicalName = componentName + "[]";
                }
            } else if (classInfo.enclosingClass() != null) {
                if (classInfo.declaringClass() != null && !isSynthetic()) {
                    var enclosingName = classInfo.declaringClass().classObject().getCanonicalName();
                    if (enclosingName != null) {
                        canonicalName = enclosingName + "." + getSimpleName();
                    }
                }
            } else {
                canonicalName = getSimpleName();
            }
        }
        return canonicalName;
    }

    private boolean isSynthetic() {
        return (classInfo.modifiers() & ModifiersInfo.SYNTHETIC) != 0;
    }

    public boolean isPrimitive() {
        return classInfo.primitiveKind() != ClassInfo.PrimitiveKind.NOT;
    }

    public boolean isArray() {
        return classInfo.itemType() != null;
    }

    public boolean isEnum() {
        return (classInfo.modifiers() & ModifiersInfo.ENUM) != 0;
    }

    public boolean isInterface() {
        return (classInfo.modifiers() & ModifiersInfo.INTERFACE) != 0;
    }
    
    public boolean isAnnotation() {
        return (classInfo.modifiers() & ModifiersInfo.ANNOTATION) != 0;
    }

    public boolean isLocalClass() {
        return (classInfo.modifiers() & ModifiersInfo.SYNTHETIC) != 0 && classInfo.enclosingClass() != null;
    }

    public boolean isMemberClass() {
        return getDeclaringClass() != null;
    }

    public TClass<?> getComponentType() {
        return (TClass<?>) (Object) classInfo.itemType().classObject();
    }

    public TField[] getDeclaredFields() throws TSecurityException {
        if (declaredFields == null) {
            var reflection = classInfo.reflection();
            if (reflection == null) {
                declaredFields = new TField[0];
            } else {
                var count = reflection.fieldCount();
                declaredFields = new TField[count];
                for (int i = 0; i < count; ++i) {
                    declaredFields[i] = new TField(this, reflection.field(i));
                }
            }
        }
        return declaredFields.clone();
    }

    public TField[] getFields() throws TSecurityException {
        if (isPrimitive() || isArray()) {
            return new TField[0];
        }

        if (fields == null) {
            List<TField> fieldList = new ArrayList<>();
            TClass<?> cls = this;

            if (cls.isInterface()) {
                getFieldsOfInterfaces(cls, fieldList, new HashSet<>());
            } else {
                while (cls != null) {
                    for (TField field : cls.getDeclaredFields()) {
                        if (Modifier.isPublic(field.getModifiers())) {
                            fieldList.add(field);
                        }
                    }
                    cls = cls.getSuperclass();
                }
            }

            fields = fieldList.toArray(new TField[fieldList.size()]);
        }
        return fields.clone();
    }

    public TField getDeclaredField(String name) throws TNoSuchFieldException {
        for (TField field : getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        throw new TNoSuchFieldException();
    }

    public TField getField(String name) throws TNoSuchFieldException {
        TField result = findField(name, new HashSet<>());
        if (result == null) {
            throw new TNoSuchFieldException();
        }
        return result;
    }

    private TField findField(String name, Set<String> visited) {
        if (!visited.add(name)) {
            return null;
        }

        for (TField field : getDeclaredFields()) {
            if (TModifier.isPublic(field.getModifiers()) && field.getName().equals(name)) {
                return field;
            }
        }

        for (TClass<?> iface : getInterfaces()) {
            TField field = iface.findField(name, visited);
            if (field != null) {
                return field;
            }
        }

        TClass<?> superclass = getSuperclass();
        if (superclass != null) {
            TField field = superclass.findField(name, visited);
            if (field != null) {
                return field;
            }
        }

        return null;
    }

    @SuppressWarnings({ "raw", "unchecked" })
    public TConstructor<?>[] getDeclaredConstructors() throws TSecurityException {
        if (declaredConstructors == null) {
            var reflection = classInfo.reflection();
            if (reflection == null) {
                declaredConstructors = new TConstructor[0];
            } else {
                var total = reflection.methodCount();
                var count = 0;
                for (var i = 0; i < total; ++i) {
                    if (reflection.method(i).name().getStringObject().equals("<init>")) {
                        ++count;
                    }
                }
                declaredConstructors = new TConstructor[count];
                var j = 0;
                for (var i = 0; i < total; ++i) {
                    var info = reflection.method(i);
                    if (!info.name().getStringObject().equals("<init>")) {
                        continue;
                    }
                    declaredConstructors[j++] = new TConstructor<>(classInfo, info);
                }
            }
        }
        return declaredConstructors.clone();
    }

    public TConstructor<?>[] getConstructors() throws TSecurityException {
        TConstructor<?>[] declaredConstructors = getDeclaredConstructors();
        TConstructor<?>[] constructors = new TConstructor<?>[declaredConstructors.length];

        int sz = 0;
        for (TConstructor<?> constructor : declaredConstructors) {
            if (TModifier.isPublic(constructor.getModifiers())) {
                constructors[sz++] = constructor;
            }
        }

        if (sz < constructors.length) {
            constructors = Arrays.copyOf(constructors, sz);
        }

        return constructors;
    }

    @SuppressWarnings({ "raw", "unchecked" })
    public TConstructor<T> getDeclaredConstructor(TClass<?>... parameterTypes)
            throws TSecurityException, TNoSuchMethodException {
        for (TConstructor<?> constructor : getDeclaredConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(), parameterTypes)) {
                return (TConstructor<T>) constructor;
            }
        }
        throw new TNoSuchMethodException();
    }

    @SuppressWarnings({ "raw", "unchecked" })
    public TConstructor<T> getConstructor(TClass<?>... parameterTypes)
            throws TSecurityException, TNoSuchMethodException {
        for (TConstructor<?> constructor : getDeclaredConstructors()) {
            if (TModifier.isPublic(constructor.getModifiers())
                    && Arrays.equals(constructor.getParameterTypes(), parameterTypes)) {
                return (TConstructor<T>) constructor;
            }
        }
        throw new TNoSuchMethodException();
    }

    private static void getFieldsOfInterfaces(TClass<?> iface, List<TField> fields, Set<TClass<?>> visited) {
        if (!visited.add(iface)) {
            return;
        }
        for (TField field : iface.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())) {
                fields.add(field);
            }
        }
        for (TClass<?> superInterface : iface.getInterfaces()) {
            getFieldsOfInterfaces(superInterface, fields, visited);
        }
    }

    public TMethod[] getDeclaredMethods() {
        if (declaredMethods == null) {
            var reflection = classInfo.reflection();
            if (reflection == null) {
                declaredMethods = new TMethod[0];
            } else {
                var total = reflection.methodCount();
                var count = 0;
                for (var i = 0; i < total; ++i) {
                    if (!reflection.method(i).name().getStringObject().equals("<init>")) {
                        ++count;
                    }
                }
                declaredMethods = new TMethod[count];
                var j = 0;
                for (var i = 0; i < total; ++i) {
                    var info = reflection.method(i);
                    if (info.name().getStringObject().equals("<init>")) {
                        continue;
                    }
                    declaredMethods[j++] = new TMethod(classInfo, info);
                }
            }
        }
        return declaredMethods.clone();
    }

    public TMethod getDeclaredMethod(String name, TClass<?>... parameterTypes) throws TNoSuchMethodException,
            TSecurityException {
        TMethod bestFit = null;
        for (TMethod method : getDeclaredMethods()) {
            if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                if (bestFit == null || bestFit.getReturnType().isAssignableFrom(method.getReturnType())) {
                    bestFit = method;
                }
            }
        }
        if (bestFit == null) {
            throw new TNoSuchMethodException();
        }
        return bestFit;
    }

    public TMethod[] getMethods() throws TSecurityException {
        Map<MethodSignature, TMethod> methods = new HashMap<>();
        findMethods(this, methods);
        return methods.values().toArray(new TMethod[methods.size()]);
    }

    public TMethod getMethod(String name, TClass<?>... parameterTypes) throws TNoSuchMethodException,
            TSecurityException {
        TMethod method = findMethod(this, null, name, parameterTypes);
        if (method == null) {
            throw new TNoSuchMethodException();
        }
        return method;
    }

    private static void findMethods(TClass<?> cls, Map<MethodSignature, TMethod> methods) {
        for (TMethod method : cls.getDeclaredMethods()) {
            if (TModifier.isPublic(method.getModifiers())) {
                MethodSignature signature = new MethodSignature(method.getName(), method.getParameterTypes(),
                        method.getReturnType());
                if (!methods.containsKey(signature)) {
                    methods.put(signature, method);
                }
            }
        }

        if (!cls.isInterface()) {
            TClass<?> superclass = cls.getSuperclass();
            if (superclass != null) {
                findMethods(superclass, methods);
            }
        }

        for (TClass<?> iface : cls.getInterfaces()) {
            findMethods(iface, methods);
        }
    }

    private static TMethod findMethod(TClass<?> cls, TMethod current, String name, TClass<?>[] parameterTypes) {
        for (TMethod method : cls.getDeclaredMethods()) {
            if (TModifier.isPublic(method.getModifiers()) && method.getName().equals(name)
                    && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                if (current == null || current.getReturnType().isAssignableFrom(method.getReturnType())) {
                    current = method;
                }
            }
        }

        if (!cls.isInterface()) {
            TClass<?> superclass = cls.getSuperclass();
            if (superclass != null) {
                current = findMethod(superclass, current, name, parameterTypes);
            }
        }

        for (TClass<?> iface : cls.getInterfaces()) {
            current = findMethod(iface, current, name, parameterTypes);
        }

        return current;
    }

    private static final class MethodSignature {
        private final String name;
        private final TClass<?>[] parameterTypes;
        private final TClass<?> returnType;

        MethodSignature(String name, TClass<?>[] parameterTypes, TClass<?> returnType) {
            this.name = name;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MethodSignature)) {
                return false;
            }
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(name, that.name) && Arrays.equals(parameterTypes, that.parameterTypes)
                    && Objects.equals(returnType, that.returnType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, Arrays.hashCode(parameterTypes), returnType);
        }
    }

    public int getModifiers() {
        return classInfo.modifiers() & ModifiersInfo.JVM_FLAGS_MASK;
    }

    public boolean desiredAssertionStatus() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @PluggableDependency(ClassGenerator.class)
    public TClass<? super T> getSuperclass() {
        return classInfo.parent() != null
                ? (TClass<? super T>) (Object) classInfo.parent().classObject()
                : null;
    }

    @SuppressWarnings("unchecked")
    @PluggableDependency(ClassGenerator.class)
    public TClass<? super T>[] getInterfaces() {
        if (interfaces == null) {
            interfaces = new TClass<?>[classInfo.superinterfaceCount()];
            for (int i = 0; i < interfaces.length; ++i) {
                interfaces[i] = (TClass<? super T>) (Object) classInfo.superinterface(i).classObject();
            }
        }
        return (TClass<? super T>[]) interfaces;
    }

    @SuppressWarnings("unchecked")
    public T[] getEnumConstants() {
        if (!isEnum()) {
            return null;
        }

        var count = classInfo.enumConstantCount();
        var data = classInfo.arrayType().newArrayInstance(count);
        for (var i = 0; i < count; ++i) {
            classInfo.putItem(data, i, classInfo.enumConstant(i));
        }
        return (T[]) data;
    }

    @SuppressWarnings("unchecked")
    public T cast(TObject obj) {
        if (obj != null && !isAssignableFrom((TClass<?>) (Object) obj.getClass())) {
            throw new TClassCastException(obj.getClass().getName() + " is not subtype of " + getName());
        }
        return (T) obj;
    }

    public TClassLoader getClassLoader() {
        return TClassLoader.getSystemClassLoader();
    }

    public static TClass<?> forName(TString name) throws TClassNotFoundException {
        if (nameMap == null) {
            fillNameMap();
        }
        var result = nameMap.get((String) (Object) name);
        if (result == null) {
            throw new TClassNotFoundException((String) (Object) name);
        }
        return result;
    }

    private static void fillNameMap() {
        nameMap = new HashMap<>();
        ClassInfo.rewind();
        while (ClassInfo.hasNext()) {
            var cls = ClassInfo.next();
            var name = cls.name();
            if (name != null) {
                nameMap.put(name.getStringObject(), (TClass<?>) (Object) cls.classObject());
            }
        }
    }

    @SuppressWarnings("unused")
    public static TClass<?> forName(TString name, boolean initialize, TClassLoader loader)
            throws TClassNotFoundException {
        return forName(name);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    public T newInstance() throws TInstantiationException, TIllegalAccessException {
        var instance = classInfo.newInstance();
        if (instance == null) {
            throw new TInstantiationException();
        }
        classInfo.initializeNewInstance(instance);
        return (T) instance;
    }

    public TClass<?> getDeclaringClass() {
        var declaringClass = classInfo.declaringClass();
        return declaringClass != null ? (TClass<?>) (Object) declaringClass.classObject() : null;
    }

    public TClass<?> getEnclosingClass() {
        var enclosingClass = classInfo.enclosingClass();
        return enclosingClass != null ? (TClass<?>) (Object) enclosingClass.classObject() : null;
    }

    @SuppressWarnings("unchecked")
    public <U> TClass<? extends U> asSubclass(TClass<U> clazz) {
        if (!clazz.isAssignableFrom(this)) {
            throw new TClassCastException();
        }
        return (TClass<? extends U>) this;
    }

    @Override
    public boolean isAnnotationPresent(TClass<? extends TAnnotation> annotationClass) {
        ensureAnnotationsByType();
        return annotationsByType.containsKey(annotationClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends TAnnotation> S getAnnotation(TClass<S> annotationClass) {
        ensureAnnotationsByType();
        return (S) annotationsByType.get(annotationClass);
    }

    @Override
    public TAnnotation[] getAnnotations() {
        if (annotations == null) {
            TClass<?> cls = this;
            var initial = true;
            var map = new LinkedHashMap<Class<?>, TAnnotation>();
            while (cls != null) {
                for (var annot : cls.getDeclaredAnnotations()) {
                    if (initial || isInherited(annot)) {
                        map.putIfAbsent(annot.annotationType(), annot);
                    }
                }
                cls = cls.getSuperclass();
                initial = false;
            }
            annotations = map.values().toArray(new TAnnotation[0]);
        }
        return annotations.clone();
    }

    private static boolean isInherited(TAnnotation annot) {
        var modifiers = ((TClass<?>) (Object) annot.annotationType()).classInfo.modifiers();
        return (modifiers & ModifiersInfo.INHERITED_ANNOTATION) != 0;
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        if (declaredAnnotations == null) {
            var reflection = classInfo.reflection();
            if (reflection == null) {
                declaredAnnotations = new TAnnotation[0];
            } else {
                var count = reflection.annotationCount();
                declaredAnnotations = new TAnnotation[count];
                for (var i = 0; i < count; ++i) {
                    declaredAnnotations[i] = (TAnnotation) reflection.annotation(i).createObject();
                }
            }
        }
        return declaredAnnotations.clone();
    }

    private void ensureAnnotationsByType() {
        if (annotationsByType != null) {
            return;
        }
        annotationsByType = new HashMap<>();
        for (TAnnotation annot : getAnnotations()) {
            annotationsByType.put((TClass<?>) (Object) annot.annotationType(), annot);
        }
    }

    public InputStream getResourceAsStream(String name) {
        if (name.startsWith("/")) {
            return getClassLoader().getResourceAsStream(name.substring(1));
        }

        TClass<?> cls = this;
        while (cls.isArray()) {
            cls = cls.getComponentType();
        }
        String prefix = cls.getName().toString();
        int index = prefix.lastIndexOf('.');
        if (index >= 0) {
            name = prefix.substring(0, index + 1).replace('.', '/') + name;
        }

        return getClassLoader().getResourceAsStream(name);
    }

    public TPackage getPackage() {
        String name = getName();
        name = name.substring(0, name.lastIndexOf('.') + 1);
        return TPackage.getPackage(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TTypeVariable<TClass<T>>[] getTypeParameters() {
        if (typeParameters == null) {
            var reflection = classInfo.reflection();
            if (reflection == null) {
                typeParameters = new TTypeVariable<?>[0];
            } else {
                typeParameters = new TTypeVariable<?>[reflection.typeParameterCount()];
                for (var i = 0; i < typeParameters.length; ++i) {
                    typeParameters[i] = new TTypeVariableImpl(this, reflection.typeParameter(i));
                }
            }
        }
        return (TTypeVariable<TClass<T>>[]) typeParameters.clone();
    }

    @Override
    public String getTypeName() {
        return isArray()
            ? getComponentType().getTypeName() + "[]"
            : getName();
    }
}
