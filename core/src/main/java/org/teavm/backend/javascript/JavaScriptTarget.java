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
package org.teavm.backend.javascript;

import com.carrotsearch.hppc.ObjectIntHashMap;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.teavm.ast.ControlFlowEntry;
import org.teavm.backend.javascript.codegen.DefaultAliasProvider;
import org.teavm.backend.javascript.codegen.DefaultNamingStrategy;
import org.teavm.backend.javascript.codegen.MinifyingAliasProvider;
import org.teavm.backend.javascript.codegen.OutputSourceWriter;
import org.teavm.backend.javascript.codegen.OutputSourceWriterBuilder;
import org.teavm.backend.javascript.codegen.RememberedSource;
import org.teavm.backend.javascript.codegen.RememberingSourceWriter;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.intrinsics.ref.ReferenceQueueGenerator;
import org.teavm.backend.javascript.intrinsics.ref.ReferenceQueueTransformer;
import org.teavm.backend.javascript.intrinsics.ref.WeakReferenceDependencyListener;
import org.teavm.backend.javascript.intrinsics.ref.WeakReferenceGenerator;
import org.teavm.backend.javascript.intrinsics.ref.WeakReferenceTransformer;
import org.teavm.backend.javascript.rendering.NameFrequencyEstimator;
import org.teavm.backend.javascript.rendering.Renderer;
import org.teavm.backend.javascript.rendering.RenderingContext;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.backend.javascript.rendering.RuntimeRenderer;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.VirtualMethodContributor;
import org.teavm.backend.javascript.spi.VirtualMethodContributorContext;
import org.teavm.backend.javascript.templating.JavaScriptTemplateFactory;
import org.teavm.cache.EmptyMethodNodeCache;
import org.teavm.cache.MethodNodeCache;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DummyDebugInformationEmitter;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.DependencyType;
import org.teavm.dependency.MethodDependency;
import org.teavm.interop.PlatformMarker;
import org.teavm.interop.Platforms;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.transformation.BoundCheckInsertion;
import org.teavm.model.transformation.NullCheckFilter;
import org.teavm.model.transformation.NullCheckInsertion;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.RenderingException;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.RendererListener;
import org.teavm.vm.spi.TeaVMHostExtension;

public class JavaScriptTarget implements TeaVMTarget, TeaVMJavaScriptHost {
    private static final NumberFormat STATS_NUM_FORMAT = new DecimalFormat("#,##0");
    private static final NumberFormat STATS_PERCENT_FORMAT = new DecimalFormat("0.000 %");
    private static final MethodReference CURRENT_THREAD = new MethodReference(Thread.class,
            "currentThread", Thread.class);

