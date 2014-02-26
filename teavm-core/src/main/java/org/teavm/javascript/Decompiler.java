/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.javascript;

import java.util.*;
import org.teavm.common.*;
import org.teavm.javascript.ast.*;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.InjectedBy;
import org.teavm.javascript.ni.PreserveOriginalName;
import org.teavm.model.*;
import org.teavm.model.util.ProgramUtils;

/**
 *
 * @author Alexey Andreev
 */
public class Decompiler {
    private ClassHolderSource classSource;
    private ClassLoader classLoader;
    private Graph graph;
    private LoopGraph loopGraph;
    private GraphIndexer indexer;
    private int[] loops;
    private int[] loopSuccessors;
    private Block[] blockMap;
    private int lastBlockId;
    private RangeTree codeTree;
    private RangeTree.Node currentNode;
    private RangeTree.Node parentNode;
    private FiniteExecutor executor;
    private Map<MethodReference, Generator> generators = new HashMap<>();

    public Decompiler(ClassHolderSource classSource, ClassLoader classLoader, FiniteExecutor executor) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.executor = executor;
    }

    public int getGraphSize() {
        return this.graph.size();
    }

    class Block {
        public final IdentifiedStatement statement;
        public final List<Statement> body;
        public final int end;
        public final int start;

        public Block(IdentifiedStatement statement, List<Statement> body, int start, int end) {
            this.statement = statement;
            this.body = body;
            this.start = start;
            this.end = end;
        }
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
            result.add(null);
            final int index = i;
            executor.execute(new Runnable() {
                @Override public void run() {
                    Decompiler copy = new Decompiler(classSource, classLoader, executor);
                    copy.generators = generators;
                    result.set(index, copy.decompile(classSource.get(className)));
                }
            });
        }
        executor.complete();
        return result;
    }

    public void addGenerator(MethodReference method, Generator generator) {
        generators.put(method, generator);
    }

    private void orderClasses(String className, Set<String> visited, List<String> order) {
        if (!visited.add(className)) {
            return;
        }
        ClassHolder cls = classSource.get(className);
        if (cls == null) {
            throw new IllegalArgumentException("Class not found: " + className);
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
            fieldNode.getModifiers().addAll(mapModifiers(field.getModifiers()));
            fieldNode.setInitialValue(field.getInitialValue());
            clsNode.getFields().add(fieldNode);
        }
        for (MethodHolder method : cls.getMethods()) {
            if (method.getModifiers().contains(ElementModifier.ABSTRACT)) {
                continue;
            }
            if (method.getAnnotations().get(InjectedBy.class.getName()) != null) {
                continue;
            }
            MethodNode methodNode = decompile(method);
            clsNode.getMethods().add(methodNode);
            if (method.getAnnotations().get(PreserveOriginalName.class.getName()) != null) {
                methodNode.setOriginalNamePreserved(true);
            }
        }
        clsNode.getInterfaces().addAll(cls.getInterfaces());
        clsNode.getModifiers().addAll(mapModifiers(cls.getModifiers()));
        return clsNode;
    }

    public MethodNode decompile(MethodHolder method) {
        return method.getModifiers().contains(ElementModifier.NATIVE) ?
                decompileNative(method) : decompileRegular(method);
    }

    public NativeMethodNode decompileNative(MethodHolder method) {
        Generator generator = generators.get(method.getReference());
        if (generator == null) {
            AnnotationHolder annotHolder = method.getAnnotations().get(GeneratedBy.class.getName());
            if (annotHolder == null) {
                throw new DecompilationException("Method " + method.getOwnerName() + "." + method.getDescriptor() +
                        " is native, but no " + GeneratedBy.class.getName() + " annotation found");
            }
            ValueType annotValue = annotHolder.getValues().get("value").getJavaClass();
            String generatorClassName = ((ValueType.Object)annotValue).getClassName();
            try {
                Class<?> generatorClass = Class.forName(generatorClassName, true, classLoader);
                generator = (Generator)generatorClass.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new DecompilationException("Error instantiating generator " + generatorClassName +
                        " for native method " + method.getOwnerName() + "." + method.getDescriptor());
            }
        }
        NativeMethodNode methodNode = new NativeMethodNode(new MethodReference(method.getOwnerName(),
                method.getDescriptor()));
        methodNode.getModifiers().addAll(mapModifiers(method.getModifiers()));
        methodNode.setGenerator(generator);
        return methodNode;
    }

    public RegularMethodNode decompileRegular(MethodHolder method) {
        lastBlockId = 1;
        graph = ProgramUtils.buildControlFlowGraph(method.getProgram());
        indexer = new GraphIndexer(graph);
        graph = indexer.getGraph();
        loopGraph = new LoopGraph(this.graph);
        unflatCode();
        Program program = method.getProgram();
        blockMap = new Block[program.basicBlockCount() * 2 + 1];
        Deque<Block> stack = new ArrayDeque<>();
        BlockStatement rootStmt = new BlockStatement();
        rootStmt.setId("root");
        stack.push(new Block(rootStmt, rootStmt.getBody(), -1, -1));
        StatementGenerator generator = new StatementGenerator();
        generator.classSource = classSource;
        generator.program = program;
        generator.blockMap = blockMap;
        generator.indexer = indexer;
        parentNode = codeTree.getRoot();
        currentNode = parentNode.getFirstChild();
        for (int i = 0; i < this.graph.size(); ++i) {
            Block block = stack.peek();
            while (block.end == i) {
                stack.pop();
                block = stack.peek();
            }
            while (parentNode.getEnd() == i) {
                currentNode = parentNode.getNext();
                parentNode = parentNode.getParent();
            }
            for (Block newBlock : createBlocks(i)) {
                block.body.add(newBlock.statement);
                stack.push(newBlock);
                block = newBlock;
            }
            int node = i < indexer.size() ? indexer.nodeAt(i) : -1;
            int next = i + 1;
            int head = loops[i];
            if (head != -1 && loopSuccessors[head] == next) {
                next = head;
            }
            if (node >= 0) {
                generator.currentBlock = program.basicBlockAt(node);
                int tmp = indexer.nodeAt(next);
                generator.nextBlock = next < indexer.size() ? program.basicBlockAt(tmp) : null;
                generator.statements.clear();
                for (Instruction insn : generator.currentBlock.getInstructions()) {
                    insn.acceptVisitor(generator);
                }
                block.body.addAll(generator.statements);
                for (TryCatchBlock tryCatch : generator.currentBlock.getTryCatchBlocks()) {
                    TryCatchStatement tryCatchStmt = new TryCatchStatement();
                    tryCatchStmt.setExceptionType(tryCatch.getExceptionType());
                    tryCatchStmt.setExceptionVariable(tryCatch.getExceptionVariable().getIndex());
                    tryCatchStmt.getProtectedBody().addAll(block.body);
                    block.body.clear();
                    block.body.add(tryCatchStmt);
                    Statement handlerStmt = generator.generateJumpStatement(tryCatch.getHandler());
                    if (handlerStmt != null) {
                        tryCatchStmt.getHandler().add(handlerStmt);
                    }
                }
            }
        }
        SequentialStatement result = new SequentialStatement();
        result.getSequence().addAll(rootStmt.getBody());
        MethodReference reference = new MethodReference(method.getOwnerName(), method.getDescriptor());
        RegularMethodNode methodNode = new RegularMethodNode(reference);
        methodNode.getModifiers().addAll(mapModifiers(method.getModifiers()));
        methodNode.setBody(result);
        for (int i = 0; i < program.variableCount(); ++i) {
            methodNode.getVariables().add(program.variableAt(i).getRegister());
        }
        Optimizer optimizer = new Optimizer();
        optimizer.optimize(methodNode, method.getProgram());
        methodNode.getModifiers().addAll(mapModifiers(method.getModifiers()));
        return methodNode;
    }

    private Set<NodeModifier> mapModifiers(Set<ElementModifier> modifiers) {
        Set<NodeModifier> result = EnumSet.noneOf(NodeModifier.class);
        if (modifiers.contains(ElementModifier.STATIC)) {
            result.add(NodeModifier.STATIC);
        }
        if (modifiers.contains(ElementModifier.INTERFACE)) {
            result.add(NodeModifier.INTERFACE);
        }
        if (modifiers.contains(ElementModifier.ENUM)) {
            result.add(NodeModifier.ENUM);
        }
        return result;
    }

    private List<Block> createBlocks(int start) {
        List<Block> result = new ArrayList<>();
        while (currentNode != null && currentNode.getStart() == start) {
            Block block;
            IdentifiedStatement statement;
            if (loopSuccessors[start] == currentNode.getEnd()) {
                WhileStatement whileStatement = new WhileStatement();
                statement = whileStatement;
                block = new Block(statement, whileStatement.getBody(), start,
                        currentNode.getEnd());
            } else {
                BlockStatement blockStatement = new BlockStatement();
                statement = blockStatement;
                block = new Block(statement, blockStatement.getBody(), start,
                        currentNode.getEnd());
            }
            result.add(block);
            int mappedIndex = indexer.nodeAt(currentNode.getEnd());
            if (mappedIndex >= 0 && (blockMap[mappedIndex] == null ||
                    !(blockMap[mappedIndex].statement instanceof WhileStatement))) {
                blockMap[mappedIndex] = block;
            }
            if (loopSuccessors[start] == currentNode.getEnd()) {
                blockMap[indexer.nodeAt(start)] = block;
            }
            parentNode = currentNode;
            currentNode = currentNode.getFirstChild();
        }
        for (Block block : result) {
            block.statement.setId("block" + lastBlockId++);
        }
        return result;
    }

    private void unflatCode() {
        Graph graph = this.graph;
        int sz = graph.size();

        // Find where each loop ends
        //
        int[] loopSuccessors = new int[sz];
        Arrays.fill(loopSuccessors, sz + 1);
        for (int node = 0; node < sz; ++node) {
            Loop loop = loopGraph.loopAt(node);
            while (loop != null) {
                loopSuccessors[loop.getHead()] = node + 1;
                loop = loop.getParent();
            }
        }

        // For each node find head of loop this node belongs to.
        //
        int[] loops = new int[sz];
        Arrays.fill(loops, -1);
        for (int head = 0; head < sz; ++head) {
            int end = loopSuccessors[head];
            if (end > sz) {
                continue;
            }
            for (int node = head + 1; node < end; ++node) {
                loops[node] = head;
            }
        }

        List<RangeTree.Range> ranges = new ArrayList<>();
        for (int node = 0; node < sz; ++node) {
            if (loopSuccessors[node] <= sz) {
                ranges.add(new RangeTree.Range(node, loopSuccessors[node]));
            }
            int start = sz;
            for (int prev : graph.incomingEdges(node)) {
                start = Math.min(start, prev);
            }
            if (start < node - 1) {
                ranges.add(new RangeTree.Range(start, node));
            }
        }
        codeTree = new RangeTree(sz + 1, ranges);
        this.loopSuccessors = loopSuccessors;
        this.loops = loops;
    }
}
