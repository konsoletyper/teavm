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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.ClassNode;
import org.teavm.ast.FieldNode;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.MethodNode;
import org.teavm.ast.NativeMethodNode;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.VariableNode;
import org.teavm.ast.WhileStatement;
import org.teavm.ast.cache.MethodNodeCache;
import org.teavm.ast.optimization.Optimizer;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.cache.NoCache;
import org.teavm.common.Graph;
import org.teavm.common.GraphIndexer;
import org.teavm.common.Loop;
import org.teavm.common.LoopGraph;
import org.teavm.common.RangeTree;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.text.ListingBuilder;
import org.teavm.model.util.AsyncProgramSplitter;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TypeInferer;

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
    private Map<MethodReference, Generator> generators = new HashMap<>();
    private Set<MethodReference> methodsToSkip = new HashSet<>();
    private MethodNodeCache regularMethodCache;
    private Set<MethodReference> asyncMethods;
    private Set<MethodReference> splitMethods = new HashSet<>();
    private List<TryCatchBookmark> tryCatchBookmarks = new ArrayList<>();
    private Deque<Block> stack;
    private Program program;
    private boolean friendlyToDebugger;

    public Decompiler(ClassHolderSource classSource, ClassLoader classLoader, Set<MethodReference> asyncMethods,
            Set<MethodReference> asyncFamilyMethods, boolean friendlyToDebugger) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.asyncMethods = asyncMethods;
        splitMethods.addAll(asyncMethods);
        splitMethods.addAll(asyncFamilyMethods);
        this.friendlyToDebugger = friendlyToDebugger;
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
            if ((!isBootstrap() && method.getAnnotations().get(InjectedBy.class.getName()) != null)
                    || methodsToSkip.contains(method.getReference())) {
                continue;
            }
            MethodNode methodNode = decompile(method);
            clsNode.getMethods().add(methodNode);
        }
        clsNode.getInterfaces().addAll(cls.getInterfaces());
        clsNode.getModifiers().addAll(cls.getModifiers());
        clsNode.setAccessLevel(cls.getLevel());
        return clsNode;
    }

    public MethodNode decompile(MethodHolder method) {
        return method.getModifiers().contains(ElementModifier.NATIVE) ? decompileNative(method)
                : !asyncMethods.contains(method.getReference()) ? decompileRegular(method) : decompileAsync(method);
    }

    public NativeMethodNode decompileNative(MethodHolder method) {
        Generator generator = generators.get(method.getReference());
        if (generator == null && !isBootstrap()) {
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

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
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
        optimizer.optimize(methodNode, method.getProgram(), friendlyToDebugger);
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
        optimizer.optimize(node, splitter, friendlyToDebugger);
        node.getModifiers().addAll(method.getModifiers());

        return node;
    }

    private AsyncMethodPart getRegularMethodStatement(Program program, int[] targetBlocks, boolean async) {
        AsyncMethodPart result = new AsyncMethodPart();
        lastBlockId = 1;
        graph = ProgramUtils.buildControlFlowGraph(program);
        int[] weights = new int[graph.size()];
        for (int i = 0; i < weights.length; ++i) {
            weights[i] = program.basicBlockAt(i).instructionCount();
        }
        int[] priorities = new int[graph.size()];
        for (int i = 0; i < targetBlocks.length; ++i) {
            if (targetBlocks[i] >= 0) {
                priorities[i] = 1;
            }
        }
        indexer = new GraphIndexer(graph, weights, priorities);
        graph = indexer.getGraph();
        loopGraph = new LoopGraph(this.graph);
        unflatCode();
        blockMap = new Block[program.basicBlockCount() * 2 + 1];
        stack = new ArrayDeque<>();
        this.program = program;
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
        generator.async = async;
        for (int i = 0; i < this.graph.size(); ++i) {
            int node = i < indexer.size() ? indexer.nodeAt(i) : -1;
            int next = i + 1;
            int head = loops[i];
            if (head != -1 && loopSuccessors[head] == next) {
                next = head;
            }

            if (node >= 0) {
                generator.currentBlock = program.basicBlockAt(node);
                int tmp = indexer.nodeAt(next);
                generator.nextBlock = tmp >= 0 && next < indexer.size() ? program.basicBlockAt(tmp) : null;
            }

            closeExpiredBookmarks(generator, node, generator.currentBlock.getTryCatchBlocks());

            List<TryCatchBookmark> inheritedBookmarks = new ArrayList<>();
            Block block = stack.peek();
            while (block.end == i) {
                Block oldBlock = block;
                stack.pop();
                block = stack.peek();
                if (block.start >= 0) {
                    int mappedStart = indexer.nodeAt(block.start);
                    if (blockMap[mappedStart] == oldBlock) {
                        blockMap[mappedStart] = block;
                    }
                }

                for (int j = 0; j < oldBlock.tryCatches.size(); ++j) {
                    TryCatchBookmark bookmark = oldBlock.tryCatches.get(j);
                    TryCatchStatement tryCatchStmt = new TryCatchStatement();
                    tryCatchStmt.setExceptionType(bookmark.exceptionType);
                    tryCatchStmt.setExceptionVariable(bookmark.exceptionVariable);
                    tryCatchStmt.getHandler().add(generator.generateJumpStatement(
                            program.basicBlockAt(bookmark.exceptionHandler)));
                    List<Statement> blockPart = oldBlock.body.subList(bookmark.offset, oldBlock.body.size());
                    tryCatchStmt.getProtectedBody().addAll(blockPart);
                    blockPart.clear();
                    if (!tryCatchStmt.getProtectedBody().isEmpty()) {
                        blockPart.add(tryCatchStmt);
                    }
                    inheritedBookmarks.add(0, bookmark);
                }
                oldBlock.tryCatches.clear();
            }

            for (int j = inheritedBookmarks.size() - 1; j >= 0; --j) {
                TryCatchBookmark bookmark = inheritedBookmarks.get(j);
                bookmark.block = block;
                bookmark.offset = block.body.size();
                block.tryCatches.add(bookmark);
            }

            while (parentNode.getEnd() == i) {
                currentNode = parentNode.getNext();
                parentNode = parentNode.getParent();
            }
            for (Block newBlock : createBlocks(i)) {
                block.body.add(newBlock.statement);
                newBlock.parent = block;
                newBlock.parentOffset = block.body.size();
                stack.push(newBlock);
                block = newBlock;
            }
            createNewBookmarks(generator.currentBlock.getTryCatchBlocks());

            if (node >= 0) {
                generator.statements.clear();
                TextLocation lastLocation = null;
                for (Instruction insn : generator.currentBlock) {
                    if (insn.getLocation() != null && lastLocation != insn.getLocation()) {
                        lastLocation = insn.getLocation();
                    }
                    if (insn.getLocation() != null) {
                        generator.setCurrentLocation(lastLocation);
                    }
                    insn.acceptVisitor(generator);
                }
                if (targetBlocks[node] >= 0) {
                    GotoPartStatement stmt = new GotoPartStatement();
                    stmt.setPart(targetBlocks[node]);
                    generator.statements.add(stmt);
                }

                block.body.addAll(generator.statements);
            }
        }
        SequentialStatement resultBody = new SequentialStatement();
        resultBody.getSequence().addAll(rootStmt.getBody());
        result.setStatement(resultBody);
        return result;
    }

    private void closeExpiredBookmarks(StatementGenerator generator, int node, List<TryCatchBlock> tryCatchBlocks) {
        tryCatchBlocks = new ArrayList<>(tryCatchBlocks);
        Collections.reverse(tryCatchBlocks);

        // Find which try catch blocks have remained since the previous basic block
        int sz = Math.min(tryCatchBlocks.size(), tryCatchBookmarks.size());
        int start;
        for (start = 0; start < sz; ++start) {
            TryCatchBlock tryCatch = tryCatchBlocks.get(start);
            TryCatchBookmark bookmark = tryCatchBookmarks.get(start);
            if (tryCatch.getHandler().getIndex() != bookmark.exceptionHandler) {
                break;
            }
            if (!Objects.equals(tryCatch.getExceptionType(), bookmark.exceptionType)) {
                break;
            }
        }

        // Close old bookmarks
        List<TryCatchBookmark> removedBookmarks = new ArrayList<>();
        for (int i = tryCatchBookmarks.size() - 1; i >= start; --i) {
            TryCatchBookmark bookmark = tryCatchBookmarks.get(i);
            bookmark.block.tryCatches.remove(bookmark);
            removedBookmarks.add(bookmark);
        }

        Collections.reverse(removedBookmarks);
        for (TryCatchBookmark bookmark : removedBookmarks) {
            Block block = stack.peek();
            while (block != bookmark.block) {
                TryCatchStatement tryCatchStmt = new TryCatchStatement();
                tryCatchStmt.setExceptionType(bookmark.exceptionType);
                tryCatchStmt.setExceptionVariable(bookmark.exceptionVariable);
                tryCatchStmt.getHandler().add(generator.generateJumpStatement(
                        program.basicBlockAt(bookmark.exceptionHandler)));
                tryCatchStmt.getProtectedBody().addAll(block.body);
                block.body.clear();
                if (!tryCatchStmt.getProtectedBody().isEmpty()) {
                    block.body.add(tryCatchStmt);
                }
                block = block.parent;
            }
            TryCatchStatement tryCatchStmt = new TryCatchStatement();
            tryCatchStmt.setExceptionType(bookmark.exceptionType);
            tryCatchStmt.setExceptionVariable(bookmark.exceptionVariable);
            if (node != bookmark.exceptionHandler) {
                tryCatchStmt.getHandler().add(generator.generateJumpStatement(
                        program.basicBlockAt(bookmark.exceptionHandler)));
            }
            List<Statement> blockPart = block.body.subList(bookmark.offset, block.body.size());
            tryCatchStmt.getProtectedBody().addAll(blockPart);
            blockPart.clear();
            if (!tryCatchStmt.getProtectedBody().isEmpty()) {
                blockPart.add(tryCatchStmt);
            }
        }

        tryCatchBookmarks.subList(start, tryCatchBookmarks.size()).clear();
    }

    private void createNewBookmarks(List<TryCatchBlock> tryCatchBlocks) {
        // Add new bookmarks
        for (int i = tryCatchBookmarks.size(); i < tryCatchBlocks.size(); ++i) {
            TryCatchBlock tryCatch = tryCatchBlocks.get(tryCatchBlocks.size() - 1 - i);
            TryCatchBookmark bookmark = new TryCatchBookmark();
            bookmark.block = stack.peek();
            bookmark.offset = bookmark.block.body.size();
            bookmark.exceptionHandler = tryCatch.getHandler().getIndex();
            bookmark.exceptionType = tryCatch.getExceptionType();
            bookmark.exceptionVariable = tryCatch.getHandler().getExceptionVariable() != null
                    ? tryCatch.getHandler().getExceptionVariable().getIndex() : null;
            bookmark.block.tryCatches.add(bookmark);
            tryCatchBookmarks.add(bookmark);
        }
    }

    private List<Block> createBlocks(int start) {
        List<Block> result = new ArrayList<>();
        while (currentNode != null && currentNode.getStart() == start) {
            Block block;
            IdentifiedStatement statement;
            boolean loop = false;
            if (loopSuccessors[start] == currentNode.getEnd() || isSingleBlockLoop(start)) {
                WhileStatement whileStatement = new WhileStatement();
                statement = whileStatement;
                block = new Block(statement, whileStatement.getBody(), start, currentNode.getEnd());
                loop = true;
            } else {
                BlockStatement blockStatement = new BlockStatement();
                statement = blockStatement;
                block = new Block(statement, blockStatement.getBody(), start, currentNode.getEnd());
            }
            result.add(block);
            int mappedIndex = indexer.nodeAt(currentNode.getEnd());
            if (mappedIndex >= 0 && (blockMap[mappedIndex] == null
                    || !(blockMap[mappedIndex].statement instanceof WhileStatement))) {
                blockMap[mappedIndex] = block;
            }
            if (loop) {
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

    private boolean isSingleBlockLoop(int index) {
        for (int succ : graph.outgoingEdges(index)) {
            if (succ == index) {
                return true;
            }
        }
        return false;
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
        for (int node = 0; node < sz; ++node) {
            if (isSingleBlockLoop(node)) {
                ranges.add(new RangeTree.Range(node, node + 1));
            }
        }
        codeTree = new RangeTree(sz + 1, ranges);
        this.loopSuccessors = loopSuccessors;
        this.loops = loops;
    }
}
