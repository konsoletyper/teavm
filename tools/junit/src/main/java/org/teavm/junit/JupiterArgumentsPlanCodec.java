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
package org.teavm.junit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.ValueType;

/**
 * Serializes {@link JupiterArgumentsPlan}s into a string, so that they can be passed to an external
 * compiler (which can't run JUnit's reflection-based analysis itself), and parses them back.
 *
 * <p>The string is a sequence of tokens, each token is either {@code ~} (stands for {@code null})
 * or {@code <length>:<characters>}. The format is private to this class and may change any time,
 * so both sides must use the same version of TeaVM.
 */
final class JupiterArgumentsPlanCodec {
    private static final String ROW = "row";
    private static final String METHOD = "method";
    private static final String FIELD = "field";

    private static final String NULL = "null";
    private static final String PRIMITIVE = "primitive";
    private static final String STRING = "string";
    private static final String CLASS = "class";
    private static final String ENUM = "enum";
    private static final String STATIC_CALL = "call";
    private static final String CONSTRUCTOR = "new";
    private static final String EMPTY_ARRAY = "array";

    private JupiterArgumentsPlanCodec() {
    }

    static String encode(Map<MethodReference, JupiterArgumentsPlan> plans) {
        var writer = new Writer();
        writer.write(plans.size());
        for (var entry : plans.entrySet()) {
            writer.write(entry.getKey().toString());
            encodePlan(writer, entry.getValue());
        }
        return writer.sb.toString();
    }

    private static void encodePlan(Writer writer, JupiterArgumentsPlan plan) {
        writer.write(plan.sharedInstance);
        writer.write(plan.segments.size());
        for (var segment : plan.segments) {
            if (segment instanceof JupiterArgumentsPlan.StaticRow row) {
                writer.write(ROW);
                writer.write(row.values.size());
                for (var value : row.values) {
                    encodeValue(writer, value);
                }
            } else if (segment instanceof JupiterArgumentsPlan.FactoryMethod factory) {
                writer.write(METHOD);
                writer.write(factory.method.toString());
                writer.write(factory.isStatic);
                writer.write(factory.isPrivate);
            } else if (segment instanceof JupiterArgumentsPlan.FactoryField factory) {
                writer.write(FIELD);
                writer.write(factory.field.getClassName());
                writer.write(factory.field.getFieldName());
                writer.write(factory.type.toString());
                writer.write(factory.isStatic);
            } else {
                throw new IllegalArgumentException("Unsupported segment: " + segment.getClass().getName());
            }
        }
    }

    private static void encodeValue(Writer writer, JupiterArgumentsPlan.StaticValue value) {
        if (value instanceof JupiterArgumentsPlan.NullValue) {
            writer.write(NULL);
        } else if (value instanceof JupiterArgumentsPlan.PrimitiveValue primitive) {
            writer.write(PRIMITIVE);
            writer.write(primitive.kind.name());
            writer.write(Long.toString(primitive.bits));
        } else if (value instanceof JupiterArgumentsPlan.StringValue string) {
            writer.write(STRING);
            writer.write(string.value);
        } else if (value instanceof JupiterArgumentsPlan.ClassValue cls) {
            writer.write(CLASS);
            writer.write(cls.type.toString());
        } else if (value instanceof JupiterArgumentsPlan.EnumValue enumValue) {
            writer.write(ENUM);
            writer.write(enumValue.field.getClassName());
            writer.write(enumValue.field.getFieldName());
        } else if (value instanceof JupiterArgumentsPlan.StaticCallValue call) {
            writer.write(STATIC_CALL);
            writer.write(call.method.toString());
            writer.write(call.argument);
        } else if (value instanceof JupiterArgumentsPlan.ConstructorValue constructor) {
            writer.write(CONSTRUCTOR);
            writer.write(constructor.className);
            writer.write(constructor.argument);
        } else if (value instanceof JupiterArgumentsPlan.EmptyArrayValue array) {
            writer.write(EMPTY_ARRAY);
            writer.write(array.componentType.toString());
        } else {
            throw new IllegalArgumentException("Unsupported value: " + value.getClass().getName());
        }
    }

