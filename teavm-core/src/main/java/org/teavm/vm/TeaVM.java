/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.vm;

import java.io.*;
import java.util.*;
import org.teavm.codegen.*;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.dependency.*;
import org.teavm.diagnostics.AccumulationDiagnostics;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.javascript.*;
import org.teavm.javascript.ast.ClassNode;
import org.teavm.javascript.spi.GeneratedBy;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.InjectedBy;
import org.teavm.javascript.spi.Injector;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.*;
import org.teavm.optimization.*;
import org.teavm.vm.spi.RendererListener;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * <p>TeaVM itself. This class builds a JavaScript VM that runs a certain code.
 * Here you can specify entry points into your code (such like {@code main} method).
 * TeaVM guarantees that all required classes and methods will be provided by
 * built VM.</p>
 *
 * <p>Here is a typical code snippet:</p>
 *
 * <pre>{@code
 *ClassLoader classLoader = ...; // obtain ClassLoader somewhere
 *ClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
 *TeaVM vm = new TeaVMBuilder()
 *        .setClassLoader(classLoader)
 *        .setClassSource(classSource)
 *        .build();
 *vm.setMinifying(false); // optionally disable obfuscation
 *vm.installPlugins();    // install all default plugins
 *                        // that are found in a classpath
 *vm.entryPoint("main", new MethodReference(
 *        "fully.qualified.ClassName",  "main",
 *         ValueType.array(ValueType.object("java.lang.String")),
 *         ValueType.VOID));
 *StringBuilder sb = new StringBuilder();
 *vm.build(sb, null);
 *vm.checkForMissingItems();
 *}</pre>
 *
 * @author Alexey Andreev
 */
public class TeaVM implements TeaVMHost, ServiceRepository {
    private ClassReaderSource classSource;
    private DependencyChecker dependencyChecker;
    private AccumulationDiagnostics diagnostics = new AccumulationDiagnostics();
    private ClassLoader classLoader;
    private boolean minifying = true;
    private boolean bytecodeLogging;
    private OutputStream logStream = System.out;
    private Map<String, TeaVMEntryPoint> entryPoints = new HashMap<>();
    private Map<String, String> exportedClasses = new HashMap<>();
    private Map<MethodReference, Generator> methodGenerators = new HashMap<>();
    private Map<MethodReference, Injector> methodInjectors = new HashMap<>();
    private List<RendererListener> rendererListeners = new ArrayList<>();
    private Map<Class<?>, Object> services = new HashMap<>();
    private Properties properties = new Properties();
    private DebugInformationEmitter debugEmitter;
    private ProgramCache programCache;
    private RegularMethodNodeCache astCache = new EmptyRegularMethodNodeCache();
    private boolean incremental;
    private TeaVMProgressListener progressListener;
    private boolean cancelled;
    private ListableClassHolderSource writtenClasses;
    private Set<MethodReference> asyncMethods = new HashSet<>();
    private Set<MethodReference> asyncFamilyMethods = new HashSet<>();

