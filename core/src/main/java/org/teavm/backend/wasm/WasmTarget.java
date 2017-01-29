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
import java.util.Comparator;
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
import org.teavm.backend.wasm.intrinsics.ExceptionHandlingIntrinsic;
import org.teavm.backend.wasm.intrinsics.FunctionIntrinsic;
import org.teavm.backend.wasm.intrinsics.GCIntrinsic;
import org.teavm.backend.wasm.intrinsics.MutatorIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformClassIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformObjectIntrinsic;
import org.teavm.backend.wasm.intrinsics.ShadowStackIntrinsic;
import org.teavm.backend.wasm.intrinsics.StructureIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmRuntimeIntrinsic;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.optimization.UnusedFunctionElimination;
import org.teavm.backend.wasm.patches.ClassPatch;
import org.teavm.backend.wasm.render.WasmBinaryRenderer;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.backend.wasm.render.WasmBinaryWriter;
import org.teavm.backend.wasm.render.WasmCRenderer;
import org.teavm.backend.wasm.render.WasmRenderer;
import org.teavm.backend.wasm.transformation.IndirectCallTraceTransformation;
import org.teavm.backend.wasm.transformation.MemoryAccessTraceTransformation;
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
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.lowlevel.ClassInitializerEliminator;
import org.teavm.model.lowlevel.ClassInitializerTransformer;
import org.teavm.model.lowlevel.ShadowStackTransformer;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.ExceptionHandling;
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
    private boolean wastEmitted;
    private boolean cEmitted;
    private ClassInitializerEliminator classInitializerEliminator;
    private ClassInitializerTransformer classInitializerTransformer;
    private ShadowStackTransformer shadowStackTransformer;
    private MethodDescriptor clinitDescriptor = new MethodDescriptor("<clinit>", void.class);
    private WasmBinaryVersion version = WasmBinaryVersion.V_0xC;

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;
        classInitializerEliminator = new ClassInitializerEliminator(controller.getUnprocessedClassSource());
        classInitializerTransformer = new ClassInitializerTransformer();
        shadowStackTransformer = new ShadowStackTransformer(controller.getUnprocessedClassSource());
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

    public boolean isWastEmitted() {
        return wastEmitted;
    }

    public void setWastEmitted(boolean wastEmitted) {
        this.wastEmitted = wastEmitted;
    }

    public boolean isCEmitted() {
        return cEmitted;
    }

    public void setCEmitted(boolean cEmitted) {
        this.cEmitted = cEmitted;
    }

    public WasmBinaryVersion getVersion() {
        return version;
    }

    public void setVersion(WasmBinaryVersion version) {
        this.version = version;
    }

    @Override
    public void contributeDependencies(DependencyChecker dependencyChecker) {
        for (Class<?> type : Arrays.asList(int.class, long.class, float.class, double.class)) {
            MethodReference method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
            dependencyChecker.linkMethod(method, null).use();
        }
        for (Class<?> type : Arrays.asList(float.class, double.class)) {
            MethodReference method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
            dependencyChecker.linkMethod(method, null).use();
        }

        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "align", Address.class, int.class,
                Address.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "fillZero", Address.class, int.class,
                void.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "moveMemoryBlock", Address.class,
                Address.class, int.class, void.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "allocStack",
                int.class, Address.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "getStackTop", Address.class),
                null) .use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "getNextStackFrame", Address.class,
                Address.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "getStackRootCount", Address.class,
                int.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "getStackRootPointer", Address.class,
                Address.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "setExceptionHandlerId", Address.class,
                int.class, void.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(WasmRuntime.class, "getCallSiteId", Address.class,
                int.class), null).use();

        dependencyChecker.linkMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(Allocator.class, "allocateMultiArray",
                RuntimeClass.class, Address.class, int.class, RuntimeArray.class), null).use();

        dependencyChecker.linkMethod(new MethodReference(Allocator.class, "<clinit>", void.class), null).use();

        dependencyChecker.linkMethod(new MethodReference(ExceptionHandling.class, "throwException",
                Throwable.class, void.class), null).use();

        dependencyChecker.linkMethod(new MethodReference(ExceptionHandling.class, "catchException",
                Throwable.class), null).use();

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
    public void afterOptimizations(Program program, MethodReader method, ListableClassReaderSource classes) {
        ClassReader cls = classes.get(method.getOwnerName());
        boolean hasClinit = cls.getMethod(clinitDescriptor) != null;
        if (needsClinitCall(method) && hasClinit) {
            BasicBlock entryBlock = program.basicBlockAt(0);
            InitClassInstruction initInsn = new InitClassInstruction();
            initInsn.setClassName(method.getOwnerName());
            entryBlock.addFirst(initInsn);
        }

        classInitializerEliminator.apply(program);
        classInitializerTransformer.transform(program);
        shadowStackTransformer.apply(program, method);
    }

    private static boolean needsClinitCall(MethodReader method) {
        if (method.getName().equals("<clinit>")) {
            return false;
        }
        if (method.getName().equals("<init>")) {
            return true;
        }
        return method.hasModifier(ElementModifier.STATIC);
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName)
            throws IOException {
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
        WasmGenerationContext context = new WasmGenerationContext(classes, module, controller.getDiagnostics(),
                vtableProvider, tagRegistry, stringPool);

        context.addIntrinsic(new AddressIntrinsic(classGenerator));
        context.addIntrinsic(new StructureIntrinsic(classGenerator));
        context.addIntrinsic(new FunctionIntrinsic(classGenerator));
        WasmRuntimeIntrinsic wasmRuntimeIntrinsic = new WasmRuntimeIntrinsic();
        context.addIntrinsic(wasmRuntimeIntrinsic);
        context.addIntrinsic(new AllocatorIntrinsic(classGenerator));
        context.addIntrinsic(new PlatformIntrinsic());
        context.addIntrinsic(new PlatformClassIntrinsic());
        context.addIntrinsic(new PlatformObjectIntrinsic(classGenerator));
        context.addIntrinsic(new ClassIntrinsic());
        GCIntrinsic gcIntrinsic = new GCIntrinsic();
        context.addIntrinsic(gcIntrinsic);
        MutatorIntrinsic mutatorIntrinsic = new MutatorIntrinsic();
        context.addIntrinsic(mutatorIntrinsic);
        context.addIntrinsic(new ShadowStackIntrinsic());
        ExceptionHandlingIntrinsic exceptionHandlingIntrinsic = new ExceptionHandlingIntrinsic(binaryWriter,
                classGenerator);
        context.addIntrinsic(exceptionHandlingIntrinsic);

        WasmGenerator generator = new WasmGenerator(decompiler, classes, context, classGenerator, binaryWriter);

        module.setMemorySize(128);
        generateMethods(classes, context, generator, module);
        exceptionHandlingIntrinsic.postProcess(shadowStackTransformer.getCallSites());
        generateIsSupertypeFunctions(tagRegistry, module, classGenerator);
        classGenerator.postProcess();
        mutatorIntrinsic.setStaticGcRootsAddress(classGenerator.getStaticGcRootsAddress());

        WasmMemorySegment dataSegment = new WasmMemorySegment();
        dataSegment.setData(binaryWriter.getData());
        dataSegment.setOffset(256);
        module.getSegments().add(dataSegment);

        renderMemoryLayout(module, binaryWriter.getAddress(), gcIntrinsic, wasmRuntimeIntrinsic);
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

        for (String functionName : classGenerator.getFunctionTable()) {
            WasmFunction function = module.getFunctions().get(functionName);
            assert function != null : "Function referenced from function table not found: " + functionName;
            module.getFunctionTable().add(function);
        }

        new UnusedFunctionElimination(module).apply();

        if (Boolean.parseBoolean(System.getProperty("wasm.memoryTrace", "false"))) {
            new MemoryAccessTraceTransformation(module).apply();
        }
        if (Boolean.parseBoolean(System.getProperty("wasm.indirectCallTrace", "false"))) {
            new IndirectCallTraceTransformation(module).apply();
        }

        WasmBinaryWriter writer = new WasmBinaryWriter();
        WasmBinaryRenderer renderer = new WasmBinaryRenderer(writer, version);
        renderer.render(module);

        try (OutputStream output = buildTarget.createResource(outputName)) {
            output.write(writer.getData());
            output.flush();
        }

        if (wastEmitted) {
            emitWast(module, buildTarget, getBaseName(outputName) + ".wast");
        }
        if (cEmitted) {
            emitC(module, buildTarget, getBaseName(outputName) + ".c");
        }
    }

    private String getBaseName(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? name : name.substring(0, index);
    }

    private void emitWast(WasmModule module, BuildTarget buildTarget, String outputName) throws IOException {
        WasmRenderer renderer = new WasmRenderer();
        renderer.setLineNumbersEmitted(debugging);
        renderer.render(module);
        try (OutputStream output = buildTarget.createResource(outputName);
                Writer writer = new OutputStreamWriter(output, "UTF-8")) {
            writer.write(renderer.toString());
        }
    }

    private void emitC(WasmModule module, BuildTarget buildTarget, String outputName) throws IOException {
        WasmCRenderer renderer = new WasmCRenderer();
        renderer.setLineNumbersEmitted(Boolean.parseBoolean(System.getProperty("wasm.c.lineNumbers", "false")));
        renderer.setMemoryAccessChecked(Boolean.parseBoolean(System.getProperty("wasm.c.assertMemory", "false")));
        renderer.render(module);
        try (OutputStream output = buildTarget.createResource(outputName);
                Writer writer = new OutputStreamWriter(output, "UTF-8")) {
            writer.write(renderer.toString());
        }
    }

    private void generateMethods(ListableClassHolderSource classes, WasmGenerationContext context,
            WasmGenerator generator, WasmModule module) {
        List<MethodHolder> methods = new ArrayList<>();
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (context.getIntrinsic(method.getReference()) != null) {
                    continue;
                }
                module.add(generator.generateDefinition(method.getReference()));
                methods.add(method);
            }
        }

        for (MethodHolder method : methods) {
            ClassHolder cls = classes.get(method.getOwnerName());

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
                generator.generateNative(method.getReference());
                continue;
            }
            if (implementor.getProgram() == null || implementor.getProgram().basicBlockCount() == 0) {
                continue;
            }
            if (method == implementor) {
                generator.generate(method.getReference(), implementor);
            } else {
                generateStub(module, method, implementor);
            }
            if (controller.wasCancelled()) {
                return;
            }
        }
    }

    private void generateIsSupertypeFunctions(TagRegistry tagRegistry, WasmModule module,
            WasmClassGenerator classGenerator) {
        for (ValueType type : classGenerator.getRegisteredClasses()) {
            WasmFunction function = new WasmFunction(WasmMangling.mangleIsSupertype(type));
            function.getParameters().add(WasmType.INT32);
            function.setResult(WasmType.INT32);
            module.add(function);

            WasmLocal subtypeVar = new WasmLocal(WasmType.INT32, "subtype");
            function.add(subtypeVar);

            if (type instanceof ValueType.Object) {
                String className = ((ValueType.Object) type).getClassName();
                generateIsClass(subtypeVar, classGenerator, tagRegistry, className, function.getBody());
            } else if (type instanceof ValueType.Array) {
                ValueType itemType = ((ValueType.Array) type).getItemType();
                generateIsArray(subtypeVar, classGenerator, itemType, function.getBody());
            } else {
                int expected = classGenerator.getClassPointer(type);
                WasmExpression condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ,
                        new WasmGetLocal(subtypeVar), new WasmInt32Constant(expected));
                function.getBody().add(new WasmReturn(condition));
            }
        }
    }

    private void generateIsClass(WasmLocal subtypeVar, WasmClassGenerator classGenerator, TagRegistry tagRegistry,
            String className, List<WasmExpression> body) {
        List<TagRegistry.Range> ranges = tagRegistry.getRanges(className);
        if (ranges.isEmpty()) {
            body.add(new WasmReturn(new WasmInt32Constant(0)));
            return;
        }

        int tagOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeClass.class.getName(), "tag"));

        WasmExpression tagExpression = new WasmGetLocal(subtypeVar);
        tagExpression = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, tagExpression,
                new WasmInt32Constant(tagOffset));
        tagExpression = new WasmLoadInt32(4, tagExpression, WasmInt32Subtype.INT32);
        body.add(new WasmSetLocal(subtypeVar, tagExpression));

        ranges.sort(Comparator.comparingInt(range -> range.lower));

        int lower = ranges.get(0).lower;
        int upper = ranges.get(ranges.size() - 1).upper;

        WasmExpression lowerCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                new WasmGetLocal(subtypeVar), new WasmInt32Constant(lower));
        WasmConditional testLower = new WasmConditional(lowerCondition);
        testLower.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));
        body.add(testLower);

        WasmExpression upperCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                new WasmGetLocal(subtypeVar), new WasmInt32Constant(upper));
        WasmConditional testUpper = new WasmConditional(upperCondition);
        testUpper.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));
        body.add(testUpper);

        for (int i = 1; i < ranges.size(); ++i) {
            int lowerHole = ranges.get(i - 1).upper;
            int upperHole = ranges.get(i).lower;

            lowerCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                    new WasmGetLocal(subtypeVar), new WasmInt32Constant(lowerHole));
            testLower = new WasmConditional(lowerCondition);
            body.add(testLower);

            upperCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                    new WasmGetLocal(subtypeVar), new WasmInt32Constant(upperHole));
            testUpper = new WasmConditional(upperCondition);
            testUpper.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));

            testLower.getThenBlock().getBody().add(testUpper);
        }

        body.add(new WasmReturn(new WasmInt32Constant(1)));
    }

    private void generateIsArray(WasmLocal subtypeVar, WasmClassGenerator classGenerator, ValueType itemType,
            List<WasmExpression> body) {
        int itemOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeClass.class.getName(), "itemType"));

        WasmExpression itemExpression = new WasmGetLocal(subtypeVar);
        itemExpression = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, itemExpression,
                new WasmInt32Constant(itemOffset));
        itemExpression = new WasmLoadInt32(4, itemExpression, WasmInt32Subtype.INT32);
        body.add(new WasmSetLocal(subtypeVar, itemExpression));

        WasmExpression itemCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ,
                new WasmGetLocal(subtypeVar), new WasmInt32Constant(0));
        WasmConditional itemTest = new WasmConditional(itemCondition);
        itemTest.setType(WasmType.INT32);
        itemTest.getThenBlock().getBody().add(new WasmInt32Constant(0));

        WasmCall delegateToItem = new WasmCall(WasmMangling.mangleIsSupertype(itemType));
        delegateToItem.getArguments().add(new WasmGetLocal(subtypeVar));
        itemTest.getElseBlock().getBody().add(delegateToItem);

        body.add(new WasmReturn(itemTest));
    }

    private WasmFunction generateStub(WasmModule module, MethodHolder method, MethodHolder implementor) {
        WasmFunction function = module.getFunctions().get(WasmMangling.mangleMethod(method.getReference()));

        WasmCall call = new WasmCall(WasmMangling.mangleMethod(implementor.getReference()));
        for (WasmType param : function.getParameters()) {
            WasmLocal local = new WasmLocal(param);
            function.add(local);
            call.getArguments().add(new WasmGetLocal(local));
        }

        if (method.getResultType() == ValueType.VOID) {
            function.getBody().add(call);
        } else {
            function.getBody().add(new WasmReturn(call));
        }
        return function;
    }

    private void renderClinit(ListableClassReaderSource classes, WasmClassGenerator classGenerator,
            WasmModule module) {
        for (ValueType type : classGenerator.getRegisteredClasses()) {
            if (!(type instanceof ValueType.Object)) {
                continue;
            }
            String className = ((ValueType.Object) type).getClassName();
            if (classGenerator.isStructure(className)) {
                continue;
            }

            ClassReader cls = classes.get(className);
            if (cls == null) {
                continue;
            }
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

    private void renderMemoryLayout(WasmModule module, int address, GCIntrinsic gcIntrinsic,
            WasmRuntimeIntrinsic runtimeIntrinsic) {
        address = (((address - 1) / 256) + 1) * 256;

        runtimeIntrinsic.setStackAddress(address);
        address += 65536;

        int gcMemory = module.getMemorySize() * 65536 - address;
        int storageSize = (gcMemory >> 6) >> 2 << 2;
        gcIntrinsic.setGCStorageAddress(address);
        gcIntrinsic.setGCStorageSize(storageSize);

        gcMemory -= storageSize;
        address += storageSize;
        int regionSize = 32768;
        int regionCount = gcMemory / (2 + regionSize) + 1;
        gcIntrinsic.setRegionSize(regionSize);
        gcIntrinsic.setRegionsAddress(address);
        gcIntrinsic.setRegionMaxCount(regionCount);

        address += regionCount * 2;
        address = (address + 4) >> 2 << 2;

        gcMemory = module.getMemorySize() * 65536 - address;
        gcIntrinsic.setHeapAddress(address);
        gcIntrinsic.setAvailableBytes(gcMemory);
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
                    for (Instruction insn : block) {
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
