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
package org.teavm.backend.wasm.generate.classes;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.model.ValueType;

class WasmGCNewArrayFunctionGenerator {
    private WasmModule module;
    private WasmFunctionTypes functionTypes;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmGCNameProvider names;
    private Queue<Runnable> queue;
    private WasmFunction newObjectArrayFunction;

    WasmGCNewArrayFunctionGenerator(WasmModule module, WasmFunctionTypes functionTypes,
            WasmGCClassInfoProvider classInfoProvider, WasmGCNameProvider names,
            Queue<Runnable> queue) {
        this.module = module;
        this.functionTypes = functionTypes;
        this.classInfoProvider = classInfoProvider;
        this.names = names;
        this.queue = queue;
    }

    WasmFunction generateNewArrayFunction(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            var classInfoType = classInfoProvider.reflectionTypes().classInfo();
            var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));
            var functionType = new WasmFunctionType(null, classInfo.getType(),
                    List.of(classInfoType.structure().getReference(), WasmType.INT32));
            module.types.add(functionType);
            functionType.setFinal(true);
            functionType.getSupertypes().add(classInfoType.newArrayFunctionType());
            var function = new WasmFunction(functionType);
            function.setName(names.topLevel("Array<" + names.suggestForType(itemType) + ">@new"));
            module.functions.add(function);

            queue.add(() -> {
                var clsLocal = new WasmLocal(classInfoType.structure().getReference(), "this");
                var sizeLocal = new WasmLocal(WasmType.INT32, "length");
                function.add(clsLocal);
                function.add(sizeLocal);
                var targetVar = new WasmLocal(classInfo.getType(), "result");
                function.add(targetVar);

                var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                        .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                        .getUnpackedType();
                var wasmArray = (WasmArray) wasmArrayType.composite;

                function.getBody().builder()
                        .getGlobal(classInfo.getVirtualTablePointer())
                        .nullConst(WasmType.EQ)
                        .getLocal(sizeLocal)
                        .arrayNewDefault(wasmArray)
                        .structNew(classInfo.getStructure());
            });
            return function;
        } else {
            return getNewObjectArrayFunction();
        }
    }

    WasmFunction getNewObjectArrayFunction() {
        if (newObjectArrayFunction == null) {
            newObjectArrayFunction = generateNewObjectArrayFunction();
        }
        return newObjectArrayFunction;
    }

    private WasmFunction generateNewObjectArrayFunction() {
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Object")));
        var classInfoType = classInfoProvider.reflectionTypes().classInfo();

        var functionType = new WasmFunctionType(null, classInfo.getType(),
                List.of(classInfoType.structure().getReference(), WasmType.INT32));
        module.types.add(functionType);
        functionType.setFinal(true);
        functionType.getSupertypes().add(classInfoType.newArrayFunctionType());

        var function = new WasmFunction(functionType);
        function.setName(names.topLevel("Array<" + names.suggestForClass("java.lang.Object") + ">@new"));
        module.functions.add(function);

        queue.add(() -> {
            var clsLocal = new WasmLocal(classInfoType.structure().getReference(), "this");
            var sizeLocal = new WasmLocal(WasmType.INT32, "length");
            function.add(clsLocal);
            function.add(sizeLocal);
            var targetVar = new WasmLocal(classInfo.getType(), "result");
            function.add(targetVar);

            var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                    .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                    .getUnpackedType();
            var wasmArray = (WasmArray) wasmArrayType.composite;

            function.getBody().builder()
                    .getLocal(clsLocal)
                    .call(classInfoProvider.getGetArrayClassFunction())
                    .structGet(classInfoType.structure(), classInfoType.vtableIndex())
                    .nullConst(WasmType.EQ)
                    .getLocal(sizeLocal)
                    .arrayNewDefault(wasmArray)
                    .structNew(classInfo.getStructure());
        });
        return function;
    }

    WasmFunction generateNewMultiArrayFunction(int depth) {
        var arrayType = ValueType.arrayOf(ValueType.object("java.lang.Object"));
        var arrayClass = classInfoProvider.getClassInfo(arrayType);
        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
        var classInfoType = classInfoProvider.reflectionTypes().classInfo();

        var parameterTypes = new WasmType[depth + 1];
        Arrays.fill(parameterTypes, WasmType.INT32);
        parameterTypes[0] = classInfoType.structure().getReference();
        var functionType = functionTypes.of(arrayClass.getType(), parameterTypes);

        var function = new WasmFunction(functionType);
        function.setName(names.topLevel(names.suggestForType(arrayType) + "@new:" + depth));
        module.functions.add(function);

        queue.add(() -> {
            var itemTypeLocal = new WasmLocal(classInfoType.structure().getReference(), "itemType");
            function.add(itemTypeLocal);
            var dimensionLocals = new WasmLocal[depth];
            for (var i = 0; i < depth; ++i) {
                var dimensionLocal = new WasmLocal(WasmType.INT32, "dim" + i);
                dimensionLocals[i] = dimensionLocal;
                function.add(dimensionLocal);
            }
            var indexLocal = new WasmLocal(WasmType.INT32, "index");
            var nextItemTypeLocal = new WasmLocal(classInfoType.structure().getReference(), "nextItemType");
            function.add(indexLocal);
            function.add(nextItemTypeLocal);

            var dataField = arrayClass.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
            var dataFieldTypeRef = (WasmType.CompositeReference) dataField.getUnpackedType();
            var dataArray = (WasmArray) dataFieldTypeRef.composite;
            var dataLocal = new WasmLocal(dataArray.getReference(), "data");
            function.add(dataLocal);

            var resultVar = new WasmLocal(arrayClass.getType(), "result");
            function.add(resultVar);

            var allocFunction = getNewObjectArrayFunction();
            var body = function.getBody().builder();
            body
                    .getLocal(itemTypeLocal)
                    .getLocal(dimensionLocals[0])
                    .call(allocFunction)
                    .teeLocal(resultVar);
            body
                    .structGet(arrayClass.getStructure(), WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                    .setLocal(dataLocal);
            body.getLocal(itemTypeLocal)
                    .structGet(classInfoType.structure(), classInfoType.itemTypeIndex())
                    .setLocal(nextItemTypeLocal);

            var guardBody = body.block();
            guardBody.getLocal(dimensionLocals[0])
                    .intUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ)
                    .branch(guardBody);

            WasmLocal createNextArrayLocal = null;
            WasmFunctionType nextFunctionType = null;
            WasmFunction nextFunction = null;
            if (depth == 2) {
                nextFunctionType = functionTypes.of(objectClass.getType(),
                        classInfoType.structure().getReference(), WasmType.INT32);
                createNextArrayLocal = new WasmLocal(nextFunctionType.getReference(), "createNextArray");
                function.add(createNextArrayLocal);
                guardBody.getLocal(nextItemTypeLocal)
                        .structGet(classInfoType.structure(), classInfoType.newArrayFunctionIndex())
                        .setLocal(createNextArrayLocal);
            } else {
                nextFunction = classInfoProvider.getMultiArrayConstructor(depth - 1);
            }

            var loopBody = guardBody.loop();
            loopBody.getLocal(dataLocal).getLocal(indexLocal);
            loopBody.getLocal(nextItemTypeLocal);
            for (var i = 1; i < depth; ++i) {
                loopBody.getLocal(dimensionLocals[i]);
            }
            if (depth == 2) {
                loopBody.getLocal(createNextArrayLocal).callReference(nextFunctionType);
            } else {
                loopBody.call(nextFunction);
            }
            loopBody.arraySet(dataArray);
            loopBody.getLocal(indexLocal).i32Const(1)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD)
                    .teeLocal(indexLocal);
            loopBody.getLocal(dimensionLocals[0])
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_UNSIGNED)
                    .branch(loopBody);

            body.getLocal(resultVar);
        });
        return function;
    }
}
