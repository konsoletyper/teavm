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
package org.teavm.backend.c.generate;

import org.teavm.backend.c.util.InteropUtil;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.model.ClassReader;
import org.teavm.model.ValueType;

public final class CodeGeneratorUtil {
    private CodeGeneratorUtil() {
    }

    public static void writeIntValue(CodeWriter writer, int i) {
        if (i == Integer.MIN_VALUE) {
            writer.print("(int32_t) INT32_C(0x80000000)");
        } else {
            long v = i;
            if (i < 0) {
                writer.print("-");
                v = -v;
            }
            writer.print("INT32_C(");
            writeLongConstant(writer, v);
            writer.print(")");
        }
    }

    public static void writeValue(CodeWriter writer, GenerationContext context, IncludeManager includes,
            Object value) {
        if (value == null) {
            writer.print("NULL");
        } else if (value instanceof String) {
            includes.includePath("strings.h");
            int index = context.getStringPool().getStringIndex((String) value);
            writer.print("TEAVM_GET_STRING(" + index + ")");
        } else if (value instanceof Integer) {
            writeIntValue(writer, (Integer) value);
        } else if (value instanceof Long) {
            long v = (Long) value;
            if (v == Long.MIN_VALUE) {
                writer.print("(int64_t) INT64_C(0x8000000000000000)");
            } else {
                if (v < 0) {
                    writer.print("-");
                    v = -v;
                }
                writer.print("INT64_C(");
                writeLongConstant(writer, v);
                writer.print(")");
            }
        } else if (value instanceof Float) {
            float f = (Float) value;
            if (Float.isInfinite(f)) {
                if (f < 0) {
                    writer.print("-");
                }
                writer.print("INFINITY");
            } else if (Float.isNaN(f)) {
                writer.print("NAN");
            } else  if ((int) f == f) {
                writer.print(f + "f");
            } else {
                writer.print(Float.toHexString(f) + "f");
            }
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (Double.isInfinite(d)) {
                if (d < 0) {
                    writer.print("-");
                }
                writer.print("INFINITY");
            } else if (Double.isNaN(d)) {
                writer.print("NAN");
            } else if ((int) d == d) {
                writer.print(value.toString());
            } else {
                writer.print(Double.toHexString(d));
            }
        } else if (value instanceof Boolean) {
            writer.print((Boolean) value ? "1" : "0");
        } else if (value instanceof Character) {
            writeIntValue(writer, (char) value);
        } else if (value instanceof ValueType) {
            ValueType type = (ValueType) value;
            if (type instanceof ValueType.Object
                    && !context.getCharacteristics().isManaged(((ValueType.Object) type).getClassName())) {
                writer.print("NULL");
            } else {
                includes.includeType(type);
                writer.print("&").print(context.getNames().forClassInstance(type));
            }
        }
    }

    private static void writeLongConstant(CodeWriter writer, long v) {
        if (v == Long.MIN_VALUE) {
            writer.print("0x8000000000000000");
            return;
        }
        writer.print(String.valueOf(v));
    }

    public static void printClassReference(CodeWriter writer, IncludeManager includes, NameProvider names,
            ClassReader cls, String className) {
        if (cls != null && InteropUtil.isNative(cls)) {
            InteropUtil.processInclude(cls.getAnnotations(), includes);
            InteropUtil.printNativeReference(writer, cls);
        } else {
            includes.includeClass(className);
            writer.print(names.forClass(className));
        }
    }
}
