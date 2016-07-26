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
package org.teavm.wasm.decompile;

import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.common.IntegerArray;
import org.teavm.common.Loop;
import org.teavm.common.LoopGraph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TypeInferer;
import org.teavm.model.util.VariableType;
import org.teavm.wasm.model.WasmFunction;
import org.teavm.wasm.model.WasmLocal;
import org.teavm.wasm.model.WasmType;
import org.teavm.wasm.model.expression.WasmBlock;
import org.teavm.wasm.model.expression.WasmConditional;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmIntBinary;
import org.teavm.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.wasm.model.expression.WasmIntType;
import org.teavm.wasm.model.expression.WasmLocalReference;

public class WasmDecompiler {
    private WasmFunction function;
    private Program program;
    private LoopGraph cfg;
    private DominatorTree dom;
    private Graph domGraph;
    private Label[] labels;
    private TypeInferer typeInferer;
    private Step[] stack;
    private int stackTop;

    public void decompile(Program program, MethodReference methodReference, WasmFunction function) {
        this.function = function;
        this.program = program;
        prepare(methodReference);
        deferEnter(0, function.getBody());
        run();
    }

    private void prepare(MethodReference methodReference) {
        cfg = new LoopGraph(ProgramUtils.buildControlFlowGraph(program));
        dom = GraphUtils.buildDominatorTree(cfg);
        domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());
        labels = new Label[cfg.size()];
        stack = new Step[cfg.size() * 4];
        typeInferer = new TypeInferer();
        typeInferer.inferTypes(program, methodReference);

        int maxLocal = 0;
        for (int i = 0; i < program.variableCount(); ++i) {
            maxLocal = Math.max(maxLocal, program.variableAt(i).getRegister());
        }

        WasmType[] types = new WasmType[maxLocal];
        for (int i = 0; i < program.variableCount(); ++i) {
            int register = program.variableAt(i).getRegister();
            if (types[register] == null) {
                types[register] = DecompileSupport.mapType(typeInferer.typeOf(i));
            }
        }

        for (int i = 0; i < maxLocal; ++i) {
            WasmType type = types[i];
            function.getLocalVariables().add(new WasmLocal(type != null ? type : WasmType.INT32, null));
        }
    }

    private void run() {
        while (stackTop > 0) {
            stack[stackTop--].perform();
        }
    }

    private void push(Step step) {
        stack[stackTop++] = step;
    }

    private void deferEnter(int node, List<WasmExpression> expressions) {
        push(() -> enter(node, expressions));
    }

    private void enter(int node, List<WasmExpression> expressions) {
        Loop loop = cfg.loopAt(node);
        if (loop != null && loop.getHead() == node) {
            expressions = enterLoop(loop, expressions);
        }

        DecompilationVisitor decompilationVisitor = new DecompilationVisitor(expressions, context);
        compileInstructions(node, decompilationVisitor);

        int successors = domGraph.outgoingEdgesCount(node);
        if (successors == 1) {
            deferEnter(domGraph.outgoingEdges(node)[0], expressions);
        } else if (successors == 0) {
            BasicBlock block = program.basicBlockAt(node);
            block.getLastInstruction().acceptVisitor(decompilationVisitor);
        } else {
            compileFork(node, expressions);
        }
    }

    private void compileInstructions(int node, DecompilationVisitor decompilationVisitor) {
        BasicBlock block = program.basicBlockAt(node);
        int lastInstructionIndex = block.getInstructions().size() - 1;
        for (int i = 0; i < lastInstructionIndex; ++i) {
            block.getInstructions().get(i).acceptVisitor(decompilationVisitor);
        }
    }

    private List<WasmExpression> enterLoop(Loop loop, List<WasmExpression> expressions) {
        WasmBlock wasmLoop = new WasmBlock(true);
        List<WasmExpression> innerExpressions = wasmLoop.getBody();
        expressions.add(wasmLoop);
        LoopExitStep exitStep = new LoopExitStep(expressions, wasmLoop);
        push(exitStep);

        IntObjectMap<Label> newLabels = new IntObjectOpenHashMap<>();
        int[] exits = findLoopExits(loop);
        Label breakLabel = exitStep.createBreakLabel();
        for (int exit : exits) {
            newLabels.put(exit, breakLabel);
        }
        newLabels.put(loop.getHead(), exitStep.createContinueLabel());
        withLabels(newLabels);

        return innerExpressions;
    }

    private int[] findLoopExits(Loop loop) {
        IntegerArray exits = new IntegerArray(2);
        for (int i = 0; i < cfg.size(); ++i) {
            Loop nodeLoop = cfg.loopAt(i);
            if (nodeLoop != null && nodeLoop.isChildOf(loop)) {
                for (int successor : cfg.outgoingEdges(i)) {
                    Loop successorLoop = cfg.loopAt(successor);
                    if (successorLoop == null || !successorLoop.isChildOf(loop)) {
                        exits.add(i);
                        break;
                    }
                }
            }
        }
        return exits.getAll();
    }

    private void compileFork(int node, List<WasmExpression> expressions) {
        Instruction instruction = program.basicBlockAt(node).getLastInstruction();
        if (instruction instanceof BranchingInstruction) {
            compileFork(node, expressions, (BranchingInstruction) instruction);
        } else if (instruction instanceof BinaryBranchingInstruction) {
            compileFork(node, expressions, (BinaryBranchingInstruction) instruction);
        }
    }

    private void compileFork(int node, List<WasmExpression> expressions, BranchingInstruction instruction) {
        WasmLocal local = function.getLocalVariables().get(instruction.getOperand().getRegister());
        WasmExpression operand = new WasmLocalReference(local);
        VariableType operandType = typeInferer.typeOf(instruction.getOperand().getIndex());
        WasmExpression condition = DecompileSupport.getCondition(instruction, local, operandType);
        compileIf(node, expressions, condition, instruction.getConsequent().getIndex(),
                instruction.getAlternative().getIndex());
    }

    private void compileFork(int node, List<WasmExpression> expressions, BinaryBranchingInstruction instruction) {
        WasmLocal a = function.getLocalVariables().get(instruction.getFirstOperand().getIndex());
        WasmLocal b = function.getLocalVariables().get(instruction.getSecondOperand().getIndex());
        WasmExpression condition = DecompileSupport.getCondition(instruction, a, b);
        compileIf(node, expressions, condition, instruction.getConsequent().getIndex(),
                instruction.getAlternative().getIndex());
    }

    private void compileIf(int node, List<WasmExpression> expressions, WasmExpression condition,
            int thenNode, int elseNode) {
        boolean ownsThen = dom.directlyDominates(node, thenNode);
        boolean ownsElse = dom.directlyDominates(node, elseNode);
        int[] exits = Arrays.stream(domGraph.outgoingEdges(node))
                .filter(n -> n != thenNode && n != elseNode)
                .toArray();

        if (ownsThen && ownsElse) {
            WasmConditional conditional = new WasmConditional(condition);
            expressions.add(conditional);
            compileBranch(conditional.getElseBlock(), elseNode, exits);
            compileBranch(conditional.getThenBlock(), thenNode, exits);
        }
    }


    private void withLabels(IntObjectMap<Label> labels) {
        IntObjectMap<Label> undo = new IntObjectOpenHashMap<>();
        for (int node : labels.keys().toArray()) {
            undo.put(node, this.labels[node]);
        }
        push(() -> withLabelsImpl(undo));
        withLabelsImpl(labels);
    }

    private void withLabelsImpl(IntObjectMap<Label> labels) {
        for (int node : labels.keys().toArray()) {
            this.labels[node] = labels.get(node);
        }
    }
}
