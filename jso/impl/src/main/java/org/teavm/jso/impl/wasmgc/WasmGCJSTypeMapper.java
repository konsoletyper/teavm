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
package org.teavm.jso.impl.wasmgc;

import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapper;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapperFactory;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapperFactoryContext;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.jso.impl.JSTypeHelper;
import org.teavm.model.ValueType;

class WasmGCJSTypeMapper implements WasmGCCustomTypeMapper, WasmGCCustomTypeMapperFactory {
    private JSTypeHelper typeHelper;
    private WasmGCClassInfoProvider classInfoProvider;

    @Override
    public WasmType map(String className) {
        if (typeHelper.isJavaScriptClass(className)) {
            return WasmType.Reference.EXTERN;
        } else if (className.equals(WasmGCJSRuntime.CharArrayData.class.getName())) {
            var cls = classInfoProvider.getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
            var field = cls.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
            var ref = (WasmType.CompositeReference) field.getType().asUnpackedType();
            return ref.composite.getReference();
        } else if (className.equals(WasmGCJSRuntime.NonNullExternal.class.getName())) {
            return WasmType.SpecialReferenceKind.EXTERN.asNonNullType();
        }
        return null;
    }

    @Override
    public WasmGCCustomTypeMapper createTypeMapper(WasmGCCustomTypeMapperFactoryContext context) {
        this.typeHelper = new JSTypeHelper(context.originalClasses());
        this.classInfoProvider = context.classInfoProvider();
        return this;
    }
}
