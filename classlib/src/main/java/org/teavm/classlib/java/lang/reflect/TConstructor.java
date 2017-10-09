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

import org.teavm.classlib.impl.reflection.Converter;
import org.teavm.classlib.impl.reflection.Flags;
import org.teavm.classlib.impl.reflection.JSCallable;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TInstantiationException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.platform.PlatformObject;
import org.teavm.platform.PlatformSequence;

public class TConstructor<T> extends TAccessibleObject implements TMember {
    private TClass<T> declaringClass;
    private String name;
    private int modifiers;
    private int accessLevel;
    private TClass<?>[] parameterTypes;
    private JSCallable callable;

    public TConstructor(TClass<T> declaringClass, String name, int modifiers, int accessLevel,
            TClass<?>[] parameterTypes, JSCallable callable) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.modifiers = modifiers;
        this.accessLevel = accessLevel;
        this.parameterTypes = parameterTypes;
        this.callable = callable;
    }

    @Override
    public TClass<T> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getModifiers() {
        return Flags.getModifiers(modifiers, accessLevel);
    }

    @Override
    public boolean isSynthetic() {
        return (modifiers & Flags.SYNTHETIC) != 0;
    }

    public TClass<?>[] getParameterTypes() {
        return parameterTypes.clone();
    }

    public int getParameterCount() {
        return parameterTypes.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TModifier.toString(getModifiers()));
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(declaringClass.getName().toString()).append('(');
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
        if ((modifiers & Flags.ABSTRACT) != 0) {
            throw new TInstantiationException();
        }
        if (callable == null) {
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

        PlatformSequence<PlatformObject> jsArgs = Converter.arrayFromJava(initargs);
        PlatformObject instance = declaringClass.newEmptyInstance();
        callable.call(instance, jsArgs);
        return (T) Converter.toJava(instance);
    }

    public boolean isVarArgs() {
        return (modifiers & Flags.VARARGS) != 0;
    }
}
