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
import org.teavm.classlib.impl.reflection.ObjectList;
import org.teavm.classlib.java.lang.TClass;

class TParameterizedTypeImpl extends TLazyResolvedType implements TParameterizedType {
    private TClass<?> rawType;
    private ObjectList actualTypeArguments;
    private TType[] actualTypeArgumentsArray;
    private TType ownerType;

    TParameterizedTypeImpl(TClass<?> rawType, ObjectList actualTypeArguments, TType ownerType) {
        this.rawType = rawType;
        this.actualTypeArguments = actualTypeArguments;
        this.ownerType = ownerType;
    }

    static TParameterizedTypeImpl create(TClass<?> rawType, ObjectList actualTypeArguments) {
        return new TParameterizedTypeImpl(rawType, actualTypeArguments, null);
    }

    @Override
    public TType[] getActualTypeArguments() {
        if (actualTypeArgumentsArray == null) {
            if (actualTypeArguments == null) {
                actualTypeArgumentsArray = new TType[0];
            } else {
                var array = actualTypeArguments.asArray();
                actualTypeArgumentsArray = new TType[array.length];
                System.arraycopy(array, 0, actualTypeArgumentsArray, 0, array.length);
            }
        }
        return actualTypeArgumentsArray.clone();
    }

    @Override
    public TType getRawType() {
        return rawType;
    }

    @Override
    public TType getOwnerType() {
        return ownerType;
    }

    @Override
    void resolve(TGenericDeclaration declaration) {
        if (actualTypeArguments == null) {
            actualTypeArgumentsArray = new TType[0];
        } else {
            var array = actualTypeArguments.asArray();
            actualTypeArgumentsArray = new TType[array.length];
            for (var i = 0; i < array.length; ++i) {
                actualTypeArgumentsArray[i] = TTypeVariableStub.resolve((TType) array[i], declaration);
            }
        }
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
        return rawType == that.rawType
                && Objects.equals(ownerType, that.ownerType)
                && Arrays.equals(getActualTypeArguments(), that.getActualTypeArguments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawType, ownerType, Arrays.hashCode(getActualTypeArguments()));
    }

    @Override
    public String toString() {
        var args = getActualTypeArguments();
        var sb = new StringBuilder(rawType.getName()).append('<').append(args[0]);
        for (int i = 1; i < args.length; i++) {
            sb.append(',').append(args[i]);
        }
        return sb.append(">").toString();
    }
}
