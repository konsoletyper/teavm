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
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.impl.reflection.Flags;
import org.teavm.classlib.impl.reflection.MethodCaller;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TIllegalAccessException;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.platform.Platform;

public class TMethod extends TExecutable implements TMember {
    private String name;
    private TClass<?> returnType;
    private MethodCaller caller;

    public TMethod(TClass<?> declaringClass, String name, int flags, int accessLevel, TClass<?> returnType,
            TClass<?>[] parameterTypes, MethodCaller caller, Annotation[] declaredAnnotations,
            TTypeVariable<?>[] typeParameters) {
        super(declaringClass, flags, accessLevel, parameterTypes, declaredAnnotations, typeParameters);
        this.name = name;
        this.returnType = returnType;
        this.caller = caller;
    }

    @Override
    public String getName() {
        return name;
    }

    public TClass<?> getReturnType() {
        return returnType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TModifier.toString(getModifiers(), (getDeclaringClass().getModifiers() & TModifier.INTERFACE) != 0));
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(getReturnType().getName()).append(' ').append(getDeclaringClass().getName()).append('.')
                .append(name).append('(');
        TClass<?>[] parameterTypes = getParameterTypes();
        if (parameterTypes.length > 0) {
            sb.append(parameterTypes[0].getName());
            for (int i = 1; i < parameterTypes.length; ++i) {
                sb.append(',').append(parameterTypes[i].getName());
            }
        }
        sb.append(')');

        return sb.toString();
    }

    public Object invoke(Object obj, Object... args) throws TIllegalAccessException, TIllegalArgumentException,
            TInvocationTargetException {
        if (caller == null) {
            throw new TIllegalAccessException();
        }

        if (args.length != parameterTypes.length) {
            throw new TIllegalArgumentException();
        }

        if ((flags & Flags.STATIC) == 0) {
            if (!declaringClass.isInstance((TObject) obj)) {
                throw new TIllegalArgumentException();
            }
        } else if (PlatformDetector.isJavaScript()) {
            Platform.initClass(declaringClass.getPlatformClass());
        }

        for (int i = 0; i < args.length; ++i) {
            if (!parameterTypes[i].isPrimitive() && args[i] != null
                    && !parameterTypes[i].isInstance((TObject) args[i])) {
                throw new TIllegalArgumentException();
            }
            if (parameterTypes[i].isPrimitive() && args[i] == null) {
                throw new TIllegalArgumentException();
            }
        }

        return caller.call(obj, args);
    }

    public boolean isBridge() {
        return (flags & Flags.BRIDGE) != 0;
    }
}
