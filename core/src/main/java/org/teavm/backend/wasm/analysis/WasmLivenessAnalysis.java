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
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;

/**
 * <p>Computes liveness of locals of a Wasm function.</p>
 *
 * <p>Analysis works in two phases. First phase is performed by constructor. Live sets are stored only
 * in joint nodes: at the start and at the end of every instruction list, and right before instructions
 * that have more than one successor (branches, conditionals) or that terminate a path inside
 * {@code try_table}.</p>
 *
 * <p>First phase relies on structured control flow of Wasm. The function body is walked backwards once,
 * descending into nested lists, so that code that follows an instruction is always walked before
 * the instruction. Since every branch except branch to a loop targets the end of an enclosing block,
 * live set of the target is already known when the branch is reached. The only exception are branches to loops,
 * since live set at the start of the loop body is only known after the body is walked. Therefore, after
 * the body of a loop is walked, its live set at entry is propagated to branches to the loop. This propagation
 * is performed by worklist, which only carries the locals that were added to the live sets of the nodes
 * (deltas): locals are propagated backwards from a node until next joint node, excluding locals that are
 * defined ({@code local.set} or {@code local.tee}) or used ({@code local.get}, since they are already live
 * before the use) on the way. Such propagation never leaves the loop body, since all propagated locals are
 * already live at the start of the loop.</p>
 *
 * <p>Second phase is performed on demand by {@link #iterate(WasmInstructionList)}, which reconstructs
 * live sets for every instruction of the given list from the live sets of joint nodes.</p>
 *
 * <p>Exception handling mirrors IR-level liveness, where every instruction of a protected block is considered
 * as possibly throwing. Locals that are live at entry of exception handlers of a {@code try_table}
 * (handler locals) are considered live in the entire body, including its entry, and their definitions inside
 * the body don't kill them. Instead of edges from every throwing instruction to handlers, handler locals are
 * added at every point where a path leaves the body: at the end of the body and before every terminating
 * instruction inside the body. Since definitions don't kill handler locals, this is enough to make them live
 * everywhere in the body. When live set at entry of a handler grows during propagation, new locals are
 * propagated from these points.</p>
 *
 * <p>Locals are identified by their index in the function. The function must not be modified
 * after the analysis was created.</p>
 */
public class WasmLivenessAnalysis {
    private WasmFunction function;
    private Map<WasmInstructionList, ListInfo> listInfos = new HashMap<>();

    private ArrayDeque<Node> worklist = new ArrayDeque<>();
    private BitSet liveSet = new BitSet();
    private BitSet walkSet = new BitSet();
    private BitSet diffSet = new BitSet();
    private BitSet deltaSet = new BitSet();
    private int[] buffer = new int[16];

    public WasmLivenessAnalysis(WasmFunction function) {
        this.function = function;
        new Collector().visitMany(function.getBody());
        walkList(listInfos.get(function.getBody()));
        worklist = null;
        liveSet = null;
        walkSet = null;
        diffSet = null;
        deltaSet = null;
    }

    public WasmFunction getFunction() {
        return function;
    }

    /**
     * Returns locals that are live at the start of the given list.
     */
    public List<WasmLocal> getLiveIn(WasmInstructionList list) {
        var info = listInfos.get(list);
        if (info == null) {
            throw new IllegalArgumentException("Given instruction list does not belong to the function");
        }
        var live = info.entry.live;
        var locals = function.getLocalVariables();
        var result = new ArrayList<WasmLocal>(live.cardinality());
        for (var i = live.nextSetBit(0); i >= 0; i = live.nextSetBit(i + 1)) {
            result.add(locals.get(i));
        }
        return result;
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
        var liveSet = (BitSet) info.exit.live.clone();
        if (info.isTryBody()) {
            info.scope.addHandlerLocalsTo(liveSet);
        }
        var genSets = new int[count + 1][];
        var killSets = new int[count + 1][];
        var index = count;
        for (var insn = list.getLast(); insn != null; insn = insn.getPrevious()) {
            transfer(info, insn, genSets, killSets, --index, liveSet);
        }

        return new WasmLivenessCursor(function, genSets, killSets, liveSet, list.getFirst());
    }

