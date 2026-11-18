/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReader;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.text.ListingBuilder;

/**
 * <p>Checks that a program in SSA form satisfies IR invariants:</p>
 *
 * <ul>
 *   <li>each variable is defined at most once;</li>
 *   <li>each used variable is defined;</li>
 *   <li>definition of a variable dominates all its usages; for phi incomings, definition must dominate
 *     the end of the incoming's source block;</li>
 *   <li>phi incomings cover exactly predecessors of phi's block, one incoming per normal edge;</li>
 *   <li>no block is both normal and exceptional predecessor of another block;</li>
 *   <li>for each handler phi and each protected block, incomings from this block (in order of the list)
 *     consist of at most one <i>entry value</i>, which is not defined by an instruction of the protected block
 *     and which must go first, followed by values defined by instructions of the protected block,
 *     in order of these instructions.</li>
 * </ul>
 *
 * <p>Unreachable blocks are ignored. Variables with indexes below {@code implicitVariableCount}
 * (i.e. {@code this} and parameters) are considered defined before the entry block.</p>
 *
 * <p>Checks can be enabled in compiler by {@code teavm.checkIr} system property, see {@link #ENABLED}.</p>
 */
public class ProgramChecker {
    public static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("teavm.checkIr", "false"));

    private static final int UNDEFINED = -3;
    private static final int IMPLICIT = -2;
    private static final int BLOCK_START = -1;

    private final Program program;
    private final int implicitVariableCount;
    private final List<ProgramViolation> violations = new ArrayList<>();
    private boolean[] reachable;
    private DominatorTree domTree;
    private int[] definitionBlock;
    // Position of instruction which defines variable, or one of UNDEFINED, IMPLICIT, BLOCK_START
    private int[] definitionPosition;
    private List<Set<BasicBlock>> normalPredecessors = new ArrayList<>();
    private List<Set<BasicBlock>> exceptionalPredecessors = new ArrayList<>();


    private ProgramChecker(Program program, int implicitVariableCount) {
        this.program = program;
        this.implicitVariableCount = Math.min(implicitVariableCount, program.variableCount());
    }

    public static List<ProgramViolation> check(Program program, int implicitVariableCount) {
        var checker = new ProgramChecker(program, implicitVariableCount);
        checker.check();
        return checker.violations;
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }

    /**
     * Checks program and throws {@link ProgramInvariantException} if there are violations.
     *
     * @param description describes method and the point in the pipeline, used in exception message.
     */
    public static void verify(Program program, int implicitVariableCount, String description) {
        if (isBootstrap()) {
            return;
        }
        var violations = check(program, implicitVariableCount);
        if (violations.isEmpty()) {
            return;
        }
        var sb = new StringBuilder();
        sb.append("IR invariants violated ").append(description).append(":\n");
        for (var violation : violations) {
            sb.append("  ").append(violation).append('\n');
        }
        sb.append("Program:\n");
        try {
            sb.append(new ListingBuilder().buildListing(program, "  "));
        } catch (RuntimeException e) {
            sb.append("  <failed to build listing: ").append(e).append(">\n");
        }
        throw new ProgramInvariantException(sb.toString(), violations);
    }

    public static void verify(Program program, MethodReader method, String stage) {
        var methodName = method.getReference() != null
                ? method.getReference().toString()
                : method.getDescriptor().toString();
        verify(program, method.parameterCount() + 1, "in " + methodName + " " + stage);
    }

    private void check() {
        if (program.basicBlockCount() == 0) {
            return;
        }
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        findReachable(cfg);
        domTree = GraphUtils.buildDominatorTree(cfg);
        collectDefinitions();
        collectPredecessors();
        checkInstructionUsages();
        checkPhis();
    }

    private void findReachable(Graph cfg) {
        reachable = new boolean[cfg.size()];
        var stack = new int[cfg.size()];
        int top = 0;
        stack[top++] = 0;
        reachable[0] = true;
        while (top > 0) {
            int node = stack[--top];
            for (int successor : cfg.outgoingEdges(node)) {
                if (!reachable[successor]) {
                    reachable[successor] = true;
                    stack[top++] = successor;
                }
            }
        }
    }

    private void collectDefinitions() {
        definitionBlock = new int[program.variableCount()];
        definitionPosition = new int[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            definitionBlock[i] = -1;
            definitionPosition[i] = i < implicitVariableCount ? IMPLICIT : UNDEFINED;
        }

        var definitionExtractor = new DefinitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (!reachable[i]) {
                continue;
            }
            var block = program.basicBlockAt(i);
            if (block.getExceptionVariable() != null) {
                define(block.getExceptionVariable(), i, BLOCK_START);
            }
            for (var phi : block.getPhis()) {
                define(phi.getReceiver(), i, BLOCK_START);
            }
            int position = 0;
            for (var insn : block) {
                insn.acceptVisitor(definitionExtractor);
                for (var variable : definitionExtractor.getDefinedVariables()) {
                    define(variable, i, position);
                }
                ++position;
            }
        }
    }

    private void define(Variable variable, int block, int position) {
        int index = variable.getIndex();
        if (definitionPosition[index] != UNDEFINED) {
            report(ProgramViolation.Kind.MULTIPLE_DEFINITION, block, "variable " + name(variable)
                    + " defined at " + describeDefinition(index) + " is defined again at "
                    + describePosition(block, position));
            return;
        }
        definitionBlock[index] = block;
        definitionPosition[index] = position;
    }

    private void collectPredecessors() {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            normalPredecessors.add(new LinkedHashSet<>());
            exceptionalPredecessors.add(new LinkedHashSet<>());
        }
        var transitionExtractor = new TransitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (!reachable[i]) {
                continue;
            }
            var block = program.basicBlockAt(i);
            var last = block.getLastInstruction();
            if (last != null) {
                last.acceptVisitor(transitionExtractor);
                if (transitionExtractor.getTargets() != null) {
                    for (var target : transitionExtractor.getTargets()) {
                        normalPredecessors.get(target.getIndex()).add(block);
                    }
                }
            }
            for (var tryCatch : block.getTryCatchBlocks()) {
                exceptionalPredecessors.get(tryCatch.getHandler().getIndex()).add(block);
            }
        }

        // The compiler loosely relies on this in some places (for example, PhiUpdater does not
        // rename incomings on normal edge to exception handler, so they get lost), mostly to avoid
        // dealing with ambiguity of such incomings. Can be relaxed when all such places are fixed.
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            for (var predecessor : normalPredecessors.get(i)) {
                if (exceptionalPredecessors.get(i).contains(predecessor)) {
                    report(ProgramViolation.Kind.NORMAL_AND_EXCEPTIONAL_EDGE, predecessor.getIndex(),
                            "$" + predecessor.getIndex() + " both jumps to $" + i + " and is protected by it");
                }
            }
        }
    }

    private void checkInstructionUsages() {
        var usageExtractor = new UsageExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (!reachable[i]) {
                continue;
            }
            var block = program.basicBlockAt(i);
            int position = 0;
            for (Instruction insn : block) {
                insn.acceptVisitor(usageExtractor);
                for (var variable : usageExtractor.getUsedVariables()) {
                    checkUsage(variable, i, position);
                }
                ++position;
            }
        }
    }

    private void checkUsage(Variable variable, int block, int position) {
        int index = variable.getIndex();
        int definitionPosition = this.definitionPosition[index];
        String where = "used at " + describePosition(block, position);
        if (definitionPosition == UNDEFINED) {
            report(ProgramViolation.Kind.UNDEFINED_VARIABLE, block, "variable " + name(variable)
                    + " " + where + " is not defined");
            return;
        }
        if (definitionPosition == IMPLICIT) {
            return;
        }
        int definitionBlock = this.definitionBlock[index];
        boolean ok = definitionBlock == block
                ? definitionPosition < position
                : domTree.dominates(definitionBlock, block);
        if (!ok) {
            report(ProgramViolation.Kind.NOT_DOMINATED, block, "variable " + name(variable) + " " + where
                    + " is not dominated by its definition at " + describeDefinition(index));
        }
    }

    private void checkPhis() {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (!reachable[i]) {
                continue;
            }
            var block = program.basicBlockAt(i);
            if (i == 0 && !block.getPhis().isEmpty()) {
                report(ProgramViolation.Kind.PHI_IN_ENTRY_BLOCK, i, "entry block contains phis");
            }
            for (var phi : block.getPhis()) {
                checkPhi(block, phi);
            }
        }
    }

    private void checkPhi(BasicBlock block, Phi phi) {
        var normal = normalPredecessors.get(block.getIndex());
        var exceptional = exceptionalPredecessors.get(block.getIndex());
        String phiName = "phi " + name(phi.getReceiver()) + " in $" + block.getIndex();

        Map<BasicBlock, List<Incoming>> incomingsBySource = new LinkedHashMap<>();
        for (var incoming : phi.getIncomings()) {
            var source = incoming.getSource();
            if (!reachable[source.getIndex()]) {
                continue;
            }
            incomingsBySource.computeIfAbsent(source, k -> new ArrayList<>()).add(incoming);
        }

        for (var predecessor : normal) {
            if (!incomingsBySource.containsKey(predecessor)) {
                report(ProgramViolation.Kind.MISSING_INCOMING, block.getIndex(), phiName
                        + " has no incoming from predecessor $" + predecessor.getIndex());
            }
        }
        for (var predecessor : exceptional) {
            if (!incomingsBySource.containsKey(predecessor) && !normal.contains(predecessor)) {
                report(ProgramViolation.Kind.MISSING_INCOMING, block.getIndex(), phiName
                        + " has no incoming from protected block $" + predecessor.getIndex());
            }
        }

        for (var entry : incomingsBySource.entrySet()) {
            var source = entry.getKey();
            var incomings = entry.getValue();
            if (normal.contains(source)) {
                if (exceptional.contains(source)) {
                    // Already reported as NORMAL_AND_EXCEPTIONAL_EDGE, incomings are ambiguous here
                    continue;
                }
                if (incomings.size() > 1) {
                    report(ProgramViolation.Kind.MULTIPLE_INCOMINGS, block.getIndex(), phiName
                            + " has " + incomings.size() + " incomings from $" + source.getIndex());
                }
                for (var incoming : incomings) {
                    checkIncomingDominance(phiName, block, incoming);
                }
            } else if (exceptional.contains(source)) {
                checkExceptionIncomings(phiName, block, source, incomings);
            } else {
                report(ProgramViolation.Kind.INCOMING_FROM_NON_PREDECESSOR, block.getIndex(), phiName
                        + " has incoming from $" + source.getIndex() + " which is not a predecessor");
            }
        }
    }

    private void checkIncomingDominance(String phiName, BasicBlock block, Incoming incoming) {
        var value = incoming.getValue();
        int index = value.getIndex();
        int sourceIndex = incoming.getSource().getIndex();
        int definitionPosition = this.definitionPosition[index];
        if (definitionPosition == UNDEFINED) {
            report(ProgramViolation.Kind.UNDEFINED_VARIABLE, block.getIndex(), phiName + " has incoming "
                    + name(value) + " from $" + sourceIndex + " which is not defined");
            return;
        }
        if (definitionPosition == IMPLICIT) {
            return;
        }
        if (!domTree.dominates(definitionBlock[index], sourceIndex)) {
            report(ProgramViolation.Kind.NOT_DOMINATED, block.getIndex(), phiName + " has incoming "
                    + name(value) + " from $" + sourceIndex + " which is not dominated by its definition at "
                    + describeDefinition(index));
        }
    }

    private void checkExceptionIncomings(String phiName, BasicBlock block, BasicBlock source,
            List<Incoming> incomings) {
        int sourceIndex = source.getIndex();
        int lastPosition = BLOCK_START;
        Incoming lastIncoming = null;
        boolean first = true;
        // TODO: duplicates (like `phi @a from $1, @a from $1`) are not reported for now, since they are
        //  benign and produced by some optimizations (NullnessInformation.dispose, GVN copy propagation).
        //  Ideally, each incoming must represent distinct virtual edge, so duplicates should be prohibited.
        Set<Variable> seen = new LinkedHashSet<>();
        for (var incoming : incomings) {
            var value = incoming.getValue();
            if (!seen.add(value)) {
                continue;
            }
            int index = value.getIndex();
            int definitionPosition = this.definitionPosition[index];
            if (definitionPosition == UNDEFINED) {
                report(ProgramViolation.Kind.UNDEFINED_VARIABLE, block.getIndex(), phiName + " has incoming "
                        + name(value) + " from $" + sourceIndex + " which is not defined");
            } else if (definitionPosition >= 0 && definitionBlock[index] == sourceIndex) {
                if (definitionPosition <= lastPosition) {
                    report(ProgramViolation.Kind.HANDLER_PHI_ORDER, block.getIndex(), phiName + " has incoming "
                            + name(value) + " from $" + sourceIndex + " defined at "
                            + describeDefinition(index) + ", which goes after incoming "
                            + name(lastIncoming.getValue()) + " defined at "
                            + describeDefinition(lastIncoming.getValue().getIndex()));
                }
                lastPosition = definitionPosition;
                lastIncoming = incoming;
            } else {
                if (!first) {
                    report(ProgramViolation.Kind.HANDLER_PHI_ENTRY_VALUE, block.getIndex(), phiName
                            + " has incoming " + name(value) + " from $" + sourceIndex
                            + " which is not defined by an instruction of $" + sourceIndex
                            + ", but is not the first incoming from this block");
                }
                if (definitionPosition != IMPLICIT && definitionBlock[index] != sourceIndex
                        && !domTree.dominates(definitionBlock[index], sourceIndex)) {
                    report(ProgramViolation.Kind.NOT_DOMINATED, block.getIndex(), phiName + " has incoming "
                            + name(value) + " from $" + sourceIndex
                            + " which is not dominated by its definition at " + describeDefinition(index));
                }
            }
            first = false;
        }
    }

    private void report(ProgramViolation.Kind kind, int block, String message) {
        violations.add(new ProgramViolation(kind, block, message));
    }

    private String describeDefinition(int variable) {
        int position = definitionPosition[variable];
        switch (position) {
            case IMPLICIT:
                return "method entry";
            case UNDEFINED:
                return "<nowhere>";
            default:
                return describePosition(definitionBlock[variable], position);
        }
    }

    private static String describePosition(int block, int position) {
        return position == BLOCK_START
                ? "start of $" + block
                : "instruction #" + position + " of $" + block;
    }

    private static String name(Variable variable) {
        return "@" + (variable.getLabel() != null ? variable.getLabel() : String.valueOf(variable.getIndex()));
    }
}
