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
package org.teavm.newir.builder;

import java.util.function.Consumer;
import org.teavm.newir.decl.IrFunction;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrContinueLoopExpr;
import org.teavm.newir.expr.IrDoubleConstantExpr;
import org.teavm.newir.expr.IrExitBlockExpr;
import org.teavm.newir.expr.IrExitLoopExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrFloatConstantExpr;
import org.teavm.newir.expr.IrGetVariableExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLongConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrOperation;
import org.teavm.newir.expr.IrOperationExpr;
import org.teavm.newir.expr.IrParameter;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrProgram;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrStringConstantExpr;
import org.teavm.newir.expr.IrVariable;
import org.teavm.newir.type.IrType;

public final class IrExprBuilder {
    private static IrProgram currentFunction;
    private static IrBlockExpr functionBlock;
    private static IrExpr lastExpr;
    private static IrExpr lastReturnedExpr;

    private IrExprBuilder() {
    }

    public static IrIntConstantExpr value(int value) {
        return new IrIntConstantExpr(value);
    }

    public static IrLongConstantExpr value(long value) {
        return new IrLongConstantExpr(value);
    }

    public static IrFloatConstantExpr value(float value) {
        return new IrFloatConstantExpr(value);
    }

    public static IrDoubleConstantExpr value(double value) {
        return new IrDoubleConstantExpr(value);
    }

    public static IrStringConstantExpr value(String value) {
        return new IrStringConstantExpr(value);
    }

    public static IrOperationExpr nullValue() {
        return IrOperationExpr.of(IrOperation.NULL);
    }

    public static IrProgram program(IrType... parameterTypes) {
        return new IrProgram(parameterTypes);
    }

    public static IrProgram program(IrType[] parameterTypes, Consumer<IrParameter[]> body) {
        if (currentFunction != null) {
            throw new IllegalStateException("Can't create function within function");
        }
        IrProgram function = new IrProgram(parameterTypes);
        currentFunction = function;
        functionBlock = new IrBlockExpr();
        currentFunction.setBody(functionBlock);
        IrParameter[] parameters = new IrParameter[function.getParameterCount()];
        for (int i = 0; i < parameters.length; ++i) {
            parameters[i] = function.getParameter(i);
        }
        body.accept(parameters);
        if (lastExpr != null) {
            functionBlock.setBody(lastExpr);
        }
        currentFunction = null;
        return function;
    }

    public static IrVariable intVar() {
        return variable(IrType.INT);
    }

    public static IrVariable objectVar() {
        return variable(IrType.OBJECT);
    }

    public static IrVariable longVar() {
        return variable(IrType.LONG);
    }

    public static IrVariable floatVar() {
        return variable(IrType.FLOAT);
    }

    public static IrVariable doubleVar() {
        return variable(IrType.DOUBLE);
    }

    public static IrVariable booleanVar() {
        return variable(IrType.BOOLEAN);
    }

    public static IrExpr set(IrVariable var, IrExpr value) {
        return thenExpr(new IrSetVariableExpr(value, var));
    }

    public static IrExpr set(IrVariable var, int value) {
        return set(var, value(value));
    }

    public static IrExpr get(IrVariable var) {
        return new IrGetVariableExpr(var);
    }

    public static IrExpr get(IrParameter param) {
        return new IrParameterExpr(param);
    }

