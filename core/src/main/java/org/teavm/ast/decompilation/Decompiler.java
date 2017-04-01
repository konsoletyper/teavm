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
package org.teavm.ast.decompilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.ClassNode;
import org.teavm.ast.FieldNode;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.MethodNode;
import org.teavm.ast.NativeMethodNode;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.Statement;
import org.teavm.ast.VariableNode;
import org.teavm.ast.cache.MethodNodeCache;
import org.teavm.ast.optimization.Optimizer;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.cache.NoCache;
import org.teavm.common.Graph;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.text.ListingBuilder;
import org.teavm.model.util.AsyncProgramSplitter;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TypeInferer;

public class Decompiler {
    private ClassHolderSource classSource;
    private ClassLoader classLoader;
    private Graph graph;
    private Map<MethodReference, Generator> generators = new HashMap<>();
    private Set<MethodReference> methodsToSkip = new HashSet<>();
    private MethodNodeCache regularMethodCache;
    private Set<MethodReference> asyncMethods;
    private Set<MethodReference> splitMethods = new HashSet<>();

    public Decompiler(ClassHolderSource classSource, ClassLoader classLoader, Set<MethodReference> asyncMethods,
            Set<MethodReference> asyncFamilyMethods) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.asyncMethods = asyncMethods;
        splitMethods.addAll(asyncMethods);
        splitMethods.addAll(asyncFamilyMethods);
    }

    public MethodNodeCache getRegularMethodCache() {
        return regularMethodCache;
    }

    public void setRegularMethodCache(MethodNodeCache regularMethodCache) {
        this.regularMethodCache = regularMethodCache;
    }

    public int getGraphSize() {
        return this.graph.size();
    }

    static class Block {
        public Block parent;
        public int parentOffset;
        public final IdentifiedStatement statement;
        public final List<Statement> body;
        public final int end;
        public final int start;
        public final List<TryCatchBookmark> tryCatches = new ArrayList<>();

        public Block(IdentifiedStatement statement, List<Statement> body, int start, int end) {
            this.statement = statement;
            this.body = body;
            this.start = start;
            this.end = end;
        }
    }

    static class TryCatchBookmark {
        Block block;
        int offset;
        String exceptionType;
        Integer exceptionVariable;
        int exceptionHandler;
    }

    public List<ClassNode> decompile(Collection<String> classNames) {
        List<String> sequence = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String className : classNames) {
            orderClasses(className, visited, sequence);
        }
        final List<ClassNode> result = new ArrayList<>();
        for (int i = 0; i < sequence.size(); ++i) {
            final String className = sequence.get(i);
            result.add(decompile(classSource.get(className)));
        }
        return result;
    }

    public List<String> getClassOrdering(Collection<String> classNames) {
        List<String> sequence = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String className : classNames) {
            orderClasses(className, visited, sequence);
        }
        return sequence;
    }

    public void addGenerator(MethodReference method, Generator generator) {
        generators.put(method, generator);
    }

    public void addMethodToSkip(MethodReference method) {
        methodsToSkip.add(method);
    }

    private void orderClasses(String className, Set<String> visited, List<String> order) {
        if (!visited.add(className)) {
            return;
        }
        ClassHolder cls = classSource.get(className);
        if (cls == null) {
            return;
        }
        if (cls.getParent() != null) {
            orderClasses(cls.getParent(), visited, order);
        }
        for (String iface : cls.getInterfaces()) {
            orderClasses(iface, visited, order);
        }
        order.add(className);
    }

    public ClassNode decompile(ClassHolder cls) {
        ClassNode clsNode = new ClassNode(cls.getName(), cls.getParent());
        for (FieldHolder field : cls.getFields()) {
            FieldNode fieldNode = new FieldNode(field.getName(), field.getType());
            fieldNode.getModifiers().addAll(field.getModifiers());
            fieldNode.setInitialValue(field.getInitialValue());
            clsNode.getFields().add(fieldNode);
        }
        for (MethodHolder method : cls.getMethods()) {
            if (method.getModifiers().contains(ElementModifier.ABSTRACT)) {
                continue;
            }
            if (method.getAnnotations().get(InjectedBy.class.getName()) != null
                    || methodsToSkip.contains(method.getReference())) {
                continue;
            }
            MethodNode methodNode = decompile(method);
            clsNode.getMethods().add(methodNode);
        }
        clsNode.getInterfaces().addAll(cls.getInterfaces());
        clsNode.getModifiers().addAll(cls.getModifiers());
        return clsNode;
    }

    public MethodNode decompile(MethodHolder method) {
        return method.getModifiers().contains(ElementModifier.NATIVE) ? decompileNative(method)
                : !asyncMethods.contains(method.getReference()) ? decompileRegular(method) : decompileAsync(method);
    }

    public NativeMethodNode decompileNative(MethodHolder method) {
        Generator generator = generators.get(method.getReference());
        if (generator == null) {
            AnnotationHolder annotHolder = method.getAnnotations().get(GeneratedBy.class.getName());
            if (annotHolder == null) {
                throw new DecompilationException("Method " + method.getOwnerName() + "." + method.getDescriptor()
                        + " is native, but no " + GeneratedBy.class.getName() + " annotation found");
            }
            ValueType annotValue = annotHolder.getValues().get("value").getJavaClass();
            String generatorClassName = ((ValueType.Object) annotValue).getClassName();
            try {
                Class<?> generatorClass = Class.forName(generatorClassName, true, classLoader);
                generator = (Generator) generatorClass.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new DecompilationException("Error instantiating generator " + generatorClassName
                        + " for native method " + method.getOwnerName() + "." + method.getDescriptor());
            }
        }
        NativeMethodNode methodNode = new NativeMethodNode(new MethodReference(method.getOwnerName(),
                method.getDescriptor()));
        methodNode.getModifiers().addAll(method.getModifiers());
        methodNode.setGenerator(generator);
        methodNode.setAsync(asyncMethods.contains(method.getReference()));
        return methodNode;
    }

    public RegularMethodNode decompileRegular(MethodHolder method) {
        if (regularMethodCache == null || method.getAnnotations().get(NoCache.class.getName()) != null) {
            return decompileRegularCacheMiss(method);
        }
        RegularMethodNode node = regularMethodCache.get(method.getReference());
        if (node == null) {
            node = decompileRegularCacheMiss(method);
            regularMethodCache.store(method.getReference(), node);
        }
        return node;
    }

    public RegularMethodNode decompileRegularCacheMiss(MethodHolder method) {
        RegularMethodNode methodNode = new RegularMethodNode(method.getReference());
        Program program = method.getProgram();
        int[] targetBlocks = new int[program.basicBlockCount()];
        Arrays.fill(targetBlocks, -1);
        try {
            methodNode.setBody(getRegularMethodStatement(program, targetBlocks, false).getStatement());
        } catch (RuntimeException e) {
            StringBuilder sb = new StringBuilder("Error decompiling method " + method.getReference() + ":\n");
            sb.append(new ListingBuilder().buildListing(program, "  "));
            throw new DecompilationException(sb.toString(), e);
        }

        TypeInferer typeInferer = new TypeInferer();
        typeInferer.inferTypes(program, method.getReference());
        for (int i = 0; i < program.variableCount(); ++i) {
            VariableNode variable = new VariableNode(program.variableAt(i).getRegister(), typeInferer.typeOf(i));
            variable.setName(program.variableAt(i).getDebugName());
            methodNode.getVariables().add(variable);
        }

        Optimizer optimizer = new Optimizer();
        optimizer.optimize(methodNode, method.getProgram());
        methodNode.getModifiers().addAll(method.getModifiers());

        return methodNode;
    }

    public AsyncMethodNode decompileAsync(MethodHolder method) {
        if (regularMethodCache == null || method.getAnnotations().get(NoCache.class.getName()) != null) {
            return decompileAsyncCacheMiss(method);
        }
        AsyncMethodNode node = regularMethodCache.getAsync(method.getReference());
        if (node == null || !checkAsyncRelevant(node)) {
            node = decompileAsyncCacheMiss(method);
            regularMethodCache.storeAsync(method.getReference(), node);
        }
        return node;
    }

    private boolean checkAsyncRelevant(AsyncMethodNode node) {
        AsyncCallsFinder asyncCallsFinder = new AsyncCallsFinder();
        for (AsyncMethodPart part : node.getBody()) {
            part.getStatement().acceptVisitor(asyncCallsFinder);
        }
        for (MethodReference asyncCall : asyncCallsFinder.asyncCalls) {
            if (!splitMethods.contains(asyncCall)) {
                return false;
            }
        }
        asyncCallsFinder.allCalls.removeAll(asyncCallsFinder.asyncCalls);
        for (MethodReference asyncCall : asyncCallsFinder.allCalls) {
            if (splitMethods.contains(asyncCall)) {
                return false;
            }
        }
        return true;
    }

    private AsyncMethodNode decompileAsyncCacheMiss(MethodHolder method) {
        AsyncMethodNode node = new AsyncMethodNode(method.getReference());
        AsyncProgramSplitter splitter = new AsyncProgramSplitter(classSource, splitMethods);
        splitter.split(method.getProgram());
        for (int i = 0; i < splitter.size(); ++i) {
            AsyncMethodPart part;
            try {
                part = getRegularMethodStatement(splitter.getProgram(i), splitter.getBlockSuccessors(i), i > 0);
            } catch (RuntimeException e) {
                StringBuilder sb = new StringBuilder("Error decompiling method " + method.getReference()
                        + " part " + i + ":\n");
                sb.append(new ListingBuilder().buildListing(splitter.getProgram(i), "  "));
                throw new DecompilationException(sb.toString(), e);
            }
            node.getBody().add(part);
        }

        Program program = method.getProgram();
        TypeInferer typeInferer = new TypeInferer();
        typeInferer.inferTypes(program, method.getReference());
        for (int i = 0; i < program.variableCount(); ++i) {
            VariableNode variable = new VariableNode(program.variableAt(i).getRegister(), typeInferer.typeOf(i));
            variable.setName(program.variableAt(i).getDebugName());
            node.getVariables().add(variable);
        }

        Optimizer optimizer = new Optimizer();
        optimizer.optimize(node, splitter);
        node.getModifiers().addAll(method.getModifiers());

        return node;
    }

    private AsyncMethodPart getRegularMethodStatement(Program program, int[] targetBlocks, boolean async) {
        AsyncMethodPart result = new AsyncMethodPart();
        graph = ProgramUtils.buildControlFlowGraph(program);

        BlockStatement rootStmt = new BlockStatement();
        rootStmt.setId("root");
        StatementGenerator generator = new StatementGenerator();
        generator.classSource = classSource;
        generator.program = program;
        generator.prepare();

        generator.statements = rootStmt.getBody();
        generator.currentBlock = program.basicBlockAt(0);
        generator.processBlock();

        result.setStatement(rootStmt);

        return result;
    }
}
