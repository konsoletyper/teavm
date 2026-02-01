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

import java.util.Arrays;
import java.util.Objects;
import org.teavm.runtime.reflect.ParameterizedTypeInfo;

class TParameterizedTypeImpl implements TParameterizedType {
    private TGenericDeclaration declaration;
    private ParameterizedTypeInfo info;
    private TType[] actualTypeArguments;
    private boolean ownerTypeInitialized;
    private TType ownerType;

    TParameterizedTypeImpl(TGenericDeclaration declaration, ParameterizedTypeInfo info) {
        this.declaration = declaration;
        this.info = info;
    }

    @Override
    public TType[] getActualTypeArguments() {
        if (actualTypeArguments == null) {
            actualTypeArguments = new TType[info.actualTypeArgumentCount()];
            for (var i = 0; i < actualTypeArguments.length; i++) {
                actualTypeArguments[i] = TGenericTypeFactory.create(declaration, info.actualTypeArgument(i));
            }
        }
        return actualTypeArguments.clone();
    }

    @Override
    public TType getRawType() {
        return (TType) (Object) info.rawType().classObject();
    }

    @Override
    public TType getOwnerType() {
        if (!ownerTypeInitialized) {
            ownerTypeInitialized = true;
            if (info.ownerType() != null) {
                ownerType = TGenericTypeFactory.create(declaration, info.ownerType());
            }
        }
        return ownerType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TParameterizedTypeImpl)) {
            return false;
        }
        var that = (TParameterizedTypeImpl) obj;
        return getRawType() == that.getRawType()
                && Objects.equals(getOwnerType(), that.getOwnerType())
                && Arrays.equals(getActualTypeArguments(), that.getActualTypeArguments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawType(), ownerType, Arrays.hashCode(getActualTypeArguments()));
    }

    @Override
    public String toString() {
        var args = getActualTypeArguments();
        var sb = new StringBuilder();
        if (ownerType != null) {
            sb.append(ownerType.getTypeName()).append("$").append(info.rawType().classObject().getSimpleName());
        } else {
            sb.append(info.rawType().classObject().getName());
        }
        sb.append('<').append(args[0].getTypeName());
        for (int i = 1; i < args.length; i++) {
            sb.append(',').append(args[i].getTypeName());
        }
        return sb.append(">").toString();
    }
}
