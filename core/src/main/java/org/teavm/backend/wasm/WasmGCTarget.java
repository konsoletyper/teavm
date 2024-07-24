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
package org.teavm.backend.wasm;

import java.io.IOException;
import java.util.List;
import org.teavm.backend.wasm.gc.WasmGCDependencies;
import org.teavm.backend.wasm.generate.gc.WasmGCDeclarationsGenerator;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerators;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.render.WasmBinaryRenderer;
import org.teavm.backend.wasm.render.WasmBinaryStatsCollector;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.backend.wasm.render.WasmBinaryWriter;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.interop.Platforms;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;
import org.teavm.model.transformation.BoundCheckInsertion;
import org.teavm.model.transformation.NullCheckFilter;
import org.teavm.model.transformation.NullCheckInsertion;
import org.teavm.model.util.VariableCategoryProvider;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;

public class WasmGCTarget implements TeaVMTarget {
    private TeaVMTargetController controller;
    private NullCheckInsertion nullCheckInsertion;
    private BoundCheckInsertion boundCheckInsertion = new BoundCheckInsertion();
    private boolean obfuscated;

    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;
        nullCheckInsertion = new NullCheckInsertion(NullCheckFilter.EMPTY);
    }

    @Override
    public VariableCategoryProvider variableCategoryProvider() {
        return null;
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        return List.of();
    }

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        return List.of();
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        new WasmGCDependencies(dependencyAnalyzer).contribute();
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return List.of();
    }

    @Override
    public void beforeOptimizations(Program program, MethodReader method) {
        /*
        nullCheckInsertion.transformProgram(program, method.getReference());
        boundCheckInsertion.transformProgram(program, method.getReference());
        */
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method) {
    }

    @Override
    public String[] getPlatformTags() {
        return new String[] { Platforms.WEBASSEMBLY_GC };
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName) throws IOException {
        var module = new WasmModule();
        var customGenerators = new WasmGCCustomGenerators();
        var declarationsGenerator = new WasmGCDeclarationsGenerator(
                module,
                classes,
                controller::isVirtual,
                controller.getClassInitializerInfo(),
                controller.getDependencyInfo(),
                controller.getDiagnostics(),
                customGenerators
        );
        declarationsGenerator.setFriendlyToDebugger(controller.isFriendlyToDebugger());
        var moduleGenerator = new WasmGCModuleGenerator(this, declarationsGenerator);
        var mainFunction = moduleGenerator.generateMainFunction(controller.getEntryPoint());
        mainFunction.setExportName(controller.getEntryPointName());
        mainFunction.setName(controller.getEntryPointName());
        moduleGenerator.generate();

        emitWasmFile(module, buildTarget, outputName);
    }

    private void emitWasmFile(WasmModule module, BuildTarget buildTarget, String outputName) throws IOException {
        var binaryWriter = new WasmBinaryWriter();
        var binaryRenderer = new WasmBinaryRenderer(binaryWriter, WasmBinaryVersion.V_0x1, obfuscated,
                null, null, null, null, WasmBinaryStatsCollector.EMPTY);
        binaryRenderer.render(module);
        var data = binaryWriter.getData();
        if (!outputName.endsWith(".wasm")) {
            outputName += ".wasm";
        }
        try (var output = buildTarget.createResource(outputName)) {
            output.write(data);
        }
    }
}
