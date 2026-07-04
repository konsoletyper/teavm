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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.InvokeInstruction;

public class StructureIntrinsic implements WasmGCInlineIntrinsic {
    private final WasmGCClassInfoProvider classInfoProvider;

    public StructureIntrinsic(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    @Override
    public void apply(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "toAddress":
            case "cast":
                builder.getLocal(context.mapToLocal(invocation.getInstance()));
                break;
            case "sizeOf": {
                var type = (ValueType.Object) context.classConstant(invocation.getArguments().get(0));
                builder.i32Const(classInfoProvider.getHeapSize(type.getClassName()));
                break;
            }
            case "add": {
                var type = context.classConstant(invocation.getArguments().get(0));
                var className = ((ValueType.Object) type).getClassName();
                int size = classInfoProvider.getHeapSize(className);
                int alignment = classInfoProvider.getHeapAlignment(className);
                size = WasmGeneratorUtil.align(size, alignment);

                builder.getLocal(context.mapToLocal(invocation.getArguments().get(1)))
                        .getLocal(context.mapToLocal(invocation.getArguments().get(2)))
                        .i32Const(size).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
                break;
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
