/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.interpreter;

import static org.teavm.newir.interpreter.instructions.Instructions.directJump;
import static org.teavm.newir.interpreter.instructions.Instructions.dmov;
import static org.teavm.newir.interpreter.instructions.Instructions.fmov;
import static org.teavm.newir.interpreter.instructions.Instructions.icst;
import static org.teavm.newir.interpreter.instructions.Instructions.imov;
import static org.teavm.newir.interpreter.instructions.Instructions.jumpIfFalse;
import static org.teavm.newir.interpreter.instructions.Instructions.jumpIfTrue;
import static org.teavm.newir.interpreter.instructions.Instructions.lmov;
import static org.teavm.newir.interpreter.instructions.Instructions.omov;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.ObjectHashSet;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.ObjectSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import org.teavm.newir.expr.IrArrayElementExpr;
import org.teavm.newir.expr.IrBinaryExpr;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrBoundsCheckExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrCallType;
import org.teavm.newir.expr.IrCastExpr;
import org.teavm.newir.expr.IrCaughtExceptionExpr;
import org.teavm.newir.expr.IrCaughtValueExpr;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrContinueLoopExpr;
import org.teavm.newir.expr.IrDoubleConstantExpr;
import org.teavm.newir.expr.IrExitBlockExpr;
import org.teavm.newir.expr.IrExitLoopExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrExprVisitor;
import org.teavm.newir.expr.IrFloatConstantExpr;
import org.teavm.newir.expr.IrGetFieldExpr;
import org.teavm.newir.expr.IrGetStaticFieldExpr;
import org.teavm.newir.expr.IrInstanceOfExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLongConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrLoopHeaderExpr;
import org.teavm.newir.expr.IrNewArrayExpr;
import org.teavm.newir.expr.IrNewObjectExpr;
import org.teavm.newir.expr.IrNullaryExpr;
import org.teavm.newir.expr.IrParameter;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrSequenceExpr;
import org.teavm.newir.expr.IrSetArrayElementExpr;
import org.teavm.newir.expr.IrSetFieldExpr;
import org.teavm.newir.expr.IrSetStaticFieldExpr;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrStringConstantExpr;
import org.teavm.newir.expr.IrThrowExpr;
import org.teavm.newir.expr.IrTryCatchExpr;
import org.teavm.newir.expr.IrTupleComponentExpr;
import org.teavm.newir.expr.IrTupleExpr;
import org.teavm.newir.expr.IrType;
import org.teavm.newir.expr.IrTypeKind;
import org.teavm.newir.expr.IrUnaryExpr;
import org.teavm.newir.expr.IrVariable;
import org.teavm.newir.expr.IrVariableExpr;
import org.teavm.newir.interpreter.instructions.ArithmeticInstructions;
import org.teavm.newir.interpreter.instructions.Instructions;
import org.teavm.newir.interpreter.instructions.JavaInstructions;

public class InterpreterBuilderVisitor implements IrExprVisitor {
    public int resultSlot;
    public final ProgramBuilder builder = new ProgramBuilder();

    private int maxIntIndex;
    private int maxLongIndex;
    private int maxFloatIndex;
    private int maxDoubleIndex;
    private int maxObjectIndex;

    private ObjectIntMap<IrExpr> pendingConsumers = new ObjectIntHashMap<>();
    private ObjectIntMap<IrExpr> exprSlots = new ObjectIntHashMap<>();
    private Set<IrExpr> borrowedSlots = new HashSet<>();
    private ObjectSet<IrExpr> done = new ObjectHashSet<>();
    private Map<IrLoopExpr, List<IntConsumer>> loopBreaks = new HashMap<>();
    private ObjectIntMap<IrLoopExpr> loopHeaders = new ObjectIntHashMap<>();
    private Map<IrBlockExpr, List<IntConsumer>> blockBreaks = new HashMap<>();
    private ObjectIntMap<IrParameter> parameterSlots;
    private ObjectIntMap<IrVariable> variableSlots;
    private IntConsumer whenTrue;
    private IntConsumer whenFalse;
    private int suggestedJumpLabel;
    private IntConsumer suggestedJump;
    private IntStack freeIntSlots = new IntStack();
    private IntStack freeLongSlots = new IntStack();
    private IntStack freeFloatSlots = new IntStack();
    private IntStack freeDoubleSlots = new IntStack();
    private IntStack freeObjectSlots = new IntStack();

