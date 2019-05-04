/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.metaprogramming.impl.reflect;

import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.reflect.ReflectParameterizedType;
import org.teavm.metaprogramming.reflect.ReflectTypeArgument;

public class ReflectRawTypeImpl implements ReflectParameterizedType {
    private ReflectContext context;
    private ReflectClassImpl<?> constructor;
    private ReflectParameterizedType ownerType;
    private boolean ownerTypeResolved;

    public ReflectRawTypeImpl(ReflectContext context, ReflectClassImpl<?> constructor) {
        this.context = context;
        this.constructor = constructor;
    }

    ReflectRawTypeImpl(ReflectClassImpl<?> constructor) {
        this.constructor = constructor;
    }

    @Override
    public ReflectTypeArgument[] getTypeArguments() {
        return ReflectContext.EMPTY_TYPE_ARGUMENTS;
    }

    @Override
    public ReflectParameterizedType getOwnerType() {
        if (!ownerTypeResolved) {
            ownerTypeResolved = true;
            ReflectClassImpl<?> declaringClass = constructor.getDeclaringClass();
            if (declaringClass != null) {
                ownerType = (ReflectParameterizedType) context.getRawGenericType(declaringClass);
            }
        }
        return ownerType;
    }

    @Override
    public ReflectClass<?> getTypeConstructor() {
        return constructor;
    }
}
