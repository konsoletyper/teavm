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
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
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
    @Rename("toString")
    public TString toString0() {
        return name;
    }

    @Override
    public final boolean equals(TObject other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    protected final TObject clone() throws TCloneNotSupportedException {
        throw new TCloneNotSupportedException();
    }

    @SuppressWarnings("unchecked")
    public final TClass<E> getDeclaringClass() {
        TClass<?> thisClass = TClass.wrap(getClass());
        TClass<?> superClass = thisClass.getSuperclass();
        return (TClass<E>)(superClass == TClass.wrap(TEnum.class) ? thisClass : superClass);
    }

    @Override
    public final int compareTo(E o) {
        if (o.getDeclaringClass() != getDeclaringClass()) {
            throw new TIllegalArgumentException(TString.wrap("Can't compare " +
                    getDeclaringClass().getName().toString() + " to " +
                    o.getDeclaringClass().getName().toString()));
        }
        return TInteger.compare(ordinal, o.ordinal());
    }
}
