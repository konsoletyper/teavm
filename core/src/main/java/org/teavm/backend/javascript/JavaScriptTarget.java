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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.teavm.ast.ClassNode;
import org.teavm.ast.cache.EmptyRegularMethodNodeCache;
import org.teavm.ast.cache.MethodNodeCache;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.javascript.codegen.AliasProvider;
import org.teavm.backend.javascript.codegen.DefaultAliasProvider;
import org.teavm.backend.javascript.codegen.DefaultNamingStrategy;
import org.teavm.backend.javascript.codegen.MinifyingAliasProvider;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.codegen.SourceWriterBuilder;
import org.teavm.backend.javascript.rendering.Renderer;
import org.teavm.backend.javascript.rendering.RenderingContext;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DummyDebugInformationEmitter;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.MethodDependency;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
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
    private TeaVMTargetController controller;
    private boolean minifying = true;
    private final Map<MethodReference, Generator> methodGenerators = new HashMap<>();
    private final Map<MethodReference, Injector> methodInjectors = new HashMap<>();
    private final List<Function<ProviderContext, Generator>> generatorProviders = new ArrayList<>();
    private final List<Function<ProviderContext, Injector>> injectorProviders = new ArrayList<>();
    private final List<RendererListener> rendererListeners = new ArrayList<>();
    private DebugInformationEmitter debugEmitter;
    private MethodNodeCache astCache = new EmptyRegularMethodNodeCache();
    private final Set<MethodReference> asyncMethods = new HashSet<>();
    private final Set<MethodReference> asyncFamilyMethods = new HashSet<>();
    private ClassInitializerInsertionTransformer clinitInsertionTransformer;

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

    @Override
    public boolean requiresRegisterAllocation() {
        return true;
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return Collections.singletonList(this);
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        dependencyAnalyzer.linkMethod(new MethodReference(Class.class.getName(), "getClass",
                ValueType.object("org.teavm.platform.PlatformClass"), ValueType.parse(Class.class)), null).use();
        dependencyAnalyzer.linkMethod(new MethodReference(String.class, "<init>", char[].class, void.class),
                null).use();
        dependencyAnalyzer.linkMethod(new MethodReference(String.class, "getChars", int.class, int.class, char[].class,
                int.class, void.class), null).use();

        MethodDependency internDep = dependencyAnalyzer.linkMethod(new MethodReference(String.class, "intern",
                String.class), null);
        internDep.getVariable(0).propagate(dependencyAnalyzer.getType("java.lang.String"));
        internDep.use();

        dependencyAnalyzer.linkMethod(new MethodReference(String.class, "length", int.class), null).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Object.class, "clone", Object.class), null).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Thread.class, "currentThread", Thread.class), null).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Thread.class, "getMainThread", Thread.class), null).use();
        dependencyAnalyzer.linkMethod(
                new MethodReference(Thread.class, "setCurrentThread", Thread.class, void.class), null).use();
        MethodDependency exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(
                NoClassDefFoundError.class, "<init>", String.class, void.class), null);

        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(NoClassDefFoundError.class.getName()));
        exceptionCons.getVariable(1).propagate(dependencyAnalyzer.getType("java.lang.String"));
        exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(NoSuchFieldError.class, "<init>",
                String.class, void.class), null);
        exceptionCons.use();
        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(NoSuchFieldError.class.getName()));
        exceptionCons.getVariable(1).propagate(dependencyAnalyzer.getType("java.lang.String"));
        exceptionCons = dependencyAnalyzer.linkMethod(new MethodReference(NoSuchMethodError.class, "<init>",
                String.class, void.class), null);
        exceptionCons.use();
        exceptionCons.getVariable(0).propagate(dependencyAnalyzer.getType(NoSuchMethodError.class.getName()));
        exceptionCons.getVariable(1).propagate(dependencyAnalyzer.getType("java.lang.String"));
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget target, String outputName) {
        try (OutputStream output = target.createResource(outputName);
                Writer writer = new OutputStreamWriter(output, "UTF-8")) {
            emit(classes, writer, target);
        } catch (IOException e) {
            throw new RenderingException(e);
        }
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method, ListableClassReaderSource classSource) {
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
        SourceWriter sourceWriter = builder.build(writer);

        DebugInformationEmitter debugEmitterToUse = debugEmitter;
        if (debugEmitterToUse == null) {
            debugEmitterToUse = new DummyDebugInformationEmitter();
        }
        RenderingContext renderingContext = new RenderingContext(debugEmitterToUse, classes,
                controller.getClassLoader(), controller.getServices(), controller.getProperties(), naming);
        renderingContext.setMinifying(minifying);
        Renderer renderer = new Renderer(sourceWriter, asyncMethods, asyncFamilyMethods,
                controller.getDiagnostics(), renderingContext);
        renderer.setProperties(controller.getProperties());
        renderer.setMinifying(minifying);
        if (debugEmitter != null) {
            for (String className : classes.getClassNames()) {
                ClassHolder cls = classes.get(className);
                for (MethodHolder method : cls.getMethods()) {
                    if (method.getProgram() != null) {
                        emitCFG(debugEmitter, method.getProgram());
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
            for (RendererListener listener : rendererListeners) {
                listener.begin(renderer, target);
            }
            sourceWriter.append("\"use strict\";").newLine();
            renderer.renderRuntime();
            renderer.render(clsNodes);
            renderer.renderStringPool();
            renderer.renderStringConstants();
            for (Map.Entry<String, TeaVMEntryPoint> entry : controller.getEntryPoints().entrySet()) {
                sourceWriter.append("var ").append(entry.getKey()).ws().append("=").ws();
                MethodReference ref = entry.getValue().getReference();
                sourceWriter.append(naming.getFullNameFor(ref));
                sourceWriter.append(";").newLine();
            }
            for (Map.Entry<String, String> entry : controller.getExportedClasses().entrySet()) {
                sourceWriter.append("var ").append(entry.getKey()).ws().append("=").ws()
                        .appendClass(entry.getValue()).append(";").newLine();
            }
            for (RendererListener listener : rendererListeners) {
                listener.complete();
            }
        } catch (IOException e) {
            throw new RenderingException("IO Error occured", e);
        }
    }

    private List<ClassNode> modelToAst(ListableClassHolderSource classes) {
        AsyncMethodFinder asyncFinder = new AsyncMethodFinder(controller.getDependencyInfo().getCallGraph(),
                controller.getDiagnostics());
        asyncFinder.find(classes);
        asyncMethods.addAll(asyncFinder.getAsyncMethods());
        asyncFamilyMethods.addAll(asyncFinder.getAsyncFamilyMethods());

        Decompiler decompiler = new Decompiler(classes, controller.getClassLoader(), asyncMethods, asyncFamilyMethods,
                controller.isFriendlyToDebugger());
        decompiler.setRegularMethodCache(controller.isIncremental() ? astCache : null);

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
        initExceptionInsn.getArguments().add(constVar);
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
        Map<TextLocation, TextLocation[]> cfg = ProgramUtils.getLocationCFG(program);
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
}
