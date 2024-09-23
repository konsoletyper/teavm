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
package org.teavm.backend.wasm.parser;

public interface TypeSectionListener extends SectionListener {
    default void startRecType(int count) {
    }

    default void endRecType() {
    }

    default void startType(int index, boolean open, int[] supertypes) {
    }

    default void startArrayType() {
    }

    default void endArrayType() {
    }

    default void startStructType(int fieldCount) {
    }

    default void field(WasmHollowStorageType hollowType, boolean mutable) {
    }

    default void endStructType() {
    }

    default void funcType(int paramCount) {
    }

    default void funcTypeResults(int returnCount) {
    }

    default void resultType(WasmHollowType type) {
    }

    default void endFuncType() {
    }

    default void endType() {
    }
}
