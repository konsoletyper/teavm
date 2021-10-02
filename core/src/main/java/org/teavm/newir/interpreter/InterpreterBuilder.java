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
import static org.teavm.newir.interpreter.instructions.Instructions.imov;
import static org.teavm.newir.interpreter.instructions.Instructions.jumpIfFalse;
import static org.teavm.newir.interpreter.instructions.Instructions.jumpIfTrue;
import static org.teavm.newir.interpreter.instructions.Instructions.lmov;
import static org.teavm.newir.interpreter.instructions.Instructions.omov;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import org.teavm.newir.analysis.ExprConsumerCount;
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

public class InterpreterBuilder implements IrExprVisitor {
    public int resultSlot;
    public final ProgramBuilder builder = new ProgramBuilder();

    private int maxIntIndex;
    private int maxLongIndex;
    private int maxFloatIndex;
    private int maxDoubleIndex;
    private int maxObjectIndex;

    private ObjectIntMap<IrExpr> exprSlots = new ObjectIntHashMap<>();
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
    private ExprConsumerCount consumerCount;

    public InterpreterBuilder(
            int intIndex,
            int longIndex,
            int floatIndex,
            int doubleIndex,
            int objectIndex,
            ObjectIntMap<IrParameter> parameterSlots,
            ObjectIntMap<IrVariable> variableSlots,
            ExprConsumerCount consumerCount
    ) {
        maxIntIndex = intIndex;
        maxLongIndex = longIndex;
        maxFloatIndex = floatIndex;
        maxDoubleIndex = doubleIndex;
        maxObjectIndex = objectIndex;
        this.parameterSlots = parameterSlots;
        this.variableSlots = variableSlots;
        this.consumerCount = consumerCount;
    }

    public int build(IrExpr expr) {
        int result = exprSlots.getOrDefault(expr, -1);
        int remaining = consumerCount.decAndGet(expr);
        int ignoreCount = consumerCount.getIgnored(expr);
        boolean wasInPool;

        if (result < 0) {
            if (result == -2) {
                throw new IllegalStateException("Cycle found in IR");
            }
            exprSlots.put(expr, -2);

            int resultSlotBackup = resultSlot;
            if (remaining >= ignoreCount) {
                result = getSlotFromPool(expr);
                resultSlot = result;
            } else {
                throw new IllegalStateException();
            }
            expr.acceptVisitor(this);
            resultSlot = resultSlotBackup;
            if (remaining > 0) {
                exprSlots.put(expr, result);
            }
        }

        if (remaining == ignoreCount) {
            returnSlotToPool(expr, result);
        }
        if (remaining == 0) {
            exprSlots.remove(expr);
        }

        return result;
    }

    private void buildIgnoring(IrExpr expr) {
        int result = exprSlots.getOrDefault(expr, -1);
        int remaining = consumerCount.decAndGet(expr);
        int ignoreCount = consumerCount.getIgnored(expr);

        if (result < 0) {
            if (result == -2) {
                throw new IllegalStateException("Cycle found in IR");
            }
            exprSlots.put(expr, -2);

            int resultSlotBackup = resultSlot;
            resultSlot = -1;
            expr.acceptVisitor(this);
            resultSlot = resultSlotBackup;
            if (remaining > 0) {
                result = remaining >= ignoreCount ? getSlotFromPool(expr) : null;
                exprSlots.put(expr, result);
            }
        }

        if (remaining == ignoreCount) {
            returnSlotToPool(expr, resultSlot);
        }
        if (remaining == 0) {
            exprSlots.remove(expr);
        }
    }

