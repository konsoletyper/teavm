/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.perf;

import java.io.File;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.perf.runtime.BenchmarkWasmGCEntryPoint;

public class WasmGCBackend extends BrowserBackend {
    private static final String RUNTIME_RESOURCE = "org/teavm/backend/wasm/wasm-gc-runtime.js";
    private static final String DEOBFUSCATOR_RESOURCE = "org/teavm/backend/wasm/deobfuscator.wasm";

    public WasmGCBackend(BenchmarkEnvironment environment, String browser) {
        super(environment, browser);
    }

    @Override
    public String getName() {
        return "wasm-gc";
    }

    @Override
    public String getDescription() {
        return "TeaVM WebAssembly GC";
    }

    @Override
    protected String getRunnerType() {
        return "WASM_GC";
    }

    @Override
    public CompiledBenchmark compile(BenchmarkInfo benchmark, File directory) throws BenchmarkException {
        var target = new WasmGCTarget();
        build(target, BenchmarkWasmGCEntryPoint.class.getName(), benchmark, directory, "benchmark.wasm");
        copyResource(RUNTIME_RESOURCE, new File(directory, "benchmark.wasm-runtime.js"));
        if (BenchmarkBackend.class.getClassLoader().getResource(DEOBFUSCATOR_RESOURCE) != null) {
            copyResource(DEOBFUSCATOR_RESOURCE, new File(directory, "benchmark.wasm-deobfuscator.wasm"));
        }
        return new CompiledBenchmark(benchmark, directory, new File(directory, "benchmark.wasm"));
    }
}
