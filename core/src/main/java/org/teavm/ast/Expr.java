/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.ast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.teavm.model.*;

public abstract class Expr implements Cloneable {
    private TextLocation location;

    public abstract void acceptVisitor(ExprVisitor visitor);

    @Override
    public Expr clone() {
        return clone(new HashMap<>());
    }

    protected abstract Expr clone(Map<Expr, Expr> cache);

    public static Expr constant(Object value) {
        ConstantExpr expr = new ConstantExpr();
        expr.setValue(value);
        return expr;
    }

    public static Expr var(int index) {
        VariableExpr expr = new VariableExpr();
        expr.setIndex(index);
        return expr;
    }

    public static Expr binary(BinaryOperation op, OperationType type, Expr first, Expr second) {
        BinaryExpr expr = new BinaryExpr();
        expr.setFirstOperand(first);
        expr.setSecondOperand(second);
        expr.setOperation(op);
        expr.setType(type);
        return expr;
    }

    public static Expr binary(BinaryOperation op, OperationType type, Expr first, Expr second, TextLocation loc) {
        Expr expr = binary(op, type, first, second);
        expr.setLocation(loc);
        return expr;
    }

    public static Expr unary(UnaryOperation op, OperationType type, Expr arg) {
        UnaryExpr expr = new UnaryExpr();
        expr.setOperand(arg);
        expr.setOperation(op);
        expr.setType(type);
        return expr;
    }

    public static Expr invert(Expr expr) {
        UnaryExpr result = new UnaryExpr();
        result.setOperand(expr);
        result.setOperation(UnaryOperation.NOT);
        result.setLocation(expr.getLocation());
        return result;
    }

    public static Expr subscript(Expr array, Expr index, ArrayType type) {
        SubscriptExpr expr = new SubscriptExpr();
        expr.setArray(array);
        expr.setIndex(index);
        expr.setType(type);
        return expr;
    }

    public static Expr createArray(ValueType type, Expr length) {
        NewArrayExpr expr = new NewArrayExpr();
        expr.setType(type);
        expr.setLength(length);
        return expr;
    }

    public static Expr createArray(ValueType type, Expr... dimensions) {
        NewMultiArrayExpr expr = new NewMultiArrayExpr();
        expr.setType(type);
        expr.getDimensions().addAll(Arrays.asList(dimensions));
        return expr;
    }

    public static Expr createObject(String type) {
        NewExpr expr = new NewExpr();
        expr.setConstructedClass(type);
        return expr;
    }

    public static InvocationExpr constructObject(MethodReference method, Expr[] arguments) {
        InvocationExpr expr = new InvocationExpr();
        expr.setMethod(method);
        expr.setType(InvocationType.CONSTRUCTOR);
        expr.getArguments().addAll(Arrays.asList(arguments));
        return expr;
    }

    public static Expr qualify(Expr target, FieldReference field) {
        QualificationExpr expr = new QualificationExpr();
        expr.setQualified(target);
        expr.setField(field);
        return expr;
    }

    public static InvocationExpr invoke(MethodReference method, Expr target, Expr[] arguments) {
        InvocationExpr expr = new InvocationExpr();
        expr.setMethod(method);
        expr.setType(InvocationType.DYNAMIC);
        expr.getArguments().add(target);
        expr.getArguments().addAll(Arrays.asList(arguments));
        return expr;
    }

    public static InvocationExpr invokeSpecial(MethodReference method, Expr target, Expr[] arguments) {
        InvocationExpr expr = new InvocationExpr();
        expr.setMethod(method);
        expr.setType(InvocationType.SPECIAL);
        expr.getArguments().add(target);
        expr.getArguments().addAll(Arrays.asList(arguments));
        return expr;
    }

    public static InvocationExpr invokeStatic(MethodReference method, Expr[] arguments) {
        InvocationExpr expr = new InvocationExpr();
        expr.setMethod(method);
        expr.setType(InvocationType.STATIC);
        expr.getArguments().addAll(Arrays.asList(arguments));
        return expr;
    }

    public static Expr instanceOf(Expr target, ValueType className) {
        InstanceOfExpr expr = new InstanceOfExpr();
        expr.setExpr(target);
        expr.setType(className);
        return expr;
    }

    public TextLocation getLocation() {
        return location;
    }

    public void setLocation(TextLocation location) {
        this.location = location;
    }
}
