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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.analysis.WasmDominanceOrderWalker;
import org.teavm.backend.wasm.analysis.WasmLivenessAnalysis;
import org.teavm.backend.wasm.analysis.WasmLivenessCursor;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;

/**
 * <p>Reduces number of locals by merging locals of the same type that are never live simultaneously.
 * This works similarly to register allocation for SSA, but since number of locals is unlimited,
 * there's no spilling.</p>
 *
 * <p>Every local gets a <em>color</em>, and all locals of the same color are eventually replaced
 * by a single local. Instructions are visited in dominance order by {@link WasmDominanceOrderWalker},
 * while maintaining which color is occupied by which local. When a local is defined first time
 * ({@code local.set} or {@code local.tee}), it gets a color of the same type that is not occupied
 * by any live local, or a new color, if there's no such color. When a local is read last time,
 * its color is released. On forks in dominator tree the state is saved, and restored when walk proceeds
 * to the other branch. At join points state from dominator lacks locals defined after the dominator,
 * so it's complemented by locals that are live at the join point.</p>
 *
 * <p>Locals that are live at the start of the function (i.e. parameters and locals that rely on
 * default value) get distinct colors before the walk. Parameters always keep their indexes.</p>
 *
 * <p>Since instructions are visited in their textual order and every forward edge points to an instruction
 * that follows its source, a local that is live at a reachable point has always got its color before
 * the walk reaches this point. Thus, interference of two locals is always detected at the definition
 * of one of them. For a local with single definition, the color is chosen at the definition, so it never
 * conflicts. However, a local may have multiple definitions (e.g. one in every branch of a conditional),
 * and a color chosen at the first definition may be occupied at another one. In this case, the local is
 * <em>pinned</em>, i.e. it gets a dedicated color that is never shared with other locals, and the whole
 * allocation is repeated.</p>
 */
public class WasmLocalsOptimizer {
    private WasmFunction function;
    private List<WasmLocal> locals;
    private WasmLivenessAnalysis liveness;
    private boolean[] pinned;
    private boolean conflict;

    private int[] colors;
    private List<WasmType> colorTypes = new ArrayList<>();
    private List<String> colorNames = new ArrayList<>();
    private List<WasmLocal> colorLocals = new ArrayList<>();
    private Map<WasmType, List<Integer>> sharedColorsByType = new HashMap<>();
    private int[] owners = new int[16];

    private WasmLivenessCursor cursor;
    private ArrayDeque<WasmLivenessCursor> cursorStack = new ArrayDeque<>();

    private boolean friendlyToDebugger;

    public void optimize(WasmFunction function) {
        this.function = function;
        locals = function.getLocalVariables();
        if (!locals.isEmpty()) {
            liveness = new WasmLivenessAnalysis(function);
            pinned = new boolean[locals.size()];
            do {
                conflict = false;
                allocate();
            } while (conflict);
            rewrite();
        }
        this.function = null;
        locals = null;
        liveness = null;
        pinned = null;
        colors = null;
        colorTypes.clear();
        colorNames.clear();
        colorLocals.clear();
        sharedColorsByType.clear();
    }

    public void setFriendlyToDebugger(boolean debuggerFriendly) {
        this.friendlyToDebugger = debuggerFriendly;
    }

    private void allocate() {
        colors = new int[locals.size()];
        Arrays.fill(colors, -1);
        colorTypes.clear();
        colorNames.clear();
        sharedColorsByType.clear();

        var parameterCount = Math.min(function.getType().getParameterTypes().size(), locals.size());
        for (var i = 0; i < parameterCount; ++i) {
            colors[i] = newColor(locals.get(i));
        }
        var entryLive = liveness.iterate(function.getBody()).getLiveIn();
        for (var local : entryLive) {
            if (colors[local.getIndex()] < 0) {
                colors[local.getIndex()] = newColor(local);
            }
        }
        for (var i = parameterCount; i < locals.size(); ++i) {
            if (pinned[i]) {
                colors[i] = newColor(locals.get(i));
            }
        }

        new Walker().walk(function.getBody());
    }

    private int newColor(WasmLocal local) {
        var color = colorTypes.size();
        colorTypes.add(local.getType());
        colorNames.add(local.getName());
        if (!pinned[local.getIndex()]) {
            sharedColorsByType.computeIfAbsent(local.getType(), k -> new ArrayList<>()).add(color);
        }
        if (color >= owners.length) {
            owners = Arrays.copyOf(owners, owners.length * 2);
        }
        owners[color] = -1;
        return color;
    }

