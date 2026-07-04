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
package org.teavm.backend.wasm.generate.methods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.model.TextLocation;
import org.teavm.model.Variable;

class WasmVariableTracker {
    private WasmFunction function;
    private int firstVariable;
    private VarDefinition[] varDefinitions;
    private Level level;

    WasmVariableTracker(WasmFunction function, int firstVariable, WasmInstructionBuilder builder, int varCount) {
        this.function = function;
        this.firstVariable = firstVariable;
        varDefinitions = new VarDefinition[varCount];
        level = new Level(builder, null);
    }

    void enterLevel(WasmInstructionBuilder builder) {
        level = new Level(builder, level);
    }

    void exitLevel() {
        level = level.next;
    }

    WasmInstruction getTargetInstructionAtLevel(int depth) {
        var entry = level.top;
        for (var i = 0; i < depth; ++i) {
            entry = entry.next;
        }
        return entry != null ? entry.mostRecentInstructionAtLevel : level.mostRecentTopLevelInstruction;
    }

    void storeToVariable(Variable variable) {
        advanceMostRecentInstruction();
        var varDef = new VarDefinition(level, level.builder.list.getLast(), variable,
                level.mostRecentTopLevelDefinition != null ? level.mostRecentVarDefinition.order + 1 : 0);
        level.mostRecentVarDefinition = varDef;
        varDefinitions[varDef.variable.getIndex()] = varDef;
        if (level.top != null) {
            level.top.mostRecentDefinitionAtLevel = varDef;
        } else {
            level.mostRecentTopLevelDefinition = varDef;
        }
        var depth = level.top != null ? level.top.depth + 1 : 0;
        level.top = new StackEntry(level.top, depth, varDef);
        varDef.stackEntry = level.top;
    }

    void advanceMostRecentInstruction() {
        var instruction = level.builder.list.getLast();
        if (level.top != null) {
            level.top.mostRecentInstructionAtLevel = instruction;
        } else {
            level.mostRecentTopLevelInstruction = instruction;
        }
    }

    List<WasmInstruction> pushArgs(List<? extends Variable> args) {
        if (args.isEmpty()) {
            return Collections.emptyList();
        }
        var startArgsIndex = findSuitableDefinition(args, 0);
        var result = new ArrayList<WasmInstruction>();
        if (startArgsIndex < 0) {
            for (var arg : args) {
                ensureVariableInLocal(arg);
                level.builder.getLocal(mapToLocal(arg));
                result.add(level.builder.list.getLast());
            }
            return result;
        }
        var endArgsIndex = startArgsIndex + 1;
        fillMissingArgs(args, 0, startArgsIndex, result);
        var entry = findEntry(args.get(startArgsIndex)).next;
        result.add(entry == null ? level.mostRecentTopLevelInstruction : entry.mostRecentInstructionAtLevel);
        while (true) {
            var nextArgsIndex = findSuitableDefinition(args, endArgsIndex);
            if (nextArgsIndex < 0) {
                break;
            }
            fillMissingArgs(args, endArgsIndex, nextArgsIndex, result);
            if (endArgsIndex + 1 < nextArgsIndex) {
                flushExtraArgs(args.get(endArgsIndex), args.get(nextArgsIndex - 1));
            }
            entry = findEntry(args.get(nextArgsIndex)).next;
            result.add(entry == null ? level.mostRecentTopLevelInstruction : entry.mostRecentInstructionAtLevel);
            endArgsIndex = nextArgsIndex + 1;
        }
        entry = findEntry(args.get(startArgsIndex));
        while (level.top != entry.next) {
            level.top.definition.stackEntry = null;
            level.top = level.top.next;
        }
        return result;
    }

    void ensureVariableInLocal(Variable variable) {
        var def = varDefinitions[variable.getIndex()];
        if (def != null && !def.savedToLocal) {
            def.savedToLocal = true;
            var tee = new WasmTeeLocal(mapToLocal(def.variable));
            tee.setLocation(def.instruction.getLocation());
            def.instruction.insertNext(tee);
        }
    }

