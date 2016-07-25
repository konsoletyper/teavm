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
import org.teavm.common.IntegerArray;
import org.teavm.common.Loop;
import org.teavm.common.LoopGraph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.util.VariableType;
import org.teavm.wasm.model.WasmLocal;
import org.teavm.wasm.model.expression.WasmBlock;
import org.teavm.wasm.model.expression.WasmConditional;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmFloat32Constant;
import org.teavm.wasm.model.expression.WasmFloat64Constant;
import org.teavm.wasm.model.expression.WasmFloatBinary;
import org.teavm.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.wasm.model.expression.WasmFloatType;
import org.teavm.wasm.model.expression.WasmInt32Constant;
import org.teavm.wasm.model.expression.WasmInt64Constant;
import org.teavm.wasm.model.expression.WasmIntBinary;
import org.teavm.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.wasm.model.expression.WasmIntType;
import org.teavm.wasm.model.expression.WasmLocalReference;

public class EnterStep implements Step {
    private Context context;
    private List<WasmExpression> expressions;
    private List<WasmExpression> innerExpressions;
    private int node;
    private DecompilationVisitor decompilationVisitor;

    public EnterStep(Context context, List<WasmExpression> expressions, int node) {
        this.context = context;
        this.expressions = expressions;
        this.innerExpressions = expressions;
        this.node = node;
    }

    @Override
    public void perform() {
        LoopGraph cfg = context.getCfg();
        Graph domGraph = context.getDomGraph();

        Loop loop = cfg.loopAt(node);
        if (loop != null && loop.getHead() == node) {
            enterLoop(loop);
        }

        decompilationVisitor = new DecompilationVisitor(innerExpressions, context);
        compileInstructions();

        int successors = domGraph.outgoingEdgesCount(node);
        if (successors == 1) {
            enterNext(domGraph.outgoingEdges(node)[0]);
        } else if (successors == 0) {
            BasicBlock block = context.getProgram().basicBlockAt(node);
            block.getLastInstruction().acceptVisitor(decompilationVisitor);
        } else {
            compileFork();
        }
    }

    private void compileInstructions() {
        BasicBlock block = context.getProgram().basicBlockAt(node);
        int lastInstructionIndex = block.getInstructions().size() - 1;
        for (int i = 0; i < lastInstructionIndex; ++i) {
            block.getInstructions().get(i).acceptVisitor(decompilationVisitor);
        }
    }

    private void enterLoop(Loop loop) {
        WasmBlock wasmLoop = new WasmBlock(true);
        innerExpressions = wasmLoop.getBody();
        expressions.add(wasmLoop);
        LoopExitStep exitStep = new LoopExitStep(expressions, wasmLoop);
        context.push(exitStep);

        IntObjectMap<Label> newLabels = new IntObjectOpenHashMap<>();
        int[] exits = findLoopExits(loop);
        Label breakLabel = exitStep.createBreakLabel();
        for (int exit : exits) {
            newLabels.put(exit, breakLabel);
        }
        newLabels.put(loop.getHead(), exitStep.createContinueLabel());
        ContextUtils.withLabels(context, newLabels);
    }

    private int[] findLoopExits(Loop loop) {
        IntegerArray exits = new IntegerArray(2);
        LoopGraph cfg = context.getCfg();
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

    private void compileFork() {
        Instruction instruction = context.getProgram().basicBlockAt(node).getLastInstruction();
        if (instruction instanceof BranchingInstruction) {
            compileFork((BranchingInstruction) instruction);
        } else if (instruction instanceof BinaryBranchingInstruction) {
            compileFork((BinaryBranchingInstruction) instruction);
        }
    }

    private void compileFork(BranchingInstruction instruction) {
        WasmLocal local = context.getLocal(instruction.getOperand().getIndex());
        WasmExpression operand = new WasmLocalReference(local);
        VariableType operandType = context.getLocalType(instruction.getOperand().getIndex());

        WasmExpression condition;
        switch (operandType) {
            case FLOAT:
                condition = new WasmFloatBinary(WasmFloatType.FLOAT32, getFloatCondition(instruction.getCondition()),
                        operand, new WasmFloat32Constant(0));
                break;
            case DOUBLE:
                condition = new WasmFloatBinary(WasmFloatType.FLOAT64, getFloatCondition(instruction.getCondition()),
                        operand, new WasmFloat64Constant(0));
                break;
            case INT:
                condition = new WasmIntBinary(WasmIntType.INT32, getIntCondition(instruction.getCondition()),
                        operand, new WasmInt32Constant(0));
                break;
            case LONG:
                condition = new WasmIntBinary(WasmIntType.INT64, getIntCondition(instruction.getCondition()),
                        operand, new WasmInt64Constant(0));
                break;
            default:
                condition = new WasmIntBinary(WasmIntType.INT32, getReferenceCondition(instruction.getCondition()),
                        operand, new WasmInt32Constant(0));
                break;
        }

        compileIf(condition, instruction.getConsequent().getIndex(), instruction.getAlternative().getIndex());
    }

    private void compileFork(BinaryBranchingInstruction instruction) {
        WasmExpression a = new WasmLocalReference(context.getLocal(instruction.getFirstOperand().getIndex()));
        WasmExpression b = new WasmLocalReference(context.getLocal(instruction.getSecondOperand().getIndex()));

        WasmExpression condition;
        switch (instruction.getCondition()) {
            case REFERENCE_EQUAL:
            case EQUAL:
                condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, a, b);
                break;
            case REFERENCE_NOT_EQUAL:
            case NOT_EQUAL:
                condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.NE, a, b);
                break;
            default:
                throw new IllegalArgumentException(instruction.getCondition().toString());
        }