    private void transfer(ListInfo listInfo, WasmInstruction insn, int[][] genSets, int[][] killSets,
            int index, BitSet liveSet) {
        if (insn instanceof WasmGetLocal getLocal) {
            var localIndex = getLocal.getLocal().getIndex();
            if (!liveSet.get(localIndex)) {
                killSets[index] = new int[] { localIndex };
                liveSet.set(localIndex);
            }
        } else if (insn instanceof WasmSetLocal setLocal) {
            transferDefinition(listInfo, setLocal.getLocal().getIndex(), genSets, index, liveSet);
        } else if (insn instanceof WasmTeeLocal teeLocal) {
            transferDefinition(listInfo, teeLocal.getLocal().getIndex(), genSets, index, liveSet);
        } else if (insn instanceof WasmBlock block) {
            transferLivenessInfo(genSets, killSets, index, liveSet, listInfos.get(block.getBody()).entry.live);
        } else if (insn instanceof WasmTry tryInsn) {
            transferLivenessInfo(genSets, killSets, index, liveSet, listInfos.get(tryInsn.getBody()).entry.live);
        } else if (mayHaveNode(insn)) {
            var node = listInfo.insnNodes[index];
            if (node != null) {
                transferLivenessInfo(genSets, killSets, index, liveSet, node.live);
            } else if (insn.isTerminating()) {
                liveSet.clear();
            }
        }
    }

    private void transferDefinition(ListInfo listInfo, int localIndex, int[][] genSets, int index,
            BitSet liveSet) {
        if (liveSet.get(localIndex) && isKilling(listInfo, localIndex)) {
            genSets[index] = new int[] { localIndex };
            liveSet.clear(localIndex);
        }
    }

    /**
     * Replaces live set after instruction ({@code liveSet}) with live set before instruction ({@code live}),
     * recording difference between them.
     */
    private void transferLivenessInfo(int[][] genSets, int[][] killSets, int index, BitSet liveSet, BitSet live) {
        killSets[index] = difference(live, liveSet);
        genSets[index] = difference(liveSet, live);
        liveSet.clear();
        liveSet.or(live);
    }

    private int[] difference(BitSet a, BitSet b) {
        var count = 0;
        for (var bit = a.nextSetBit(0); bit >= 0; bit = a.nextSetBit(bit + 1)) {
            if (!b.get(bit)) {
                if (count == buffer.length) {
                    buffer = Arrays.copyOf(buffer, count * 2);
                }
                buffer[count++] = bit;
            }
        }
        return count > 0 ? Arrays.copyOf(buffer, count) : null;
    }

