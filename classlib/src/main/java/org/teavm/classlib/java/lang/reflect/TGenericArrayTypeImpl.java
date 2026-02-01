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

import org.teavm.runtime.reflect.GenericArrayInfo;

class TGenericArrayTypeImpl implements TGenericArrayType {
    private TGenericDeclaration declaration;
    private GenericArrayInfo info;
    private TType genericComponentType;

    TGenericArrayTypeImpl(TGenericDeclaration declaration, GenericArrayInfo info) {
        this.declaration = declaration;
        this.info = info;
    }

    @Override
    public TType getGenericComponentType() {
        if (genericComponentType == null) {
            genericComponentType = TGenericTypeFactory.create(declaration, info.getItemType());
        }
        return genericComponentType;
    }

    @Override
    public String toString() {
        return genericComponentType.getTypeName() + "[]";
    }
}
