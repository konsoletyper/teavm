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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmArrayCopy;
import org.teavm.backend.wasm.model.instruction.WasmArrayGet;
import org.teavm.backend.wasm.model.instruction.WasmArrayLength;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.instruction.WasmArraySet;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCallReference;
import org.teavm.backend.wasm.model.instruction.WasmCast;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmConversion;
import org.teavm.backend.wasm.model.instruction.WasmCopy;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmExternConversion;
import org.teavm.backend.wasm.model.instruction.WasmFill;
import org.teavm.backend.wasm.model.instruction.WasmFloat32Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloat64Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinary;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnary;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReference;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCall;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInstructionUtil;
import org.teavm.backend.wasm.model.instruction.WasmInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmInt31Get;
import org.teavm.backend.wasm.model.instruction.WasmInt31Reference;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt64Constant;
import org.teavm.backend.wasm.model.instruction.WasmIntBinary;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnary;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIsNull;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat32;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat64;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt32;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt64;
import org.teavm.backend.wasm.model.instruction.WasmMemoryGrow;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.backend.wasm.model.instruction.WasmReferencesEqual;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat32;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat64;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt32;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt64;
import org.teavm.backend.wasm.model.instruction.WasmStructGet;
import org.teavm.backend.wasm.model.instruction.WasmStructNew;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmStructSet;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTest;
import org.teavm.backend.wasm.model.instruction.WasmThrow;
import org.teavm.backend.wasm.model.instruction.WasmTry;
import org.teavm.backend.wasm.model.instruction.WasmTypeInference;
import org.teavm.backend.wasm.model.instruction.WasmUnreachable;

/**
 * <p>Performs optimizations directly on Wasm instructions. To keep compilation fast, all optimizations are
 * performed in a single pass, visiting every instruction once, in order. The pass does not need liveness
 * of locals. It may leave stores to locals that are never read afterwards; these are removed by
 * {@link WasmDeadStoreOptimizer}, which should run after this pass.</p>
 *
 * <p>The pass replaces unnecessary spills of stack values to locals, i.e.</p>
 *
 * <pre>
 *     ; push a
 *     local.set 1
 *     ; push b
 *     local.set 2
 *     local.get 1
 *     local.get 2
 * </pre>
 *
 * <p>with</p>
 *
 * <pre>
 *     ; push a
 *     local.tee 1
 *     ; push b
 *     local.tee 2
 * </pre>
 *
 * <p>Each instruction list is walked from first to last instruction, simulating operand stack,
 * see {@link StackEntry}. Every {@code local.set} is recorded as a <em>write</em>, see {@link Write}, which
 * is attached to the stack entry that was on top when the write happened. The written value could have stayed
 * on the stack right above this entry, if {@code local.set} was replaced with {@code local.tee}. When the entry
 * is consumed, writes attached to it are discarded, since their values would reside among consumed values.</p>
 *
 * <p>When {@code local.get} reads a local whose most recent write is attached to the current top of the stack,
 * the write becomes {@code local.tee} and {@code local.get} is removed. Writes that were attached to the same
 * entry after this write are re-attached to the value kept on the stack, since now they happen above it.</p>
 *
 * <p>{@code local.tee} is always kept, even if the removed {@code local.get} was the only read of the value,
 * since the value may be read elsewhere, including targets of branches and exception handlers.
 * {@link WasmDeadStoreOptimizer} decides this precisely, using liveness computed after this pass.</p>
 *
 * <p>When the write is attached to an entry below the top of the stack, i.e.</p>
 *
 * <pre>
 *     ; compute a
 *     local.set 1
 *     ; push b
 *     local.get 1
 *     i32.sub
 * </pre>
 *
 * <p>computation of {@code a} is moved after instructions that push {@code b}, if it's safe, so that
 * the write can be turned into {@code local.tee}, see {@link #tryReschedule(Write, WasmGetLocal)}.
 * To support this, stack entries and writes track first instruction of the computation of a value.</p>
 *
 * <p>Before the main pass, conditional jumps over blocks are turned into {@code if} instructions,
 * see {@link #reconstructConditionals(WasmInstructionList)}. This merges blocks, so that the main pass
 * sees longer instruction lists.</p>
 *
 * <p>Before the main pass processes an instruction list, construction of arrays followed by assignments
 * to all elements are replaced with {@code array.new_fixed} in this list, see
 * {@link WasmArrayConstructionOptimizer}. This keeps values of elements on stack, and the main pass removes
 * their spills.</p>
 */
public class WasmInstructionsOptimizer {
    private WasmFunction function;
    private final Map<WasmInstructionList, Set<WasmInstruction>> branchRefs = new HashMap<>();
    private final Map<WasmInstructionList, List<WasmCatchClause>> branchCatchRefs = new HashMap<>();
    private final WasmTypeInference conditionTypeInference = new WasmTypeInference();
    private final Set<WasmInstruction> conditionInstructions = new HashSet<>();
    private final WasmArrayConstructionOptimizer arrayConstructionOptimizer = new WasmArrayConstructionOptimizer();
    private final WasmReschedulingAnalysis moveAnalysis = new WasmReschedulingAnalysis();
    private final WasmReschedulingAnalysis crossAnalysis = new WasmReschedulingAnalysis();
    private int catchingTryDepth;
    private StackEntry stackTop;