    public static IrExpr add(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.IADD, a, b);
    }

    public static IrExpr add(IrExpr a, int b) {
        return add(a, value(b));
    }

    public static IrExpr add(IrExpr a, IrExpr b, IrExpr... remaining) {
        IrExpr acc = IrOperationExpr.of(IrOperation.IADD, a, b);
        for (IrExpr r : remaining) {
            acc = IrOperationExpr.of(IrOperation.IADD, acc, r);
        }
        return acc;
    }

    public static IrExpr sub(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.ISUB, a, b);
    }

    public static IrExpr mul(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.IMUL, a, b);
    }

    public static IrExpr div(IrExpr a, IrExpr b) {
        return thenExpr(IrOperationExpr.of(IrOperation.IDIV, a, b));
    }

    public static IrExpr rem(IrExpr a, IrExpr b) {
        return thenExpr(IrOperationExpr.of(IrOperation.IREM, a, b));
    }

    public static IrExpr equal(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.IEQ, a, b);
    }

    public static IrExpr equal(IrExpr a, int b) {
        return equal(a, value(b));
    }

    public static IrExpr notEqual(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.INE, a, b);
    }

    public static IrExpr notEqual(IrExpr a, int b) {
        return notEqual(a, value(b));
    }

    public static IrExpr less(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.ILT, a, b);
    }

    public static IrExpr less(IrExpr a, int b) {
        return less(a, value(b));
    }

    public static IrExpr lessEq(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.ILE, a, b);
    }

    public static IrExpr lessEq(IrExpr a, int b) {
        return lessEq(a, value(b));
    }

    public static IrExpr greater(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.IGT, a, b);
    }

    public static IrExpr greater(IrExpr a, int b) {
        return greater(a, value(b));
    }

    public static IrExpr greaterEq(IrExpr a, IrExpr b) {
        return IrOperationExpr.of(IrOperation.IGE, a, b);
    }

    public static IrExpr greaterEq(IrExpr a, int b) {
        return greaterEq(a, value(b));
    }

    public static IrExpr call(IrFunction function, IrExpr... arguments) {
        return thenExpr(IrCallExpr.of(function.getCallTarget(), arguments));
    }

    public static IrLoopExpr loop() {
        return new IrLoopExpr();
    }

    public static IrExpr loop(Consumer<LoopLabel> body) {
        checkInFunctionBuilder();
        IrLoopExpr loop = new IrLoopExpr();
        if (lastExpr != null) {
            loop.setPreheader(lastExpr);
        }
        lastExpr = loop.getHeader();
        body.accept(new LoopLabel() {
            @Override
            public void breakLoop() {
                thenExpr(new IrExitLoopExpr(loop));
            }

            @Override
            public void continueLoop() {
                thenExpr(new IrContinueLoopExpr(loop));
            }
        });
        if (lastExpr == loop.getHeader()) {
            return loop;
        }
        loop.setBody(lastExpr);
        lastExpr = null;
        return thenExpr(loop);
    }

    public static IrExpr ifCond(IrExpr condition, Runnable thenDo, Runnable elseDo) {
        checkInFunctionBuilder();
        IrBlockExpr thenBlock = new IrBlockExpr();
        IrBlockExpr elseBlock = new IrBlockExpr();
        boolean ordered = false;
        IrConditionalExpr expr = new IrConditionalExpr(condition, thenBlock, elseBlock);

        IrExpr lastCondBackup = lastExpr;
        lastReturnedExpr = null;
        lastExpr = condition;
        thenDo.run();
        expr.setThenExpr(lastReturnedExpr != null ? lastReturnedExpr : IrExpr.VOID);
        ordered |= lastExpr != lastCondBackup;

        lastReturnedExpr = null;
        lastExpr = condition;
        elseDo.run();
        expr.setElseExpr(lastReturnedExpr != null ? lastReturnedExpr : IrExpr.VOID);
        ordered |= lastExpr != lastCondBackup;
        if (ordered) {
            lastExpr = expr;
        }
        return expr;
    }

    public static IrExpr ifCond(IrExpr condition, Runnable thenDo) {
        checkInFunctionBuilder();
        return ifCond(condition, thenDo, () -> { });
    }

    public static IrExpr exitFunction(IrExpr value) {
        checkInFunctionBuilder();
        return thenExpr(new IrExitBlockExpr(value, functionBlock));
    }

    public static IrExpr exitFunction() {
        return exitFunction(IrExpr.VOID);
    }

    public static IrVariable variable(IrType type) {
        checkInFunctionBuilder();
        return currentFunction.addVariable(type);
    }

    private static void checkInFunctionBuilder() {
        if (currentFunction == null) {
            throw new IllegalStateException("Can only call this method in function builder");
        }
    }

    private static IrExpr thenExpr(IrExpr expr) {
        if (currentFunction != null) {
            if (lastExpr != null) {
                for (int i = 0; i < expr.getInputCount(); ++i) {
                    if (expr.getInput(i) == lastExpr) {
                        lastExpr = expr;
                        lastReturnedExpr = expr;
                        return expr;
                    }
                }
                expr.setPrevious(lastExpr);
            } else {
                lastExpr = expr;
            }
            lastReturnedExpr = lastExpr;
            return lastExpr;
        } else {
            return expr;
        }
    }

    public interface LoopLabel {
        void breakLoop();

        void continueLoop();
    }
}
