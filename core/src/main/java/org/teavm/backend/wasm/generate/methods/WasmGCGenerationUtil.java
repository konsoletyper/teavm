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
package org.teavm.backend.wasm.generate.methods;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.model.ValueType;

public class WasmGCGenerationUtil {
    private WasmGCClassInfoProvider classInfoProvider;

    public WasmGCGenerationUtil(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    public WasmExpression allocateArrayWithElements(ValueType itemType,
            Supplier<List<? extends WasmExpression>> data) {
        return allocateArray(itemType, arrayType -> {
            var expr = new WasmArrayNewFixed(arrayType);
            expr.getElements().addAll(data.get());
            return expr;
        });
    }

    public WasmExpression allocateArray(ValueType itemType, Function<WasmArray, WasmExpression> data) {
        var classInfoType = classInfoProvider.reflectionTypes().classInfo();
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));

        var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                .getUnpackedType();
        var wasmArray = (WasmArray) wasmArrayType.composite;

        var structNew = new WasmStructNew(classInfo.getStructure());
        int depth = 1;
        while (itemType instanceof ValueType.Array) {
            itemType = ((ValueType.Array) itemType).getItemType();
        }
        WasmExpression arrayClassRef = new WasmGetGlobal(classInfoProvider.getClassInfo(itemType).getPointer());
        for (var i = 0; i < depth; ++i) {
            arrayClassRef = new WasmCall(classInfoProvider.getGetArrayClassFunction(), arrayClassRef);
        }
        var arrayVt = new WasmStructGet(classInfoType.structure(), arrayClassRef, classInfoType.vtableIndex());
        structNew.getInitializers().add(arrayVt);
        structNew.getInitializers().add(new WasmNullConstant(WasmType.EQ));
        structNew.getInitializers().add(data.apply(wasmArray));
        return structNew;
    }

    public static WasmExpression getOrIfNull(WasmType type, WasmExpression base,
            Function<WasmExpression, WasmExpression> extractor, WasmExpression defaultValue) {
        var block = new WasmBlock(false);
        block.setType(type != null ? type.asBlock() : null);

        var innerBlock = new WasmBlock(false);
        var brNull = new WasmNullBranch(WasmNullCondition.NULL, base, innerBlock);
        var br = new WasmBreak(block);
        br.setResult(extractor.apply(brNull));
        innerBlock.getBody().add(br);

        block.getBody().add(innerBlock);
        block.getBody().add(defaultValue);

        return block;
    }

    public static WasmExpression getArrayLengthOfNullable(WasmExpression base) {
        return getOrIfNull(WasmType.INT32, base, WasmArrayLength::new, new WasmInt32Constant(0));
    }
}