    /*
     * The most recent write of each local, if it still can be turned into local.tee, otherwise null.
     * Writes made in nested lists stay here after these lists are processed. They can't be paired with reads
     * in outer lists (see StackEntry.list), but they shadow outer writes of the same locals, which are not
     * the most recent writes anymore.
     */
    private Write[] writesByLocal;

    public void optimize(WasmFunction function) {
        this.function = function;
        new BranchRefCollector().visitMany(function.getBody());
        reconstructConditionals(function.getBody());
        writesByLocal = new Write[function.getLocalVariables().size()];
        processList(function.getBody(), 0);
        writesByLocal = null;
        branchRefs.clear();
        this.function = null;
    }

    /**
     * <p>Turns conditional jumps over blocks into {@code if} instructions. The following code:</p>
     *
     * <pre>
     *     block $2
     *       block $1
     *         ; condition
     *         br_if $1
     *         ; else
     *         br $2
     *       end
     *       ; then
     *     end
     * </pre>
     *
     * <p>is turned into</p>
     *
     * <pre>
     *     ; condition
     *     if
     *       ; then
     *     else
     *       ; else
     *     end
     * </pre>
     *
     * <p>Instead of {@code br $2}, else part may end with any other terminating instruction, which is kept.
     * When a block does not match this pattern, it is turned into {@code if} without else:</p>
     *
     * <pre>
     *     block $1
     *       ; condition
     *       br_if $1
     *       ; body
     *     end
     * </pre>
     *
     * <p>becomes</p>
     *
     * <pre>
     *     ; negated condition
     *     if
     *       ; body
     *     end
     * </pre>
     *
     * <p>Instead of {@code br_if $1}, both patterns recognize {@code if} without else, whose then part ends
     * with {@code br $1}. Code generator produces such instruction when the jump requires assignments
     * to phi variables. In this case the {@code if} itself is reused, so the assignments stay at the beginning
     * of the branch which is taken by the jump.</p>
     *
     * <p>Condition consists of all instructions of the block that precede the jump. They are moved out of
     * the block, so they must leave exactly one value on the stack and must not refer to the block.
     * Blocks are processed from outer to inner, so that the inner block of the first pattern is not turned
     * into {@code if} before the outer block is examined.</p>
     */
    private void reconstructConditionals(WasmInstructionList list) {
        var insn = list.getFirst();
        while (insn != null) {
            if (insn instanceof WasmBlock block) {
                if (!block.isLoop()) {
                    optimizeLastBreak(block.getBody());
                    var replacement = reconstructConditional(block);
                    if (replacement != null) {
                        // Condition may contain blocks as well, so examine the replacement from the beginning
                        insn = replacement;
                        continue;
                    }
                }
                reconstructConditionals(block.getBody());
            } else if (insn instanceof WasmConditional conditional) {
                optimizeLastBreak(conditional.getThenBlock());
                optimizeLastBreak(conditional.getElseBlock());
                reconstructConditionals(conditional.getThenBlock());
                reconstructConditionals(conditional.getElseBlock());
            } else if (insn instanceof WasmTry tryInsn) {
                reconstructConditionals(tryInsn.getBody());
            }
            insn = insn.getNext();
        }
    }

    private void optimizeLastBreak(WasmInstructionList list) {
        var last = list.getLast();
        if (last == null) {
            return;
        }
        if (last instanceof WasmBreak br) {
            if (br.getTarget() == list) {
                removeRef(list, br);
                br.delete();
            } else {
                var beforeLast = last.getPrevious();
                if (beforeLast instanceof WasmBranch beforeBr) {
                    if (beforeBr.getTarget() == list) {
                        removeRef(beforeBr.getTarget(), beforeBr);
                        beforeBr.setTarget(br.getTarget());
                        refs(beforeBr.getTarget()).add(beforeBr);
                        br.delete();
                        removeRef(br.getTarget(), br);
                        WasmInstructionUtil.negate(beforeBr.getPrevious());
                    }
                }
            }
        }
    }

    private WasmInstruction reconstructConditional(WasmBlock block) {
        if (WasmBlockType.inputCount(block.getType()) != 0) {
            return null;
        }
        var result = reconstructIfElse(block);
        if (result == null && WasmBlockType.outputCount(block.getType()) == 0) {
            result = reconstructIf(block);
        }
        return result;
    }

    private WasmInstruction reconstructIfElse(WasmBlock outer) {
        if (!(outer.getBody().getFirst() instanceof WasmBlock inner) || inner.isLoop()
                || WasmBlockType.inputCount(inner.getType()) != 0
                || WasmBlockType.outputCount(inner.getType()) != 0) {
            return null;
        }

        // Jump is the only way to reach 'then' part, otherwise 'then' can't be moved inside 'if'
        var innerRefs = refs(inner.getBody());
        if (innerRefs.size() != 1) {
            return null;
        }
        var jump = asJump(innerRefs.iterator().next(), inner.getBody());
        if (jump == null || !isCondition(inner.getBody().getFirst(), jump)
                || isReferencedFromCondition(outer.getBody(), inner.getBody())) {
            return null;
        }
        var last = inner.getBody().getLast();
        if (last == jump || !last.isTerminating()) {
            return null;
        }

        var conditional = toConditional(jump);
        conditional.setType(outer.getType());
        if (last instanceof WasmBreak br && br.getTarget() == outer.getBody()) {
            removeRef(outer.getBody(), br);
            br.delete();
        }
        moveFollowing(conditional, conditional.getElseBlock());
        inner.delete();
        removeTrailingBreak(outer.getBody());
        conditional.getThenBlock().transferFrom(outer.getBody());

        var first = inner.getBody().getFirst();
        moveBefore(inner.getBody(), outer);
        outer.delete();
        retargetToArms(outer.getBody(), conditional);
        return first;
    }

