/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.lang.reflect;

import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.annotation.TAnnotation;

public class TTypeVariableImpl implements TTypeVariable<TGenericDeclaration> {
    TType[] bounds;
    TGenericDeclaration declaration;
    private String name;

    private TTypeVariableImpl(String name) {
        this.name = name;
    }

    static TTypeVariableImpl create(String name) {
        return new TTypeVariableImpl(name);
    }

    @Override
    public TType[] getBounds() {
        return bounds != null ? bounds : new TType[0];
    }

    @Override
    public TGenericDeclaration getGenericDeclaration() {
        return declaration;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T extends TAnnotation> T getAnnotation(TClass<T> annotationClass) {
        return null;
    }

    @Override
    public TAnnotation[] getAnnotations() {
        return new TAnnotation[0];
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        return new TAnnotation[0];
    }
}
