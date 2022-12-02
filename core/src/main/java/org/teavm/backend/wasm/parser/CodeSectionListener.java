/*
 *  Copyright 2022 Alexey Andreev.
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

import org.teavm.backend.wasm.model.WasmType;

public interface CodeSectionListener {
    void address(int address);

    void sectionStart(int functionCount);

    boolean functionStart(int index, int size);

    void localsStart(int count);

    void local(int start, int count, WasmType type);

    void localsEnd();

    void functionEnd();

    void sectionEnd();
}
