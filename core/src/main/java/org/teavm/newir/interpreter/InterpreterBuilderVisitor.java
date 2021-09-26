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

import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.ObjectHashSet;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.ObjectSet;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
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

public class InterpreterBuilderVisitor implements IrExprVisitor {
    public int resultSlot;
    public final List<InterpreterInstruction> instructions = new ArrayList<>();

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
    private int suggestedJumpPtr;
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
            blockBreak.accept(instructions.size());
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

        IntConsumer jump;
        if (suggestedJump != null && suggestedJumpPtr == instructions.size()) {
            jump = suggestedJump;
        } else {
            InterpreterInstruction.Goto gotoInsn = new InterpreterInstruction.Goto();
            instructions.add(gotoInsn);
            jump = gotoInsn::setTarget;
        }
        blockBreaks.get(expr.getBlock()).add(jump);
        resultSlot = -1;
    }

    @Override
    public void visit(IrLoopExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        expr.getPreheader().acceptVisitor(this);

        int headerPtr = instructions.size();
        loopHeaders.put(expr, headerPtr);
        List<IntConsumer> thisLoopBreaks = new ArrayList<>();
        loopBreaks.put(expr, thisLoopBreaks);

        expr.getBody().acceptVisitor(this);
        instructions.add(new InterpreterInstruction.Goto(headerPtr));

        for (IntConsumer breakConsumer : thisLoopBreaks) {
            breakConsumer.accept(instructions.size());
        }

        loopHeaders.remove(expr);
        loopBreaks.remove(expr);
        resultSlot = -1;
    }

    @Override
    public void visit(IrLoopHeaderExpr expr) {
    }

    @Override
    public void visit(IrExitLoopExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        IntConsumer jump;
        if (suggestedJump != null && suggestedJumpPtr == instructions.size()) {
            jump = suggestedJump;
        } else {
            InterpreterInstruction.Goto gotoInsn = new InterpreterInstruction.Goto();
            instructions.add(gotoInsn);
            jump = gotoInsn::setTarget;
        }
        loopBreaks.get(expr.getLoop()).add(jump);
        resultSlot = -1;
    }

    @Override
    public void visit(IrContinueLoopExpr expr) {
        if (checkDone(expr)) {
            return;
        }
        int header = loopHeaders.get(expr.getLoop());
        if (suggestedJump != null && suggestedJumpPtr == instructions.size()) {
            suggestedJump.accept(header);
        } else {
            instructions.add(new InterpreterInstruction.Goto(header));
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
        release(expr.getCondition());
        requestJumps(expr.getCondition());
        IntConsumer trueJump = whenTrue;
        IntConsumer falseJump = whenFalse;

        InterpreterInstruction.Goto jumpAfterCondition;

        int thenPtr = instructions.size();
        int suggestedJumpPtrBackup = suggestedJumpPtr;
        IntConsumer suggestedJumpBackup = suggestedJump;
        suggestedJump = trueJump;
        suggestedJumpPtr = instructions.size();
        alloc(expr.getThenExpr());
        forceResultSlot(expr.getThenExpr(), slot);
        expr.getThenExpr().acceptVisitor(this);
        release(expr.getThenExpr());

        if (instructions.size() > thenPtr && expr.getThenExpr().getType().getKind() != IrTypeKind.UNREACHABLE) {
            jumpAfterCondition = new InterpreterInstruction.Goto();
            instructions.add(jumpAfterCondition);
        }

        if (instructions.size() == thenPtr) {
            suggestedJump = falseJump;
            suggestedJumpPtr = instructions.size();
        }
        alloc(expr.getElseExpr());
        forceResultSlot(expr.getThenExpr(), slot);
        expr.getElseExpr().acceptVisitor(this);
        release(expr.getElseExpr());

        suggestedJump = suggestedJumpBackup;
        suggestedJumpPtr = suggestedJumpPtrBackup;
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
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] + ctx.iv[b]));
                break;
            case LADD:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] + ctx.lv[b]));
                break;
            case FADD:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.fv[a] + ctx.fv[b]));
                break;
            case DADD:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.dv[a] + ctx.dv[b]));
                break;

            case ISUB:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] - ctx.iv[b]));
                break;
            case LSUB:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] - ctx.lv[b]));
                break;
            case FSUB:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.fv[a] - ctx.fv[b]));
                break;
            case DSUB:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.dv[a] - ctx.dv[b]));

            case IMUL:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] * ctx.iv[b]));
                break;
            case LMUL:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] * ctx.lv[b]));
                break;
            case FMUL:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.fv[a] * ctx.fv[b]));
                break;
            case DMUL:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.dv[a] * ctx.dv[b]));
                break;

            case IDIV:
            case IDIV_SAFE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] / ctx.iv[b]));
                break;
            case LDIV:
            case LDIV_SAFE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] / ctx.lv[b]));
                break;
            case FDIV:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.fv[a] / ctx.fv[b]));
                break;
            case DDIV:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.dv[a] / ctx.dv[b]));
                break;

            case IREM:
            case IREM_SAFE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] % ctx.iv[b]));
                break;
            case LREM:
            case LREM_SAFE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] % ctx.lv[b]));
                break;
            case FREM:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.fv[a] % ctx.fv[b]));
                break;
            case DREM:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.dv[a] % ctx.dv[b]));
                break;

            case IAND:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] & ctx.iv[b]));
                break;
            case LAND:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] & ctx.lv[b]));
                break;

            case IOR:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] | ctx.iv[b]));
                break;
            case LOR:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] | ctx.lv[b]));
                break;

            case IXOR:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] ^ ctx.iv[b]));
                break;
            case LXOR:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.lv[a] ^ ctx.lv[b]));
                break;

            case IEQ:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] == ctx.iv[b]));
                break;
            case INE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] != ctx.iv[b]));
                break;
            case ILT:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] < ctx.iv[b]));
                break;
            case ILE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] <= ctx.iv[b]));
                break;
            case IGT:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] > ctx.iv[b]));
                break;
            case IGE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.iv[a] >= ctx.iv[b]));
                break;

            case REF_EQ:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.ov[a] == ctx.ov[b]));
                break;
            case REF_NE:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, ctx.ov[a] != ctx.ov[b]));
                break;

            case LCMP:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, Long.compare(ctx.lv[a], ctx.lv[b])));
                break;
            case FCMP:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, Float.compare(ctx.fv[a], ctx.fv[b])));
                break;
            case DCMP:
                simpleBinary(expr, (a, b, r) -> ctx -> ctx.emit(r, Double.compare(ctx.dv[a], ctx.dv[b])));
                break;

            case LOGICAL_AND:
                logicalAnd(expr);
                break;

            case LOGICAL_OR:
                logicalOr(expr);
                break;
        }
    }

    private void simpleBinary(IrBinaryExpr expr, BinaryExprInterpreterFactory factory) {
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
            instructions.add(factory.create(a, b, r));
        }
    }

    private void logicalAnd(IrBinaryExpr expr) {
        alloc(expr.getFirst());
        requestJumps(expr.getFirst());
        release(expr.getFirst());

        whenTrue.accept(instructions.size());
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
        whenTrue = ptr -> {
            firstWhenFalse.accept(instructions.size());
            secondWhenTrue.accept(ptr);
        };
        saveBooleanIfNecessary(expr);
    }

    private void logicalOr(IrBinaryExpr expr) {

    }

    private void requestJumps(IrExpr expr) {
        expr.acceptVisitor(this);
        if (whenTrue == null) {
            int slot = resultSlot(expr);
            int ptr = instructions.size();
            instructions.add(null);
            whenTrue = targetPtr -> instructions.set(ptr, new InterpreterInstruction.JumpIfTrue(slot, targetPtr));
            whenFalse = targetPtr -> instructions.set(ptr, new InterpreterInstruction.JumpIfFalse(slot, targetPtr));
        }
    }

    private void saveBooleanIfNecessary(IrExpr expr) {
        if (pendingConsumers.getOrDefault(expr, 0) != 1) {
            int slot = resultSlot(expr);
            whenTrue.accept(instructions.size());
            instructions.add(ctx -> ctx.iv[slot] = 1);
            InterpreterInstruction.Goto gotoInsn = new InterpreterInstruction.Goto();
            instructions.add(gotoInsn);
            whenFalse.accept(instructions.size());
            instructions.add(ctx -> ctx.iv[slot] = 0);
            gotoInsn.setTarget(instructions.size());
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
        ValueType[] argumentTypes = new ValueType[expr.getInputCount() - firstArg];
        for (int i = firstArg; i < expr.getInputCount(); ++i) {
            expr.getInput(i).acceptVisitor(this);
            arguments[i - firstArg] = resultSlot;
            argumentTypes[i - firstArg] = expr.getMethod().parameterType(i - firstArg);
        }

        for (int i = 0; i < expr.getInputCount(); ++i) {
            release(expr.getInput(i));
        }

        int slot = resultSlot(expr);

        ValueType type = expr.getMethod().getReturnType();
        Method method = getMethod(expr.getMethod());
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    instructions.add(ctx -> {
                        ctx.emit(slot, (boolean) ctx.invoke(method, instance, arguments, argumentTypes));
                    });
                    break;
                case CHARACTER:
                    instructions.add(ctx -> {
                        ctx.emit(slot, (char) ctx.invoke(method, instance, arguments, argumentTypes));
                    });
                    break;
                case BYTE:
                case SHORT:
                case INTEGER:
                    instructions.add(ctx -> {
                        ctx.emit(slot, ((Number) ctx.invoke(method, instance, arguments, argumentTypes)).intValue());
                    });
                    break;
                case LONG:
                    instructions.add(ctx -> {
                        ctx.emit(slot, ((Number) ctx.invoke(method, instance, arguments, argumentTypes)).longValue());
                    });
                    break;
                case FLOAT:
                    instructions.add(ctx -> {
                        ctx.emit(slot, ((Number) ctx.invoke(method, instance, arguments, argumentTypes)).floatValue());
                    });
                    break;
                case DOUBLE:
                    instructions.add(ctx -> {
                        ctx.emit(slot, ((Number) ctx.invoke(method, instance, arguments, argumentTypes))
                                .doubleValue());
                    });
                    break;
            }
        } else {
            instructions.add(ctx -> {
                ctx.emit(slot, ctx.invoke(method, instance, arguments, argumentTypes));
            });
        }
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

        Field field = getField(expr.getField());
        int slot = resultSlot(expr);
        ValueType type = expr.getFieldType();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, (boolean) field.get(ctx.ov[objectSlot]));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case CHARACTER:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, (char) field.get(ctx.ov[objectSlot]));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case BYTE:
                case SHORT:
                case INTEGER:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(ctx.ov[objectSlot])).intValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case LONG:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(ctx.ov[objectSlot])).longValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case FLOAT:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(ctx.ov[objectSlot])).floatValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case DOUBLE:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(ctx.ov[objectSlot])).doubleValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
            }
        } else {
            instructions.add(ctx -> {
                try {
                    ctx.emit(slot, field.get(ctx.ov[objectSlot]));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        resultSlot = slot;
    }

    @Override
    public void visit(IrGetStaticFieldExpr expr) {
        if (checkDone(expr)) {
            return;
        }

        Field field = getField(expr.getField());
        int slot = resultSlot(expr);
        ValueType type = expr.getFieldType();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, (boolean) field.get(null));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case CHARACTER:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, (char) field.get(null));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case BYTE:
                case SHORT:
                case INTEGER:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(null)).intValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case LONG:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(null)).longValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case FLOAT:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(null)).floatValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
                case DOUBLE:
                    instructions.add(ctx -> {
                        try {
                            ctx.emit(slot, ((Number) field.get(null)).doubleValue());
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    break;
            }
        } else {
            instructions.add(ctx -> {
                try {
                    ctx.emit(slot, field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        resultSlot = slot;
    }

    private Field getField(FieldReference ref) {
        Class<?> cls;
        try {
            cls = Class.forName(ref.getClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found " + ref.getClassName(), e);
        }
        Field field;
        try {
            field = cls.getField(ref.getFieldName());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Field not found: " + ref.getFieldName(), e);
        }
        return field;
    }

    private Method getMethod(MethodReference ref) {
        Class<?> cls;
        try {
            cls = Class.forName(ref.getClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found " + ref.getClassName(), e);
        }
        Class<?>[] parameterTypes = new Class[ref.parameterCount()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = valueTypeToClass(ref.parameterType(i));
        }
        try {
            return cls.getMethod(ref.getName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Class<?> valueTypeToClass(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return boolean.class;
                case BYTE:
                    return byte.class;
                case SHORT:
                    return short.class;
                case CHARACTER:
                    return char.class;
                case INTEGER:
                    return int.class;
                case LONG:
                    return long.class;
                case FLOAT:
                    return float.class;
                case DOUBLE:
                    return double.class;
                default:
                    throw new IllegalArgumentException();
            }
        } else if (type instanceof ValueType.Void) {
            return void.class;
        } else if (type instanceof ValueType.Array) {
            Class<?> itemClass = valueTypeToClass(((ValueType.Array) type).getItemType());
            return Array.newInstance(itemClass, 0).getClass();
        } else if (type instanceof ValueType.Object) {
            try {
                return Class.forName(((ValueType.Object) type).getClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException();
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
                instructions.add(ctx -> ctx.emit(to, ctx.iv[from]));
                break;
            case LONG:
                instructions.add(ctx -> ctx.emit(to, ctx.lv[from]));
                break;
            case FLOAT:
                instructions.add(ctx -> ctx.emit(to, ctx.fv[from]));
                break;
            case DOUBLE:
                instructions.add(ctx -> ctx.emit(to, ctx.dv[from]));
                break;
            case OBJECT:
                instructions.add(ctx -> ctx.emit(to, ctx.ov[from]));
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

    interface BinaryExprInterpreterFactory {
        InterpreterInstruction create(int a, int b, int r);
    }
}
