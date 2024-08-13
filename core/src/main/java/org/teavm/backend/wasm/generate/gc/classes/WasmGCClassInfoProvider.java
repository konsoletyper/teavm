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

import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;

public interface WasmGCClassInfoProvider {
    int CLASS_FIELD_OFFSET = 0;
    int MONITOR_FIELD_OFFSET = 1;
    int CUSTOM_FIELD_OFFSETS = 2;
    int ARRAY_DATA_FIELD_OFFSET = 2;

    WasmGCClassInfo getClassInfo(ValueType type);

    int getFieldIndex(FieldReference fieldRef);

    WasmGlobal getStaticFieldLocation(FieldReference fieldRef);

    int getVirtualMethodsOffset();

    int getClassArrayItemOffset();

    int getClassSupertypeFunctionOffset();

    default WasmGCClassInfo getClassInfo(String name) {
        return getClassInfo(ValueType.object(name));
    }
}
