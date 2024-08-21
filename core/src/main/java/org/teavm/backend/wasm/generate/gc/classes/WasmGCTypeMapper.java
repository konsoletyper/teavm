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
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmPackedType;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.ValueType;

public class WasmGCTypeMapper {
    private ClassReaderSource classes;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmFunctionTypes functionTypes;
    private WasmModule module;

    WasmGCTypeMapper(ClassReaderSource classes, WasmGCClassInfoProvider classInfoProvider,
            WasmFunctionTypes functionTypes, WasmModule module) {
        this.classes = classes;
        this.classInfoProvider = classInfoProvider;
        this.functionTypes = functionTypes;
        this.module = module;
    }

    public WasmStorageType mapStorageType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                case BOOLEAN:
                    return WasmStorageType.packed(WasmPackedType.INT8);
                case SHORT:
                case CHARACTER:
                    return WasmStorageType.packed(WasmPackedType.INT8);
                case INTEGER:
                    return WasmType.INT32.asStorage();
                case LONG:
                    return WasmType.INT64.asStorage();
                case FLOAT:
                    return WasmType.FLOAT32.asStorage();
                case DOUBLE:
                    return WasmType.FLOAT64.asStorage();
                default:
                    throw new IllegalArgumentException();
            }
        } else {
            return classInfoProvider.getClassInfo(type).getType().asStorage();
        }
    }

    public WasmType mapType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                case BOOLEAN:
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
                default:
                    throw new IllegalArgumentException();
            }
        } else if (type instanceof ValueType.Void) {
            return null;
        } else if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            var cls = classes.get(className);
            if (cls == null) {
                className = "java.lang.Object";
            }
            return classInfoProvider.getClassInfo(className).getType();
        } else if (type instanceof ValueType.Array) {
            var degree = 0;
            while (type instanceof ValueType.Array) {
                type = ((ValueType.Array) type).getItemType();
                ++degree;
            }
            if (type instanceof ValueType.Object) {
                var className = ((ValueType.Object) type).getClassName();
                var cls = classes.get(className);
                if (cls != null) {
                    className = "java.lang.Object";
                }
            }
            while (degree-- > 0) {
                type = ValueType.arrayOf(type);
            }
            return classInfoProvider.getClassInfo(type).getType();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public WasmFunctionType getFunctionType(String className, MethodDescriptor methodDesc, boolean fresh) {
        var returnType = mapType(methodDesc.getResultType());
        var javaParamTypes = methodDesc.getParameterTypes();
        var paramTypes = new WasmType[javaParamTypes.length + 1];
        paramTypes[0] = classInfoProvider.getClassInfo(className).getType();
        for (var i = 0; i < javaParamTypes.length; ++i) {
            paramTypes[i + 1] = mapType(javaParamTypes[i]);
        }
        if (fresh) {
            var type = new WasmFunctionType(null, returnType, List.of(paramTypes));
            module.types.add(type);
            return type;
        } else {
            return functionTypes.of(returnType, paramTypes);
        }
    }
}
