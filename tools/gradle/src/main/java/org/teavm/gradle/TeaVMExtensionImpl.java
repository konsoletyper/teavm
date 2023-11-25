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
package org.teavm.gradle;

import groovy.lang.Closure;
import java.io.File;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.teavm.gradle.api.OptimizationLevel;
import org.teavm.gradle.api.SourceFilePolicy;
import org.teavm.gradle.api.TeaVMCConfiguration;
import org.teavm.gradle.api.TeaVMCommonConfiguration;
import org.teavm.gradle.api.TeaVMExtension;
import org.teavm.gradle.api.TeaVMJSConfiguration;
import org.teavm.gradle.api.TeaVMWasiConfiguration;
import org.teavm.gradle.api.TeaVMWasmConfiguration;

class TeaVMExtensionImpl extends TeaVMBaseExtensionImpl implements TeaVMExtension {
    private TeaVMJSConfiguration js;
    private TeaVMWasmConfiguration wasm;
    private TeaVMWasiConfiguration wasi;
    private TeaVMCConfiguration c;
    private TeaVMCommonConfiguration all;

    TeaVMExtensionImpl(Project project, ObjectFactory objectFactory) {
        super(project, objectFactory);
        js = objectFactory.newInstance(TeaVMJSConfiguration.class);
        wasm = objectFactory.newInstance(TeaVMWasmConfiguration.class);
        wasi = objectFactory.newInstance(TeaVMWasiConfiguration.class);
        c = objectFactory.newInstance(TeaVMCConfiguration.class);
        all = objectFactory.newInstance(TeaVMCommonConfiguration.class);
        inherit(js, all);
        inherit(wasm, all);
        inherit(wasi, all);
        inherit(c, all);
        setupDefaults();
    }

    private void setupDefaults() {
        setupJsDefaults();
        setupWasmDefaults();
        setupWasiDefaults();
        setupCDefaults();
        setupAllDefaults();
    }

    private void setupJsDefaults() {
        js.getRelativePathInOutputDir().convention("js");
        js.getObfuscated().convention(property("js.obfuscated").map(Boolean::parseBoolean).orElse(true));
        js.getSourceMap().convention(property("js.sourceMap").map(Boolean::parseBoolean).orElse(false));
        js.getStrict().convention(property("js.strict").map(Boolean::parseBoolean).orElse(false));
        js.getEntryPointName().convention("main");
        js.getTargetFileName().convention(project.provider(() -> project.getName() + ".js"));
        js.getAddedToWebApp().convention(property("js.addedToWebApp").map(Boolean::parseBoolean).orElse(false));
        js.getOptimization().convention(property("js.optimization").map(OptimizationLevel::valueOf)
                .orElse(OptimizationLevel.BALANCED));
        js.getSourceFilePolicy().convention(property("js.sourceFilePolicy")
                .map(SourceFilePolicy::valueOf)
                .orElse(SourceFilePolicy.DO_NOTHING));
    }

    private void setupWasmDefaults() {
        wasm.getRelativePathInOutputDir().convention("wasm");
        wasm.getMinHeapSize().convention(1);
        wasm.getMaxHeapSize().convention(16);
        wasm.getOptimization().convention(property("wasm.optimization").map(OptimizationLevel::valueOf)
                .orElse(OptimizationLevel.AGGRESSIVE));
        wasm.getTargetFileName().convention(project.provider(() -> project.getName() + ".wasm"));
        wasm.getAddedToWebApp().convention(property("wasm.addedToWebApp").map(Boolean::parseBoolean).orElse(false));
    }

    private void setupWasiDefaults() {
        wasi.getRelativePathInOutputDir().convention("wasi");
        wasi.getMinHeapSize().convention(1);
        wasi.getMaxHeapSize().convention(16);
        wasi.getOptimization().convention(property("wasi.optimization").map(OptimizationLevel::valueOf)
                .orElse(OptimizationLevel.AGGRESSIVE));
        wasi.getTargetFileName().convention(project.provider(() -> project.getName() + ".wasm"));
    }

    private void setupCDefaults() {
        c.getRelativePathInOutputDir().convention("c");
        c.getMinHeapSize().convention(1);
        c.getMaxHeapSize().convention(16);
        c.getHeapDump().convention(property("c.heapDump").map(Boolean::parseBoolean).orElse(false));
        c.getShortFileNames().convention(property("c.shortFileName").map(Boolean::parseBoolean).orElse(true));
        c.getOptimization().convention(property("c.optimization").map(OptimizationLevel::valueOf)
                .orElse(OptimizationLevel.AGGRESSIVE));
        c.getObfuscated().convention(true);
    }

    private void setupAllDefaults() {
        all.getOutputDir().convention(new File(project.getBuildDir(), "generated/teavm"));
        all.getDebugInformation().convention(property("debugInformation").map(Boolean::parseBoolean).orElse(false));
        all.getOptimization().convention(OptimizationLevel.BALANCED);
        all.getFastGlobalAnalysis().convention(property("fastGlobalAnalysis").map(Boolean::parseBoolean).orElse(false));
        all.getOutOfProcess().convention(property("outOfProcess").map(Boolean::parseBoolean).orElse(false));
        all.getProcessMemory().convention(property("processMemory").map(Integer::parseInt).orElse(512));
    }

    @Override
    public TeaVMJSConfiguration getJs() {
        return js;
    }

    @Override
    public void js(Action<TeaVMJSConfiguration> action) {
        action.execute(getJs());
    }

    @Override
    public void js(Closure<?> action) {
        action.rehydrate(getJs(), action.getOwner(), action.getThisObject()).call();
    }

    @Override
    public TeaVMWasmConfiguration getWasm() {
        return wasm;
    }

    @Override
    public void wasm(Action<TeaVMWasmConfiguration> action) {
        action.execute(getWasm());
    }

    @Override
    public void wasm(Closure<?> action) {
        action.rehydrate(getWasm(), action.getOwner(), action.getThisObject()).call();
    }

    @Override
    public TeaVMWasiConfiguration getWasi() {
        return wasi;
    }

    @Override
    public void wasi(Action<TeaVMWasiConfiguration> action) {
        action.execute(wasi);
    }

    @Override
    public void wasi(Closure<?> action) {
        action.rehydrate(getWasi(), action.getOwner(), action.getThisObject()).call();
    }

    @Override
    public TeaVMCConfiguration getC() {
        return c;
    }

    @Override
    public void c(Action<TeaVMCConfiguration> action) {
        action.execute(c);
    }

    @Override
    public void c(Closure<?> action) {
        action.rehydrate(getC(), action.getOwner(), action.getThisObject()).call();
    }

    @Override
    public TeaVMCommonConfiguration getAll() {
        return all;
    }

    @Override
    public void all(Action<TeaVMCommonConfiguration> action) {
        action.execute(getAll());
    }

    @Override
    public void all(Closure<?> action) {
        action.rehydrate(getAll(), action.getOwner(), action.getThisObject()).call();
    }

    private void inherit(TeaVMCommonConfiguration target, TeaVMCommonConfiguration source) {
        target.getMainClass().convention(source.getMainClass());
        target.getOutputDir().convention(source.getOutputDir());
        target.getDebugInformation().convention(source.getDebugInformation());
        target.getFastGlobalAnalysis().convention(source.getFastGlobalAnalysis());
        target.getOptimization().convention(source.getOptimization());
        target.getProperties().putAll(source.getProperties());

        target.getOutOfProcess().convention(source.getOutOfProcess());
        target.getProcessMemory().convention(source.getProcessMemory());
    }
}
