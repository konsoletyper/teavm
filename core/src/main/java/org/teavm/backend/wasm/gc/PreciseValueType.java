/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.gc;

import java.util.Objects;
import org.teavm.model.ValueType;

public class PreciseValueType {
    public final ValueType valueType;
    public final boolean isArrayUnwrap;

    public PreciseValueType(ValueType valueType, boolean isArrayUnwrap) {
        this.valueType = valueType;
        this.isArrayUnwrap = isArrayUnwrap;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PreciseValueType)) {
            return false;
        }

        var that = (PreciseValueType) o;
        return isArrayUnwrap == that.isArrayUnwrap && valueType.equals(that.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueType, isArrayUnwrap);
    }
}
