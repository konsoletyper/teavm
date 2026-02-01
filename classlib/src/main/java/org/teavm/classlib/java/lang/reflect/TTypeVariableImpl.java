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
import org.teavm.runtime.reflect.TypeVariableInfo;
import org.teavm.runtime.reflect.TypeVariableReference;

public class TTypeVariableImpl implements TTypeVariable<TGenericDeclaration> {
    private TGenericDeclaration declaration;
    private TypeVariableInfo info;
    private TType[] bounds;

    public TTypeVariableImpl(TGenericDeclaration declaration, TypeVariableInfo info) {
        this.declaration = declaration;
        this.info = info;
    }

    @Override
    public TType[] getBounds() {
        if (bounds == null) {
            var count = info.boundCount();
            bounds = new TType[count];
            for (var i = 0; i < count; ++i) {
                bounds[i] = TGenericTypeFactory.create(declaration, info.bound(i));
            }
        }
        return bounds.clone();
    }

    @Override
    public TGenericDeclaration getGenericDeclaration() {
        return declaration;
    }

    @Override
    public String getName() {
        return info.name().getStringObject();
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

    @Override
    public String toString() {
        return getName();
    }

    static TTypeVariable<?> resolve(TGenericDeclaration declaration, TypeVariableReference ref) {
        if (ref.level() == 0) {
            return declaration.getTypeParameters()[ref.index()];
        } else {
            var declaringClass = declaration instanceof TMember
                    ? ((TMember) declaration).getDeclaringClass()
                    : ((TClass<?>) declaration).getDeclaringClass();
            return declaringClass.getTypeParameters()[ref.index()];
        }
    }
}
