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

import org.teavm.model.ValueType;

public final class CodeGeneratorUtil {
    private CodeGeneratorUtil() {
    }

    public static void writeValue(CodeWriter writer, GenerationContext context, Object value) {
        if (value == null) {
            writer.print("NULL");
        } else if (value instanceof String) {
            int index = context.getStringPool().getStringIndex((String) value);
            writer.print("(stringPool + " + index + ")");
        } else if (value instanceof Integer) {
            int i = (Integer) value;
            long v = i;
            if (i == Integer.MIN_VALUE) {
                writer.print("(int32_t) INT32_C(0x80000000)");
            } else {
                if (i < 0) {
                    writer.print("-");
                    v = -v;
                }
                writer.print("INT32_C(");
                writeLongConstant(writer, v);
                writer.print(")");
            }
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
            } else {
                writer.print(f + "f");
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
            } else {
                writer.print(value.toString());
            }
        } else if (value instanceof Boolean) {
            writer.print((Boolean) value ? "1" : "0");
        } else if (value instanceof ValueType) {
            writer.print("&").print(context.getNames().forClassInstance((ValueType) value));
        }
    }

    private static void writeLongConstant(CodeWriter writer, long v) {
        if (v == Long.MIN_VALUE) {
            writer.print("0x8000000000000000");
            return;
        }
        writer.print(String.valueOf(v));
    }

}
