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

import java.lang.annotation.Annotation;
import org.teavm.classlib.impl.reflection.Flags;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.annotation.TAnnotation;

public abstract class TExecutable extends TAccessibleObject implements TMember, TGenericDeclaration {
    TClass<?> declaringClass;
    int flags;
    int accessLevel;
    TClass<?>[] parameterTypes;
    Object[] declaredAnnotations;

    TExecutable(TClass<?> declaringClass, int flags, int accessLevel, TClass<?>[] parameterTypes,
            Annotation[] declaredAnnotations) {
        this.declaringClass = declaringClass;
        this.flags = flags;
        this.accessLevel = accessLevel;
        this.parameterTypes = parameterTypes;
        this.declaredAnnotations = declaredAnnotations;
    }

    @Override
    public TClass<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public int getModifiers() {
        return Flags.getModifiers(flags, accessLevel);
    }

    public TClass<?>[] getParameterTypes() {
        return parameterTypes.clone();
    }

    public int getParameterCount() {
        return parameterTypes.length;
    }

    @Override
    public boolean isSynthetic() {
        return (flags & Flags.SYNTHETIC) != 0;
    }

    public boolean isVarArgs() {
        return (flags & Flags.VARARGS) != 0;
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        return declaredAnnotations != null ? (TAnnotation[]) declaredAnnotations.clone() : new TAnnotation[0];
    }
}
