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
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.newir.expr.IrBinaryExpr;
import org.teavm.newir.expr.IrBinaryOperation;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrCallType;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrContinueLoopExpr;
import org.teavm.newir.expr.IrDoubleConstantExpr;
import org.teavm.newir.expr.IrExitBlockExpr;
import org.teavm.newir.expr.IrExitLoopExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrFloatConstantExpr;
import org.teavm.newir.expr.IrFunction;
import org.teavm.newir.expr.IrGetFieldExpr;
import org.teavm.newir.expr.IrGetStaticFieldExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLongConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrNullaryExpr;
import org.teavm.newir.expr.IrNullaryOperation;
import org.teavm.newir.expr.IrParameter;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrSequenceExpr;
import org.teavm.newir.expr.IrSetFieldExpr;
import org.teavm.newir.expr.IrSetStaticFieldExpr;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrStringConstantExpr;
import org.teavm.newir.expr.IrType;
import org.teavm.newir.expr.IrVariable;
import org.teavm.newir.expr.IrVariableExpr;

public final class IrExprBuilder {
    private static IrFunction currentFunction;
    private static IrBlockExpr functionBlock;
    private static IrExpr lastExpr;

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

    public static IrNullaryExpr nullValue() {
        return new IrNullaryExpr(IrNullaryOperation.NULL);
    }

    public static IrFunction function(IrType... parameterTypes) {
        return new IrFunction(parameterTypes);
    }

    public static IrFunction function(IrType[] parameterTypes, Consumer<IrParameter[]> body) {
        if (currentFunction != null) {
            throw new IllegalStateException("Can't create function within function");
        }
        IrFunction function = new IrFunction(parameterTypes);
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
        return new IrVariableExpr(var);
    }

    public static IrExpr get(IrParameter param) {
        return new IrParameterExpr(param);
    }

    public static IrExpr add(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.IADD);
    }

    public static IrExpr add(IrExpr a, int b) {
        return add(a, value(b));
    }

    public static IrExpr add(IrExpr a, IrExpr b, IrExpr... remaining) {
        IrExpr acc = new IrBinaryExpr(a, b, IrBinaryOperation.IADD);
        for (IrExpr r : remaining) {
            acc = new IrBinaryExpr(acc, r, IrBinaryOperation.IADD);
        }
        return acc;
    }

    public static IrExpr sub(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.ISUB);
    }

    public static IrExpr mul(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.IMUL);
    }

    public static IrExpr div(IrExpr a, IrExpr b) {
        return thenExpr(new IrBinaryExpr(a, b, IrBinaryOperation.IDIV));
    }

    public static IrExpr rem(IrExpr a, IrExpr b) {
        return thenExpr(new IrBinaryExpr(a, b, IrBinaryOperation.IREM));
    }

    public static IrExpr equal(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.IEQ);
    }

    public static IrExpr equal(IrExpr a, int b) {
        return equal(a, value(b));
    }

    public static IrExpr notEqual(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.INE);
    }

    public static IrExpr notEqual(IrExpr a, int b) {
        return notEqual(a, value(b));
    }

    public static IrExpr less(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.ILT);
    }

    public static IrExpr less(IrExpr a, int b) {
        return less(a, value(b));
    }

    public static IrExpr lessEq(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.ILT);
    }

    public static IrExpr lessEq(IrExpr a, int b) {
        return lessEq(a, value(b));
    }

    public static IrExpr greater(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.IGT);
    }

    public static IrExpr greater(IrExpr a, int b) {
        return greater(a, value(b));
    }

    public static IrExpr greaterEq(IrExpr a, IrExpr b) {
        return new IrBinaryExpr(a, b, IrBinaryOperation.IGE);
    }

    public static IrExpr greaterEq(IrExpr a, int b) {
        return greaterEq(a, value(b));
    }

    public static IrExpr get(IrExpr a, FieldReference field, ValueType type) {
        return thenExpr(new IrGetFieldExpr(a, field, type));
    }

    public static IrExpr get(IrExpr a, Class<?> cls, String fieldName, Class<?> type) {
        return get(a, new FieldReference(cls.getName(), fieldName), ValueType.parse(type));
    }

    public static IrExpr get(FieldReference field, ValueType type) {
        return thenExpr(new IrGetStaticFieldExpr(field, type));
    }

    public static IrExpr get(Class<?> cls, String fieldName, Class<?> type) {
        return get(new FieldReference(cls.getName(), fieldName), ValueType.parse(type));
    }

    public static IrExpr set(IrExpr a, FieldReference field, ValueType type, IrExpr value) {
        return thenExpr(new IrSetFieldExpr(a, value, field, type));
    }

    public static IrExpr set(FieldReference field, ValueType type, IrExpr value) {
        return thenExpr(new IrSetStaticFieldExpr(value, field, type));
    }

    public static IrExpr callStatic(MethodReference method, IrExpr... arguments) {
        return thenExpr(IrCallExpr.of(method, IrCallType.STATIC, arguments));
    }

    public static IrExpr callVirtual(MethodReference method, IrExpr... arguments) {
        return thenExpr(IrCallExpr.of(method, IrCallType.VIRTUAL, arguments));
    }

    public static IrExpr callSpecial(MethodReference method, IrExpr... arguments) {
        return thenExpr(IrCallExpr.of(method, IrCallType.SPECIAL, arguments));
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
        lastExpr = condition;
        thenDo.run();
        expr.setThenExpr(lastExpr);
        ordered |= lastExpr != lastCondBackup;

        lastExpr = condition;
        elseDo.run();
        expr.setElseExpr(lastExpr);
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
                        return expr;
                    }
                }
                lastExpr = new IrSequenceExpr(lastExpr, expr);
            } else {
                lastExpr = expr;
            }
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
