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

import java.io.IOException;
import java.util.*;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.DefaultCallGraph;
import org.teavm.common.*;
import org.teavm.common.CachedMapper.KeyListener;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;
import org.teavm.model.util.ModelUtils;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyChecker implements DependencyInfo, DependencyAgent {
    static final boolean shouldLog = System.getProperty("org.teavm.logDependencies", "false").equals("true");
    private int classNameSuffix;
    private DependencyClassSource classSource;
    private ClassLoader classLoader;
    private Mapper<MethodReference, MethodReader> methodReaderCache;
    private Mapper<FieldReference, FieldReader> fieldReaderCache;
    private CachedMapper<MethodReference, MethodDependency> methodCache;
    private CachedMapper<FieldReference, FieldDependency> fieldCache;
    private CachedMapper<String, ClassDependency> classCache;
    private List<DependencyListener> listeners = new ArrayList<>();
    private ServiceRepository services;
    private Queue<Runnable> tasks = new ArrayDeque<>();
    Set<MethodDependency> missingMethods = new HashSet<>();
    Set<ClassDependency> missingClasses = new HashSet<>();
    Set<FieldDependency> missingFields = new HashSet<>();
    List<DependencyType> types = new ArrayList<>();
    Map<String, DependencyType> typeMap = new HashMap<>();
    private DependencyCheckerInterruptor interruptor;
    private boolean interrupted;
    private Diagnostics diagnostics;
    private DefaultCallGraph callGraph = new DefaultCallGraph();

    public DependencyChecker(ClassReaderSource classSource, ClassLoader classLoader, ServiceRepository services,
            Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
        this.classSource = new DependencyClassSource(classSource, diagnostics);
        this.classLoader = classLoader;
        this.services = services;
        methodReaderCache = new CachedMapper<>(new Mapper<MethodReference, MethodReader>() {
            @Override public MethodReader map(MethodReference preimage) {
                return findMethodReader(preimage);
            }
        });
        fieldReaderCache = new CachedMapper<>(new Mapper<FieldReference, FieldReader>() {
            @Override public FieldReader map(FieldReference preimage) {
                return findFieldReader(preimage);
            }
        });
        methodCache = new CachedMapper<>(new Mapper<MethodReference, MethodDependency>() {
            @Override public MethodDependency map(MethodReference preimage) {
                MethodReader method = methodReaderCache.map(preimage);
                if (method != null && !method.getReference().equals(preimage)) {
                    return methodCache.map(method.getReference());
                }
                return createMethodDep(preimage, method);
            }
        });
        fieldCache = new CachedMapper<>(new Mapper<FieldReference, FieldDependency>() {
            @Override public FieldDependency map(FieldReference preimage) {
                FieldReader field = fieldReaderCache.map(preimage);
                if (field != null && !field.getReference().equals(preimage)) {
                    return fieldCache.map(field.getReference());
                }
                return createFieldNode(preimage, field);
            }
        });

        classCache = new CachedMapper<>(new Mapper<String, ClassDependency>() {
            @Override public ClassDependency map(String preimage) {
                return createClassDependency(preimage);
            }
        });
        methodCache.addKeyListener(new KeyListener<MethodReference>() {
            @Override public void keyAdded(MethodReference key) {
                MethodDependency graph = methodCache.getKnown(key);
                if (!graph.isMissing()) {
                    for (DependencyListener listener : listeners) {
                        listener.methodAchieved(DependencyChecker.this, graph);
                    }
                    activateDependencyPlugin(graph);
                }
            }
        });
        fieldCache.addKeyListener(new KeyListener<FieldReference>() {
            @Override public void keyAdded(FieldReference key) {
                FieldDependency fieldDep = fieldCache.getKnown(key);
                if (!fieldDep.isMissing()) {
                    for (DependencyListener listener : listeners) {
                        listener.fieldAchieved(DependencyChecker.this, fieldDep);
                    }
                }
            }
        });
        classCache.addKeyListener(new KeyListener<String>() {
            @Override public void keyAdded(String key) {
                ClassDependency classDep = classCache.getKnown(key);
                if (!classDep.isMissing()) {
                    for (DependencyListener listener : listeners) {
                        listener.classAchieved(DependencyChecker.this, key);
                    }
                }
            }
        });
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

    @Override
    public DependencyType getType(String name) {
        DependencyType type = typeMap.get(name);
        if (type == null) {
            type = new DependencyType(this, name, types.size());
            types.add(type);
            typeMap.put(name, type);
        }
        return type;
    }

    @Override
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

    @Override
    public String generateClassName() {
        return "$$teavm_generated_class$$" + classNameSuffix++;
    }

    @Override
    public void submitClass(ClassHolder cls) {
        classSource.submit(ModelUtils.copyClass(cls));
    }

    public void addDependencyListener(DependencyListener listener) {
        listeners.add(listener);
        listener.started(this);
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

    void schedulePropagation(final DependencyConsumer consumer, final DependencyType type) {
        tasks.add(new Runnable() {
            @Override public void run() {
                consumer.consume(type);
            }
        });
    }

    void schedulePropagation(final DependencyConsumer consumer, final DependencyType[] types) {
        tasks.add(new Runnable() {
            @Override public void run() {
                for (DependencyType type : types) {
                    consumer.consume(type);
                }
            }
        });
    }

    @Override
    public ClassDependency linkClass(String className, CallLocation callLocation) {
        return classCache.map(className);
    }

    private ClassDependency createClassDependency(String className, CallLocation callLocation) {
        ClassReader cls = classSource.get(className);
        ClassDependency dependency = new ClassDependency(this, className, stack, cls);
        if (dependency.isMissing()) {
            missingClasses.add(dependency);
        } else {
            if (cls.getParent() != null && !cls.getParent().equals(className)) {
                linkClass(cls.getParent(), stack);
            }
            for (String ifaceName : cls.getInterfaces()) {
                linkClass(ifaceName, stack);
            }
        }
        return dependency;
    }

    @Override
    public MethodDependency linkMethod(MethodReference methodRef, CallLocation callLocation) {
        if (methodRef == null) {
            throw new IllegalArgumentException();
        }
        callGraph.getNode(methodRef);
        if (callLocation != null) {
            callGraph.addNode(caller);
            callGraph.getNode(caller).addCallSite(methodRef, location);
        }
        return methodCache.map(methodRef);
    }

    void initClass(ClassDependency cls, final CallLocation callLocation) {
        ClassReader reader = cls.getClassReader();
        final MethodReader method = reader.getMethod(new MethodDescriptor("<clinit>", void.class));
        if (method != null) {
            tasks.add(new Runnable() {
                @Override public void run() {
                    linkMethod(method.getReference(), callLocation).use();
                }
            });
        }
    }

    private MethodReader findMethodReader(MethodReference methodRef) {
        String clsName = methodRef.getClassName();
        MethodDescriptor desc = methodRef.getDescriptor();
        ClassReader cls = classSource.get(clsName);
        if (cls == null) {
            return null;
        }
        MethodReader reader = cls.getMethod(desc);
        if (reader != null) {
            return reader;
        }
        if (cls.getParent() != null && cls.getParent().equals(cls.getParent())) {
            reader = methodReaderCache.map(new MethodReference(cls.getParent(), desc));
            if (reader != null) {
                return reader;
            }
        }
        for (String ifaceName : cls.getInterfaces()) {
            reader = methodReaderCache.map(new MethodReference(ifaceName, desc));
            if (reader != null) {
                return reader;
            }
        }
        return null;
    }

    private FieldReader findFieldReader(FieldReference fieldRef) {
        String clsName = fieldRef.getClassName();
        String name = fieldRef.getFieldName();
        while (clsName != null) {
            ClassReader cls = classSource.get(clsName);
            if (cls == null) {
                return null;
            }
            FieldReader field = cls.getField(name);
            if (field != null) {
                return field;
            }
            if (clsName.equals(cls.getParent())) {
                break;
            }
            clsName = cls.getParent();
        }
        return null;
    }

    private MethodDependency createMethodDep(MethodReference methodRef, MethodReader method, DependencyStack stack) {
        if (stack == null) {
            stack = DependencyStack.ROOT;
        }
        ValueType[] arguments = methodRef.getParameterTypes();
        int paramCount = arguments.length + 1;
        int varCount = Math.max(paramCount, method != null && method.getProgram() != null ?
                method.getProgram().variableCount() : 0);
        DependencyNode[] parameterNodes = new DependencyNode[varCount];
        for (int i = 0; i < varCount; ++i) {
            parameterNodes[i] = new DependencyNode(this);
            if (shouldLog) {
                parameterNodes[i].setTag(methodRef + ":" + i);
            }
        }
        DependencyNode resultNode;
        if (methodRef.getDescriptor().getResultType() == ValueType.VOID) {
            resultNode = null;
        } else {
            resultNode = new DependencyNode(this);
            if (shouldLog) {
                resultNode.setTag(methodRef + ":RESULT");
            }
        }
        DependencyNode thrown = createNode();
        if (shouldLog) {
            thrown.setTag(methodRef + ":THROWN");
        }
        final MethodDependency dep = new MethodDependency(this, parameterNodes, paramCount, resultNode, thrown,
                stack, method, methodRef);
        if (method != null) {
            final DependencyStack initClassStack = stack;
            tasks.add(new Runnable() {
                @Override public void run() {
                    linkClass(dep.getMethod().getOwnerName(), dep.getStack()).initClass(initClassStack);
                }
            });
        } else {
            missingMethods.add(dep);
        }
        return dep;
    }

    void scheduleMethodAnalysis(final MethodDependency dep) {
        tasks.add(new Runnable() {
            @Override public void run() {
                DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(DependencyChecker.this);
                graphBuilder.buildGraph(dep);
            }
        });
    }

    @Override
    public Collection<MethodReference> getAchievableMethods() {
        return methodCache.getCachedPreimages();
    }

    @Override
    public Collection<FieldReference> getAchievableFields() {
        return fieldCache.getCachedPreimages();
    }

    @Override
    public Collection<String> getAchievableClasses() {
        return classCache.getCachedPreimages();
    }

    @Override
    public FieldDependency linkField(FieldReference fieldRef, MethodReference caller, InstructionLocation location) {
        fieldStacks.put(fieldRef, stack);
        return fieldCache.map(fieldRef);
    }

    @Override
    public FieldDependency getField(FieldReference fieldRef) {
        return fieldCache.getKnown(fieldRef);
    }

    @Override
    public ClassDependency getClass(String className) {
        return classCache.getKnown(className);
    }

    private FieldDependency createFieldNode(final FieldReference fieldRef, FieldReader field,
            final DependencyStack stack) {
        DependencyNode node = new DependencyNode(this);
        if (shouldLog) {
            node.setTag(fieldRef.getClassName() + "#" + fieldRef.getFieldName());
        }
        FieldDependency dep = new FieldDependency(node, stack, field, fieldRef);
        if (dep.isMissing()) {
            missingFields.add(dep);
        } else {
            tasks.add(new Runnable() {
                @Override public void run() {
                    linkClass(fieldRef.getClassName(), stack).initClass(stack);
                }
            });
        }
        return dep;
    }

    private void activateDependencyPlugin(MethodDependency methodDep) {
        AnnotationReader depAnnot = methodDep.getMethod().getAnnotations().get(PluggableDependency.class.getName());
        if (depAnnot == null) {
            return;
        }
        ValueType depType = depAnnot.getValue("value").getJavaClass();
        String depClassName = ((ValueType.Object)depType).getClassName();
        Class<?> depClass;
        try {
            depClass = Class.forName(depClassName, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Dependency plugin not found: " + depClassName, e);
        }
        DependencyPlugin plugin;
        try {
            plugin = (DependencyPlugin)depClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Can't instantiate dependency plugin " + depClassName, e);
        }
        plugin.methodAchieved(this, methodDep);
    }

    @Override
    public MethodDependency getMethod(MethodReference methodRef) {
        return methodCache.getKnown(methodRef);
    }

    public void processDependencies() {
        interrupted = false;
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

    @Override
    public <T> T getService(Class<T> type) {
        return services.getService(type);
    }

    @Override
    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    public CallGraph getCallGraph() {
        return callGraph;
    }
}