    public InterpreterBuilderVisitor(
            int intIndex,
            int longIndex,
            int floatIndex,
            int doubleIndex,
            int objectIndex,
            ObjectIntMap<IrParameter> parameterSlots,
            ObjectIntMap<IrVariable> variableSlots
    ) {
        maxIntIndex = intIndex;
        maxLongIndex = longIndex;
        maxFloatIndex = floatIndex;
        maxDoubleIndex = doubleIndex;
        maxObjectIndex = objectIndex;
        this.parameterSlots = parameterSlots;
        this.variableSlots = variableSlots;
    }

    public int getMaxIntIndex() {
        return maxIntIndex;
    }

    public int getMaxLongIndex() {
        return maxLongIndex;
    }

    public int getMaxFloatIndex() {
        return maxFloatIndex;
    }

    public int getMaxDoubleIndex() {
        return maxDoubleIndex;
    }

    public int getMaxObjectIndex() {
        return maxObjectIndex;
    }

    public List<Instruction> getInstructions() {
        return builder.instructions;
    }

    @Override
    public void visit(IrSequenceExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        alloc(expr.getSecond());
        expr.getFirst().acceptVisitor(this);
        expr.getSecond().acceptVisitor(this);
        release(expr.getSecond());
    }

    @Override
    public void visit(IrBlockExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        List<IntConsumer> thisBlockBreaks = new ArrayList<>();
        blockBreaks.put(expr, thisBlockBreaks);
        expr.getBody().acceptVisitor(this);
        blockBreaks.remove(expr);

        for (IntConsumer blockBreak : thisBlockBreaks) {
            blockBreak.accept(builder.label());
        }

        resultSlot = -1;
    }

    @Override
    public void visit(IrExitBlockExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        if (checkDone(expr)) {
            return;
        }

        IntConsumer jump = suggestedJump != null && suggestedJumpLabel == builder.label()
                ? suggestedJump
                : builder.addJump(directJump());

        blockBreaks.get(expr.getBlock()).add(jump);
        resultSlot = -1;
    }

    @Override
    public void visit(IrLoopExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        expr.getPreheader().acceptVisitor(this);

        int headerLabel = builder.label();
        loopHeaders.put(expr, headerLabel);
        List<IntConsumer> thisLoopBreaks = new ArrayList<>();
        loopBreaks.put(expr, thisLoopBreaks);

        expr.getBody().acceptVisitor(this);
        builder.addJump(directJump(headerLabel));

        for (IntConsumer breakConsumer : thisLoopBreaks) {
            breakConsumer.accept(builder.label());
        }

        loopHeaders.remove(expr);
        loopBreaks.remove(expr);
        resultSlot = -1;
    }

    @Override
    public void visit(IrLoopHeaderExpr expr) {
        checkDone(expr);
    }

    @Override
    public void visit(IrExitLoopExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        IntConsumer jump = suggestedJump != null && suggestedJumpLabel == builder.label()
                ? suggestedJump
                : builder.addJump(directJump());

        loopBreaks.get(expr.getLoop()).add(jump);
        resultSlot = -1;
    }

    @Override
    public void visit(IrContinueLoopExpr expr) {
        if (checkDone(expr)) {
            return;
        }
        int header = loopHeaders.get(expr.getLoop());
        if (suggestedJump != null && suggestedJumpLabel == builder.label()) {
            suggestedJump.accept(header);
        } else {
            builder.addJump(directJump(header));
        }
        resultSlot = -1;
    }

    @Override
    public void visit(IrTupleExpr expr) {

    }

    @Override
    public void visit(IrTupleComponentExpr expr) {

    }

