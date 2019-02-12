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
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.teavm.ast.ClassNode;
import org.teavm.ast.MethodNode;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.Statement;
import org.teavm.ast.analysis.LocationGraphBuilder;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.javascript.codegen.AliasProvider;
import org.teavm.backend.javascript.codegen.DefaultAliasProvider;
import org.teavm.backend.javascript.codegen.DefaultNamingStrategy;
import org.teavm.backend.javascript.codegen.MinifyingAliasProvider;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.codegen.SourceWriterBuilder;
import org.teavm.backend.javascript.rendering.Renderer;
import org.teavm.backend.javascript.rendering.RenderingContext;
import org.teavm.backend.javascript.rendering.RuntimeRenderer;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.VirtualMethodContributor;
import org.teavm.backend.javascript.spi.VirtualMethodContributorContext;
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
import org.teavm.interop.PlatformMarkers;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
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
import org.teavm.model.transformation.ClassInitializerInsertionTransformer;
import org.teavm.model.util.AsyncMethodFinder;
import org.teavm.model.util.ProgramUtils;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.RenderingException;
import org.teavm.vm.TeaVMEntryPoint;
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
    private boolean minifying = true;
    private boolean stackTraceIncluded;
    private final Map<MethodReference, Generator> methodGenerators = new HashMap<>();
    private final Map<MethodReference, Injector> methodInjectors = new HashMap<>();
    private final List<Function<ProviderContext, Generator>> generatorProviders = new ArrayList<>();
    private final List<Function<ProviderContext, Injector>> injectorProviders = new ArrayList<>();
    private final List<RendererListener> rendererListeners = new ArrayList<>();
    private DebugInformationEmitter debugEmitter;
    private MethodNodeCache astCache = EmptyMethodNodeCache.INSTANCE;
    private final Set<MethodReference> asyncMethods = new HashSet<>();
    private final Set<MethodReference> asyncFamilyMethods = new HashSet<>();
    private ClassInitializerInsertionTransformer clinitInsertionTransformer;
    private List<VirtualMethodContributor> customVirtualMethods = new ArrayList<>();
    private boolean classScoped;
    @Override
    public List<ClassHolderTransformer> getTransformers() {
        return Collections.emptyList();
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        return Collections.emptyList();
    }

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;
        clinitInsertionTransformer = new ClassInitializerInsertionTransformer(controller.getUnprocessedClassSource());
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
     * Reports whether this TeaVM instance uses obfuscation when generating the JavaScript code.
     *
     * @see #setMinifying(boolean)
     * @return whether TeaVM produces obfuscated code.
     */
    public boolean isMinifying() {
        return minifying;
    }

    /**
     * Specifies whether this TeaVM instance uses obfuscation when generating the JavaScript code.
     *
     * @see #isMinifying()
     * @param minifying whether TeaVM should obfuscate code.
     */
    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
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

    public void setClassScoped(boolean classScoped) {
        this.classScoped = classScoped;
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

        dep = dependencyAnalyzer.linkMethod(new MethodReference(String.class, "<init>", char[].class, void.class));
        dep.getVariable(0).propagate(stringType);
        dep.getVariable(1).propagate(dependencyAnalyzer.getType("[C"));
        dep.use();

        dependencyAnalyzer.linkField(new FieldReference(String.class.getName(), "characters"));

        dependencyAnalyzer.linkMethod(new MethodReference(Object.class, "clone", Object.class));
        MethodDependency exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(
                NoClassDefFoundError.class, "<init>", String.class, void.class));

        dep = dependencyAnalyzer.linkMethod(new MethodReference(Object.class, "toString", String.class));
        dep.getVariable(0).propagate(dependencyAnalyzer.getType("java.lang.Object"));
        dep.use();

        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(NoClassDefFoundError.class.getName()));
        exceptionCons.getVariable(1).propagate(stringType);
        exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(NoSuchFieldError.class, "<init>",
                String.class, void.class));
        exceptionCons.use();
        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(NoSuchFieldError.class.getName()));
        exceptionCons.getVariable(1).propagate(stringType);
        exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(NoSuchMethodError.class, "<init>",
                String.class, void.class));
        exceptionCons.use();
        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(NoSuchMethodError.class.getName()));
        exceptionCons.getVariable(1).propagate(stringType);

        exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(
                RuntimeException.class, "<init>", String.class, void.class));
        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(RuntimeException.class.getName()));
        exceptionCons.getVariable(1).propagate(stringType);
        exceptionCons.use();

        if (stackTraceIncluded) {
            includeStackTraceMethods(dependencyAnalyzer);
        }

        dependencyAnalyzer.addDependencyListener(new AbstractDependencyListener() {
            @Override
            public void methodReached(DependencyAgent agent, MethodDependency method) {
                if (method.getReference().equals(CURRENT_THREAD)) {
                    method.use();
                }
                agent.linkMethod(new MethodReference(Thread.class, "setCurrentThread", Thread.class, void.class))
                        .use();
            }
        });
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
    public void beforeOptimizations(Program program, MethodReader method) {
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method) {
        clinitInsertionTransformer.apply(method, program);
    }

    private void emit(ListableClassHolderSource classes, Writer writer, BuildTarget target) {
        List<ClassNode> clsNodes = modelToAst(classes);
        if (controller.wasCancelled()) {
            return;
        }

        AliasProvider aliasProvider = minifying ? new MinifyingAliasProvider() : new DefaultAliasProvider();
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, controller.getUnprocessedClassSource());
        SourceWriterBuilder builder = new SourceWriterBuilder(naming);
        builder.setMinified(minifying);
        builder.setClassScoped(classScoped);
        SourceWriter sourceWriter = builder.build(writer);

        DebugInformationEmitter debugEmitterToUse = debugEmitter;
        if (debugEmitterToUse == null) {
            debugEmitterToUse = new DummyDebugInformationEmitter();
        }
        VirtualMethodContributorContext virtualMethodContributorContext = new VirtualMethodContributorContextImpl(
                classes);
        RenderingContext renderingContext = new RenderingContext(debugEmitterToUse,
                controller.getUnprocessedClassSource(), classes,
                controller.getClassLoader(), controller.getServices(), controller.getProperties(), naming,
                controller.getDependencyInfo(), m -> isVirtual(virtualMethodContributorContext, m));
        renderingContext.setMinifying(minifying);
        Renderer renderer = new Renderer(sourceWriter, asyncMethods, asyncFamilyMethods,
                controller.getDiagnostics(), renderingContext, classScoped);
        RuntimeRenderer runtimeRenderer = new RuntimeRenderer(classes, naming, sourceWriter);
        renderer.setProperties(controller.getProperties());
        renderer.setMinifying(minifying);
        renderer.setProgressConsumer(controller::reportProgress);
        if (debugEmitter != null) {
            for (ClassNode classNode : clsNodes) {
                ClassHolder cls = classes.get(classNode.getName());
                for (MethodNode methodNode : classNode.getMethods()) {
                    if (methodNode instanceof RegularMethodNode) {
                        emitCFG(debugEmitter, ((RegularMethodNode) methodNode).getBody());
                    } else {
                        MethodHolder method = cls.getMethod(methodNode.getReference().getDescriptor());
                        if (method != null && method.getProgram() != null) {
                            emitCFG(debugEmitter, method.getProgram());
                        }
                    }
                }
                if (controller.wasCancelled()) {
                    return;
                }
            }
            renderer.setDebugEmitter(debugEmitter);
        }
        renderer.getDebugEmitter().setLocationProvider(sourceWriter);
        for (Map.Entry<MethodReference, Injector> entry : methodInjectors.entrySet()) {
            renderingContext.addInjector(entry.getKey(), entry.getValue());
        }
        try {
            printWrapperStart(sourceWriter);

            for (RendererListener listener : rendererListeners) {
                listener.begin(renderer, target);
            }
            int start = sourceWriter.getOffset();

            renderer.prepare(clsNodes);
            runtimeRenderer.renderRuntime();
            if (classScoped) {
                sourceWriter.append("var ").append(Renderer.CONTAINER_OBJECT).ws().append("=").ws()
                        .append("Object.create(null);").newLine();
            }
            if (!renderer.render(clsNodes)) {
                return;
            }
            renderer.renderStringPool();
            renderer.renderStringConstants();
            renderer.renderCompatibilityStubs();

            if (renderer.isLongLibraryUsed()) {
                runtimeRenderer.renderHandWrittenRuntime("long.js");
            }
            if (renderer.isThreadLibraryUsed()) {
                runtimeRenderer.renderHandWrittenRuntime("thread.js");
            } else {
                runtimeRenderer.renderHandWrittenRuntime("simpleThread.js");
            }

            for (Map.Entry<? extends String, ? extends TeaVMEntryPoint> entry
                    : controller.getEntryPoints().entrySet()) {
                sourceWriter.append("").append(entry.getKey()).ws().append("=").ws();
                MethodReference ref = entry.getValue().getMethod();
                sourceWriter.append("$rt_mainStarter(").appendMethodBody(ref);
                sourceWriter.append(");").newLine();
            }

            for (RendererListener listener : rendererListeners) {
                listener.complete();
            }

            printWrapperEnd(sourceWriter);

            int totalSize = sourceWriter.getOffset() - start;
            printStats(renderer, totalSize);
        } catch (IOException e) {
            throw new RenderingException("IO Error occured", e);
        }
    }

    private void printWrapperStart(SourceWriter writer) throws IOException {
        writer.append("\"use strict\";").newLine();
        for (String key : controller.getEntryPoints().keySet()) {
            writer.append("var ").append(key).append(";").softNewLine();
        }
        writer.append("(function()").ws().append("{").newLine();
    }

    private void printWrapperEnd(SourceWriter writer) throws IOException {
        writer.append("})();").newLine();
    }

    private void printStats(Renderer renderer, int totalSize) {
        if (!Boolean.parseBoolean(System.getProperty("teavm.js.stats", "false"))) {
            return;
        }

        System.out.println("Total output size: " + STATS_NUM_FORMAT.format(totalSize));
        System.out.println("Metadata size: " + getSizeWithPercentage(renderer.getMetadataSize(), totalSize));
        System.out.println("String pool size: " + getSizeWithPercentage(renderer.getStringPoolSize(), totalSize));

        ObjectIntMap<String> packageSizeMap = new ObjectIntHashMap<>();
        for (String className : renderer.getClassesInStats()) {
            String packageName = className.substring(0, className.lastIndexOf('.') + 1);
            int classSize = renderer.getClassSize(className);
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

    static class PackageNode {
        String name;
        Map<String, PackageNode> children = new HashMap<>();
    }

    private List<ClassNode> modelToAst(ListableClassHolderSource classes) {
        AsyncMethodFinder asyncFinder = new AsyncMethodFinder(controller.getDependencyInfo().getCallGraph(),
                controller.getDiagnostics());
        asyncFinder.find(classes);
        asyncMethods.addAll(asyncFinder.getAsyncMethods());
        asyncFamilyMethods.addAll(asyncFinder.getAsyncFamilyMethods());

        Decompiler decompiler = new Decompiler(classes, controller.getClassLoader(), controller.getCacheStatus(),
                asyncMethods, asyncFamilyMethods, controller.isFriendlyToDebugger(), false);
        decompiler.setRegularMethodCache(astCache);

        for (Map.Entry<MethodReference, Generator> entry : methodGenerators.entrySet()) {
            decompiler.addGenerator(entry.getKey(), entry.getValue());
        }
        for (MethodReference injectedMethod : methodInjectors.keySet()) {
            decompiler.addMethodToSkip(injectedMethod);
        }
        List<String> classOrder = decompiler.getClassOrdering(classes.getClassNames());
        List<ClassNode> classNodes = new ArrayList<>();
        for (String className : classOrder) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                preprocessNativeMethod(method, decompiler);
                if (controller.wasCancelled()) {
                    break;
                }
            }
            classNodes.add(decompiler.decompile(cls));
        }
        return classNodes;
    }

    private void preprocessNativeMethod(MethodHolder method, Decompiler decompiler) {
        if (!method.getModifiers().contains(ElementModifier.NATIVE)
                || methodGenerators.get(method.getReference()) != null
                || methodInjectors.get(method.getReference()) != null) {
            return;
        }

        boolean found = false;
        ProviderContext context = new ProviderContextImpl(method.getReference());
        for (Function<ProviderContext, Generator> provider : generatorProviders) {
            Generator generator = provider.apply(context);
            if (generator != null) {
                methodGenerators.put(method.getReference(), generator);
                decompiler.addGenerator(method.getReference(), generator);
                found = true;
                break;
            }
        }
        for (Function<ProviderContext, Injector> provider : injectorProviders) {
            Injector injector = provider.apply(context);
            if (injector != null) {
                methodInjectors.put(method.getReference(), injector);
                decompiler.addMethodToSkip(method.getReference());
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
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }

    private void emitCFG(DebugInformationEmitter emitter, Program program) {
        emitCFG(emitter, ProgramUtils.getLocationCFG(program));
    }

    private void emitCFG(DebugInformationEmitter emitter, Statement program) {
        emitCFG(emitter, LocationGraphBuilder.build(program));
    }

    private void emitCFG(DebugInformationEmitter emitter, Map<TextLocation, TextLocation[]> cfg) {
        for (Map.Entry<TextLocation, TextLocation[]> entry : cfg.entrySet()) {
            SourceLocation location = map(entry.getKey());
            SourceLocation[] successors = new SourceLocation[entry.getValue().length];
            for (int i = 0; i < entry.getValue().length; ++i) {
                successors[i] = map(entry.getValue()[i]);
            }
            emitter.addSuccessors(location, successors);
        }
    }

    private static SourceLocation map(TextLocation location) {
        if (location == null) {
            return null;
        }
        return new SourceLocation(location.getFileName(), location.getLine());
    }

    @Override
    public String[] getPlatformTags() {
        return new String[] { PlatformMarkers.JAVASCRIPT };
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
