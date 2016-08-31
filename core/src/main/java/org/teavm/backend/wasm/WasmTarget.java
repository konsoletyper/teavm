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
package org.teavm.backend.wasm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.generate.WasmDependencyListener;
import org.teavm.backend.wasm.generate.WasmGenerationContext;
import org.teavm.backend.wasm.generate.WasmGenerator;
import org.teavm.backend.wasm.generate.WasmMangling;
import org.teavm.backend.wasm.generate.WasmStringPool;
import org.teavm.backend.wasm.intrinsics.AddressIntrinsic;
import org.teavm.backend.wasm.intrinsics.AllocatorIntrinsic;
import org.teavm.backend.wasm.intrinsics.ClassIntrinsic;
import org.teavm.backend.wasm.intrinsics.FunctionIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformClassIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformObjectIntrinsic;
import org.teavm.backend.wasm.intrinsics.StructureIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmRuntimeIntrinsic;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.patches.ClassPatch;
import org.teavm.backend.wasm.patches.ObjectPatch;
import org.teavm.backend.wasm.render.WasmCRenderer;
import org.teavm.dependency.ClassDependency;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyListener;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeJavaObject;
import org.teavm.runtime.RuntimeObject;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMEntryPoint;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;

public class WasmTarget implements TeaVMTarget {
    private TeaVMTargetController controller;
    private boolean debugging;

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
    public List<ClassHolderTransformer> getTransformers() {
        List<ClassHolderTransformer> transformers = new ArrayList<>();
        transformers.add(new ObjectPatch());
        transformers.add(new ClassPatch());
        transformers.add(new WasmDependencyListener());
        return transformers;
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        List<DependencyListener> listeners = new ArrayList<>();
        listeners.add(new WasmDependencyListener());
        return listeners;
    }

