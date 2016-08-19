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
import java.util.HashMap;
import java.util.Map;
import org.teavm.classlib.impl.DeclaringClassMetadataGenerator;
import org.teavm.classlib.java.lang.annotation.TAnnotation;
import org.teavm.classlib.java.lang.reflect.TAnnotatedElement;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformClass;
import org.teavm.platform.metadata.ClassResource;
import org.teavm.platform.metadata.ClassScopedMetadataProvider;

public class TClass<T> extends TObject implements TAnnotatedElement {
    TString name;
    TString simpleName;
    private PlatformClass platformClass;
    private TAnnotation[] annotationsCache;
    private Map<TClass<?>, TAnnotation> annotationsByType;

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

    public TString getName() {
        if (name == null) {
            name = TString.wrap(platformClass.getMetadata().getName());
        }
        return name;
    }

    public TString getSimpleName() {
        if (simpleName == null) {
            if (isArray()) {
                simpleName = getComponentType().getSimpleName().concat(TString.wrap("[]"));
                return simpleName;
            }
            String name = platformClass.getMetadata().getName();
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
        return platformClass.getMetadata().isPrimitive();
    }

    public boolean isArray() {
        return platformClass.getMetadata().getArrayItem() != null;
    }

    public boolean isEnum() {
        return platformClass.getMetadata().isEnum();
    }

    public TClass<?> getComponentType() {
        return getClass(platformClass.getMetadata().getArrayItem());
    }

    @SuppressWarnings("unchecked")
    static TClass<TBoolean> booleanClass() {
        return (TClass<TBoolean>) getClass(Platform.getPrimitives().getBooleanClass());
    }

    @SuppressWarnings("unchecked")
    static TClass<TByte> byteClass() {
        return (TClass<TByte>) getClass(Platform.getPrimitives().getByteClass());
    }

    @SuppressWarnings("unchecked")
    static TClass<TShort> shortClass() {
        return (TClass<TShort>) getClass(Platform.getPrimitives().getShortClass());
    }

    @SuppressWarnings("unchecked")
    static TClass<TLong> longClass() {
        return (TClass<TLong>) getClass(Platform.getPrimitives().getLongClass());
    }

    @SuppressWarnings("unchecked")
    static TClass<TFloat> floatClass() {
        return (TClass<TFloat>) getClass(Platform.getPrimitives().getFloatClass());
    }

    @SuppressWarnings("unchecked")
    static TClass<TDouble> doubleClass() {
        return (TClass<TDouble>) getClass(Platform.getPrimitives().getDoubleClass());
    }

    public boolean desiredAssertionStatus() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public TClass<? super T> getSuperclass() {
        return (TClass<? super T>) getClass(platformClass.getMetadata().getSuperclass());
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
