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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.backend.wasm.model.WasmArrayConstructor;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.instruction.WasmArraySet;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCallReference;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmFloat32Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloat64Constant;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCall;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt64Constant;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmStructGet;
import org.teavm.backend.wasm.model.instruction.WasmStructNew;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmThrow;
import org.teavm.backend.wasm.model.instruction.WasmTry;

/**
 * <p>Replaces construction of an array of constant length, followed by assignments to all its elements,
 * with {@code array.new_fixed}. The following code, produced for {@code new int[] { a, b }}:</p>
 *
 * <pre>
 *     ; push class info
 *     local.get $size     ; $size is known to hold 2
 *     call $newArray
 *     local.set $array
 *     local.get $array
 *     struct.get $Array 2
 *     local.set $data
 *     ; compute a
 *     local.get $data
 *     local.get $index    ; $index is known to hold 0
 *     local.get $a
 *     array.set
 *     ; compute b
 *     local.get $data
 *     i32.const 1
 *     local.get $b
 *     array.set
 * </pre>
 *
 * <p>is turned into</p>
 *
 * <pre>
 *     ; push class info
 *     ; header of the array, see {@link WasmArrayConstructor#emitHeader}
 *     ; compute a
 *     local.get $a
 *     ; compute b
 *     local.get $b
 *     array.new_fixed 2
 *     local.tee $data
 *     struct.new $Array
 *     local.set $array
 *</pre>
 *
 * <p>Code generator merges {@code local.set} and immediately following {@code local.get} of the same local into
 * {@code local.tee}, so storing the array may be merged with getting it before {@code struct.get},
 * and storing data may be merged with getting it at the first assignment. Both forms are recognized.</p>
 *
 * <p>Allocating function is recognized by {@link WasmFunction#getArrayConstructor()}. Each element is
 * assigned exactly once, in order of indexes. Between assignments there can be any instructions that leave
 * the operand stack unchanged (like computation of elements, class initialization, etc.), as long as they
 * don't access neither array nor its data. Values of elements are kept on the operand stack, so constructions
 * may be nested, i.e. element can be another array, constructed between assignments.</p>
 *
 * <p>Assignments to array and data locals are postponed, not removed, so it does not matter whether these
 * locals are read afterwards. Stores that turn out to be dead are removed by {@link WasmDeadStoreOptimizer}.
 * The same applies to locals that hold length and indexes: only reads of these locals are removed.
 * However, any instruction that may leave the instruction list between allocation and the last assignment
 * prevents optimization, as postponed locals may be read there. This includes branches to outer blocks and,
 * inside {@code try}, instructions that may throw.</p>
 *
 * <p>Instruction list is examined in a single pass, like an automaton: allocations start constructions,
 * which are kept on a stack, and every instruction either continues the topmost matching construction
 * or is checked against all pending constructions. Locals are known to hold constants, when they are assigned
 * by {@code i32.const} earlier in the same list and not written since then, including nested lists.
 * Each list is processed separately, nested lists are not examined for constructions.</p>
 */
class WasmArrayConstructionOptimizer {
    // Engines don't accept array.new_fixed with more elements
    private static final int MAX_LENGTH = 10000;

    private final List<Construction> pending = new ArrayList<>();
    private final List<Construction> completed = new ArrayList<>();
    private final Map<WasmLocal, Integer> constants = new HashMap<>();
    private final EffectVisitor effectVisitor = new EffectVisitor();
    private boolean insideTry;

    /**
     * Processes top-level instructions of the given list.
     *
     * @param insideTry whether the list is (transitively) inside {@code try} that catches exceptions.
     */
    void process(WasmInstructionList list, boolean insideTry) {
        this.insideTry = insideTry;
        var insn = list.getFirst();
        while (insn != null) {
            insn = step(insn);
        }
        for (var construction : completed) {
            apply(construction);
        }
        pending.clear();
        completed.clear();
        constants.clear();
    }

