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
package org.teavm.backend.wasm.intrinsics.gc;

import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapper;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapperFactory;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapperFactoryContext;
import org.teavm.backend.wasm.model.WasmType;

public class WasmGCAsyncTypeMapperFactory implements WasmGCCustomTypeMapperFactory {
    @Override
    public WasmGCCustomTypeMapper createTypeMapper(WasmGCCustomTypeMapperFactoryContext context) {
        return className -> {
            if (className.equals("org.teavm.runtime.Fiber$PlatformObject")) {
                return WasmType.Reference.ANY;
            } else if (className.equals("org.teavm.runtime.Fiber$PlatformFunction")) {
                return WasmType.Reference.FUNC;
            }
            return null;
        };
    }
}