    static Map<MethodReference, JupiterArgumentsPlan> decode(String text) {
        var result = new LinkedHashMap<MethodReference, JupiterArgumentsPlan>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        var reader = new Reader(text);
        var count = reader.readInt();
        for (var i = 0; i < count; ++i) {
            var method = MethodReference.parse(reader.read());
            result.put(method, decodePlan(reader));
        }
        if (!reader.atEnd()) {
            throw new IllegalArgumentException("Unexpected data at position " + reader.index);
        }
        return result;
    }

    private static JupiterArgumentsPlan decodePlan(Reader reader) {
        var sharedInstance = reader.readBoolean();
        var segmentCount = reader.readInt();
        var segments = new ArrayList<JupiterArgumentsPlan.Segment>(segmentCount);
        for (var i = 0; i < segmentCount; ++i) {
            var kind = reader.read();
            switch (kind) {
                case ROW: {
                    var valueCount = reader.readInt();
                    var values = new ArrayList<JupiterArgumentsPlan.StaticValue>(valueCount);
                    for (var j = 0; j < valueCount; ++j) {
                        values.add(decodeValue(reader));
                    }
                    segments.add(new JupiterArgumentsPlan.StaticRow(values));
                    break;
                }
                case METHOD: {
                    var method = MethodReference.parse(reader.read());
                    var isStatic = reader.readBoolean();
                    var isPrivate = reader.readBoolean();
                    segments.add(new JupiterArgumentsPlan.FactoryMethod(method, isStatic, isPrivate));
                    break;
                }
                case FIELD: {
                    var className = reader.read();
                    var fieldName = reader.read();
                    var type = ValueType.parse(reader.read());
                    var isStatic = reader.readBoolean();
                    segments.add(new JupiterArgumentsPlan.FactoryField(new FieldReference(className, fieldName),
                            type, isStatic));
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown segment kind: " + kind);
            }
        }
        return new JupiterArgumentsPlan(segments, sharedInstance);
    }

    private static JupiterArgumentsPlan.StaticValue decodeValue(Reader reader) {
        var kind = reader.read();
        switch (kind) {
            case NULL:
                return JupiterArgumentsPlan.NullValue.INSTANCE;
            case PRIMITIVE: {
                var type = PrimitiveType.valueOf(reader.read());
                return new JupiterArgumentsPlan.PrimitiveValue(type, Long.parseLong(reader.read()));
            }
            case STRING:
                return new JupiterArgumentsPlan.StringValue(reader.read());
            case CLASS:
                return new JupiterArgumentsPlan.ClassValue(ValueType.parse(reader.read()));
            case ENUM: {
                var className = reader.read();
                var name = reader.read();
                return new JupiterArgumentsPlan.EnumValue(new FieldReference(className, name));
            }
            case STATIC_CALL: {
                var method = MethodReference.parse(reader.read());
                return new JupiterArgumentsPlan.StaticCallValue(method, reader.read());
            }
            case CONSTRUCTOR: {
                var className = reader.read();
                return new JupiterArgumentsPlan.ConstructorValue(className, reader.read());
            }
            case EMPTY_ARRAY:
                return new JupiterArgumentsPlan.EmptyArrayValue(ValueType.parse(reader.read()));
            default:
                throw new IllegalArgumentException("Unknown value kind: " + kind);
        }
    }

    private static class Writer {
        final StringBuilder sb = new StringBuilder();

        void write(String token) {
            if (token == null) {
                sb.append('~');
            } else {
                sb.append(token.length()).append(':').append(token);
            }
        }

        void write(int value) {
            write(Integer.toString(value));
        }

        void write(boolean value) {
            write(value ? "1" : "0");
        }
    }

    private static class Reader {
        final String text;
        int index;

        Reader(String text) {
            this.text = text;
        }

        boolean atEnd() {
            return index == text.length();
        }

        String read() {
            if (atEnd()) {
                throw new IllegalArgumentException("Unexpected end of data");
            }
            if (text.charAt(index) == '~') {
                index++;
                return null;
            }
            var colon = text.indexOf(':', index);
            if (colon < 0) {
                throw new IllegalArgumentException("Malformed token at position " + index);
            }
            var length = Integer.parseInt(text.substring(index, colon));
            var end = colon + 1 + length;
            if (end > text.length()) {
                throw new IllegalArgumentException("Token at position " + index + " exceeds data length");
            }
            index = end;
            return text.substring(colon + 1, end);
        }

        int readInt() {
            return Integer.parseInt(read());
        }

        boolean readBoolean() {
            return read().equals("1");
        }
    }
}
