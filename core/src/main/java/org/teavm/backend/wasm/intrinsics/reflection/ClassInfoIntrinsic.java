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

import java.util.List;
import java.util.function.ToIntFunction;
import org.teavm.ast.Expr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.CachedValue;
import org.teavm.backend.wasm.generate.reflection.ClassInfoStruct;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.reflection.ReflectionDependencyListener;

public class ClassInfoIntrinsic implements WasmGCIntrinsic {
    private ReflectionDependencyListener reflection;

    public ClassInfoIntrinsic(ReflectionDependencyListener reflection) {
        this.reflection = reflection;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "modifiers":
                fieldAccess(invocation, context, builder, ClassInfoStruct::modifiersIndex);
                break;
            case "primitiveKind":
                fieldAccess(invocation, context, builder, ClassInfoStruct::primitiveKindIndex);
                break;
            case "parent":
                fieldAccess(invocation, context, builder, ClassInfoStruct::parentIndex);
                break;
            case "name":
                fieldAccess(invocation, context, builder, ClassInfoStruct::nameIndex);
                break;
            case "simpleName":
                fieldAccess(invocation, context, builder, ClassInfoStruct::simpleNameIndex);
                break;
            case "declaringClass":
                fieldAccess(invocation, context, builder, ClassInfoStruct::declaringClassIndex);
                break;
            case "enclosingClass":
                fieldAccess(invocation, context, builder, ClassInfoStruct::enclosingClassIndex);
                break;
            case "itemType":
                fieldAccess(invocation, context, builder, ClassInfoStruct::itemTypeIndex);
                break;
            case "classObject": {
                context.generate(builder, invocation.getArguments().get(0));
                builder.call(context.classInfoProvider().reflectionTypes().classInfo().classObjectFunction());
                break;
            }
            case "isSuperTypeOf": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var args = List.of(invocation.getArguments().get(1), invocation.getArguments().get(0));
                callVirtual(context, builder, classInfoType.supertypeFunctionType(),
                        classInfoType.supertypeFunctionIndex(), args, 1);
                break;
            }
            case "newArrayInstance": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var args = List.of(invocation.getArguments().get(0), invocation.getArguments().get(1));
                callVirtual(context, builder, classInfoType.newArrayFunctionType(),
                        classInfoType.newArrayFunctionIndex(), args, 0);
                break;
            }
            case "arrayLength": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var args = List.of(invocation.getArguments().get(0), invocation.getArguments().get(1));
                callVirtual(context, builder, classInfoType.arrayLengthFunctionType(),
                        classInfoType.arrayLengthIndex(), args, 0);
                break;
            }
            case "getItem": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var args = List.of(invocation.getArguments().get(0), invocation.getArguments().get(1),
                        invocation.getArguments().get(2));
                callVirtual(context, builder, classInfoType.getItemFunctionType(),
                        classInfoType.getItemIndex(), args, 0);
                break;
            }
            case "putItem": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var args = List.of(invocation.getArguments().get(0), invocation.getArguments().get(1),
                        invocation.getArguments().get(2), invocation.getArguments().get(3));
                callVirtual(context, builder, classInfoType.putItemFunctionType(),
                        classInfoType.putItemIndex(), args, 0);
                break;
            }
            case "initialize": {
                var block = builder.block();
                fieldAccess(invocation, context, block, ClassInfoStruct::initializerIndex);
                var isSuspend = context.isAsyncMethod(invocation.getMethod());
                block
                        .nullBranch(WasmNullCondition.NULL, block)
                        .callReference(context.functionTypes().of(null), isSuspend);
                break;
            }
            case "enumConstantCount": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                context.generate(builder, invocation.getArguments().get(0));
                var cachedReceiver = context.valueCache().create(
                        classInfoType.structure().getReference(), builder);
                builder.drop();

                var outerBlock = builder.block(WasmType.INT32);
                var initBlock = outerBlock.block(classInfoType.enumConstantsType().getReference());
                var fnType = context.functionTypes().of(classInfoType.enumConstantsType().getReference());

                initBlock
                        .append(cachedReceiver)
                        .structGet(classInfoType.structure(), classInfoType.enumConstantsIndex())
                        .nullBranch(WasmNullCondition.NOT_NULL, initBlock)
                        .append(cachedReceiver)
                        .append(cachedReceiver)
                        .structGet(classInfoType.structure(), classInfoType.initEnumConstantsIndex())
                        .callReference(fnType)
                        .structSet(classInfoType.structure(), classInfoType.enumConstantsIndex());

                initBlock
                        .append(cachedReceiver)
                        .structGet(classInfoType.structure(), classInfoType.enumConstantsIndex());

                outerBlock.arrayLength();
                cachedReceiver.release();
                break;
            }
            case "enumConstant": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                fieldAccess(invocation, context, builder, ClassInfoStruct::enumConstantsIndex);
                context.generate(builder, invocation.getArguments().get(1));
                builder.arrayGet(classInfoType.enumConstantsType());
                break;
            }
            case "superinterfaceCount": {
                var outerBlock = builder.block(WasmType.INT32);
                var innerBlock = outerBlock.block();

                fieldAccess(invocation, context, innerBlock, ClassInfoStruct::interfacesIndex);
                innerBlock
                        .nullBranch(WasmNullCondition.NULL, innerBlock)
                        .arrayLength()
                        .breakTo(outerBlock);

                outerBlock.i32Const(0);
                break;
            }
            case "superinterface": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                fieldAccess(invocation, context, builder, ClassInfoStruct::interfacesIndex);
                context.generate(builder, invocation.getArguments().get(1));
                builder.arrayGet(classInfoType.interfacesType());
                break;
            }
            case "reflection": {
                var metadataGen = new ReflectionMetadataGenerator(context.names(), context.module(),
                        context.functionTypes(), context.dependency(), reflection, context.classes(),
                        context.classInfoProvider(), context.functions(), context.typeMapper(),
                        context.strings(), context.classInitInfo(), context.virtualTables());
                metadataGen.generate();
                context.addToInitializer(fn -> {
                    fn.getBody().builder().call(metadataGen.initFunction());
                });
                fieldAccess(invocation, context, builder, ClassInfoStruct::reflectionInfoIndex);
                break;
            }
            case "rewind": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                builder
                        .getGlobal(classInfoType.firstClassGlobal())
                        .setGlobal(classInfoType.currentClassGlobal());
                break;
            }
            case "hasNext": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                builder
                        .getGlobal(classInfoType.currentClassGlobal())
                        .isNull()
                        .negate();
                break;
            }
            case "next": {
                var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
                var currentCache = context.tempVars().acquire(classInfoType.structure().getReference());
                builder
                        .getGlobal(classInfoType.currentClassGlobal())
                        .teeLocal(currentCache)
                        .structGet(classInfoType.structure(), classInfoType.nextClassIndex())
                        .setGlobal(classInfoType.currentClassGlobal())
                        .getLocal(currentCache);
                context.tempVars().release(currentCache);
                break;
            }
            case "newInstance": {
                fieldAccess(invocation, context, builder, ClassInfoStruct::createInstanceIndex);
                var objType = context.classInfoProvider().getClassInfo("java.lang.Object");
                builder.callReference(context.functionTypes().of(objType.getType()));
                break;
            }
            case "initializeNewInstance": {
                var objType = context.classInfoProvider().getClassInfo("java.lang.Object");
                var isSuspend = context.isAsyncMethod(invocation.getMethod());

                var outerBlock = builder.block(WasmType.INT32);
                var innerBlock = outerBlock.block();

                context.generate(innerBlock, invocation.getArguments().get(1));
                fieldAccess(invocation, context, innerBlock, ClassInfoStruct::initNewInstanceIndex);
                innerBlock
                        .nullBranch(WasmNullCondition.NULL, innerBlock)
                        .callReference(context.functionTypes().of(null, objType.getType()), isSuspend)
                        .i32Const(1)
                        .breakTo(outerBlock);

                outerBlock.i32Const(0);
                break;
            }
            case "arrayType": {
                context.generate(builder, invocation.getArguments().get(0));
                builder.call(context.classInfoProvider().getGetArrayClassFunction());
                break;
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private void fieldAccess(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder, ToIntFunction<ClassInfoStruct> field) {
        var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
        context.generate(builder, invocation.getArguments().get(0));
        builder.structGet(classInfoType.structure(), field.applyAsInt(classInfoType));
    }

    private void callVirtual(WasmGCIntrinsicContext context, WasmInstructionBuilder builder,
            WasmFunctionType fnType, int index, List<Expr> argExprs, int receiverArgIndex) {
        var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
        CachedValue cachedReceiver = null;
        for (int i = 0; i < argExprs.size(); i++) {
            context.generate(builder, argExprs.get(i));
            if (i == receiverArgIndex) {
                cachedReceiver = context.valueCache().create(
                        classInfoType.structure().getReference(), builder);
            }
        }
        builder
                .append(cachedReceiver)
                .structGet(classInfoType.structure(), index)
                .callReference(fnType);
        cachedReceiver.release();
    }
}
