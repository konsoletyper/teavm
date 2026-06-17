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
package org.teavm.backend.wasm.analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCallReference;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCall;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmThrow;
import org.teavm.backend.wasm.model.instruction.WasmTry;

/**
 * <p>Computes liveness of locals of a Wasm function.</p>
 *
 * <p>Analysis works in two phases. First phase is performed by constructor. Live sets are stored only
 * in joint nodes: at the start and at the end of every instruction list, and right before instructions
 * that have more than one successor (branches, conditionals, instructions that may throw inside
 * {@code try_table}). First, every instruction list is walked backwards once, collecting uses
 * ({@code local.get}) of locals and putting them to joint nodes. Then joint nodes whose live sets grew
 * are processed by worklist: live set of a node is propagated backwards until next joint node,
 * excluding locals that are defined ({@code local.set} or {@code local.tee}) on the way, and the difference
 * is added to the next node, which is put to the worklist if its set grew. Since live sets only grow,
 * worklist items carry no sets: each node is put to the worklist queue at most once until it's processed,
 * and its current live set is used at the moment it's processed.</p>
 *
 * <p>Second phase is performed on demand by {@link #iterate(WasmInstructionList)}, which reconstructs
 * live sets for every instruction of the given list from the live sets of joint nodes.</p>
 *
 * <p>Locals are identified by their index in the function. The function must not be modified
 * after the analysis was created.</p>
 */
public class WasmLivenessAnalysis {
    private static final BitSet EMPTY = new BitSet();

    private WasmFunction function;
    private Map<WasmInstructionList, ListInfo> listInfos = new HashMap<>();
    private Map<WasmInstruction, InstructionNode> instructionNodes = new HashMap<>();

    private ArrayDeque<Node> worklist = new ArrayDeque<>();
    private BitSet walkSet = new BitSet();
    private BitSet diffSet = new BitSet();

    public WasmLivenessAnalysis(WasmFunction function) {
        this.function = function;
        var collector = new Collector();
        collector.listInfo(function.getBody());
        collector.visitMany(function.getBody());
        // Seed inner and later lists first, so that their results are available when outer lists are walked
        var lists = collector.lists;
        for (var i = lists.size() - 1; i >= 0; --i) {
            var list = lists.get(i).list;
            walkSet.clear();
            walkBackwards(list, list.getLast(), true);
            propagate();
        }
        worklist = null;
        walkSet = null;
        diffSet = null;
    }

    public WasmFunction getFunction() {
        return function;
    }

    /**
     * Reconstructs live sets for instructions of the given list and returns cursor that points
     * to the first instruction of the list.
     */
    public WasmLivenessCursor iterate(WasmInstructionList list) {
        var info = listInfos.get(list);
        if (info == null) {
            throw new IllegalArgumentException("Given instruction list does not belong to the function");
        }

        var count = 0;
        for (var insn = list.getFirst(); insn != null; insn = insn.getNext()) {
            ++count;
        }
        var instructions = new WasmInstruction[count];
        var liveSets = new BitSet[count + 1];
        liveSets[count] = info.exit.live;
        var index = count;
        for (var insn = list.getLast(); insn != null; insn = insn.getPrevious()) {
            instructions[--index] = insn;
            liveSets[index] = transfer(insn, liveSets[index + 1]);
        }

        return new WasmLivenessCursor(function, instructions, liveSets);
    }

    /**
     * Computes live-in set of an instruction from its live-out set. Never modifies given set,
     * may return either given set or set stored in a joint node, if live set does not change.
     */
    private BitSet transfer(WasmInstruction insn, BitSet after) {
        var node = instructionNodes.get(insn);
        if (node != null) {
            return node.live;
        }
        if (insn instanceof WasmGetLocal) {
            var index = ((WasmGetLocal) insn).getLocal().getIndex();
            if (after.get(index)) {
                return after;
            }
            var result = (BitSet) after.clone();
            result.set(index);
            return result;
        } else if (insn instanceof WasmSetLocal || insn instanceof WasmTeeLocal) {
            var index = insn instanceof WasmSetLocal
                    ? ((WasmSetLocal) insn).getLocal().getIndex()
                    : ((WasmTeeLocal) insn).getLocal().getIndex();
            if (!after.get(index)) {
                return after;
            }
            var result = (BitSet) after.clone();
            result.clear(index);
            return result;
        } else if (insn instanceof WasmBlock) {
            return listInfos.get(((WasmBlock) insn).getBody()).entry.live;
        } else if (insn instanceof WasmTry) {
            return listInfos.get(((WasmTry) insn).getBody()).entry.live;
        } else if (insn.isTerminating()) {
            return EMPTY;
        } else {
            return after;
        }
    }

