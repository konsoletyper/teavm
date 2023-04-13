/*
 *  Copyright 2021 Alexey Andreev.
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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BinaryOperation;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.OperationType;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnaryOperation;
import org.teavm.ast.WhileStatement;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.UsageExtractor;

public class NewDecompiler {
    private Program program;
    private Graph cfg;
    private DominatorTree dom;
    private Graph domGraph;
    private UsageExtractor usageExtractor = new UsageExtractor();
    private DefinitionExtractor definitionExtractor = new DefinitionExtractor();
    private int[] dfs;
    private int[] varUsageCount;
    private int[] varDefinitionCount;
    private boolean[] varUsedOnce;
    private List<ExprStackElement> exprStack = new ArrayList<>();
    private Expr[] relocatableVars;
    private List<Statement> statements;
    private BasicBlock currentBlock;
    private boolean returnedVariableRelocatable;
    private IdentifiedStatement[] jumpTargets;
    private BasicBlock nextBlock;
    private WhileStatement[] loopExits;
    private ObjectIntMap<IdentifiedStatement> identifiedStatementUseCount = new ObjectIntHashMap<>();
    private boolean[] processingLoops;
    private boolean[] processingTryCatch;
    private boolean[] currentSequenceNodes;
    private List<TryCatchElement> currentTryCatches = new ArrayList<>();
    private boolean[] processedTryCatchHandlers = new boolean[0];
    private boolean[] visitedBlocks = new boolean[0];

    public Statement decompile(Program program) {
        this.program = program;
        prepare();
        currentBlock = program.basicBlockAt(0);
        statements = new ArrayList<>();
        nextBlock = null;
        calculateResult();
        Statement result;
        if (statements.size() != 1) {
            var seq = new SequentialStatement();
            seq.getSequence().addAll(statements);
            result = seq;
        } else {
            result = statements.get(0);
        }
        cleanup();
        return result;
    }

    private static int blockEnterNode(BasicBlock block) {
        return blockEnterNode(block.getIndex());
    }

    private static int blockEnterNode(int blockIndex) {
        return blockIndex * 2;
    }

    private static int blockExitNode(BasicBlock block) {
        return blockExitNode(block.getIndex());
    }

    private static int blockExitNode(int blockIndex) {
        return blockIndex * 2 + 1;
    }

    private static int owningBlockIndex(int node) {
        return node / 2;
    }

    private BasicBlock owningBlock(int node) {
        return program.basicBlockAt(owningBlockIndex(node));
    }

    private int enteringBlockCount(BasicBlock block) {
        return cfg.incomingEdgesCount(block.getIndex() * 2);
    }

    private void prepare() {
        cfg = ProgramUtils.buildControlFlowGraph2(program);
        dfs = GraphUtils.dfs(cfg);
        dom = GraphUtils.buildDominatorTree(cfg);
        domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());
        relocatableVars = new Expr[program.variableCount()];
        jumpTargets = new IdentifiedStatement[program.basicBlockCount()];
        processingLoops = new boolean[program.basicBlockCount()];
        processingTryCatch = new boolean[program.basicBlockCount()];
        currentSequenceNodes = new boolean[program.basicBlockCount()];
        loopExits = new WhileStatement[program.basicBlockCount()];
        calculateVarInfo();
    }

    private void calculateVarInfo() {
        varUsageCount = new int[program.variableCount()];
        varDefinitionCount = new int[program.variableCount()];
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            var block = program.basicBlockAt(i);
            for (var instruction : block) {
                instruction.acceptVisitor(usageExtractor);
                instruction.acceptVisitor(definitionExtractor);
                var usedVars = usageExtractor.getUsedVariables();
                if (usedVars != null) {
                    for (var usedVar : usedVars) {
                        varUsageCount[usedVar.getIndex()]++;
                    }
                }
                var defVars = definitionExtractor.getDefinedVariables();
                if (defVars != null) {
                    for (var defVar : defVars) {
                        varDefinitionCount[defVar.getIndex()]++;
                    }
                }
            }
            if (block.getExceptionVariable() != null) {
                varDefinitionCount[block.getExceptionVariable().getIndex()]++;
            }
        }
    }

    private void cleanup() {
        program = null;
        dom = null;
        cfg = null;
        dfs = null;
        statements = null;
        relocatableVars = null;
        jumpTargets = null;
        currentSequenceNodes = null;
        loopExits = null;
    }

    private void calculateResult() {
        var tryCatchLevel = currentTryCatches.size();
        while (currentBlock != null) {
            if (!processingLoops[currentBlock.getIndex()] && isLoopHead()) {
                processLoop();
            } else if (!processTryCatchHeader()) {
                visitedBlocks[currentBlock.getIndex()] = true;
                applyNewTryCatchStack();
                for (var instruction : currentBlock) {
                    instruction.acceptVisitor(instructionDecompiler);
                }
            }
        }
        finishTryCatches(tryCatchLevel);
    }

    private void applyNewTryCatchStack() {
        var newTryCatches = currentBlock.getTryCatchBlocks();
        var maxCommonSize = Math.min(newTryCatches.size(), currentTryCatches.size());
        var commonSize = 0;
        for (; commonSize < maxCommonSize; ++commonSize) {
            if (!currentTryCatches.get(commonSize).equalTo(newTryCatches.get(commonSize))) {
                break;
            }
        }

        closeTryCatches(commonSize);

        for (var i = commonSize; i < newTryCatches.size(); ++i) {
            var tryCatch = newTryCatches.get(i);
            var next = i > 0 ? currentTryCatches.get(i - 1) : null;
            var element = new TryCatchElement(tryCatch.getExceptionType(), tryCatch.getHandler(),
                    statements, statements.size(), currentTryCatches.size(), next);
            currentTryCatches.add(element);
        }
    }

    private void closeTryCatches(int targetLevel) {
        while (currentTryCatches.size() > targetLevel) {
            var tryCatch = currentTryCatches.remove(currentTryCatches.size() - 1);
            var statementsToWrap = tryCatch.targetStatements.subList(tryCatch.start, tryCatch.targetStatements.size());
            var tryCatchStatement = new TryCatchStatement();
            tryCatchStatement.getProtectedBody().addAll(statementsToWrap);
            tryCatchStatement.setExceptionType(tryCatch.exceptionType);
            var exceptionVariable = tryCatch.handler.getExceptionVariable();
            tryCatchStatement.setExceptionVariable(exceptionVariable != null ? exceptionVariable.getRegister() : -1);
            var jumpTarget = getJumpTarget(tryCatch.handler);
            var breakStatement = new BreakStatement();
            breakStatement.setTarget(jumpTarget);
            tryCatchStatement.getHandler().add(breakStatement);
            statementsToWrap.clear();
            tryCatch.targetStatements.add(tryCatchStatement);
        }
    }

    private void finishTryCatches(int initialLevel) {
        closeTryCatches(initialLevel);
    }

    private boolean processTryCatchHeader() {
        if (processingTryCatch[currentBlock.getIndex()]) {
            return false;
        }
        var immediatelyDominatedNodes = domGraph.outgoingEdges(blockEnterNode(currentBlock));
        if (immediatelyDominatedNodes.length == 1) {
            return false;
        }

        var childBlockCount = immediatelyDominatedNodes.length;
        var childBlocks = new BasicBlock[childBlockCount];
        for (int i = 0; i < childBlocks.length; i++) {
            childBlocks[i] = owningBlock(immediatelyDominatedNodes[i]);
        }

        var blockStatements = new BlockStatement[childBlockCount];
        for (int i = 0; i < childBlocks.length; i++) {
            var childBlock = childBlocks[i];
            var blockStatement = new BlockStatement();
            jumpTargets[childBlock.getIndex()] = blockStatement;
            blockStatements[i] = blockStatement;
        }

        processingTryCatch[currentBlock.getIndex()] = true;
        processTryBlockStatements(blockStatements, childBlocks);
        processingTryCatch[currentBlock.getIndex()] = false;
        currentBlock = childBlocks.length > 0 ? childBlocks[childBlocks.length - 1] : null;
        return true;
    }

    private void processTryBlockStatements(BlockStatement[] blockStatements, BasicBlock[] childBlocks) {
        for (int i = 0; i < childBlocks.length - 1; ++i) {
            var prevBlockStatement = blockStatements[i];
            var childBlock = childBlocks[i];
            processBlock(childBlock, childBlocks[i + 1], prevBlockStatement.getBody());
            var blockStatement = blockStatements[i + 1];
            addChildBlock(prevBlockStatement, blockStatement.getBody());
        }
        var lastBlockStatement = blockStatements[blockStatements.length - 1];
        addChildBlock(lastBlockStatement, statements);
    }

    private void processLoop() {
        flushStack();
        var statementsBackup = statements;
        var nextBlockBackup = nextBlock;

        var loop = new WhileStatement();
        fillLoopNodes();
        var loopExit = getBestExit();
        if (loopExits[loopExit.getIndex()] != null) {
            loopExit = null;
        } else {
            loopExits[loopExit.getIndex()] = loop;
        }
        nextBlock = currentBlock;
        statements = loop.getBody();
        jumpTargets[currentBlock.getIndex()] = loop;
        processingLoops[currentBlock.getIndex()] = true;
        calculateResult();
        currentBlock = loopExit;
        exprStack.clear();
        optimizeLoop(loop);

        statements = statementsBackup;
        statements.add(loop);
        nextBlock = nextBlockBackup;
        if (loopExit != null) {
            loopExits[loopExit.getIndex()] = null;
        }
    }

    private void optimizeLoop(WhileStatement loop) {
        if (loop.getCondition() != null || loop.getBody().isEmpty()) {
            return;
        }
        var first = loop.getBody().get(0);
        if (!(first instanceof ConditionalStatement)) {
            return;
        }
        var firstIf = (ConditionalStatement) first;
        if (!firstIf.getAlternative().isEmpty() || firstIf.getConsequent().size() != 1) {
            return;
        }
        var firstIfThen = firstIf.getConsequent().get(0);
        if (!(firstIfThen instanceof BreakStatement)) {
            return;
        }
        var firstBreak = (BreakStatement) firstIfThen;
        if (firstBreak.getTarget() != loop) {
            return;
        }
        loop.getBody().remove(0);
        loop.setCondition(not(firstIf.getCondition()));
    }

    private BasicBlock getBestExit() {
        var stack = new IntStack();
        stack.push(currentBlock.getIndex());
        var nonLoopTargets = new IntArrayList();
        BasicBlock bestExit = null;
        int bestExitScore = 0;

        while (!stack.isEmpty()) {
            int node = stack.pop();
            var targets = domGraph.outgoingEdges(blockExitNode(node));

            for (int target : targets) {
                if (!currentSequenceNodes[owningBlockIndex(target)]) {
                    nonLoopTargets.add(owningBlockIndex(target));
                }
            }

            if (!nonLoopTargets.isEmpty()) {
                int bestNonLoopTarget = nonLoopTargets.get(0);
                for (int i = 1; i < nonLoopTargets.size(); ++i) {
                    int candidate = nonLoopTargets.get(i);
                    if (dfs[blockEnterNode(bestNonLoopTarget)] < dfs[blockEnterNode(candidate)]) {
                        bestNonLoopTarget = candidate;
                    }
                }
                nonLoopTargets.clear();
                int score = getComplexity(bestNonLoopTarget);
                if (score > bestExitScore) {
                    bestExitScore = score;
                    bestExit = program.basicBlockAt(bestNonLoopTarget);
                }
            }

            for (int target : targets) {
                if (currentSequenceNodes[owningBlockIndex(target)]) {
                    stack.push(owningBlockIndex(target));
                }
            }
        }

        return bestExit;
    }

    private int getComplexity(int blockIndex) {
        int complexity = 0;
        var stack = new IntStack();
        stack.push(blockIndex);
        var visited = new boolean[program.basicBlockCount()];
        while (!stack.isEmpty()) {
            blockIndex = stack.pop();
            if (visited[blockIndex]) {
                continue;
            }
            visited[blockIndex] = true;
            complexity += program.basicBlockAt(blockIndex).instructionCount();
            for (int successor : cfg.outgoingEdges(blockEnterNode(blockIndex))) {
                int successorIndex = owningBlockIndex(successor);
                if (!visited[successorIndex]) {
                    stack.push(successorIndex);
                }
            }
            for (int successor : cfg.outgoingEdges(blockExitNode(blockIndex))) {
                int successorIndex = owningBlockIndex(successor);
                if (!visited[successorIndex]) {
                    stack.push(successorIndex);
                }
            }
        }
        return complexity;
    }

    private void fillLoopNodes() {
        Arrays.fill(currentSequenceNodes, false);
        var stack = new int[cfg.size()];
        int stackPtr = 0;
        stack[stackPtr++] = currentBlock.getIndex();

        while (stackPtr > 0) {
            int node = stack[--stackPtr];
            if (currentSequenceNodes[node]) {
                continue;
            }
            currentSequenceNodes[node] = true;
            for (int source : cfg.incomingEdges(blockEnterNode(node))) {
                int sourceIndex = owningBlockIndex(source);
                if (!currentSequenceNodes[sourceIndex] && dom.dominates(blockEnterNode(currentBlock), source)) {
                    stack[stackPtr++] = sourceIndex;
                }
            }
        }
    }

    private boolean isLoopHead() {
        int enterNode = blockEnterNode(currentBlock);
        for (int source : cfg.incomingEdges(enterNode)) {
            if (dom.dominates(enterNode, source)) {
                return true;
            }
        }
        return false;
    }

    private void processBlock(BasicBlock block, BasicBlock next, List<Statement> statements) {
        var currentBlockBackup = currentBlock;
        var nextBlockBackup = nextBlock;
        var stackBackup = exprStack;
        var statementsBackup = this.statements;

        currentBlock = block;
        nextBlock = next;
        exprStack = new ArrayList<>();
        this.statements = statements;
        calculateResult();

        currentBlock = currentBlockBackup;
        nextBlock = nextBlockBackup;
        exprStack = stackBackup;
        this.statements = statementsBackup;
    }

    private void assignVariable(int variable, Expr value, boolean relocatable) {
        if (varUsageCount[variable] <= 1 && varDefinitionCount[variable] == 1) {
            if (relocatable) {
                relocatableVars[variable] = value;
            } else {
                exprStack.add(new ExprStackElement(variable, value));
            }
        } else {
            if (!relocatable) {
                flushStack();
            }
            var statement = Statement.assign(Expr.var(variable), value);
            statements.add(statement);
        }
    }

    private void assignConstant(int variable, Expr value) {
        if (varDefinitionCount[variable] == 1) {
            relocatableVars[variable] = value;
        } else {
            var statement = Statement.assign(Expr.var(variable), value);
            statements.add(statement);
        }
    }

    private Expr getVariable(int variable) {
        int usageCount = varUsageCount[variable];
        var relocatable = relocatableVars[variable];
        if (relocatable != null) {
            returnedVariableRelocatable = true;
            return relocatable;
        }

        if (usageCount == 1 && !exprStack.isEmpty()) {
            var index = exprStack.size() - 1;
            var element = exprStack.get(index);
            if (exprStack.get(index).variable == variable) {
                exprStack.remove(index);
                returnedVariableRelocatable = false;
                return element.value;
            }
        }

        returnedVariableRelocatable = true;
        return Expr.var(variable);
    }

    private void flushStack() {
        int j = 0;
        for (var element : exprStack) {
            var statement = Statement.assign(Expr.var(element.variable), element.value);
            statements.add(statement);
        }
        exprStack.subList(j, exprStack.size()).clear();
    }

    private Expr and(Expr a, Expr b) {
        if (a instanceof UnaryExpr && b instanceof UnaryExpr) {
            if (((UnaryExpr) a).getOperation() == UnaryOperation.NOT
                    && ((UnaryExpr) b).getOperation() == UnaryOperation.NOT) {
                return Expr.invert(Expr.or(((UnaryExpr) a).getOperand(), ((UnaryExpr) b).getOperand()));
            }
        }
        return Expr.and(a, b);
    }

    private Expr not(Expr expr) {
        if (expr instanceof UnaryExpr) {
            var unary = (UnaryExpr) expr;
            if (unary.getOperation() == UnaryOperation.NOT) {
                return unary.getOperand();
            }
        }
        return Expr.invert(expr);
    }


    private boolean isIntZero(Expr expr) {
        if (!(expr instanceof ConstantExpr)) {
            return false;
        }
        var value = ((ConstantExpr) expr).getValue();
        return Integer.valueOf(0).equals(value);
    }

    private Expr cond(BinaryOperation op, NumericOperandType opType, Variable firstOp, Variable secondOp) {
        var second = getVariable(secondOp.getIndex());
        var first = getVariable(firstOp.getIndex());
        return Expr.binary(op, mapNumericType(opType), first, second);
    }

    private Expr cond(BinaryOperation op, NumericOperandType opType, Variable firstOp, Expr second) {
        var first = getVariable(firstOp.getIndex());
        if (opType == NumericOperandType.INT && isIntZero(second)) {
            if (first instanceof BinaryExpr) {
                var firstBinary = (BinaryExpr) first;
                if (firstBinary.getOperation() == BinaryOperation.SUBTRACT
                        || firstBinary.getOperation() == BinaryOperation.COMPARE) {
                    return Expr.binary(op, firstBinary.getType(), firstBinary.getFirstOperand(),
                            firstBinary.getSecondOperand());
                }
            }
        }
        return Expr.binary(op, mapNumericType(opType), first, second);
    }

    private void branch(Expr condition, BasicBlock ifTrue, BasicBlock ifFalse) {
        if (loopExits[ifTrue.getIndex()] != null) {
            loopExitBranch(condition, ifTrue, ifTrue);
            return;
        } else if (loopExits[ifFalse.getIndex()] != null) {
            loopExitBranch(not(condition), ifFalse, ifTrue);
            return;
        }

        int sourceNode = blockExitNode(currentBlock);
        var immediatelyDominatedNodes = domGraph.outgoingEdges(sourceNode);
        boolean ownsTrueBranch = ownsBranch(ifTrue);
        boolean ownsFalseBranch = ownsBranch(ifFalse);

        int childBlockCount = immediatelyDominatedNodes.length;
        if (ownsTrueBranch) {
            childBlockCount--;
        }
        if (ownsFalseBranch) {
            childBlockCount--;
        }
        var childBlocks = new BasicBlock[childBlockCount];
        int j = 0;
        for (var immediatelyDominatedNode : immediatelyDominatedNodes) {
            var childBlock = owningBlock(immediatelyDominatedNode);
            if (ownsTrueBranch && childBlock == ifTrue
                    || ownsFalseBranch && childBlock == ifFalse) {
                continue;
            }
            childBlocks[j++] = childBlock;
        }
        Arrays.sort(childBlocks, Comparator.comparing(b -> dfs[b.getIndex()]));

        var blockStatements = new BlockStatement[childBlockCount];
        for (int i = 0; i < childBlocks.length; i++) {
            var childBlock = childBlocks[i];
            var blockStatement = new BlockStatement();
            jumpTargets[childBlock.getIndex()] = blockStatement;
            blockStatements[i] = blockStatement;
        }

        var firstChildBlock = childBlocks.length > 0 ? childBlocks[0] : null;
        var blockAfterIf = firstChildBlock != null ? firstChildBlock : nextBlock;
        var ifStatement = new ConditionalStatement();
        ifStatement.setCondition(condition);

        if (ownsTrueBranch) {
            processBlock(ifTrue, blockAfterIf, ifStatement.getConsequent());
        } else {
            addJumpStatement(ifStatement.getConsequent(), ifTrue, blockAfterIf);
        }

        if (ownsFalseBranch) {
            processBlock(ifFalse, blockAfterIf, ifStatement.getAlternative());
        } else {
            addJumpStatement(ifStatement.getAlternative(), ifFalse, blockAfterIf);
        }

        optimizeIf(ifStatement);

        processBlockStatements(blockStatements, childBlocks, ifStatement);

        currentBlock = childBlocks.length > 0 ? childBlocks[childBlocks.length - 1] : null;
    }

    private void switchBranch(Expr condition, SwitchInstruction instruction) {
        int sourceNode = blockExitNode(currentBlock);
        var immediatelyDominatedNodes = domGraph.outgoingEdges(sourceNode);
        var childBlockCount = immediatelyDominatedNodes.length;

        var targets = new LinkedHashSet<BasicBlock>(instruction.getEntries().size());
        for (var entry : instruction.getEntries()) {
            targets.add(entry.getTarget());
        }
        targets.add(instruction.getDefaultTarget());

        var isRegularBranch = new boolean[program.basicBlockCount()];
        for (var target : targets) {
            if (cfg.incomingEdgesCount(blockEnterNode(target)) == 1) {
                childBlockCount--;
                isRegularBranch[target.getIndex()] = true;
            }
        }

        var childBlocks = new BasicBlock[childBlockCount];
        int j = 0;
        for (var immediatelyDominatedNode : immediatelyDominatedNodes) {
            var childBlock = owningBlock(immediatelyDominatedNode);
            if (isRegularBranch[childBlock.getIndex()]) {
                continue;
            }
            childBlocks[j++] = childBlock;
        }
        Arrays.sort(childBlocks, Comparator.comparing(b -> dfs[b.getIndex()]));

        var blockStatements = new BlockStatement[childBlockCount];
        for (int i = 0; i < childBlocks.length; i++) {
            var childBlock = childBlocks[i];
            var blockStatement = new BlockStatement();
            jumpTargets[childBlock.getIndex()] = blockStatement;
            blockStatements[i] = blockStatement;
        }

        var firstChildBlock = childBlocks.length > 0 ? childBlocks[0] : null;
        var blockAfterSwitch = firstChildBlock != null ? firstChildBlock : nextBlock;
        var switchStatement = new SwitchStatement();
        switchStatement.setValue(condition);

        var clausesByBlock = new SwitchClauseProto[program.basicBlockCount()];
        var clauses = new ArrayList<SwitchClauseProto>();
        for (var entry : instruction.getEntries()) {
            var clause = clausesByBlock[entry.getTarget().getIndex()];
            if (clause == null) {
                clause = new SwitchClauseProto(new SwitchClause());
                clausesByBlock[entry.getTarget().getIndex()] = clause;
                clauses.add(clause);
                switchStatement.getClauses().add(clause.clause);
                if (dom.dominates(sourceNode, blockEnterNode(entry.getTarget()))
                        && isRegularBranch[entry.getTarget().getIndex()]) {
                    processBlock(entry.getTarget(), blockAfterSwitch, clause.clause.getBody());
                } else {
                    addJumpStatement(clause.clause.getBody(), entry.getTarget(), blockAfterSwitch);
                }
            }
            clause.conditions.add(entry.getCondition());
        }

        if (dom.dominates(sourceNode, blockEnterNode(instruction.getDefaultTarget()))) {
            processBlock(instruction.getDefaultTarget(), blockAfterSwitch, switchStatement.getDefaultClause());
        } else {
            addJumpStatement(switchStatement.getDefaultClause(), instruction.getDefaultTarget(), blockAfterSwitch);
        }

        for (var clause : clauses) {
            clause.clause.setConditions(clause.conditions.toArray());
        }

        processBlockStatements(blockStatements, childBlocks, switchStatement);

        currentBlock = childBlocks.length > 0 ? childBlocks[childBlocks.length - 1] : null;
    }

    private void processBlockStatements(BlockStatement[] blockStatements, BasicBlock[] childBlocks,
            Statement mainStatement) {
        if (blockStatements.length > 0) {
            for (int i = 0; i < childBlocks.length - 1; ++i) {
                var prevBlockStatement = blockStatements[i];
                optimizeConditionalBlock(prevBlockStatement);
                var blockStatement = blockStatements[i + 1];
                addChildBlock(prevBlockStatement, blockStatement.getBody());
                var childBlock = childBlocks[i];
                processBlock(childBlock, childBlocks[i + 1], blockStatement.getBody());
            }
            var lastBlockStatement = blockStatements[blockStatements.length - 1];
            optimizeConditionalBlock(lastBlockStatement);
            addChildBlock(lastBlockStatement, statements);
            blockStatements[0].getBody().add(mainStatement);
        } else {
            statements.add(mainStatement);
        }
    }

    private static class SwitchClauseProto {
        final SwitchClause clause;
        final IntArrayList conditions = new IntArrayList();

        SwitchClauseProto(SwitchClause clause) {
            this.clause = clause;
        }
    }

    private void loopExitBranch(Expr expr, BasicBlock loopExit, BasicBlock next) {
        var ifStatement = new ConditionalStatement();
        ifStatement.setCondition(expr);
        var breakStatement = new BreakStatement();
        breakStatement.setTarget(loopExits[loopExit.getIndex()]);
        ifStatement.getConsequent().add(breakStatement);
        statements.add(ifStatement);
        currentBlock = next;
    }

    private void addChildBlock(BlockStatement blockStatement, List<Statement> containingList) {
        if (identifiedStatementUseCount.get(blockStatement) > 0) {
            containingList.add(blockStatement);
        } else {
            containingList.addAll(blockStatement.getBody());
        }
    }

    private boolean ownsBranch(BasicBlock branch) {
        return dom.immediateDominatorOf(blockEnterNode(branch)) == blockExitNode(currentBlock)
                && enteringBlockCount(branch) == 1;
    }

    private void optimizeConditionalBlock(BlockStatement statement) {
        while (optimizeFirstIfWithLastBreak(statement)) {
            // repeat
        }
    }

    private boolean optimizeFirstIfWithLastBreak(BlockStatement statement) {
        if (statement.getBody().isEmpty()) {
            return false;
        }
        var firstStatement = statement.getBody().get(0);
        if (!(firstStatement instanceof ConditionalStatement)) {
            return false;
        }
        var nestedIf = (ConditionalStatement) firstStatement;
        if (nestedIf.getConsequent().isEmpty()) {
            return false;
        }
        var last = nestedIf.getConsequent().get(nestedIf.getConsequent().size() - 1);
        if (!(last instanceof BreakStatement)) {
            return false;
        }
        if (((BreakStatement) last).getTarget() != statement) {
            return false;
        }
        nestedIf.getConsequent().remove(nestedIf.getConsequent().size() - 1);
        var statementsToMove = statement.getBody().subList(1, statement.getBody().size());
        nestedIf.getAlternative().addAll(statementsToMove);
        statementsToMove.clear();
        identifiedStatementUseCount.put(statement, identifiedStatementUseCount.get(statement) - 1);
        optimizeIf(nestedIf);
        return true;
    }

    private boolean optimizeIf(ConditionalStatement statement) {
        return invertIf(statement) | mergeNestedIfs(statement) | invertNotCondition(statement);
    }

    private boolean invertIf(ConditionalStatement statement) {
        if (statement.getAlternative().isEmpty() || !statement.getConsequent().isEmpty()) {
            return false;
        }
        statement.setCondition(not(statement.getCondition()));
        statement.getConsequent().addAll(statement.getAlternative());
        statement.getAlternative().clear();
        return true;
    }

    private boolean mergeNestedIfs(ConditionalStatement statement) {
        if (!statement.getAlternative().isEmpty() || statement.getConsequent().size() != 1) {
            return false;
        }
        var firstNested = statement.getConsequent().get(0);
        if (!(firstNested instanceof ConditionalStatement)) {
            return false;
        }
        var nestedIf = (ConditionalStatement) firstNested;
        if (!nestedIf.getAlternative().isEmpty()) {
            return false;
        }
        statement.getConsequent().clear();
        statement.getConsequent().addAll(nestedIf.getConsequent());
        statement.setCondition(and(statement.getCondition(), nestedIf.getCondition()));
        invertNotCondition(statement);
        return true;
    }

    private boolean invertNotCondition(ConditionalStatement statement) {
        if (!statement.getConsequent().isEmpty() && !statement.getAlternative().isEmpty()
                && statement.getCondition() instanceof UnaryExpr
                && ((UnaryExpr) statement.getCondition()).getOperation() == UnaryOperation.NOT) {
            statement.setCondition(((UnaryExpr) statement.getCondition()).getOperand());
            var tmp = new ArrayList<>(statement.getAlternative());
            statement.getAlternative().clear();
            statement.getAlternative().addAll(statement.getConsequent());
            statement.getConsequent().clear();
            statement.getConsequent().addAll(tmp);
            return true;
        }
        return false;
    }

    private void binary(BinaryOperation op, NumericOperandType opType, BinaryInstruction insn,
            boolean relocatable) {
        var second = getVariable(insn.getSecondOperand().getIndex());
        relocatable &= returnedVariableRelocatable;
        var first = getVariable(insn.getFirstOperand().getIndex());
        relocatable &= returnedVariableRelocatable;
        var result = Expr.binary(op, mapNumericType(opType), first, second);
        assignVariable(insn.getReceiver().getIndex(), result, relocatable);
    }

    private boolean isIntegerType(NumericOperandType operandType) {
        switch (operandType) {
            case INT:
            case LONG:
                return true;
            default:
                return false;
        }
    }

    private OperationType mapNumericType(NumericOperandType operandType) {
        if (operandType == null) {
            return null;
        }
        switch (operandType) {
            case INT:
                return OperationType.INT;
            case LONG:
                return OperationType.LONG;
            case FLOAT:
                return OperationType.FLOAT;
            case DOUBLE:
                return OperationType.DOUBLE;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void exitCurrentDominator(BasicBlock target) {
        var jump = getJumpStatement(target, nextBlock);
        if (jump != null) {
            statements.add(jump);
        }
        currentBlock = null;
    }

    private Statement getJumpStatement(BasicBlock target, BasicBlock nextBlock) {
        if (target == nextBlock) {
            return null;
        }
        var targetStatement = getJumpTarget(target);
        var breakStatement = new BreakStatement();
        breakStatement.setTarget(targetStatement);
        return breakStatement;
    }

    private void addJumpStatement(List<Statement> statements, BasicBlock target, BasicBlock nextBlock) {
        var statement = getJumpStatement(target, nextBlock);
        if (statement != null) {
            statements.add(statement);
        }
    }

    private IdentifiedStatement getJumpTarget(BasicBlock target) {
        var targetStatement = jumpTargets[target.getIndex()];
        int count = identifiedStatementUseCount.get(targetStatement);
        identifiedStatementUseCount.put(targetStatement, count + 1);
        return targetStatement;
    }

    private InstructionVisitor instructionDecompiler = new InstructionVisitor() {
        private List<Expr> arguments = new ArrayList<>();

        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            assignConstant(insn.getReceiver().getIndex(), Expr.constant(insn.getConstant()));
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            assignConstant(insn.getReceiver().getIndex(), Expr.constant(null));
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            assignConstant(insn.getReceiver().getIndex(), Expr.constant(insn.getConstant()));
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            assignConstant(insn.getReceiver().getIndex(), Expr.constant(insn.getConstant()));
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            assignConstant(insn.getReceiver().getIndex(), Expr.constant(insn.getConstant()));
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            assignConstant(insn.getReceiver().getIndex(), Expr.constant(insn.getConstant()));
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            assignConstant(insn.getReceiver().getIndex(), Expr.constant(insn.getConstant()));
        }

        @Override
        public void visit(BinaryInstruction insn) {
            switch (insn.getOperation()) {
                case ADD:
                    binary(BinaryOperation.ADD, insn.getOperandType(), insn, true);
                    break;
                case SUBTRACT:
                    binary(BinaryOperation.SUBTRACT, insn.getOperandType(), insn, true);
                    break;
                case MULTIPLY:
                    binary(BinaryOperation.MULTIPLY, insn.getOperandType(), insn, true);
                    break;
                case DIVIDE:
                    binary(BinaryOperation.DIVIDE, insn.getOperandType(), insn, !isIntegerType(insn.getOperandType()));
                    break;
                case MODULO:
                    binary(BinaryOperation.MODULO, insn.getOperandType(), insn, !isIntegerType(insn.getOperandType()));
                    break;
                case COMPARE:
                    binary(BinaryOperation.COMPARE, insn.getOperandType(), insn, true);
                    break;
                case AND:
                    binary(BinaryOperation.BITWISE_AND, insn.getOperandType(), insn, true);
                    break;
                case OR:
                    binary(BinaryOperation.BITWISE_OR, insn.getOperandType(), insn, true);
                    break;
                case XOR:
                    binary(BinaryOperation.BITWISE_XOR, insn.getOperandType(), insn, true);
                    break;
                case SHIFT_LEFT:
                    binary(BinaryOperation.LEFT_SHIFT, insn.getOperandType(), insn, true);
                    break;
                case SHIFT_RIGHT:
                    binary(BinaryOperation.RIGHT_SHIFT, insn.getOperandType(), insn, true);
                    break;
                case SHIFT_RIGHT_UNSIGNED:
                    binary(BinaryOperation.UNSIGNED_RIGHT_SHIFT, insn.getOperandType(), insn, true);
                    break;
            }
        }

        @Override
        public void visit(NegateInstruction insn) {
            var operand = getVariable(insn.getOperand().getIndex());
            boolean relocatable = returnedVariableRelocatable;
            var result = Expr.unary(UnaryOperation.NEGATE, mapNumericType(insn.getOperandType()), operand);
            assignVariable(insn.getReceiver().getIndex(), result, relocatable);
        }

        private void unary() {

        }

        @Override
        public void visit(AssignInstruction insn) {

        }

        @Override
        public void visit(CastInstruction insn) {

        }

        @Override
        public void visit(CastNumberInstruction insn) {

        }

        @Override
        public void visit(CastIntegerInstruction insn) {

        }

        @Override
        public void visit(BranchingInstruction insn) {
            Expr condition;
            switch (insn.getCondition()) {
                case NULL:
                    condition = cond(BinaryOperation.EQUALS, null, insn.getOperand(), Expr.constant(null));
                    break;
                case NOT_NULL:
                    condition = cond(BinaryOperation.NOT_EQUALS, null, insn.getOperand(), Expr.constant(null));
                    break;
                case EQUAL:
                    condition = cond(BinaryOperation.EQUALS, NumericOperandType.INT, insn.getOperand(),
                            Expr.constant(0));
                    break;
                case NOT_EQUAL:
                    condition = cond(BinaryOperation.NOT_EQUALS, NumericOperandType.INT, insn.getOperand(),
                            Expr.constant(0));
                    break;
                case LESS:
                    condition = cond(BinaryOperation.LESS, NumericOperandType.INT, insn.getOperand(), Expr.constant(0));
                    break;
                case LESS_OR_EQUAL:
                    condition = cond(BinaryOperation.LESS_OR_EQUALS, NumericOperandType.INT, insn.getOperand(),
                            Expr.constant(0));
                    break;
                case GREATER:
                    condition = cond(BinaryOperation.GREATER, NumericOperandType.INT, insn.getOperand(),
                            Expr.constant(0));
                    break;
                case GREATER_OR_EQUAL:
                    condition = cond(BinaryOperation.GREATER_OR_EQUALS, NumericOperandType.INT, insn.getOperand(),
                            Expr.constant(0));
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            branch(condition, insn.getConsequent(), insn.getAlternative());
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            Expr condition;
            switch (insn.getCondition()) {
                case REFERENCE_EQUAL:
                    condition = cond(BinaryOperation.EQUALS, null, insn.getFirstOperand(), insn.getSecondOperand());
                    break;
                case REFERENCE_NOT_EQUAL:
                    condition = cond(BinaryOperation.NOT_EQUALS, null, insn.getFirstOperand(),
                            insn.getSecondOperand());
                    break;
                case EQUAL:
                    condition = cond(BinaryOperation.EQUALS, NumericOperandType.INT, insn.getFirstOperand(),
                            insn.getSecondOperand());
                    break;
                case NOT_EQUAL:
                    condition = cond(BinaryOperation.NOT_EQUALS, NumericOperandType.INT, insn.getFirstOperand(),
                            insn.getSecondOperand());
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            branch(condition, insn.getConsequent(), insn.getAlternative());
        }

        @Override
        public void visit(JumpInstruction insn) {
            int sourceNode = blockExitNode(currentBlock);
            int targetNode = blockEnterNode(insn.getTarget());
            if (dom.immediateDominatorOf(targetNode) == sourceNode) {
                currentBlock = insn.getTarget();
            } else {
                flushStack();
                exitCurrentDominator(insn.getTarget());
            }
        }

        @Override
        public void visit(SwitchInstruction insn) {
            var condition = getVariable(insn.getCondition().getIndex());
            switchBranch(condition, insn);
        }

        @Override
        public void visit(ExitInstruction insn) {
            Expr returnValue;
            if (insn.getValueToReturn() != null) {
                returnValue = getVariable(insn.getValueToReturn().getIndex());
            } else {
                returnValue = null;
            }
            flushStack();
            if (nextBlock != null || returnValue != null) {
                var statement = new ReturnStatement();
                statement.setResult(returnValue);
                statements.add(statement);
            }
            currentBlock = null;
        }

        @Override
        public void visit(RaiseInstruction insn) {

        }

        @Override
        public void visit(ConstructArrayInstruction insn) {

        }

        @Override
        public void visit(ConstructInstruction insn) {

        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {

        }

        @Override
        public void visit(GetFieldInstruction insn) {

        }

        @Override
        public void visit(PutFieldInstruction insn) {

        }

        @Override
        public void visit(ArrayLengthInstruction insn) {

        }

        @Override
        public void visit(CloneArrayInstruction insn) {

        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {

        }

        @Override
        public void visit(GetElementInstruction insn) {

        }

        @Override
        public void visit(PutElementInstruction insn) {

        }

        @Override
        public void visit(InvokeInstruction insn) {
            for (int i = insn.getArguments().size() - 1; i >= 0; --i) {
                arguments.add(getVariable(insn.getArguments().get(i).getIndex()));
            }
            if (insn.getInstance() != null) {
                arguments.add(getVariable(insn.getInstance().getIndex()));
            }
            Collections.reverse(arguments);

            var expr = new InvocationExpr();
            expr.setMethod(insn.getMethod());
            if (insn.getInstance() == null) {
                expr.setType(InvocationType.STATIC);
            } else {
                switch (insn.getType()) {
                    case SPECIAL:
                        expr.setType(InvocationType.SPECIAL);
                        break;
                    case VIRTUAL:
                        expr.setType(InvocationType.DYNAMIC);
                        break;
                }
            }
            expr.getArguments().addAll(arguments);
            arguments.clear();
            if (insn.getReceiver() != null) {
                assignVariable(insn.getReceiver().getIndex(), expr, false);
            } else {
                flushStack();
                AssignmentStatement statement = new AssignmentStatement();
                statement.setRightValue(expr);
                statements.add(statement);
            }
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
        }

        @Override
        public void visit(IsInstanceInstruction insn) {

        }

        @Override
        public void visit(InitClassInstruction insn) {

        }

        @Override
        public void visit(NullCheckInstruction insn) {

        }

        @Override
        public void visit(MonitorEnterInstruction insn) {

        }

        @Override
        public void visit(MonitorExitInstruction insn) {

        }

        @Override
        public void visit(BoundCheckInstruction insn) {

        }
    };

    private static class ExprStackElement {
        int variable;
        Expr value;

        ExprStackElement(int variable, Expr value) {
            this.variable = variable;
            this.value = value;
        }
    }

    private static class TryCatchElement {
        final String exceptionType;
        final BasicBlock handler;
        List<Statement> targetStatements;
        int start;
        final int level;
        final TryCatchElement next;

        TryCatchElement(String exceptionType, BasicBlock handler, List<Statement> targetStatements, int start,
                int level, TryCatchElement next) {
            this.exceptionType = exceptionType;
            this.handler = handler;
            this.targetStatements = targetStatements;
            this.start = start;
            this.level = level;
            this.next = next;
        }

        boolean equalTo(TryCatchBlock block) {
            return Objects.equals(exceptionType, block.getExceptionType()) && handler == block.getHandler();
        }
    }
}
