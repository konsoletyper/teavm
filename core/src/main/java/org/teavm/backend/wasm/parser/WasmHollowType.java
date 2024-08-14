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
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmType;

public class WasmHollowType {
    public static final Number INT32 = new Number(WasmNumType.INT32);
    public static final Number INT64 = new Number(WasmNumType.INT64);
    public static final Number FLOAT32 = new Number(WasmNumType.FLOAT32);
    public static final Number FLOAT64 = new Number(WasmNumType.FLOAT64);

    private WasmStorageType.Regular storageType;

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
        public static final SpecialReference FUNC = new SpecialReference(WasmType.SpecialReferenceKind.FUNC);
        public static final SpecialReference ANY = new SpecialReference(WasmType.SpecialReferenceKind.ANY);
        public static final SpecialReference EXTERN = new SpecialReference(WasmType.SpecialReferenceKind.EXTERN);
        public static final SpecialReference STRUCT = new SpecialReference(WasmType.SpecialReferenceKind.STRUCT);
        public static final SpecialReference ARRAY = new SpecialReference(WasmType.SpecialReferenceKind.ARRAY);
    }

    public static final class CompositeReference extends WasmHollowType.Reference {
        public final int index;

        public CompositeReference(int index) {
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
            return index == that.index;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(index);
        }
    }

    public static final class SpecialReference extends WasmHollowType.Reference {
        public final WasmType.SpecialReferenceKind kind;

        SpecialReference(WasmType.SpecialReferenceKind kind) {
            this.kind = kind;
        }
    }
}
