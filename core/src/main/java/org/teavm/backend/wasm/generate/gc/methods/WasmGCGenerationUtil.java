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
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public class WasmGCGenerationUtil {
    private WasmGCClassInfoProvider classInfoProvider;
    private TemporaryVariablePool tempVars;

    public WasmGCGenerationUtil(WasmGCClassInfoProvider classInfoProvider, TemporaryVariablePool tempVars) {
        this.classInfoProvider = classInfoProvider;
        this.tempVars = tempVars;
    }

    public void allocateArray(ValueType itemType, WasmExpression length, TextLocation location, WasmLocal local,
            List<WasmExpression> target) {
        allocateArray(itemType, location, local, target, arrayType -> new WasmArrayNewDefault(arrayType, length));
    }

    public void allocateArray(ValueType itemType, List<? extends WasmExpression> data, TextLocation location,
            WasmLocal local, List<WasmExpression> target) {
        allocateArray(itemType, location, local, target, arrayType -> {
            var expr = new WasmArrayNewFixed(arrayType);
            expr.getElements().addAll(data);
            return expr;
        });
    }

    public void allocateArray(ValueType itemType, TextLocation location,
            WasmLocal local, List<WasmExpression> target, Function<WasmArray, WasmExpression> data) {
        var classInfo = classInfoProvider.getClassInfo(ValueType.arrayOf(itemType));
        var block = new WasmBlock(false);
        block.setType(classInfo.getType());
        var targetVar = local;
        if (targetVar == null) {
            targetVar = tempVars.acquire(classInfo.getType());
        }

        var structNew = new WasmSetLocal(targetVar, new WasmStructNewDefault(classInfo.getStructure()));
        structNew.setLocation(location);
        target.add(structNew);

        var initClassField = new WasmStructSet(classInfo.getStructure(), new WasmGetLocal(targetVar),
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET, new WasmGetGlobal(classInfo.getPointer()));
        initClassField.setLocation(location);
        target.add(initClassField);

        var wasmArrayType = (WasmType.CompositeReference) classInfo.getStructure().getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET)
                .getUnpackedType();
        var wasmArray = (WasmArray) wasmArrayType.composite;
        var initArrayField = new WasmStructSet(
                classInfo.getStructure(),
                new WasmGetLocal(targetVar),
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET,
                data.apply(wasmArray)
        );
        initArrayField.setLocation(location);
        target.add(initArrayField);

        if (local == null) {
            var getLocal = new WasmGetLocal(targetVar);
            getLocal.setLocation(location);
            target.add(getLocal);
            tempVars.release(targetVar);
        }
    }
}
