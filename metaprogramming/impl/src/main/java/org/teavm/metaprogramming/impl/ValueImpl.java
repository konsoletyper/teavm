/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.metaprogramming.impl;

import org.teavm.metaprogramming.Value;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;

public class ValueImpl<T> implements Value<T> {
    Variable innerValue;
    VariableContext context;
    ValueType type;

    public ValueImpl(Variable innerValue, VariableContext context, ValueType type) {
        this.innerValue = innerValue;
        this.context = context;
        this.type = type;
    }

    @Override
    public T get() {
        throw new IllegalStateException("Can only read this value in emitter domain");
    }
}