    TeaVM(ClassReaderSource classSource, ClassLoader classLoader) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        dependencyChecker = new DependencyChecker(this.classSource, classLoader, this, diagnostics);
        progressListener = new TeaVMProgressListener() {
            @Override public TeaVMProgressFeedback progressReached(int progress) {
                return TeaVMProgressFeedback.CONTINUE;
            }
            @Override public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
                return TeaVMProgressFeedback.CONTINUE;
            }
        };
    }

    @Override
    public void add(DependencyListener listener) {
        dependencyChecker.addDependencyListener(listener);
    }

    @Override
    public void add(ClassHolderTransformer transformer) {
        dependencyChecker.addClassTransformer(transformer);
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
    public void add(RendererListener listener) {
        rendererListeners.add(listener);
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Reports whether this TeaVM instance uses obfuscation when generating the JavaScript code.
     *
     * @see #setMinifying(boolean)
     */
    public boolean isMinifying() {
        return minifying;
    }

    /**
     * Specifies whether this TeaVM instance uses obfuscation when generating the JavaScript code.
     *
     * @see #isMinifying()
     */
    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public boolean isBytecodeLogging() {
        return bytecodeLogging;
    }

    public void setBytecodeLogging(boolean bytecodeLogging) {
        this.bytecodeLogging = bytecodeLogging;
    }

    /**
     * Specifies configuration properties for TeaVM and its plugins. You should call this method before
     * installing any plugins or interceptors.
     *
     * @param properties configuration properties to set. These properties will be copied into this VM instance,
     * so VM won't see any further changes in this object.
     */
    public void setProperties(Properties properties) {
        this.properties.clear();
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    @Override
    public Properties getProperties() {
        return new Properties(properties);
    }

    public RegularMethodNodeCache getAstCache() {
        return astCache;
    }

    public void setAstCache(RegularMethodNodeCache methodAstCache) {
        this.astCache = methodAstCache;
    }

    public ProgramCache getProgramCache() {
        return programCache;
    }

    public void setProgramCache(ProgramCache programCache) {
        this.programCache = programCache;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public TeaVMProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(TeaVMProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public ProblemProvider getProblemProvider() {
        return diagnostics;
    }

    /**
     * <p>Adds an entry point. TeaVM guarantees, that all methods that are required by the entry point
     * will be available at run-time in browser. Also you need to specify for each parameter of entry point
     * which actual types will be passed here by calling {@link TeaVMEntryPoint#withValue(int, String)}.
     * It is highly recommended to read explanation on {@link TeaVMEntryPoint} class documentation.</p>
     *
     * <p>You should call this method after installing all plugins and interceptors, but before
     * doing the actual build.</p>
     *
     * @param name the name under which this entry point will be available for JavaScript code.
     * @param ref a full reference to the method which is an entry point.
     * @return an entry point that you can additionally adjust.
     */
    public TeaVMEntryPoint entryPoint(String name, MethodReference ref) {
        if (name != null) {
            if (entryPoints.containsKey(name)) {
                throw new IllegalArgumentException("Entry point with public name `" + name + "' already defined " +
                        "for method " + ref);
            }
        }
        TeaVMEntryPoint entryPoint = new TeaVMEntryPoint(name, ref, dependencyChecker.linkMethod(ref, null));
        dependencyChecker.linkClass(ref.getClassName(), null).initClass(null);
        if (name != null) {
            entryPoints.put(name, entryPoint);
        }
        return entryPoint;
    }

    /**
     * <p>Adds an entry point. TeaVM guarantees, that all methods that are required by the entry point
     * will be available at run-time in browser. Also you need to specify for each parameter of entry point
     * which actual types will be passed here by calling {@link TeaVMEntryPoint#withValue(int, String)}.
     * It is highly recommended to read explanation on {@link TeaVMEntryPoint} class documentation.</p>
     *
     * <p>You should call this method after installing all plugins and interceptors, but before
     * doing the actual build.</p>
     *
     * @param ref a full reference to the method which is an entry point.
     * @return an entry point that you can additionally adjust.
     */
    public TeaVMEntryPoint entryPoint(MethodReference ref) {
        return entryPoint(null, ref);
    }

    public TeaVMEntryPoint linkMethod(MethodReference ref) {
        TeaVMEntryPoint entryPoint = new TeaVMEntryPoint("", ref, dependencyChecker.linkMethod(ref, null));
        dependencyChecker.linkClass(ref.getClassName(), null).initClass(null);
        return entryPoint;
    }

    public void exportType(String name, String className) {
        if (exportedClasses.containsKey(name)) {
            throw new IllegalArgumentException("Class with public name `" + name + "' already defined for class " +
                    className);
        }
        dependencyChecker.linkClass(className, null).initClass(null);
        exportedClasses.put(name, className);
    }

    public void linkType(String className) {
        dependencyChecker.linkClass(className, null).initClass(null);
    }

    /**
     * Gets a {@link ClassReaderSource} which is used by this TeaVM instance. It is exactly what was
     * passed to {@link TeaVMBuilder#setClassSource(ClassHolderSource)}.
     */
    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public Collection<String> getClasses() {
        return dependencyChecker.getAchievableClasses();
    }

    public DependencyInfo getDependencyInfo() {
        return dependencyChecker;
    }

    public ListableClassReaderSource getWrittenClasses() {
        return writtenClasses;
    }

    public DebugInformationEmitter getDebugEmitter() {
        return debugEmitter;
    }

    public void setDebugEmitter(DebugInformationEmitter debugEmitter) {
        this.debugEmitter = debugEmitter;
    }

    /**
     * <p>Does actual build. Call this method after TeaVM is fully configured and all entry points
     * are specified. This method may fail if there are items (classes, methods and fields)
     * that are required by entry points, but weren't found in classpath. In this case no
     * actual generation happens and no exceptions thrown, but you can further call
     * {@link #checkForViolations()} or {@link #hasMissingItems()} to learn the build state.</p>
     *
     * @param writer where to generate JavaScript. Should not be null.
     * @param target where to generate additional resources. Can be null, but if there are
     * plugins or inteceptors that generate additional resources, the build process will fail.
     */
    public void build(Appendable writer, BuildTarget target) throws RenderingException {
        // Check dependencies
        reportPhase(TeaVMPhase.DEPENDENCY_CHECKING, 1);
        if (wasCancelled()) {
            return;
        }
        AliasProvider aliasProvider = minifying ? new MinifyingAliasProvider() : new DefaultAliasProvider();
        dependencyChecker.setInterruptor(new DependencyCheckerInterruptor() {
            @Override public boolean shouldContinue() {
                return progressListener.progressReached(0) == TeaVMProgressFeedback.CONTINUE;
            }
        });
        dependencyChecker.linkMethod(new MethodReference(Class.class.getName(), "getClass",
                ValueType.object("org.teavm.platform.PlatformClass"), ValueType.parse(Class.class)), null).use();
        dependencyChecker.linkMethod(new MethodReference(String.class, "<init>", char[].class, void.class),
                null).use();
        dependencyChecker.linkMethod(new MethodReference(String.class, "getChars", int.class, int.class, char[].class,
                int.class, void.class), null).use();
        MethodDependency internDep = dependencyChecker.linkMethod(new MethodReference(String.class, "intern",
                String.class), null);
        internDep.getVariable(0).propagate(dependencyChecker.getType("java.lang.String"));
        internDep.use();
        dependencyChecker.linkMethod(new MethodReference(String.class, "length", int.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(Object.class, "clone", Object.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(Thread.class, "currentThread", Thread.class), null).use();
        dependencyChecker.linkMethod(new MethodReference(Thread.class, "getMainThread", Thread.class), null).use();
        dependencyChecker.linkMethod(
                new MethodReference(Thread.class, "setCurrentThread", Thread.class, void.class), null).use();
        MethodDependency exceptionCons = dependencyChecker.linkMethod(new MethodReference(
                NoClassDefFoundError.class, "<init>", String.class, void.class), null);
        exceptionCons.use();
        exceptionCons.getVariable(0).propagate(dependencyChecker.getType(NoClassDefFoundError.class.getName()));
        exceptionCons.getVariable(1).propagate(dependencyChecker.getType("java.lang.String"));
        exceptionCons = dependencyChecker.linkMethod(new MethodReference(NoSuchFieldError.class, "<init>",
                String.class, void.class), null);
        exceptionCons.use();
        exceptionCons.getVariable(0).propagate(dependencyChecker.getType(NoSuchFieldError.class.getName()));
        exceptionCons.getVariable(1).propagate(dependencyChecker.getType("java.lang.String"));
        exceptionCons = dependencyChecker.linkMethod(new MethodReference(NoSuchMethodError.class, "<init>",
                String.class, void.class), null);
        exceptionCons.use();
        exceptionCons.getVariable(0).propagate(dependencyChecker.getType(NoSuchMethodError.class.getName()));
        exceptionCons.getVariable(1).propagate(dependencyChecker.getType("java.lang.String"));
        dependencyChecker.processDependencies();
        if (wasCancelled() || !diagnostics.getSevereProblems().isEmpty()) {
            return;
        }

        // Link
        reportPhase(TeaVMPhase.LINKING, 1);
        if (wasCancelled()) {
            return;
        }
        ListableClassHolderSource classSet = link(dependencyChecker);
        writtenClasses = classSet;
        if (wasCancelled()) {
            return;
        }

        // Optimize and allocate registers
        if (!incremental) {
            devirtualize(classSet, dependencyChecker);
            if (wasCancelled()) {
                return;
            }
        }

        List<ClassNode> clsNodes = modelToAst(classSet);

        // Render
        reportPhase(TeaVMPhase.RENDERING, classSet.getClassNames().size());
        if (wasCancelled()) {
            return;
        }
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, dependencyChecker.getClassSource());
        naming.setMinifying(minifying);
        SourceWriterBuilder builder = new SourceWriterBuilder(naming);
        builder.setMinified(minifying);
        SourceWriter sourceWriter = builder.build(writer);
        Renderer renderer = new Renderer(sourceWriter, classSet, classLoader, this, asyncMethods, asyncFamilyMethods,
                diagnostics);
        renderer.setProperties(properties);
        renderer.setMinifying(minifying);
        if (debugEmitter != null) {
            int classIndex = 0;
            for (String className : classSet.getClassNames()) {
                ClassHolder cls = classSet.get(className);
                for (MethodHolder method : cls.getMethods()) {
                    if (method.getProgram() != null) {
                        emitCFG(debugEmitter, method.getProgram());
                    }
                }
                reportProgress(++classIndex);
                if (wasCancelled()) {
                    return;
                }
            }
            renderer.setDebugEmitter(debugEmitter);
        }
        renderer.getDebugEmitter().setLocationProvider(sourceWriter);
        for (Map.Entry<MethodReference, Injector> entry : methodInjectors.entrySet()) {
            renderer.addInjector(entry.getKey(), entry.getValue());
        }
        try {
            for (RendererListener listener : rendererListeners) {
                listener.begin(renderer, target);
            }
            sourceWriter.append("\"use strict\";").newLine();
            renderer.renderRuntime();
            renderer.render(clsNodes);
            renderer.renderStringPool();
            for (Map.Entry<String, TeaVMEntryPoint> entry : entryPoints.entrySet()) {
                sourceWriter.append("var ").append(entry.getKey()).ws().append("=").ws();
                MethodReference ref = entry.getValue().reference;
                boolean asyncMethod = asyncMethods.contains(ref);
                boolean wrapAsync = !asyncMethod && entry.getValue().isAsync();
                if (wrapAsync) {
                    sourceWriter.append("$rt_staticAsyncAdapter(").appendMethodBody(ref).append(')');
                } else {
                    sourceWriter.append(asyncMethod ? naming.getFullNameForAsync(ref) : naming.getFullNameFor(ref));
                }

                if (wrapAsync) {
                    sourceWriter.append(")");
                }
                sourceWriter.append(";").newLine();
            }
            for (Map.Entry<String, String> entry : exportedClasses.entrySet()) {
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

    public ListableClassHolderSource link(DependencyInfo dependency) {
        reportPhase(TeaVMPhase.LINKING, dependency.getAchievableClasses().size());
        Linker linker = new Linker();
        MutableClassHolderSource cutClasses = new MutableClassHolderSource();
        MissingItemsProcessor missingItemsProcessor = new MissingItemsProcessor(dependency, diagnostics);
        if (wasCancelled()) {
            return cutClasses;
        }
        int index = 0;
        for (String className : dependency.getAchievableClasses()) {
            ClassReader clsReader = dependency.getClassSource().get(className);
            if (clsReader == null) {
                continue;
            }
            ClassHolder cls = ModelUtils.copyClass(clsReader);
            cutClasses.putClassHolder(cls);
            missingItemsProcessor.processClass(cls);
            linker.link(dependency, cls);
            progressListener.progressReached(++index);
        }
        return cutClasses;
    }

    private void reportPhase(TeaVMPhase phase, int progressLimit) {
        if (progressListener.phaseStarted(phase, progressLimit) == TeaVMProgressFeedback.CANCEL) {
            cancelled = true;
        }
    }

    private void reportProgress(int progress) {
        if (progressListener.progressReached(progress) == TeaVMProgressFeedback.CANCEL) {
            cancelled = true;
        }
    }

    private void emitCFG(DebugInformationEmitter emitter, Program program) {
        Map<InstructionLocation, InstructionLocation[]> cfg = ProgramUtils.getLocationCFG(program);
        for (Map.Entry<InstructionLocation, InstructionLocation[]> entry : cfg.entrySet()) {
            SourceLocation location = map(entry.getKey());
            SourceLocation[] successors = new SourceLocation[entry.getValue().length];
            for (int i = 0; i < entry.getValue().length; ++i) {
                successors[i] = map(entry.getValue()[i]);
            }
            emitter.addSuccessors(location, successors);
        }
    }

    private static SourceLocation map(InstructionLocation location) {
        if (location == null) {
            return null;
        }
        return new SourceLocation(location.getFileName(), location.getLine());
    }

    private void devirtualize(ListableClassHolderSource classes, DependencyInfo dependency)  {
        reportPhase(TeaVMPhase.DEVIRTUALIZATION, classes.getClassNames().size());
        if (wasCancelled()) {
            return;
        }
        final Devirtualization devirtualization = new Devirtualization(dependency, classes);
        int index = 0;
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (final MethodHolder method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    devirtualization.apply(method);
                }
            }
            reportProgress(++index);
            if (wasCancelled()) {
                return;
            }
        }
    }

    private List<ClassNode> modelToAst(ListableClassHolderSource classes) {
        AsyncMethodFinder asyncFinder = new AsyncMethodFinder(dependencyChecker.getCallGraph(), diagnostics);
        asyncFinder.find(classes);
        asyncMethods.addAll(asyncFinder.getAsyncMethods());
        asyncFamilyMethods.addAll(asyncFinder.getAsyncFamilyMethods());

        progressListener.phaseStarted(TeaVMPhase.DECOMPILATION, classes.getClassNames().size());
        Decompiler decompiler = new Decompiler(classes, classLoader, asyncMethods, asyncFamilyMethods);
        decompiler.setRegularMethodCache(incremental ? astCache : null);

        for (Map.Entry<MethodReference, Generator> entry : methodGenerators.entrySet()) {
            decompiler.addGenerator(entry.getKey(), entry.getValue());
        }
        for (MethodReference injectedMethod : methodInjectors.keySet()) {
            decompiler.addMethodToPass(injectedMethod);
        }
        List<String> classOrder = decompiler.getClassOrdering(classes.getClassNames());
        List<ClassNode> classNodes = new ArrayList<>();
        int index = 0;
        try (PrintWriter bytecodeLogger = bytecodeLogging ?
                new PrintWriter(new OutputStreamWriter(logStream, "UTF-8")) : null) {
            for (String className : classOrder) {
                ClassHolder cls = classes.get(className);
                for (MethodHolder method : cls.getMethods()) {
                    processMethod(method);
                    preprocessNativeMethod(method);
                    if (bytecodeLogging) {
                        logMethodBytecode(bytecodeLogger, method);
                    }
                }
                classNodes.add(decompiler.decompile(cls));
                progressListener.progressReached(++index);
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is expected to be supported");
        }
        return classNodes;
    }

    private void preprocessNativeMethod(MethodHolder method) {
        if (!method.getModifiers().contains(ElementModifier.NATIVE) ||
                methodGenerators.get(method.getReference()) != null ||
                methodInjectors.get(method.getReference()) != null ||
                method.getAnnotations().get(GeneratedBy.class.getName()) != null ||
                method.getAnnotations().get(InjectedBy.class.getName()) != null) {
            return;
        }
        method.getModifiers().remove(ElementModifier.NATIVE);

        Program program = new Program();
        method.setProgram(program);
        BasicBlock block = program.createBasicBlock();
        Variable exceptionVar = program.createVariable();
        ConstructInstruction newExceptionInsn = new ConstructInstruction();
        newExceptionInsn.setType(NoSuchMethodError.class.getName());
        newExceptionInsn.setReceiver(exceptionVar);
        block.getInstructions().add(newExceptionInsn);

        Variable constVar = program.createVariable();
        StringConstantInstruction constInsn = new StringConstantInstruction();
        constInsn.setConstant("Native method implementation not found: " + method.getReference());
        constInsn.setReceiver(constVar);
        block.getInstructions().add(constInsn);

        InvokeInstruction initExceptionInsn = new InvokeInstruction();
        initExceptionInsn.setInstance(exceptionVar);
        initExceptionInsn.setMethod(new MethodReference(NoSuchMethodError.class, "<init>", String.class, void.class));
        initExceptionInsn.setType(InvocationType.SPECIAL);
        initExceptionInsn.getArguments().add(constVar);
        block.getInstructions().add(initExceptionInsn);

        RaiseInstruction raiseInsn = new RaiseInstruction();
        raiseInsn.setException(exceptionVar);
        block.getInstructions().add(raiseInsn);

        diagnostics.error(new CallLocation(method.getReference()), "Native method {{m0}} has no implementation",
                method.getReference());
    }

    private void processMethod(MethodHolder method) {
        if (method.getProgram() == null) {
            return;
        }
        Program optimizedProgram = incremental && programCache != null ?
                programCache.get(method.getReference()) : null;
        if (optimizedProgram == null) {
            optimizedProgram = ProgramUtils.copy(method.getProgram());
            if (optimizedProgram.basicBlockCount() > 0) {
                for (MethodOptimization optimization : getOptimizations()) {
                    optimization.optimize(method, optimizedProgram);
                }
                RegisterAllocator allocator = new RegisterAllocator();
                allocator.allocateRegisters(method, optimizedProgram);
            }
            if (incremental && programCache != null) {
                programCache.store(method.getReference(), optimizedProgram);
            }
        }
        method.setProgram(optimizedProgram);
    }

    private List<MethodOptimization> getOptimizations() {
        return Arrays.<MethodOptimization>asList(new ArrayUnwrapMotion(), new LoopInvariantMotion(),
                new GlobalValueNumbering(), new UnusedVariableElimination());
    }

    private void logMethodBytecode(PrintWriter writer, MethodHolder method) {
        writer.print("    ");
        printModifiers(writer, method);
        writer.print(method.getName() + "(");
        ValueType[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (i > 0) {
                writer.print(", ");
            }
            printType(writer, parameterTypes[i]);
        }
        writer.println(")");
        Program program = method.getProgram();
        if (program != null && program.basicBlockCount() > 0) {
            ListingBuilder builder = new ListingBuilder();
            writer.print(builder.buildListing(program, "        "));
            writer.print("        Register allocation:");
            for (int i = 0; i < program.variableCount(); ++i) {
                writer.print(i + ":" + program.variableAt(i).getRegister() + " ");
            }
            writer.println();
            writer.println();
            writer.flush();
        } else {
            writer.println();
        }
    }

    private void printType(PrintWriter writer, ValueType type) {
        if (type instanceof ValueType.Object) {
            writer.print(((ValueType.Object)type).getClassName());
        } else if (type instanceof ValueType.Array) {
            printType(writer, ((ValueType.Array)type).getItemType());
            writer.print("[]");
        } else if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    writer.print("boolean");
                    break;
                case SHORT:
                    writer.print("short");
                    break;
                case BYTE:
                    writer.print("byte");
                    break;
                case CHARACTER:
                    writer.print("char");
                    break;
                case DOUBLE:
                    writer.print("double");
                    break;
                case FLOAT:
                    writer.print("float");
                    break;
                case INTEGER:
                    writer.print("int");
                    break;
                case LONG:
                    writer.print("long");
                    break;
            }
        }
    }

    private void printModifiers(PrintWriter writer, ElementHolder element) {
        switch (element.getLevel()) {
            case PRIVATE:
                writer.print("private ");
                break;
            case PUBLIC:
                writer.print("public ");
                break;
            case PROTECTED:
                writer.print("protected ");
                break;
            default:
                break;
        }
        Set<ElementModifier> modifiers = element.getModifiers();
        if (modifiers.contains(ElementModifier.ABSTRACT)) {
            writer.print("abstract ");
        }
        if (modifiers.contains(ElementModifier.FINAL)) {
            writer.print("final ");
        }
        if (modifiers.contains(ElementModifier.STATIC)) {
            writer.print("static ");
        }
        if (modifiers.contains(ElementModifier.NATIVE)) {
            writer.print("native ");
        }
    }

    public void build(File dir, String fileName) throws RenderingException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(dir, fileName)), "UTF-8")) {
            build(writer, new DirectoryBuildTarget(dir));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Platform does not support UTF-8", e);
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    /**
     * <p>Finds and install all plugins in the current class path. The standard {@link ServiceLoader}
     * approach is used to find plugins. So this method scans all
     * <code>META-INF/services/org.teavm.vm.spi.TeaVMPlugin</code> resources and
     * obtains all implementation classes that are enumerated there.</p>
     */
    public void installPlugins() {
        for (TeaVMPlugin plugin : ServiceLoader.load(TeaVMPlugin.class, classLoader)) {
            plugin.install(this);
        }
    }

    @Override
    public <T> T getService(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("Service not registered: " + type.getName());
        }
        return type.cast(service);
    }

    @Override
    public <T> void registerService(Class<T> type, T instance) {
        services.put(type, instance);
    }
}
