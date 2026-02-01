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
import org.teavm.runtime.reflect.GenericTypeInfo;

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
                return TTypeVariableImpl.resolve(declaration, info.asTypeVariable());
            case GenericTypeInfo.Kind.GENERIC_ARRAY:
                return new TGenericArrayTypeImpl(declaration, info.asGenericArray());
            case GenericTypeInfo.Kind.LOWER_BOUND_WILDCARD:
                return new TWildcardTypeImpl(declaration, info.asWildcard());
            default:
                throw new IllegalStateException();
        }
    }
}
