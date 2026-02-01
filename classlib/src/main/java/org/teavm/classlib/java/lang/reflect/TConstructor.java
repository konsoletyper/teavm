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

import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TInstantiationException;
import org.teavm.runtime.reflect.MethodInfo;
import org.teavm.runtime.reflect.ModifiersInfo;

public class TConstructor<T> extends TExecutable implements TMember {
    public TConstructor(ClassInfo declaringClass, MethodInfo methodInfo) {
        super(declaringClass, methodInfo);
    }

    @Override
    public String getName() {
        return declaringClass.classObject().getSimpleName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TModifier.toString(getModifiers()));
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(declaringClass.classObject().getName()).append('(');
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
        if ((methodInfo.modifiers() & ModifiersInfo.ABSTRACT) != 0) {
            throw new TInstantiationException();
        }
        var caller = methodInfo.caller();
        if (caller == null) {
            throw new TIllegalAccessException();
        }

        if (initargs.length != methodInfo.parameterCount()) {
            throw new TIllegalArgumentException();
        }
        validateArgs(initargs);
        declaringClass.initialize();

        var instance = declaringClass.newInstance();
        return (T) caller.call(instance, initargs);
    }

}