    public void buildTo(IrExpr expr, int result) {
        int actualSlot = exprSlots.getOrDefault(expr, -1);
        int remaining = consumerCount.decAndGet(expr);

        if (actualSlot >= 0) {
            if (actualSlot != result) {
                move(expr.getType(), actualSlot, result);
            }
        } else {
            if (actualSlot == -2) {
                throw new IllegalStateException("Cycle found in IR");
            }
            exprSlots.put(expr, -2);
            int resultSlotBackup = resultSlot;
            resultSlot = result;
            expr.acceptVisitor(this);
            resultSlot = resultSlotBackup;
            if (remaining > 0) {
                exprSlots.put(expr, result);
            }
        }

        if (remaining == 0) {
            exprSlots.remove(expr);
        }
    }

    private int getSlotFromPool(IrExpr expr) {
        switch (expr.getType().getKind()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
                return freeIntSlots.isEmpty() ? maxIntIndex++ : freeIntSlots.pop();
            case LONG:
                return freeLongSlots.isEmpty() ? maxLongIndex++ : freeLongSlots.pop();
            case FLOAT:
                return freeFloatSlots.isEmpty() ? maxFloatIndex++ : freeFloatSlots.pop();
            case DOUBLE:
                return freeDoubleSlots.isEmpty() ? maxDoubleIndex : freeDoubleSlots.pop();
            default:
                return 0;
        }
    }

    private void returnSlotToPool(IrExpr expr, int slot) {
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
        buildIgnoring(expr.getFirst());
        build(expr.getSecond());
    }

    @Override
    public void visit(IrBlockExpr expr) {
        List<IntConsumer> thisBlockBreaks = new ArrayList<>();
        blockBreaks.put(expr, thisBlockBreaks);
        buildTo(expr.getBody(), resultSlot);
        blockBreaks.remove(expr);

        for (IntConsumer blockBreak : thisBlockBreaks) {
            blockBreak.accept(builder.label());
        }
    }

    @Override
    public void visit(IrExitBlockExpr expr) {
        IntConsumer jump = suggestedJump != null && suggestedJumpLabel == builder.label()
                ? suggestedJump
                : builder.addJump(directJump());

        blockBreaks.get(expr.getBlock()).add(jump);
    }

    @Override
    public void visit(IrLoopExpr expr) {
        build(expr.getPreheader());

        int headerLabel = builder.label();
        loopHeaders.put(expr, headerLabel);
        List<IntConsumer> thisLoopBreaks = new ArrayList<>();
        loopBreaks.put(expr, thisLoopBreaks);

        build(expr.getBody());
        builder.addJump(directJump(headerLabel));

        for (IntConsumer breakConsumer : thisLoopBreaks) {
            breakConsumer.accept(builder.label());
        }

        loopHeaders.remove(expr);
        loopBreaks.remove(expr);
    }

    @Override
    public void visit(IrLoopHeaderExpr expr) {
    }

    @Override
    public void visit(IrExitLoopExpr expr) {
        IntConsumer jump = suggestedJump != null && suggestedJumpLabel == builder.label()
                ? suggestedJump
                : builder.addJump(directJump());

        loopBreaks.get(expr.getLoop()).add(jump);
    }

    @Override
    public void visit(IrContinueLoopExpr expr) {
        int header = loopHeaders.get(expr.getLoop());
        if (suggestedJump != null && suggestedJumpLabel == builder.label()) {
            suggestedJump.accept(header);
        } else {
            builder.addJump(directJump(header));
        }
    }

    @Override
    public void visit(IrTupleExpr expr) {
    }

    @Override
    public void visit(IrTupleComponentExpr expr) {
    }

