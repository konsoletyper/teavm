/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.interop.Rename;

public abstract class TEnum<E extends TEnum<E>> extends TObject implements TComparable<E>, TSerializable {
    private TString name;
    private int ordinal;

    protected TEnum(TString name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public final TString name() {
        return name;
    }

    public final int ordinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public final boolean equals(Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Rename("clone")
    protected final TObject clone0() throws TCloneNotSupportedException {
        throw new TCloneNotSupportedException();
    }

    @SuppressWarnings("unchecked")
    public final TClass<E> getDeclaringClass() {
        Class<E> result = (Class<E>) getClass();
        return (TClass<E>) (Object) (result.getSuperclass().equals(Enum.class) ? result : result.getSuperclass());
    }

    @Override
    public final int compareTo(E o) {
        if (o.getDeclaringClass() != getDeclaringClass()) {
            throw new TIllegalArgumentException(TString.wrap("Can't compare "
                    + getDeclaringClass().getName().toString() + " to " + o.getDeclaringClass().getName().toString()));
        }
        return TInteger.compare(ordinal, o.ordinal());
    }

    public static <T extends TEnum<T>> T valueOf(TClass<T> enumType, TString name) {
        // TODO: speed-up this method, use caching
        T[] constants = enumType.getEnumConstants();
        if (constants == null) {
            throw new TIllegalArgumentException(TString.wrap("Class does not represent enum: " + enumType.getName()));
        }
        for (T constant : constants) {
            if (constant.name().equals(name)) {
                return constant;
            }
        }
        throw new TIllegalArgumentException(TString.wrap("Enum " + enumType.getName() + " does not have the " + name
                + "constant"));
    }
}
