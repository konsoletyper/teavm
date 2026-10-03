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

    /** An argument value computed at compile time, which knows how to re-create itself in generated code. */
    interface StaticValue {
        ValueEmitter emit(ProgramEmitter pe, ValueType targetType);
    }
}