    private void propagate() {
        while (!worklist.isEmpty()) {
            var node = worklist.removeFirst();
            node.queued = false;
            node.process();
        }
    }

    private void enqueue(Node node) {
        if (node.queued) {
            return;
        }
        node.queued = true;
        worklist.addLast(node);
    }

    /**
     * Adds given locals to live set of the node, and puts node to the worklist if its live set grew.
     */
    private void propagateTo(Node node, BitSet locals) {
        diffSet.clear();
        diffSet.or(locals);
        diffSet.andNot(node.live);
        if (!diffSet.isEmpty()) {
            node.live.or(diffSet);
            enqueue(node);
        }
    }

    private void walkBackwards(WasmInstructionList list, WasmInstruction insn, BitSet locals) {
        walkSet.clear();
        walkSet.or(locals);
        walkBackwards(list, insn, false);
    }

    /**
     * <p>Walks from the given instruction backwards with locals from {@link #walkSet}, given that they are live
     * right after this instruction, until the next joint node, then propagates locals to this node.</p>
     *
     * <p>In seed mode, walk starts with no live locals at the end of the list, collects uses
     * ({@code local.get}) and passes the entire list. Otherwise, walk stops as soon as all locals are killed.
     * Uses kill locals as well, since propagation from them is initiated by seed walk.</p>
     *
     * @param list list where walk is performed
     * @param insn instruction to start from, {@code null} means that locals are live at the start of the list.
     */
    private void walkBackwards(WasmInstructionList list, WasmInstruction insn, boolean seed) {
        var locals = walkSet;
        for (; insn != null; insn = insn.getPrevious()) {
            if (insn instanceof WasmGetLocal) {
                var index = ((WasmGetLocal) insn).getLocal().getIndex();
                if (seed) {
                    locals.set(index);
                    continue;
                }
                locals.clear(index);
            } else if (insn instanceof WasmSetLocal) {
                locals.clear(((WasmSetLocal) insn).getLocal().getIndex());
            } else if (insn instanceof WasmTeeLocal) {
                locals.clear(((WasmTeeLocal) insn).getLocal().getIndex());
            } else if (insn.isTerminating()) {
                // Can't get here from preceding instruction. When terminating instruction is a branch,
                // propagation through it is initiated by its target.
                if (!seed) {
                    return;
                }
                locals.clear();
                continue;
            } else if (insn instanceof WasmBlock || insn instanceof WasmConditional || insn instanceof WasmTry) {
                propagateTo(exitOf(insn), locals);
                if (!seed) {
                    return;
                }
                locals.clear();
                continue;
            } else if (hasMultipleSuccessors(insn)) {
                var node = instructionNodes.get(insn);
                if (node == null) {
                    continue;
                }
                if (!seed) {
                    propagateTo(node, locals);
                    return;
                }
                // Seed walk continues to propagate these locals, so there's no need to process the node
                locals.andNot(node.live);
                node.live.or(locals);
                continue;
            } else {
                continue;
            }
            if (!seed && locals.isEmpty()) {
                return;
            }
        }
        propagateTo(listInfos.get(list).entry, locals);
    }

    private ExitNode exitOf(WasmInstruction insn) {
        WasmInstructionList body;
        if (insn instanceof WasmBlock) {
            body = ((WasmBlock) insn).getBody();
        } else if (insn instanceof WasmConditional) {
            body = ((WasmConditional) insn).getThenBlock();
        } else {
            body = ((WasmTry) insn).getBody();
        }
        return listInfos.get(body).exit;
    }

    private static boolean hasMultipleSuccessors(WasmInstruction insn) {
        return insn instanceof WasmBranch || insn instanceof WasmNullBranch || insn instanceof WasmCastBranch
                || isThrowing(insn);
    }

    private static boolean isThrowing(WasmInstruction insn) {
        return insn instanceof WasmCall || insn instanceof WasmIndirectCall || insn instanceof WasmCallReference
                || insn instanceof WasmThrow;
    }

    private class Collector extends WasmDefaultInstructionVisitor {
        private ListInfo[] catchTargets = new ListInfo[0];
        final List<ListInfo> lists = new ArrayList<>();

        ListInfo listInfo(WasmInstructionList list) {
            var info = listInfos.get(list);
            if (info == null) {
                info = new ListInfo(list, new ExitNode());
                register(info);
            }
            return info;
        }

        private void register(ListInfo info) {
            listInfos.put(info.list, info);
            info.exit.lists.add(info.list);
            lists.add(info);
        }

