/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.wasm.render;

public interface WasmBinaryStatsCollector {
    void addClassCodeSize(String className, int bytes);

    void addClassMetadataSize(String className, int bytes);

    void addStringsSize(int bytes);

    void addSectionSize(String name, int bytes);

    WasmBinaryStatsCollector EMPTY = new WasmBinaryStatsCollector() {
        @Override
        public void addClassCodeSize(String className, int bytes) {
        }

        @Override
        public void addClassMetadataSize(String className, int bytes) {
        }

        @Override
        public void addStringsSize(int bytes) {
        }

        @Override
        public void addSectionSize(String name, int bytes) {
        }
    };
}
