/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.junit;

import static org.teavm.junit.PropertyNames.OPTIMIZED;
import static org.teavm.junit.PropertyNames.WASI_ENABLED;
import static org.teavm.junit.PropertyNames.WASI_RUNNER;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.WasmRuntimeType;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ReferenceCache;

class WasiPlatformSupport extends BaseWebAssemblyPlatformSupport {
    WasiPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache) {
        super(classSource, referenceCache);
    }

    @Override
    TestRunStrategy createRunStrategy(File outputDir) {
        String wasiCommand = System.getProperty(WASI_RUNNER);
        if (wasiCommand != null) {
            return new WasiRunStrategy(wasiCommand);
        }
        return null;
    }

    @Override
    protected WasmRuntimeType getRuntimeType() {
        return WasmRuntimeType.WASI;
    }

    @Override
    TestPlatform getPlatform() {
        return TestPlatform.WASI;
    }

    @Override
    String getPath() {
        return "wasi";
    }

    @Override
    boolean isEnabled() {
        return Boolean.getBoolean(WASI_ENABLED);
    }

    @Override
    List<TeaVMTestConfiguration<WasmTarget>> getConfigurations() {
        List<TeaVMTestConfiguration<WasmTarget>> configurations = new ArrayList<>();
        configurations.add(TeaVMTestConfiguration.WASM_DEFAULT);
        if (Boolean.getBoolean(OPTIMIZED)) {
            configurations.add(TeaVMTestConfiguration.WASM_OPTIMIZED);
        }
        return configurations;
    }
}
