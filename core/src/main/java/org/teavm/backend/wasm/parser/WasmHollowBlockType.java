/*
 *  Copyright 2025 Alexey Andreev.
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

public class WasmHollowBlockType {
    private WasmHollowBlockType() {
    }

    public static class Function extends WasmHollowBlockType {
        public final int ref;

        public Function(int ref) {
            this.ref = ref;
        }
    }

    public static class Value extends WasmHollowBlockType {
        public final WasmHollowType type;

        public Value(WasmHollowType type) {
            this.type = type;
        }
    }
}
