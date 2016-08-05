/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.wasm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.dependency.DependencyChecker;
import org.teavm.interop.Import;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMEntryPoint;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;
import org.teavm.wasm.generate.WasmGenerationContext;
import org.teavm.wasm.generate.WasmGenerator;
import org.teavm.wasm.generate.WasmMangling;
import org.teavm.wasm.model.WasmFunction;
import org.teavm.wasm.model.WasmModule;
import org.teavm.wasm.render.WasmRenderer;
import org.teavm.wasm.runtime.WasmRuntime;

public class WasmTarget implements TeaVMTarget {
    private TeaVMTargetController controller;

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return Collections.emptyList();
    }

    @Override
    public boolean requiresRegisterAllocation() {
        return true;
    }

    @Override
    public void contributeDependencies(DependencyChecker dependencyChecker) {
        for (Class type : Arrays.asList(int.class, long.class, float.class, double.class)) {
            MethodReference method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
            dependencyChecker.linkMethod(method, null).use();
        }
        for (Class type : Arrays.asList(float.class, double.class)) {
            MethodReference method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
            dependencyChecker.linkMethod(method, null).use();
        }
    }

    @Override
    public void emit(ListableClassHolderSource classes, OutputStream output, BuildTarget buildTarget) {
        Decompiler decompiler = new Decompiler(classes, controller.getClassLoader(), new HashSet<>(), new HashSet<>());
        WasmGenerationContext context = new WasmGenerationContext(classes);
        WasmGenerator generator = new WasmGenerator(decompiler, classes, context);

        WasmModule module = new WasmModule();
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (method.hasModifier(ElementModifier.NATIVE)) {
                    if (context.getImportedMethod(method.getReference()) == null) {
                        CallLocation location = new CallLocation(method.getReference());
                        controller.getDiagnostics().error(location, "Method {{m0}} is native but "
                                + "has no {{c1}} annotation on it", method.getReference(), Import.class);
                    }
                    module.add(generator.generateNative(method.getReference()));
                    continue;
                }
                if (method.getProgram() == null || method.getProgram().basicBlockCount() == 0) {
                    continue;
                }
                module.add(generator.generate(method.getReference()));
                if (controller.wasCancelled()) {
                    return;
                }
            }
        }

        for (TeaVMEntryPoint entryPoint : controller.getEntryPoints().values()) {
            String mangledName = WasmMangling.mangleMethod(entryPoint.getReference());
            WasmFunction function = module.getFunctions().get(mangledName);
            if (function != null) {
                function.setExportName(entryPoint.getPublicName());
            }
        }

        WasmRenderer renderer = new WasmRenderer();
        renderer.render(module);

        try {
            Writer writer = new OutputStreamWriter(output, "UTF-8");
            writer.append(renderer.toString());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        TeaVM vm = new TeaVMBuilder(new WasmTarget()).build();
        vm.installPlugins();
        vm.entryPoint("main", new MethodReference(Example.class, "main", String[].class, void.class));
        try (OutputStream output = new FileOutputStream(args[0])) {
            vm.build(output, null);
            System.err.println("Problems found: " + vm.getProblemProvider().getProblems().size());
        }
    }
}
