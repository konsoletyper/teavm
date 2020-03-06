/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.dependency;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import org.objectweb.asm.tree.ClassNode;
import org.teavm.cache.IncrementalDependencyProvider;
import org.teavm.cache.IncrementalDependencyRegistration;
import org.teavm.callgraph.CallGraph;
import org.teavm.common.CachedFunction;
import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.AnnotationReader;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.util.BasicBlockSplitter;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;
import org.teavm.parsing.Parser;

public abstract class DependencyAnalyzer implements DependencyInfo {
    private static final int PROPAGATION_STACK_THRESHOLD = 50;
    private static final MethodDescriptor CLINIT_METHOD = new MethodDescriptor("<clinit>", void.class);
    static final boolean shouldLog = System.getProperty("org.teavm.logDependencies", "false").equals("true");
    static final boolean shouldTag = System.getProperty("org.teavm.tagDependencies", "false").equals("true")
            || shouldLog;
    static final boolean dependencyReport = System.getProperty("org.teavm.dependencyReport", "false").equals("true");
    private int classNameSuffix;
    private DependencyClassSource classSource;
    ClassReaderSource agentClassSource;
    private ClassLoader classLoader;
    private Map<String, Map<MethodDescriptor, Optional<MethodHolder>>> methodReaderCache = new HashMap<>(1000, 0.5f);
    private Map<MethodReference, MethodDependency> implementationCache = new HashMap<>();
    private Function<FieldReference, FieldHolder> fieldReaderCache;
    private Map<String, Map<MethodDescriptor, MethodDependency>> methodCache = new HashMap<>();
    private Set<MethodReference> reachedMethods = new LinkedHashSet<>();
    private Set<MethodReference> readonlyReachedMethods = Collections.unmodifiableSet(reachedMethods);
    private CachedFunction<FieldReference, FieldDependency> fieldCache;
    private CachedFunction<String, ClassDependency> classCache;
    private List<DependencyListener> listeners = new ArrayList<>();
    private ServiceRepository services;
    private Deque<Transition> pendingTransitions = new ArrayDeque<>();
    private Deque<Runnable> tasks = new ArrayDeque<>();
    private Queue<Runnable> deferredTasks = new ArrayDeque<>();
    List<DependencyType> types = new ArrayList<>();
    private Map<String, DependencyType> typeMap = new HashMap<>();
    private DependencyAnalyzerInterruptor interruptor;
    private boolean interrupted;
    private Diagnostics diagnostics;
    DefaultCallGraph callGraph = new DefaultCallGraph();
    private DependencyAgent agent;
    Map<MethodReference, BootstrapMethodSubstitutor> bootstrapMethodSubstitutors = new HashMap<>();
    Map<MethodReference, DependencyPlugin> dependencyPlugins = new HashMap<>();
    private boolean completing;
    private Map<String, DependencyTypeFilter> superClassFilters = new HashMap<>();
    private List<DependencyNode> allNodes = new ArrayList<>();
    private ClassHierarchy classHierarchy;
    IncrementalCache incrementalCache = new IncrementalCache();
    boolean asyncSupported;
    private ReferenceCache referenceCache;
    private Set<String> generatedClassNames = new HashSet<>();
    DependencyType classType;

    DependencyAnalyzer(ClassReaderSource classSource, ClassLoader classLoader, ServiceRepository services,
            Diagnostics diagnostics, ReferenceCache referenceCache) {
        this.diagnostics = diagnostics;
        this.referenceCache = referenceCache;
        this.classSource = new DependencyClassSource(classSource, diagnostics, incrementalCache);
        agentClassSource = this.classSource;
        classHierarchy = new ClassHierarchy(this.classSource);
        this.classLoader = classLoader;
        this.services = services;
        fieldReaderCache = new CachedFunction<>(preimage -> this.classSource.resolveMutable(preimage));
        fieldCache = new CachedFunction<>(preimage -> {
            FieldReader field = fieldReaderCache.apply(preimage);
            if (field != null && !field.getReference().equals(preimage)) {
                return fieldCache.apply(field.getReference());
            }
            FieldDependency node = createFieldNode(preimage, field);
            if (field != null && field.getInitialValue() instanceof String) {
                node.getValue().propagate(getType("java.lang.String"));
            }
            return node;
        });

        classCache = new CachedFunction<>(this::createClassDependency);

        agent = new DependencyAgent(this);
        classType = getType("java.lang.Class");
    }

