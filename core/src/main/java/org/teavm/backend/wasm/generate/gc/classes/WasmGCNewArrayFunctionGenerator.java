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
package org.teavm.backend.wasm.generate.gc.classes;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.model.ValueType;

class WasmGCNewArrayFunctionGenerator {
    private WasmModule module;
    private WasmFunctionTypes functionTypes;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmFunctionType newArrayFunctionType;
    private WasmGCNameProvider names;
    private Queue<Runnable> queue;

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
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));
        var functionType = new WasmFunctionType(null, classInfo.getType(), List.of(WasmType.INT32));
        module.types.add(functionType);
        functionType.setFinal(true);
        functionType.getSupertypes().add(getNewArrayFunctionType());
        var function = new WasmFunction(functionType);
        function.setName(names.topLevel("Array<" + names.suggestForType(itemType) + ">@new"));
        module.functions.add(function);

        queue.add(() -> {
            var sizeLocal = new WasmLocal(WasmType.INT32, "length");
            function.add(sizeLocal);
            var tempVars = new TemporaryVariablePool(function);
            var genUtil = new WasmGCGenerationUtil(classInfoProvider, tempVars);
            var targetVar = new WasmLocal(classInfo.getType(), "result");
            function.add(targetVar);
            genUtil.allocateArray(itemType, () -> new WasmGetLocal(sizeLocal), null, targetVar, function.getBody());
            function.getBody().add(new WasmGetLocal(targetVar));
        });
        return function;
    }

    WasmFunction generateNewMultiArrayFunction(ValueType itemType, int depth) {
        var arrayType = itemType;
        for (var i = 0; i < depth; ++i) {
            arrayType = ValueType.arrayOf(arrayType);
        }
        var classInfo = classInfoProvider.getClassInfo(arrayType);

        var parameterTypes = new WasmType[depth];
        Arrays.fill(parameterTypes, WasmType.INT32);
        var functionType = functionTypes.of(classInfo.getType(), parameterTypes);

        var function = new WasmFunction(functionType);
        function.setName(names.topLevel(names.suggestForType(arrayType) + "@new:" + depth));
        module.functions.add(function);

        var finalArrayType = arrayType;
        queue.add(() -> {
            var dimensionLocals = new WasmLocal[depth];
            for (var i = 0; i < depth; ++i) {
                var dimensionLocal = new WasmLocal(WasmType.INT32, "dim" + i);
                dimensionLocals[i] = dimensionLocal;
                function.add(dimensionLocal);
            }
            var indexLocal = new WasmLocal(WasmType.INT32, "index");
            function.add(indexLocal);

            var dataField = classInfo.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
            var dataFieldTypeRef = (WasmType.CompositeReference) dataField.getUnpackedType();
            var dataArray = (WasmArray) dataFieldTypeRef.composite;
            var dataLocal = new WasmLocal(dataArray.getReference(), "data");
            function.add(dataLocal);

            var resultVar = new WasmLocal(classInfo.getType(), "result");
            function.add(resultVar);

            var arrayItemType = ((ValueType.Array) finalArrayType).getItemType();
            var allocFunction = classInfoProvider.getArrayConstructor(arrayItemType, 1);
            function.getBody().add(new WasmSetLocal(resultVar, new WasmCall(allocFunction,
                    new WasmGetLocal(dimensionLocals[0]))));
            function.getBody().add(new WasmSetLocal(dataLocal, new WasmStructGet(classInfo.getStructure(),
                    new WasmGetLocal(resultVar), WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)));

            var zeroGuard = new WasmBlock(false);
            function.getBody().add(zeroGuard);
            zeroGuard.getBody().add(new WasmBranch(new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ,
                    new WasmGetLocal(dimensionLocals[0])), zeroGuard));

            var loop = new WasmBlock(true);
            zeroGuard.getBody().add(loop);
            var itemFunction = classInfoProvider.getArrayConstructor(itemType, depth - 1);
            var args = new WasmExpression[depth - 1];
            for (var i = 0; i < args.length; ++i) {
                args[i] = new WasmGetLocal(dimensionLocals[i + 1]);
            }
            loop.getBody().add(new WasmArraySet(dataArray, new WasmGetLocal(dataLocal), new WasmGetLocal(indexLocal),
                    new WasmCall(itemFunction, args)));
            var incrementIndex = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                    new WasmGetLocal(indexLocal), new WasmInt32Constant(1));
            loop.getBody().add(new WasmSetLocal(indexLocal, incrementIndex));
            var continueCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_UNSIGNED,
                    new WasmGetLocal(indexLocal), new WasmGetLocal(dimensionLocals[0]));
            loop.getBody().add(new WasmBranch(continueCondition, loop));

            function.getBody().add(new WasmGetLocal(resultVar));
        });
        return function;
    }

    WasmFunctionType getNewArrayFunctionType() {
        if (newArrayFunctionType == null) {
            newArrayFunctionType = functionTypes.of(classInfoProvider.getClassInfo("java.lang.Object").getType(),
                    WasmType.INT32);
            newArrayFunctionType.setFinal(false);
        }
        return newArrayFunctionType;
    }
}