        compileIf(condition, instruction.getConsequent().getIndex(), instruction.getAlternative().getIndex());
    }

    private WasmFloatBinaryOperation getFloatCondition(BranchingCondition condition) {
        switch (condition) {
            case EQUAL:
                return WasmFloatBinaryOperation.EQ;
            case NOT_EQUAL:
                return WasmFloatBinaryOperation.NE;
            case GREATER:
                return WasmFloatBinaryOperation.GT;
            case GREATER_OR_EQUAL:
                return WasmFloatBinaryOperation.GE;
            case LESS:
                return WasmFloatBinaryOperation.LT;
            case LESS_OR_EQUAL:
                return WasmFloatBinaryOperation.LE;
            case NULL:
            case NOT_NULL:
                break;
        }
        throw new IllegalArgumentException(condition.toString());
    }

    private WasmIntBinaryOperation getIntCondition(BranchingCondition condition) {
        switch (condition) {
            case EQUAL:
                return WasmIntBinaryOperation.EQ;
            case NOT_EQUAL:
                return WasmIntBinaryOperation.NE;
            case GREATER:
                return WasmIntBinaryOperation.GT_SIGNED;
            case GREATER_OR_EQUAL:
                return WasmIntBinaryOperation.GE_SIGNED;
            case LESS:
                return WasmIntBinaryOperation.LT_SIGNED;
            case LESS_OR_EQUAL:
                return WasmIntBinaryOperation.LE_SIGNED;
            case NULL:
            case NOT_NULL:
                break;
        }
        throw new IllegalArgumentException(condition.toString());
    }

    private WasmIntBinaryOperation getReferenceCondition(BranchingCondition condition) {
        switch (condition) {
            case NULL:
                return WasmIntBinaryOperation.EQ;
            case NOT_NULL:
                return WasmIntBinaryOperation.NE;
            default:
                break;
        }
        throw new IllegalArgumentException(condition.toString());
    }

    private void compileIf(WasmExpression condition, int thenNode, int elseNode) {
        DominatorTree dom = context.getDomTree();
        Graph domGraph = context.getDomGraph();

        boolean ownsThen = dom.directlyDominates(node, thenNode);
        boolean ownsElse = dom.directlyDominates(node, elseNode);
        int[] exits = Arrays.stream(domGraph.outgoingEdges(node))
                .filter(n -> n != thenNode && n != elseNode)
                .toArray();

        if (ownsThen && ownsElse) {
            WasmConditional conditional = new WasmConditional(condition);
            innerExpressions.add(conditional);
            compileBranch(conditional.getElseBlock(), elseNode, exits);
            compileBranch(conditional.getThenBlock(), thenNode, exits);
        }
    }

    private void compileBranch(WasmBlock block, int branchNode, int[] exits) {
        IntObjectMap<Label> thenLabels = new IntObjectOpenHashMap<>();
        for (int exit : exits) {
            thenLabels.put(exit, new BlockBreakLabel(block));
        }
        ContextUtils.withLabels(context, thenLabels);
        context.push(new EnterStep(context, block.getBody(), branchNode));
    }

    private void enterNext(int nextNode) {
        context.push(new EnterStep(context, innerExpressions, nextNode));
    }
}
