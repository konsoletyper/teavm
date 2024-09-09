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
package org.teavm.backend.wasm.intrinsics.gc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt31Get;
import org.teavm.backend.wasm.model.expression.WasmInt31Reference;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmTest;
import org.teavm.model.ValueType;

public class ObjectIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "getClass":
                return generateGetClass(invocation, context);
            case "getMonitor":
                return generateGetMonitor(invocation, context);
            case "setMonitor":
                return generateSetMonitor(invocation, context);
            case "wasmGCIdentity":
                return generateGetIdentity(invocation, context);
            case "setWasmGCIdentity":
                return generateSetIdentity(invocation, context);
            case "cloneObject":
                return generateClone(invocation, context);
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmExpression generateGetClass(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var obj = context.generate(invocation.getArguments().get(0));
        var objectStruct = context.classInfoProvider().getClassInfo("java.lang.Object").getStructure();
        var result = new WasmStructGet(objectStruct, obj, WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
        result.setLocation(invocation.getLocation());
        return result;
    }

    private WasmExpression generateGetMonitor(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var monitorStruct = context.classInfoProvider().getClassInfo(ValueType.object("java.lang.Object$Monitor"))
                .getStructure();
        var monitorType = monitorStruct.getReference();
        var monitorNotNullType = monitorStruct.getNonNullReference();
        var objectStruct = context.classInfoProvider().getClassInfo(ValueType.object("java.lang.Object"))
                .getStructure();
        var block = new WasmBlock(false);
        block.setType(monitorType);
        var tmpVar = context.tempVars().acquire(WasmType.Reference.ANY);
        var instance = context.generate(invocation.getArguments().get(0));
        block.getBody().add(new WasmSetLocal(tmpVar, new WasmStructGet(objectStruct, instance,
                WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET)));

        WasmExpression test = new WasmTest(new WasmGetLocal(tmpVar), monitorNotNullType);
        test = new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, test);
        var branch = new WasmBranch(test, block);
        branch.setResult(new WasmNullConstant(monitorType));
        block.getBody().add(new WasmDrop(branch));

        block.getBody().add(new WasmCast(new WasmGetLocal(tmpVar), monitorNotNullType));
        context.tempVars().release(tmpVar);
        return block;
    }

    private WasmExpression generateSetMonitor(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var objectStruct = context.classInfoProvider().getClassInfo(ValueType.object("java.lang.Object"))
                .getStructure();
        var instance = context.generate(invocation.getArguments().get(0));
        var monitor = context.generate(invocation.getArguments().get(1));
        return new WasmStructSet(objectStruct, instance, WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET, monitor);
    }

    private WasmExpression generateGetIdentity(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var objectStruct = context.classInfoProvider().getClassInfo(ValueType.object("java.lang.Object"))
                .getStructure();
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        var tmpVar = context.tempVars().acquire(WasmType.Reference.ANY);
        var instance = context.generate(invocation.getArguments().get(0));
        block.getBody().add(new WasmSetLocal(tmpVar, new WasmStructGet(objectStruct, instance,
                WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET)));

        WasmExpression test = new WasmTest(new WasmGetLocal(tmpVar),
                WasmType.SpecialReferenceKind.I31.asNonNullType());
        test = new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, test);
        var branch = new WasmBranch(test, block);
        branch.setResult(new WasmInt32Constant(-1));
        block.getBody().add(new WasmDrop(branch));

        var i31ref = new WasmCast(new WasmGetLocal(tmpVar), WasmType.SpecialReferenceKind.I31.asNonNullType());
        block.getBody().add(new WasmInt31Get(i31ref, WasmSignedType.UNSIGNED));
        context.tempVars().release(tmpVar);
        return block;
    }

    private WasmExpression generateSetIdentity(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var objectStruct = context.classInfoProvider().getClassInfo(ValueType.object("java.lang.Object"))
                .getStructure();
        var instance = context.generate(invocation.getArguments().get(0));
        var identity = context.generate(invocation.getArguments().get(1));
        var identityWrapper = new WasmInt31Reference(identity);
        return new WasmStructSet(objectStruct, instance, WasmGCClassInfoProvider.MONITOR_FIELD_OFFSET,
                identityWrapper);
    }

    private WasmExpression generateClone(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var objectStruct = context.classInfoProvider().getClassInfo("java.lang.Object").getStructure();
        var classStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();

        var block = new WasmBlock(false);
        block.setType(objectStruct.getReference());
        var obj = context.exprCache().create(context.generate(invocation.getArguments().get(0)),
                objectStruct.getReference(), invocation.getLocation(), block.getBody());
        var cls = new WasmStructGet(objectStruct, obj.expr(), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
        var functionRef = new WasmStructGet(classStruct, cls, context.classInfoProvider().getCloneOffset());
        var call = new WasmCallReference(functionRef, context.functionTypes().of(
                objectStruct.getReference(), objectStruct.getReference()));
        call.getArguments().add(obj.expr());
        block.getBody().add(call);

        obj.release();
        return block;
    }
}
