/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.reflection;

import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapper;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ValueType;

public class WasmGCReflectionTypeMapper implements WasmGCCustomTypeMapper {
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmFunctionTypes functionTypes;

    public WasmGCReflectionTypeMapper(WasmGCClassInfoProvider classInfoProvider, WasmFunctionTypes functionTypes) {
        this.classInfoProvider = classInfoProvider;
        this.functionTypes = functionTypes;
    }

    @Override
    public WasmType map(String className) {
        switch (className) {
            case "org.teavm.classlib.impl.reflection.ObjectList":
                return classInfoProvider.getObjectArrayType().getReference();

            case "org.teavm.classlib.impl.reflection.FieldInfo":
                return classInfoProvider.reflection().getReflectionFieldType().getReference();
            case "org.teavm.classlib.impl.reflection.FieldInfoList":
                return classInfoProvider.reflection().getReflectionFieldArrayType().getReference();

            case "org.teavm.classlib.impl.reflection.MethodInfo":
                return classInfoProvider.reflection().getReflectionMethodType().getReference();
            case "org.teavm.classlib.impl.reflection.MethodInfoList":
                return classInfoProvider.reflection().getReflectionMethodArrayType().getReference();

            case "org.teavm.classlib.impl.reflection.ClassList":
                return classInfoProvider.reflection().getClassArrayType().getReference();

            case "org.teavm.classlib.impl.reflection.FieldReader": {
                var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
                return functionTypes.of(objType, objType).getReference();
            }
            case "org.teavm.classlib.impl.reflection.FieldWriter": {
                var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
                return functionTypes.of(null, objType, objType).getReference();
            }
            case "org.teavm.classlib.impl.reflection.MethodCaller": {
                var objType = classInfoProvider.getClassInfo("java.lang.Object").getType();
                var objArrayType = classInfoProvider.getClassInfo(
                        ValueType.arrayOf(ValueType.object("java.lang.Object"))).getType();
                return functionTypes.of(objType, objType, objArrayType).getReference();
            }
        }
        return null;
    }
}
