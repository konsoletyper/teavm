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

import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.ValueType;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.reflect.ClassInfo;

public final class ClassGeneratorUtil {
    private ClassGeneratorUtil() {
    }

    public static int applyPrimitiveFlags(int flags, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            flags |= getPrimitiveFlag((ValueType.Primitive) type) << RuntimeClass.PRIMITIVE_TYPE_SHIFT;
        } else {
            flags |=  ClassInfo.PrimitiveKind.VOID << RuntimeClass.PRIMITIVE_TYPE_SHIFT;
        }
        return flags;
    }

    private static int getPrimitiveFlag(ValueType.Primitive type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return ClassInfo.PrimitiveKind.BOOLEAN;
            case BYTE:
                return ClassInfo.PrimitiveKind.BYTE;
            case SHORT:
                return ClassInfo.PrimitiveKind.SHORT;
            case CHARACTER:
                return ClassInfo.PrimitiveKind.CHAR;
            case INTEGER:
                return ClassInfo.PrimitiveKind.INT;
            case LONG:
                return ClassInfo.PrimitiveKind.LONG;
            case FLOAT:
                return ClassInfo.PrimitiveKind.FLOAT;
            case DOUBLE:
                return ClassInfo.PrimitiveKind.DOUBLE;
            default:
                throw new AssertionError();
        }
    }

    public static int contributeToFlags(ClassReader cls, int flags) {
        if (cls != null) {
            switch (cls.getName()) {
                case "java.lang.ref.WeakReference":
                    flags |= RuntimeClass.VM_TYPE_WEAKREFERENCE << RuntimeClass.VM_TYPE_SHIFT;
                    break;
                case "java.lang.ref.ReferenceQueue":
                    flags |= RuntimeClass.VM_TYPE_REFERENCEQUEUE << RuntimeClass.VM_TYPE_SHIFT;
                    break;
            }
        }

        return flags;
    }


    public static boolean isBufferObjectField(FieldReader cls) {
        return cls.getAnnotations().get("java.nio.NativeBufferObjectMarker") != null;
    }
}