    public boolean isDebugging() {
        return debugging;
    }

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
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

        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "decodeData", Address.class,
                Address.class, void.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "fillZero", Address.class, int.class,
                void.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "moveMemoryBlock", Address.class,
                Address.class, int.class, void.class), null).use();

        dependencyChecker.linkMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class), null).use();

        dependencyChecker.linkMethod(new MethodReference(Allocator.class, "<clinit>", void.class), null).use();

        dependencyChecker.linkField(new FieldReference("java.lang.Object", "monitor"), null);

        ClassDependency runtimeClassDep = dependencyChecker.linkClass(RuntimeClass.class.getName(), null);
        ClassDependency runtimeObjectDep = dependencyChecker.linkClass(RuntimeObject.class.getName(), null);
        ClassDependency runtimeJavaObjectDep = dependencyChecker.linkClass(RuntimeJavaObject.class.getName(), null);
        ClassDependency runtimeArrayDep = dependencyChecker.linkClass(RuntimeArray.class.getName(), null);
        for (ClassDependency classDep : Arrays.asList(runtimeClassDep, runtimeObjectDep, runtimeJavaObjectDep,
                runtimeArrayDep)) {
            for (FieldReader field : classDep.getClassReader().getFields()) {
                dependencyChecker.linkField(field.getReference(), null);
            }
        }
    }

    @Override
    public void emit(ListableClassHolderSource classes, OutputStream output, BuildTarget buildTarget) {
        WasmModule module = new WasmModule();
        WasmFunction initFunction = new WasmFunction("__start__");

        VirtualTableProvider vtableProvider = createVirtualTableProvider(classes);
        TagRegistry tagRegistry = new TagRegistry(classes);
        BinaryWriter binaryWriter = new BinaryWriter(256);
        WasmClassGenerator classGenerator = new WasmClassGenerator(
                classes, vtableProvider, tagRegistry, binaryWriter);

        Decompiler decompiler = new Decompiler(classes, controller.getClassLoader(), new HashSet<>(),
                new HashSet<>());
        WasmStringPool stringPool = new WasmStringPool(classGenerator, binaryWriter);
        WasmGenerationContext context = new WasmGenerationContext(classes, controller.getDiagnostics(),
                vtableProvider, tagRegistry, stringPool);

        context.addIntrinsic(new AddressIntrinsic(classGenerator));
        context.addIntrinsic(new StructureIntrinsic(classGenerator));
        context.addIntrinsic(new FunctionIntrinsic(classGenerator));
        context.addIntrinsic(new WasmRuntimeIntrinsic());
        context.addIntrinsic(new AllocatorIntrinsic());
        context.addIntrinsic(new PlatformIntrinsic());
        context.addIntrinsic(new PlatformClassIntrinsic());
        context.addIntrinsic(new PlatformObjectIntrinsic(classGenerator));
        context.addIntrinsic(new ClassIntrinsic());

        WasmGenerator generator = new WasmGenerator(decompiler, classes, context, classGenerator);

        module.setMemorySize(64);

        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (method.getOwnerName().equals(Allocator.class.getName())
                        && method.getName().equals("initialize")) {
                    continue;
                }
                if (context.getIntrinsic(method.getReference()) != null) {
                    continue;
                }

                MethodHolder implementor = method;
                AnnotationHolder delegateAnnot = method.getAnnotations().get(DelegateTo.class.getName());
                if (delegateAnnot != null) {
                    String methodName = delegateAnnot.getValue("value").getString();
                    boolean found = false;
                    for (MethodHolder candidate : cls.getMethods()) {
                        if (candidate.getName().equals(methodName)) {
                            if (found) {
                                controller.getDiagnostics().error(new CallLocation(method.getReference()),
                                        "Method is delegated to " + methodName + " but several implementations "
                                        + "found");
                                break;
                            }
                            implementor = candidate;
                            found = true;
                        }
                    }
                }

                if (implementor.hasModifier(ElementModifier.NATIVE)) {
                    if (context.getImportedMethod(method.getReference()) == null) {
                        CallLocation location = new CallLocation(method.getReference());
                        controller.getDiagnostics().error(location, "Method {{m0}} is native but "
                                + "has no {{c1}} annotation on it", method.getReference(), Import.class.getName());
                    }
                    module.add(generator.generateNative(method.getReference()));
                    continue;
                }
                if (implementor.getProgram() == null || implementor.getProgram().basicBlockCount() == 0) {
                    continue;
                }
                module.add(generator.generate(method.getReference(), implementor));
                if (controller.wasCancelled()) {
                    return;
                }
            }
        }

        classGenerator.postProcess();

        WasmMemorySegment dataSegment = new WasmMemorySegment();
        dataSegment.setData(binaryWriter.getData());
        dataSegment.setOffset(256);
        module.getSegments().add(dataSegment);

        WasmMemorySegment metadataSegment = new WasmMemorySegment();
        metadataSegment.setData(binaryWriter.getMetadata());
        metadataSegment.setOffset(binaryWriter.getAddress());
        module.getSegments().add(metadataSegment);

        MethodReference initData = new MethodReference(WasmRuntime.class, "decodeData", Address.class,
                Address.class, void.class);
        WasmCall initDataCall = new WasmCall(WasmMangling.mangleMethod(initData));
        initDataCall.getArguments().add(new WasmInt32Constant(256));
        initDataCall.getArguments().add(new WasmInt32Constant(binaryWriter.getAddress()));
        initFunction.getBody().add(initDataCall);

        renderAllocatorInit(module, binaryWriter.getAddress());
        renderClinit(classes, classGenerator, module);
        if (controller.wasCancelled()) {
            return;
        }

        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            if (cls.getAnnotations().get(StaticInit.class.getName()) == null) {
                continue;
            }
            MethodReader clinit = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
            if (clinit == null) {
                continue;
            }
            initFunction.getBody().add(new WasmCall(WasmMangling.mangleInitializer(className)));
        }
        module.add(initFunction);
        module.setStartFunction(initFunction);

        for (TeaVMEntryPoint entryPoint : controller.getEntryPoints().values()) {
            String mangledName = WasmMangling.mangleMethod(entryPoint.getReference());
            WasmFunction function = module.getFunctions().get(mangledName);
            if (function != null) {
                function.setExportName(entryPoint.getPublicName());
            }
        }

        for (String function : classGenerator.getFunctionTable()) {
            module.getFunctionTable().add(module.getFunctions().get(function));
        }

        WasmCRenderer renderer = new WasmCRenderer();
        renderer.setOutputLineNumbers(debugging);
        renderer.render(module);

        try {
            Writer writer = new OutputStreamWriter(output, "UTF-8");
            writer.append(renderer.toString());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderClinit(ListableClassReaderSource classes, WasmClassGenerator classGenerator,
            WasmModule module) {
        for (String className : classes.getClassNames()) {
            if (classGenerator.isStructure(className)) {
                continue;
            }

            ClassReader cls = classes.get(className);
            MethodReader method = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
            if (method == null) {
                continue;
            }

            WasmFunction initFunction = new WasmFunction(WasmMangling.mangleInitializer(className));
            module.add(initFunction);

            WasmBlock block = new WasmBlock(false);

            int index = classGenerator.getClassPointer(ValueType.object(className))
                    + classGenerator.getFieldOffset(new FieldReference(RuntimeClass.class.getName(), "flags"));
            WasmExpression initFlag = new WasmLoadInt32(4, new WasmInt32Constant(index), WasmInt32Subtype.INT32);
            initFlag = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND, initFlag,
                    new WasmInt32Constant(RuntimeClass.INITIALIZED));
            block.getBody().add(new WasmBranch(initFlag, block));
            initFunction.getBody().add(block);

            initFlag = new WasmLoadInt32(4, new WasmInt32Constant(index), WasmInt32Subtype.INT32);
            initFlag = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.OR, initFlag,
                    new WasmInt32Constant(RuntimeClass.INITIALIZED));
            block.getBody().add(new WasmStoreInt32(4, new WasmInt32Constant(index), initFlag,
                    WasmInt32Subtype.INT32));

            if (method != null) {
                block.getBody().add(new WasmCall(WasmMangling.mangleMethod(method.getReference())));
            }

            if (controller.wasCancelled()) {
                break;
            }
        }
    }

    private void renderAllocatorInit(WasmModule module, int address) {
        address = (((address - 1) / 4096) + 1) * 4096;

        WasmFunction function = new WasmFunction(WasmMangling.mangleMethod(new MethodReference(
                Allocator.class, "initialize", Address.class)));
        function.setResult(WasmType.INT32);
        function.getBody().add(new WasmReturn(new WasmInt32Constant(address)));
        module.add(function);
    }

    private VirtualTableProvider createVirtualTableProvider(ListableClassHolderSource classes) {
        Set<MethodReference> virtualMethods = new HashSet<>();

        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                Program program = method.getProgram();
                if (program == null) {
                    continue;
                }
                for (int i = 0; i < program.basicBlockCount(); ++i) {
                    BasicBlock block = program.basicBlockAt(i);
                    for (Instruction insn : block.getInstructions()) {
                        if (insn instanceof InvokeInstruction) {
                            InvokeInstruction invoke = (InvokeInstruction) insn;
                            if (invoke.getType() == InvocationType.VIRTUAL) {
                                virtualMethods.add(invoke.getMethod());
                            }
                        } else if (insn instanceof CloneArrayInstruction) {
                            virtualMethods.add(new MethodReference(Object.class, "clone", Object.class));
                        }
                    }
                }
            }
        }

        return new VirtualTableProvider(classes, virtualMethods);
    }
}
