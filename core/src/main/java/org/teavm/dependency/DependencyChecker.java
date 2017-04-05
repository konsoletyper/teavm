/*
 *  Copyright 2012 Alexey Andreev.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.DefaultCallGraph;
import org.teavm.callgraph.DefaultCallGraphNode;
import org.teavm.common.CachedMapper;
import org.teavm.common.Mapper;
import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AnnotationReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ReferenceCache;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;
import org.teavm.parsing.Parser;

public class DependencyChecker implements DependencyInfo {
    static final boolean shouldLog = System.getProperty("org.teavm.logDependencies", "false").equals("true");
    private int classNameSuffix;
    private DependencyClassSource classSource;
    private ClassLoader classLoader;
    private Mapper<MethodReference, MethodHolder> methodReaderCache;
    private Mapper<FieldReference, FieldHolder> fieldReaderCache;
    private CachedMapper<MethodReference, MethodDependency> methodCache;
    private CachedMapper<FieldReference, FieldDependency> fieldCache;
    private CachedMapper<String, ClassDependency> classCache;
    private List<DependencyListener> listeners = new ArrayList<>();
    private ServiceRepository services;
    private Queue<Runnable> tasks = new ArrayDeque<>();
    List<DependencyType> types = new ArrayList<>();
    private Map<String, DependencyType> typeMap = new HashMap<>();
    private DependencyCheckerInterruptor interruptor;
    private boolean interrupted;
    private Diagnostics diagnostics;
    DefaultCallGraph callGraph = new DefaultCallGraph();
    private DependencyAgent agent;
    Map<MethodReference, BootstrapMethodSubstitutor> bootstrapMethodSubstitutors = new HashMap<>();
    private boolean completing;

    public DependencyChecker(ClassReaderSource classSource, ClassLoader classLoader, ServiceRepository services,
            Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
        this.classSource = new DependencyClassSource(classSource, diagnostics);
        this.classLoader = classLoader;
        this.services = services;
        methodReaderCache = new CachedMapper<>(preimage -> this.classSource.resolveMutable(preimage));
        fieldReaderCache = new CachedMapper<>(preimage -> this.classSource.resolveMutable(preimage));
        methodCache = new CachedMapper<>(preimage ->  {
            MethodHolder method = methodReaderCache.map(preimage);
            if (method != null && !method.getReference().equals(preimage)) {
                return methodCache.map(method.getReference());
            }
            return createMethodDep(preimage, method);
        });
        fieldCache = new CachedMapper<>(preimage -> {
            FieldReader field = fieldReaderCache.map(preimage);
            if (field != null && !field.getReference().equals(preimage)) {
                return fieldCache.map(field.getReference());
            }
            return createFieldNode(preimage, field);
        });

        classCache = new CachedMapper<>(this::createClassDependency);

        agent = new DependencyAgent(this);
    }

    public DependencyAgent getAgent() {
        return agent;
    }

    public DependencyCheckerInterruptor getInterruptor() {
        return interruptor;
    }

    public void setInterruptor(DependencyCheckerInterruptor interruptor) {
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
        return new DependencyNode(this);
    }

    @Override
    public ClassReaderSource getClassSource() {
        return classSource;
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
        submitClass(new Parser(new ReferenceCache()).parseClass(node));
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
            tasks.add(() -> {
                DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(DependencyChecker.this);
                graphBuilder.buildGraph(dep);
                dep.used = true;
            });

            processQueue();
        }
    }

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
        MethodDependency method = linkMethod(methodRef, null);
        method.use();
        DependencyNode[] varNodes = method.getVariables();
        varNodes[0].propagate(getType(methodRef.getClassName()));
        for (int i = 0; i < argumentTypes.length; ++i) {
            varNodes[i + 1].propagate(getType(argumentTypes[i]));
        }
    }

    private int propagationDepth;

    void schedulePropagation(DependencyConsumer consumer, DependencyType type) {
        if (propagationDepth < 50) {
            ++propagationDepth;
            consumer.consume(type);
            --propagationDepth;
        } else {
            tasks.add(() -> consumer.consume(type));
        }
    }

    void schedulePropagation(DependencyConsumer consumer, DependencyType[] types) {
        if (propagationDepth < 50) {
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

    private Set<String> classesAddedByRoot = new HashSet<>();

    public ClassDependency linkClass(String className, CallLocation callLocation) {
        if (completing && getClass(className) == null) {
            throw new IllegalStateException("Can't link class during completion phase");
        }
        ClassDependency dep = classCache.map(className);
        boolean added = true;
        if (callLocation != null && callLocation.getMethod() != null) {
            DefaultCallGraphNode callGraphNode = callGraph.getNode(callLocation.getMethod());
            if (!addClassAccess(callGraphNode, className, callLocation.getSourceLocation())) {
                added = false;
            }
        } else {
            added = classesAddedByRoot.add(className);
        }
        if (!dep.isMissing() && added) {
            tasks.add(() -> {
                for (DependencyListener listener : listeners) {
                    listener.classReached(agent, className, callLocation);
                }
            });
        }
        return dep;
    }

    private boolean addClassAccess(DefaultCallGraphNode node, String className, TextLocation loc) {
        if (!node.addClassAccess(className, loc)) {
            return false;
        }
        ClassReader cls = classSource.get(className);
        if (cls != null) {
            if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
                addClassAccess(node, cls.getParent(), loc);
            }
            for (String iface : cls.getInterfaces()) {
                addClassAccess(node, iface, loc);
            }
        }
        return true;
    }

    private ClassDependency createClassDependency(String className) {
        ClassReader cls = classSource.get(className);
        ClassDependency dependency = new ClassDependency(this, className, cls);
        if (!dependency.isMissing()) {
            if (cls.getParent() != null && !cls.getParent().equals(className)) {
                linkClass(cls.getParent(), null);
            }
            for (String ifaceName : cls.getInterfaces()) {
                linkClass(ifaceName, null);
            }
        }
        return dependency;
    }

    private Set<MethodReference> methodsAddedByRoot = new HashSet<>();

    public MethodDependency linkMethod(MethodReference methodRef, CallLocation callLocation) {
        if (methodRef == null) {
            throw new IllegalArgumentException();
        }
        MethodReader methodReader = methodReaderCache.map(methodRef);
        if (methodReader != null) {
            methodRef = methodReader.getReference();
        }

        if (completing && getMethod(methodRef) == null) {
            throw new IllegalStateException("Can't submit class during completion phase");
        }
        callGraph.getNode(methodRef);
        boolean added;
        if (callLocation != null && callLocation.getMethod() != null) {
            added = callGraph.getNode(callLocation.getMethod()).addCallSite(methodRef,
                    callLocation.getSourceLocation());
        } else {
            added = methodsAddedByRoot.add(methodRef);
        }
        MethodDependency graph = methodCache.map(methodRef);
        if (!graph.isMissing() && added) {
            for (DependencyListener listener : listeners) {
                listener.methodReached(agent, graph, callLocation);
            }
            activateDependencyPlugin(graph, callLocation);
        }
        return graph;
    }

    void initClass(ClassDependency cls, CallLocation callLocation) {
        ClassReader reader = cls.getClassReader();
        MethodReader method = reader.getMethod(new MethodDescriptor("<clinit>", void.class));
        if (method != null) {
            tasks.add(() -> linkMethod(method.getReference(), callLocation).use());
        }
    }

    private MethodDependency createMethodDep(MethodReference methodRef, MethodHolder method) {
        ValueType[] arguments = methodRef.getParameterTypes();
        int paramCount = arguments.length + 1;
        DependencyNode[] parameterNodes = new DependencyNode[arguments.length + 1];
        for (int i = 0; i < parameterNodes.length; ++i) {
            parameterNodes[i] = createNode();
            parameterNodes[i].method = methodRef;
            if (shouldLog) {
                parameterNodes[i].setTag(methodRef + ":" + i);
            }
        }
        DependencyNode resultNode;
        if (methodRef.getDescriptor().getResultType() == ValueType.VOID) {
            resultNode = null;
        } else {
            resultNode = createNode();
            resultNode.method = methodRef;
            if (shouldLog) {
                resultNode.setTag(methodRef + ":RESULT");
            }
        }
        DependencyNode thrown = createNode();
        thrown.method = methodRef;
        if (shouldLog) {
            thrown.setTag(methodRef + ":THROWN");
        }
        MethodDependency dep = new MethodDependency(this, parameterNodes, paramCount, resultNode, thrown,
                method, methodRef);
        if (method != null) {
            tasks.add(() -> {
                CallLocation caller = new CallLocation(dep.getMethod().getReference());
                linkClass(dep.getMethod().getOwnerName(), caller).initClass(caller);
            });
        }
        return dep;
    }

    void scheduleMethodAnalysis(MethodDependency dep) {
        tasks.add(() -> {
            DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(DependencyChecker.this);
            graphBuilder.buildGraph(dep);
        });
    }

    @Override
    public Collection<MethodReference> getReachableMethods() {
        return methodCache.getCachedPreimages();
    }

    @Override
    public Collection<FieldReference> getReachableFields() {
        return fieldCache.getCachedPreimages();
    }

    @Override
    public Collection<String> getReachableClasses() {
        return classCache.getCachedPreimages();
    }

    private Set<FieldReference> fieldsAddedByRoot = new HashSet<>();

    public FieldDependency linkField(FieldReference fieldRef, CallLocation location) {
        if (completing) {
            throw new IllegalStateException("Can't submit class during completion phase");
        }
        boolean added;
        if (location != null) {
            added = callGraph.getNode(location.getMethod()).addFieldAccess(fieldRef, location.getSourceLocation());
        } else {
            added = fieldsAddedByRoot.add(fieldRef);
        }
        FieldDependency dep = fieldCache.map(fieldRef);
        if (!dep.isMissing()) {
            tasks.add(() -> linkClass(fieldRef.getClassName(), location).initClass(location));
        }
        if (!dep.isMissing() && added) {
            for (DependencyListener listener : listeners) {
                listener.fieldReached(agent, dep, location);
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
        DependencyNode node = createNode();
        if (shouldLog) {
            node.setTag(fieldRef.getClassName() + "#" + fieldRef.getFieldName());
        }
        FieldDependency dep = new FieldDependency(node, field, fieldRef);
        if (!dep.isMissing()) {
            tasks.add(() -> linkClass(fieldRef.getClassName(), null).initClass(null));
        }
        return dep;
    }

    private void activateDependencyPlugin(MethodDependency methodDep, CallLocation location) {
        attachDependencyPlugin(methodDep);
        if (methodDep.dependencyPlugin != null) {
            methodDep.dependencyPlugin.methodReached(agent, methodDep, location);
        }
    }

    private void attachDependencyPlugin(MethodDependency methodDep) {
        if (methodDep.dependencyPluginAttached) {
            return;
        }
        methodDep.dependencyPluginAttached = true;
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

    @Override
    public MethodDependency getMethod(MethodReference methodRef) {
        return methodCache.getKnown(methodRef);
    }

    @Override
    public MethodDependency getMethodImplementation(MethodReference methodRef) {
        MethodReader method = methodReaderCache.map(methodRef);
        return method != null ? methodCache.getKnown(method.getReference()) : null;
    }

    private void processQueue() {
        if (interrupted) {
            return;
        }
        int index = 0;
        while (!tasks.isEmpty()) {
            tasks.poll().run();
            if (++index == 100) {
                if (interruptor != null && !interruptor.shouldContinue()) {
                    interrupted = true;
                    break;
                }
                index = 0;
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
}