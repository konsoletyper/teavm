/*
 *  Copyright 2026 Alexey Andreev.
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
import org.teavm.runtime.reflect.ClassInfoUtil;
import org.teavm.runtime.reflect.GenericTypeInfo;
import org.teavm.runtime.reflect.TypeVariableReference;

class TGenericTypeFactory {
    private TGenericTypeFactory() {
    }

    static TType create(TGenericDeclaration declaration, GenericTypeInfo info) {
        switch (info.kind()) {
            case GenericTypeInfo.Kind.PARAMETERIZED_TYPE: {
                var paramType = info.asParameterizedType();
                if (paramType.actualTypeArgumentCount() == 0) {
                    return (TClass<?>) (Object) paramType.rawType().classObject();
                } else {
                    return new TParameterizedTypeImpl(declaration, info.asParameterizedType());
                }
            }
            case GenericTypeInfo.Kind.TYPE_VARIABLE:
                return resolve(declaration, info.asTypeVariable());
            case GenericTypeInfo.Kind.GENERIC_ARRAY:
                return new TGenericArrayTypeImpl(declaration, info.asGenericArray());
            case GenericTypeInfo.Kind.LOWER_BOUND_WILDCARD:
            case GenericTypeInfo.Kind.UPPER_BOUND_WILDCARD:
            case GenericTypeInfo.Kind.UNBOUNDED_WILDCARD:
                return new TWildcardTypeImpl(declaration, info.asWildcard());
            case GenericTypeInfo.Kind.RAW_TYPE:
                return (TType) (Object) ClassInfoUtil.resolve(info.asRawType().rawType()).classObject();
            default:
                throw new IllegalStateException();
        }
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