    private WasmInstruction step(WasmInstruction insn) {
        if (insn instanceof WasmGetLocal get) {
            for (var i = pending.size() - 1; i >= 0; --i) {
                var construction = pending.get(i);
                WasmInstruction next = null;
                if (construction.dataLocal == null && construction.arrayLocal == get.getLocal()) {
                    next = matchUnwrap(get, get.getNext(), construction);
                } else if (construction.dataLocal == get.getLocal()) {
                    next = matchElement(get, get.getNext(), construction);
                }
                if (next != null) {
                    return next;
                }
            }
        } else if (insn instanceof WasmCall call) {
            var next = matchAllocation(call);
            if (next != null) {
                return next;
            }
        }
        handleGeneric(insn);
        if (insn instanceof WasmSetLocal set && set.getPrevious() instanceof WasmInt32Constant constant) {
            constants.put(set.getLocal(), constant.getValue());
        }
        return insn.getNext();
    }

    private WasmInstruction matchAllocation(WasmCall call) {
        var constructor = call.getFunction().getArrayConstructor();
        if (constructor == null) {
            return null;
        }
        var store = call.getNext();
        WasmLocal arrayLocal;
        if (store instanceof WasmSetLocal set) {
            arrayLocal = set.getLocal();
        } else if (store instanceof WasmTeeLocal tee) {
            arrayLocal = tee.getLocal();
        } else {
            return null;
        }
        var length = constantValue(call.getPrevious());
        if (length == null || length <= 0 || length > MAX_LENGTH) {
            return null;
        }

        handleGeneric(call);
        write(arrayLocal);
        var construction = new Construction(constructor, call, store, arrayLocal, length);
        pending.add(construction);
        if (store instanceof WasmTeeLocal) {
            // Code generator merges local.set and subsequent local.get of the same local into local.tee
            var next = matchUnwrap(null, store.getNext(), construction);
            if (next == null) {
                pending.remove(construction);
                next = store.getNext();
            }
            return next;
        }
        return store.getNext();
    }

    private WasmInstruction matchUnwrap(WasmGetLocal get, WasmInstruction insn, Construction construction) {
        var constructor = construction.constructor;
        if (!(insn instanceof WasmStructGet structGet) || structGet.getType() != constructor.getStructure()
                || structGet.getFieldIndex() != constructor.getDataField()) {
            return null;
        }
        var store = structGet.getNext();
        WasmLocal dataLocal;
        if (store instanceof WasmSetLocal set) {
            dataLocal = set.getLocal();
        } else if (store instanceof WasmTeeLocal tee) {
            dataLocal = tee.getLocal();
        } else {
            return null;
        }
        if (dataLocal == construction.arrayLocal) {
            return null;
        }
        write(dataLocal);
        construction.unwrapGet = get;
        construction.unwrap = structGet;
        construction.dataStore = store;
        construction.dataLocal = dataLocal;
        if (store instanceof WasmTeeLocal) {
            var next = matchElement(null, store.getNext(), construction);
            if (next == null) {
                pending.remove(construction);
                next = store.getNext();
            }
            return next;
        }
        return store.getNext();
    }

    private WasmInstruction matchElement(WasmGetLocal get, WasmInstruction index, Construction construction) {
        var indexValue = constantValue(index);
        if (indexValue == null || indexValue != construction.elements.size()) {
            return null;
        }
        var value = index.getNext();
        if (!isSimpleValue(value, construction) || !(value.getNext() instanceof WasmArraySet set)
                || set.getType() != construction.constructor.getArray()) {
            return null;
        }

        // Value of element is kept on stack, so it must be above values of constructions started earlier
        while (pending.get(pending.size() - 1) != construction) {
            pending.remove(pending.size() - 1);
        }
        if (index instanceof WasmGetLocal indexGet) {
            read(indexGet.getLocal());
        }
        if (value instanceof WasmGetLocal valueGet) {
            read(valueGet.getLocal());
        }
        construction.elements.add(new Element(get, index, set));
        if (construction.elements.size() == construction.length) {
            pending.remove(pending.size() - 1);
            completed.add(construction);
        }
        return set.getNext();
    }