    @Override
    public void visit(IrConditionalExpr expr) {
        int slot = resultSlot(expr);

        alloc(expr.getCondition());
        expr.getCondition().acceptVisitor(this);
        requestJumps(expr.getCondition());
        IntConsumer trueJump = whenTrue;
        IntConsumer falseJump = whenFalse;

        int thenLabel = builder.label();
        int suggestedJumpPtrBackup = suggestedJumpLabel;
        IntConsumer suggestedJumpBackup = suggestedJump;
        suggestedJump = trueJump;
        suggestedJumpLabel = builder.label();
        alloc(expr.getThenExpr());
        forceResultSlot(expr.getThenExpr(), slot);
        expr.getThenExpr().acceptVisitor(this);
        release(expr.getThenExpr());

        IntConsumer jumpAfterCondition = builder.label() != thenLabel
                && expr.getThenExpr().getType().getKind() != IrTypeKind.UNREACHABLE
                ? builder.addJump(directJump()) : null;

        if (builder.label() == thenLabel) {
            suggestedJump = falseJump;
            suggestedJumpLabel = builder.label();
        } else {
            falseJump.accept(builder.label());
        }
        alloc(expr.getElseExpr());
        forceResultSlot(expr.getThenExpr(), slot);
        expr.getElseExpr().acceptVisitor(this);
        release(expr.getElseExpr());

        if (jumpAfterCondition != null) {
            jumpAfterCondition.accept(builder.label());
        } else if (builder.label() == thenLabel) {
            falseJump.accept(builder.label());
        }

        suggestedJump = suggestedJumpBackup;
        suggestedJumpLabel = suggestedJumpPtrBackup;

        release(expr.getCondition());
        resultSlot = slot;
    }

    @Override
    public void visit(IrNullaryExpr expr) {

    }

    @Override
    public void visit(IrUnaryExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        switch (expr.getOperation()) {

        }
    }

    @Override
    public void visit(IrBinaryExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        switch (expr.getOperation()) {
            case IADD:
                simpleBinary(expr, ArithmeticInstructions::iadd);
                break;
            case LADD:
                simpleBinary(expr, ArithmeticInstructions::ladd);
                break;
            case FADD:
                simpleBinary(expr, ArithmeticInstructions::fadd);
                break;
            case DADD:
                simpleBinary(expr, ArithmeticInstructions::dadd);
                break;

            case ISUB:
                simpleBinary(expr, ArithmeticInstructions::isub);
                break;
            case LSUB:
                simpleBinary(expr, ArithmeticInstructions::lsub);
                break;
            case FSUB:
                simpleBinary(expr, ArithmeticInstructions::fsub);
                break;
            case DSUB:
                simpleBinary(expr, ArithmeticInstructions::dsub);
                break;

            case IMUL:
                simpleBinary(expr, ArithmeticInstructions::imul);
                break;
            case LMUL:
                simpleBinary(expr, ArithmeticInstructions::lmul);
                break;
            case FMUL:
                simpleBinary(expr, ArithmeticInstructions::fmul);
                break;
            case DMUL:
                simpleBinary(expr, ArithmeticInstructions::dmul);
                break;

            case IDIV:
            case IDIV_SAFE:
                simpleBinary(expr, ArithmeticInstructions::idiv);
                break;
            case LDIV:
            case LDIV_SAFE:
                simpleBinary(expr, ArithmeticInstructions::ldiv);
                break;
            case FDIV:
                simpleBinary(expr, ArithmeticInstructions::fdiv);
                break;
            case DDIV:
                simpleBinary(expr, ArithmeticInstructions::ddiv);
                break;

            case IREM:
            case IREM_SAFE:
                simpleBinary(expr, ArithmeticInstructions::irem);
                break;
            case LREM:
            case LREM_SAFE:
                simpleBinary(expr, ArithmeticInstructions::lrem);
                break;
            case FREM:
                simpleBinary(expr, ArithmeticInstructions::frem);
                break;
            case DREM:
                simpleBinary(expr, ArithmeticInstructions::drem);
                break;

            case IAND:
                simpleBinary(expr, ArithmeticInstructions::iand);
                break;
            case LAND:
                simpleBinary(expr, ArithmeticInstructions::land);
                break;

            case IOR:
                simpleBinary(expr, ArithmeticInstructions::ior);
                break;
            case LOR:
                simpleBinary(expr, ArithmeticInstructions::lor);
                break;

            case IXOR:
                simpleBinary(expr, ArithmeticInstructions::ixor);
                break;
            case LXOR:
                simpleBinary(expr, ArithmeticInstructions::lxor);
                break;

            case IEQ:
                simpleBinary(expr, ArithmeticInstructions::ieq);
                break;
            case INE:
                simpleBinary(expr, ArithmeticInstructions::ine);
                break;
            case ILT:
                simpleBinary(expr, ArithmeticInstructions::ilt);
                break;
            case ILE:
                simpleBinary(expr, ArithmeticInstructions::ile);
                break;
            case IGT:
                simpleBinary(expr, ArithmeticInstructions::igt);
                break;
            case IGE:
                simpleBinary(expr, ArithmeticInstructions::ige);
                break;

            case REF_EQ:
                simpleBinary(expr, ArithmeticInstructions::oeq);
                break;
            case REF_NE:
                simpleBinary(expr, ArithmeticInstructions::one);
                break;

            case LCMP:
                simpleBinary(expr, ArithmeticInstructions::lcmp);
                break;
            case FCMP:
                simpleBinary(expr, ArithmeticInstructions::fcmp);
                break;
            case DCMP:
                simpleBinary(expr, ArithmeticInstructions::dcmp);
                break;

            case LOGICAL_AND:
                logicalAnd(expr);
                break;

            case LOGICAL_OR:
                logicalOr(expr);
                break;
        }
    }