    private int findSuitableDefinition(List<? extends Variable> args, int start) {
        for (var i = start; i < args.size(); ++i) {
            if (findEntry(args.get(i)) != null) {
                return allArgsDefinedBefore(args, start, i) ? i : -1;
            }
        }
        return -1;
    }

    private void fillMissingArgs(List<? extends Variable> args, int start, int end, List<WasmInstruction> result) {
        if (start + 1 >= end) {
            return;
        }
        var currentEntry = varDefinitions[args.get(end).getIndex()].stackEntry;
        var nextEntry = currentEntry.next;
        var referenceInsn = nextEntry != null
                ? nextEntry.mostRecentInstructionAtLevel
                : level.mostRecentTopLevelInstruction;
        TextLocation location;
        if (referenceInsn != null) {
            location = referenceInsn.getLocation();
        } else if (level.builder.list.getFirst() != null) {
            location = level.builder.list.getFirst().getLocation();
        } else {
            location = null;
        }
        for (var i = start; i < end; ++i) {
            var arg = args.get(i);
            ensureVariableInLocal(arg);
            var insn = new WasmGetLocal(mapToLocal(arg));
            insn.setLocation(location);
            level.builder.list.insertAfter(insn, referenceInsn);
            result.add(insn);
            referenceInsn = insn;
        }
    }

    private void flushExtraArgs(Variable start, Variable end) {
        var entry = findEntry(end);
        var finalEntry = findEntry(start);
        while (entry != finalEntry) {
            entry.definition.savedToLocal = true;
            var store = new WasmSetLocal(mapToLocal(entry.definition.variable));
            store.setLocation(entry.definition.instruction.getLocation());
            entry.definition.instruction.insertNext(store);
            entry.definition.stackEntry = null;
            entry = entry.next;
        }
    }

    private StackEntry findEntry(Variable variable) {
        var definition = varDefinitions[variable.getIndex()];
        return definition != null && definition.level == level ? definition.stackEntry : null;
    }

    private boolean allArgsDefinedBefore(List<? extends Variable> args, int start, int index) {
        var currentEntry = varDefinitions[args.get(index).getIndex()].stackEntry;
        var nextEntry = currentEntry.next;
        var referenceDef = nextEntry != null
                ? nextEntry.mostRecentDefinitionAtLevel
                : level.mostRecentTopLevelDefinition;
        var referenceOrder = referenceDef != null ? referenceDef.order : -1;
        for (var i = start; i < index; ++i) {
            var def = varDefinitions[args.get(i).getIndex()];
            if (def != null && def.level != level && def.order > referenceOrder) {
                return false;
            }
        }
        return true;
    }


    WasmLocal mapToLocal(Variable variable) {
        return function.getLocalVariables().get(variable.getIndex() - firstVariable);
    }

    boolean hasStack() {
        return level.top != null;
    }

    void dropStack() {
        while (level.top != null) {
            var afterInsn = level.top.definition.instruction;
            var drop = new WasmDrop();
            drop.setLocation(afterInsn.getLocation());
            afterInsn.insertNext(drop);
            level.top = level.top.next;
        }
    }

    private static class Level {
        private WasmInstructionBuilder builder;
        private Level next;
        private VarDefinition mostRecentVarDefinition;
        private StackEntry top;
        private WasmInstruction mostRecentTopLevelInstruction;
        private VarDefinition mostRecentTopLevelDefinition;

        Level(WasmInstructionBuilder builder, Level next) {
            this.builder = builder;
            this.next = next;
        }
    }

    private static class StackEntry {
        private WasmInstruction mostRecentInstructionAtLevel;
        private VarDefinition mostRecentDefinitionAtLevel;
        private StackEntry next;
        private int depth;
        private VarDefinition definition;

        StackEntry(StackEntry next, int depth, VarDefinition definition) {
            this.next = next;
            this.depth = depth;
            this.definition = definition;
        }
    }

    private static class VarDefinition {
        Level level;
        StackEntry stackEntry;
        WasmInstruction instruction;
        Variable variable;
        int order;
        boolean savedToLocal;

        VarDefinition(Level level,  WasmInstruction instruction, Variable variable, int order) {
            this.level = level;
            this.instruction = instruction;
            this.variable = variable;
            this.order = order;
        }
    }
}
