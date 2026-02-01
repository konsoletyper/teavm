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
import org.teavm.runtime.reflect.GenericTypeInfo;
import org.teavm.runtime.reflect.WildcardTypeInfo;

class TWildcardTypeImpl implements TWildcardType {
    private TGenericDeclaration declaration;
    private WildcardTypeInfo info;
    private TType[] upperBounds;
    private TType[] lowerBounds;

    TWildcardTypeImpl(TGenericDeclaration declaration, WildcardTypeInfo info) {
        this.declaration = declaration;
        this.info = info;
    }

    @Override
    public TType[] getUpperBounds() {
        if (upperBounds == null) {
            if (info.kind() == GenericTypeInfo.Kind.UPPER_BOUND_WILDCARD) {
                upperBounds = new TType[] { TGenericTypeFactory.create(declaration, info.bound()) };
            } else {
                upperBounds = new TType[] { (TType) (Object) Object.class };
            }
        }
        return upperBounds.clone();
    }

    @Override
    public TType[] getLowerBounds() {
        if (upperBounds == null) {
            if (info.kind() == GenericTypeInfo.Kind.LOWER_BOUND_WILDCARD) {
                lowerBounds = new TType[] { TGenericTypeFactory.create(declaration, info.bound()) };
            } else {
                lowerBounds = new TType[0];
            }
        }
        return lowerBounds.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TWildcardTypeImpl)) {
            return false;
        }
        var that = (TWildcardTypeImpl) o;
        return Objects.deepEquals(upperBounds, that.upperBounds) && Objects.deepEquals(lowerBounds, that.lowerBounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(upperBounds), Arrays.hashCode(lowerBounds));
    }

    @Override
    public String toString() {
        switch (info.kind()) {
            case GenericTypeInfo.Kind.LOWER_BOUND_WILDCARD:
                return "? extends " + getUpperBounds()[0].getTypeName();
            case GenericTypeInfo.Kind.UPPER_BOUND_WILDCARD:
                return "? super " + getLowerBounds()[0].getTypeName();
            default:
                return "?";
        }
    }
}
