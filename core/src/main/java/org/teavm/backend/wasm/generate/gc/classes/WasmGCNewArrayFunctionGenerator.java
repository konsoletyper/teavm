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
import java.util.function.Function;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.model.ValueType;

class WasmGCNewArrayFunctionGenerator {
    private WasmModule module;
    private WasmFunctionTypes functionTypes;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmFunctionType newArrayFunctionType;
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
            var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));
            var classClass = classInfoProvider.getClassInfo("java.lang.Class").getType();
            var functionType = new WasmFunctionType(null, classInfo.getType(), List.of(classClass, WasmType.INT32));
            module.types.add(functionType);
            functionType.setFinal(true);
            functionType.getSupertypes().add(getNewArrayFunctionType());
            var function = new WasmFunction(functionType);
            function.setName(names.topLevel("Array<" + names.suggestForType(itemType) + ">@new"));
            module.functions.add(function);

            queue.add(() -> {
                var clsLocal = new WasmLocal(classClass, "this");
                var sizeLocal = new WasmLocal(WasmType.INT32, "length");
                function.add(clsLocal);
                function.add(sizeLocal);
                var targetVar = new WasmLocal(classInfo.getType(), "result");
                function.add(targetVar);
                function.getBody().add(allocateArray(itemType, sizeLocal));
            });
            return function;
        } else {
            return getNewObjectArrayFunction();
        }
    }

    private WasmExpression allocateArray(ValueType itemType, WasmLocal sizeLocal) {
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));

        var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                .getUnpackedType();
        var wasmArray = (WasmArray) wasmArrayType.composite;

        var structNew = new WasmStructNew(classInfo.getStructure());
        structNew.getInitializers().add(new WasmGetGlobal(classInfo.getVirtualTablePointer()));
        structNew.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));
        structNew.getInitializers().add(new WasmArrayNewDefault(wasmArray, new WasmGetLocal(sizeLocal)));
        return structNew;
    }

    WasmFunction getNewObjectArrayFunction() {
        if (newObjectArrayFunction == null) {
            newObjectArrayFunction = generateNewObjectArrayFunction();
        }
        return newObjectArrayFunction;
    }

    private WasmFunction generateNewObjectArrayFunction() {
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(ValueType.object("java.lang.Object")));
        var classClass = classInfoProvider.getClassInfo("java.lang.Class");
        var functionType = new WasmFunctionType(null, classInfo.getType(), List.of(classClass.getType(),
                WasmType.INT32));
        module.types.add(functionType);
        functionType.setFinal(true);
        functionType.getSupertypes().add(getNewArrayFunctionType());
        var function = new WasmFunction(functionType);
        function.setName(names.topLevel("Array<" + names.suggestForClass("java.lang.Object") + ">@new"));
        module.functions.add(function);

        queue.add(() -> {
            var clsLocal = new WasmLocal(classClass.getType(), "this");
            var sizeLocal = new WasmLocal(WasmType.INT32, "length");
            function.add(clsLocal);
            function.add(sizeLocal);
            var targetVar = new WasmLocal(classInfo.getType(), "result");
            function.add(targetVar);

            var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                    .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                    .getUnpackedType();
            var wasmArray = (WasmArray) wasmArrayType.composite;

            var arrayCls = new WasmCall(classInfoProvider.getGetArrayClassFunction(), new WasmGetLocal(clsLocal));
            var arrayVt = new WasmStructGet(classClass.getStructure(), arrayCls,
                    classInfoProvider.getClassVtFieldOffset());

            var structNew = new WasmStructNew(classInfo.getStructure());
            structNew.getInitializers().add(arrayVt);
            structNew.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));
            structNew.getInitializers().add(new WasmArrayNewDefault(wasmArray, new WasmGetLocal(sizeLocal)));
            function.getBody().add(structNew);
        });
        return function;
    }

    WasmFunction generateNewMultiArrayFunction(int depth) {
        var arrayType = ValueType.arrayOf(ValueType.object("java.lang.Object"));
        var arrayClass = classInfoProvider.getClassInfo(arrayType);
        var objectClass = classInfoProvider.getClassInfo("java.lang.Object");
        var classClass = classInfoProvider.getClassInfo("java.lang.Class");

        var parameterTypes = new WasmType[depth + 1];
        Arrays.fill(parameterTypes, WasmType.INT32);
        parameterTypes[0] = classClass.getType();
        var functionType = functionTypes.of(arrayClass.getType(), parameterTypes);

        var function = new WasmFunction(functionType);
        function.setName(names.topLevel(names.suggestForType(arrayType) + "@new:" + depth));
        module.functions.add(function);

        queue.add(() -> {
            var itemTypeLocal = new WasmLocal(classClass.getType(), "itemType");
            function.add(itemTypeLocal);
            var dimensionLocals = new WasmLocal[depth];
            for (var i = 0; i < depth; ++i) {
                var dimensionLocal = new WasmLocal(WasmType.INT32, "dim" + i);
                dimensionLocals[i] = dimensionLocal;
                function.add(dimensionLocal);
            }
            var indexLocal = new WasmLocal(WasmType.INT32, "index");
            var nextItemTypeLocal = new WasmLocal(classClass.getType(), "nextItemType");
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
            function.getBody().add(new WasmSetLocal(resultVar, new WasmCall(allocFunction,
                    new WasmGetLocal(itemTypeLocal), new WasmGetLocal(dimensionLocals[0]))));
            function.getBody().add(new WasmSetLocal(dataLocal, new WasmStructGet(arrayClass.getStructure(),
                    new WasmGetLocal(resultVar), WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)));
            function.getBody().add(new WasmSetLocal(nextItemTypeLocal, new WasmStructGet(classClass.getStructure(),
                    new WasmGetLocal(itemTypeLocal), classInfoProvider.getClassArrayItemOffset())));

            var zeroGuard = new WasmBlock(false);
            function.getBody().add(zeroGuard);
            zeroGuard.getBody().add(new WasmBranch(new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ,
                    new WasmGetLocal(dimensionLocals[0])), zeroGuard));

            Function<WasmExpression[], WasmExpression> nextArrayConstructor;
            if (depth == 2) {
                var nextFunctionType = functionTypes.of(objectClass.getType(),
                        classClass.getType(), WasmType.INT32);
                var createNextArrayLocal = new WasmLocal(nextFunctionType.getReference(), "createNextArray");
                function.add(createNextArrayLocal);
                zeroGuard.getBody().add(new WasmSetLocal(createNextArrayLocal, new WasmStructGet(
                        classClass.getStructure(), new WasmGetLocal(nextItemTypeLocal),
                        classInfoProvider.getNewArrayFunctionOffset())));
                nextArrayConstructor = args -> new WasmCallReference(new WasmGetLocal(createNextArrayLocal),
                        nextFunctionType, args);
            } else {
                var nextFunction = classInfoProvider.getMultiArrayConstructor(depth - 1);
                nextArrayConstructor = args -> new WasmCall(nextFunction, args);
            }
            var loop = new WasmBlock(true);
            zeroGuard.getBody().add(loop);
            var args = new WasmExpression[depth];
            args[0] = new WasmGetLocal(nextItemTypeLocal);
            for (var i = 1; i < args.length; ++i) {
                args[i] = new WasmGetLocal(dimensionLocals[i]);
            }
            loop.getBody().add(new WasmArraySet(dataArray, new WasmGetLocal(dataLocal), new WasmGetLocal(indexLocal),
                    nextArrayConstructor.apply(args)));
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
            newArrayFunctionType = functionTypes.of(
                    classInfoProvider.getClassInfo("java.lang.Object").getType(),
                    classInfoProvider.getClassInfo("java.lang.Class").getType(),
                    WasmType.INT32
            );
            newArrayFunctionType.setFinal(false);
        }
        return newArrayFunctionType;
    }
}
