/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.lowlevel.generate;

import org.teavm.model.ValueType;
import org.teavm.runtime.RuntimeClass;

public final class ClassGeneratorUtil {
    private ClassGeneratorUtil() {
    }

    public static int applyPrimitiveFlags(int flags, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            flags |= getPrimitiveFlag((ValueType.Primitive) type) << RuntimeClass.PRIMITIVE_SHIFT;
        } else {
            flags |= RuntimeClass.VOID_PRIMITIVE << RuntimeClass.PRIMITIVE_SHIFT;
        }
        return flags;
    }

    private static int getPrimitiveFlag(ValueType.Primitive type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return RuntimeClass.BOOLEAN_PRIMITIVE;
            case BYTE:
                return RuntimeClass.BYTE_PRIMITIVE;
            case SHORT:
                return RuntimeClass.SHORT_PRIMITIVE;
            case CHARACTER:
                return RuntimeClass.CHAR_PRIMITIVE;
            case INTEGER:
                return RuntimeClass.INT_PRIMITIVE;
            case LONG:
                return RuntimeClass.LONG_PRIMITIVE;
            case FLOAT:
                return RuntimeClass.FLOAT_PRIMITIVE;
            case DOUBLE:
                return RuntimeClass.DOUBLE_PRIMITIVE;
            default:
                throw new AssertionError();
        }
    }
}
