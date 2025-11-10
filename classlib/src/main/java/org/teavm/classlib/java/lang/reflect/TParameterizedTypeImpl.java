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

import org.teavm.classlib.java.lang.TClass;

class TParameterizedTypeImpl implements TParameterizedType {
    private TClass<?> rawType;
    private TType[] actualTypeArguments;
    private TType ownerType;

    TParameterizedTypeImpl(TClass<?> rawType, TType[] actualTypeArguments, TType ownerType) {
        this.actualTypeArguments = actualTypeArguments;
        this.ownerType = ownerType;
    }

    @Override
    public TType[] getActualTypeArguments() {
        return actualTypeArguments.clone();
    }

    @Override
    public TType getRawType() {
        return rawType;
    }

    @Override
    public TType getOwnerType() {
        return ownerType;
    }
}
