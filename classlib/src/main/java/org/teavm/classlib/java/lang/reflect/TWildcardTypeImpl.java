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

class TWildcardTypeImpl extends TLazyResolvedType implements TWildcardType {
    private TType[] upperBounds;
    private TType[] lowerBounds;

    private TWildcardTypeImpl(TType[] upperBounds, TType[] lowerBounds) {
        this.upperBounds = upperBounds;
        this.lowerBounds = lowerBounds;
    }

    static TWildcardTypeImpl upper(TType upperBound) {
        if (upperBound == null) {
            upperBound = (TType) (Object) Object.class;
        }
        return new TWildcardTypeImpl(new TType[] { upperBound }, new TType[0]);
    }

    static TWildcardTypeImpl lower(TType lowerBound) {
        return new TWildcardTypeImpl(new TType[] { (TType) (Object) Object.class }, new TType[] { lowerBound });
    }

    @Override
    public TType[] getUpperBounds() {
        return upperBounds.clone();
    }

    @Override
    public TType[] getLowerBounds() {
        return lowerBounds.clone();
    }

    @Override
    void resolve(TGenericDeclaration declaration) {
        if (upperBounds != null) {
            for (var i = 0; i < upperBounds.length; ++i) {
                upperBounds[i] = TTypeVariableStub.resolve(upperBounds[i], declaration);
            }
        }
        if (lowerBounds != null) {
            for (var i = 0; i < lowerBounds.length; ++i) {
                lowerBounds[i] = TTypeVariableStub.resolve(lowerBounds[i], declaration);
            }
        }
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
        if (lowerBounds.length == 0) {
            if (upperBounds[0].equals(Object.class)) {
                return "?";
            }
            return "? extends " + upperBounds[0].getTypeName();
        } else {
            return "? super " + lowerBounds[0].getTypeName();
        }
    }
}
