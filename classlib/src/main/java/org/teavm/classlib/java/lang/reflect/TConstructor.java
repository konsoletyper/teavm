/*
 *  Copyright 2016 Alexey Andreev.
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
import org.teavm.classlib.impl.reflection.MethodCaller;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TInstantiationException;
import org.teavm.classlib.java.lang.TObject;

public class TConstructor<T> extends TExecutable implements TMember {
    private String name;
    private MethodCaller caller;

    public TConstructor(TClass<T> declaringClass, String name, int modifiers, int accessLevel,
            TClass<?>[] parameterTypes, MethodCaller caller, Annotation[] declaredAnnotations) {
        super(declaringClass, modifiers, accessLevel, parameterTypes, declaredAnnotations);
        this.name = name;
        this.caller = caller;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TModifier.toString(getModifiers()));
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(declaringClass.getName().toString()).append('(');
        TClass<?>[] parameterTypes = getParameterTypes();
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parameterTypes[i].getName());
        }
        return sb.append(')').toString();
    }

    @SuppressWarnings("unchecked")
    public T newInstance(Object... initargs) throws TInstantiationException, TIllegalAccessException,
            TIllegalArgumentException, TInvocationTargetException {
        if ((flags & Flags.ABSTRACT) != 0) {
            throw new TInstantiationException();
        }
        if (caller == null) {
            throw new TIllegalAccessException();
        }

        if (initargs.length != parameterTypes.length) {
            throw new TIllegalArgumentException();
        }
        for (int i = 0; i < initargs.length; ++i) {
            if (!parameterTypes[i].isPrimitive() && initargs[i] != null
                    && !parameterTypes[i].isInstance((TObject) initargs[i])) {
                throw new TIllegalArgumentException();
            }
            if (parameterTypes[i].isPrimitive() && initargs[i] == null) {
                throw new TIllegalArgumentException();
            }
        }

        return (T) caller.call(null, initargs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TTypeVariable<TConstructor<T>>[] getTypeParameters() {
        return (TTypeVariable<TConstructor<T>>[]) new TTypeVariable<?>[0];
    }
}
