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

import java.util.function.Consumer;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.WasmGCClassConsumerContext;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.model.ClassReaderSource;

interface WasmGCJsoContext {
    ClassReaderSource classes();

    WasmModule module();

    WasmFunctionTypes functionTypes();

    BaseWasmFunctionRepository functions();

    WasmGCNameProvider names();

    WasmGCStringProvider strings();

    WasmGCTypeMapper typeMapper();

    WasmTag exceptionTag();

    String entryPoint();

    void addToInitializer(Consumer<WasmFunction> initializerContributor);

    static WasmGCJsoContext wrap(WasmGCIntrinsicContext context) {
        return new WasmGCJsoContext() {
            @Override
            public ClassReaderSource classes() {
                return context.hierarchy().getClassSource();
            }

            @Override
            public WasmModule module() {
                return context.module();
            }

            @Override
            public WasmFunctionTypes functionTypes() {
                return context.functionTypes();
            }

            @Override
            public BaseWasmFunctionRepository functions() {
                return context.functions();
            }

            @Override
            public WasmGCNameProvider names() {
                return context.names();
            }

            @Override
            public WasmGCStringProvider strings() {
                return context.strings();
            }

            @Override
            public WasmGCTypeMapper typeMapper() {
                return context.typeMapper();
            }

            @Override
            public WasmTag exceptionTag() {
                return context.exceptionTag();
            }

            @Override
            public String entryPoint() {
                return context.entryPoint();
            }

            @Override
            public void addToInitializer(Consumer<WasmFunction> initializerContributor) {
                context.addToInitializer(initializerContributor);
            }
        };
    }

    static WasmGCJsoContext wrap(WasmGCCustomGeneratorContext context) {
        return new WasmGCJsoContext() {
            @Override
            public ClassReaderSource classes() {
                return context.classes();
            }

            @Override
            public WasmModule module() {
                return context.module();
            }

            @Override
            public WasmFunctionTypes functionTypes() {
                return context.functionTypes();
            }

            @Override
            public BaseWasmFunctionRepository functions() {
                return context.functions();
            }

            @Override
            public WasmGCNameProvider names() {
                return context.names();
            }

            @Override
            public WasmGCStringProvider strings() {
                return context.strings();
            }

            @Override
            public WasmGCTypeMapper typeMapper() {
                return context.typeMapper();
            }

            @Override
            public WasmTag exceptionTag() {
                return context.exceptionTag();
            }

            @Override
            public String entryPoint() {
                return context.entryPoint();
            }

            @Override
            public void addToInitializer(Consumer<WasmFunction> initializerContributor) {
                context.addToInitializer(initializerContributor);
            }
        };
    }

    static WasmGCJsoContext wrap(WasmGCClassConsumerContext context) {
        return new WasmGCJsoContext() {
            @Override
            public ClassReaderSource classes() {
                return context.classes();
            }

            @Override
            public WasmModule module() {
                return context.module();
            }

            @Override
            public WasmFunctionTypes functionTypes() {
                return context.functionTypes();
            }

            @Override
            public BaseWasmFunctionRepository functions() {
                return context.functions();
            }

            @Override
            public WasmGCNameProvider names() {
                return context.names();
            }

            @Override
            public WasmGCStringProvider strings() {
                return context.strings();
            }

            @Override
            public WasmGCTypeMapper typeMapper() {
                return context.typeMapper();
            }

            @Override
            public WasmTag exceptionTag() {
                return context.exceptionTag();
            }

            @Override
            public String entryPoint() {
                return context.entryPoint();
            }

            @Override
            public void addToInitializer(Consumer<WasmFunction> initializerContributor) {
                context.addToInitializer(initializerContributor);
            }
        };
    }
}