    private TeaVMTargetController controller;
    private boolean obfuscated = true;
    private boolean stackTraceIncluded;
    private final Map<MethodReference, Generator> methodGenerators = new HashMap<>();
    private final Map<MethodReference, Injector> methodInjectors = new HashMap<>();
    private final List<Function<ProviderContext, Generator>> generatorProviders = new ArrayList<>();
    private final List<Function<ProviderContext, Injector>> injectorProviders = new ArrayList<>();
    private final List<RendererListener> rendererListeners = new ArrayList<>();
    private DebugInformationEmitter debugEmitter;
    private MethodNodeCache astCache = EmptyMethodNodeCache.INSTANCE;
    private final Set<MethodReference> asyncMethods = new HashSet<>();
    private List<VirtualMethodContributor> customVirtualMethods = new ArrayList<>();
    private boolean strict;
    private BoundCheckInsertion boundCheckInsertion = new BoundCheckInsertion();
    private NullCheckInsertion nullCheckInsertion = new NullCheckInsertion(NullCheckFilter.EMPTY);
    private final Map<String, String> importedModules = new LinkedHashMap<>();
    private JavaScriptTemplateFactory templateFactory;

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        return List.of(
                new WeakReferenceTransformer(),
                new ReferenceQueueTransformer()
        );
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        return Collections.emptyList();
    }

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;

        templateFactory = new JavaScriptTemplateFactory(controller.getClassLoader(),
                controller.getDependencyInfo().getClassSource());

        var weakRefGenerator = new WeakReferenceGenerator(templateFactory);
        methodGenerators.put(new MethodReference(WeakReference.class, "<init>", Object.class,
                ReferenceQueue.class, void.class), weakRefGenerator);
        methodGenerators.put(new MethodReference(WeakReference.class, "get", Object.class), weakRefGenerator);
        methodGenerators.put(new MethodReference(WeakReference.class, "clear", void.class), weakRefGenerator);

        var refQueueGenerator = new ReferenceQueueGenerator();
        methodGenerators.put(new MethodReference(ReferenceQueue.class, "<init>", void.class), refQueueGenerator);
        methodGenerators.put(new MethodReference(ReferenceQueue.class, "poll", Reference.class), refQueueGenerator);
    }

    @Override
    public void add(RendererListener listener) {
        rendererListeners.add(listener);
    }

    @Override
    public void add(MethodReference methodRef, Generator generator) {
        methodGenerators.put(methodRef, generator);
    }

    @Override
    public void add(MethodReference methodRef, Injector injector) {
        methodInjectors.put(methodRef, injector);
    }

    @Override
    public void addGeneratorProvider(Function<ProviderContext, Generator> provider) {
        generatorProviders.add(provider);
    }

    @Override
    public void addInjectorProvider(Function<ProviderContext, Injector> provider) {
        injectorProviders.add(provider);
    }

    /**
     * Specifies whether this TeaVM instance uses obfuscation when generating the JavaScript code.
     *
     * @param obfuscated whether TeaVM should obfuscate code.
     */
    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    public MethodNodeCache getAstCache() {
        return astCache;
    }

    public void setAstCache(MethodNodeCache methodAstCache) {
        this.astCache = methodAstCache;
    }

    public DebugInformationEmitter getDebugEmitter() {
        return debugEmitter;
    }

    public void setDebugEmitter(DebugInformationEmitter debugEmitter) {
        this.debugEmitter = debugEmitter;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public boolean requiresRegisterAllocation() {
        return true;
    }

    public void setStackTraceIncluded(boolean stackTraceIncluded) {
        this.stackTraceIncluded = stackTraceIncluded;
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return Collections.singletonList(this);
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        MethodDependency dep;

        DependencyType stringType = dependencyAnalyzer.getType("java.lang.String");

        dep = dependencyAnalyzer.linkMethod(new MethodReference(Class.class.getName(), "getClass",
                ValueType.object("org.teavm.platform.PlatformClass"), ValueType.parse(Class.class)));
        dep.getVariable(0).propagate(dependencyAnalyzer.getType("org.teavm.platform.PlatformClass"));
        dep.getResult().propagate(dependencyAnalyzer.getType("java.lang.Class"));
        dep.use();

        dep = dependencyAnalyzer.linkMethod(new MethodReference(String.class, "<init>", Object.class, void.class));
        dep.getVariable(0).propagate(stringType);
        dep.use();

        dependencyAnalyzer.linkField(new FieldReference(String.class.getName(), "characters"));

        dep = dependencyAnalyzer.linkMethod(new MethodReference(Object.class, "toString", String.class));
        dep.getVariable(0).propagate(dependencyAnalyzer.getType("java.lang.Object"));
        dep.use();

        var exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(
                RuntimeException.class, "<init>", String.class, void.class));
        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(RuntimeException.class.getName()));
        exceptionCons.getVariable(1).propagate(stringType);
        exceptionCons.use();

        if (strict) {
            exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(
                    ArrayIndexOutOfBoundsException.class, "<init>", void.class));
            exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(
                    ArrayIndexOutOfBoundsException.class.getName()));
            exceptionCons.use();

            exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(
                    NullPointerException.class, "<init>", void.class));
            exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(NullPointerException.class.getName()));
            exceptionCons.use();

            exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(
                    ClassCastException.class, "<init>", void.class));
            exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(ClassCastException.class.getName()));
            exceptionCons.use();
        }

        if (stackTraceIncluded) {
            includeStackTraceMethods(dependencyAnalyzer);
        }

        dependencyAnalyzer.linkMethod(new MethodReference(Throwable.class, "getMessage", String.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Throwable.class, "getCause", Throwable.class)).use();

        dependencyAnalyzer.addDependencyListener(new AbstractDependencyListener() {
            @Override
            public void methodReached(DependencyAgent agent, MethodDependency method) {
                if (method.getReference().equals(CURRENT_THREAD)) {
                    method.use();
                    agent.linkMethod(new MethodReference(Thread.class, "setCurrentThread", Thread.class, void.class))
                            .use();
                }
            }
        });

        dependencyAnalyzer.addDependencyListener(new WeakReferenceDependencyListener());
    }

    public static void includeStackTraceMethods(DependencyAnalyzer dependencyAnalyzer) {
        MethodDependency dep;

        DependencyType stringType = dependencyAnalyzer.getType("java.lang.String");

        dep = dependencyAnalyzer.linkMethod(new MethodReference(
                StackTraceElement.class, "<init>", String.class, String.class, String.class,
                int.class, void.class));
        dep.getVariable(0).propagate(dependencyAnalyzer.getType(StackTraceElement.class.getName()));
        dep.getVariable(1).propagate(stringType);
        dep.getVariable(2).propagate(stringType);
        dep.getVariable(3).propagate(stringType);
        dep.use();

        dep = dependencyAnalyzer.linkMethod(new MethodReference(
                Throwable.class, "setStackTrace", StackTraceElement[].class, void.class));
        dep.getVariable(0).propagate(dependencyAnalyzer.getType(Throwable.class.getName()));
        dep.getVariable(1).propagate(dependencyAnalyzer.getType("[Ljava/lang/StackTraceElement;"));
        dep.getVariable(1).getArrayItem().propagate(dependencyAnalyzer.getType(StackTraceElement.class.getName()));
        dep.use();
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget target, String outputName) {
        try (OutputStream output = target.createResource(outputName);
                Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            emit(classes, writer, target);
        } catch (IOException e) {
            throw new RenderingException(e);
        }
    }

    @Override
    public void beforeInlining(Program program, MethodReader method) {
        if (strict) {
            nullCheckInsertion.transformProgram(program, method.getReference());
        }
    }

    @Override
    public void beforeOptimizations(Program program, MethodReader method) {
        if (strict) {
            boundCheckInsertion.transformProgram(program, method.getReference());
        }
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method) {
    }

    private void emit(ListableClassHolderSource classes, Writer writer, BuildTarget target) {
        var aliasProvider = obfuscated ? new MinifyingAliasProvider() : new DefaultAliasProvider();
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, controller.getUnprocessedClassSource());
        DebugInformationEmitter debugEmitterToUse = debugEmitter;
        if (debugEmitterToUse == null) {
            debugEmitterToUse = new DummyDebugInformationEmitter();
        }

        var virtualMethodContributorContext = new VirtualMethodContributorContextImpl(classes);
        RenderingContext renderingContext = new RenderingContext(debugEmitterToUse,
                controller.getUnprocessedClassSource(), classes,
                controller.getClassLoader(), controller.getServices(), controller.getProperties(), naming,
                controller.getDependencyInfo(), m -> isVirtual(virtualMethodContributorContext, m),
                controller.getClassInitializerInfo(), strict
        ) {
            @Override
            public String importModule(String name) {
                return JavaScriptTarget.this.importModule(name);
            }
        };
        renderingContext.setMinifying(obfuscated);

        if (controller.wasCancelled()) {
            return;
        }

        var builder = new OutputSourceWriterBuilder(naming);
        builder.setMinified(obfuscated);

        for (var className : classes.getClassNames()) {
            var cls = classes.get(className);
            for (var method : cls.getMethods()) {
                preprocessNativeMethod(method);
            }
        }
        for (var entry : methodInjectors.entrySet()) {
            renderingContext.addInjector(entry.getKey(), entry.getValue());
        }

        var rememberingWriter = new RememberingSourceWriter(debugEmitter != null);
        var renderer = new Renderer(rememberingWriter, asyncMethods, renderingContext, controller.getDiagnostics(),
                methodGenerators, astCache, controller.getCacheStatus(), templateFactory);
        renderer.setProperties(controller.getProperties());
        renderer.setProgressConsumer(controller::reportProgress);

        for (var listener : rendererListeners) {
            listener.begin(renderer, target);
        }
        if (!renderer.render(classes, controller.isFriendlyToDebugger())) {
            return;
        }
        var declarations = rememberingWriter.save();
        rememberingWriter.clear();

        renderer.renderStringPool();
        renderer.renderStringConstants();
        renderer.renderCompatibilityStubs();
        for (var entry : controller.getEntryPoints().entrySet()) {
            rememberingWriter.appendFunction("$rt_exports").append(".").append(entry.getKey()).ws().append("=").ws();
            var ref = entry.getValue().getMethod();
            rememberingWriter.appendFunction("$rt_mainStarter").append("(").appendMethodBody(ref);
            rememberingWriter.append(");").newLine();
            rememberingWriter.appendFunction("$rt_exports").append(".").append(entry.getKey()).append(".")
                    .append("javaException").ws().append("=").ws().appendFunction("$rt_javaException")
                    .append(";").newLine();
        }

        for (var listener : rendererListeners) {
            listener.complete();
        }
        var epilogue = rememberingWriter.save();
        rememberingWriter.clear();

        if (renderingContext.isMinifying()) {
            var frequencyEstimator = new NameFrequencyEstimator();
            declarations.replay(frequencyEstimator, RememberedSource.FILTER_REF);
            epilogue.replay(frequencyEstimator, RememberedSource.FILTER_REF);
            frequencyEstimator.apply(naming);
        }

        var sourceWriter = builder.build(writer);
        sourceWriter.setDebugInformationEmitter(debugEmitterToUse);
        printWrapperStart(sourceWriter);

        int start = sourceWriter.getOffset();

        RuntimeRenderer runtimeRenderer = new RuntimeRenderer(classes, sourceWriter,
                controller.getClassInitializerInfo());
        runtimeRenderer.prepareAstParts(renderer.isThreadLibraryUsed());
        declarations.replay(runtimeRenderer.sink, RememberedSource.FILTER_REF);
        epilogue.replay(runtimeRenderer.sink, RememberedSource.FILTER_REF);
        runtimeRenderer.removeUnusedParts();
        runtimeRenderer.renderRuntime();
        declarations.write(sourceWriter, 0);
        runtimeRenderer.renderEpilogue();
        epilogue.write(sourceWriter, 0);

        printWrapperEnd(sourceWriter);

        int totalSize = sourceWriter.getOffset() - start;
        printStats(sourceWriter, totalSize);
    }

    private void printWrapperStart(SourceWriter writer) {
        writer.append("\"use strict\";").newLine();
        printUmdStart(writer);
        writer.append("function(").appendFunction("$rt_exports");
        for (var moduleName : importedModules.values()) {
            writer.append(",").ws().appendFunction(moduleName);
        }
        writer.append(")").appendBlockStart();
    }

    private String importModule(String name) {
        return importedModules.computeIfAbsent(name, n -> "$rt_imported_" + importedModules.size());
    }

    private void printUmdStart(SourceWriter writer) {
        writer.append("(function(module)").appendBlockStart();
        writer.appendIf().append("typeof define").ws().append("===").ws().append("'function'")
                .ws().append("&&").ws().append("define.amd)").appendBlockStart();
        writer.append("define(['exports'");
        for (var moduleName : importedModules.keySet()) {
            writer.append(',').ws().append('"').append(RenderingUtil.escapeString(moduleName)).append('"');
        }
        writer.append("],").ws().append("function(exports");
        for (var moduleAlias : importedModules.values()) {
            writer.append(',').ws().appendFunction(moduleAlias);
        }
        writer.append(")").ws().appendBlockStart();
        writer.append("module(exports");
        for (var moduleAlias : importedModules.values()) {
            writer.append(',').ws().appendFunction(moduleAlias);
        }
        writer.append(");").softNewLine();
        writer.outdent().append("});").softNewLine();

        writer.appendElseIf().append("typeof exports").ws()
                .append("===").ws().append("'object'").ws().append("&&").ws()
                .append("exports").ws().append("!==").ws().append("null").ws().append("&&").ws()
                .append("typeof exports.nodeName").ws().append("!==").ws().append("'string')").appendBlockStart();
        writer.append("module(exports");
        for (var moduleName : importedModules.keySet()) {
            writer.append(',').ws().append("require(\"").append(RenderingUtil.escapeString(moduleName)).append("\")");
        }
        writer.append(");").softNewLine();

        writer.appendElse();
        writer.append("module(");
        writer.outdent().append("typeof self").ws().append("!==").ws().append("'undefined'")
                .ws().append("?").ws().append("self")
                .ws().append(":").ws().append("this");

        writer.append(");").softNewLine();
        writer.appendBlockEnd();
        writer.outdent().append("}(");
    }

    private void printWrapperEnd(SourceWriter writer) {
        writer.outdent().append("}));").newLine();
    }

    private void printStats(OutputSourceWriter writer, int totalSize) {
        if (!Boolean.parseBoolean(System.getProperty("teavm.js.stats", "false"))) {
            return;
        }

        System.out.println("Total output size: " + STATS_NUM_FORMAT.format(totalSize));
        System.out.println("Metadata size: " + getSizeWithPercentage(
                writer.getSectionSize(Renderer.SECTION_METADATA), totalSize));
        System.out.println("String pool size: " + getSizeWithPercentage(
                writer.getSectionSize(Renderer.SECTION_STRING_POOL), totalSize));

        var packageSizeMap = new ObjectIntHashMap<String>();
        for (String className : writer.getClassesInStats()) {
            String packageName = className.substring(0, className.lastIndexOf('.') + 1);
            int classSize = writer.getClassSize(className);
            packageSizeMap.put(packageName, packageSizeMap.getOrDefault(packageName, 0) + classSize);
        }

        String[] packageNames = packageSizeMap.keys().toArray(String.class);
        Arrays.sort(packageNames, Comparator.comparing(p -> -packageSizeMap.getOrDefault(p, 0)));
        for (String packageName : packageNames) {
            System.out.println("Package '" + packageName + "' size: "
                    + getSizeWithPercentage(packageSizeMap.get(packageName), totalSize));
        }
    }

    private String getSizeWithPercentage(int size, int totalSize) {
        return STATS_NUM_FORMAT.format(size) + " (" + STATS_PERCENT_FORMAT.format((double) size / totalSize) + ")";
    }

    private void preprocessNativeMethod(MethodHolder method) {
        if (!method.getModifiers().contains(ElementModifier.NATIVE)
                || methodGenerators.get(method.getReference()) != null
                || methodInjectors.get(method.getReference()) != null) {
            return;
        }

        boolean found = false;
        ProviderContext context = new ProviderContextImpl(method.getReference());
        for (var provider : generatorProviders) {
            Generator generator = provider.apply(context);
            if (generator != null) {
                methodGenerators.put(method.getReference(), generator);
                found = true;
                break;
            }
        }
        for (var provider : injectorProviders) {
            Injector injector = provider.apply(context);
            if (injector != null) {
                methodInjectors.put(method.getReference(), injector);
                found = true;
                break;
            }
        }

        if (found) {
            return;
        }

        if (!isBootstrap()) {
            if (method.getAnnotations().get(GeneratedBy.class.getName()) != null
                    || method.getAnnotations().get(InjectedBy.class.getName()) != null) {
                return;
            }
        }
        method.getModifiers().remove(ElementModifier.NATIVE);

        Program program = new Program();
        method.setProgram(program);

        for (int i = 0; i <= method.parameterCount(); ++i) {
            program.createVariable();
        }

        BasicBlock block = program.createBasicBlock();
        Variable exceptionVar = program.createVariable();
        ConstructInstruction newExceptionInsn = new ConstructInstruction();
        newExceptionInsn.setType(NoSuchMethodError.class.getName());
        newExceptionInsn.setReceiver(exceptionVar);
        block.add(newExceptionInsn);

        Variable constVar = program.createVariable();
        StringConstantInstruction constInsn = new StringConstantInstruction();
        constInsn.setConstant("Native method implementation not found: " + method.getReference());
        constInsn.setReceiver(constVar);
        block.add(constInsn);

        InvokeInstruction initExceptionInsn = new InvokeInstruction();
        initExceptionInsn.setInstance(exceptionVar);
        initExceptionInsn.setMethod(new MethodReference(NoSuchMethodError.class, "<init>", String.class, void.class));
        initExceptionInsn.setType(InvocationType.SPECIAL);
        initExceptionInsn.setArguments(constVar);
        block.add(initExceptionInsn);

        RaiseInstruction raiseInsn = new RaiseInstruction();
        raiseInsn.setException(exceptionVar);
        block.add(raiseInsn);

        controller.getDiagnostics().error(new CallLocation(method.getReference()),
                "Native method {{m0}} has no implementation",  method.getReference());
    }

    class ProviderContextImpl implements ProviderContext {
        private MethodReference method;

        ProviderContextImpl(MethodReference method) {
            this.method = method;
        }

        @Override
        public MethodReference getMethod() {
            return method;
        }

        @Override
        public ClassReaderSource getClassSource() {
            return controller.getUnprocessedClassSource();
        }

        @Override
        public <T> T getService(Class<T> type) {
            return controller.getServices().getService(type);
        }
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }

    private void emitCFG(DebugInformationEmitter emitter, ControlFlowEntry[] cfg) {
        for (ControlFlowEntry entry : cfg) {
            SourceLocation location = map(entry.from);
            SourceLocation[] successors = new SourceLocation[entry.to.length];
            for (int i = 0; i < entry.to.length; ++i) {
                successors[i] = map(entry.to[i]);
            }
            emitter.addSuccessors(location, successors);
        }
    }

    private static SourceLocation map(TextLocation location) {
        if (location == null || location.isEmpty()) {
            return null;
        }
        return new SourceLocation(location.getFileName(), location.getLine());
    }

    @Override
    public String[] getPlatformTags() {
        return new String[] { Platforms.JAVASCRIPT };
    }

    @Override
    public void addVirtualMethods(VirtualMethodContributor virtualMethods) {
        customVirtualMethods.add(virtualMethods);
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    private boolean isVirtual(VirtualMethodContributorContext context, MethodReference method) {
        if (controller.isVirtual(method)) {
            return true;
        }
        for (VirtualMethodContributor predicate : customVirtualMethods) {
            if (predicate.isVirtual(context, method)) {
                return true;
            }
        }
        return false;
    }

    static class VirtualMethodContributorContextImpl implements VirtualMethodContributorContext {
        private ClassReaderSource classSource;

        VirtualMethodContributorContextImpl(ClassReaderSource classSource) {
            this.classSource = classSource;
        }

        @Override
        public ClassReaderSource getClassSource() {
            return classSource;
        }
    }
}
