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
 * <p>Each instruction list is walked from first to last instruction, simulating operand stack.
 * Along with the operand stack, a stack of <em>spills</em> is maintained. Every {@code local.set} introduces
 * a spill, i.e. a value that could have stayed on operand stack, if {@code local.set} was replaced by
 * {@code local.tee}. Values and spills are ordered by their {@link Node#position}, which reflects
 * where a spilled value would reside relatively to actual values, if it was kept on stack.</p>
 *
 * <p>Every {@code local.get} pushes a value which refers to the spill produced by the most recent
 * {@code local.set} of the same local. When an instruction consumes values, they are examined from
 * the deepest to the topmost one. If a value was pushed by {@code local.get} and its spill would reside
 * exactly at the place of the value, the spill becomes a {@code local.tee} and the {@code local.get}
 * is removed. Finally, all spills that would reside among consumed values are discarded.</p>
 *
 * <p>{@code local.tee} is always kept, even if the removed {@code local.get} was the only read of the value,
 * since the value may be read elsewhere, including targets of branches and exception handlers.
 * {@link WasmDeadStoreOptimizer} decides this precisely, using liveness computed after this pass.</p>
 *
 * <p>Additionally, identity copies, i.e. {@code local.get X ... local.set X} with no writes to {@code X}
 * in between, are removed, see {@link #handleIdentityCopy(WasmSetLocal)}.</p>
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
    private Spill[] lastSpills;
    private int[] definitionCounts;
    private final List<Value> operands = new ArrayList<>();
    private final InstructionVisitor visitor = new InstructionVisitor();
    private int nextPosition;
    private int frameStart;
    private Value top;
    private Spill topSpill;
    private final Map<WasmInstructionList, List<BranchRef>> branchRefs = new HashMap<>();
    private final WasmTypeInference conditionTypeInference = new WasmTypeInference();
    private final Set<WasmInstruction> conditionInstructions = new HashSet<>();
    private final WasmArrayConstructionOptimizer arrayConstructionOptimizer = new WasmArrayConstructionOptimizer();
    private int catchingTryDepth;

    public void optimize(WasmFunction function) {
        this.function = function;
        new BranchRefCollector().visitMany(function.getBody());
        reconstructConditionals(function.getBody());

        lastSpills = new Spill[function.getLocalVariables().size()];
        definitionCounts = new int[function.getLocalVariables().size()];
        nextPosition = 0;
        processList(function.getBody());
        branchRefs.clear();
        this.function = null;
        lastSpills = null;
        definitionCounts = null;
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
                    var replacement = reconstructConditional(block);
                    if (replacement != null) {
                        // Condition may contain blocks as well, so examine the replacement from the beginning
                        insn = replacement;
                        continue;
                    }
                }
                reconstructConditionals(block.getBody());
            } else if (insn instanceof WasmConditional conditional) {
                reconstructConditionals(conditional.getThenBlock());
                reconstructConditionals(conditional.getElseBlock());
            } else if (insn instanceof WasmTry tryInsn) {
                reconstructConditionals(tryInsn.getBody());
            }
            insn = insn.getNext();
        }
    }

    private WasmInstruction reconstructConditional(WasmBlock block) {
        if (inputCount(block.getType()) != 0) {
            return null;
        }
        var result = reconstructIfElse(block);
        if (result == null && outputCount(block.getType()) == 0) {
            result = reconstructIf(block);
        }
        return result;
    }

    private WasmInstruction reconstructIfElse(WasmBlock outer) {
        if (!(outer.getBody().getFirst() instanceof WasmBlock inner) || inner.isLoop()
                || inputCount(inner.getType()) != 0 || outputCount(inner.getType()) != 0) {
            return null;
        }

        // Jump is the only way to reach 'then' part, otherwise 'then' can't be moved inside 'if'
        var innerRefs = refs(inner.getBody());
        if (innerRefs.size() != 1) {
            return null;
        }
        var jump = asJump(innerRefs.get(0).insn, inner.getBody());
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
     * since then part often contains identity copies, which are only removed by the main pass.
     * The condition is already consumed at this moment, so instruction preceding 'if' is not touched
     * by the main pass anymore.
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
        if (inputCount(conditional.getType()) != 0 || outputCount(conditional.getType()) != 0
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
        refs(br.getTarget()).add(new BranchRef(branch, null));
        return true;
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
     * <p>Both parts are already processed by the main pass, so their spills are discarded and removing
     * {@code local.set} from them is safe. The returned {@code local.set} must be processed
     * by the main pass, so it can be turned into {@code local.tee} or removed.</p>
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
        return inputCount(conditional.getType()) == 0 && outputCount(conditional.getType()) == 0
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
        for (var ref : refs(target)) {
            var insn = ref.insn;
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

    private List<BranchRef> refs(WasmInstructionList target) {
        return branchRefs.computeIfAbsent(target, k -> new ArrayList<>());
    }

    private void removeRef(WasmInstructionList target, WasmInstruction insn) {
        refs(target).removeIf(ref -> ref.insn == insn);
    }

    private void retargetToArms(WasmInstructionList target, WasmConditional conditional) {
        var refs = branchRefs.remove(target);
        if (refs == null) {
            return;
        }
        for (var ref : refs) {
            var insn = ref.insn;
            while (insn.getOwner() != conditional.getThenBlock() && insn.getOwner() != conditional.getElseBlock()) {
                insn = insn.getOwner().getBreakTarget();
            }
            retarget(ref, target, insn.getOwner());
        }
    }

    private void retarget(BranchRef ref, WasmInstructionList from, WasmInstructionList to) {
        if (ref.clause != null) {
            ref.clause.setTarget(to);
        } else if (ref.insn instanceof WasmBranch branch) {
            branch.setTarget(to);
        } else if (ref.insn instanceof WasmBreak br) {
            br.setTarget(to);
        } else if (ref.insn instanceof WasmNullBranch branch) {
            branch.setTarget(to);
        } else if (ref.insn instanceof WasmCastBranch branch) {
            branch.setTarget(to);
        } else if (ref.insn instanceof WasmSwitch switchInsn) {
            switchInsn.getTargets().replaceAll(t -> t == from ? to : t);
            if (switchInsn.getDefaultTarget() == from) {
                switchInsn.setDefaultTarget(to);
            }
        }
        refs(to).add(ref);
    }

    private void processList(WasmInstructionList list) {
        arrayConstructionOptimizer.process(list, catchingTryDepth > 0);
        var savedTop = top;
        var savedTopSpill = topSpill;
        var savedFrameStart = frameStart;
        top = null;
        topSpill = null;
        frameStart = nextPosition;
        var insn = list.getFirst();
        while (insn != null) {
            var next = insn.getNext();
            insn.acceptVisitor(visitor);
            insn = next;
        }
        // Falling through the end of the list consumes its results
        consume(fallThroughArity(list));
        discardSpills(frameStart);

        top = savedTop;
        topSpill = savedTopSpill;
        frameStart = savedFrameStart;
    }

    /**
     * <p>Handles {@code local.set} that writes to a local the value that was read from the same local, while
     * the local was not written since then. Such copies appear after coalescing of locals.</p>
     *
     * <p>Identity copy is turned into a special spill, positioned where the value was read. If a later
     * {@code local.get} is kept on stack, both {@code local.set} and the later {@code local.get} are removed,
     * so the value read by the original {@code local.get} stays on stack. Otherwise, when the spill is
     * discarded, both {@code local.get} and {@code local.set} are removed. In both cases, the spill that
     * was the last one for the local before the copy becomes the last one again, since the copy does not
     * change the value of the local.</p>
     */
    private boolean handleIdentityCopy(WasmSetLocal set) {
        var value = top;
        if (value == null || value.copyOf == null) {
            return false;
        }
        var index = set.getLocal().getIndex();
        if (value.copyOf.getLocal() != set.getLocal() || value.definitionCount != definitionCounts[index]) {
            return false;
        }
        top = value.previous;
        discardSpills(value.position);
        var spill = new Spill(value.position, topSpill, set);
        spill.identityGet = value.copyOf;
        spill.shadowed = lastSpills[index];
        topSpill = spill;
        lastSpills[index] = spill;
        return true;
    }

    private void retire(Spill spill) {
        spill.alive = false;
        if (spill.identityGet != null) {
            var index = spill.set.getLocal().getIndex();
            if (lastSpills[index] == spill) {
                lastSpills[index] = spill.shadowed;
            }
        }
    }

    private void consume(int count) {
        var value = top;
        for (var i = 0; i < count && value != null; ++i) {
            operands.add(value);
            value = value.previous;
        }
        if (operands.isEmpty()) {
            return;
        }
        top = value;

        // Position of the last thing that stays below the value being examined, either an actual value or
        // a spill that is about to be turned into local.tee
        var bottomPosition = value != null ? value.position : -1;
        var firstConsumedPosition = 0;
        for (var i = operands.size() - 1; i >= 0; --i) {
            var operand = operands.get(i);
            var spill = operand.spill;
            if (spill != null && spill.alive && spill.position > bottomPosition) {
                keepOnStack(spill, operand);
                bottomPosition = spill.position;
            } else {
                bottomPosition = operand.position;
            }
            if (i == operands.size() - 1) {
                firstConsumedPosition = bottomPosition;
            }
        }
        operands.clear();

        discardSpills(firstConsumedPosition);
    }

    private void keepOnStack(Spill spill, Value value) {
        var set = spill.set;
        if (spill.identityGet != null) {
            set.delete();
            value.get.delete();
            retire(spill);
            return;
        }
        var tee = new WasmTeeLocal(set.getLocal());
        tee.setLocation(set.getLocation());
        set.insertNext(tee);
        set.delete();
        value.get.delete();
        spill.alive = false;
    }

    private void discardSpills(int position) {
        while (topSpill != null && topSpill.position >= position) {
            if (topSpill.alive && topSpill.identityGet != null) {
                topSpill.identityGet.delete();
                topSpill.set.delete();
            }
            retire(topSpill);
            topSpill = topSpill.previous;
        }
    }

    private void consumeAll() {
        consume(Integer.MAX_VALUE);
        discardSpills(frameStart);
    }

    private Value push() {
        var value = new Value(nextPosition++, top);
        top = value;
        return value;
    }

    private void push(int count) {
        for (var i = 0; i < count; ++i) {
            push();
        }
    }

    private void transform(int consumed, int produced) {
        consume(consumed);
        push(produced);
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
            return outputCount(((WasmConditional) owner).getType());
        }
        if (owner instanceof WasmTry) {
            return ((WasmTry) owner).getType() != null ? 1 : 0;
        }
        throw new IllegalArgumentException("Unknown branch target: " + owner.getClass().getName());
    }

    private int fallThroughArity(WasmInstructionList list) {
        var owner = list.getBreakTarget();
        if (owner instanceof WasmBlock block) {
            return outputCount(block.getType());
        }
        return branchArity(list);
    }

    private static int inputCount(WasmBlockType type) {
        return type != null ? type.getInputTypes().size() : 0;
    }

    private static int outputCount(WasmBlockType type) {
        return type != null ? type.getOutputTypes().size() : 0;
    }

    private static abstract class Node {
        final int position;

        Node(int position) {
            this.position = position;
        }
    }

    private static class Value extends Node {
        final Value previous;
        WasmGetLocal get;
        WasmGetLocal copyOf;
        int definitionCount;
        Spill spill;

        Value(int position, Value previous) {
            super(position);
            this.previous = previous;
        }
    }

    private static class Spill extends Node {
        final Spill previous;
        final WasmSetLocal set;
        boolean alive = true;
        WasmGetLocal identityGet;
        Spill shadowed;

        Spill(int position, Spill previous, WasmSetLocal set) {
            super(position);
            this.previous = previous;
            this.set = set;
        }
    }

    private static class BranchRef {
        final WasmInstruction insn;
        final WasmCatchClause clause;

        BranchRef(WasmInstruction insn, WasmCatchClause clause) {
            this.insn = insn;
            this.clause = clause;
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
                refs(clause.getTarget()).add(new BranchRef(instruction, clause));
            }
        }

        private void addRef(WasmInstructionList target, WasmInstruction insn) {
            refs(target).add(new BranchRef(insn, null));
        }
    }

    private class InstructionVisitor implements WasmInstructionVisitor {
        @Override
        public void visit(WasmUnreachable instruction) {
            consumeAll();
        }

        @Override
        public void visit(WasmBlock instruction) {
            consume(inputCount(instruction.getType()));
            processList(instruction.getBody());
            push(outputCount(instruction.getType()));
        }

        @Override
        public void visit(WasmConditional instruction) {
            consume(1 + inputCount(instruction.getType()));
            processList(instruction.getThenBlock());
            processList(instruction.getElseBlock());
            swapEmptyThen(instruction);
            removeConditionInversion(instruction);
            if (replaceWithBranch(instruction)) {
                return;
            }
            var set = hoistSet(instruction);
            push(outputCount(instruction.getType()));
            if (set != null) {
                visit(set);
            }
        }

        @Override
        public void visit(WasmBranch instruction) {
            var arity = branchArity(instruction.getTarget());
            transform(arity + 1, arity);
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            var arity = branchArity(instruction.getTarget());
            if (instruction.getCondition() == WasmNullCondition.NULL) {
                transform(arity + 1, arity + 1);
            } else {
                transform(arity, arity - 1);
            }
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            var arity = branchArity(instruction.getTarget());
            transform(arity, arity);
        }

        @Override
        public void visit(WasmBreak instruction) {
            consumeAll();
        }

        @Override
        public void visit(WasmSwitch instruction) {
            consumeAll();
        }

        @Override
        public void visit(WasmReturn instruction) {
            consumeAll();
        }

        @Override
        public void visit(WasmInt32Constant instruction) {
            push();
        }

        @Override
        public void visit(WasmInt64Constant instruction) {
            push();
        }

        @Override
        public void visit(WasmFloat32Constant instruction) {
            push();
        }

        @Override
        public void visit(WasmFloat64Constant instruction) {
            push();
        }

        @Override
        public void visit(WasmNullConstant instruction) {
            push();
        }

        @Override
        public void visit(WasmIsNull instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmGetLocal instruction) {
            var value = push();
            value.copyOf = instruction;
            value.definitionCount = definitionCounts[instruction.getLocal().getIndex()];
            var spill = lastSpills[instruction.getLocal().getIndex()];
            if (spill != null && spill.alive && spill.position >= frameStart) {
                value.get = instruction;
                value.spill = spill;
            }
        }

        @Override
        public void visit(WasmSetLocal instruction) {
            if (handleIdentityCopy(instruction)) {
                return;
            }
            definitionCounts[instruction.getLocal().getIndex()]++;
            consume(1);
            var spill = new Spill(nextPosition++, topSpill, instruction);
            topSpill = spill;
            lastSpills[instruction.getLocal().getIndex()] = spill;
        }

        @Override
        public void visit(WasmTeeLocal instruction) {
            lastSpills[instruction.getLocal().getIndex()] = null;
            definitionCounts[instruction.getLocal().getIndex()]++;
            transform(1, 1);
        }

        @Override
        public void visit(WasmGetGlobal instruction) {
            push();
        }

        @Override
        public void visit(WasmSetGlobal instruction) {
            consume(1);
        }

        @Override
        public void visit(WasmIntBinary instruction) {
            transform(2, 1);
        }

        @Override
        public void visit(WasmFloatBinary instruction) {
            transform(2, 1);
        }

        @Override
        public void visit(WasmIntUnary instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmFloatUnary instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmConversion instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmCall instruction) {
            var type = instruction.getFunction().getType();
            transform(type.getParameterTypes().size(), type.getReturnTypes().size());
        }

        @Override
        public void visit(WasmIndirectCall instruction) {
            var type = instruction.getType();
            transform(type.getParameterTypes().size() + 1, type.getReturnTypes().size());
        }

        @Override
        public void visit(WasmCallReference instruction) {
            var type = instruction.getType();
            transform(type.getParameterTypes().size() + 1, type.getReturnTypes().size());
        }

        @Override
        public void visit(WasmDrop instruction) {
            consume(1);
        }

        @Override
        public void visit(WasmLoadInt32 instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmLoadInt64 instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmLoadFloat32 instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmLoadFloat64 instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmStoreInt32 instruction) {
            consume(2);
        }

        @Override
        public void visit(WasmStoreInt64 instruction) {
            consume(2);
        }

        @Override
        public void visit(WasmStoreFloat32 instruction) {
            consume(2);
        }

        @Override
        public void visit(WasmStoreFloat64 instruction) {
            consume(2);
        }

        @Override
        public void visit(WasmMemoryGrow instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmFill instruction) {
            consume(3);
        }

        @Override
        public void visit(WasmCopy instruction) {
            consume(3);
        }

        @Override
        public void visit(WasmTry instruction) {
            var catching = !instruction.getCatches().isEmpty();
            if (catching) {
                catchingTryDepth++;
            }
            processList(instruction.getBody());
            if (catching) {
                catchingTryDepth--;
            }
            push(instruction.getType() != null ? 1 : 0);
        }

        @Override
        public void visit(WasmThrow instruction) {
            consumeAll();
        }

        @Override
        public void visit(WasmReferencesEqual instruction) {
            transform(2, 1);
        }

        @Override
        public void visit(WasmCast instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmTest instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmExternConversion instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmStructNew instruction) {
            transform(instruction.getType().getFields().size(), 1);
        }

        @Override
        public void visit(WasmStructNewDefault instruction) {
            push();
        }

        @Override
        public void visit(WasmStructGet instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmStructSet instruction) {
            consume(2);
        }

        @Override
        public void visit(WasmArrayNewDefault instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmArrayNewFixed instruction) {
            transform(instruction.getSize(), 1);
        }

        @Override
        public void visit(WasmArrayGet instruction) {
            transform(2, 1);
        }

        @Override
        public void visit(WasmArraySet instruction) {
            consume(3);
        }

        @Override
        public void visit(WasmArrayLength instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmArrayCopy instruction) {
            consume(5);
        }

        @Override
        public void visit(WasmFunctionReference instruction) {
            push();
        }

        @Override
        public void visit(WasmInt31Reference instruction) {
            transform(1, 1);
        }

        @Override
        public void visit(WasmInt31Get instruction) {
            transform(1, 1);
        }
    }
}