    private WasmInstruction reconstructIf(WasmBlock block) {
        var body = block.getBody();
        var jump = findJump(body);
        if (jump == null || jump.getNext() == null || !isCondition(body.getFirst(), jump)
                || isReferencedFromCondition(body, body)) {
            return null;
        }

        WasmConditional conditional;
        WasmInstructionList bodyArm;
        if (jump instanceof WasmBranch) {
            negateCondition(jump.getPrevious());
            conditional = toConditional(jump);
            bodyArm = conditional.getThenBlock();
        } else {
            conditional = toConditional(jump);
            bodyArm = conditional.getElseBlock();
        }
        removeTrailingBreak(body);
        moveFollowing(conditional, bodyArm);

        var first = body.getFirst();
        moveBefore(body, block);
        block.delete();
        retargetToArms(body, conditional);
        return first;
    }

    // Jump to the end of the block that is about to be removed is equivalent to falling through
    private void removeTrailingBreak(WasmInstructionList body) {
        if (body.getLast() instanceof WasmBreak br && br.getTarget() == body) {
            removeRef(body, br);
            br.delete();
        }
    }

    /**
     * 'if' with empty then part is longer than 'if' with negated condition, unless negation produces
     * additional instruction, in which case size is the same. This is done after processing both parts,
     * when the condition is already consumed, so instruction preceding 'if' is not touched by the main pass
     * anymore.
     */
    private void swapEmptyThen(WasmConditional conditional) {
        if (!conditional.getThenBlock().isEmpty() || conditional.getElseBlock().isEmpty()
                || conditional.getPrevious() == null) {
            return;
        }
        negateCondition(conditional.getPrevious());
        conditional.getThenBlock().transferFrom(conditional.getElseBlock());
        var refs = branchRefs.remove(conditional.getElseBlock());
        if (refs != null) {
            for (var ref : refs) {
                retarget(ref, conditional.getElseBlock(), conditional.getThenBlock());
            }
        }
    }

    /**
     * Replaces {@code i32.eqz if A else B end} with {@code if B else A end}. Similarly to
     * {@link #swapEmptyThen(WasmConditional)}, it's safe to modify instruction preceding 'if' at this moment.
     */
    private void removeConditionInversion(WasmConditional conditional) {
        if (conditional.getThenBlock().isEmpty() || conditional.getElseBlock().isEmpty()
                || !(conditional.getPrevious() instanceof WasmIntUnary unary)
                || unary.getOperation() != WasmIntUnaryOperation.EQZ || unary.getType() != WasmIntType.INT32) {
            return;
        }
        unary.delete();
        var thenBlock = conditional.getThenBlock();
        var elseBlock = conditional.getElseBlock();
        var tmp = new WasmInstructionList();
        tmp.transferFrom(thenBlock);
        thenBlock.transferFrom(elseBlock);
        elseBlock.transferFrom(tmp);

        var thenRefs = branchRefs.remove(thenBlock);
        var elseRefs = branchRefs.remove(elseBlock);
        if (thenRefs != null) {
            for (var ref : thenRefs) {
                retarget(ref, thenBlock, elseBlock);
            }
        }
        if (elseRefs != null) {
            for (var ref : elseRefs) {
                retarget(ref, elseBlock, thenBlock);
            }
        }
    }

    /**
     * Replaces {@code if br $X end} with {@code br_if $X}. Such instruction appears when code generator
     * emits jump with assignments to phi variables, and all these assignments are removed as identity copies.
     * Since condition is already consumed and then part contains no values, the main pass needs no
     * adjustments.
     */
    private boolean replaceWithBranch(WasmConditional conditional) {
        if (WasmBlockType.inputCount(conditional.getType()) != 0
                || WasmBlockType.outputCount(conditional.getType()) != 0
                || !conditional.getElseBlock().isEmpty()
                || !(conditional.getThenBlock().getFirst() instanceof WasmBreak br)
                || br.getNext() != null || br.getTarget() == conditional.getThenBlock()) {
            return false;
        }
        var branch = new WasmBranch(br.getTarget());
        branch.setLocation(br.getLocation());
        conditional.insertNext(branch);
        conditional.delete();
        removeRef(br.getTarget(), br);
        refs(br.getTarget()).add(branch);
        return true;
    }

    /*
      Replaces condition in form

      if (result i32)
        1|0
      else
        0|1
      end

      with eqz eqz or eqz respectively
     */
    private void checkTrueFalseCondition(WasmConditional conditional) {
        if (conditional.getType() == null || conditional.getType().getOutputTypes().size() != 1
                || conditional.getType().getOutputTypes().get(0) != WasmType.INT32) {
            return;
        }
        if (!isSingleInstruction(conditional.getThenBlock()) || !isSingleInstruction(conditional.getElseBlock())) {
            return;
        }
        if (!(conditional.getThenBlock().getFirst() instanceof WasmInt32Constant thenCst)
                || !(conditional.getElseBlock().getFirst() instanceof WasmInt32Constant elseCst)) {
            return;
        }
        if (thenCst.getValue() == 1 && elseCst.getValue() == 0) {
            WasmInstructionUtil.negate(conditional.getPrevious());
            insertEqzBefore(conditional);
            conditional.delete();
        } else if (thenCst.getValue() == 0 && elseCst.getValue() == 1) {
            WasmInstructionUtil.negate(conditional.getPrevious());
            conditional.delete();
        }
    }

