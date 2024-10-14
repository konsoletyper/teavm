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
package org.teavm.gradle.tasks;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.teavm.gradle.api.WasmDebugInfoLevel;
import org.teavm.gradle.api.WasmDebugInfoLocation;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.builder.BuildStrategy;

public abstract class GenerateWasmGCTask extends TeaVMTask {
    public GenerateWasmGCTask() {
        getStrict().convention(true);
        getObfuscated().convention(true);
        getDebugInfo().convention(true);
        getDebugInfoLevel().convention(WasmDebugInfoLevel.DEOBFUSCATION);
        getDebugInfoLocation().convention(WasmDebugInfoLocation.EXTERNAL);
        getSourceMap().convention(false);
    }

    @Input
    public abstract Property<Boolean> getStrict();

    @Input
    public abstract Property<Boolean> getObfuscated();

    @Input
    public abstract Property<Boolean> getDebugInfo();

    @Input
    public abstract Property<WasmDebugInfoLevel> getDebugInfoLevel();

    @Input
    public abstract Property<WasmDebugInfoLocation> getDebugInfoLocation();

    @Input
    public abstract Property<Boolean> getSourceMap();

    @Override
    protected void setupBuilder(BuildStrategy builder) {
        builder.setStrict(getStrict().get());
        builder.setObfuscated(getObfuscated().get());
        builder.setDebugInformationGenerated(getDebugInfo().get());
        builder.setSourceMapsFileGenerated(getSourceMap().get());
        switch (getDebugInfoLevel().get()) {
            case FULL:
                builder.setWasmDebugInfoLevel(org.teavm.backend.wasm.WasmDebugInfoLevel.FULL);
                break;
            case DEOBFUSCATION:
                builder.setWasmDebugInfoLevel(org.teavm.backend.wasm.WasmDebugInfoLevel.DEOBFUSCATION);
                break;
        }
        switch (getDebugInfoLocation().get()) {
            case EMBEDDED:
                builder.setWasmDebugInfoLocation(org.teavm.backend.wasm.WasmDebugInfoLocation.EMBEDDED);
                break;
            case EXTERNAL:
                builder.setWasmDebugInfoLocation(org.teavm.backend.wasm.WasmDebugInfoLocation.EXTERNAL);
                break;
        }
        builder.setTargetType(TeaVMTargetType.WEBASSEMBLY_GC);
    }
}
