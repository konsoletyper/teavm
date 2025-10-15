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
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassFlags;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.impl.reflection.ClassSupport;
import org.teavm.classlib.impl.reflection.FieldInfoList;
import org.teavm.classlib.impl.reflection.FieldReader;
import org.teavm.classlib.impl.reflection.FieldWriter;
import org.teavm.classlib.impl.reflection.Flags;
import org.teavm.classlib.impl.reflection.JSClass;
import org.teavm.classlib.impl.reflection.JSField;
import org.teavm.classlib.impl.reflection.JSMethodMember;
import org.teavm.classlib.impl.reflection.MethodCaller;
import org.teavm.classlib.impl.reflection.MethodInfoList;
import org.teavm.classlib.java.lang.annotation.TAnnotation;
import org.teavm.classlib.java.lang.reflect.TAnnotatedElement;
import org.teavm.classlib.java.lang.reflect.TConstructor;
import org.teavm.classlib.java.lang.reflect.TField;
import org.teavm.classlib.java.lang.reflect.TMethod;
import org.teavm.classlib.java.lang.reflect.TModifier;
import org.teavm.classlib.java.lang.reflect.TType;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.core.JSArray;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformClass;
import org.teavm.platform.PlatformObject;
import org.teavm.platform.PlatformSequence;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public final class TClass<T> extends TObject implements TAnnotatedElement, TType {
    private static Map<String, TClass<?>> nameMap;
    String name;
    String simpleName;
    String canonicalName;
    private PlatformClass platformClass;
    private TAnnotation[] annotationsCache;
    private TAnnotation[] declaredAnnotationsCache;
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

    @Override
    public String toString() {
        return (isInterface() ? "interface " : (isPrimitive() ? "" : "class ")) + getName();
    }

    private String obfuscatedToString() {
        return "javaClass@" + identity();
    }

    public PlatformClass getPlatformClass() {
        return platformClass;
    }

    @DelegateTo("isInstanceLowLevel")
    public boolean isInstance(TObject obj) {
        if (PlatformDetector.isWebAssemblyGC()) {
            return obj != null && isAssignableFrom((TClass<?>) (Object) obj.getClass());
        }
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

    public String getName() {
        if (PlatformDetector.isWebAssemblyGC()) {
            var result = getNameImpl();
            if (result == null) {
                if (isArray()) {
                    var componentType = getComponentType();
                    String componentName = componentType.getName();
                    if (componentName != null) {
                        result = componentType.isArray() ? "[" + componentName : "[L" + componentName + ";";
                        setNameImpl(result);
                    }
                }
            }
            return result;
        } else if (PlatformDetector.isLowLevel()) {
            String result = getNameCache(this);
            if (result == null) {
                result = Platform.getName(platformClass);
                if (result == null) {
                    if (isArray()) {
                        TClass<?> componentType = getComponentType();
                        String componentName = componentType.getName();
                        if (componentName != null) {
                            result = componentType.isArray() ? "[" + componentName : "[L" + componentName + ";";
                        }
                    }
                }
                setNameCache(this, result);
            }
            return result;
        } else {
            if (name == null) {
                name = Platform.getName(platformClass);
            }
            return name;
        }
    }

    @PluggableDependency(ClassDependencyListener.class)
    private native String getNameImpl();

    private native void setNameImpl(String name);

    public String getSimpleName() {
        String simpleName = getSimpleNameCache(this);
        if (simpleName == null) {
            if (isArray()) {
                simpleName = getComponentType().getSimpleName() + "[]";
            } else if (getEnclosingClass() != null) {
                simpleName = PlatformDetector.isWebAssemblyGC()
                    ? getSimpleNameCache(this)
                    : Platform.getSimpleName(platformClass);
                if (simpleName == null) {
                    simpleName = "";
                }
            } else {
                var name = PlatformDetector.isWebAssemblyGC()
                        ? getName()
                        : Platform.getName(platformClass);
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
            }
            setSimpleNameCache(this, simpleName);
        }
        return simpleName;
    }

    @DelegateTo("getSimpleNameCacheLowLevel")
    private static String getSimpleNameCache(TClass<?> self) {
        return self.simpleName;
    }

    @Unmanaged
    @PluggableDependency(ClassDependencyListener.class)
    private static RuntimeObject getSimpleNameCacheLowLevel(RuntimeClass self) {
        return self.simpleNameCache;
    }

    @DelegateTo("setSimpleNameCacheLowLevel")
    private static void setSimpleNameCache(TClass<?> self, String value) {
        self.simpleName = value;
    }

    @Unmanaged
    private static void setSimpleNameCacheLowLevel(RuntimeClass self, RuntimeObject object) {
        self.simpleNameCache = object;
    }

    @DelegateTo("getNameCacheLowLevel")
    private static String getNameCache(TClass<?> self) {
        return self.name;
    }

    @Unmanaged
    @PluggableDependency(ClassDependencyListener.class)
    private static RuntimeObject getNameCacheLowLevel(RuntimeClass self) {
        return self.nameCache;
    }

    @DelegateTo("setNameCacheLowLevel")
    private static void setNameCache(TClass<?> self, String value) {
        self.name = value;
    }

    @Unmanaged
    private static void setNameCacheLowLevel(RuntimeClass self, RuntimeObject object) {
        self.nameCache = object;
    }

    public String getCanonicalName() {
        String result = getCanonicalNameCache();
        if (result == null) {
            if (isArray()) {
                String componentName = getComponentType().getCanonicalName();
                if (componentName == null) {
                    return null;
                }
                result = componentName + "[]";
            } else if (getEnclosingClass() != null) {
                if (getDeclaringClass() == null || isSynthetic()) {
                    return null;
                }
                String enclosingName = getDeclaringClass().getCanonicalName();
                if (enclosingName == null) {
                    return null;
                }
                result = enclosingName + "." + getSimpleName();
            } else {
                result = getName();
            }
            setCanonicalNameCache(result);
        }
        return result;
    }

    private boolean isSynthetic() {
        if (PlatformDetector.isWebAssemblyGC()) {
            return (getWasmGCFlags() & WasmGCClassFlags.SYNTHETIC) != 0;
        } else if (PlatformDetector.isJavaScript()) {
            return (platformClass.getMetadata().getAccessLevel() & Flags.SYNTHETIC) != 0;
        } else {
            return (RuntimeClass.getClass(Address.ofObject(this).toStructure()).flags & RuntimeClass.SYNTHETIC) != 0;
        }
    }

    @DelegateTo("getCanonicalNameCacheLowLevel")
    private String getCanonicalNameCache() {
        return canonicalName;
    }

    @Unmanaged
    @PluggableDependency(ClassDependencyListener.class)
    private RuntimeObject getCanonicalNameCacheLowLevel() {
        return Address.ofObject(this).<RuntimeClass>toStructure().canonicalName;
    }

    @DelegateTo("setCanonicalNameCacheLowLevel")
    private void setCanonicalNameCache(String value) {
        canonicalName = value;
    }

    @Unmanaged
    private void setCanonicalNameCacheLowLevel(RuntimeObject object) {
        Address.ofObject(this).<RuntimeClass>toStructure().canonicalName = object;
    }

    public boolean isPrimitive() {
        if (PlatformDetector.isWebAssemblyGC()) {
            return (getWasmGCFlags() & WasmGCClassFlags.PRIMITIVE) != 0;
        }
        return Platform.isPrimitive(platformClass);
    }

    public boolean isArray() {
        if (PlatformDetector.isWebAssemblyGC()) {
            return getComponentType() != null;
        }
        return Platform.getArrayItem(platformClass) != null;
    }

    public boolean isEnum() {
        if (PlatformDetector.isWebAssemblyGC()) {
            return (getWasmGCFlags() & WasmGCClassFlags.ENUM) != 0;
        }
        return Platform.isEnum(platformClass);
    }

    public boolean isInterface() {
        if (PlatformDetector.isWebAssemblyGC()) {
            return (getWasmGCFlags() & WasmGCClassFlags.INTERFACE) != 0;
        }
        return (platformClass.getMetadata().getFlags() & Flags.INTERFACE) != 0;
    }
    
    public boolean isAnnotation() {
        if (PlatformDetector.isWebAssemblyGC()) {
            return (getWasmGCFlags() & WasmGCClassFlags.ANNOTATION) != 0;
        }
        return (platformClass.getMetadata().getFlags() & Flags.ANNOTATION) != 0;
    }

    public boolean isLocalClass() {
        if (PlatformDetector.isWebAssemblyGC()) {
            return (getWasmGCFlags() & WasmGCClassFlags.SYNTHETIC) != 0 && getEnclosingClass() != null;
        }
        return (platformClass.getMetadata().getFlags() & Flags.SYNTHETIC) != 0 && getEnclosingClass() != null;
    }

    public boolean isMemberClass() {
        return getDeclaringClass() != null;
    }

    private native int getWasmGCFlags();

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
            if (PlatformDetector.isJavaScript()) {
                JSClass jsClass = (JSClass) getPlatformClass().getMetadata();
                JSArray<JSField> jsFields = jsClass.getFields();
                declaredFields = new TField[jsFields.getLength()];
                for (int i = 0; i < jsFields.getLength(); ++i) {
                    JSField jsField = jsFields.get(i);
                    declaredFields[i] = new TField(this, jsField.getName(), jsField.getModifiers(),
                            jsField.getAccessLevel(), TClass.getClass(jsField.getType()),
                            FieldReader.forJs(jsField.getGetter()),
                            FieldWriter.forJs(jsField.getSetter()));
                }
            } else {
                var infoList = getDeclaredFieldsImpl();
                if (infoList == null) {
                    declaredFields = new TField[0];
                } else {
                    declaredFields = new TField[infoList.count()];
                    for (var i = 0; i < declaredFields.length; ++i) {
                        var fieldInfo = infoList.get(i);
                        declaredFields[i] = new TField(this, fieldInfo.name(), fieldInfo.modifiers(),
                                fieldInfo.accessLevel(), (TClass<?>) (Object) fieldInfo.type(),
                                fieldInfo.reader(), fieldInfo.writer());
                    }
                }
            }
        }
        return declaredFields.clone();
    }

    private native FieldInfoList getDeclaredFieldsImpl();

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

    @InjectedBy(ClassGenerator.class)
    @PluggableDependency(ClassGenerator.class)
    public native PlatformObject newEmptyInstance();

    @SuppressWarnings({ "raw", "unchecked" })
    public TConstructor<?>[] getDeclaredConstructors() throws TSecurityException {
        if (isPrimitive() || isArray()) {
            return new TConstructor<?>[0];
        }

        if (declaredConstructors == null) {
            initReflection();
            if (PlatformDetector.isJavaScript()) {
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
                    declaredConstructors[count++] = new TConstructor<>(this, jsMethod.getName(),
                            jsMethod.getModifiers(), jsMethod.getAccessLevel(), parameterTypes,
                            MethodCaller.forJs(jsMethod.getCallable()));
                }
                declaredConstructors = Arrays.copyOf(declaredConstructors, count);
            } else {
                var methodInfoList = getDeclaredMethodsImpl();
                if (methodInfoList == null) {
                    declaredConstructors = new TConstructor[0];
                } else {
                    declaredConstructors = new TConstructor[methodInfoList.count()];
                    int count = 0;
                    for (int i = 0; i < methodInfoList.count(); ++i) {
                        var methodInfo = methodInfoList.get(i);
                        if (!methodInfo.name().equals("<init>")) {
                            continue;
                        }
                        var paramTypeInfoList = methodInfo.parameterTypes();
                        var parameterTypes = new TClass<?>[paramTypeInfoList.count()];
                        for (int j = 0; j < parameterTypes.length; ++j) {
                            parameterTypes[j] = (TClass<?>) (Object) paramTypeInfoList.get(j);
                        }
                        declaredConstructors[count++] = new TConstructor<>(this, methodInfo.name(),
                                methodInfo.modifiers(), methodInfo.accessLevel(), parameterTypes,
                                methodInfo.caller());
                    }
                    declaredConstructors = Arrays.copyOf(declaredConstructors, count);
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
        if (isPrimitive() || isArray()) {
            return new TMethod[0];
        }
        if (declaredMethods == null) {
            initReflection();
            if (PlatformDetector.isJavaScript()) {
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
                            jsMethod.getAccessLevel(), returnType, parameterTypes,
                            MethodCaller.forJs(jsMethod.getCallable()));
                }
                declaredMethods = Arrays.copyOf(declaredMethods, count);
            } else {
                var methodInfoList = getDeclaredMethodsImpl();
                if (methodInfoList == null) {
                    declaredMethods = new TMethod[0];
                } else {
                    declaredMethods = new TMethod[methodInfoList.count()];
                    int count = 0;
                    for (int i = 0; i < methodInfoList.count(); ++i) {
                        var methodInfo = methodInfoList.get(i);
                        if (methodInfo.name().equals("<init>") || methodInfo.name().equals("<clinit>")) {
                            continue;
                        }
                        var paramTypeInfoList = methodInfo.parameterTypes();
                        var parameterTypes = new TClass<?>[paramTypeInfoList.count()];
                        for (int j = 0; j < parameterTypes.length; ++j) {
                            parameterTypes[j] = (TClass<?>) (Object) paramTypeInfoList.get(j);
                        }
                        var returnType = methodInfo.returnType();
                        declaredMethods[count++] = new TMethod(this, methodInfo.name(), methodInfo.modifiers(),
                                methodInfo.accessLevel(), (TClass<?>) (Object) returnType, parameterTypes,
                                methodInfo.caller());
                    }
                    declaredMethods = Arrays.copyOf(declaredMethods, count);
                }
            }
        }
        return declaredMethods.clone();
    }

    private native MethodInfoList getDeclaredMethodsImpl();

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
        if (PlatformDetector.isJavaScript()) {
            int flags = platformClass.getMetadata().getFlags();
            int accessLevel = platformClass.getMetadata().getAccessLevel();
            return Flags.getModifiers(flags, accessLevel);
        } else if (PlatformDetector.isWebAssemblyGC()) {
            return getWasmGCFlags() & WasmGCClassFlags.JVM_FLAGS_MASK;
        } else {
            return 0;
        }
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
        if (PlatformDetector.isWebAssemblyGC()) {
            var result = getInterfacesImpl();
            return result != null ? result.clone() : (TClass<? super T>[]) new TClass<?>[0];
        } else {
            PlatformSequence<PlatformClass> supertypes = platformClass.getMetadata().getSupertypes();

            TClass<? super T>[] filteredSupertypes = (TClass<? super T>[]) new TClass<?>[supertypes.getLength()];
            int j = 0;
            for (int i = 0; i < supertypes.getLength(); ++i) {
                if (supertypes.get(i) != platformClass.getMetadata().getSuperclass()) {
                    filteredSupertypes[j++] = (TClass<? super T>) getClass(supertypes.get(i));
                }
            }

            if (filteredSupertypes.length > j) {
                filteredSupertypes = Arrays.copyOf(filteredSupertypes, j);
            }
            return filteredSupertypes;
        }
    }

    private native TClass<? super T>[] getInterfacesImpl();

    @SuppressWarnings("unchecked")
    public T[] getEnumConstants() {
        if (!isEnum()) {
            return null;
        }
        if (PlatformDetector.isWebAssemblyGC()) {
            return (T[]) ClassSupport.getEnumConstants((Class<?>) (Object) this);
        } else {
            Platform.initClass(platformClass);
            return (T[]) Platform.getEnumConstants(platformClass).clone();
        }
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
        if (PlatformDetector.isJavaScript()) {
            PlatformClass cls = Platform.lookupClass(name.toString());
            if (cls == null) {
                throw new TClassNotFoundException();
            }
            return getClass(cls);
        } else {
            if (nameMap == null) {
                fillNameMap();
            }
            var result = nameMap.get((String) (Object) name);
            if (result == null) {
                throw new TClassNotFoundException((String) (Object) name);
            }
            return result;
        }
    }

    private static void fillNameMap() {
        nameMap = new HashMap<>();
        var cls = last();
        while (cls != null) {
            var name = cls.getNameImpl();
            if (name != null) {
                nameMap.put(name, cls);
            }
            cls = cls.previous();
        }
    }

    @PluggableDependency(ClassDependencyListener.class)
    private static native TClass<?> last();

    @PluggableDependency(ClassDependencyListener.class)
    private native TClass<?> previous();

    @SuppressWarnings("unused")
    public static TClass<?> forName(TString name, boolean initialize, TClassLoader loader)
            throws TClassNotFoundException {
        return forName(name);
    }

    @PluggableDependency(ClassDependencyListener.class)
    void initialize() {
        if (PlatformDetector.isJavaScript()) {
            Platform.initClass(platformClass);
        } else {
            initializeImpl();
        }
    }

    private native void initializeImpl();

    @SuppressWarnings({ "unchecked", "unused" })
    public T newInstance() throws TInstantiationException, TIllegalAccessException {
        Object instance;
        if (PlatformDetector.isJavaScript()) {
            instance = Platform.newInstance(platformClass);
        } else {
            initReflection();
            instance = newInstanceImpl();
        }
        if (instance == null) {
            throw new TInstantiationException();
        }
        return (T) instance;
    }

    private native Object newInstanceImpl();

    public TClass<?> getDeclaringClass() {
        PlatformClass result = Platform.getDeclaringClass(getPlatformClass());
        return result != null ? getClass(result) : null;
    }

    public TClass<?> getEnclosingClass() {
        PlatformClass result = Platform.getEnclosingClass(getPlatformClass());
        return result != null ? getClass(result) : null;
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
        if (annotationsCache == null) {
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
            annotationsCache = map.values().toArray(new TAnnotation[0]);
        }
        return annotationsCache.clone();
    }

    private static boolean isInherited(TAnnotation annot) {
        if (PlatformDetector.isWebAssemblyGC()) {
            var flags = ((TClass<?>) (Object) annot.annotationType()).getWasmGCFlags();
            return (flags & WasmGCClassFlags.INHERITED_ANNOTATIONS) != 0;
        } else {
            var platformClass = ((TClass<?>) (Object) annot.annotationType()).platformClass;
            return (platformClass.getMetadata().getFlags() & Flags.INHERITED_ANNOTATION) != 0;
        }
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        if (declaredAnnotationsCache == null) {
            if (PlatformDetector.isWebAssemblyGC()) {
                declaredAnnotationsCache = getDeclaredAnnotationsImpl();
            } else {
                declaredAnnotationsCache = (TAnnotation[]) Platform.getAnnotations(getPlatformClass());
            }
            if (declaredAnnotationsCache == null) {
                declaredAnnotationsCache = new TAnnotation[0];
            }
        }
        return declaredAnnotationsCache.clone();
    }

    private native TAnnotation[] getDeclaredAnnotationsImpl();

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
