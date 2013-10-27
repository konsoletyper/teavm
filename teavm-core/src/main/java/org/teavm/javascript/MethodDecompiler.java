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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.teavm.codegen.DefaultAliasProvider;
import org.teavm.codegen.DefaultNamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.common.*;
import org.teavm.javascript.ast.*;
import org.teavm.model.*;
import org.teavm.model.util.ProgramUtils;
import org.teavm.parsing.Parser;

/**
 *
 * @author Alexey Andreev
 */
public class MethodDecompiler {
    private ClassHolderSource classSource;
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

    public MethodDecompiler(ClassHolderSource classSource) {
        this.classSource = classSource;
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

    public RenderableMethod decompile(MethodHolder method) {
        lastBlockId = 1;
        indexer = new GraphIndexer(ProgramUtils.buildControlFlowGraph(method.getProgram()));
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
        generator.outgoings = getPhiOutgoings(program);
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
                generator.nextBlock = next < indexer.size() ? program.basicBlockAt(indexer.nodeAt(next)) : null;
                generator.statements.clear();
                for (Instruction insn : generator.currentBlock.getInstructions()) {
                    insn.acceptVisitor(generator);
                }
                block.body.addAll(generator.statements);
            }
        }
        SequentialStatement result = new SequentialStatement();
        result.getSequence().addAll(rootStmt.getBody());
        RenderableMethod renderable = new RenderableMethod(method);
        renderable.setBody(result);
        renderable.setVariableCount(program.variableCount());
        return renderable;
    }

    private Incoming[][] getPhiOutgoings(Program program) {
        List<List<Incoming>> outgoings = new ArrayList<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            outgoings.add(new ArrayList<Incoming>());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock basicBlock = program.basicBlockAt(i);
            for (Phi phi : basicBlock.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    outgoings.get(incoming.getSource().getIndex()).add(incoming);
                }
            }
        }
        Incoming[][] result = new Incoming[outgoings.size()][];
        for (int i = 0; i < outgoings.size(); ++i) {
            result[i] = outgoings.get(i).toArray(new Incoming[0]);
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

    public static void main(String... args) throws IOException {
        MutableClassHolderSource source = new MutableClassHolderSource();
        ClassHolder arrayListCls = Parser.parseClass(readClass(ArrayList.class.getName()));
        source.putClassHolder(arrayListCls);
        source.putClassHolder(Parser.parseClass(readClass(AbstractList.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(StringBuilder.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(IllegalArgumentException.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(IndexOutOfBoundsException.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(Exception.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(RuntimeException.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(Throwable.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(System.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(Object.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(Arrays.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(ArrayList.class.getName() + "$ListItr")));
        source.putClassHolder(Parser.parseClass(readClass(ArrayList.class.getName() + "$Itr")));
        source.putClassHolder(Parser.parseClass(readClass(ArrayList.class.getName() + "$SubList")));
        source.putClassHolder(Parser.parseClass(readClass(Collection.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(ObjectOutputStream.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(ObjectInputStream.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(ConcurrentModificationException.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(Math.class.getName())));
        source.putClassHolder(Parser.parseClass(readClass(OutOfMemoryError.class.getName())));
        MethodDecompiler decompiler = new MethodDecompiler(source);
        DefaultAliasProvider aliasProvider = new DefaultAliasProvider();
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, source);
        SourceWriter writer = new SourceWriter(naming);
        Renderer renderer = new Renderer(writer, source);
        Optimizer optimizer = new Optimizer();
        for (MethodHolder method : arrayListCls.getMethods()) {
            RenderableMethod renderableMethod = decompiler.decompile(method);
            optimizer.optimize(renderableMethod);
            renderer.render(renderableMethod);
        }
        System.out.println(writer);
    }

    private static ClassNode readClass(String className) throws IOException {
        ClassLoader classLoader = MethodDecompiler.class.getClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(className.replace('.', '/') + ".class")) {
            ClassReader reader = new ClassReader(input);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            return node;
        }
    }
}
