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

import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.InjectedBy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TClass<T> extends TObject {
    TString name;
    boolean primitive;
    boolean array;
    boolean isEnum;
    private TClass<?> componentType;
    private boolean componentTypeDirty = true;

    static TClass<?> createNew() {
        return new TClass<>();
    }

    @InjectedBy(ClassNativeGenerator.class)
    public native boolean isInstance(TObject obj);

    @InjectedBy(ClassNativeGenerator.class)
    public native boolean isAssignableFrom(TClass<?> obj);

    public TString getName() {
        return new TString(name);
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public boolean isArray() {
        return array;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public TClass<?> getComponentType() {
        if (componentTypeDirty) {
            componentType = getComponentType0();
            componentTypeDirty = false;
        }
        return componentType;
    }

    @GeneratedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    private native TClass<?> getComponentType0();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    static native TClass<TBoolean> booleanClass();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    static native TClass<TCharacter> charClass();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    static native TClass<TInteger> intClass();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    static native TClass<TVoid> voidClass();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    static native TClass<TByte> byteClass();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    static native TClass<TFloat> floatClass();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    static native TClass<TDouble> doubleClass();

    @InjectedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    public static native <S extends TObject> TClass<S> wrap(Class<S> cls);

    public boolean desiredAssertionStatus() {
        return true;
    }

    @GeneratedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    public native TClass<? super T> getSuperclass();

    public T[] getEnumConstants() {
        return isEnum ? getEnumConstantsImpl() : null;
    }

    @InjectedBy(ClassNativeGenerator.class)
    public native T[] getEnumConstantsImpl();

    @SuppressWarnings("unchecked")
    public T cast(TObject obj) {
        if (obj != null && !isAssignableFrom(TClass.wrap(obj.getClass()))) {
            throw new TClassCastException(TString.wrap(new TStringBuilder()
                    .append(TClass.wrap(obj.getClass()).getName())
                    .append(TString.wrap(" is not subtype of ")).append(name).toString()));
        }
        return (T)obj;
    }

    public TClassLoader getClassLoader() {
        return TClassLoader.getSystemClassLoader();
    }

    @GeneratedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    private static native TClass<?> forNameImpl(TString name);

    public static TClass<?> forName(TString name) throws TClassNotFoundException {
        TClass<?> result = forNameImpl(name);
        if (result == null) {
            throw new TClassNotFoundException();
        }
        return result;
    }

    @SuppressWarnings("unused")
    public static TClass<?> forName(TString name, boolean initialize, TClassLoader loader)
                     throws TClassNotFoundException {
        return forName(name);
    }

    @GeneratedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    public native T newInstance() throws TInstantiationException, TIllegalAccessException;

    @GeneratedBy(ClassNativeGenerator.class)
    @PluggableDependency(ClassNativeGenerator.class)
    public native TClass<?> getDeclaringClass();
}
