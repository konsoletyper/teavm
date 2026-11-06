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
package org.teavm.backend.wasm.optimization;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.backend.wasm.analysis.WasmDominanceOrderWalker;
import org.teavm.backend.wasm.analysis.WasmInstructionReachabilityAnalysis;
import org.teavm.backend.wasm.analysis.WasmLivenessAnalysis;
import org.teavm.backend.wasm.analysis.WasmLivenessCursor;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;

public class WasmLocalsOptimizer {
    private WasmFunction function;
    private List<WasmLocal> locals;
    private WasmLivenessAnalysis liveness;
    private WasmInstructionReachabilityAnalysis reachability;

    private int[] colors;
    private WasmLivenessCursor cursor;
    private IntSet[] skippedColors;
    private boolean needsCoalescing;
    private List<ColorInfo> colorInfos = new ArrayList<>();
    private List<WasmLocal> colorLocals = new ArrayList<>();
    private Map<WasmType, IntArrayList> availableColorsByType = new HashMap<>();
    private IntArrayList owners = new IntArrayList();
    private List<WasmLocal> genBatch = new ArrayList<>();
    private Graph assignmentGraph;

    private boolean friendlyToDebugger;

    public void optimize(WasmFunction function) {
        if (function.getLocalVariables().isEmpty()) {
            return;
        }
        this.function = function;
        locals = function.getLocalVariables();
        liveness = new WasmLivenessAnalysis(function);
        skippedColors = new IntSet[locals.size()];
        prepareAssignmentGraph();
        reachability = new WasmInstructionReachabilityAnalysis();
        reachability.visitMany(function.getBody());
        colors = new int[locals.size()];
        Arrays.fill(colors, -1);
        allocate();
        if (needsCoalescing) {
            Arrays.fill(colors, -1);
            colorInfos.clear();
            owners.clear();
            availableColorsByType.clear();
            allocate();
        }
        rewrite();
        reachability = null;
        skippedColors = null;
        liveness = null;
        colors = null;
        availableColorsByType.clear();
        this.function = null;
        locals = null;
    }

    private void prepareAssignmentGraph() {
        var builder = new GraphBuilder(locals.size());
        var assignmentFinder = new WasmLocalAssignmentFinder(builder);
        assignmentFinder.visitMany(function.getBody());
        assignmentGraph = builder.build();
    }

    public void setFriendlyToDebugger(boolean debuggerFriendly) {
        this.friendlyToDebugger = debuggerFriendly;
    }

    private void allocate() {
        var parameterCount = Math.min(function.getType().getParameterTypes().size(), locals.size());
        for (var i = 0; i < parameterCount; ++i) {
            colors[i] = newColor(locals.get(i));
        }
        var entryLive = liveness.getLiveIn(function.getBody());
        for (var local : entryLive) {
            var color = colors[local.getIndex()];
            if (color < 0) {
                color = newColor(local);
                colors[local.getIndex()] = color;
            }
            owners.set(color, local.getIndex());
        }

        new Walker().walk(function.getBody());
    }

    private int newColor(WasmLocal local) {
        var color = colorInfos.size();
        var colorInfo = new ColorInfo(local.getType(), local.getName());
        colorInfos.add(colorInfo);
        owners.add(-1);
        return color;
    }

    private boolean isFreeFor(int color, WasmLocal local) {
        if (!isFree(color)) {
            return false;
        }
        var set = skippedColors[local.getIndex()];
        return set == null || !set.contains(color);
    }

    private boolean isFree(int color) {
        return owners.get(color) < 0;
    }

    private void kill(WasmLocal local) {
        flush();
        var index = local.getIndex();
        var color = colors[index];
        if (color >= 0 && owners.get(color) == index) {
            availableColorsByType.computeIfAbsent(local.getType(), k -> new IntArrayList()).add(color);
            owners.set(color, -1);
        }
    }

    private void gen(WasmLocal local) {
        genBatch.add(local);
    }

    private void flush() {
        if (!genBatch.isEmpty()) {
            for (var i = 0; i < genBatch.size(); ++i) {
                var local = genBatch.get(i);
                if (hasAffinity(local)) {
                    flushSingleLocalGen(local);
                    genBatch.set(i, null);
                }
            }
            for (var local : genBatch) {
                if (local != null) {
                    flushSingleLocalGen(local);
                }
            }
            genBatch.clear();
        }
    }