    private Integer constantValue(WasmInstruction insn) {
        if (insn instanceof WasmInt32Constant constant) {
            return constant.getValue();
        } else if (insn instanceof WasmGetLocal get) {
            return constants.get(get.getLocal());
        }
        return null;
    }

    private static boolean isSimpleValue(WasmInstruction insn, Construction construction) {
        if (insn instanceof WasmGetLocal get) {
            return get.getLocal() != construction.arrayLocal && get.getLocal() != construction.dataLocal;
        }
        return insn instanceof WasmInt32Constant || insn instanceof WasmInt64Constant
                || insn instanceof WasmFloat32Constant || insn instanceof WasmFloat64Constant
                || insn instanceof WasmNullConstant || insn instanceof WasmGetGlobal;
    }

    private void handleGeneric(WasmInstruction insn) {
        if (insn instanceof WasmGetLocal get) {
            read(get.getLocal());
        } else if (insn instanceof WasmSetLocal set) {
            write(set.getLocal());
        } else if (insn instanceof WasmTeeLocal tee) {
            write(tee.getLocal());
        } else if (insn instanceof WasmBlock || insn instanceof WasmConditional || insn instanceof WasmTry) {
            if (pending.isEmpty()) {
                // Nested lists may write to locals, but there's nothing to examine thoroughly
                constants.clear();
            } else {
                insn.acceptVisitor(effectVisitor);
                effectVisitor.innerLists.clear();
            }
        } else if (insn instanceof WasmBranch || insn instanceof WasmNullBranch || insn instanceof WasmCastBranch
                || insn instanceof WasmSwitch || insn instanceof WasmBreak) {
            pending.clear();
        } else if (insideTry && mayThrow(insn)) {
            pending.clear();
        }
        if (insn.isTerminating()) {
            pending.clear();
        }
    }

    private static boolean mayThrow(WasmInstruction insn) {
        return insn instanceof WasmCall || insn instanceof WasmCallReference || insn instanceof WasmIndirectCall
                || insn instanceof WasmThrow;
    }

    private void read(WasmLocal local) {
        kill(local);
    }

    private void write(WasmLocal local) {
        constants.remove(local);
        kill(local);
    }

    private void kill(WasmLocal local) {
        if (!pending.isEmpty()) {
            pending.removeIf(construction -> construction.arrayLocal == local || construction.dataLocal == local);
        }
    }

    private void apply(Construction construction) {
        var call = construction.call;
        var location = call.getLocation();
        var size = call.getPrevious();
        var classInfo = size.getPrevious();

        var header = new WasmInstructionList();
        construction.constructor.emitHeader(header.builder());
        for (var insn = header.getFirst(); insn != null; insn = insn.getNext()) {
            insn.setLocation(location);
        }
        var firstHeaderInsn = header.getFirst();
        call.insertNext(header);
        if (firstHeaderInsn instanceof WasmDrop && isPure(classInfo)) {
            classInfo.delete();
            firstHeaderInsn.delete();
        }
        call.delete();
        size.delete();

        if (construction.unwrapGet != null) {
            construction.unwrapGet.delete();
        }
        construction.unwrap.delete();

        var lastSet = construction.elements.get(construction.elements.size() - 1).set;
        var arrayNew = new WasmArrayNewFixed(construction.constructor.getArray(), construction.length);
        arrayNew.setLocation(location);
        var dataTee = new WasmTeeLocal(construction.dataLocal);
        dataTee.setLocation(construction.dataStore.getLocation());
        var structNew = new WasmStructNew(construction.constructor.getStructure());
        structNew.setLocation(location);
        var arraySet = new WasmSetLocal(construction.arrayLocal);
        arraySet.setLocation(construction.arrayStore.getLocation());
        lastSet.insertNext(arrayNew);
        arrayNew.insertNext(dataTee);
        dataTee.insertNext(structNew);
        structNew.insertNext(arraySet);
        construction.dataStore.delete();
        construction.arrayStore.delete();

        for (var element : construction.elements) {
            if (element.dataGet != null) {
                element.dataGet.delete();
            }
            element.index.delete();
            element.set.delete();
        }
    }

