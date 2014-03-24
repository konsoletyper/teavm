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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.teavm.common.*;
import org.teavm.common.ConcurrentCachedMapper.KeyListener;
import org.teavm.model.*;
import org.teavm.model.util.ModelUtils;

/**
 *
 * @author Alexey Andreev
 */
public class DependencyChecker implements DependencyInfo, DependencyAgent {
    private static Object dummyValue = new Object();
    static final boolean shouldLog = System.getProperty("org.teavm.logDependencies", "false").equals("true");
    private int classNameSuffix;
    private DependencyClassSource classSource;
    private ClassLoader classLoader;
    private FiniteExecutor executor;
    private Mapper<MethodReference, MethodReader> methodReaderCache;
    private Mapper<FieldReference, FieldReader> fieldReaderCache;
    private ConcurrentMap<MethodReference, DependencyStack> stacks = new ConcurrentHashMap<>();
    private ConcurrentMap<FieldReference, DependencyStack> fieldStacks = new ConcurrentHashMap<>();
    private ConcurrentMap<String, DependencyStack> classStacks = new ConcurrentHashMap<>();
    private ConcurrentCachedMapper<MethodReference, MethodDependency> methodCache;
    private ConcurrentCachedMapper<FieldReference, FieldDependency> fieldCache;
    private ConcurrentMap<String, Object> achievableClasses = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Object> initializedClasses = new ConcurrentHashMap<>();
    private List<DependencyListener> listeners = new ArrayList<>();
    ConcurrentMap<MethodReference, DependencyStack> missingMethods = new ConcurrentHashMap<>();
    ConcurrentMap<String, DependencyStack> missingClasses = new ConcurrentHashMap<>();
    ConcurrentMap<FieldReference, DependencyStack> missingFields = new ConcurrentHashMap<>();

    public DependencyChecker(ClassReaderSource classSource, ClassLoader classLoader) {
        this(classSource, classLoader, new SimpleFiniteExecutor());
    }