    private void simpleBinary(IrBinaryExpr expr, BinaryInstructionFactory factory) {
        alloc(expr.getFirst());
        alloc(expr.getSecond());
        expr.getFirst().acceptVisitor(this);
        int a = resultSlot;
        expr.getSecond().acceptVisitor(this);
        int b = resultSlot;
        release(expr.getSecond());
        release(expr.getFirst());

        int r = resultSlot(expr);
        this.resultSlot = r;
        if (r > 0) {
            builder.add(factory.create(r, a, b));
        }
    }

    private void logicalAnd(IrBinaryExpr expr) {
        alloc(expr.getFirst());
        requestJumps(expr.getFirst());
        release(expr.getFirst());

        whenTrue.accept(builder.label());
        IntConsumer firstWhenFalse = whenFalse;

        alloc(expr.getSecond());
        saveBooleanIfNecessary(expr.getSecond());
        release(expr.getSecond());

        IntConsumer secondWhenTrue = whenTrue;
        IntConsumer secondWhenFalse = whenFalse;

        whenFalse = ptr -> {
            firstWhenFalse.accept(ptr);
            secondWhenFalse.accept(ptr);
        };
        int label = builder.label();
        whenTrue = ptr -> {
            firstWhenFalse.accept(label);
            secondWhenTrue.accept(ptr);
        };
        saveBooleanIfNecessary(expr);
    }

    private void logicalOr(IrBinaryExpr expr) {

    }

    private void requestJumps(IrExpr expr) {
        expr.acceptVisitor(this);
        if (whenTrue == null) {
            int condition = resultSlot;
            int label = builder.emptySlot();
            whenTrue = targetLabel -> builder.putInSlot(label, jumpIfTrue(targetLabel, condition));
            whenFalse = targetLabel -> builder.putInSlot(label, jumpIfFalse(targetLabel, condition));
        }
    }

    private void saveBooleanIfNecessary(IrExpr expr) {
        if (pendingConsumers.getOrDefault(expr, 0) != 1) {
            int slot = resultSlot(expr);
            whenTrue.accept(builder.label());
            builder.add(icst(slot, 1));
            IntConsumer gotoInsn = builder.addJump(directJump());
            whenFalse.accept(builder.label());
            builder.add(icst(slot, 0));
            gotoInsn.accept(builder.label());
        }
    }

