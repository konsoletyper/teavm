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

import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.InjectedBy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TClass<T> extends TObject {
    String name;
    boolean primitive;
    boolean array;
    private TClass<?> componentType;
    private boolean componentTypeDirty = true;

    static TClass<?> createNew() {
        return new TClass<>();
    }

    @InjectedBy(ClassNativeGenerator.class)
    public native boolean isInstance(TObject obj);

    @InjectedBy(ClassNativeGenerator.class)
    public native boolean isAssignableFrom(TClass<?> obj);

    public String getName() {
        return name;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public boolean isArray() {
        return array;
    }

    public TClass<?> getComponentType() {
        if (componentTypeDirty) {
            componentType = getComponentType0();
            componentTypeDirty = false;
        }
        return componentType;
    }

    @GeneratedBy(ClassNativeGenerator.class)
    private native TClass<?> getComponentType0();

    @InjectedBy(ClassNativeGenerator.class)
    static native TClass<TBoolean> booleanClass();

    @InjectedBy(ClassNativeGenerator.class)
    static native TClass<TInteger> intClass();
}
