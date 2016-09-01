/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import org.teavm.ast.OperationType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ValueType;
import org.teavm.model.util.VariableType;

public final class WasmGeneratorUtil {
    private WasmGeneratorUtil() {
    }

    public static WasmType mapType(OperationType type) {
        switch (type) {
            case INT:
                return WasmType.INT32;
            case LONG:
                return WasmType.INT64;
            case FLOAT:
                return WasmType.FLOAT32;
            case DOUBLE:
                return WasmType.FLOAT64;
        }
        throw new IllegalArgumentException(type.toString());
    }

    public static WasmType mapType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case CHARACTER:
                case INTEGER:
                    return WasmType.INT32;
                case LONG:
                    return WasmType.INT64;
                case FLOAT:
                    return WasmType.FLOAT32;
                case DOUBLE:
                    return WasmType.FLOAT64;
            }
        } else if (type == ValueType.VOID) {
            return null;
        }
        return WasmType.INT32;
    }

    public static WasmType mapType(VariableType type) {
        switch (type) {
            case INT:
                return WasmType.INT32;
            case LONG:
                return WasmType.INT64;
            case FLOAT:
                return WasmType.FLOAT32;
            case DOUBLE:
                return WasmType.FLOAT64;
            default:
                return WasmType.INT32;
        }
    }
}