    @Override
    public void visit(IrThrowExpr expr) {

    }

    @Override
    public void visit(IrTryCatchExpr expr) {

    }

    @Override
    public void visit(IrCaughtExceptionExpr expr) {

    }

    @Override
    public void visit(IrCaughtValueExpr expr) {

    }

    @Override
    public void visit(IrBoundsCheckExpr expr) {

    }

    @Override
    public void visit(IrCastExpr expr) {

    }

    @Override
    public void visit(IrInstanceOfExpr expr) {

    }

    @Override
    public void visit(IrCallExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        for (int i = 0; i < expr.getInputCount(); ++i) {
            alloc(expr.getInput(i));
        }

        int firstArg;
        int instance;
        if (expr.getCallType() != IrCallType.STATIC) {
            firstArg = 1;
            expr.getInput(0).acceptVisitor(this);
            instance = resultSlot;
        } else {
            firstArg = 0;
            instance = -1;
        }

        int[] arguments = new int[expr.getInputCount() - firstArg];
        for (int i = firstArg; i < expr.getInputCount(); ++i) {
            expr.getInput(i).acceptVisitor(this);
            arguments[i - firstArg] = resultSlot;
        }

        for (int i = 0; i < expr.getInputCount(); ++i) {
            release(expr.getInput(i));
        }

        int slot = resultSlot(expr);

        builder.add(JavaInstructions.call(slot, expr.getMethod(), instance, arguments));

        resultSlot = slot;
    }

    @Override
    public void visit(IrGetFieldExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        alloc(expr.getArgument());
        expr.getArgument().acceptVisitor(this);
        release(expr.getArgument());
        int objectSlot = resultSlot;

        int slot = resultSlot(expr);
        builder.add(JavaInstructions.getField(slot, expr.getField(), expr.getFieldType(), objectSlot));
        resultSlot = slot;
    }

    @Override
    public void visit(IrGetStaticFieldExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        int slot = resultSlot(expr);
        builder.add(JavaInstructions.getField(slot, expr.getField(), expr.getFieldType(), -1));
        resultSlot = slot;
    }


    @Override
    public void visit(IrSetFieldExpr expr) {

    }

    @Override
    public void visit(IrSetStaticFieldExpr expr) {

    }

    @Override
    public void visit(IrNewObjectExpr expr) {

    }

    @Override
    public void visit(IrNewArrayExpr expr) {

    }

    @Override
    public void visit(IrParameterExpr expr) {
        resultSlot = parameterSlots.get(expr.getParameter());
    }

    @Override
    public void visit(IrVariableExpr expr) {
        resultSlot = variableSlots.get(expr.getVariable());
    }

    @Override
    public void visit(IrSetVariableExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        alloc(expr.getArgument());
        expr.getArgument().acceptVisitor(this);
        int valueSlot = resultSlot;
        int targetSlot = variableSlots.get(expr.getVariable());
        if (valueSlot >= 0) {
            move(expr.getVariable().getType(), valueSlot, targetSlot);
        }
        release(expr.getArgument());
        resultSlot = -1;
    }

    @Override
    public void visit(IrIntConstantExpr expr) {
        if (checkDone(expr)) {
            return;
        }
        int slot = resultSlot(expr);
        builder.add(Instructions.icst(slot, expr.getValue()));
        resultSlot = slot;
    }

    @Override
    public void visit(IrLongConstantExpr expr) {

    }

    @Override
    public void visit(IrFloatConstantExpr expr) {

    }

    @Override
    public void visit(IrDoubleConstantExpr expr) {

    }

    @Override
    public void visit(IrStringConstantExpr expr) {

    }

    @Override
    public void visit(IrArrayElementExpr expr) {

    }

    @Override
    public void visit(IrSetArrayElementExpr expr) {

    }

    private void alloc(IrExpr expr) {
        pendingConsumers.put(expr, pendingConsumers.getOrDefault(expr, 0) + 1);
    }

