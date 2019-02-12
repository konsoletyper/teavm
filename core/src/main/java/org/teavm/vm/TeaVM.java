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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.teavm.cache.AlwaysStaleCacheStatus;
import org.teavm.cache.AnnotationAwareCacheStatus;
import org.teavm.cache.CacheStatus;
import org.teavm.cache.EmptyProgramCache;
import org.teavm.cache.ProgramDependencyExtractor;
import org.teavm.common.ServiceRepository;
import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.Linker;
import org.teavm.dependency.MethodDependency;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.diagnostics.AccumulationDiagnostics;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.MutableClassHolderSource;
import org.teavm.model.Program;
import org.teavm.model.ProgramCache;
import org.teavm.model.ValueType;
import org.teavm.model.optimization.ArrayUnwrapMotion;
import org.teavm.model.optimization.ClassInitElimination;
import org.teavm.model.optimization.ConstantConditionElimination;
import org.teavm.model.optimization.DefaultInliningStrategy;
import org.teavm.model.optimization.Devirtualization;
import org.teavm.model.optimization.GlobalValueNumbering;
import org.teavm.model.optimization.Inlining;
import org.teavm.model.optimization.InliningStrategy;
import org.teavm.model.optimization.LoopInvariantMotion;
import org.teavm.model.optimization.MethodOptimization;
import org.teavm.model.optimization.MethodOptimizationContext;
import org.teavm.model.optimization.RedundantJumpElimination;
import org.teavm.model.optimization.RedundantNullCheckElimination;
import org.teavm.model.optimization.ScalarReplacement;
import org.teavm.model.optimization.UnreachableBasicBlockElimination;
import org.teavm.model.optimization.UnusedVariableElimination;
import org.teavm.model.text.ListingBuilder;
import org.teavm.model.util.MissingItemsProcessor;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.RegisterAllocator;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMHostExtension;
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
    private static final MethodDescriptor MAIN_METHOD_DESC = new MethodDescriptor("main",
            ValueType.arrayOf(ValueType.object("java.lang.String")), ValueType.VOID);

    private final ClassReaderSource classSource;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final AccumulationDiagnostics diagnostics = new AccumulationDiagnostics();
    private final ClassLoader classLoader;
    private final Map<String, TeaVMEntryPoint> entryPoints = new LinkedHashMap<>();
    private final Map<String, TeaVMEntryPoint> readonlyEntryPoints = Collections.unmodifiableMap(entryPoints);
    private final Set<String> preservedClasses = new HashSet<>();
    private final Set<String> readonlyPreservedClasses = Collections.unmodifiableSet(preservedClasses);
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Properties properties = new Properties();
    private ProgramCache programCache = EmptyProgramCache.INSTANCE;
    private CacheStatus rawCacheStatus = AlwaysStaleCacheStatus.INSTANCE;
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.SIMPLE;
    private TeaVMProgressListener progressListener;
    private boolean cancelled;
    private ListableClassHolderSource writtenClasses;
    private TeaVMTarget target;
    private Map<Class<?>, TeaVMHostExtension> extensions = new HashMap<>();
    private Set<? extends MethodReference> virtualMethods;
    private AnnotationAwareCacheStatus cacheStatus;
    private ProgramDependencyExtractor programDependencyExtractor = new ProgramDependencyExtractor();
    private List<Predicate<MethodReference>> additionalVirtualMethods = new ArrayList<>();
    private int lastKnownClasses;
    private int compileProgressReportStart;
    private int compileProgressReportLimit;
    private int compileProgressLimit;
    private int compileProgressValue;

    TeaVM(TeaVMBuilder builder) {
        target = builder.target;
        classSource = builder.classSource;
        classLoader = builder.classLoader;
        dependencyAnalyzer = builder.dependencyAnalyzerFactory.create(this.classSource, classLoader,
                this, diagnostics);
        progressListener = new TeaVMProgressListener() {
            @Override public TeaVMProgressFeedback progressReached(int progress) {
                return TeaVMProgressFeedback.CONTINUE;
            }
            @Override public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
                return TeaVMProgressFeedback.CONTINUE;
            }
        };

        for (ClassHolderTransformer transformer : target.getTransformers()) {
            dependencyAnalyzer.addClassTransformer(transformer);
        }
        for (DependencyListener listener : target.getDependencyListeners()) {
            dependencyAnalyzer.addDependencyListener(listener);
        }

        for (TeaVMHostExtension extension : target.getHostExtensions()) {
            for (Class<?> extensionType : getExtensionTypes(extension)) {
                extensions.put(extensionType, extension);
            }
        }
    }

    public void addVirtualMethods(Predicate<MethodReference> virtualMethods) {
        additionalVirtualMethods.add(virtualMethods);
    }

    @Override
    public void add(DependencyListener listener) {
        dependencyAnalyzer.addDependencyListener(listener);
    }

    @Override
    public void add(ClassHolderTransformer transformer) {
        dependencyAnalyzer.addClassTransformer(transformer);
    }

    @Override
    public void add(MethodReference methodRef, BootstrapMethodSubstitutor substitutor) {
        dependencyAnalyzer.addBootstrapMethodSubstitutor(methodRef, substitutor);
    }

    @Override
    public void add(MethodReference methodRef, DependencyPlugin dependencyPlugin) {
        dependencyAnalyzer.addDependencyPlugin(methodRef, dependencyPlugin);
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
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

    public ProgramCache getProgramCache() {
        return programCache;
    }

    public void setProgramCache(ProgramCache programCache) {
        this.programCache = programCache;
    }

    public void setCacheStatus(CacheStatus cacheStatus) {
        rawCacheStatus = cacheStatus;
    }

    public TeaVMOptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }

    public void setOptimizationLevel(TeaVMOptimizationLevel optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
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

    @Override
    public String[] getPlatformTags() {
        return target.getPlatformTags();
    }

    public void entryPoint(String className, String name) {
        if (entryPoints.containsKey(name)) {
            throw new IllegalArgumentException("Entry point with public name `" + name + "' already defined "
                    + "for class " + className);
        }

        ClassReader cls = dependencyAnalyzer.getClassSource().get(className);
        if (cls == null) {
            diagnostics.error(null, "There's no main class: '{{c0}}'", className);
            return;
        }

        if (cls.getMethod(MAIN_METHOD_DESC) == null) {
            diagnostics.error(null, "Specified main class '{{c0}}' does not have method '" + MAIN_METHOD_DESC + "'",
                    cls.getName());
            return;
        }

        MethodDependency mainMethod = dependencyAnalyzer.linkMethod(new MethodReference(className,
                "main", ValueType.parse(String[].class), ValueType.VOID));

        TeaVMEntryPoint entryPoint = new TeaVMEntryPoint(name, mainMethod);
        dependencyAnalyzer.defer(() -> {
            dependencyAnalyzer.linkClass(className).initClass(null);
            mainMethod.getVariable(1).propagate(dependencyAnalyzer.getType("[Ljava/lang/String;"));
            mainMethod.getVariable(1).getArrayItem().propagate(dependencyAnalyzer.getType("java.lang.String"));
            mainMethod.use();
        });
        entryPoints.put(name, entryPoint);
    }

    public void entryPoint(String className) {
        entryPoint(className, "main");
    }

    public void preserveType(String className) {
        dependencyAnalyzer.defer(() -> {
            dependencyAnalyzer.linkClass(className).initClass(null);
        });
        preservedClasses.add(className);
    }

    /**
     * Gets a {@link ClassReaderSource} which is used by this TeaVM instance. It is exactly what was
     * passed to {@link TeaVMBuilder#setClassSource(ClassReaderSource)}.
     *
     * @return class source.
     */
    public ClassReaderSource getClassSource() {
        return classSource;
    }

    /**
     * Gets a {@link ClassReaderSource} which is similar to that of {@link #getClassSource()},
     * except that it also contains classes with applied transformations together with
     * classes, generated via {@link org.teavm.dependency.DependencyAgent#submitClass(ClassHolder)}.
     */
    public ClassReaderSource getDependencyClassSource() {
        return dependencyAnalyzer.getClassSource();
    }

    public Collection<String> getClasses() {
        return dependencyAnalyzer.getReachableClasses();
    }

    public Collection<MethodReference> getMethods() {
        return dependencyAnalyzer.getReachableMethods();
    }

    public DependencyInfo getDependencyInfo() {
        return dependencyAnalyzer;
    }

    public ListableClassReaderSource getWrittenClasses() {
        return writtenClasses;
    }

    public void setLastKnownClasses(int lastKnownClasses) {
        this.lastKnownClasses = lastKnownClasses;
    }

    /**
     * <p>Does actual build. Call this method after TeaVM is fully configured and all entry points
     * are specified. This method may fail if there are items (classes, methods and fields)
     * that are required by entry points, but weren't found in classpath. In this case no
     * actual generation happens and no exceptions thrown, but you can further call
     * {@link #getProblemProvider()} to learn the build state.</p>
     *
     * @param buildTarget where to generate additional resources. Can be null, but if there are
     * plugins or interceptors that generate additional resources, the build process will fail.
     * @param outputName name of output file within buildTarget. Should not be null.
     */
    public void build(BuildTarget buildTarget, String outputName) {
        target.setController(targetController);

        // Check dependencies
        reportPhase(TeaVMPhase.DEPENDENCY_ANALYSIS, lastKnownClasses > 0 ? lastKnownClasses : 1);
        if (wasCancelled()) {
            return;
        }

        dependencyAnalyzer.setAsyncSupported(target.isAsyncSupported());
        dependencyAnalyzer.setInterruptor(() -> {
            int progress = lastKnownClasses > 0 ? dependencyAnalyzer.getReachableClasses().size() : 0;
            cancelled |= progressListener.progressReached(progress) != TeaVMProgressFeedback.CONTINUE;
            return !cancelled;
        });
        target.contributeDependencies(dependencyAnalyzer);
        dependencyAnalyzer.processDependencies();
        if (wasCancelled() || !diagnostics.getSevereProblems().isEmpty()) {
            return;
        }

        cacheStatus = new AnnotationAwareCacheStatus(rawCacheStatus, dependencyAnalyzer.getIncrementalDependencies(),
                dependencyAnalyzer.getClassSource());
        cacheStatus.addSynthesizedClasses(dependencyAnalyzer::isSynthesizedClass);
        dependencyAnalyzer.setInterruptor(null);

        if (wasCancelled()) {
            return;
        }

        boolean isLazy = optimizationLevel == TeaVMOptimizationLevel.SIMPLE;
        ListableClassHolderSource classSet;
        if (isLazy) {
            initCompileProgress(1000);
            classSet = lazyPipeline();
        } else {
            initCompileProgress(500);
            classSet = eagerPipeline();
            if (wasCancelled()) {
                return;
            }
        }

        if (wasCancelled()) {
            return;
        }

        // Render
        try {
            if (!isLazy) {
                compileProgressReportStart = 500;
                compileProgressReportLimit = 1000;
            }
            target.emit(classSet, buildTarget, outputName);
        } catch (IOException e) {
            throw new RuntimeException("Error generating output files", e);
        }
    }

    private void initCompileProgress(int limit) {
        reportPhase(TeaVMPhase.COMPILING, 1000);
        compileProgressReportStart = 0;
        compileProgressReportLimit = limit;
    }

    private ListableClassHolderSource eagerPipeline() {
        compileProgressValue = 0;
        compileProgressLimit = dependencyAnalyzer.getReachableClasses().size();
        if (optimizationLevel == TeaVMOptimizationLevel.ADVANCED) {
            compileProgressLimit *= 4;
        } else {
            compileProgressLimit *= 2;
        }

        ListableClassHolderSource classSet = link(dependencyAnalyzer);
        writtenClasses = classSet;
        if (wasCancelled()) {
            return null;
        }

        if (optimizationLevel != TeaVMOptimizationLevel.SIMPLE) {
            devirtualize(classSet);
            if (wasCancelled()) {
                return null;
            }
        }

        dependencyAnalyzer.cleanupTypes();

        inline(classSet);
        if (wasCancelled()) {
            return null;
        }

        // Optimize and allocate registers
        optimize(classSet);
        if (wasCancelled()) {
            return null;
        }

        return classSet;
    }

    private ListableClassHolderSource lazyPipeline() {
        return new PostProcessingClassHolderSource();
    }

    public ListableClassHolderSource link(DependencyAnalyzer dependency) {
        Linker linker = new Linker(dependency);
        MutableClassHolderSource cutClasses = new MutableClassHolderSource();
        MissingItemsProcessor missingItemsProcessor = new MissingItemsProcessor(dependency,
                dependency.getClassHierarchy(), diagnostics);
        if (wasCancelled()) {
            return cutClasses;
        }

        if (wasCancelled()) {
            return cutClasses;
        }

        for (String className : dependency.getReachableClasses()) {
            ClassReader clsReader = dependency.getClassSource().get(className);
            if (clsReader != null) {
                ClassHolder cls = ModelUtils.copyClass(clsReader);
                cutClasses.putClassHolder(cls);
                missingItemsProcessor.processClass(cls);
                linker.link(cls);
            }
            reportCompileProgress(++compileProgressValue);
            if (wasCancelled()) {
                break;
            }
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

    private void reportCompileProgress(int progress) {
        reportProgress(compileProgressReportStart
                + progress * (compileProgressReportLimit - compileProgressReportStart) / compileProgressLimit);
    }

    private void devirtualize(ListableClassHolderSource classes) {
        if (wasCancelled()) {
            return;
        }
        Devirtualization devirtualization = new Devirtualization(dependencyAnalyzer,
                dependencyAnalyzer.getClassHierarchy());
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    devirtualization.apply(method);
                }
            }
            reportCompileProgress(++compileProgressValue);
            if (wasCancelled()) {
                break;
            }
        }
        virtualMethods = devirtualization.getVirtualMethods();
    }

    private void inline(ListableClassHolderSource classes) {
        if (optimizationLevel != TeaVMOptimizationLevel.ADVANCED) {
            return;
        }

        InliningStrategy inliningStrategy;
        if (optimizationLevel == TeaVMOptimizationLevel.FULL) {
            inliningStrategy = new DefaultInliningStrategy(17, 7, false);
        } else {
            inliningStrategy = new DefaultInliningStrategy(100, 5, true);
        }

        Inlining inlining = new Inlining(new ClassHierarchy(classes), dependencyAnalyzer, inliningStrategy,
                classes, this::isExternal);
        List<MethodReference> methodReferences = inlining.getOrder();
        int classCount = classes.getClassNames().size();
        int initialValue = compileProgressValue;
        for (int i = 0; i < methodReferences.size(); i++) {
            MethodReference methodReference = methodReferences.get(i);
            ClassHolder cls = classes.get(methodReference.getClassName());
            MethodHolder method = cls.getMethod(methodReference.getDescriptor());

            if (method.getProgram() != null) {
                if (!inlining.hasUsages(methodReference)) {
                    method.setProgram(null);
                } else {
                    Program program = method.getProgram();
                    MethodOptimizationContextImpl context = new MethodOptimizationContextImpl(method);
                    inlining.apply(program, method.getReference());
                    new UnusedVariableElimination().optimize(context, program);
                }
            }

            int newProgress = initialValue + classCount * i / methodReferences.size();
            if (newProgress > compileProgressValue) {
                compileProgressValue = newProgress;
                reportCompileProgress(++compileProgressValue);
                if (wasCancelled()) {
                    break;
                }
            }
        }
    }

    private void optimize(ListableClassHolderSource classSource) {
        for (String className : classSource.getClassNames()) {
            ClassHolder cls = classSource.get(className);
            for (MethodHolder method : cls.getMethods()) {
                optimizeMethod(method);
            }
            reportCompileProgress(++compileProgressValue);
            if (wasCancelled()) {
                break;
            }
        }
    }

    private void optimizeMethod(MethodHolder method) {
        if (method.getProgram() == null) {
            return;
        }

        Program optimizedProgram = !cacheStatus.isStaleMethod(method.getReference())
                ? programCache.get(method.getReference(), cacheStatus)
                : null;
        if (optimizedProgram == null) {
            optimizedProgram = optimizeMethodCacheMiss(method, ProgramUtils.copy(method.getProgram()));
            Program finalProgram = optimizedProgram;
            programCache.store(method.getReference(), finalProgram,
                    () -> programDependencyExtractor.extractDependencies(finalProgram));
        }
        method.setProgram(optimizedProgram);
    }

    private Program optimizeMethodCacheMiss(MethodHolder method, Program optimizedProgram) {
        target.beforeOptimizations(optimizedProgram, method);

        if (optimizedProgram.basicBlockCount() > 0) {
            MethodOptimizationContextImpl context = new MethodOptimizationContextImpl(method);
            boolean changed;
            do {
                changed = false;
                for (MethodOptimization optimization : getOptimizations()) {
                    try {
                        changed |= optimization.optimize(context, optimizedProgram);
                    } catch (Exception | AssertionError e) {
                        ListingBuilder listingBuilder = new ListingBuilder();
                        String listing = listingBuilder.buildListing(optimizedProgram, "");
                        System.err.println("Error optimizing program for method " + method.getReference()
                                + ":\n" + listing);
                        throw new RuntimeException(e);
                    }
                }
            } while (changed);

            target.afterOptimizations(optimizedProgram, method);
            if (target.requiresRegisterAllocation()) {
                RegisterAllocator allocator = new RegisterAllocator();
                allocator.allocateRegisters(method.getReference(), optimizedProgram,
                        optimizationLevel == TeaVMOptimizationLevel.SIMPLE);
            }
        }

        return optimizedProgram;
    }

    class MethodOptimizationContextImpl implements MethodOptimizationContext {
        private MethodReader method;

        MethodOptimizationContextImpl(MethodReader method) {
            this.method = method;
        }

        @Override
        public MethodReader getMethod() {
            return method;
        }

        @Override
        public DependencyInfo getDependencyInfo() {
            return dependencyAnalyzer;
        }
    }

    private List<MethodOptimization> getOptimizations() {
        List<MethodOptimization> optimizations = new ArrayList<>();
        optimizations.add(new RedundantJumpElimination());
        optimizations.add(new ArrayUnwrapMotion());
        if (optimizationLevel.ordinal() >= TeaVMOptimizationLevel.ADVANCED.ordinal()) {
            optimizations.add(new ScalarReplacement());
            //optimizations.add(new LoopInversion());
            optimizations.add(new LoopInvariantMotion());
        }
        optimizations.add(new GlobalValueNumbering(optimizationLevel == TeaVMOptimizationLevel.SIMPLE));
        if (optimizationLevel.ordinal() >= TeaVMOptimizationLevel.ADVANCED.ordinal()) {
            optimizations.add(new RedundantNullCheckElimination());
            optimizations.add(new ConstantConditionElimination());
            optimizations.add(new RedundantJumpElimination());
            optimizations.add(new UnusedVariableElimination());
        }
        optimizations.add(new ClassInitElimination());
        optimizations.add(new UnreachableBasicBlockElimination());
        optimizations.add(new UnusedVariableElimination());
        return optimizations;
    }

    public void build(File dir, String fileName) {
        build(new DirectoryBuildTarget(dir), fileName);
    }

    /**
     * <p>Finds and install all plugins in the current class path. The standard {@link ServiceLoader}
     * approach is used to find plugins. So this method scans all
     * <code>META-INF/services/org.teavm.vm.spi.TeaVMPlugin</code> resources and
     * obtains all implementation classes that are enumerated there.</p>
     */
    public void installPlugins() {
        for (TeaVMPlugin plugin : TeaVMPluginLoader.load(classLoader)) {
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

    @Override
    public <T extends TeaVMHostExtension> T getExtension(Class<T> extensionType) {
        Object extension = extensions.get(extensionType);
        return extension != null ? extensionType.cast(extension) : null;
    }

    private Collection<Class<? extends TeaVMHostExtension>> getExtensionTypes(TeaVMHostExtension extension) {
        return Arrays.stream(extension.getClass().getInterfaces())
                .filter(cls -> cls.isInterface() && TeaVMHostExtension.class.isAssignableFrom(cls))
                .<Class<? extends TeaVMHostExtension>>map(cls -> cls.asSubclass(TeaVMHostExtension.class))
                .collect(Collectors.toSet());
    }

    boolean isExternal(MethodReference method) {
        MethodDependencyInfo dep = dependencyAnalyzer.getMethod(method);
        if (dep != null && dep.isCalled()) {
            return true;
        }
        return isVirtual(method);
    }

    boolean isVirtual(MethodReference method) {
        if (method.getName().equals("<init>") || method.getName().equals("<clinit>")) {
            return false;
        }
        return virtualMethods == null || virtualMethods.contains(method)
                || additionalVirtualMethods.stream().anyMatch(p -> p.test(method));
    }

    private TeaVMTargetController targetController = new TeaVMTargetController() {
        @Override
        public boolean wasCancelled() {
            return TeaVM.this.wasCancelled();
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ClassReaderSource getUnprocessedClassSource() {
            return dependencyAnalyzer.getClassSource();
        }

        @Override
        public CacheStatus getCacheStatus() {
            return cacheStatus;
        }

        @Override
        public DependencyInfo getDependencyInfo() {
            return dependencyAnalyzer;
        }

        @Override
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        @Override
        public ServiceRepository getServices() {
            return TeaVM.this;
        }

        @Override
        public Map<String, TeaVMEntryPoint> getEntryPoints() {
            return readonlyEntryPoints;
        }

        @Override
        public Set<String> getPreservedClasses() {
            return readonlyPreservedClasses;
        }

        @Override
        public boolean isFriendlyToDebugger() {
            return optimizationLevel == TeaVMOptimizationLevel.SIMPLE;
        }

        @Override
        public boolean isVirtual(MethodReference method) {
            return TeaVM.this.isVirtual(method);
        }

        @Override
        public TeaVMProgressFeedback reportProgress(int progress) {
            progress = progress * (compileProgressReportLimit - compileProgressReportStart) / 1000
                    + compileProgressReportStart;
            return progressListener.progressReached(progress);
        }
    };

    class PostProcessingClassHolderSource implements ListableClassHolderSource {
        private Linker linker = new Linker(dependencyAnalyzer);
        private MissingItemsProcessor missingItemsProcessor = new MissingItemsProcessor(dependencyAnalyzer,
                dependencyAnalyzer.getClassHierarchy(), diagnostics);
        private Map<String, ClassHolder> cache = new HashMap<>();
        private Set<String> classNames = Collections.unmodifiableSet(new HashSet<>(
                dependencyAnalyzer.getReachableClasses().stream()
                        .filter(className -> dependencyAnalyzer.getClassSource().get(className) != null)
                        .collect(Collectors.toList())));

        @Override
        public ClassHolder get(String name) {
            return cache.computeIfAbsent(name, className -> {
                ClassReader classReader = dependencyAnalyzer.getClassSource().get(className);
                if (classReader == null) {
                    return null;
                }
                ClassHolder cls = ModelUtils.copyClass(classReader, false);

                for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
                    FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
                    if (dependencyAnalyzer.getField(fieldRef) == null) {
                        cls.removeField(field);
                    }
                }

                Function<MethodHolder, Program> programSupplier = method -> {
                    Program program = !cacheStatus.isStaleMethod(method.getReference())
                            ? programCache.get(method.getReference(), cacheStatus)
                            : null;
                    if (program == null) {
                        program = ProgramUtils.copy(classReader.getMethod(method.getDescriptor()).getProgram());
                        missingItemsProcessor.processMethod(method.getReference(), program);
                        linker.link(method.getReference(), program);
                        program = optimizeMethodCacheMiss(method, program);
                        Program finalProgram = program;
                        programCache.store(method.getReference(), finalProgram,
                                () -> programDependencyExtractor.extractDependencies(finalProgram));
                    }
                    return program;
                };
                for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
                    MethodDependencyInfo methodDep = dependencyAnalyzer.getMethod(method.getReference());
                    if (methodDep == null) {
                        cls.removeMethod(method);
                    } else if (!methodDep.isUsed()) {
                        method.getModifiers().add(ElementModifier.ABSTRACT);
                    } else {
                        MethodReader methodReader = classReader.getMethod(method.getDescriptor());
                        if (methodReader != null && methodReader.getProgram() != null) {
                            method.setProgramSupplier(programSupplier);
                        }
                    }
                }
                return cls;
            });
        }

        @Override
        public Set<String> getClassNames() {
            return classNames;
        }
    }
}
