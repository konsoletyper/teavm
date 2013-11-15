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
import org.teavm.codegen.ConcurrentCachedMapper;
import org.teavm.codegen.Mapper;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyChecker {
    private static Object dummyValue = new Object();
    static final boolean shouldLog = System.getProperty("org.teavm.logDependencies", "false").equals("true");
    private ClassHolderSource classSource;
    private ScheduledThreadPoolExecutor executor;
    private ConcurrentCachedMapper<MethodReference, MethodGraph> methodCache;
    private ConcurrentCachedMapper<FieldReference, DependencyNode> fieldCache;
    private ConcurrentMap<String, Object> achievableClasses = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> initializedClasses = new ConcurrentHashMap<>();
    private volatile RuntimeException exceptionOccured;

    public DependencyChecker(ClassHolderSource classSource) {
        this(classSource, 1);
    }

    public DependencyChecker(ClassHolderSource classSource, int numThreads) {
        this.classSource = classSource;
        executor = new ScheduledThreadPoolExecutor(numThreads);
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
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (RuntimeException e) {
                    exceptionOccured = e;
                    executor.shutdownNow();
                }
            }
        });
    }

    public void checkDependencies() {
        exceptionOccured = null;
        while (true) {
            try {
                if (executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (exceptionOccured != null) {
            throw exceptionOccured;
        }
    }

    void achieveClass(String className) {
        achievableClasses.put(className, dummyValue);
    }

    public MethodGraph attachMethodGraph(MethodReference methodRef) {
        return methodCache.map(methodRef);
    }

    private void initClass(String className) {
        MethodDescriptor clinitDesc = new MethodDescriptor("<clinit>");
        while (className != null) {
            if (initializedClasses.putIfAbsent(className, clinitDesc) != null) {
                break;
            }
            ClassHolder cls = classSource.getClassHolder(className);
            if (cls.getMethod(clinitDesc) != null) {
                attachMethodGraph(new MethodReference(className, clinitDesc));
            }
            className = cls.getParent();
        }
    }

    private MethodGraph createMethodGraph(MethodReference methodRef) {
        initClass(methodRef.getClassName());
        ClassHolder cls = classSource.getClassHolder(methodRef.getClassName());
        MethodHolder method = cls.getMethod(methodRef.getDescriptor());
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
        MethodGraph graph = new MethodGraph(parameterNodes, paramCount, resultNode, this);
        DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(this);
        graphBuilder.buildGraph(method, graph);
        return graph;
    }

    public boolean isMethodAchievable(MethodReference methodRef) {
        return methodCache.caches(methodRef);
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
}
