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
package org.teavm.devserver;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.backend.wasm.WasmDebugInfoLocation;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.backend.wasm.debug.sourcemap.SourceMapBuilder;
import org.teavm.cache.MethodNodeCache;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.MemoryBuildTarget;

public class CodeServletWasmGCBackend implements CodeServletBackend<WasmGCTarget> {
    private SourceMapBuilder sourceMapBuilder;

    @Override
    public WasmGCTarget createTarget() {
        return new WasmGCTarget();
    }

    @Override
    public void setup(WasmGCTarget target, MethodNodeCache astCache, ReferenceCache referenceCache,
            CodeServletSettings settings) {
        target.setDebugInfo(true);
        target.setDebugInfoLevel(WasmDebugInfoLevel.FULL);
        target.setDebugInfoLocation(WasmDebugInfoLocation.EMBEDDED);
        target.setSharedBuffer(settings.wasmSharedBuffer());
        target.setSourceMapLocation(settings.fileName() + ".map");
        sourceMapBuilder = new SourceMapBuilder();
        target.setSourceMapBuilder(sourceMapBuilder);
    }

    @Override
    public void afterBuild(MemoryBuildTarget buildTarget, CodeServletSettings settings) {
        var fileName = settings.fileName() + ".map";
        sourceMapBuilder.addSourceResolver(fn -> "src/" + fn);
        try (var writer = new OutputStreamWriter(buildTarget.createResource(fileName), StandardCharsets.UTF_8)) {
            sourceMapBuilder.writeSourceMap(writer);
        } catch (IOException e) {
            throw new RuntimeException("IO error occurred writing debug information", e);
        }
        var name = settings.wasmModularRuntime() ? "wasm-gc-modular-runtime" : "wasm-gc-runtime";
        var resourceName = "org/teavm/backend/wasm/" + name + ".js";
        try (var writer = buildTarget.createResource(settings.fileName() + "-runtime.js");
                var reader = CodeServlet.class.getClassLoader().getResourceAsStream(resourceName)) {
            reader.transferTo(writer);
        } catch (IOException e) {
            throw new RuntimeException("IO error occurred writing JS runtime", e);
        }
        sourceMapBuilder = null;
    }
}
