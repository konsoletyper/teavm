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
package org.teavm.ast.optimization;

import java.util.BitSet;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.MethodNode;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.VariableNode;
import org.teavm.common.Graph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.util.AsyncProgramSplitter;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.LivenessAnalyzer;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.UsageExtractor;

public class Optimizer {
    public void optimize(RegularMethodNode method, Program program, boolean friendlyToDebugger) {
        ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getVariables().size());
        stats.analyze(program);
        boolean[] preservedVars = new boolean[stats.writes.length];
        BreakEliminator breakEliminator = new BreakEliminator();
        breakEliminator.eliminate(method.getBody());
        OptimizingVisitor optimizer = new OptimizingVisitor(preservedVars, stats.writes, stats.reads,
                friendlyToDebugger);
        method.getBody().acceptVisitor(optimizer);
        method.setBody(optimizer.resultStmt);
        int paramCount = method.getReference().parameterCount();

        UnusedVariableEliminator unusedEliminator = new UnusedVariableEliminator(paramCount, method.getVariables());
        method.getBody().acceptVisitor(unusedEliminator);
        method.getVariables().clear();
        method.getVariables().addAll(unusedEliminator.getReorderedVariables());

        method.getBody().acceptVisitor(new RedundantLabelEliminator());
        method.getBody().acceptVisitor(new RedundantReturnElimination());

        for (int i = 0; i < method.getVariables().size(); ++i) {
            method.getVariables().get(i).setIndex(i);
        }
    }

    public void optimize(AsyncMethodNode method, AsyncProgramSplitter splitter, boolean friendlyToDebugger) {
        LivenessAnalyzer liveness = new LivenessAnalyzer();
        liveness.analyze(splitter.getOriginalProgram());

        Graph cfg = ProgramUtils.buildControlFlowGraph(splitter.getOriginalProgram());

        for (int i = 0; i < splitter.size(); ++i) {
            boolean[] preservedVars = new boolean[method.getVariables().size()];
            ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getVariables().size());
            stats.analyze(splitter.getProgram(i));

            AsyncMethodPart part = method.getBody().get(i);
            BreakEliminator breakEliminator = new BreakEliminator();
            breakEliminator.eliminate(part.getStatement());
            findEscapingLiveVars(liveness, cfg, splitter, i, preservedVars);
            OptimizingVisitor optimizer = new OptimizingVisitor(preservedVars, stats.writes, stats.reads,
                    friendlyToDebugger);
            part.getStatement().acceptVisitor(optimizer);
            part.setStatement(optimizer.resultStmt);
        }

        int paramCount = method.getReference().parameterCount();
        UnusedVariableEliminator unusedEliminator = new UnusedVariableEliminator(paramCount, method.getVariables());
        for (AsyncMethodPart part : method.getBody()) {
            part.getStatement().acceptVisitor(unusedEliminator);
        }
        method.getVariables().clear();
        method.getVariables().addAll(unusedEliminator.getReorderedVariables());

        RedundantLabelEliminator labelEliminator = new RedundantLabelEliminator();
        for (AsyncMethodPart part : method.getBody()) {
            part.getStatement().acceptVisitor(labelEliminator);
        }

        for (int i = 0; i < method.getVariables().size(); ++i) {
            method.getVariables().get(i).setIndex(i);
        }
    }

    private void preserveDebuggableVars(boolean[] variablesToPreserve, MethodNode methodNode) {
        for (VariableNode varNode : methodNode.getVariables()) {
            if (varNode.getName() != null) {
                variablesToPreserve[varNode.getIndex()] = true;
            }
        }
    }

    private void findEscapingLiveVars(LivenessAnalyzer liveness, Graph cfg, AsyncProgramSplitter splitter,
            int partIndex, boolean[] output) {
        Program originalProgram = splitter.getOriginalProgram();
        Program program = splitter.getProgram(partIndex);
        int[] successors = splitter.getBlockSuccessors(partIndex);
        Instruction[] splitPoints = splitter.getSplitPoints(partIndex);
        int[] originalBlocks = splitter.getOriginalBlocks(partIndex);

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (successors[i] < 0 || originalBlocks[i] < 0) {
                continue;
            }

            // Determine live-out vars
            BitSet liveVars = new BitSet();
            for (int succ : cfg.outgoingEdges(originalBlocks[i])) {
                liveVars.or(liveness.liveIn(succ));
            }

            // Remove from live set all variables that are defined in these blocks
            DefinitionExtractor defExtractor = new DefinitionExtractor();
            UsageExtractor useExtractor = new UsageExtractor();
            BasicBlock block = originalProgram.basicBlockAt(originalBlocks[i]);
            Instruction splitPoint = splitPoints[i].getPrevious();
            for (Instruction insn = block.getLastInstruction(); insn != splitPoint; insn = insn.getPrevious()) {
                insn.acceptVisitor(defExtractor);
                insn.acceptVisitor(useExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    liveVars.clear(var.getIndex());
                }
                for (Variable var : useExtractor.getUsedVariables()) {
                    liveVars.set(var.getIndex());
                }
            }

            // Add live variables to output
            for (int j = liveVars.nextSetBit(0); j >= 0; j = liveVars.nextSetBit(j + 1)) {
                output[j] = true;
            }
        }
    }
}
