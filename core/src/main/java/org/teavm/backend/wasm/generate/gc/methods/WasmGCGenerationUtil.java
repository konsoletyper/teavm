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
package org.teavm.backend.wasm.generate.gc.methods;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.model.ValueType;

public class WasmGCGenerationUtil {
    private WasmGCClassInfoProvider classInfoProvider;

    public WasmGCGenerationUtil(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    public WasmExpression allocateArray(ValueType itemType, Supplier<WasmExpression> length) {
        return allocateArray(itemType, arrayType -> new WasmArrayNewDefault(arrayType, length.get()));
    }

    public WasmExpression allocateArrayWithElements(ValueType itemType,
            Supplier<List<? extends WasmExpression>> data) {
        return allocateArray(itemType, arrayType -> {
            var expr = new WasmArrayNewFixed(arrayType);
            expr.getElements().addAll(data.get());
            return expr;
        });
    }

    public WasmExpression allocateArray(ValueType itemType,  Function<WasmArray, WasmExpression> data) {
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));

        var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                .getUnpackedType();
        var wasmArray = (WasmArray) wasmArrayType.composite;

        var structNew = new WasmStructNew(classInfo.getStructure());
        structNew.getInitializers().add(new WasmGetGlobal(classInfo.getVirtualTablePointer()));
        structNew.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));
        structNew.getInitializers().add(data.apply(wasmArray));
        return structNew;
    }
}
