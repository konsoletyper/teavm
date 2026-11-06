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

import java.util.List;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

/**
 * Describes, in declaration order, where the arguments of a Jupiter {@code @ParameterizedTest}
 * come from, so that {@link TestEntryPointTransformer} can generate one launcher per invocation
 * inside the compiled artifact. Invocation {@code i} in the compiled artifact must correspond to
 * invocation {@code i} Jupiter performs on the JVM, so segments are kept in the same order Jupiter
 * evaluates argument sources.
 *
 * <p>Sources that only depend on annotation values ({@code @ValueSource}, {@code @CsvSource},
 * {@code @EnumSource}, {@code @NullSource}, {@code @EmptySource}) are evaluated on the JVM at
 * compile time and become {@link StaticRow}s. Sources that run user code ({@code @MethodSource},
 * {@code @FieldSource}) are evaluated inside the compiled artifact, the same way TestNG's
 * {@code @DataProvider} is.
 */
final class JupiterArgumentsPlan {
    final List<Segment> segments;

    /** Whether all invocations share one test instance ({@code @TestInstance(PER_CLASS)}). */
    final boolean sharedInstance;

    JupiterArgumentsPlan(List<Segment> segments, boolean sharedInstance) {
        this.segments = segments;
        this.sharedInstance = sharedInstance;
    }

    interface Segment {
    }

    /** One invocation with arguments known at compile time, already converted to parameter types. */
    static final class StaticRow implements Segment {
        final List<StaticValue> values;

        StaticRow(List<StaticValue> values) {
            this.values = values;
        }
    }

    /** Invocations produced by calling a factory method ({@code @MethodSource}). */
    static final class FactoryMethod implements Segment {
        final MethodReference method;
        final boolean isStatic;
        final boolean isPrivate;

        FactoryMethod(MethodReference method, boolean isStatic, boolean isPrivate) {
            this.method = method;
            this.isStatic = isStatic;
            this.isPrivate = isPrivate;
        }
    }

    /** Invocations produced by reading a field ({@code @FieldSource}). */
    static final class FactoryField implements Segment {
        final FieldReference field;
        final ValueType type;
        final boolean isStatic;

        FactoryField(FieldReference field, ValueType type, boolean isStatic) {
            this.field = field;
            this.type = type;
            this.isStatic = isStatic;
        }
    }

    /**
     * An argument value computed at compile time, which knows how to re-create itself in generated code.
     * Implementations form a closed set, so that plans can be serialized (see {@link JupiterArgumentsPlanCodec})
     * and passed to an external compiler.
     */
    interface StaticValue {
        ValueEmitter emit(ProgramEmitter pe, ValueType targetType);
    }

    static final class NullValue implements StaticValue {
        static final NullValue INSTANCE = new NullValue();

        private NullValue() {
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            return pe.constantNull(targetType);
        }
    }

    /**
     * Primitive constant. {@code bits} keeps value as {@code long}, for {@code float} and {@code double}
     * it's raw bit representation.
     */
    static final class PrimitiveValue implements StaticValue {
        final PrimitiveType kind;
        final long bits;

        PrimitiveValue(PrimitiveType kind, long bits) {
            this.kind = kind;
            this.bits = bits;
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            switch (kind) {
                case BOOLEAN:
                    return pe.constant((int) bits).cast(ValueType.BOOLEAN).cast(targetType);
                case CHARACTER:
                    return pe.constant((int) bits).cast(ValueType.CHARACTER).cast(targetType);
                case BYTE:
                    return pe.constant((int) bits).cast(ValueType.BYTE).cast(targetType);
                case SHORT:
                    return pe.constant((int) bits).cast(ValueType.SHORT).cast(targetType);
                case INTEGER:
                    return pe.constant((int) bits).cast(targetType);
                case LONG:
                    return pe.constant(bits).cast(targetType);
                case FLOAT:
                    return pe.constant(Float.intBitsToFloat((int) bits)).cast(targetType);
                default:
                    return pe.constant(Double.longBitsToDouble(bits)).cast(targetType);
            }
        }
    }

    static final class StringValue implements StaticValue {
        final String value;

        StringValue(String value) {
            this.value = value;
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            return pe.constant(value).cast(targetType);
        }
    }

    static final class ClassValue implements StaticValue {
        final ValueType type;

        ClassValue(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            return pe.constant(type).cast(targetType);
        }
    }

    static final class EnumValue implements StaticValue {
        final FieldReference field;

        EnumValue(FieldReference field) {
            this.field = field;
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            return pe.getField(field, ValueType.object(field.getClassName())).cast(targetType);
        }
    }

    /** Result of invocation of a static method, optionally taking single string argument. */
    static final class StaticCallValue implements StaticValue {
        final MethodReference method;
        final String argument;

        StaticCallValue(MethodReference method, String argument) {
            this.method = method;
            this.argument = argument;
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            var result = argument != null
                    ? pe.invoke(method, pe.constant(argument).cast(method.parameterType(0)))
                    : pe.invoke(method);
            return result.cast(targetType);
        }
    }

    /** Instance created by constructor, optionally taking single string argument. */
    static final class ConstructorValue implements StaticValue {
        final String className;
        final String argument;

        ConstructorValue(String className, String argument) {
            this.className = className;
            this.argument = argument;
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            var result = argument != null
                    ? pe.construct(className, pe.constant(argument))
                    : pe.construct(className);
            return result.cast(targetType);
        }
    }

    static final class EmptyArrayValue implements StaticValue {
        final ValueType componentType;

        EmptyArrayValue(ValueType componentType) {
            this.componentType = componentType;
        }

        @Override
        public ValueEmitter emit(ProgramEmitter pe, ValueType targetType) {
            return pe.constructArray(componentType, 0).cast(targetType);
        }
    }
}