    private boolean isSingleInstruction(WasmInstructionList list) {
        return list.getFirst() != null && list.getLast() == list.getFirst();
    }

    private void insertEqzBefore(WasmInstruction insn) {
        var eqz = new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
        eqz.setLocation(insn.getLocation());
        insn.insertPrevious(eqz);
    }

    /**
     * <p>When both parts of {@code if} end with assignment to the same local (or one of them is terminating),
     * the assignment is moved after {@code if}, which now produces the value:</p>
     *
     * <pre>
     *     if                    if (result T)
     *       ...                   ...
     *       local.set $x   =>   else
     *     else                    ...
     *       ...                 end
     *       local.set $x        local.set $x
     *     end
     * </pre>
     *
     * <p>Both parts are already processed by the main pass in their own frames, so writes made in them can't
     * be paired with reads anymore and removing {@code local.set} from them is safe. The returned
     * {@code local.set} must be processed by the main pass, so it replaces these writes in
     * {@link #writesByLocal} and can be turned into {@code local.tee} or removed.</p>
     */
    private WasmSetLocal hoistSet(WasmConditional conditional) {
        if (conditional.getType() != null || conditional.getThenBlock().isEmpty()
                || conditional.getElseBlock().isEmpty()) {
            return null;
        }
        // Branches to the end of 'if' would have to provide the value
        if (!refs(conditional.getThenBlock()).isEmpty() || !refs(conditional.getElseBlock()).isEmpty()) {
            return null;
        }
        var thenLast = conditional.getThenBlock().getLast();
        var elseLast = conditional.getElseBlock().getLast();
        var thenSet = thenLast instanceof WasmSetLocal set ? set : null;
        var elseSet = elseLast instanceof WasmSetLocal set ? set : null;
        if (thenSet == null && elseSet == null) {
            return null;
        }
        if (thenSet != null && elseSet != null) {
            if (thenSet.getLocal() != elseSet.getLocal()) {
                return null;
            }
        } else if (thenSet == null && !thenLast.isTerminating() || elseSet == null && !elseLast.isTerminating()) {
            return null;
        }

        var local = thenSet != null ? thenSet.getLocal() : elseSet.getLocal();
        var result = new WasmSetLocal(local);
        result.setLocation(thenSet != null ? thenSet.getLocation() : elseSet.getLocation());
        conditional.setType(local.getType().asBlock());
        if (thenSet != null) {
            thenSet.delete();
        }
        if (elseSet != null) {
            elseSet.delete();
        }
        conditional.insertNext(result);
        checkTrueFalseCondition(conditional);
        return result;
    }

