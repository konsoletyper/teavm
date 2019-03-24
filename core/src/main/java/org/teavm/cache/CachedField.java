/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.cache;

import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.GenericValueType;
import org.teavm.model.ValueType;

class CachedField extends CachedMember implements FieldReader {
    ValueType type;
    GenericValueType genericType;
    Object initialValue;
    FieldReference reference;

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public GenericValueType getGenericType() {
        return genericType;
    }

    @Override
    public Object getInitialValue() {
        return initialValue;
    }

    @Override
    public FieldReference getReference() {
        return reference;
    }
}