    public void setObfuscated(boolean obfuscated) {
        classSource.obfuscated = obfuscated;
    }

    public void setStrict(boolean strict) {
        classSource.strict = strict;
    }

    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    public DependencyAgent getAgent() {
        return agent;
    }

    public DependencyAnalyzerInterruptor getInterruptor() {
        return interruptor;
    }

    public void setInterruptor(DependencyAnalyzerInterruptor interruptor) {
        this.interruptor = interruptor;
    }

    public boolean wasInterrupted() {
        return interrupted;
    }

    public DependencyType getType(String name) {
        DependencyType type = typeMap.get(name);
        if (type == null) {
            type = new DependencyType(this, name, types.size());
            types.add(type);
            typeMap.put(name, type);
        }
        return type;
    }

    public DependencyNode createNode() {
        return createNode(null);
    }

    DependencyNode createNode(ValueType typeFilter) {
        if (typeFilter != null && typeFilter.isObject("java.lang.Object")) {
            typeFilter = null;
        }
        DependencyNode node = new DependencyNode(this, typeFilter);
        allNodes.add(node);
        return node;
    }

    @Override
    public ClassReaderSource getClassSource() {
        return classSource != null ? classSource : agentClassSource;
    }

    public boolean isSynthesizedClass(String className) {
        return classSource != null ? classSource.isGeneratedClass(className) : generatedClassNames.contains(className);
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String generateClassName() {
        return "$$teavm_generated_class$$" + classNameSuffix++;
    }

    public String submitClassFile(byte[] data) {
        ClassNode node = new ClassNode();
        org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(data);
        reader.accept(node, 0);
        submitClass(new Parser(referenceCache).parseClass(node));
        return node.name;
    }

    public void submitClass(ClassHolder cls) {
        if (completing) {
            throw new IllegalStateException("Can't submit class during completion phase");
        }
        classSource.submit(ModelUtils.copyClass(cls));
    }

    public void submitMethod(MethodReference methodRef, Program program) {
        if (!completing) {
            ClassHolder cls = classSource.get(methodRef.getClassName());

            if (cls == null) {
                throw new IllegalArgumentException("Class not found: " + methodRef.getClassName());
            }
            if (cls.getMethod(methodRef.getDescriptor()) != null) {
                throw new IllegalArgumentException("Method already exists: " + methodRef.getClassName());
            }
            MethodHolder method = new MethodHolder(methodRef.getDescriptor());
            method.getModifiers().add(ElementModifier.STATIC);
            method.setProgram(ProgramUtils.copy(program));
            new UnreachableBasicBlockEliminator().optimize(program);
            cls.addMethod(method);
        } else {
            MethodDependency dep = getMethod(methodRef);
            if (dep == null) {
                throw new IllegalArgumentException("Method was not reached: " + methodRef);
            }
            MethodHolder method = dep.method;

            if (!method.hasModifier(ElementModifier.NATIVE)) {
                throw new IllegalArgumentException("Method is not native: " + methodRef);
            }
            if (!dep.used) {
                return;
            }
            method.getModifiers().remove(ElementModifier.NATIVE);
            method.setProgram(ProgramUtils.copy(program));
            new UnreachableBasicBlockEliminator().optimize(method.getProgram());

            dep.used = false;
            lock(dep, false);
            deferredTasks.add(() -> {
                processInvokeDynamic(dep);
                processMethod(dep);
                dep.used = true;
            });

            processQueue();
        }
    }

    protected abstract void processMethod(MethodDependency methodDep);

    public void addDependencyListener(DependencyListener listener) {
        listeners.add(listener);
        listener.started(agent);
    }

    public void addClassTransformer(ClassHolderTransformer transformer) {
        classSource.addTransformer(transformer);
    }

    public void addEntryPoint(MethodReference methodRef, String... argumentTypes) {
        ValueType[] parameters = methodRef.getDescriptor().getParameterTypes();
        if (parameters.length + 1 != argumentTypes.length) {
            throw new IllegalArgumentException("argumentTypes length does not match the number of method's arguments");
        }
        MethodDependency method = linkMethod(methodRef);
        method.use();
        DependencyNode[] varNodes = method.getVariables();
        varNodes[0].propagate(getType(methodRef.getClassName()));
        for (int i = 0; i < argumentTypes.length; ++i) {
            varNodes[i + 1].propagate(getType(argumentTypes[i]));
        }
    }

    private int propagationDepth;

    void schedulePropagation(DependencyConsumer consumer, DependencyType type) {
        if (propagationDepth < PROPAGATION_STACK_THRESHOLD) {
            ++propagationDepth;
            consumer.consume(type);
            --propagationDepth;
        } else {
            tasks.add(() -> consumer.consume(type));
        }
    }

    void schedulePropagation(Transition consumer, DependencyType type) {
        if (!consumer.destination.filter(type)) {
            return;
        }

        if (consumer.pendingTypes == null && propagationDepth < PROPAGATION_STACK_THRESHOLD
                && consumer.pointsToDomainOrigin() && consumer.destination.propagateCount < 20) {
            ++propagationDepth;
            consumer.consume(type);
            --propagationDepth;
        } else {
            if (consumer.pendingTypes == null) {
                pendingTransitions.add(consumer);
                consumer.pendingTypes = new IntHashSet(50);
            }
            consumer.pendingTypes.add(type.index);
        }
    }

    void schedulePropagation(Transition consumer, DependencyType[] types) {
        if (types.length == 0) {
            return;
        }
        if (types.length == 1) {
            schedulePropagation(consumer, types[0]);
            return;
        }

        if (consumer.pendingTypes == null && propagationDepth < PROPAGATION_STACK_THRESHOLD
                && consumer.pointsToDomainOrigin() && consumer.destination.propagateCount < 20) {
            ++propagationDepth;
            consumer.consume(types);
            --propagationDepth;
        } else {
            if (consumer.pendingTypes == null) {
                pendingTransitions.add(consumer);
                consumer.pendingTypes = new IntHashSet(Math.max(50, types.length));
            }
            consumer.pendingTypes.ensureCapacity(types.length + consumer.pendingTypes.size());
            for (DependencyType type : types) {
                consumer.pendingTypes.add(type.index);
            }
        }
    }

    void schedulePropagation(DependencyConsumer consumer, DependencyType[] types) {
        if (types.length == 0) {
            return;
        }
        if (types.length == 1) {
            schedulePropagation(consumer, types[0]);
            return;
        }

        if (propagationDepth < PROPAGATION_STACK_THRESHOLD) {
            ++propagationDepth;
            for (DependencyType type : types) {
                consumer.consume(type);
            }
            --propagationDepth;
        } else {
            tasks.add(() -> {
                for (DependencyType type : types) {
                    consumer.consume(type);
                }
            });
        }
    }

    public void defer(Runnable task) {
        deferredTasks.add(task);
    }

    public ClassDependency linkClass(String className) {
        if (completing && getClass(className) == null) {
            throw new IllegalStateException("Can't link class during completion phase");
        }
        ClassDependency dep = classCache.apply(className);
        if (!dep.activated) {
            dep.activated = true;
            if (!dep.isMissing()) {
                deferredTasks.add(() -> {
                    for (DependencyListener listener : listeners) {
                        listener.classReached(agent, className);
                    }
                });

                ClassReader cls = dep.getClassReader();
                if (cls.getParent() != null && !classCache.caches(cls.getParent())) {
                    linkClass(cls.getParent());
                }
                for (String iface : cls.getInterfaces()) {
                    if (!classCache.caches(iface)) {
                        linkClass(iface);
                    }
                }
            }
        }

        return dep;
    }

    private ClassDependency createClassDependency(String className) {
        ClassReader cls = classSource.get(className);
        return new ClassDependency(this, className, cls);
    }

    public MethodDependency linkMethod(String className, MethodDescriptor descriptor) {
        MethodDependency dep = getMethodDependency(className, descriptor);

        if (!dep.activated) {
            reachedMethods.add(dep.getReference());
            dep.activated = true;
            if (!dep.isMissing()) {
                for (DependencyListener listener : listeners) {
                    listener.methodReached(agent, dep);
                }
                activateDependencyPlugin(dep);
            }
        }
        return dep;
    }

    public MethodDependency linkMethod(MethodReference method) {
        return linkMethod(method.getClassName(), method.getDescriptor());
    }

    void initClass(ClassDependency cls, CallLocation location) {
        ClassReader reader = cls.getClassReader();
        MethodReader method = reader.getMethod(CLINIT_METHOD);
        if (method != null) {
            deferredTasks.add(() -> {
                MethodDependency initMethod = linkMethod(method.getReference());
                if (location != null) {
                    initMethod.addLocation(location);
                }
                initMethod.use();
            });
        }
    }

    private MethodDependency createMethodDep(MethodReference methodRef, MethodHolder method) {
        ValueType[] arguments = methodRef.getParameterTypes();
        int paramCount = arguments.length + 1;
        DependencyNode[] parameterNodes = new DependencyNode[arguments.length + 1];

        parameterNodes[0] = createParameterNode(methodRef, ValueType.object(methodRef.getClassName()), 0);
        for (int i = 0; i < arguments.length; ++i) {
            parameterNodes[i + 1] = createParameterNode(methodRef, arguments[i], i + 1);
        }

        DependencyNode resultNode;
        if (methodRef.getDescriptor().getResultType() == ValueType.VOID) {
            resultNode = null;
        } else {
            resultNode = createResultNode(methodRef);
        }
        DependencyNode thrown = createThrownNode(methodRef);
        MethodDependency dep = new MethodDependency(this, parameterNodes, paramCount, resultNode, thrown,
                method, methodRef);
        if (method != null) {
            deferredTasks.add(() -> linkClass(dep.getMethod().getOwnerName())
                    .initClass(new CallLocation(dep.getMethod().getReference())));
        }
        return dep;
    }

    abstract DependencyNode createParameterNode(MethodReference method, ValueType type, int index);

    abstract DependencyNode createResultNode(MethodReference method);

    abstract DependencyNode createThrownNode(MethodReference method);

    abstract DependencyNode createFieldNode(FieldReference field, ValueType type);

    abstract DependencyNode createArrayItemNode(DependencyNode parent);

    abstract DependencyNode createClassValueNode(int degree, DependencyNode parent);

    void scheduleMethodAnalysis(MethodDependency dep) {
        deferredTasks.add(() -> {
            processInvokeDynamic(dep);
            processMethod(dep);
        });
    }

    @Override
    public Collection<MethodReference> getReachableMethods() {
        return readonlyReachedMethods;
    }

    @Override
    public Collection<FieldReference> getReachableFields() {
        return fieldCache.getCachedPreimages();
    }

    @Override
    public Collection<String> getReachableClasses() {
        return classCache.getCachedPreimages();
    }

    public FieldDependency linkField(FieldReference fieldRef) {
        FieldDependency dep = fieldCache.apply(fieldRef);
        if (!dep.activated) {
            dep.activated = true;
            if (!dep.isMissing()) {
                for (DependencyListener listener : listeners) {
                    listener.fieldReached(agent, dep);
                }
            }
        }
        return dep;
    }

    @Override
    public FieldDependency getField(FieldReference fieldRef) {
        return fieldCache.getKnown(fieldRef);
    }

    @Override
    public ClassDependency getClass(String className) {
        return classCache.getKnown(className);
    }

    private FieldDependency createFieldNode(FieldReference fieldRef, FieldReader field) {
        DependencyNode node = createFieldNode(fieldRef, field != null ? field.getType() : null);
        return new FieldDependency(node, field, fieldRef);
    }

    private void activateDependencyPlugin(MethodDependency methodDep) {
        attachDependencyPlugin(methodDep);
        if (methodDep.dependencyPlugin != null) {
            methodDep.dependencyPlugin.methodReached(agent, methodDep);
        }
    }

    private void attachDependencyPlugin(MethodDependency methodDep) {
        if (methodDep.dependencyPluginAttached) {
            return;
        }
        methodDep.dependencyPluginAttached = true;

        methodDep.dependencyPlugin = dependencyPlugins.get(methodDep.getReference());
        if (methodDep.dependencyPlugin != null || isBootstrap()) {
            return;
        }

        AnnotationReader depAnnot = methodDep.getMethod().getAnnotations().get(PluggableDependency.class.getName());
        if (depAnnot == null) {
            return;
        }
        ValueType depType = depAnnot.getValue("value").getJavaClass();
        String depClassName = ((ValueType.Object) depType).getClassName();
        Class<?> depClass;
        try {
            depClass = Class.forName(depClassName, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Dependency plugin not found: " + depClassName, e);
        }
        try {
            methodDep.dependencyPlugin = (DependencyPlugin) depClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Can't instantiate dependency plugin " + depClassName, e);
        }
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }

    @Override
    public MethodDependency getMethod(MethodReference methodRef) {
        return getMethod(methodRef.getClassName(), methodRef.getDescriptor());
    }

    public MethodDependency getMethod(String className, MethodDescriptor descriptor) {
        Map<MethodDescriptor, MethodDependency> map = methodCache.get(className);
        if (map == null) {
            return null;
        }
        return map.get(descriptor);
    }

    @Override
    public MethodDependency getMethodImplementation(MethodReference methodRef) {
        return implementationCache.computeIfAbsent(methodRef, m -> {
            MethodReader resolved = agentClassSource.resolveImplementation(m);
            return resolved != null ? getMethod(resolved.getReference()) : null;
        });
    }

    private MethodHolder getMethodHolder(String className, MethodDescriptor descriptor) {
        return methodReaderCache
                .computeIfAbsent(className, k -> new HashMap<>(100, 0.5f))
                .computeIfAbsent(descriptor, k -> Optional.ofNullable(
                        classSource.resolveMutableImplementation(className, k)))
                .orElse(null);
    }

    private MethodDependency getMethodDependency(String className, MethodDescriptor descriptor) {
        Map<MethodDescriptor, MethodDependency> map = methodCache.computeIfAbsent(className, k -> new HashMap<>());
        MethodDependency result = map.get(descriptor);
        if (result == null) {
            MethodHolder method = getMethodHolder(className, descriptor);
            if (method != null && !(method.getDescriptor().equals(descriptor)
                    && method.getOwnerName().equals(className))) {
                result = getMethodDependency(method.getOwnerName(), method.getDescriptor());
            } else {
                MethodReference reference = method != null
                        ? method.getReference()
                        : new MethodReference(className, descriptor);
                result = createMethodDep(reference, method);
            }
            map.put(descriptor, result);
        }
        return result;
    }

    private void processQueue() {
        if (interrupted) {
            return;
        }
        while (!deferredTasks.isEmpty() || !tasks.isEmpty() || !pendingTransitions.isEmpty()) {
            while (true) {
                processNodeToNodeTransitionQueue();
                if (tasks.isEmpty()) {
                    break;
                }
                while (!tasks.isEmpty()) {
                    tasks.remove().run();
                }
                if (interruptor != null && !interruptor.shouldContinue()) {
                    interrupted = true;
                    return;
                }
            }

            propagationDepth = PROPAGATION_STACK_THRESHOLD;
            while (!deferredTasks.isEmpty()) {
                deferredTasks.remove().run();
            }
            propagationDepth = 0;
        }
    }

    private void processNodeToNodeTransitionQueue() {
        while (!pendingTransitions.isEmpty()) {
            Transition transition = pendingTransitions.remove();
            IntSet pendingTypes = transition.pendingTypes;
            transition.pendingTypes = null;
            if (pendingTypes.size() == 1) {
                DependencyType type = types.get(pendingTypes.iterator().next().value);
                transition.consume(type);
            } else {
                DependencyType[] typesToPropagate = new DependencyType[pendingTypes.size()];
                int index = 0;
                for (IntCursor cursor : pendingTypes) {
                    typesToPropagate[index++] = types.get(cursor.value);
                }
                transition.consume(typesToPropagate);
            }
        }
    }

    public void processDependencies() {
        interrupted = false;
        processQueue();
        if (!interrupted) {
            completing = true;
            lock();
            for (DependencyListener listener : listeners) {
                listener.completing(agent);
            }
        }

        for (DependencyListener listener : listeners) {
            listener.complete();
        }

        if (dependencyReport) {
            reportDependencies();
        }
    }

    private void reportDependencies() {
        List<ReportEntry> report = new ArrayList<>();
        int domainCount = 0;
        for (DependencyNode node : allNodes) {
            String tag = node.tag + "";
            if (node.typeSet != null && node.typeSet.origin == node) {
                ++domainCount;
                tag += "{*}";
            }
            report.add(new ReportEntry(tag, node.getTypes().length));
        }

        report.sort(Comparator.comparingInt(n -> -n.count));
        for (ReportEntry entry : report) {
            System.out.println(entry.title + ": " + entry.count);
        }

        System.out.println("Total nodes: " + allNodes.size());
        System.out.println("Total domains: " + domainCount);
    }

    public void cleanup(ClassSourcePacker classSourcePacker) {
        for (DependencyNode node : allNodes) {
            node.followers = null;
            node.transitions = null;
            node.transitionList = null;
            node.method = null;
        }

        for (DependencyNode node : allNodes) {
            if (node.typeSet != null) {
                node.typeSet.cleanup();
            }
        }

        for (Map<?, MethodDependency> map : methodCache.values()) {
            for (MethodDependency methodDependency : map.values()) {
                methodDependency.locationListeners = null;
                methodDependency.locations = null;
                methodDependency.cleanup();
            }
        }

        for (FieldReference fieldRef : fieldCache.getCachedPreimages()) {
            FieldDependency field = fieldCache.getKnown(fieldRef);
            if (field != null) {
                field.locationListeners = null;
                field.locations = null;
                field.cleanup();
            }
        }

        for (String className : classCache.getCachedPreimages()) {
            ClassDependency cls = classCache.getKnown(className);
            cls.cleanup();
        }

        allNodes.clear();
        classSource.cleanup();
        agent.cleanup();
        listeners.clear();
        classSource.innerHierarchy = null;

        agentClassSource = classSourcePacker.pack(classSource,
                ClassClosureAnalyzer.build(classSource, new ArrayList<>(classSource.cache.keySet())));
        if (classSource != agentClassSource) {
            classHierarchy = new ClassHierarchy(agentClassSource);
            generatedClassNames.addAll(classSource.getGeneratedClassNames());
        }
        classSource = null;
        methodReaderCache = null;
        fieldReaderCache = null;
    }

    public void cleanupTypes() {
        for (MethodReference reachableMethod : getReachableMethods()) {
            MethodDependency dependency = getMethod(reachableMethod);
            for (int i = dependency.getParameterCount() + 1; i < dependency.getVariableCount(); ++i) {
                dependency.variableNodes[i] = null;
            }
        }
    }

    static class ReportEntry {
        String title;
        int count;

        ReportEntry(String title, int count) {
            this.title = title;
            this.count = count;
        }
    }

    private void lock() {
        for (MethodReference method : getReachableMethods()) {
            lock(getMethod(method), true);
        }
        for (FieldReference field : getReachableFields()) {
            lock(getField(field));
        }
    }

    private void lock(MethodDependency dep, boolean lock) {
        for (DependencyNode node : dep.variableNodes) {
            if (node != null) {
                node.locked = lock;
            }
        }
        if (dep.resultNode != null) {
            dep.resultNode.locked = lock;
        }
        if (dep.thrown != null) {
            dep.thrown.locked = lock;
        }
    }

    private void lock(FieldDependency dep) {
        dep.value.locked = true;
    }

    public <T> T getService(Class<T> type) {
        return services.getService(type);
    }

    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    @Override
    public CallGraph getCallGraph() {
        return callGraph;
    }

    public void addBootstrapMethodSubstitutor(MethodReference method, BootstrapMethodSubstitutor substitutor) {
        bootstrapMethodSubstitutors.put(method, substitutor);
    }

    public void addDependencyPlugin(MethodReference method, DependencyPlugin dependencyPlugin) {
        dependencyPlugins.put(method, dependencyPlugin);
    }

    public IncrementalDependencyProvider getIncrementalDependencies() {
        return incrementalCache;
    }

    DependencyTypeFilter getSuperClassFilter(String superClass) {
        DependencyTypeFilter result = superClassFilters.get(superClass);
        if (result == null) {
            if (superClass.startsWith("[")) {
                char second = superClass.charAt(1);
                if (second == '[') {
                    result = new SuperArrayFilter(this, getSuperClassFilter(superClass.substring(1)));
                } else if (second == 'L') {
                    ValueType.Object itemType = (ValueType.Object) ValueType.parse(superClass.substring(1));
                    result = new SuperArrayFilter(this, getSuperClassFilter(itemType.getClassName()));
                } else {
                    result = new ExactTypeFilter(getType(superClass));
                }
            } else {
                if (superClass.equals("java.lang.Object")) {
                    result = t -> true;
                } else {
                    result = new SuperClassFilter(this, getType(superClass));
                }
            }
            superClassFilters.put(superClass, result);
        }
        return result;
    }

    private void processInvokeDynamic(MethodDependency methodDep) {
        if (methodDep.method == null) {
            return;
        }

        Program program = methodDep.method.getProgram();
        if (program == null) {
            return;
        }

        ProgramEmitter pe = ProgramEmitter.create(program, classHierarchy);
        BasicBlockSplitter splitter = new BasicBlockSplitter(program);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (!(insn instanceof InvokeDynamicInstruction)) {
                    continue;
                }
                block = insn.getBasicBlock();

                InvokeDynamicInstruction indy = (InvokeDynamicInstruction) insn;
                MethodReference bootstrapMethod = new MethodReference(indy.getBootstrapMethod().getClassName(),
                        indy.getBootstrapMethod().getName(), indy.getBootstrapMethod().signature());
                BootstrapMethodSubstitutor substitutor = bootstrapMethodSubstitutors.get(bootstrapMethod);
                if (substitutor == null) {
                    NullConstantInstruction nullInsn = new NullConstantInstruction();
                    nullInsn.setReceiver(indy.getReceiver());
                    nullInsn.setLocation(indy.getLocation());
                    insn.replace(nullInsn);
                    CallLocation location = new CallLocation(methodDep.getReference(), insn.getLocation());
                    diagnostics.error(location, "Substitutor for bootstrap method {{m0}} was not found",
                            bootstrapMethod);
                    continue;
                }

                BasicBlock splitBlock = splitter.split(block, insn);

                pe.enter(block);
                pe.setCurrentLocation(indy.getLocation());
                insn.delete();

                List<ValueEmitter> arguments = new ArrayList<>();
                for (int k = 0; k < indy.getArguments().size(); ++k) {
                    arguments.add(pe.var(indy.getArguments().get(k), indy.getMethod().parameterType(k)));
                }
                DynamicCallSite callSite = new DynamicCallSite(
                        methodDep.getReference(), indy.getMethod(),
                        indy.getInstance() != null ? pe.var(indy.getInstance(),
                                ValueType.object(methodDep.getMethod().getOwnerName())) : null,
                        arguments, indy.getBootstrapMethod(), indy.getBootstrapArguments(),
                        agent);
                ValueEmitter result = substitutor.substitute(callSite, pe);
                if (result.getVariable() != null && result.getVariable() != indy.getReceiver()) {
                    AssignInstruction assign = new AssignInstruction();
                    assign.setAssignee(result.getVariable());
                    assign.setReceiver(indy.getReceiver());
                    pe.addInstruction(assign);
                }
                pe.jump(splitBlock);
            }
        }
        splitter.fixProgram();
    }