    private static boolean isPure(WasmInstruction insn) {
        return insn instanceof WasmGetGlobal || insn instanceof WasmGetLocal || insn instanceof WasmInt32Constant
                || insn instanceof WasmInt64Constant || insn instanceof WasmFloat32Constant
                || insn instanceof WasmFloat64Constant || insn instanceof WasmNullConstant;
    }

    private static class Construction {
        final WasmArrayConstructor constructor;
        final WasmCall call;
        final WasmInstruction arrayStore;
        final WasmLocal arrayLocal;
        final int length;
        WasmGetLocal unwrapGet;
        WasmStructGet unwrap;
        WasmInstruction dataStore;
        WasmLocal dataLocal;
        final List<Element> elements = new ArrayList<>();

        Construction(WasmArrayConstructor constructor, WasmCall call, WasmInstruction arrayStore,
                WasmLocal arrayLocal, int length) {
            this.constructor = constructor;
            this.call = call;
            this.arrayStore = arrayStore;
            this.arrayLocal = arrayLocal;
            this.length = length;
        }
    }

    private static class Element {
        final WasmGetLocal dataGet;
        final WasmInstruction index;
        final WasmArraySet set;

        Element(WasmGetLocal dataGet, WasmInstruction index, WasmArraySet set) {
            this.dataGet = dataGet;
            this.index = index;
            this.set = set;
        }
    }

    private class EffectVisitor extends WasmDefaultInstructionVisitor {
        // Branch can only target enclosing lists, so it leaves examined instruction unless its target is here
        final Set<WasmInstructionList> innerLists = new HashSet<>();

        @Override
        public void visit(WasmBlock instruction) {
            innerLists.add(instruction.getBody());
            super.visit(instruction);
        }

        @Override
        public void visit(WasmConditional instruction) {
            innerLists.add(instruction.getThenBlock());
            innerLists.add(instruction.getElseBlock());
            super.visit(instruction);
        }

        @Override
        public void visit(WasmTry instruction) {
            // Exception handlers may reside outside of examined instruction
            pending.clear();
        }

        @Override
        public void visit(WasmBranch instruction) {
            checkTarget(instruction.getTarget());
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            checkTarget(instruction.getTarget());
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            checkTarget(instruction.getTarget());
        }

        @Override
        public void visit(WasmBreak instruction) {
            checkTarget(instruction.getTarget());
        }

        @Override
        public void visit(WasmSwitch instruction) {
            for (var target : instruction.getTargets()) {
                checkTarget(target);
            }
            checkTarget(instruction.getDefaultTarget());
        }

        @Override
        public void visit(WasmGetLocal instruction) {
            read(instruction.getLocal());
        }

        @Override
        public void visit(WasmSetLocal instruction) {
            write(instruction.getLocal());
        }

        @Override
        public void visit(WasmTeeLocal instruction) {
            write(instruction.getLocal());
        }

        @Override
        public void visit(WasmCall instruction) {
            checkThrow();
        }

        @Override
        public void visit(WasmCallReference instruction) {
            checkThrow();
        }

        @Override
        public void visit(WasmIndirectCall instruction) {
            checkThrow();
        }

        @Override
        public void visit(WasmThrow instruction) {
            checkThrow();
        }

        private void checkTarget(WasmInstructionList target) {
            if (!innerLists.contains(target)) {
                pending.clear();
            }
        }

        private void checkThrow() {
            if (insideTry) {
                pending.clear();
            }
        }
    }
}