    /**
     * Checks whether definition of the given local in the given list kills it, i.e. whether the local
     * is not a handler local of any enclosing {@code try_table}.
     */
    private static boolean isKilling(ListInfo listInfo, int localIndex) {
        for (var scope = listInfo.scope; scope != null; scope = scope.parent) {
            if (scope.handlerLocals.get(localIndex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Walks the given list backwards, given that {@link #liveSet} contains locals that are live at the end
     * of the list. When method returns, {@link #liveSet} contains locals that are live at the start
     * of the list.</p>
     *
     * <p>Live sets of all joint nodes inside the list are computed, except for contributions of branches
     * to enclosing loops. For loops, these contributions are added by propagation from loop entry.</p>
     */
    private void walkList(ListInfo info) {
        var live = liveSet;
        var index = info.size - 1;
        for (var insn = info.list.getLast(); insn != null; insn = insn.getPrevious(), --index) {
            if (insn instanceof WasmGetLocal getLocal) {
                live.set(getLocal.getLocal().getIndex());
            } else if (insn instanceof WasmSetLocal setLocal) {
                var localIndex = setLocal.getLocal().getIndex();
                if (isKilling(info, localIndex)) {
                    live.clear(localIndex);
                }
            } else if (insn instanceof WasmTeeLocal teeLocal) {
                var localIndex = teeLocal.getLocal().getIndex();
                if (isKilling(info, localIndex)) {
                    live.clear(localIndex);
                }
            } else if (insn instanceof WasmBlock block) {
                var blockInfo = listInfos.get(block.getBody());
                blockInfo.exit.live.or(live);
                walkList(blockInfo);
            } else if (insn instanceof WasmTry tryInsn) {
                var bodyInfo = listInfos.get(tryInsn.getBody());
                bodyInfo.exit.live.or(live);
                var scope = bodyInfo.scope;
                for (var target : scope.targets) {
                    scope.handlerLocals.or(target.loop ? target.entry.live : target.exit.live);
                }
                scope.addHandlerLocalsTo(live);
                walkList(bodyInfo);
            } else if (insn instanceof WasmConditional conditional) {
                var thenInfo = listInfos.get(conditional.getThenBlock());
                var elseInfo = listInfos.get(conditional.getElseBlock());
                var exit = thenInfo.exit;
                exit.live.or(live);
                walkList(thenInfo);
                live.clear();
                live.or(exit.live);
                walkList(elseInfo);
                live.or(thenInfo.entry.live);
                info.insnNodes[index].live.or(live);
            } else if (mayHaveNode(insn)) {
                var node = info.insnNodes[index];
                if (insn.isTerminating()) {
                    live.clear();
                }
                if (node != null) {
                    for (var target : node.targets) {
                        live.or(target.loop ? target.entry.live : target.exit.live);
                    }
                    if (insn.isTerminating() && info.scope != null) {
                        info.scope.addHandlerLocalsTo(live);
                    }
                    node.live.or(live);
                }
            }
        }

        var entry = info.entry;
        entry.live.or(live);
        if (info.loop && !entry.live.isEmpty()) {
            for (var source : entry.sources) {
                propagateTo(source, entry.live);
            }
            entry.notifyScopes(entry.live);
            propagate();
        }
    }

    private void propagate() {
        while (!worklist.isEmpty()) {
            var node = worklist.removeFirst();
            node.queued = false;
            deltaSet.clear();
            deltaSet.or(node.delta);
            node.delta.clear();
            node.process(deltaSet);
        }
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
            if (node.delta == null) {
                node.delta = new BitSet();
            }
            node.delta.or(diffSet);
            if (!node.queued) {
                node.queued = true;
                worklist.addLast(node);
            }
        }
    }

    private void walkBackwards(ListInfo listInfo, int index, WasmInstruction insn, BitSet locals) {
        walkSet.clear();
        walkSet.or(locals);
        walkBackwards(listInfo, insn.getPrevious(), index - 1);
    }

    /**
     * Walks from the given instruction backwards with locals from {@link #walkSet}, given that they were
     * just added to live set right after this instruction, until the next joint node, then propagates locals
     * to this node. Walk stops as soon as all locals are killed. Uses kill locals as well, since they
     * are already live before the use.
     */
    private void walkBackwards(ListInfo listInfo, WasmInstruction insn, int index) {
        var list = listInfo.list;
        var locals = walkSet;
        for (; insn != null; insn = insn.getPrevious(), --index) {
            if (insn instanceof WasmGetLocal getLocal) {
                locals.clear(getLocal.getLocal().getIndex());
            } else if (insn instanceof WasmSetLocal setLocal) {
                var localIndex = setLocal.getLocal().getIndex();
                if (isKilling(listInfo, localIndex)) {
                    locals.clear(localIndex);
                }
            } else if (insn instanceof WasmTeeLocal teeLocal) {
                var localIndex = teeLocal.getLocal().getIndex();
                if (isKilling(listInfo, localIndex)) {
                    locals.clear(localIndex);
                }
            } else if (insn.isTerminating()) {
                // Can't get here from preceding instruction
                return;
            } else if (insn instanceof WasmBlock || insn instanceof WasmConditional || insn instanceof WasmTry) {
                propagateTo(exitOf(insn), locals);
                return;
            } else if (mayHaveNode(insn)) {
                var node = listInfo.insnNodes[index];
                if (node != null) {
                    propagateTo(node, locals);
                    return;
                }
                continue;
            } else {
                continue;
            }
            if (locals.isEmpty()) {
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

    /**
     * Checks whether instruction may have {@link InstructionNode}, i.e. whether it's worth to look it up.
     */
    private static boolean mayHaveNode(WasmInstruction insn) {
        return insn instanceof WasmBranch || insn instanceof WasmNullBranch || insn instanceof WasmCastBranch
                || insn instanceof WasmConditional || insn.isTerminating();
    }

    private class Collector extends WasmDefaultInstructionVisitor {
        private ListInfo listInfo;
        private int index;
        private WasmInstruction currentInsn;
        private TryScope scope;

        ListInfo listInfo(WasmInstructionList list) {
            var info = listInfos.get(list);
            if (info == null) {
                info = new ListInfo(listInfo, index, list, new ExitNode(), scope);
                register(info);
            }
            return info;
        }

        private void register(ListInfo info) {
            listInfos.put(info.list, info);
            info.exit.lists.add(info);
        }

        private InstructionNode insnNode() {
            var result = listInfo.insnNodes[index];
            if (result == null) {
                result = new InstructionNode(listInfo, index, currentInsn);
                listInfo.insnNodes[index] = result;
            }
            return result;
        }

        private void addSource(WasmInstructionList target) {
            var targetInfo = listInfo(target);
            var sources = targetInfo.loop ? targetInfo.entry.sources : targetInfo.exit.sources;
            var node = insnNode();
            sources.add(node);
            node.targets.add(targetInfo);
        }

        @Override
        public void visitMany(WasmInstructionList instructions) {
            var savedIndex = index;
            var savedListInfo = listInfo;
            listInfo = listInfo(instructions);
            index = 0;
            for (var instruction : instructions) {
                currentInsn = instruction;
                visitDefault(instruction);
                ++index;
            }
            index = savedIndex;
            listInfo = savedListInfo;
        }

        @Override
        public void visitDefault(WasmInstruction instruction) {
            super.visitDefault(instruction);
            if (scope != null && instruction.isTerminating()) {
                var node = insnNode();
                for (var s = scope; s != null; s = s.parent) {
                    s.exits.add(node);
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
            insnNode();
            // Both branches of conditional lead to the same point
            var exit = new ExitNode();
            register(new ListInfo(listInfo, index, instruction.getThenBlock(), exit, scope));
            register(new ListInfo(listInfo, index, instruction.getElseBlock(), exit, scope));
            super.visit(instruction);
        }

        @Override
        public void visit(WasmTry instruction) {
            var outerScope = scope;
            var newScope = new TryScope(outerScope);
            for (var catchClause : instruction.getCatches()) {
                var target = listInfo(catchClause.getTarget());
                if (!newScope.targets.contains(target)) {
                    newScope.targets.add(target);
                    (target.loop ? target.entry : target.exit).addScope(newScope);
                }
            }
            scope = newScope;
            newScope.body = listInfo(instruction.getBody());
            super.visit(instruction);
            scope = outerScope;
        }

        @Override
        public void visit(WasmBranch instruction) {
            addSource(instruction.getTarget());
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            addSource(instruction.getTarget());
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            addSource(instruction.getTarget());
        }

        @Override
        public void visit(WasmBreak instruction) {
            addSource(instruction.getTarget());
        }

        @Override
        public void visit(WasmSwitch instruction) {
            for (var target : instruction.getTargets()) {
                addSource(target);
            }
            addSource(instruction.getDefaultTarget());
        }
    }

    /**
     * Body of {@code try_table} with its handler locals.
     */
    private class TryScope {
        final TryScope parent;
        final List<ListInfo> targets = new ArrayList<>(1);
        /**
         * Terminating instructions inside the body, including bodies of nested {@code try_table}.
         */
        final List<InstructionNode> exits = new ArrayList<>();
        final BitSet handlerLocals = new BitSet();
        ListInfo body;

        TryScope(TryScope parent) {
            this.parent = parent;
        }

        void addHandlerLocalsTo(BitSet set) {
            for (var scope = this; scope != null; scope = scope.parent) {
                set.or(scope.handlerLocals);
            }
        }

        /**
         * Adds locals to handler locals and propagates new ones from every point where a path leaves the body.
         */
        void addHandlerLocals(BitSet locals) {
            var added = (BitSet) locals.clone();
            added.andNot(handlerLocals);
            if (added.isEmpty()) {
                return;
            }
            handlerLocals.or(added);
            for (var exit : exits) {
                propagateTo(exit, added);
            }
            walkSet.clear();
            walkSet.or(added);
            walkBackwards(body, body.list.getLast(), body.size - 1);
        }
    }

    private class ListInfo {
        final WasmInstructionList list;
        final boolean loop;
        final EntryNode entry;
        final ExitNode exit;
        final int size;
        /**
         * Innermost {@code try_table} body that contains the list, or that is the list itself.
         */
        final TryScope scope;
        InstructionNode[] insnNodes;

        ListInfo(ListInfo parent, int indexInParent, WasmInstructionList list, ExitNode exit, TryScope scope) {
            this.list = list;
            loop = list.getBreakTarget() instanceof WasmBlock && ((WasmBlock) list.getBreakTarget()).isLoop();
            entry = new EntryNode(parent, indexInParent, list, loop);
            this.exit = exit;
            this.scope = scope;
            size = list.calculateSize();
            insnNodes = new InstructionNode[size];
        }

        boolean isTryBody() {
            return scope != null && scope.body == this;
        }
    }

    private abstract static class Node {
        final BitSet live = new BitSet();
        /**
         * Locals that were added to {@link #live} since the node was processed last time.
         */
        BitSet delta;
        boolean queued;
        /**
         * Bodies of {@code try_table} that have handler at this node.
         */
        List<TryScope> scopes;

        abstract void process(BitSet delta);

        void addScope(TryScope scope) {
            if (scopes == null) {
                scopes = new ArrayList<>(1);
            }
            scopes.add(scope);
        }

        void notifyScopes(BitSet delta) {
            if (scopes != null) {
                for (var scope : scopes) {
                    scope.addHandlerLocals(delta);
                }
            }
        }
    }

    /**
     * Point at the end of one or two (in case of conditional) lists, i.e. right after the instruction
     * that owns these lists.
     */
    private class ExitNode extends Node {
        final List<ListInfo> lists = new ArrayList<>(1);
        final List<InstructionNode> sources = new ArrayList<>();

        @Override
        void process(BitSet delta) {
            for (var source : sources) {
                propagateTo(source, delta);
            }
            for (var list : lists) {
                walkSet.clear();
                walkSet.or(delta);
                walkBackwards(list, list.list.getLast(), list.size - 1);
            }
            notifyScopes(delta);
        }
    }

    /**
     * Point at the start of a list.
     */
    private class EntryNode extends Node {
        final ListInfo containingListInfo;
        final int indexInContainingList;
        final WasmInstructionList list;
        final List<InstructionNode> sources;

        EntryNode(ListInfo containingListInfo, int indexInContainingList, WasmInstructionList list, boolean loop) {
            this.containingListInfo = containingListInfo;
            this.indexInContainingList = indexInContainingList;
            this.list = list;
            sources = loop ? new ArrayList<>() : List.of();
        }

        @Override
        void process(BitSet delta) {
            for (var source : sources) {
                propagateTo(source, delta);
            }
            notifyScopes(delta);
            var owner = list.getBreakTarget();
            if (owner instanceof WasmConditional) {
                propagateTo(containingListInfo.insnNodes[indexInContainingList], delta);
            } else if (owner != null) {
                walkSet.clear();
                walkSet.or(delta);
                walkBackwards(containingListInfo, owner.getPrevious(), indexInContainingList - 1);
            }
        }
    }

    /**
     * Point right before an instruction that has multiple successors or that terminates a path
     * inside {@code try_table}.
     */
    private class InstructionNode extends Node {
        final ListInfo listInfo;
        final int index;
        final WasmInstruction instruction;
        final List<ListInfo> targets = new ArrayList<>(1);

        InstructionNode(ListInfo listInfo, int index, WasmInstruction instruction) {
            this.listInfo = listInfo;
            this.index = index;
            this.instruction = instruction;
        }

        @Override
        void process(BitSet delta) {
            walkBackwards(listInfo, index, instruction, delta);
        }
    }
}