    @Override
    public void visit(IrConditionalExpr expr) {
        requestJumps(expr.getCondition());
        IntConsumer trueJump = whenTrue;
        IntConsumer falseJump = whenFalse;

        int thenLabel = builder.label();
        int suggestedJumpPtrBackup = suggestedJumpLabel;
        IntConsumer suggestedJumpBackup = suggestedJump;
        suggestedJump = trueJump;
        suggestedJumpLabel = builder.label();
        buildTo(expr.getThenExpr(), resultSlot);

        IntConsumer jumpAfterCondition = builder.label() != thenLabel
                && expr.getThenExpr().getType().getKind() != IrTypeKind.UNREACHABLE
                ? builder.addJump(directJump()) : null;

        if (builder.label() == thenLabel) {
            suggestedJump = falseJump;
            suggestedJumpLabel = builder.label();
        } else {
            falseJump.accept(builder.label());
        }
        buildTo(expr.getElseExpr(), resultSlot);

        if (jumpAfterCondition != null) {
            jumpAfterCondition.accept(builder.label());
        } else if (builder.label() == thenLabel) {
            falseJump.accept(builder.label());
        }

        suggestedJump = suggestedJumpBackup;
        suggestedJumpLabel = suggestedJumpPtrBackup;
    }

    @Override
    public void visit(IrNullaryExpr expr) {

    }

    @Override
    public void visit(IrUnaryExpr expr) {
        switch (expr.getOperation()) {

        }
    }

    @Override
    public void visit(IrBinaryExpr expr) {
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
        int a = build(expr.getFirst());
        int b = build(expr.getSecond());
        if (resultSlot > 0) {
            builder.add(factory.create(resultSlot, a, b));
        }
    }

    private void logicalAnd(IrBinaryExpr expr) {
        requestJumps(expr.getFirst());

        whenTrue.accept(builder.label());
        IntConsumer firstWhenFalse = whenFalse;

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
    }

    private void logicalOr(IrBinaryExpr expr) {

    }

    private void requestJumps(IrExpr expr) {
        int condition = build(expr);
        if (whenTrue == null) {
            int label = builder.emptySlot();
            whenTrue = targetLabel -> builder.putInSlot(label, jumpIfTrue(targetLabel, condition));
            whenFalse = targetLabel -> builder.putInSlot(label, jumpIfFalse(targetLabel, condition));
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
        int firstArg;
        int instance;
        if (expr.getCallType() != IrCallType.STATIC) {
            firstArg = 1;
            instance = build(expr.getInput(0));
        } else {
            firstArg = 0;
            instance = -1;
        }

        int[] arguments = new int[expr.getInputCount() - firstArg];
        for (int i = firstArg; i < expr.getInputCount(); ++i) {
            arguments[i - firstArg] = build(expr.getInput(i));
        }

        builder.add(JavaInstructions.call(resultSlot, expr.getMethod(), instance, arguments));
    }

    @Override
    public void visit(IrGetFieldExpr expr) {
        int objectSlot = build(expr.getArgument());
        if (resultSlot >= 0) {
            builder.add(JavaInstructions.getField(resultSlot, expr.getField(), expr.getFieldType(), objectSlot));
        }
    }

    @Override
    public void visit(IrGetStaticFieldExpr expr) {
        if (resultSlot >= 0) {
            builder.add(JavaInstructions.getField(resultSlot, expr.getField(), expr.getFieldType(), -1));
        }
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
        int targetSlot = variableSlots.get(expr.getVariable());
        buildTo(expr.getArgument(), targetSlot);
    }

    @Override
    public void visit(IrIntConstantExpr expr) {
        if (resultSlot >= 0) {
            builder.add(Instructions.icst(resultSlot, expr.getValue()));
        }
    }

    @Override
    public void visit(IrLongConstantExpr expr) {
        if (resultSlot >= 0) {
            builder.add(Instructions.lcst(resultSlot, expr.getValue()));
        }
    }

    @Override
    public void visit(IrFloatConstantExpr expr) {
        if (resultSlot >= 0) {
            builder.add(Instructions.fcst(resultSlot, expr.getValue()));
        }
    }

    @Override
    public void visit(IrDoubleConstantExpr expr) {
        if (resultSlot >= 0) {
            builder.add(Instructions.dcst(resultSlot, expr.getValue()));
        }
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

    interface BinaryInstructionFactory {
        Instruction create(int a, int b, int r);
    }
}
