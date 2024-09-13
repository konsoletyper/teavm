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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmReferencesEqual;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;

public class WasmGCSupertypeFunctionGenerator implements WasmGCSupertypeFunctionProvider {
    private Map<ValueType, WasmFunction> functions = new HashMap<>();
    private WasmModule module;
    private WasmGCClassGenerator classGenerator;
    private WasmGCNameProvider nameProvider;
    private TagRegistry tagRegistry;
    private WasmFunctionTypes functionTypes;
    private WasmFunctionType functionType;

    WasmGCSupertypeFunctionGenerator(
            WasmModule module,
            WasmGCClassGenerator classGenerator,
            WasmGCNameProvider nameProvider,
            TagRegistry tagRegistry,
            WasmFunctionTypes functionTypes
    ) {
        this.module = module;
        this.classGenerator = classGenerator;
        this.nameProvider = nameProvider;
        this.tagRegistry = tagRegistry;
        this.functionTypes = functionTypes;
    }

    @Override
    public WasmFunction getIsSupertypeFunction(ValueType type) {
        var result = functions.get(type);
        if (result == null) {
            result = generateIsSupertypeFunction(type);
            functions.put(type, result);
        }
        return result;
    }

    private WasmFunction generateIsSupertypeFunction(ValueType type) {
        var function = new WasmFunction(getFunctionType());
        function.setName(nameProvider.topLevel(nameProvider.suggestForType(type) + "@isSupertypes"));
        var subtypeVar = new WasmLocal(classGenerator.standardClasses.classClass().getType(), "subtype");
        function.add(subtypeVar);
        module.functions.add(function);

        if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            generateIsClass(subtypeVar, className, function);
        } else if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            generateIsArray(subtypeVar, itemType, function.getBody());
        } else {
            var expected = classGenerator.getClassInfo(type).pointer;
            var condition = new WasmReferencesEqual(new WasmGetLocal(subtypeVar), new WasmGetGlobal(expected));
            function.getBody().add(new WasmReturn(condition));
        }

        return function;
    }

    private void generateIsClass(WasmLocal subtypeVar, String className, WasmFunction function) {
        var body = function.getBody();
        var ranges = tagRegistry.getRanges(className);
        if (ranges.isEmpty()) {
            body.add(new WasmReturn(new WasmInt32Constant(0)));
            return;
        }

        int tagOffset = classGenerator.getClassTagOffset();

        var tagVar = new WasmLocal(WasmType.INT32, "tag");
        function.add(tagVar);
        var tagExpression = getClassField(new WasmGetLocal(subtypeVar), tagOffset);
        body.add(new WasmSetLocal(tagVar, tagExpression));

        ranges.sort(Comparator.comparingInt(range -> range.lower));

        int lower = ranges.get(0).lower;
        int upper = ranges.get(ranges.size() - 1).upper;

        var lowerCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                new WasmGetLocal(tagVar), new WasmInt32Constant(lower));
        var testLower = new WasmConditional(lowerCondition);
        testLower.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));
        body.add(testLower);

        var upperCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GE_SIGNED,
                new WasmGetLocal(tagVar), new WasmInt32Constant(upper));
        var testUpper = new WasmConditional(upperCondition);
        testUpper.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));
        body.add(testUpper);

        for (int i = 1; i < ranges.size(); ++i) {
            int lowerHole = ranges.get(i - 1).upper;
            int upperHole = ranges.get(i).lower;

            lowerCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GE_SIGNED,
                    new WasmGetLocal(tagVar), new WasmInt32Constant(lowerHole));
            testLower = new WasmConditional(lowerCondition);
            body.add(testLower);

            upperCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                    new WasmGetLocal(tagVar), new WasmInt32Constant(upperHole));
            testUpper = new WasmConditional(upperCondition);
            testUpper.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));

            testLower.getThenBlock().getBody().add(testUpper);
        }

        body.add(new WasmReturn(new WasmInt32Constant(1)));
    }

    private void generateIsArray(WasmLocal subtypeVar, ValueType itemType, List<WasmExpression> body) {
        int itemOffset = classGenerator.getClassArrayItemOffset();

        var itemExpression = getClassField(new WasmGetLocal(subtypeVar), itemOffset);
        body.add(new WasmSetLocal(subtypeVar, itemExpression));

        var itemTest = new WasmConditional(new WasmIsNull(new WasmGetLocal(subtypeVar)));
        itemTest.setType(WasmType.INT32);
        itemTest.getThenBlock().getBody().add(new WasmInt32Constant(0));

        var delegateToItem = new WasmCall(getIsSupertypeFunction(itemType));
        delegateToItem.getArguments().add(new WasmGetLocal(subtypeVar));
        itemTest.getElseBlock().getBody().add(delegateToItem);

        body.add(new WasmReturn(itemTest));
    }

    public WasmFunctionType getFunctionType() {
        if (functionType == null) {
            functionType = functionTypes.of(WasmType.INT32, classGenerator.standardClasses.classClass().getType());
        }
        return functionType;
    }


    private WasmExpression getClassField(WasmExpression instance, int fieldIndex) {
        return new WasmStructGet(
                classGenerator.standardClasses.classClass().getStructure(),
                instance,
                fieldIndex
        );
    }
}
