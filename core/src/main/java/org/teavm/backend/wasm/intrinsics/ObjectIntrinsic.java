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

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmSignedType;
import org.teavm.model.ValueType;

public class ObjectIntrinsic implements WasmGCInlineIntrinsic {
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmFunctionTypes functionTypes;

    public ObjectIntrinsic(WasmGCClassInfoProvider classInfoProvider, WasmFunctionTypes functionTypes) {
        this.classInfoProvider = classInfoProvider;
        this.functionTypes = functionTypes;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "getClassInfo":
                generateGetClass(invocation, context, builder);
                break;
            case "getMonitor":
                generateGetMonitor(invocation, context, builder);
                break;
            case "setMonitor":
                generateSetMonitor(invocation, context, builder);
                break;
            case "wasmGCIdentity":
                generateGetIdentity(invocation, context, builder);
                break;
            case "setWasmGCIdentity":
                generateSetIdentity(invocation, context, builder);
                break;
            case "cloneObject":
                generateClone(invocation, context, builder);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void generateGetClass(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var objectInfo = classInfoProvider.getClassInfo("java.lang.Object");
        var objectStruct = objectInfo.getStructure();
        context.generate(builder, invocation.getArguments().get(0));
        builder
                .structGet(objectStruct, WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                .structGet(objectInfo.getVirtualTableStructure(), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
    }

    private void generateGetMonitor(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var monitorStruct = classInfoProvider.getClassInfo(ValueType.object("java.lang.Object$Monitor"))
                .getStructure();
        var monitorType = monitorStruct.getReference();
        var monitorNotNullType = monitorStruct.getNonNullReference();
        var objectStruct = classInfoProvider.getClassInfo(ValueType.object("java.lang.Object"))
                .getStructure();
        var tmpVar = context.tempVars().acquire(WasmType.ANY);
        var block = builder.block(monitorType);

        context.generate(block, invocation.getArguments().get(0));
        block
                .structGet(objectStruct, WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET)
                .setLocal(tmpVar)
                .nullConst(monitorType)
                .getLocal(tmpVar)
                .test(monitorNotNullType)
                .intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ)
                .branch(block)
                .drop()
                .getLocal(tmpVar)
                .cast(monitorNotNullType);
        context.tempVars().release(tmpVar);
    }

    private void generateSetMonitor(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var objectStruct = classInfoProvider.getClassInfo(ValueType.object("java.lang.Object")).getStructure();
        context.generate(builder, invocation.getArguments().get(0));
        context.generate(builder, invocation.getArguments().get(1));
        builder.structSet(objectStruct, WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET);
    }

    private void generateGetIdentity(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var objectStruct = classInfoProvider.getClassInfo(ValueType.object("java.lang.Object")).getStructure();
        var tmpVar = context.tempVars().acquire(WasmType.ANY);
        var block = builder.block(WasmType.INT32);

        context.generate(block, invocation.getArguments().get(0));
        block
                .structGet(objectStruct, WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET)
                .setLocal(tmpVar)
                .i32Const(-1)
                .getLocal(tmpVar)
                .test(WasmType.SpecialReferenceKind.I31.asNonNullType())
                .negate()
                .branch(block)
                .drop()
                .getLocal(tmpVar)
                .cast(WasmType.SpecialReferenceKind.I31.asNonNullType())
                .i31Get(WasmSignedType.UNSIGNED);
        context.tempVars().release(tmpVar);
    }

    private void generateSetIdentity(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var objectStruct = classInfoProvider.getClassInfo(ValueType.object("java.lang.Object"))
                .getStructure();
        context.generate(builder, invocation.getArguments().get(0));
        context.generate(builder, invocation.getArguments().get(1));
        builder
                .i31Ref()
                .structSet(objectStruct, WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET);
    }

    private void generateClone(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var objectInfo = classInfoProvider.getClassInfo("java.lang.Object");
        var objectStruct = objectInfo.getStructure();
        var classStruct = classInfoProvider.reflectionTypes().classInfo();

        context.generate(builder, invocation.getArguments().get(0));
        var cachedObj = context.valueCache().create(objectStruct.getReference(), builder);
        builder
                .append(cachedObj)
                .structGet(objectStruct, WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                .structGet(objectInfo.getVirtualTableStructure(), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET)
                .structGet(classStruct.structure(), classStruct.cloneFunctionIndex())
                .callReference(functionTypes.of(objectStruct.getReference(), objectStruct.getReference()));

        cachedObj.release();
    }
}