    private void release(IrExpr expr) {
        int newCount = pendingConsumers.getOrDefault(expr, 0) - 1;
        if (newCount == 0) {
            pendingConsumers.remove(expr);
            int slot = exprSlots.getOrDefault(expr, -1);
            if (slot >= 0 && borrowedSlots.remove(expr)) {
                exprSlots.remove(expr);
                switch (expr.getType().getKind()) {
                    case BOOLEAN:
                    case BYTE:
                    case SHORT:
                    case CHAR:
                    case INT:
                        freeIntSlots.add(slot);
                        break;
                    case LONG:
                        freeLongSlots.add(slot);
                        break;
                    case FLOAT:
                        freeFloatSlots.add(slot);
                        break;
                    case DOUBLE:
                        freeDoubleSlots.add(slot);
                        break;
                    case OBJECT:
                        freeObjectSlots.add(slot);
                        break;
                    case UNREACHABLE:
                    case VOID:
                    case TUPLE:
                        break;
                }
            }
        } else {
            pendingConsumers.put(expr, newCount);
        }
    }

    private int resultSlot(IrExpr expr) {
        return resultSlot(expr, -1);
    }

    private void forceResultSlot(IrExpr expr, int slot) {
        if (slot == -1) {
            return;
        }
        int actual = resultSlot(expr, slot);
        if (actual != -1 && actual != slot) {
            move(expr.getType(), actual, slot);
        }
    }

    private void move(IrType type, int from, int to) {
        if (from == to || to == -1) {
            return;
        }
        switch (type.getKind()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
                builder.add(imov(to, from));
                break;
            case LONG:
                builder.add(lmov(to, from));
                break;
            case FLOAT:
                builder.add(fmov(to, from));
                break;
            case DOUBLE:
                builder.add(dmov(to, from));
                break;
            case OBJECT:
                builder.add(omov(to, from));
                break;
            default:
                break;
        }
    }

    private int resultSlot(IrExpr expr, int suggested) {
        if (pendingConsumers.getOrDefault(expr, 0) == 0) {
            return -1;
        }
        int slot = exprSlots.getOrDefault(expr, -1);
        if (slot < 0) {
            switch (expr.getType().getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT: {
                    if (suggested >= 0) {
                        slot = suggested;
                        borrowedSlots.add(expr);
                    } else if (freeIntSlots.isEmpty()) {
                        slot = maxIntIndex++;
                    } else {
                        slot = freeIntSlots.pop();
                    }
                    break;
                }
                case LONG:
                    if (suggested >= 0) {
                        slot = suggested;
                        borrowedSlots.add(expr);
                    } else if (freeLongSlots.isEmpty()) {
                        slot = maxLongIndex++;
                    } else {
                        slot = freeLongSlots.pop();
                    }
                    break;
                case FLOAT:
                    if (suggested >= 0) {
                        slot = suggested;
                        borrowedSlots.add(expr);
                    } else if (freeFloatSlots.isEmpty()) {
                        slot = maxFloatIndex++;
                    } else {
                        slot = freeFloatSlots.pop();
                    }
                    break;
                case DOUBLE:
                    if (suggested >= 0) {
                        slot = suggested;
                        borrowedSlots.add(expr);
                    } else if (freeDoubleSlots.isEmpty()) {
                        slot = maxDoubleIndex++;
                    } else {
                        slot = freeDoubleSlots.pop();
                    }
                    break;
                case OBJECT:
                    if (suggested >= 0) {
                        slot = suggested;
                    } else if (freeObjectSlots.isEmpty()) {
                        slot = maxObjectIndex++;
                    } else {
                        slot = freeObjectSlots.pop();
                    }
                    break;
                case UNREACHABLE:
                case VOID:
                case TUPLE:
                    break;
            }
            if (slot >= 0) {
                exprSlots.put(expr, slot);
            }
        }
        return slot;
    }

    private boolean checkDone(IrExpr expr) {
        if (done.contains(expr)) {
            resultSlot = resultSlot(expr);
            return true;
        }
        done.add(expr);
        return false;
    }

    interface BinaryInstructionFactory {
        Instruction create(int a, int b, int r);
    }
}
