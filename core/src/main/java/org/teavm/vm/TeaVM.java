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
import org.teavm.diagnostics.AccumulationDiagnostics;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
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
import org.teavm.model.optimization.Devirtualization;
import org.teavm.model.optimization.GlobalValueNumbering;
import org.teavm.model.optimization.Inlining;
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
     * passed to {@link TeaVMBuilder#setClassSource(ClassHolderSource)}.
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

        // Link
        if (wasCancelled()) {
            return;
        }
        ListableClassHolderSource classSet = link(dependencyAnalyzer);
        writtenClasses = classSet;
        if (wasCancelled()) {
            return;
        }

        // Optimize and allocate registers
        int maxOptimizationProgress = classSet.getClassNames().size();
        if (optimizationLevel == TeaVMOptimizationLevel.ADVANCED) {
            maxOptimizationProgress *= 2;
        } else if (optimizationLevel == TeaVMOptimizationLevel.FULL) {
            maxOptimizationProgress *= 3;
        }
        reportPhase(TeaVMPhase.OPTIMIZATION, maxOptimizationProgress);

        int progress = 0;
        if (optimizationLevel != TeaVMOptimizationLevel.SIMPLE) {
            progress = devirtualize(progress, classSet, dependencyAnalyzer);
            if (wasCancelled()) {
                return;
            }
        }

        dependencyAnalyzer.cleanup();
        progress = inline(progress, classSet, dependencyAnalyzer);
        if (wasCancelled()) {
            return;
        }

        optimize(progress, classSet);
        if (wasCancelled()) {
            return;
        }

        // Render
        try {
            reportPhase(TeaVMPhase.RENDERING, 1000);
            target.emit(classSet, buildTarget, outputName);
        } catch (IOException e) {
            throw new RuntimeException("Error generating output files", e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public ListableClassHolderSource link(DependencyAnalyzer dependency) {
        reportPhase(TeaVMPhase.LINKING, dependency.getReachableClasses().size());
        Linker linker = new Linker();
        MutableClassHolderSource cutClasses = new MutableClassHolderSource();
        MissingItemsProcessor missingItemsProcessor = new MissingItemsProcessor(dependency,
                dependency.getClassHierarchy(), diagnostics);
        if (wasCancelled()) {
            return cutClasses;
        }
        int index = 0;

        if (wasCancelled()) {
            return cutClasses;
        }

        for (String className : dependency.getReachableClasses()) {
            ClassReader clsReader = dependency.getClassSource().get(className);
            if (clsReader == null) {
                continue;
            }
            ClassHolder cls = ModelUtils.copyClass(clsReader);
            cutClasses.putClassHolder(cls);
            missingItemsProcessor.processClass(cls);
            linker.link(dependency, cls);
            reportProgress(++index);
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

    private int devirtualize(int progress, ListableClassHolderSource classes, DependencyInfo dependency) {
        if (wasCancelled()) {
            return progress;
        }
        Devirtualization devirtualization = new Devirtualization(dependency, classes);
        int index = 0;
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    devirtualization.apply(method);
                }
            }
            reportProgress(++index);
            if (wasCancelled()) {
                break;
            }
        }
        virtualMethods = devirtualization.getVirtualMethods();
        return progress;
    }

    private int inline(int progress, ListableClassHolderSource classes, DependencyInfo dependencyInfo) {
        if (optimizationLevel != TeaVMOptimizationLevel.FULL) {
            return progress;
        }

        Map<MethodReference, Program> inlinedPrograms = new HashMap<>();
        Inlining inlining = new Inlining(new ClassHierarchy(classes), dependencyInfo);
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    Program program = ProgramUtils.copy(method.getProgram());
                    MethodOptimizationContextImpl context = new MethodOptimizationContextImpl(method, classes);
                    inlining.apply(program, method.getReference());
                    new UnusedVariableElimination().optimize(context, program);
                    inlinedPrograms.put(method.getReference(), program);
                }
            }
            reportProgress(++progress);
            if (wasCancelled()) {
                break;
            }
        }

        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    method.setProgram(inlinedPrograms.get(method.getReference()));
                }
            }
        }

        return progress;
    }

    private int optimize(int progress, ListableClassHolderSource classSource) {
        for (String className : classSource.getClassNames()) {
            ClassHolder cls = classSource.get(className);
            for (MethodHolder method : cls.getMethods()) {
                processMethod(method, classSource);
            }
            reportProgress(++progress);
            if (wasCancelled()) {
                break;
            }
        }
        return progress;
    }

    private void processMethod(MethodHolder method, ListableClassReaderSource classSource) {
        if (method.getProgram() == null) {
            return;
        }

        Program optimizedProgram = !cacheStatus.isStaleMethod(method.getReference())
                ? programCache.get(method.getReference(), cacheStatus)
                : null;
        MethodOptimizationContextImpl context = new MethodOptimizationContextImpl(method, classSource);
        if (optimizedProgram == null) {
            optimizedProgram = ProgramUtils.copy(method.getProgram());
            target.beforeOptimizations(optimizedProgram, method, classSource);

            if (optimizedProgram.basicBlockCount() > 0) {
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

                target.afterOptimizations(optimizedProgram, method, classSource);
                if (target.requiresRegisterAllocation()) {
                    RegisterAllocator allocator = new RegisterAllocator();
                    allocator.allocateRegisters(method, optimizedProgram,
                            optimizationLevel == TeaVMOptimizationLevel.SIMPLE);
                }
            }

            Program finalProgram = optimizedProgram;
            programCache.store(method.getReference(), finalProgram,
                    () -> programDependencyExtractor.extractDependencies(finalProgram));
        }
        method.setProgram(optimizedProgram);
    }

    class MethodOptimizationContextImpl implements MethodOptimizationContext {
        private MethodReader method;
        private ClassReaderSource classSource;

        MethodOptimizationContextImpl(MethodReader method, ClassReaderSource classSource) {
            this.method = method;
            this.classSource = classSource;
        }

        @Override
        public MethodReader getMethod() {
            return method;
        }

        @Override
        public DependencyInfo getDependencyInfo() {
            return dependencyAnalyzer;
        }

        @Override
        public ClassReaderSource getClassSource() {
            return classSource;
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
            return virtualMethods == null || virtualMethods.contains(method);
        }

        @Override
        public TeaVMProgressFeedback reportProgress(int progres) {
            return progressListener.progressReached(progres);
        }
    };
}