    public DependencyChecker(ClassReaderSource classSource, ClassLoader classLoader, FiniteExecutor executor) {
        this.classSource = new DependencyClassSource(classSource);
        this.classLoader = classLoader;
        this.executor = executor;
        methodReaderCache = new ConcurrentCachedMapper<>(new Mapper<MethodReference, MethodReader>() {
            @Override public MethodReader map(MethodReference preimage) {
                return findMethodReader(preimage);
            }
        });
        fieldReaderCache = new ConcurrentCachedMapper<>(new Mapper<FieldReference, FieldReader>() {
            @Override public FieldReader map(FieldReference preimage) {
                return findFieldReader(preimage);
            }
        });
        methodCache = new ConcurrentCachedMapper<>(new Mapper<MethodReference, MethodDependency>() {
            @Override public MethodDependency map(MethodReference preimage) {
                MethodReader method = methodReaderCache.map(preimage);
                if (method != null && !method.getReference().equals(preimage)) {
                    stacks.put(method.getReference(), stacks.get(preimage));
                    return methodCache.map(method.getReference());
                }
                return createMethodDep(preimage, method, stacks.get(preimage));
            }
        });
        fieldCache = new ConcurrentCachedMapper<>(new Mapper<FieldReference, FieldDependency>() {
            @Override public FieldDependency map(FieldReference preimage) {
                FieldReader field = fieldReaderCache.map(preimage);
                if (field != null && !field.getReference().equals(preimage)) {
                    fieldStacks.put(field.getReference(), fieldStacks.get(preimage));
                    return fieldCache.map(field.getReference());
                }
                return createFieldNode(preimage, field, fieldStacks.get(preimage));
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
    public String generateClassName() {
        return "$$tmp$$.TempClass" + classNameSuffix++;
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
        if (parameters.length != argumentTypes.length) {
            throw new IllegalArgumentException("argumentTypes length does not match the number of method's arguments");
        }
        MethodDependency method = linkMethod(methodRef, DependencyStack.ROOT);
        method.use();
        DependencyNode[] varNodes = method.getVariables();
        varNodes[0].propagate(methodRef.getClassName());
        for (int i = 0; i < argumentTypes.length; ++i) {
            varNodes[i + 1].propagate(argumentTypes[i]);
        }
    }

    void schedulePropagation(final DependencyConsumer consumer, final String type) {
        executor.executeFast(new Runnable() {
            @Override public void run() {
                consumer.consume(type);
            }
        });
    }

    public FiniteExecutor getExecutor() {
        return executor;
    }

    boolean achieveClass(String className, DependencyStack stack) {
        classStacks.putIfAbsent(className, stack);
        boolean result = achievableClasses.putIfAbsent(className, dummyValue) == null;
        if (result) {
            for (DependencyListener listener : listeners) {
                listener.classAchieved(this, className);
            }
        }
        return result;
    }

    @Override
    public MethodDependency linkMethod(MethodReference methodRef, DependencyStack stack) {
        if (methodRef == null) {
            throw new IllegalArgumentException();
        }
        stacks.putIfAbsent(methodRef, stack);
        return methodCache.map(methodRef);
    }

    @Override
    public void initClass(String className, final DependencyStack stack) {
        classStacks.putIfAbsent(className, stack);
        MethodDescriptor clinitDesc = new MethodDescriptor("<clinit>", ValueType.VOID);
        while (className != null) {
            if (initializedClasses.putIfAbsent(className, clinitDesc) != null) {
                break;
            }
            achieveClass(className, stack);
            achieveInterfaces(className, stack);
            ClassReader cls = classSource.get(className);
            if (cls == null) {
                missingClasses.put(className, stack);
                return;
            }
            if (cls.getMethod(clinitDesc) != null) {
                final MethodReference methodRef = new MethodReference(className, clinitDesc);
                executor.executeFast(new Runnable() {
                    @Override public void run() {
                        linkMethod(methodRef, new DependencyStack(methodRef, stack)).use();
                    }
                });
            }
            className = cls.getParent();
        }
    }

    private void achieveInterfaces(String className, DependencyStack stack) {
        classStacks.putIfAbsent(className, stack);
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            missingClasses.put(className, stack);
            return;
        }
        for (String iface : cls.getInterfaces()) {
            if (achieveClass(iface, stack)) {
                achieveInterfaces(iface, stack);
            }
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
        if (cls.getParent() != null) {
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
        stack = new DependencyStack(methodRef, stack);
        final MethodDependency dep = new MethodDependency(parameterNodes, paramCount, resultNode, thrown,
                stack, method, methodRef);
        if (method != null) {
            executor.execute(new Runnable() {
                @Override public void run() {
                    DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(DependencyChecker.this);
                    graphBuilder.buildGraph(dep);
                }
            });
        } else {
            missingMethods.putIfAbsent(methodRef, stack);
        }
        if (method != null) {
            final DependencyStack callerStack = stack;
            executor.execute(new Runnable() {
                @Override public void run() {
                    initClass(dep.getReference().getClassName(), callerStack);
                }
            });

        }
        return dep;
    }

    @Override
    public boolean isMethodAchievable(MethodReference methodRef) {
        return methodCache.caches(methodRef);
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
        return new HashSet<>(achievableClasses.keySet());
    }

    @Override
    public FieldDependency linkField(FieldReference fieldRef, DependencyStack stack) {
        fieldStacks.putIfAbsent(fieldRef, stack);
        return fieldCache.map(fieldRef);
    }

    @Override
    public FieldDependency getField(FieldReference fieldRef) {
        return fieldCache.getKnown(fieldRef);
    }

    private FieldDependency createFieldNode(FieldReference fieldRef, FieldReader field, DependencyStack stack) {
        DependencyNode node = new DependencyNode(this);
        if (field == null) {
            missingFields.putIfAbsent(fieldRef, stack);
        }
        if (shouldLog) {
            node.setTag(fieldRef.getClassName() + "#" + fieldRef.getFieldName());
        }
        if (field != null) {
            initClass(fieldRef.getClassName(), stack);
        }
        return new FieldDependency(node, stack, field, fieldRef);
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

    public void checkForMissingItems() {
        if (!hasMissingItems()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        try {
            showMissingItems(sb);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException");
        }
        throw new IllegalStateException(sb.toString());
    }

    public boolean hasMissingItems() {
        return !missingClasses.isEmpty() || !missingMethods.isEmpty() || !missingFields.isEmpty();
    }

    public void showMissingItems(Appendable sb) throws IOException {
        List<String> items = new ArrayList<>();
        Map<String, DependencyStack> stackMap = new HashMap<>();
        for (String cls : missingClasses.keySet()) {
            stackMap.put(cls, missingClasses.get(cls));
            items.add(cls);
        }
        for (MethodReference method : missingMethods.keySet()) {
            stackMap.put(method.toString(), missingMethods.get(method));
            items.add(method.toString());
        }
        for (FieldReference field : missingFields.keySet()) {
            stackMap.put(field.toString(), missingFields.get(field));
            items.add(field.toString());
        }
        Collections.sort(items);
        sb.append("Can't compile due to the following items missing:\n");
        for (String item : items) {
            sb.append("  ").append(item).append("\n");
            DependencyStack stack = stackMap.get(item);
            if (stack == null) {
                sb.append("    at unknown location\n");
            } else {
                while (stack.getMethod() != null) {
                    sb.append("    at " + stack.getMethod() + "\n");
                    stack = stack.getCause();
                }
            }
            sb.append('\n');
        }
    }
}
