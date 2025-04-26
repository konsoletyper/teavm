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
package org.teavm.backend.wasm.generate.gc.methods;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.PreciseTypeInference;
import org.teavm.backend.wasm.gc.PreciseValueType;
import org.teavm.backend.wasm.gc.WasmGCVariableCategoryProvider;
import org.teavm.backend.wasm.gc.vtable.WasmGCVirtualTableProvider;
import org.teavm.backend.wasm.generate.gc.WasmGCInitializerContributor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCStandardClasses;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCSupertypeFunctionProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Import;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.RegisterAllocator;
import org.teavm.model.util.UsageExtractor;

public class WasmGCMethodGenerator implements BaseWasmFunctionRepository {
    private WasmModule module;
    private ClassHierarchy hierarchy;
    private ListableClassHolderSource classes;
    private ClassLoader classLoader;
    private WasmGCVirtualTableProvider virtualTables;
    private ClassInitializerInfo classInitInfo;
    private WasmFunctionTypes functionTypes;
    private WasmGCSupertypeFunctionProvider supertypeFunctions;
    public final WasmGCNameProvider names;
    private Diagnostics diagnostics;
    private WasmGCTypeMapper typeMapper;
    private WasmGCCustomGeneratorProvider customGenerators;
    private WasmGCIntrinsicProvider intrinsics;
    private Queue<Runnable> queue = new ArrayDeque<>();
    private Map<MethodReference, WasmFunction> staticMethods = new HashMap<>();
    private Map<MethodReference, WasmFunction> instanceMethods = new HashMap<>();
    private boolean friendlyToDebugger;
    private Decompiler decompiler;
    private WasmGCGenerationContext context;
    private WasmFunction dummyInitializer;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmGCStandardClasses standardClasses;
    private WasmGCStringProvider strings;
    private boolean strict;
    private String entryPoint;
    private Consumer<WasmGCInitializerContributor> initializerContributors;
    private boolean compactMode;
    private DependencyInfo dependency;

    public WasmGCMethodGenerator(
            WasmModule module,
            ClassHierarchy hierarchy,
            ListableClassHolderSource classes,
            ClassLoader classLoader,
            WasmGCVirtualTableProvider virtualTables,
            ClassInitializerInfo classInitInfo,
            WasmFunctionTypes functionTypes,
            WasmGCNameProvider names,
            Diagnostics diagnostics,
            WasmGCCustomGeneratorProvider customGenerators,
            WasmGCIntrinsicProvider intrinsics,
            DependencyInfo dependency,
            boolean strict,
            String entryPoint,
            Consumer<WasmGCInitializerContributor> initializerContributors
    ) {
        this.module = module;
        this.hierarchy = hierarchy;
        this.classes = classes;
        this.classLoader = classLoader;
        this.virtualTables = virtualTables;
        this.classInitInfo = classInitInfo;
        this.functionTypes = functionTypes;
        this.names = names;
        this.diagnostics = diagnostics;
        this.customGenerators = customGenerators;
        this.intrinsics = intrinsics;
        this.dependency = dependency;
        this.strict = strict;
        this.entryPoint = entryPoint;
        this.initializerContributors = initializerContributors;
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }

