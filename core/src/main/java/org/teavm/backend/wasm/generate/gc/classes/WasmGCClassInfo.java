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

import com.carrotsearch.hppc.ObjectIntMap;
import java.util.List;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTable;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ValueType;

public class WasmGCClassInfo {
    private WasmGCClassGenerator classGenerator;
    private ValueType valueType;
    boolean heapStructure;
    WasmGCVirtualTable virtualTable;
    WasmStructure structure;
    WasmStructure virtualTableStructure;
    WasmGlobal pointer;
    WasmGlobal virtualTablePointer;
    WasmGlobal initializerPointer;
    List<WasmFunction> newArrayFunctions;
    WasmFunction initArrayFunction;
    WasmFunction supertypeFunction;
    ObjectIntMap<String> heapFieldOffsets;
    int heapSize;
    int heapAlignment;

    WasmGCClassInfo(WasmGCClassGenerator classGenerator, ValueType valueType) {
        this.classGenerator = classGenerator;
        this.valueType = valueType;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public boolean isHeapStructure() {
        return heapStructure;
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
        if (pointer == null) {
            classGenerator.initClassPointer(this);
            if (virtualTablePointer != null) {
                classGenerator.assignClassToVT(this);
            }
        }
        return pointer;
    }

    public WasmGlobal getVirtualTablePointer() {
        if (virtualTableStructure == null
                || !(valueType instanceof ValueType.Array || valueType instanceof ValueType.Object)) {
            return null;
        }
        if (virtualTablePointer == null) {
            classGenerator.initClassVirtualTable(this);
            classGenerator.assignClassToVT(this);
        }
        return virtualTablePointer;
    }

    public WasmGlobal getInitializerPointer() {
        return initializerPointer;
    }
}
