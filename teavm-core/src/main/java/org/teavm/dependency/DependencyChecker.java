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

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.common.ConcurrentCachedMapper;
import org.teavm.common.Mapper;
import org.teavm.common.ConcurrentCachedMapper.KeyListener;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyChecker {
    private static Object dummyValue = new Object();
    static final boolean shouldLog = System.getProperty("org.teavm.logDependencies", "false").equals("true");
    private ClassHolderSource classSource;
    private ClassLoader classLoader;
    private ScheduledThreadPoolExecutor executor;
    private ConcurrentMap<MethodReference, Object> abstractMethods = new ConcurrentHashMap<>();
    private ConcurrentCachedMapper<MethodReference, MethodGraph> methodCache;
    private ConcurrentCachedMapper<FieldReference, DependencyNode> fieldCache;
    private ConcurrentMap<String, Object> achievableClasses = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> initializedClasses = new ConcurrentHashMap<>();
    private AtomicReference<RuntimeException> exceptionOccured = new AtomicReference<>();

    public DependencyChecker(ClassHolderSource classSource, ClassLoader classLoader) {
        this(classSource, classLoader, Runtime.getRuntime().availableProcessors());
    }

    public DependencyChecker(ClassHolderSource classSource, ClassLoader classLoader, int numThreads) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        executor = new ScheduledThreadPoolExecutor(numThreads);
        executor.setThreadFactory(new ThreadFactory() {
            @Override public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });
        methodCache = new ConcurrentCachedMapper<>(new Mapper<MethodReference, MethodGraph>() {
            @Override public MethodGraph map(MethodReference preimage) {
                return createMethodGraph(preimage);
            }
        });
        fieldCache = new ConcurrentCachedMapper<>(new Mapper<FieldReference, DependencyNode>() {
            @Override public DependencyNode map(FieldReference preimage) {
                return createFieldNode(preimage);
            }
        });
        methodCache.addKeyListener(new KeyListener<MethodReference>() {
            @Override public void keyAdded(MethodReference key) {
                activateDependencyPlugin(key);
            }
        });
    }

    public DependencyNode createNode() {
        return new DependencyNode(this);
    }

    public ClassHolderSource getClassSource() {
        return classSource;
    }

    public void addEntryPoint(MethodReference methodRef, String... argumentTypes) {
        ValueType[] parameters = methodRef.getDescriptor().getParameterTypes();
        if (parameters.length != argumentTypes.length) {
            throw new IllegalArgumentException("argumentTypes length does not match the number of method's arguments");
        }
        MethodGraph graph = attachMethodGraph(methodRef);
        DependencyNode[] varNodes = graph.getVariableNodes();
        varNodes[0].propagate(methodRef.getClassName());
        for (int i = 0; i < argumentTypes.length; ++i) {
            varNodes[i + 1].propagate(argumentTypes[i]);
        }
    }

    public void schedulePropagation(final DependencyConsumer consumer, final String type) {
        schedule(new Runnable() {
            @Override public void run() {
                consumer.consume(type);
            }
        });
    }

    void schedule(final Runnable runnable) {
        try {
            executor.execute(new Runnable() {
                @Override public void run() {
                    try {
                        runnable.run();
                    } catch (RuntimeException e) {
                        exceptionOccured.compareAndSet(null, e);
                        executor.shutdownNow();
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            throw exceptionOccured.get();
        }
    }

    public void checkDependencies() {
        exceptionOccured.set(null);
        while (true) {
            try {
                if (executor.getActiveCount() == 0 || executor.awaitTermination(2, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        RuntimeException e = exceptionOccured.get();
        if (e != null) {
            throw exceptionOccured.get();
        }
        executor.shutdown();
    }

    void achieveClass(String className) {
        achievableClasses.put(className, dummyValue);
    }

    public MethodGraph attachMethodGraph(MethodReference methodRef) {
        return methodCache.map(methodRef);
    }

    private void initClass(String className) {
        MethodDescriptor clinitDesc = new MethodDescriptor("<clinit>", ValueType.VOID);
        while (className != null) {
            if (initializedClasses.putIfAbsent(className, clinitDesc) != null) {
                break;
            }
            ClassHolder cls = classSource.getClassHolder(className);
            if (cls == null) {
                throw new RuntimeException("Class not found: " + className);
            }
            if (cls.getMethod(clinitDesc) != null) {
                attachMethodGraph(new MethodReference(className, clinitDesc));
            }
            className = cls.getParent();
        }
    }

    private MethodGraph createMethodGraph(final MethodReference methodRef) {
        initClass(methodRef.getClassName());
        ClassHolder cls = classSource.getClassHolder(methodRef.getClassName());
        MethodHolder method = cls.getMethod(methodRef.getDescriptor());
        if (method == null) {
            while (cls != null) {
                method = cls.getMethod(methodRef.getDescriptor());
                if (method != null) {
                    return methodCache.map(new MethodReference(cls.getName(), methodRef.getDescriptor()));
                }
                cls = cls.getParent() != null ? classSource.getClassHolder(cls.getParent()) : null;
            }
            throw new RuntimeException("Method not found: " + methodRef);
        }
        ValueType[] arguments = method.getParameterTypes();
        int paramCount = arguments.length + 1;
        int varCount = Math.max(paramCount, method.getProgram().variableCount());
        DependencyNode[] parameterNodes = new DependencyNode[varCount];
        for (int i = 0; i < varCount; ++i) {
            parameterNodes[i] = new DependencyNode(this);
            if (shouldLog) {
                parameterNodes[i].setTag(method.getOwner().getName() + "#" +
                        method.getName() + method.getDescriptor() + ":" + i);
            }
        }
        DependencyNode resultNode;
        if (method.getResultType() == ValueType.VOID) {
            resultNode = null;
        } else {
            resultNode = new DependencyNode(this);
            if (shouldLog) {
                resultNode.setTag(method.getOwner().getName() + "#" +
                        method.getName() + MethodDescriptor.get(method) + ":RESULT");
            }
        }
        final MethodGraph graph = new MethodGraph(parameterNodes, paramCount, resultNode, this);
        final MethodHolder currentMethod = method;
        schedule(new Runnable() {
            @Override public void run() {
                DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(DependencyChecker.this);
                graphBuilder.buildGraph(currentMethod, graph);
                achieveClass(methodRef.getClassName());
            }
        });
        return graph;
    }

    public boolean isMethodAchievable(MethodReference methodRef) {
        return methodCache.caches(methodRef);
    }

    public boolean isAbstractMethodAchievable(MethodReference methodRef) {
        return abstractMethods.containsKey(methodRef);
    }

    public Collection<MethodReference> getAchievableMethods() {
        return methodCache.getCachedPreimages();
    }

    public Collection<FieldReference> getAchievableFields() {
        return fieldCache.getCachedPreimages();
    }

    public Collection<String> getAchievableClasses() {
        return new HashSet<>(achievableClasses.keySet());
    }

    public DependencyNode getFieldNode(FieldReference fieldRef) {
        return fieldCache.map(fieldRef);
    }

    private DependencyNode createFieldNode(FieldReference fieldRef) {
        initClass(fieldRef.getClassName());
        DependencyNode node = new DependencyNode(this);
        if (shouldLog) {
            node.setTag(fieldRef.getClassName() + "#" + fieldRef.getFieldName());
        }
        return node;
    }

    private void activateDependencyPlugin(MethodReference methodRef) {
        ClassHolder cls = classSource.getClassHolder(methodRef.getClassName());
        MethodHolder method = cls.getMethod(methodRef.getDescriptor());
        if (method == null) {
            return;
        }
        AnnotationHolder depAnnot = method.getAnnotations().get(PluggableDependency.class.getName());
        if (depAnnot == null) {
            return;
        }
        ValueType depType = depAnnot.getValues().get("value").getJavaClass();
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
        plugin.methodAchieved(this, methodRef);
    }

    public void addAbstractMethod(MethodReference methodRef) {
        if (abstractMethods.putIfAbsent(methodRef, methodRef) == null) {
            String className = methodRef.getClassName();
            while (className != null) {
                ClassHolder cls = classSource.getClassHolder(className);
                if (cls == null) {
                    return;
                }
                MethodHolder method = cls.getMethod(methodRef.getDescriptor());
                if (method != null) {
                    abstractMethods.put(methodRef, methodRef);
                    return;
                }
                className = cls.getParent();
            }
        }
    }

    public ListableClassHolderSource cutUnachievableClasses() {
        MutableClassHolderSource cutClasses = new MutableClassHolderSource();
        for (String className : achievableClasses.keySet()) {
            ClassHolder classHolder = classSource.getClassHolder(className);
            cutClasses.putClassHolder(classHolder);
            for (MethodHolder method : classHolder.getMethods().toArray(new MethodHolder[0])) {
                MethodReference methodRef = new MethodReference(className, method.getDescriptor());
                if (!methodCache.getCachedPreimages().contains(methodRef)) {
                    if (abstractMethods.containsKey(methodRef)) {
                        method.getModifiers().add(ElementModifier.ABSTRACT);
                        method.setProgram(null);
                    } else {
                        classHolder.removeMethod(method);
                    }
                }
            }
            for (FieldHolder field : classHolder.getFields().toArray(new FieldHolder[0])) {
                FieldReference fieldRef = new FieldReference(className, field.getName());
                if (!fieldCache.getCachedPreimages().contains(fieldRef)) {
                    classHolder.removeField(field);
                }
            }
        }
        return cutClasses;
    }
}