    public void setTypeMapper(WasmGCTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    public void setFriendlyToDebugger(boolean friendlyToDebugger) {
        this.friendlyToDebugger = friendlyToDebugger;
    }

    public void setClassInfoProvider(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    public void setStandardClasses(WasmGCStandardClasses standardClasses) {
        this.standardClasses = standardClasses;
    }

    public void setSupertypeFunctions(WasmGCSupertypeFunctionProvider supertypeFunctions) {
        this.supertypeFunctions = supertypeFunctions;
    }

    public void setStrings(WasmGCStringProvider strings) {
        this.strings = strings;
    }

    public boolean process() {
        if (queue.isEmpty()) {
            return false;
        }
        while (!queue.isEmpty()) {
            queue.remove().run();
        }
        return true;
    }

    public boolean hasSomethingToGenerate() {
        return !queue.isEmpty();
    }

    @Override
    public WasmFunction forStaticMethod(MethodReference methodReference) {
        return staticMethods.computeIfAbsent(methodReference, this::createStaticFunction);
    }

    private WasmFunction createStaticFunction(MethodReference methodReference) {
        var returnType = typeMapper.mapType(methodReference.getReturnType());
        var parameterTypes = new WasmType[methodReference.parameterCount()];
        for (var i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = typeMapper.mapType(methodReference.parameterType(i));
        }
        var function = new WasmFunction(functionTypes.of(returnType, parameterTypes));
        function.setName(names.topLevel(names.suggestForMethod(methodReference)));
        module.functions.add(function);
        function.setJavaMethod(methodReference);

        var cls = classes.get(methodReference.getClassName());
        if (cls != null) {
            var method = cls.getMethod(methodReference.getDescriptor());
            if (method != null && method.hasModifier(ElementModifier.STATIC)) {
                queue.add(() -> generateMethodBody(method, function));
            }
        }

        return function;
    }

    @Override
    public WasmFunction forInstanceMethod(MethodReference methodReference) {
        return instanceMethods.computeIfAbsent(methodReference, this::createInstanceFunction);
    }

    private WasmFunction createInstanceFunction(MethodReference methodReference) {
        var returnType = typeMapper.mapType(methodReference.getReturnType());
        var parameterTypes = new WasmType[methodReference.parameterCount() + 1];
        var compactMethod = compactMode
                && typeMapper.mapType(ValueType.object(methodReference.getClassName())) instanceof WasmType.Reference;
        parameterTypes[0] = compactMethod
                ? WasmType.Reference.ANY
                : typeMapper.mapType(ValueType.object(methodReference.getClassName()));
        for (var i = 0; i < methodReference.parameterCount(); ++i) {
            parameterTypes[i + 1] = typeMapper.mapType(methodReference.parameterType(i));
        }
        var function = new WasmFunction(functionTypes.of(returnType, parameterTypes));
        function.setName(names.topLevel(names.suggestForMethod(methodReference)));
        module.functions.add(function);
        function.setJavaMethod(methodReference);

        var cls = classes.get(methodReference.getClassName());
        if (cls != null) {
            var method = cls.getMethod(methodReference.getDescriptor());
            if (method != null && !method.hasModifier(ElementModifier.STATIC)) {
                queue.add(() -> generateMethodBody(method, function));
            }
        }

        return function;
    }

    private void generateMethodBody(MethodHolder method, WasmFunction function) {
        try {
            var customGenerator = customGenerators.get(method.getReference());
            if (customGenerator != null) {
                generateCustomMethodBody(customGenerator, method.getReference(), function);
            } else if (!method.hasModifier(ElementModifier.NATIVE)) {
                generateRegularMethodBody(method, function);
            } else {
                generateNativeMethodBody(method, function);
            }
        } catch (RuntimeException e) {
            var buffer = new StringWriter();
            var printWriter = new PrintWriter(buffer);
            e.printStackTrace(printWriter);
            diagnostics.error(new CallLocation(method.getReference()),
                    "Failed generating method body due to internal exception: " + buffer);
        }
    }

    private void generateCustomMethodBody(WasmGCCustomGenerator customGenerator, MethodReference method,
            WasmFunction function) {
        customGenerator.apply(method, function, customGeneratorContext);
    }

    private void generateRegularMethodBody(MethodHolder method, WasmFunction function) {
        Objects.requireNonNull(method.getProgram());
        eliminateMultipleNullConstantUsages(method.getProgram());
        var decompiler = getDecompiler();
        var categoryProvider = new WasmGCVariableCategoryProvider(hierarchy);
        var methodCompact = compactMode && !method.hasModifier(ElementModifier.STATIC)
                && typeMapper.mapType(ValueType.object(method.getOwnerName())) instanceof WasmType.Reference;
        categoryProvider.setCompactMode(methodCompact);
        var allocator = new RegisterAllocator(categoryProvider);
        allocator.allocateRegisters(method.getReference(), method.getProgram(), friendlyToDebugger);
        var ast = decompiler.decompileRegular(method);
        var firstVar = method.hasModifier(ElementModifier.STATIC) ? 1 : 0;
        var typeInference = new PreciseTypeInference(method.getProgram(), method.getReference(), hierarchy);
        typeInference.setPhisSkipped(true);
        typeInference.setBackPropagation(true);
        typeInference.ensure();

        var registerCount = 0;
        for (var i = 0; i < method.getProgram().variableCount(); ++i) {
            registerCount = Math.max(registerCount, method.getProgram().variableAt(i).getRegister() + 1);
        }
        var originalIndexToIndex = new int[registerCount];
        Arrays.fill(originalIndexToIndex, -1);
        for (var varNode : ast.getVariables()) {
            originalIndexToIndex[varNode.getOriginalIndex()] = varNode.getIndex();
        }

        var variableRepresentatives = new int[registerCount];
        Arrays.fill(variableRepresentatives, -1);
        for (var i = 0; i < method.getProgram().variableCount(); ++i) {
            var variable = method.getProgram().variableAt(i);
            var varNodeIndex = variable.getRegister() >= 0 ? originalIndexToIndex[variable.getRegister()] : -1;
            if (varNodeIndex >= 0 && variableRepresentatives[varNodeIndex] < 0) {
                if (typeInference.typeOf(variable) != null) {
                    variableRepresentatives[varNodeIndex] = variable.getIndex();
                }
            }
        }
        for (var i = 0; i < method.getProgram().variableCount(); ++i) {
            var variable = method.getProgram().variableAt(i);
            var varNodeIndex = variable.getRegister() >= 0 ? originalIndexToIndex[variable.getRegister()] : -1;
            if (varNodeIndex >= 0 && variableRepresentatives[varNodeIndex] < 0) {
                variableRepresentatives[varNodeIndex] = variable.getIndex();
            }
        }

        var nonNullableVars = new boolean[ast.getVariables().size()];
        var preciseTypes = new PreciseValueType[ast.getVariables().size()];
        for (var i = firstVar; i < ast.getVariables().size(); ++i) {
            var representative = method.getProgram().variableAt(variableRepresentatives[i]);
            var inferredType = typeInference.typeOf(representative);
            if (inferredType == null) {
                inferredType = new PreciseValueType(ValueType.object("java.lang.Object"), false);
            }
            preciseTypes[i] = inferredType;
            nonNullableVars[i] = inferredType.isArrayUnwrap;
        }
        calculateNonNullableVars(nonNullableVars, ast);

        for (var i = firstVar; i < ast.getVariables().size(); ++i) {
            var localVar = ast.getVariables().get(i);
            var inferredType = preciseTypes[i];
            WasmType type;
            if (i == 0 && compactMode) {
                type = WasmType.Reference.ANY;
            } else if (!inferredType.isArrayUnwrap || inferredType.valueType == null) {
                type = typeMapper.mapType(inferredType.valueType);
            } else {
                var arrayType = classInfoProvider.getClassInfo(inferredType.valueType).getArray();
                type = nonNullableVars[i] ? arrayType.getNonNullReference() : arrayType.getReference();
            }
            var wasmLocal = new WasmLocal(type, localVar.getName());
            function.add(wasmLocal);
        }

        addInitializerErase(method, function);
        var visitor = new WasmGCGenerationVisitor(getGenerationContext(), method.getReference(),
                function, firstVar, false, typeInference);
        visitor.setCompactMode(methodCompact);
        visitor.generate(ast.getBody(), function.getBody());
    }

    private void eliminateMultipleNullConstantUsages(Program program) {
        var nulls = new boolean[program.variableCount()];
        var usageCount = new int[program.variableCount()];
        var locations = new TextLocation[program.variableCount()];
        var usageExtractor = new UsageExtractor();
        for (var block : program.getBasicBlocks()) {
            for (var insn : block) {
                insn.acceptVisitor(usageExtractor);
                var usedVars = usageExtractor.getUsedVariables();
                if (usedVars != null) {
                    for (var usedVar : usedVars) {
                        usageCount[usedVar.getIndex()]++;
                    }
                }
                if (insn instanceof NullConstantInstruction) {
                    var index  = ((NullConstantInstruction) insn).getReceiver().getIndex();
                    nulls[index] = true;
                    locations[index] = insn.getLocation();
                }
            }
            for (var phi : block.getPhis()) {
                for (var input : phi.getIncomings()) {
                    usageCount[input.getValue().getIndex()]++;
                }
            }
        }

        for (var i = 0; i < program.variableCount(); ++i) {
            if (nulls[i]) {
                if (usageCount[i] <= 1) {
                    nulls[i] = false;
                }
            } else {
                usageCount[i] = 0;
            }
        }

        var mapFunction = new Function<Variable, Variable>() {
            Instruction instruction;

            @Override
            public Variable apply(Variable variable) {
                if (variable.getIndex() >= nulls.length || !nulls[variable.getIndex()]
                        || usageCount[variable.getIndex()]++ == 0) {
                    return variable;
                }
                var nullConstant = new NullConstantInstruction();
                nullConstant.setReceiver(program.createVariable());
                nullConstant.setLocation(locations[variable.getIndex()]);
                instruction.insertPrevious(nullConstant);
                return nullConstant.getReceiver();
            }
        };
        var mapper = new InstructionVariableMapper(mapFunction);
        for (var block : program.getBasicBlocks()) {
            for (var insn : block) {
                mapFunction.instruction = insn;
                insn.acceptVisitor(mapper);
            }
            for (var phi : block.getPhis()) {
                for (var input : phi.getIncomings()) {
                    var index = input.getValue().getIndex();
                    if (index < nulls.length && nulls[index] && usageCount[index]++ > 0) {
                        var nullConstant = new NullConstantInstruction();
                        nullConstant.setReceiver(program.createVariable());
                        nullConstant.setLocation(locations[index]);
                        input.setValue(nullConstant.getReceiver());
                        input.getSource().getLastInstruction().insertPrevious(nullConstant);
                    }
                }
            }
        }
    }

    private void calculateNonNullableVars(boolean[] nonNullVars, RegularMethodNode ast) {
        var calculator = new NonNullVarsCalculator(nonNullVars);
        ast.getBody().acceptVisitor(calculator);
    }

    private void generateNativeMethodBody(MethodHolder method, WasmFunction function) {
        var importAnnot = method.getAnnotations().get(Import.class.getName());
        if (importAnnot == null) {
            diagnostics.error(new CallLocation(method.getReference()), "Method is not annotated with {{c0}}",
                    Import.class.getName());
            return;
        }

        function.setImportName(importAnnot.getValue("name").getString());
        var moduleName = importAnnot.getValue("module");
        function.setImportModule(moduleName != null ? moduleName.getString() : "teavm");
    }

    private void addInitializerErase(MethodReader method, WasmFunction function) {
        if (method.hasModifier(ElementModifier.STATIC) && method.getName().equals("<clinit>")
                && method.parameterCount() == 0 && classInitInfo.isDynamicInitializer(method.getOwnerName())) {
            var classInfo = classInfoProvider.getClassInfo(method.getOwnerName());
            var erase = new WasmSetGlobal(classInfo.getInitializerPointer(),
                    new WasmFunctionReference(getDummyInitializer()));
            function.getBody().add(erase);
            if (classInfoProvider.getClassInitializerOffset() >= 0) {
                function.getBody().add(new WasmStructSet(
                        standardClasses.classClass().getStructure(),
                        new WasmGetGlobal(classInfo.getPointer()),
                        classInfoProvider.getClassInitializerOffset(),
                        new WasmFunctionReference(getDummyInitializer())
                ));
            }
        }
    }

    private Decompiler getDecompiler() {
        if (decompiler == null) {
            decompiler = new Decompiler(classes, Set.of(), friendlyToDebugger);
        }
        return decompiler;
    }

    public WasmGCGenerationContext getGenerationContext() {
        if (context == null) {
            context = new WasmGCGenerationContext(
                    module,
                    virtualTables,
                    typeMapper,
                    functionTypes,
                    classes,
                    classLoader,
                    hierarchy,
                    this,
                    supertypeFunctions,
                    classInfoProvider,
                    standardClasses,
                    strings,
                    customGenerators,
                    intrinsics,
                    names,
                    strict,
                    entryPoint,
                    initializerContributors,
                    diagnostics,
                    classInitInfo,
                    dependency
            );
        }
        return context;
    }

    public WasmFunction getDummyInitializer() {
        if (dummyInitializer == null) {
            dummyInitializer = new WasmFunction(functionTypes.of(null));
            dummyInitializer.setName(names.topLevel("teavm@dummyInitializer"));
            dummyInitializer.setReferenced(true);
            module.functions.add(dummyInitializer);
        }
        return dummyInitializer;
    }

    private WasmGCCustomGeneratorContext customGeneratorContext = new WasmGCCustomGeneratorContext() {
        @Override
        public ClassLoader classLoader() {
            return classLoader;
        }

        @Override
        public ListableClassReaderSource classes() {
            return classes;
        }

        @Override
        public WasmModule module() {
            return module;
        }

        @Override
        public WasmFunctionTypes functionTypes() {
            return functionTypes;
        }

        @Override
        public WasmGCTypeMapper typeMapper() {
            return typeMapper;
        }

        @Override
        public WasmGCClassInfoProvider classInfoProvider() {
            return classInfoProvider;
        }

        @Override
        public WasmGCNameProvider names() {
            return names;
        }

        @Override
        public WasmTag exceptionTag() {
            return context.getExceptionTag();
        }

        @Override
        public BaseWasmFunctionRepository functions() {
            return WasmGCMethodGenerator.this;
        }

        @Override
        public Diagnostics diagnostics() {
            return diagnostics;
        }

        @Override
        public WasmGCStringProvider strings() {
            return context.strings();
        }

        @Override
        public String entryPoint() {
            return context.entryPoint();
        }

        @Override
        public boolean isCompactMode() {
            return compactMode;
        }

        @Override
        public void addToInitializer(Consumer<WasmFunction> initializerContributor) {
            context.addToInitializer(initializerContributor);
        }
    };
}