        private InstructionNode instructionNode(WasmInstruction insn) {
            return instructionNodes.computeIfAbsent(insn, InstructionNode::new);
        }

        private void addSource(WasmInstruction insn, WasmInstructionList target) {
            var targetInfo = listInfo(target);
            var sources = targetInfo.loop ? targetInfo.entry.sources : targetInfo.exit.sources;
            sources.add(instructionNode(insn));
        }

        @Override
        public void visitDefault(WasmInstruction instruction) {
            super.visitDefault(instruction);
            if (catchTargets.length > 0 && isThrowing(instruction)) {
                for (var target : catchTargets) {
                    addSource(instruction, target.list);
                }
            }
        }

        @Override
        public void visit(WasmBlock instruction) {
            listInfo(instruction.getBody());
            super.visit(instruction);
        }

        @Override
        public void visit(WasmConditional instruction) {
            instructionNode(instruction);
            // Both branches of conditional lead to the same point
            var exit = new ExitNode();
            register(new ListInfo(instruction.getThenBlock(), exit));
            register(new ListInfo(instruction.getElseBlock(), exit));
            super.visit(instruction);
        }

        @Override
        public void visit(WasmTry instruction) {
            listInfo(instruction.getBody());
            var outerCatchTargets = catchTargets;
            var newCatchTargets = new ArrayList<ListInfo>();
            var hasCatchAll = false;
            for (var catchClause : instruction.getCatches()) {
                var target = listInfo(catchClause.getTarget());
                if (!newCatchTargets.contains(target)) {
                    newCatchTargets.add(target);
                }
                if (catchClause.getTag() == null) {
                    hasCatchAll = true;
                }
            }
            if (!hasCatchAll) {
                for (var target : outerCatchTargets) {
                    if (!newCatchTargets.contains(target)) {
                        newCatchTargets.add(target);
                    }
                }
            }
            catchTargets = newCatchTargets.toArray(new ListInfo[0]);
            super.visit(instruction);
            catchTargets = outerCatchTargets;
        }

        @Override
        public void visit(WasmBranch instruction) {
            addSource(instruction, instruction.getTarget());
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            addSource(instruction, instruction.getTarget());
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            addSource(instruction, instruction.getTarget());
        }

        @Override
        public void visit(WasmBreak instruction) {
            addSource(instruction, instruction.getTarget());
        }

        @Override
        public void visit(WasmSwitch instruction) {
            for (var target : instruction.getTargets()) {
                addSource(instruction, target);
            }
            addSource(instruction, instruction.getDefaultTarget());
        }
    }

    private class ListInfo {
        final WasmInstructionList list;
        final boolean loop;
        final EntryNode entry;
        final ExitNode exit;

        ListInfo(WasmInstructionList list, ExitNode exit) {
            this.list = list;
            loop = list.getBreakTarget() instanceof WasmBlock && ((WasmBlock) list.getBreakTarget()).isLoop();
            entry = new EntryNode(list, loop);
            this.exit = exit;
        }
    }

    private abstract static class Node {
        final BitSet live = new BitSet();
        boolean queued;

        abstract void process();
    }

    /**
     * Point at the end of one or two (in case of conditional) lists, i.e. right after the instruction
     * that owns these lists.
     */
    private class ExitNode extends Node {
        final List<WasmInstructionList> lists = new ArrayList<>(1);
        final List<InstructionNode> sources = new ArrayList<>();

        @Override
        void process() {
            for (var source : sources) {
                propagateTo(source, live);
            }
            for (var list : lists) {
                walkBackwards(list, list.getLast(), live);
            }
        }
    }

    /**
     * Point at the start of a list.
     */
    private class EntryNode extends Node {
        final WasmInstructionList list;
        final List<InstructionNode> sources;

        EntryNode(WasmInstructionList list, boolean loop) {
            this.list = list;
            sources = loop ? new ArrayList<>() : List.of();
        }

        @Override
        void process() {
            for (var source : sources) {
                propagateTo(source, live);
            }
            var owner = list.getBreakTarget();
            if (owner instanceof WasmConditional) {
                propagateTo(instructionNodes.get(owner), live);
            } else if (owner != null) {
                walkBackwards(owner.getOwner(), owner.getPrevious(), live);
            }
        }
    }

    /**
     * Point right before an instruction that has multiple successors.
     */
    private class InstructionNode extends Node {
        final WasmInstruction instruction;

        InstructionNode(WasmInstruction instruction) {
            this.instruction = instruction;
        }

        @Override
        void process() {
            walkBackwards(instruction.getOwner(), instruction.getPrevious(), live);
        }
    }
}
