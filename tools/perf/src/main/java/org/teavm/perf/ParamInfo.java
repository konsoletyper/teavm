/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.perf;

import java.util.List;
import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;

/**
 * Describes a field marked with JMH's {@code @Param} annotation.
 */
public final class ParamInfo {
    private final int index;
    private final int stateIndex;
    private final FieldReference field;
    private final ValueType type;
    private final List<String> defaultValues;

    ParamInfo(int index, int stateIndex, FieldReference field, ValueType type, List<String> defaultValues) {
        this.index = index;
        this.stateIndex = stateIndex;
        this.field = field;
        this.type = type;
        this.defaultValues = List.copyOf(defaultValues);
    }

    public int getIndex() {
        return index;
    }

    public int getStateIndex() {
        return stateIndex;
    }

    public FieldReference getField() {
        return field;
    }

    public String getName() {
        return field.getFieldName();
    }

    public ValueType getType() {
        return type;
    }

    public List<String> getDefaultValues() {
        return defaultValues;
    }
}
