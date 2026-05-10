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
package org.teavm.platform.plugin.wasmgc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.ValueType;

class ResourceMethodIntrinsic implements WasmGCInlineIntrinsic {
    private WasmGCTypeMapper typeMapper;
    private int fieldIndex;

    ResourceMethodIntrinsic(WasmGCTypeMapper typeMapper, int fieldIndex) {
        this.typeMapper = typeMapper;
        this.fieldIndex = fieldIndex;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var structType = (WasmType.CompositeReference) typeMapper.mapType(
                ValueType.object(invocation.getMethod().getClassName()));
        var struct = (WasmStructure) structType.composite;
        context.generate(builder, invocation.getArguments().get(0));
        builder.structGet(struct, fieldIndex);
    }
}
