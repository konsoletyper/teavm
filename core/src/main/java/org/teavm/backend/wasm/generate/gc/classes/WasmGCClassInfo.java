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

import java.util.List;
import java.util.function.Consumer;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.ValueType;

public class WasmGCClassInfo {
    private ValueType valueType;
    WasmStructure structure;
    WasmStructure virtualTableStructure;
    WasmGlobal pointer;
    WasmGlobal initializerPointer;
    Consumer<List<WasmExpression>> initializer;
    WasmFunction newArrayFunction;
    WasmFunction initArrayFunction;
    WasmFunction supertypeFunction;

    WasmGCClassInfo(ValueType valueType) {
        this.valueType = valueType;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public WasmStructure getStructure() {
        return structure;
    }

    public WasmArray getArray() {
        var field = structure.getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        var type = (WasmType.CompositeReference) field.getUnpackedType();
        return (WasmArray) type.composite;
    }

    public WasmStructure getVirtualTableStructure() {
        return virtualTableStructure;
    }

    public WasmType.CompositeReference getType() {
        return getStructure().getReference();
    }

    public WasmGlobal getPointer() {
        return pointer;
    }

    public WasmGlobal getInitializerPointer() {
        return initializerPointer;
    }
}
