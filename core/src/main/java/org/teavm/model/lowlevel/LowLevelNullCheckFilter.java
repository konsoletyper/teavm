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
package org.teavm.model.lowlevel;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.transformation.NullCheckFilter;

public class LowLevelNullCheckFilter implements NullCheckFilter {
    private Characteristics characteristics;

    public LowLevelNullCheckFilter(Characteristics characteristics) {
        this.characteristics = characteristics;
    }

    @Override
    public boolean apply(FieldReference field) {
        return !characteristics.isStructure(field.getClassName())
                && !characteristics.isResource(field.getClassName());
    }

    @Override
    public boolean apply(MethodReference method) {
        return characteristics.isManaged(method.getClassName())
                && characteristics.isManaged(method)
                && !characteristics.isResource(method.getClassName());
    }
}
