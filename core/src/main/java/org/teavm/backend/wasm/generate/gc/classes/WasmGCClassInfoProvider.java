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

import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;

public interface WasmGCClassInfoProvider {
    int CLASS_FIELD_OFFSET = 0;
    int VT_FIELD_OFFSET = 0;
    int VIRTUAL_METHOD_OFFSET = 1;
    int MONITOR_FIELD_OFFSET = 1;
    int CUSTOM_FIELD_OFFSETS = 2;
    int ARRAY_DATA_FIELD_OFFSET = 2;
    int WEAK_REFERENCE_OFFSET = 2;
    int STRING_POOL_ENTRY_OFFSET = 5;

    WasmGCClassInfo getClassInfo(ValueType type);

    WasmStructure getArrayVirtualTableStructure();

    int getFieldIndex(FieldReference fieldRef);

    int getHeapFieldOffset(FieldReference fieldRef);

    int getHeapSize(String className);

    int getHeapAlignment(String className);

    WasmGlobal getStaticFieldLocation(FieldReference fieldRef);

    WasmFunction getArrayConstructor(ValueType type);

    WasmFunction getMultiArrayConstructor(int depth);

    WasmFunction getGetArrayClassFunction();

    int getClassArrayItemOffset();

    int getClassFlagsOffset();

    int getClassSupertypeFunctionOffset();

    int getClassEnclosingClassOffset();

    int getClassDeclaringClassOffset();

    int getClassParentOffset();

    int getNewArrayFunctionOffset();

    int getClassNameOffset();

    int getClassSimpleNameOffset();

    int getClassCanonicalNameOffset();

    int getClassVtFieldOffset();

    int getClassAnnotationsOffset();

    int getArrayGetOffset();

    int getArrayLengthOffset();

    int getArrayCopyOffset();

    int getEnumConstantsFunctionOffset();

    int getCloneOffset();

    int getServicesOffset();

    int getThrowableNativeOffset();

    default WasmGCClassInfo getClassInfo(String name) {
        return getClassInfo(ValueType.object(name));
    }
}
