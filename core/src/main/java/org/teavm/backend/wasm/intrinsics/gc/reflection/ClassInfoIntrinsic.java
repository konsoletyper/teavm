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
package org.teavm.backend.wasm.intrinsics.gc.reflection;

import java.util.function.ToIntFunction;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.generate.gc.reflection.ClassInfoStruct;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.reflection.ReflectionDependencyListener;

public class ClassInfoIntrinsic implements WasmGCIntrinsic {
    private ReflectionDependencyListener reflection;

    public ClassInfoIntrinsic(ReflectionDependencyListener reflection) {
        this.reflection = reflection;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "modifiers":
                return fieldAccess(invocation, context, ClassInfoStruct::modifiersIndex);
            case "primitiveKind":
                return fieldAccess(invocation, context, ClassInfoStruct::primitiveKindIndex);
            case "parent":
                return fieldAccess(invocation, context, ClassInfoStruct::parentIndex);
            case "name":
                return fieldAccess(invocation, context, ClassInfoStruct::nameIndex);
            case "simpleName":
                return fieldAccess(invocation, context, ClassInfoStruct::simpleNameIndex);
            case "declaringClass":
                return fieldAccess(invocation, context, ClassInfoStruct::declaringClassIndex);
            case "enclosingClass":
                return fieldAccess(invocation, context, ClassInfoStruct::enclosingClassIndex);
            case "itemType":
                return fieldAccess(invocation, context, ClassInfoStruct::itemTypeIndex);
            case "classObject": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var fn = context.classInfoProvider().reflectionTypes().classInfo().classObjectFunction();
                return new WasmCall(fn, receiver);
            }
            case "isSuperTypeOf": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var other = context.generate(invocation.getArguments().get(1));
                return callVirtual(context, classInfoType.supertypeFunctionType(),
                        classInfoType.supertypeFunctionIndex(), receiver, other);
            }
            case "newArrayInstance": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var size = context.generate(invocation.getArguments().get(1));
                return callVirtual(context, classInfoType.newArrayFunctionType(),
                        classInfoType.newArrayFunctionIndex(), receiver, size);
            }
            case "arrayLength": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var array = context.generate(invocation.getArguments().get(1));
                return callVirtual(context, classInfoType.arrayLengthFunctionType(), classInfoType.arrayLengthIndex(),
                        receiver, array);
            }
            case "getItem": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var array = context.generate(invocation.getArguments().get(1));
                var index = context.generate(invocation.getArguments().get(2));
                return callVirtual(context, classInfoType.getItemFunctionType(), classInfoType.getItemIndex(),
                        receiver, array, index);
            }
            case "putItem": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var array = context.generate(invocation.getArguments().get(1));
                var index = context.generate(invocation.getArguments().get(2));
                var value = context.generate(invocation.getArguments().get(3));
                return callVirtual(context, classInfoType.putItemFunctionType(), classInfoType.putItemIndex(),
                        receiver, array, index, value);
            }
            case "initialize": {
                var initializer = fieldAccess(invocation, context, ClassInfoStruct::initializerIndex);
                var block = new WasmBlock(false);
                var br = new WasmNullBranch(WasmNullCondition.NULL, initializer, block);
                var call = new WasmCallReference(br, context.functionTypes().of(null));
                if (context.isAsyncMethod(invocation.getMethod())) {
                    call.setSuspensionPoint(true);
                }
                block.getBody().add(call);
                return block;
            }
            case "enumConstantCount": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var block = new WasmBlock(false);
                block.setType(WasmType.INT32.asBlock());
                var cachedReceiver = context.exprCache().create(
                        context.generate(invocation.getArguments().get(0)),
                        classInfoType.structure().getReference(), null, block.getBody());

                var initBlock = new WasmBlock(false);
                block.getBody().add(initBlock);

                var array = new WasmStructGet(classInfoType.structure(), cachedReceiver.expr(),
                        classInfoType.enumConstantsIndex());
                initBlock.getBody().add(new WasmNullBranch(WasmNullCondition.NOT_NULL, array, initBlock));
                var fnRef = new WasmStructGet(classInfoType.structure(), cachedReceiver.expr(),
                        classInfoType.initEnumConstantsIndex());
                var fnType = context.functionTypes().of(classInfoType.enumConstantsType().getReference());
                var call = new WasmCallReference(fnRef, fnType);
                initBlock.getBody().add(new WasmStructSet(classInfoType.structure(),
                        cachedReceiver.expr(), classInfoType.enumConstantsIndex(), call));

                array = new WasmStructGet(classInfoType.structure(), cachedReceiver.expr(),
                        classInfoType.enumConstantsIndex());
                block.getBody().add(new WasmArrayLength(array));
                return block;
            }
            case "enumConstant": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var array = fieldAccess(invocation, context, ClassInfoStruct::enumConstantsIndex);
                var index = context.generate(invocation.getArguments().get(1));
                return new WasmArrayGet(classInfoType.enumConstantsType(), array, index);
            }
            case "superinterfaceCount": {
                var interfaces = fieldAccess(invocation, context, ClassInfoStruct::interfacesIndex);
                return WasmGCGenerationUtil.getArrayLengthOfNullable(interfaces);
            }
            case "superinterface": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var interfaces = fieldAccess(invocation, context, ClassInfoStruct::interfacesIndex);
                var index = context.generate(invocation.getArguments().get(1));
                return new WasmArrayGet(classInfoType.interfacesType(), interfaces, index);
            }
            case "reflection": {
                var metadataGen = new ReflectionMetadataGenerator(context.names(), context.module(),
                        context.functionTypes(), context.dependency(), reflection, context.classes(),
                        context.classInfoProvider(), context.functions(), context.typeMapper(),
                        context.strings(), context.classInitInfo(), context.virtualTables());
                metadataGen.generate();
                context.addToInitializer(fn -> fn.getBody().add(new WasmCall(metadataGen.initFunction())));
                return fieldAccess(invocation, context, ClassInfoStruct::reflectionInfoIndex);
            }
            case "rewind": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                return new WasmSetGlobal(classInfoType.currentClassGlobal(), new WasmGetGlobal(
                        classInfoType.firstClassGlobal()));
            }
            case "hasNext": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var current = new WasmGetGlobal(classInfoType.currentClassGlobal());
                return new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, new WasmIsNull(current));
            }
            case "next": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var block = new WasmBlock(false);
                block.setType(classInfoType.structure().getReference().asBlock());
                var currentCache = context.tempVars().acquire(classInfoType.structure().getReference());
                block.getBody().add(new WasmSetLocal(currentCache, new WasmGetGlobal(
                        classInfoType.currentClassGlobal())));
                var next = new WasmStructGet(classInfoType.structure(), new WasmGetLocal(currentCache),
                        classInfoType.nextClassIndex());
                block.getBody().add(new WasmSetGlobal(classInfoType.currentClassGlobal(), next));
                block.getBody().add(new WasmGetLocal(currentCache));
                context.tempVars().release(currentCache);
                return block;
            }
            case "newInstance": {
                var fn = fieldAccess(invocation, context, ClassInfoStruct::createInstanceIndex);
                var objType = context.classInfoProvider().getClassInfo("java.lang.Object");
                return new WasmCallReference(fn, context.functionTypes().of(objType.getType()));
            }
            case "initializeNewInstance": {
                var objType = context.classInfoProvider().getClassInfo("java.lang.Object");
                var block = new WasmBlock(false);
                block.setType(WasmType.INT32.asBlock());

                var innerBlock = new WasmBlock(false);
                block.getBody().add(innerBlock);
                var fnType = context.functionTypes().of(null, objType.getType());
                var fn = fieldAccess(invocation, context, ClassInfoStruct::initNewInstanceIndex);
                var br = new WasmNullBranch(WasmNullCondition.NULL, fn, innerBlock);
                var instance = context.generate(invocation.getArguments().get(1));
                var call = new WasmCallReference(br, fnType, instance);
                if (context.isAsyncMethod(invocation.getMethod())) {
                    call.setSuspensionPoint(true);
                }
                innerBlock.getBody().add(call);
                var brTrue = new WasmBreak(block);
                brTrue.setResult(new WasmInt32Constant(1));
                innerBlock.getBody().add(brTrue);

                block.getBody().add(new WasmInt32Constant(0));
                return block;
            }
            case "arrayType": {
                var fn = context.classInfoProvider().getGetArrayClassFunction();
                return new WasmCall(fn, context.generate(invocation.getArguments().get(0)));
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private WasmExpression fieldAccess(InvocationExpr invocation, WasmGCIntrinsicContext context,
            ToIntFunction<ClassInfoStruct> field) {
        var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
        var receiver = context.generate(invocation.getArguments().get(0));
        return new WasmStructGet(classInfoType.structure(), receiver, field.applyAsInt(classInfoType));
    }

    private WasmExpression callVirtual(WasmGCIntrinsicContext context, WasmFunctionType fnType, int index,
            WasmExpression... args) {
        var block = new WasmBlock(false);
        var ret = fnType.getSingleReturnType();
        if (ret != null) {
            block.setType(ret.asBlock());
        }
        var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
        var receiver = context.exprCache().create(args[0], classInfoType.structure().getReference(), null,
                block.getBody());
        args[0] = receiver.expr();
        var fnRef = new WasmStructGet(classInfoType.structure(), args[0], index);
        block.getBody().add(new WasmCallReference(fnRef, fnType, args));
        receiver.release();
        return block;
    }
}
