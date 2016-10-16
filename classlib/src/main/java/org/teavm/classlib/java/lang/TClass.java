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
import java.util.Set;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.classlib.impl.DeclaringClassMetadataGenerator;
import org.teavm.classlib.impl.reflection.Flags;
import org.teavm.classlib.impl.reflection.JSClass;
import org.teavm.classlib.impl.reflection.JSField;
import org.teavm.classlib.java.lang.annotation.TAnnotation;
import org.teavm.classlib.java.lang.reflect.TAnnotatedElement;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.classlib.java.lang.reflect.TField;
import org.teavm.classlib.java.lang.reflect.TModifier;
import org.teavm.jso.core.JSArray;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformClass;
import org.teavm.platform.PlatformSequence;
import org.teavm.platform.metadata.ClassResource;
import org.teavm.platform.metadata.ClassScopedMetadataProvider;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeJavaObject;

public class TClass<T> extends TObject implements TAnnotatedElement {
    TString name;
    TString simpleName;
    private PlatformClass platformClass;
    private TAnnotation[] annotationsCache;
    private Map<TClass<?>, TAnnotation> annotationsByType;
    private TField[] declaredFields;
    private TField[] fields;
    private static boolean jsFieldsInitialized;

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

    public boolean isInstance(TObject obj) {
        return Platform.isInstance(Platform.getPlatformObject(obj), platformClass);
    }

    public boolean isAssignableFrom(TClass<?> obj) {
        return Platform.isAssignable(obj.getPlatformClass(), platformClass);
    }

    @DelegateTo("getNameLowLevel")
    public TString getName() {
        if (name == null) {
            name = TString.wrap(Platform.getName(platformClass));
        }
        return name;
    }

    private RuntimeJavaObject getNameLowLevel() {
        RuntimeClass runtimeClass = Address.ofObject(this).toStructure();
        return runtimeClass.name;
    }

    public TString getSimpleName() {
        if (simpleName == null) {
            if (isArray()) {
                simpleName = getComponentType().getSimpleName().concat(TString.wrap("[]"));
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
            simpleName = TString.wrap(name);
        }
        return simpleName;
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

    public TClass<?> getComponentType() {
        return getClass(Platform.getArrayItem(platformClass));
    }

    public TField[] getDeclaredFields() throws TSecurityException {
        if (declaredFields == null) {
            initJsFields();
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
        return declaredFields;
    }

    private static void initJsFields() {
        if (!jsFieldsInitialized) {
            jsFieldsInitialized = true;
            createJsFields();
        }
    }

    @GeneratedBy(ClassGenerator.class)
    private static native void createJsFields();

    public TField[] getFields() throws TSecurityException {
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
        return fields;
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

    public boolean desiredAssertionStatus() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public TClass<? super T> getSuperclass() {
        return (TClass<? super T>) getClass(platformClass.getMetadata().getSuperclass());
    }

    @SuppressWarnings("unchecked")
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
        return isEnum() ? (T[]) Platform.getEnumConstants(platformClass) : null;
    }

    @SuppressWarnings("unchecked")
    public T cast(TObject obj) {
        if (obj != null && !isAssignableFrom((TClass<?>) (Object) obj.getClass())) {
            throw new TClassCastException(TString.wrap(obj.getClass().getName()
                    + " is not subtype of " + name));
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
        ClassResource res = getDeclaringClass(platformClass);
        return res != null ? getClass(Platform.classFromResource(res)) : null;
    }

    @ClassScopedMetadataProvider(DeclaringClassMetadataGenerator.class)
    private static native ClassResource getDeclaringClass(PlatformClass cls);

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
}
