/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.model;

public abstract class WasmType {
    public static final WasmType.Number INT32 = new Number(WasmNumType.INT32);
    public static final WasmType.Number INT64 = new Number(WasmNumType.INT64);
    public static final WasmType.Number FLOAT32 = new Number(WasmNumType.FLOAT32);
    public static final WasmType.Number FLOAT64 = new Number(WasmNumType.FLOAT64);

    private WasmStorageType.Regular storageType;

    private WasmType() {
    }

    public WasmStorageType.Regular asStorage() {
        if (storageType == null) {
            storageType = new WasmStorageType.Regular(this);
        }
        return storageType;
    }

    public static WasmType.Number num(WasmNumType number) {
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

    public static final class Number extends WasmType {
        public final WasmNumType number;

        private Number(WasmNumType number) {
            this.number = number;
        }
    }

    public static abstract class Reference extends WasmType {
        public static final SpecialReference FUNC = SpecialReferenceKind.FUNC.asType();
        public static final SpecialReference ANY = SpecialReferenceKind.ANY.asType();
        public static final SpecialReference EXTERN = SpecialReferenceKind.EXTERN.asType();
        public static final SpecialReference STRUCT = SpecialReferenceKind.STRUCT.asType();
        public static final SpecialReference ARRAY = SpecialReferenceKind.ARRAY.asType();
        public static final SpecialReference I31 = SpecialReferenceKind.I31.asType();

        private final boolean nullable;

        public Reference(boolean nullable) {
            this.nullable = nullable;
        }

        public boolean isNullable() {
            return nullable;
        }
    }

    public static final class CompositeReference extends Reference {
        public final WasmCompositeType composite;

        CompositeReference(WasmCompositeType composite, boolean nullable) {
            super(nullable);
            this.composite = composite;
        }
    }

    public static final class SpecialReference extends Reference {
        public final SpecialReferenceKind kind;

        private SpecialReference(SpecialReferenceKind kind, boolean nullable) {
            super(nullable);
            this.kind = kind;
        }
    }

    public enum SpecialReferenceKind {
        FUNC,
        ANY,
        EXTERN,
        STRUCT,
        ARRAY,
        I31;

        private SpecialReference type;
        private SpecialReference nonNullType;

        public final SpecialReference asType() {
            if (type == null) {
                type = new SpecialReference(this, true);
            }
            return type;
        }

        public final SpecialReference asNonNullType() {
            if (nonNullType == null) {
                nonNullType = new SpecialReference(this, false);
            }
            return nonNullType;
        }
    }
}
