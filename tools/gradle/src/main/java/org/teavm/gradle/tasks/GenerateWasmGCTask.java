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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.teavm.gradle.api.SourceFilePolicy;
import org.teavm.gradle.api.WasmDebugInfoLevel;
import org.teavm.gradle.api.WasmDebugInfoLocation;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.builder.BuildStrategy;

public abstract class GenerateWasmGCTask extends TeaVMTask {
    public GenerateWasmGCTask() {
        getStrict().convention(true);
        getObfuscated().convention(true);
        getDebugInfoLevel().convention(WasmDebugInfoLevel.DEOBFUSCATION);
        getDebugInfoLocation().convention(WasmDebugInfoLocation.EXTERNAL);
        getSourceMap().convention(false);
        getSourceFilePolicy().convention(SourceFilePolicy.LINK_LOCAL_FILES);
        getMinDirectBuffersSize().convention(2);
        getMaxDirectBuffersSize().convention(32);
        getImportedWasmMemory().convention(false);
    }

    @Input
    public abstract Property<Boolean> getStrict();

    @Input
    public abstract Property<Boolean> getObfuscated();

    @Input
    public abstract Property<WasmDebugInfoLevel> getDebugInfoLevel();

    @Input
    public abstract Property<WasmDebugInfoLocation> getDebugInfoLocation();

    @Input
    public abstract Property<Boolean> getSourceMap();

    @InputFiles
    public abstract ConfigurableFileCollection getSourceFiles();

    @Input
    @Optional
    public abstract Property<SourceFilePolicy> getSourceFilePolicy();

    @Input
    public abstract Property<Integer> getMinDirectBuffersSize();

    @Input
    public abstract Property<Integer> getMaxDirectBuffersSize();

    @Input
    public abstract Property<Boolean> getImportedWasmMemory();

    @Override
    protected void setupBuilder(BuildStrategy builder) {
        builder.setStrict(getStrict().get());
        builder.setObfuscated(getObfuscated().get());
        builder.setDebugInformationGenerated(getDebugInformation().get());
        builder.setSourceMapsFileGenerated(getSourceMap().get());
        builder.setMinDirectBuffersSize(getMinDirectBuffersSize().get() * 1024 * 1024);
        builder.setMaxDirectBuffersSize(getMaxDirectBuffersSize().get() * 1024 * 1024);
        builder.setImportedWasmMemory(getImportedWasmMemory().get());
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
        TaskUtils.applySourceFiles(getSourceFiles(), builder);
        TaskUtils.applySourceFilePolicy(getSourceFilePolicy(), builder);
    }
}
