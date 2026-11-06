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
package org.teavm.backend.wasm.model;

import java.util.function.Consumer;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;

/**
 * <p>Describes a function that takes class info and length, and returns a structure, whose last field
 * contains a newly allocated array of the given length, filled with default values.</p>
 *
 * <p>Header emitter must produce instructions that consume class info and produce values of all fields
 * of the structure, except for the last one, exactly as the described function initializes them.</p>
 *
 * <p>Fields of the structure are only accessed on demand, so the description can be created before
 * the structure is initialized.</p>
 */
public class WasmArrayConstructor {
    private final WasmStructure structure;
    private final Consumer<WasmInstructionBuilder> header;

    public WasmArrayConstructor(WasmStructure structure, Consumer<WasmInstructionBuilder> header) {
        this.structure = structure;
        this.header = header;
    }

    public WasmStructure getStructure() {
        return structure;
    }

    public int getDataField() {
        return structure.getFields().size() - 1;
    }

    public WasmArray getArray() {
        var type = (WasmType.CompositeReference) structure.getFields().get(getDataField()).getUnpackedType();
        return (WasmArray) type.composite;
    }

    public void emitHeader(WasmInstructionBuilder builder) {
        header.accept(builder);
    }
}
