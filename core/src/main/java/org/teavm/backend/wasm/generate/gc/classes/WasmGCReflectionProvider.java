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
package org.teavm.backend.wasm.generate.gc.classes;

import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmStructure;

public interface WasmGCReflectionProvider {
    int FIELD_NAME = 0;
    int FIELD_MODIFIERS = 1;
    int FIELD_ACCESS = 2;
    int FIELD_ANNOTATIONS = 3;

    int FIELD_TYPE = 4;
    int FIELD_READER = 5;
    int FIELD_WRITER = 6;

    int FIELD_RETURN_TYPE = 4;
    int FIELD_PARAMETER_TYPES = 5;
    int FIELD_CALLER = 6;

    WasmStructure getReflectionFieldType();

    WasmArray getReflectionFieldArrayType();

    WasmStructure getReflectionMethodType();

    WasmArray getReflectionMethodArrayType();

    WasmArray getClassArrayType();
}
