/*
 *  Copyright 2024 konsoletyper.
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
package org.teavm.backend.wasm.parser;

import java.util.Objects;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmType;

public class WasmHollowType {
    public static final Number INT32 = new Number(WasmNumType.INT32);
    public static final Number INT64 = new Number(WasmNumType.INT64);
    public static final Number FLOAT32 = new Number(WasmNumType.FLOAT32);
    public static final Number FLOAT64 = new Number(WasmNumType.FLOAT64);

    private WasmHollowType() {
    }

    public static Number num(WasmNumType number) {
        switch (number) {
            case INT32:
                return INT32;
            case INT64:
                return INT64;
            case FLOAT32:
                return FLOAT32;
            case FLOAT64:
                return FLOAT64;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static final class Number extends WasmHollowType {
        public final WasmNumType number;

        private Number(WasmNumType number) {
            this.number = number;
        }
    }


    public static abstract class Reference extends WasmHollowType {
        private static final SpecialReference[] references = new SpecialReference[
                WasmType.SpecialReferenceKind.values().length];
        private static final SpecialReference[] nonNullReferences = new SpecialReference[
                WasmType.SpecialReferenceKind.values().length];

        private final boolean nullable;

        public Reference(boolean nullable) {
            this.nullable = nullable;
        }

        public boolean isNullable() {
            return nullable;
        }

        public static SpecialReference special(WasmType.SpecialReferenceKind kind) {
            var result = references[kind.ordinal()];
            if (result == null) {
                result = new SpecialReference(kind, true);
                references[kind.ordinal()] = result;
            }
            return result;
        }

        public static SpecialReference nonNullSpecial(WasmType.SpecialReferenceKind kind) {
            var result = nonNullReferences[kind.ordinal()];
            if (result == null) {
                result = new SpecialReference(kind, false);
                nonNullReferences[kind.ordinal()] = result;
            }
            return result;
        }
    }

    public static final class CompositeReference extends WasmHollowType.Reference {
        public final int index;

        public CompositeReference(int index, boolean nullable) {
            super(nullable);
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CompositeReference)) {
                return false;
            }
            CompositeReference that = (CompositeReference) o;
            return index == that.index && isNullable() == that.isNullable();
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, isNullable());
        }
    }

    public static final class SpecialReference extends WasmHollowType.Reference {
        public final WasmType.SpecialReferenceKind kind;

        SpecialReference(WasmType.SpecialReferenceKind kind, boolean nullable) {
            super(nullable);
            this.kind = kind;
        }
    }
}