    /**
     * Checks whether color is not occupied by a local that is live after current instruction.
     * Color may still keep owner which is already dead, if the owner died on an edge, not by a read.
     */
    private boolean isFree(int color, int except) {
        var owner = owners[color];
        return owner < 0 || owner == except || !cursor.isLiveOut(locals.get(owner));
    }

    private void use(WasmLocal local) {
        var index = local.getIndex();
        var color = colors[index];
        if (color >= 0 && owners[color] == index && !cursor.isLiveOut(local)) {
            owners[color] = -1;
        }
    }

    private void define(WasmLocal local) {
        var index = local.getIndex();
        if (pinned[index]) {
            return;
        }
        var color = colors[index];
        if (color < 0) {
            color = findFreeColor(local);
            colors[index] = color;
            if (colorNames.get(color) == null) {
                colorNames.set(color, local.getName());
            }
        } else if (!isFree(color, index)) {
            pinned[index] = true;
            conflict = true;
            return;
        }
        owners[color] = cursor.isLiveOut(local) ? index : -1;
    }

    private int findFreeColor(WasmLocal local) {
        var candidates = sharedColorsByType.get(local.getType());
        if (candidates != null) {
            for (var color : candidates) {
                if (isFree(color, -1) && isMatchedName(colorNames.get(color), local.getName())) {
                    return color;
                }
            }
        }
        return newColor(local);
    }

    /**
     * Marks all locals live after current instruction as owners of their colors.
     */
    private void acquireLiveOut() {
        for (var local : cursor.getLiveOut()) {
            var color = colors[local.getIndex()];
            if (color >= 0) {
                owners[color] = local.getIndex();
            }
        }
    }

    private void rewrite() {
        colorLocals.clear();
        for (var i = 0; i < colorTypes.size(); ++i) {
            colorLocals.add(null);
        }
        for (var i = 0; i < locals.size(); ++i) {
            var color = colors[i];
            if (color >= 0 && colorLocals.get(color) == null) {
                colorLocals.set(color, locals.get(i));
            }
        }

        var rewriter = new Rewriter();
        rewriter.visitMany(function.getBody());

        var newLocals = new ArrayList<WasmLocal>();
        for (int i = 0; i < colorLocals.size(); i++) {
            var local = colorLocals.get(i);
            if (local != null) {
                newLocals.add(local);
            }
        }
        for (var color = 0; color < newLocals.size(); ++color) {
            colorLocals.get(color).setName(colorNames.get(color));
        }
        function.replaceLocals(newLocals);
    }

    private WasmLocal map(WasmLocal local) {
        var index = local.getIndex();
        var color = colors[index];
        if (color < 0) {
            // Only read in unreachable code
            color = colorLocals.size();
            colors[index] = color;
            colorTypes.add(local.getType());
            colorNames.add(local.getName());
            colorLocals.add(local);
        }
        return colorLocals.get(color);
    }

    private boolean isMatchedName(String a, String b) {
        return !friendlyToDebugger || a == null || b == null || a.equals(b);
    }

    private class Walker extends WasmDominanceOrderWalker<int[]> {
        @Override
        protected int[] saveState() {
            return Arrays.copyOf(owners, colorTypes.size());
        }

        @Override
        protected void restoreState(int[] state) {
            System.arraycopy(state, 0, owners, 0, state.length);
            Arrays.fill(owners, state.length, colorTypes.size(), -1);
        }

        @Override
        protected void enterList(WasmInstructionList list) {
            if (cursor != null) {
                cursorStack.push(cursor);
            }
            cursor = liveness.iterate(list);
            if (list == function.getBody()) {
                for (var local : cursor.getLiveIn()) {
                    owners[colors[local.getIndex()]] = local.getIndex();
                }
            }
        }

        @Override
        protected void exitList(WasmInstructionList list) {
            cursor = cursorStack.poll();
        }

        @Override
        protected void enterInstruction(WasmInstruction instruction) {
            if (instruction instanceof WasmGetLocal) {
                use(((WasmGetLocal) instruction).getLocal());
            } else if (instruction instanceof WasmSetLocal) {
                define(((WasmSetLocal) instruction).getLocal());
            } else if (instruction instanceof WasmTeeLocal) {
                define(((WasmTeeLocal) instruction).getLocal());
            }
        }

        @Override
        protected void exitInstruction(WasmInstruction instruction) {
            if (instruction instanceof WasmBlock || instruction instanceof WasmConditional
                    || instruction instanceof WasmTry) {
                acquireLiveOut();
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
}