    static class IncrementalCache implements IncrementalDependencyProvider, IncrementalDependencyRegistration {
        private final String[] emptyArray = new String[0];
        private Map<String, IncrementalItem> classes = new HashMap<>();
        private Map<MethodReference, IncrementalItem> methods = new HashMap<>();

        @Override
        public boolean isNoCache(String className) {
            IncrementalItem item = classes.get(className);
            return item != null && item.noCache;
        }

        @Override
        public boolean isNoCache(MethodReference method) {
            IncrementalItem item = methods.get(method);
            return item != null && item.noCache;
        }

        @Override
        public String[] getDependencies(String className) {
            IncrementalItem item = classes.get(className);
            return item != null && item.dependencies != null ? item.dependencies.toArray(new String[0]) : emptyArray;
        }

        @Override
        public String[] getDependencies(MethodReference method) {
            IncrementalItem item = methods.get(method);
            return item != null && item.dependencies != null ? item.dependencies.toArray(new String[0]) : emptyArray;
        }

        @Override
        public void setNoCache(String className) {
            classes.computeIfAbsent(className, k -> new IncrementalItem()).noCache = true;
        }

        @Override
        public void setNoCache(MethodReference method) {
            methods.computeIfAbsent(method, k -> new IncrementalItem()).noCache = true;
        }

        @Override
        public void addDependencies(String className, String... dependencies) {
            IncrementalItem item = classes.computeIfAbsent(className, k -> new IncrementalItem());
            if (item.dependencies == null) {
                item.dependencies = new LinkedHashSet<>();
            }
            item.dependencies.addAll(Arrays.asList(dependencies));
        }

        @Override
        public void addDependencies(MethodReference method, String... dependencies) {
            IncrementalItem item = this.methods.computeIfAbsent(method, k -> new IncrementalItem());
            if (item.dependencies == null) {
                item.dependencies = new LinkedHashSet<>();
            }
            item.dependencies.addAll(Arrays.asList(dependencies));
        }
    }

    static class IncrementalItem {
        boolean noCache;
        Set<String> dependencies;
    }

    abstract boolean domainOptimizationEnabled();
}