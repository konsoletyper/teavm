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
package org.teavm.backend.wasm.intrinsics.reflection;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.ValueType;

public class FieldInfoIntrinsic implements WasmGCInlineIntrinsic {
    private final WasmGCClassInfoProvider classInfoProvider;

    public FieldInfoIntrinsic(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var infoStruct = classInfoProvider.reflectionTypes().fieldInfo();
        switch (invocation.getMethod().getName()) {
            case "name":
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(infoStruct.structure(), infoStruct.nameIndex());
                break;
            case "modifiers":
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(infoStruct.structure(), infoStruct.modifiersIndex());
                break;
            case "type":
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(infoStruct.structure(), infoStruct.typeIndex());
                break;
            case "read": {
                // Stack: [rawReader, obj, readerConverter] → callReference → Object
                context.generate(builder, invocation.getArguments().get(0));
                var cachedFieldInfo = context.valueCache().create(infoStruct.structure().getReference(), builder);
                builder.structGet(infoStruct.structure(), infoStruct.readerIndex());
                context.generate(builder, invocation.getArguments().get(1));
                cachedFieldInfo.emit(builder);
                builder.structGet(infoStruct.structure(), infoStruct.readerConverterIndex())
                        .callReference(infoStruct.readerConverterType());
                cachedFieldInfo.release();
                break;
            }
            case "readAsBoolean":
            case "readAsByte":
            case "readAsShort":
            case "readAsChar":
            case "readAsInt":
                generatePrimitiveRead(invocation, context, builder, WasmType.INT32);
                break;
            case "readAsLong":
                generatePrimitiveRead(invocation, context, builder, WasmType.INT64);
                break;
            case "readAsFloat":
                generatePrimitiveRead(invocation, context, builder, WasmType.FLOAT32);
                break;
            case "readAsDouble":
                generatePrimitiveRead(invocation, context, builder, WasmType.FLOAT64);
                break;
            case "write":
                generateWrite(invocation, context, builder);
                break;
            case "reflection":
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(infoStruct.structure(), infoStruct.reflectionIndex());
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private void generatePrimitiveRead(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder, WasmType rawWasmType) {
        var infoStruct = classInfoProvider.reflectionTypes().fieldInfo();
        // Stack: [obj, rawReader(cast)] → callReference → rawValue
        context.generate(builder, invocation.getArguments().get(1));
        context.generate(builder, invocation.getArguments().get(0));
        var readerType = infoStruct.rawReaderFunctionType(rawWasmType);
        builder.structGet(infoStruct.structure(), infoStruct.readerIndex())
                .cast(readerType.getReference())
                .callReference(readerType);
    }

    private void generateWrite(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var infoStruct = classInfoProvider.reflectionTypes().fieldInfo();
        var valueParamType = invocation.getMethod().parameterType(1);
        if (valueParamType instanceof ValueType.Primitive) {
            // Primitive write: inline cast the raw writer funcref and call it directly
            // Stack: [obj, primitiveValue, rawWriter(cast)] → callReference → void
            var rawWasmType = switch (((ValueType.Primitive) valueParamType).getKind()) {
                case BOOLEAN, BYTE, SHORT, CHARACTER, INTEGER -> WasmType.INT32;
                case LONG -> WasmType.INT64;
                case FLOAT -> WasmType.FLOAT32;
                case DOUBLE -> WasmType.FLOAT64;
            };
            context.generate(builder, invocation.getArguments().get(1));
            context.generate(builder, invocation.getArguments().get(2));
            context.generate(builder, invocation.getArguments().get(0));
            var writerType = infoStruct.rawWriterFunctionType(rawWasmType);
            builder.structGet(infoStruct.structure(), infoStruct.writerIndex())
                    .cast(writerType.getReference())
                    .callReference(writerType);
        } else {
            // Generic Object write: use the writerConverter which handles unboxing internally
            // Stack: [rawWriter, obj, boxedValue, writerConverter] → callReference → void
            context.generate(builder, invocation.getArguments().get(0));
            var cachedFieldInfo = context.valueCache().create(infoStruct.structure().getReference(), builder);
            builder.structGet(infoStruct.structure(), infoStruct.writerIndex());
            context.generate(builder, invocation.getArguments().get(1));
            context.generate(builder, invocation.getArguments().get(2));
            cachedFieldInfo.emit(builder);
            builder.structGet(infoStruct.structure(), infoStruct.writerConverterIndex())
                    .callReference(infoStruct.writerConverterType());
            cachedFieldInfo.release();
        }
    }
}