    private WasmInstruction findJump(WasmInstructionList list) {
        for (var insn = list.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof WasmBranch branch && branch.getTarget() == list) {
                return insn;
            }
            if (insn instanceof WasmConditional conditional && isJumpConditional(conditional)
                    && ((WasmBreak) conditional.getThenBlock().getLast()).getTarget() == list) {
                return insn;
            }
            if (insn.isTerminating()) {
                break;
            }
        }
        return null;
    }

    private WasmInstruction asJump(WasmInstruction ref, WasmInstructionList list) {
        if (ref instanceof WasmBranch) {
            return ref.getOwner() == list ? ref : null;
        }
        if (ref instanceof WasmBreak && ref.getNext() == null
                && ref.getOwner().getBreakTarget() instanceof WasmConditional conditional
                && conditional.getOwner() == list && conditional.getThenBlock() == ref.getOwner()
                && isJumpConditional(conditional)) {
            return conditional;
        }
        return null;
    }

    private boolean isJumpConditional(WasmConditional conditional) {
        return WasmBlockType.inputCount(conditional.getType()) == 0
                && WasmBlockType.outputCount(conditional.getType()) == 0
                && conditional.getElseBlock().isEmpty()
                && conditional.getThenBlock().getLast() instanceof WasmBreak
                && refs(conditional.getThenBlock()).isEmpty();
    }

    private boolean isCondition(WasmInstruction first, WasmInstruction jump) {
        conditionInstructions.clear();
        var stack = conditionTypeInference.typeStack;
        stack.clear();
        for (var insn = first; insn != jump; insn = insn.getNext()) {
            if (insn.isTerminating()) {
                return false;
            }
            insn.acceptVisitor(conditionTypeInference);
            conditionInstructions.add(insn);
        }
        return stack.size() == 1 && stack.get(0) == WasmType.INT32;
    }

    private boolean isReferencedFromCondition(WasmInstructionList target, WasmInstructionList list) {
        for (var insn : refs(target)) {
            while (insn != null && insn.getOwner() != list) {
                insn = insn.getOwner().getBreakTarget();
            }
            if (insn != null && conditionInstructions.contains(insn)) {
                return true;
            }
        }
        for (var ref : catchRefs(target)) {
            WasmInstruction insn = ref.getOwner();
            while (insn != null && insn.getOwner() != list) {
                insn = insn.getOwner().getBreakTarget();
            }
            if (insn != null && conditionInstructions.contains(insn)) {
                return true;
            }
        }
        return false;
    }

    private void negateCondition(WasmInstruction insn) {
        if (insn instanceof WasmIntUnary unary && unary.getOperation() == WasmIntUnaryOperation.EQZ
                && unary.getType() != WasmIntType.INT32) {
            var negated = new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ);
            negated.setLocation(insn.getLocation());
            insn.insertNext(negated);
        } else if (insn instanceof WasmIntBinary binary && binary.getType() == WasmIntType.INT32
                && binary.getOperation() == WasmIntBinaryOperation.SUB) {
            // Code generator uses i32.sub to compare for inequality
            binary.setOperation(WasmIntBinaryOperation.EQ);
        } else {
            WasmInstructionUtil.negate(insn);
        }
    }

    private WasmConditional toConditional(WasmInstruction jump) {
        if (jump instanceof WasmConditional conditional) {
            var br = (WasmBreak) conditional.getThenBlock().getLast();
            removeRef(br.getTarget(), br);
            br.delete();
            return conditional;
        }
        var conditional = new WasmConditional();
        conditional.setLocation(jump.getLocation());
        jump.insertNext(conditional);
        removeRef(((WasmBranch) jump).getTarget(), jump);
        jump.delete();
        return conditional;
    }

    private static void moveFollowing(WasmInstruction insn, WasmInstructionList target) {
        while (insn.getNext() != null) {
            var next = insn.getNext();
            next.delete();
            target.add(next);
        }
    }

    private static void moveBefore(WasmInstructionList list, WasmInstruction anchor) {
        while (list.getFirst() != null) {
            var insn = list.getFirst();
            insn.delete();
            anchor.insertPrevious(insn);
        }
    }

    private Set<WasmInstruction> refs(WasmInstructionList target) {
        return branchRefs.computeIfAbsent(target, k -> new LinkedHashSet<>());
    }

    private List<WasmCatchClause> catchRefs(WasmInstructionList target) {
        return branchCatchRefs.computeIfAbsent(target, k -> new ArrayList<>());
    }

    private void removeRef(WasmInstructionList target, WasmInstruction insn) {
        var set = branchRefs.get(target);
        if (set != null) {
            set.remove(insn);
        }
    }

    private void retargetToArms(WasmInstructionList target, WasmConditional conditional) {
        var refs = branchRefs.remove(target);
        if (refs != null) {
            for (var ref : refs) {
                var insn = ref;
                if (insn.getOwner() == null) {
                    continue;
                }
                while (insn.getOwner() != conditional.getThenBlock() && insn.getOwner() != conditional.getElseBlock()) {
                    insn = insn.getOwner().getBreakTarget();
                }
                retarget(ref, target, insn.getOwner());
            }
        }
        var catchRefs = branchCatchRefs.remove(target);
        if (catchRefs != null) {
            for (var ref : catchRefs) {
                WasmInstruction insn = ref.getOwner();
                if (insn.getOwner() == null) {
                    continue;
                }
                while (insn.getOwner() != conditional.getThenBlock() && insn.getOwner() != conditional.getElseBlock()) {
                    insn = insn.getOwner().getBreakTarget();
                }
                retarget(ref, insn.getOwner());
            }
        }
    }

    private void retarget(WasmInstruction ref, WasmInstructionList from, WasmInstructionList to) {
        if (ref instanceof WasmBranch branch) {
            branch.setTarget(to);
        } else if (ref instanceof WasmBreak br) {
            br.setTarget(to);
        } else if (ref instanceof WasmNullBranch branch) {
            branch.setTarget(to);
        } else if (ref instanceof WasmCastBranch branch) {
            branch.setTarget(to);
        } else if (ref instanceof WasmSwitch switchInsn) {
            switchInsn.getTargets().replaceAll(t -> t == from ? to : t);
            if (switchInsn.getDefaultTarget() == from) {
                switchInsn.setDefaultTarget(to);
            }
        }
        refs(to).add(ref);
    }

    private void retarget(WasmCatchClause ref, WasmInstructionList to) {
        ref.setTarget(to);
        catchRefs(to).add(ref);
    }

    /**
     * Processes the list in a new frame of the simulated stack. Values that were on the stack before the list
     * are not visible, except for {@code inputCount} block parameters. Instructions that compute parameters
     * are outside of the list, so parameters can't be moved and are pushed with unknown first instruction.
     */
    private void processList(WasmInstructionList list, int inputCount) {
        arrayConstructionOptimizer.process(list, catchingTryDepth > 0);

        var savedStackTop = stackTop;
        stackTop = new StackEntry(list, null);
        for (var i = 0; i < inputCount; ++i) {
            stackTop = new StackEntry(list, stackTop);
        }
        for (var insn : list) {
            insn.acceptVisitor(optimizingVisitor);
        }
        stackTop = savedStackTop;
    }

    private int branchArity(WasmInstructionList target) {
        var owner = target.getBreakTarget();
        if (owner == null) {
            return function.getType().getReturnTypes().size();
        }
        if (owner instanceof WasmBlock block) {
            var type = block.getType();
            if (type == null) {
                return 0;
            }
            return block.isLoop() ? type.getInputTypes().size() : type.getOutputTypes().size();
        }
        if (owner instanceof WasmConditional) {
            return WasmBlockType.outputCount(((WasmConditional) owner).getType());
        }
        if (owner instanceof WasmTry) {
            return ((WasmTry) owner).getType() != null ? 1 : 0;
        }
        throw new IllegalArgumentException("Unknown branch target: " + owner.getClass().getName());
    }

    /**
     * Consumes {@code count} values.
     *
     * @return first instruction of computation of the deepest consumed value, or {@code instruction}
     *     if nothing is consumed.
     */
    private WasmInstruction pop(int count, WasmInstruction instruction) {
        for (var i = 0; i < count; ++i) {
            instruction = pop();
        }
        return instruction;
    }

    /**
     * Consumes the top value. Writes attached to it are discarded, since their values would reside among
     * consumed values.
     *
     * @return first instruction of computation of the consumed value, or {@code null} if unknown.
     */
    private WasmInstruction pop() {
        for (var write = stackTop.lastWrite; write != null; write = write.prev) {
            writesByLocal[write.instruction.getLocal().getIndex()] = null;
        }
        var instruction = stackTop.firstInstruction;
        stackTop = stackTop.next;
        return instruction;
    }

    private void push(int count) {
        for (var i = 0; i < count; ++i) {
            push();
        }
    }

    private void push(WasmInstruction instruction, WasmBlockType type) {
        if (type == null) {
            return;
        }
        if (type.getOutputTypes().size() == 1) {
            pushResult(instruction);
        } else {
            push(type.getOutputTypes().size());
        }
    }

    private void push() {
        stackTop = new StackEntry(stackTop.list, stackTop);
    }

    private void pushResult(WasmInstruction instruction) {
        stackTop = new StackEntry(stackTop.list, stackTop);
        stackTop.firstInstruction = instruction;
    }

    private void transform(int inputs, int outputs, WasmInstruction instruction) {
        instruction = pop(inputs, instruction);
        if (outputs == 1) {
            pushResult(instruction);
        } else {
            push(outputs);
        }
    }

    private void drainStack() {
        while (stackTop.next != null) {
            pop();
        }
    }

    /**
     * Pushes value read by {@code local.get} and tries to take it from the most recent write of the local,
     * turning the write into {@code local.tee}.
     *
     * @return {@code true} if the value is taken from the write, so the caller must remove {@code local.get}.
     */
    private boolean readLocal(WasmGetLocal get) {
        var below = stackTop;
        pushResult(get);
        var write = writesByLocal[get.getLocal().getIndex()];
        if (write == null) {
            return false;
        }
        if (write.owner == below) {
            splitWrites(write, stackTop);
            replaceWithTee(write.instruction);
        } else if (write.owner.list != below.list || !tryReschedule(write, get)) {
            return false;
        }
        stackTop.firstInstruction = write.startInstruction;
        return true;
    }

    /**
     * <p>Moves computation of the written value along with the write right before the read, and turns the write
     * into {@code local.tee}:</p>
     *
     * <pre>
     *     ; compute a              ; push b
     *     local.set 1              ; compute a
     *     ; push b           =>    local.tee 1
     *     local.get 1
     * </pre>
     *
     * <p><em>Moved</em> range consists of computation of {@code a} and {@code local.set}, <em>crossed</em>
     * range consists of instructions between {@code local.set} and {@code local.get}. Owner of the write
     * is still on the stack, so crossed range only pushes values above it, and computation of {@code a}
     * consumes nothing that was on the stack before it. Therefore, order of values on the stack does not change.
     * Ranges can be swapped when:</p>
     *
     * <ul>
     *   <li>they don't both have barriers (side effects, traps, exits), so their relative order is not
     *     observable;</li>
     *   <li>crossed range can't exit, since moved range always writes a local, which can be read after exit;</li>
     *   <li>moved range can't exit, if crossed range writes locals, since these writes would happen before
     *     the exit;</li>
     *   <li>crossed range does not access locals written by moved range and does not write locals read
     *     by moved range.</li>
     * </ul>
     *
     * <p>Exit is either a branch out of the range or an exception that can be caught in this function.</p>
     *
     * @param write write which is in the same list as {@code get}.
     */
    private boolean tryReschedule(Write write, WasmGetLocal get) {
        if (write.startInstruction == null) {
            return false;
        }

        moveAnalysis.reset();
        for (var insn = write.startInstruction; insn != write.instruction; insn = insn.getNext()) {
            moveAnalysis.visitDefault(insn);
        }
        write.instruction.acceptVisitor(moveAnalysis);

        crossAnalysis.reset();
        for (var insn = write.instruction.getNext(); insn != get; insn = insn.getNext()) {
            crossAnalysis.visitDefault(insn);
        }

        if (crossAnalysis.hasBarriers && moveAnalysis.hasBarriers) {
            return false;
        }
        if (canExit(crossAnalysis) || canExit(moveAnalysis) && !crossAnalysis.setLocals.isEmpty()) {
            return false;
        }
        if (crossAnalysis.getLocals.intersects(moveAnalysis.setLocals)
                || crossAnalysis.setLocals.intersects(moveAnalysis.getLocals)
                || crossAnalysis.setLocals.intersects(moveAnalysis.setLocals)) {
            return false;
        }

        var instructionToMove = write.startInstruction;
        while (true) {
            var next = instructionToMove.getNext();
            instructionToMove.delete();
            get.insertPrevious(instructionToMove);
            if (instructionToMove == write.instruction) {
                break;
            }
            instructionToMove = next;
        }
        removeWrite(write);
        replaceWithTee(write.instruction);
        return true;
    }

    private boolean canExit(WasmReschedulingAnalysis analysis) {
        return analysis.hasExits || catchingTryDepth > 0 && analysis.mayThrow;
    }

    private void replaceWithTee(WasmSetLocal set) {
        var tee = new WasmTeeLocal(set.getLocal());
        tee.setLocation(set.getLocation());
        set.insertNext(tee);
        set.delete();
    }

    /**
     * Removes the write from its owner. Writes that were attached to the owner after this one are re-attached
     * to {@code newOwner}, which is the value kept on the stack by this write, since now they happen above it.
     */
    private void splitWrites(Write write, StackEntry newOwner) {
        var owner = write.owner;
        var later = owner.lastWrite;
        if (later != write) {
            newOwner.lastWrite = later;
            while (later.prev != write) {
                later.owner = newOwner;
                later = later.prev;
            }
            later.owner = newOwner;
            later.prev = null;
        }
        owner.lastWrite = write.prev;
        writesByLocal[write.instruction.getLocal().getIndex()] = null;
    }

    private void removeWrite(Write write) {
        var owner = write.owner;
        if (owner.lastWrite == write) {
            owner.lastWrite = write.prev;
        } else {
            var later = owner.lastWrite;
            while (later.prev != write) {
                later = later.prev;
            }
            later.prev = write.prev;
        }
        writesByLocal[write.instruction.getLocal().getIndex()] = null;
    }

    /**
     * Value on the simulated operand stack. Bottom of each frame is represented by an entry without value,
     * so that writes can be attached to it.
     */
    private static class StackEntry {
        // List of the frame, writes are paired with reads only within the same list
        final WasmInstructionList list;
        // Entry below this one, null for the bottom of the frame
        final StackEntry next;
        // Writes that happened while this entry was on top of the stack, the most recent first
        Write lastWrite;
        /*
         * First instruction of the computation of the value, null if unknown. Instructions starting from this one
         * up to the instruction that pushes the value are contiguous, belong to the frame's list and consume
         * nothing that was on the stack before them. Writes made among these instructions are already
         * discarded or removed, except for writes attached to this entry.
         */
        WasmInstruction firstInstruction;

        StackEntry(WasmInstructionList list, StackEntry next) {
            this.list = list;
            this.next = next;
        }
    }

    /**
     * {@code local.set} that still can be turned into {@code local.tee}. Write is reachable from
     * {@link StackEntry#lastWrite} of its owner, and its owner is either on the stack, or belongs to a frame
     * that is already processed. Write is in {@link #writesByLocal} until it's discarded or removed.
     */
    private static class Write {
        // Entry that was on top of the stack when the write happened
        StackEntry owner;
        // Previous write attached to the same owner
        Write prev;
        final WasmSetLocal instruction;
        // First instruction of the computation of the written value, see StackEntry.firstInstruction
        final WasmInstruction startInstruction;

        Write(StackEntry owner, Write prev, WasmSetLocal instruction, WasmInstruction startInstruction) {
            this.owner = owner;
            this.prev = prev;
            this.instruction = instruction;
            this.startInstruction = startInstruction;
        }
    }

    private class BranchRefCollector extends WasmDefaultInstructionVisitor {
        @Override
        public void visit(WasmBranch instruction) {
            addRef(instruction.getTarget(), instruction);
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            addRef(instruction.getTarget(), instruction);
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            addRef(instruction.getTarget(), instruction);
        }

        @Override
        public void visit(WasmBreak instruction) {
            addRef(instruction.getTarget(), instruction);
        }

        @Override
        public void visit(WasmSwitch instruction) {
            for (var target : instruction.getTargets()) {
                addRef(target, instruction);
            }
            addRef(instruction.getDefaultTarget(), instruction);
        }

        @Override
        public void visit(WasmTry instruction) {
            super.visit(instruction);
            for (var clause : instruction.getCatches()) {
                catchRefs(clause.getTarget()).add(clause);
            }
        }

        private void addRef(WasmInstructionList target, WasmInstruction insn) {
            refs(target).add(insn);
        }
    }

    private final WasmInstructionVisitor optimizingVisitor = new WasmInstructionVisitor() {
        @Override
        public void visit(WasmUnreachable instruction) {
            drainStack();
        }

        @Override
        public void visit(WasmBlock instruction) {
            var inputs = WasmBlockType.inputCount(instruction.getType());
            var startInsn = pop(inputs, instruction);
            processList(instruction.getBody(), inputs);
            push(startInsn, instruction.getType());
        }

        @Override
        public void visit(WasmConditional instruction) {
            var inputs = WasmBlockType.inputCount(instruction.getType());
            var startInsn = pop(1 + inputs, instruction);
            processList(instruction.getThenBlock(), inputs);
            processList(instruction.getElseBlock(), inputs);
            swapEmptyThen(instruction);
            removeConditionInversion(instruction);
            if (replaceWithBranch(instruction)) {
                return;
            }
            var set = hoistSet(instruction);
            push(startInsn, instruction.getType());
            if (set != null) {
                visit(set);
            }
        }

        @Override
        public void visit(WasmBranch instruction) {
            var arity = branchArity(instruction.getTarget());
            transform(arity + 1, arity, instruction);
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            var arity = branchArity(instruction.getTarget());
            if (instruction.getCondition() == WasmNullCondition.NULL) {
                transform(arity + 1, arity + 1, instruction);
            } else {
                transform(arity, arity - 1, instruction);
            }
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            var arity = branchArity(instruction.getTarget());
            transform(arity, arity, instruction);
        }

        @Override
        public void visit(WasmBreak instruction) {
            drainStack();
        }

        @Override
        public void visit(WasmSwitch instruction) {
            drainStack();
        }

        @Override
        public void visit(WasmReturn instruction) {
            drainStack();
        }

        @Override
        public void visit(WasmInt32Constant instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmInt64Constant instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmFloat32Constant instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmFloat64Constant instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmNullConstant instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmIsNull instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmGetLocal instruction) {
            if (readLocal(instruction)) {
                instruction.delete();
            }
        }

        @Override
        public void visit(WasmSetLocal instruction) {
            var startInstruction = pop();
            var existingWrite = writesByLocal[instruction.getLocal().getIndex()];
            if (existingWrite != null) {
                removeWrite(existingWrite);
            }
            var write = new Write(stackTop, stackTop.lastWrite, instruction, startInstruction);
            stackTop.lastWrite = write;
            writesByLocal[instruction.getLocal().getIndex()] = write;
        }

        @Override
        public void visit(WasmTeeLocal instruction) {
            // Previous write of the local is not the most recent anymore
            var insn = pop();
            var write = writesByLocal[instruction.getLocal().getIndex()];
            if (write != null) {
                removeWrite(write);
            }
            pushResult(insn);
        }

        @Override
        public void visit(WasmGetGlobal instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmSetGlobal instruction) {
            pop();
        }

        @Override
        public void visit(WasmIntBinary instruction) {
            transform(2, 1, instruction);
        }

        @Override
        public void visit(WasmFloatBinary instruction) {
            transform(2, 1, instruction);
        }

        @Override
        public void visit(WasmIntUnary instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmFloatUnary instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmConversion instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmCall instruction) {
            var type = instruction.getFunction().getType();
            transform(type.getParameterTypes().size(), type.getReturnTypes().size(), instruction);
        }

        @Override
        public void visit(WasmIndirectCall instruction) {
            var type = instruction.getType();
            transform(type.getParameterTypes().size() + 1, type.getReturnTypes().size(), instruction);
        }

        @Override
        public void visit(WasmCallReference instruction) {
            var type = instruction.getType();
            transform(type.getParameterTypes().size() + 1, type.getReturnTypes().size(), instruction);
        }

        @Override
        public void visit(WasmDrop instruction) {
            pop();
        }

        @Override
        public void visit(WasmLoadInt32 instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmLoadInt64 instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmLoadFloat32 instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmLoadFloat64 instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmStoreInt32 instruction) {
            pop(2, instruction);
        }

        @Override
        public void visit(WasmStoreInt64 instruction) {
            pop(2, instruction);
        }

        @Override
        public void visit(WasmStoreFloat32 instruction) {
            pop(2, instruction);
        }

        @Override
        public void visit(WasmStoreFloat64 instruction) {
            pop(2, instruction);
        }

        @Override
        public void visit(WasmMemoryGrow instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmFill instruction) {
            pop(3, instruction);
        }

        @Override
        public void visit(WasmCopy instruction) {
            pop(3, instruction);
        }

        @Override
        public void visit(WasmTry instruction) {
            var catching = !instruction.getCatches().isEmpty();
            if (catching) {
                catchingTryDepth++;
            }
            processList(instruction.getBody(), 0);
            if (catching) {
                catchingTryDepth--;
            }
            if (instruction.getType() != null) {
                pushResult(instruction);
            }
        }

        @Override
        public void visit(WasmThrow instruction) {
            drainStack();
        }

        @Override
        public void visit(WasmReferencesEqual instruction) {
            transform(2, 1, instruction);
        }

        @Override
        public void visit(WasmCast instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmTest instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmExternConversion instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmStructNew instruction) {
            transform(instruction.getType().getFields().size(), 1, instruction);
        }

        @Override
        public void visit(WasmStructNewDefault instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmStructGet instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmStructSet instruction) {
            pop(2, instruction);
        }

        @Override
        public void visit(WasmArrayNewDefault instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmArrayNewFixed instruction) {
            transform(instruction.getSize(), 1, instruction);
        }

        @Override
        public void visit(WasmArrayGet instruction) {
            transform(2, 1, instruction);
        }

        @Override
        public void visit(WasmArraySet instruction) {
            pop(3, instruction);
        }

        @Override
        public void visit(WasmArrayLength instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmArrayCopy instruction) {
            pop(5, instruction);
        }

        @Override
        public void visit(WasmFunctionReference instruction) {
            pushResult(instruction);
        }

        @Override
        public void visit(WasmInt31Reference instruction) {
            transform(1, 1, instruction);
        }

        @Override
        public void visit(WasmInt31Get instruction) {
            transform(1, 1, instruction);
        }
    };
}
