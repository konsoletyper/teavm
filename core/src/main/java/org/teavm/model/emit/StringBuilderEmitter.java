/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.emit;

import org.teavm.model.ValueType;
import org.teavm.model.instructions.IntegerSubtype;

public class StringBuilderEmitter {
    private ProgramEmitter pe;
    private ValueEmitter sb;

    StringBuilderEmitter(ProgramEmitter pe) {
        this.pe = pe;
        sb = pe.construct(StringBuilder.class);
    }

    public StringBuilderEmitter append(ValueEmitter value) {
        if (value.getType() instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) value.getType()).getKind()) {
                case BYTE:
                    value = value.castToInteger(IntegerSubtype.BYTE);
                    break;
                case SHORT:
                    value = value.castToInteger(IntegerSubtype.SHORT);
                    break;
                default:
                    break;
            }
        } else {
            if (!value.getType().isObject("java.lang.String")) {
                value = value.cast(Object.class);
            }
        }
        sb = sb.invokeVirtual("append", StringBuilder.class, value);
        return this;
    }

    public StringBuilderEmitter append(String value) {
        sb = sb.invokeVirtual("append", StringBuilder.class, pe.constant(value));
        return this;
    }

    public StringBuilderEmitter append(int value) {
        sb = sb.invokeVirtual("append", StringBuilder.class, pe.constant(value));
        return this;
    }

    public StringBuilderEmitter append(long value) {
        sb = sb.invokeVirtual("append", StringBuilder.class, pe.constant(value));
        return this;
    }

    public StringBuilderEmitter append(float value) {
        sb = sb.invokeVirtual("append", StringBuilder.class, pe.constant(value));
        return this;
    }

    public StringBuilderEmitter append(double value) {
        sb = sb.invokeVirtual("append", StringBuilder.class, pe.constant(value));
        return this;
    }

    public ValueEmitter build() {
        return sb.invokeVirtual("toString", String.class);
    }
}
