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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.impl.reflection.Flags;
import org.teavm.classlib.impl.reflection.JSClass;
import org.teavm.classlib.impl.reflection.JSField;
import org.teavm.classlib.impl.reflection.JSMethodMember;
import org.teavm.classlib.java.lang.annotation.TAnnotation;
import org.teavm.classlib.java.lang.reflect.TAnnotatedElement;
import org.teavm.classlib.java.lang.reflect.TConstructor;
import org.teavm.classlib.java.lang.reflect.TField;
import org.teavm.classlib.java.lang.reflect.TMethod;
import org.teavm.classlib.java.lang.reflect.TModifier;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.core.JSArray;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformClass;
import org.teavm.platform.PlatformSequence;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public class TClass<T> extends TObject implements TAnnotatedElement {
    String name;
    String simpleName;
    private PlatformClass platformClass;
    private TAnnotation[] annotationsCache;
    private Map<TClass<?>, TAnnotation> annotationsByType;
    private TField[] declaredFields;
    private TField[] fields;
    private TConstructor<T>[] declaredConstructors;
    private TMethod[] declaredMethods;
    private static boolean reflectionInitialized;

    private TClass(PlatformClass platformClass) {
        this.platformClass = platformClass;
        platformClass.setJavaClass(Platform.getPlatformObject(this));
    }

    public static TClass<?> getClass(PlatformClass cls) {
        if (cls == null) {
            return null;
        }
        TClass<?> result = (TClass<?>) (Object) Platform.asJavaClass(cls.getJavaClass());
        if (result == null) {
            result = new TClass<>(cls);
        }
        return result;
    }

    public PlatformClass getPlatformClass() {
        return platformClass;
    }

    @DelegateTo("isInstanceLowLevel")
    public boolean isInstance(TObject obj) {
        return Platform.isInstance(Platform.getPlatformObject(obj), platformClass);
    }

    @Unmanaged
    private boolean isInstanceLowLevel(RuntimeObject obj) {
        return obj != null && isAssignableFromLowLevel(RuntimeClass.getClass(obj));
    }

    @DelegateTo("isAssignableFromLowLevel")
    public boolean isAssignableFrom(TClass<?> obj) {
        return Platform.isAssignable(obj.getPlatformClass(), platformClass);
    }

    @Unmanaged
    private boolean isAssignableFromLowLevel(RuntimeClass other) {
        return Address.ofObject(this).<RuntimeClass>toStructure().isSupertypeOf.apply(other);
    }

    @Unmanaged
    public String getName() {
        if (PlatformDetector.isLowLevel()) {
            return Platform.getName(platformClass);
        } else {
            if (name == null) {
                name = Platform.getName(platformClass);
            }
            return name;
        }
    }

    public String getSimpleName() {
        String simpleName = getSimpleNameCache();
        if (simpleName == null) {
            if (isArray()) {
                simpleName = getComponentType().getSimpleName() + "[]";
                setSimpleNameCache(simpleName);
                return simpleName;
            }
            String name = Platform.getName(platformClass);
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
            simpleName = name;
            setSimpleNameCache(simpleName);
        }
        return simpleName;
    }

    @DelegateTo("getSimpleNameCacheLowLevel")
    private String getSimpleNameCache() {
        return simpleName;
    }

    @Unmanaged
    @PluggableDependency(ClassDependencyListener.class)
    private RuntimeObject getSimpleNameCacheLowLevel() {
        return Address.ofObject(this).<RuntimeClass>toStructure().simpleName;
    }

    @DelegateTo("setSimpleNameCacheLowLevel")
    private void setSimpleNameCache(String value) {
        simpleName = value;
    }

    @Unmanaged
    private void setSimpleNameCacheLowLevel(RuntimeObject object) {
        Address.ofObject(this).<RuntimeClass>toStructure().simpleName = object;
    }

    public boolean isPrimitive() {
        return Platform.isPrimitive(platformClass);
    }

    public boolean isArray() {
        return Platform.getArrayItem(platformClass) != null;
    }

    public boolean isEnum() {
        return Platform.isEnum(platformClass);
    }

    public boolean isInterface() {
        return (platformClass.getMetadata().getFlags() & Flags.INTERFACE) != 0;
    }

    @PluggableDependency(ClassGenerator.class)
    public TClass<?> getComponentType() {
        return getClass(Platform.getArrayItem(platformClass));
    }

    public TField[] getDeclaredFields() throws TSecurityException {
        if (isPrimitive() || isArray()) {
            return new TField[0];
        }
        if (declaredFields == null) {
            initReflection();
            JSClass jsClass = (JSClass) getPlatformClass().getMetadata();
            JSArray<JSField> jsFields = jsClass.getFields();
            declaredFields = new TField[jsFields.getLength()];
            for (int i = 0; i < jsFields.getLength(); ++i) {
                JSField jsField = jsFields.get(i);
                declaredFields[i] = new TField(this, jsField.getName(), jsField.getModifiers(),
                        jsField.getAccessLevel(), TClass.getClass(jsField.getType()), jsField.getGetter(),
                        jsField.getSetter());
            }
        }
        return declaredFields.clone();
    }

    private static void initReflection() {
        if (!reflectionInitialized) {
            reflectionInitialized = true;
            createMetadata();
        }
    }

    @GeneratedBy(ClassGenerator.class)
    @NoSideEffects
    private static native void createMetadata();

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
                    for (TField field : declaredFields) {
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

    public TField getDeclaredField(String name) throws TNoSuchFieldError {
        for (TField field : getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        throw new TNoSuchFieldError();
    }

    public TField getField(String name) throws TNoSuchFieldError {
        TField result = findField(name, new HashSet<>());
        if (result == null) {
            throw new TNoSuchFieldError();
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

    @InjectedBy(ClassGenerator.class)
    @PluggableDependency(ClassGenerator.class)
    public native <T> T newEmptyInstance();

    @SuppressWarnings({ "raw", "unchecked" })
    public TConstructor<?>[] getDeclaredConstructors() throws TSecurityException {
        if (isPrimitive() || isArray()) {
            return new TConstructor<?>[0];
        }

        if (declaredConstructors == null) {
            initReflection();
            JSClass jsClass = (JSClass) getPlatformClass().getMetadata();
            JSArray<JSMethodMember> jsMethods = jsClass.getMethods();
            declaredConstructors = new TConstructor[jsMethods.getLength()];
            int count = 0;
            for (int i = 0; i < jsMethods.getLength(); ++i) {
                JSMethodMember jsMethod = jsMethods.get(i);
                if (!jsMethod.getName().equals("<init>")) {
                    continue;
                }
                PlatformSequence<PlatformClass> jsParameterTypes = jsMethod.getParameterTypes();
                TClass<?>[] parameterTypes = new TClass<?>[jsParameterTypes.getLength()];
                for (int j = 0; j < parameterTypes.length; ++j) {
                    parameterTypes[j] = getClass(jsParameterTypes.get(j));
                }
                declaredConstructors[count++] = new TConstructor<T>(this, jsMethod.getName(), jsMethod.getModifiers(),
                        jsMethod.getAccessLevel(), parameterTypes, jsMethod.getCallable());
            }
            declaredConstructors = Arrays.copyOf(declaredConstructors, count);
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
        if (isPrimitive() || isArray()) {
            return new TMethod[0];
        }
        if (declaredMethods == null) {
            initReflection();
            JSClass jsClass = (JSClass) getPlatformClass().getMetadata();
            JSArray<JSMethodMember> jsMethods = jsClass.getMethods();
            declaredMethods = new TMethod[jsMethods.getLength()];
            int count = 0;
            for (int i = 0; i < jsMethods.getLength(); ++i) {
                JSMethodMember jsMethod = jsMethods.get(i);
                if (jsMethod.getName().equals("<init>") || jsMethod.getName().equals("<clinit>")) {
                    continue;
                }
                PlatformSequence<PlatformClass> jsParameterTypes = jsMethod.getParameterTypes();
                TClass<?>[] parameterTypes = new TClass<?>[jsParameterTypes.getLength()];
                for (int j = 0; j < parameterTypes.length; ++j) {
                    parameterTypes[j] = getClass(jsParameterTypes.get(j));
                }
                TClass<?> returnType = getClass(jsMethod.getReturnType());
                declaredMethods[count++] = new TMethod(this, jsMethod.getName(), jsMethod.getModifiers(),
                        jsMethod.getAccessLevel(), returnType, parameterTypes, jsMethod.getCallable());
            }
            declaredMethods = Arrays.copyOf(declaredMethods, count);
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
        int flags = platformClass.getMetadata().getFlags();
        int accessLevel = platformClass.getMetadata().getAccessLevel();
        return Flags.getModifiers(flags, accessLevel);
    }

    public boolean desiredAssertionStatus() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @PluggableDependency(ClassGenerator.class)
    public TClass<? super T> getSuperclass() {
        return (TClass<? super T>) getClass(platformClass.getMetadata().getSuperclass());
    }

    @SuppressWarnings("unchecked")
    @PluggableDependency(ClassGenerator.class)
    public TClass<? super T>[] getInterfaces() {
        PlatformSequence<PlatformClass> supertypes = platformClass.getMetadata().getSupertypes();

        TClass<? super T>[] filteredSupertypes = (TClass<? super T>[]) new TClass<?>[supertypes.getLength()];
        int j = 0;
        for (int i = 0; i < supertypes.getLength(); ++i) {
            if (supertypes.get(i) != platformClass.getMetadata().getSuperclass()) {
                filteredSupertypes[j++] = (TClass<? super T>) getClass(supertypes.get(j));
            }
        }

        if (filteredSupertypes.length > j) {
            filteredSupertypes = Arrays.copyOf(filteredSupertypes, j);
        }
        return filteredSupertypes;
    }

    @SuppressWarnings("unchecked")
    public T[] getEnumConstants() {
        if (!isEnum()) {
            return null;
        }
        Platform.initClass(platformClass);
        return (T[]) Platform.getEnumConstants(platformClass).clone();
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
        PlatformClass cls = Platform.lookupClass(name.toString());
        if (cls == null) {
            throw new TClassNotFoundException();
        }
        return getClass(cls);
    }

    @SuppressWarnings("unused")
    public static TClass<?> forName(TString name, boolean initialize, TClassLoader loader)
            throws TClassNotFoundException {
        return forName(name);
    }

    @PluggableDependency(ClassDependencyListener.class)
    void initialize() {
        Platform.initClass(platformClass);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    public T newInstance() throws TInstantiationException, TIllegalAccessException {
        Object instance = Platform.newInstance(platformClass);
        if (instance == null) {
            throw new TInstantiationException();
        }
        return (T) instance;
    }

    public TClass<?> getDeclaringClass() {
        PlatformClass result = getDeclaringClassImpl(getPlatformClass());
        return result != null ? getClass(result) : null;
    }
    
    private static native PlatformClass getDeclaringClassImpl(PlatformClass cls);

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
        if (annotationsCache == null) {
            annotationsCache = (TAnnotation[]) Platform.getAnnotations(getPlatformClass());
        }
        return annotationsCache.clone();
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        return getAnnotations();
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
        String name = (String) (Object) getName();
        name = name.substring(0, name.lastIndexOf('.') + 1);
        return TPackage.getPackage(name);
    }
}