    private boolean hasAffinity(WasmLocal local) {
        var index = local.getIndex();
        var color = colors[index];
        if (color >= 0) {
            return true;
        }
        if (assignmentGraph.incomingEdgesCount(local.getIndex()) > 0) {
            for (var src : assignmentGraph.incomingEdges(local.getIndex())) {
                color = colors[src];
                if (color >= 0) {
                    return true;
                }
            }
        }
        if (assignmentGraph.outgoingEdgesCount(local.getIndex()) > 0) {
            for (var dst : assignmentGraph.outgoingEdges(local.getIndex())) {
                if (assignmentGraph.incomingEdgesCount(dst) > 1) {
                    for (var phiInput : assignmentGraph.incomingEdges(dst)) {
                        color = colors[phiInput];
                        if (color >= 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void flushSingleLocalGen(WasmLocal local) {
        var index = local.getIndex();
        var color = colors[index];
        if (color < 0) {
            color = findFreeColor(local);
            colors[index] = color;
            if (colorInfos.get(color).name == null) {
                colorInfos.get(color).name = local.getName();
            }
        } else {
            assert owners.get(color) == index || owners.get(color) < 0;
        }
        owners.set(color, index);
    }

    private int findFreeColor(WasmLocal local) {
        var coalescingColor = coalescingCandidate(local);
        if (coalescingColor >= 0) {
            return coalescingColor;
        }
        var candidates = availableColorsByType.get(local.getType());
        if (candidates != null) {
            for (var candidate : candidates) {
                if (!isFree(candidate.value)) {
                    var backup = candidates.toArray();
                    candidates.clear();
                    for (var color : backup) {
                        if (isFree(color)) {
                            candidates.add(color);
                        }
                    }
                    break;
                }
            }
            for (var candidate : candidates) {
                var color = candidate.value;
                if (isMatchedName(color, local)) {
                    return color;
                }
            }
        }
        return newColor(local);
    }

    private int coalescingCandidate(WasmLocal local) {
        for (var target : assignmentGraph.outgoingEdges(local.getIndex())) {
            var color = colors[target];
            if (color >= 0 && isMatchedName(color, local) && isMatchedType(color, local)) {
                if (isFreeFor(color, local)) {
                    return color;
                } else {
                    scheduleCoalescing(color, local);
                }
            }
            if (assignmentGraph.incomingEdgesCount(target) > 1) {
                for (var sibling : assignmentGraph.incomingEdges(target)) {
                    if (sibling != local.getIndex()) {
                        color = colors[sibling];
                        if (color >= 0 && isMatchedName(color, local) && isMatchedType(color, local)) {
                            if (isFreeFor(color, local)) {
                                return color;
                            } else {
                                scheduleCoalescing(color, local);
                            }
                        }
                    }
                }
            }
        }
        if (assignmentGraph.incomingEdgesCount(local.getIndex()) > 0) {
            for (var source : assignmentGraph.incomingEdges(local.getIndex())) {
                var color = colors[source];
                if (color >= 0 && isFreeFor(color, local) && isMatchedName(color, local)
                        && isMatchedType(color, local)) {
                    return color;
                }
            }
        }
        return -1;
    }

    private void scheduleCoalescing(int color, WasmLocal local) {
        needsCoalescing = true;
        for (var liveOut : cursor.getLiveOut()) {
            if (liveOut != local && !isPhiInput(liveOut) && !isPhi(liveOut)) {
                skipColor(liveOut, color);
            }
        }
    }

    private void skipColor(WasmLocal local, int color) {
        var set = skippedColors[local.getIndex()];
        if (set == null) {
            set = new IntHashSet();
            skippedColors[local.getIndex()] = set;
        }
        set.add(color);
    }

    private boolean isPhiInput(WasmLocal local) {
        for (var target : assignmentGraph.outgoingEdges(local.getIndex())) {
            if (assignmentGraph.incomingEdgesCount(target) > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isPhi(WasmLocal local) {
        return assignmentGraph.incomingEdgesCount(local.getIndex()) > 1;
    }

    private void rewrite() {
        colorLocals = new ArrayList<>(Collections.nCopies(colorInfos.size(), null));
        for (var local = 0; local < colors.length; ++local) {
            var color = colors[local];
            if (color >= 0 && colorLocals.get(color) == null) {
                colorLocals.set(color, locals.get(local));
                locals.get(local).setName(colorInfos.get(color).name);
            }
        }

        var rewriter = new Rewriter();
        rewriter.visitMany(function.getBody());

        var usedLocals = new ArrayList<WasmLocal>();
        for (var local : colorLocals) {
            if (local != null) {
                usedLocals.add(local);
            }
        }

        function.replaceLocals(usedLocals);
    }

    private WasmLocal map(WasmLocal local) {
        var index = local.getIndex();
        var color = colors[index];
        if (color < 0) {
            // Only read in unreachable code
            color = colorLocals.size();
            colors[index] = color;
            colorLocals.add(local);
        }
        return colorLocals.get(color);
    }

    private boolean isMatchedName(int color, WasmLocal local) {
        return isMatchedName(colorInfos.get(color).name, local.getName());
    }

    private boolean isMatchedType(int color, WasmLocal local) {
        return colorInfos.get(color).type == local.getType();
    }

    private boolean isMatchedName(String a, String b) {
        return !friendlyToDebugger || a == null || b == null || a.equals(b);
    }

    private class Walker extends WasmDominanceOrderWalker<int[]> {
        private ArrayDeque<WasmLivenessCursor> cursorStack = new ArrayDeque<>();
        private Set<WasmInstruction> instructionsToSkip = new HashSet<>();

        @Override
        protected int[] saveState() {
            flush();
            return owners.toArray();
        }

        @Override
        protected void restoreState(int[] state) {
            for (var i = 0; i < state.length; ++i) {
                owners.set(i, state[i]);
            }
            for (var i = state.length; i < owners.size(); ++i) {
                owners.set(i, -1);
            }
        }

        @Override
        protected void enterList(WasmInstructionList list) {
            if (cursor != null) {
                cursorStack.push(cursor);
            }
            cursor = liveness.iterate(list);
            if (list.getLast() instanceof WasmBreak br) {
                if (!(br.getTarget().getBreakTarget() instanceof WasmBlock block) || !block.isLoop()) {
                    var insn = br.getPrevious();
                    while (insn instanceof WasmSetLocal) {
                        instructionsToSkip.add(insn);
                        insn = insn.getPrevious();
                    }
                }
            }
            if (list == function.getBody()) {
                for (var local : cursor.getLiveIn()) {
                    owners.set(colors[local.getIndex()], local.getIndex());
                }
            } else {
                for (int i = 0; i < owners.size(); ++i) {
                    var owner = owners.get(i);
                    if (owner >= 0) {
                        var local = locals.get(owners.get(i));
                        if (!cursor.isLiveIn(local)) {
                            kill(local);
                        }
                    }
                }
                for (var local : cursor.getLiveIn()) {
                    var color = colors[local.getIndex()];
                    if (color <= 0 || owners.get(color) < 0) {
                        gen(local);
                    }
                }
            }
        }

        @Override
        protected void exitList(WasmInstructionList list) {
            flush();
            cursor = cursorStack.poll();
        }

        @Override
        protected void exitInstruction(WasmInstruction instruction) {
            if (!instructionsToSkip.remove(instruction)) {
                if (!reachability.isUnreachable(instruction)) {
                    for (var local : cursor.getKillSet()) {
                        kill(local);
                    }
                    for (var local : cursor.getGenSet()) {
                        gen(local);
                    }
                }
            }
            cursor.next();
        }
    }

    private class Rewriter extends WasmDefaultInstructionVisitor {
        @Override
        public void visit(WasmGetLocal instruction) {
            instruction.setLocal(map(instruction.getLocal()));
        }

        @Override
        public void visit(WasmSetLocal instruction) {
            instruction.setLocal(map(instruction.getLocal()));
        }

        @Override
        public void visit(WasmTeeLocal instruction) {
            instruction.setLocal(map(instruction.getLocal()));
        }
    }

    private static class ColorInfo {
        WasmType type;
        String name;

        ColorInfo(WasmType type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}
