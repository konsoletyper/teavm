/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.javascript;

import java.util.List;
import java.util.Set;
import org.teavm.codegen.NameFrequencyConsumer;
import org.teavm.javascript.ast.*;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class NameFrequencyEstimator implements StatementVisitor, ExprVisitor, MethodNodeVisitor {
    private NameFrequencyConsumer consumer;
    private ClassReaderSource classSource;
    private boolean async;
    private Set<MethodReference> injectedMethods;
    private Set<MethodReference> asyncFamilyMethods;

    public NameFrequencyEstimator(NameFrequencyConsumer consumer, ClassReaderSource classSource,
            Set<MethodReference> injectedMethods, Set<MethodReference> asyncFamilyMethods) {
        this.consumer = consumer;
        this.classSource = classSource;
        this.injectedMethods = injectedMethods;
        this.asyncFamilyMethods = asyncFamilyMethods;
    }

    private void visit(List<Statement> statements) {
        for (Statement part : statements) {
            part.acceptVisitor(this);
        }
    }

    public void estimate(ClassNode cls) {
        // Declaration
        consumer.consume(cls.getName());
        if (cls.getParentName() != null) {
            consumer.consume(cls.getParentName());
        }
        for (FieldNode field : cls.getFields()) {
            consumer.consume(new FieldReference(cls.getName(), field.getName()));
            if (field.getModifiers().contains(NodeModifier.STATIC)) {
                consumer.consume(cls.getName());
            }
        }

        // Methods
        MethodReader clinit = classSource.get(cls.getName()).getMethod(
                new MethodDescriptor("<clinit>", ValueType.VOID));
        for (MethodNode method : cls.getMethods()) {
            if (method.isAsync()) {
                consumer.consumeAsync(method.getReference());
            } else {
                consumer.consume(method.getReference());
                if (asyncFamilyMethods.contains(method.getReference())) {
                    consumer.consume(method.getReference());
                    consumer.consumeAsync(method.getReference());
                }
            }
            if (clinit != null && (method.getModifiers().contains(NodeModifier.STATIC) ||
                            method.getReference().getName().equals("<init>"))) {
                if (!method.isAsync()) {
                    consumer.consume(method.getReference());
                }
                if (asyncFamilyMethods.contains(method.getReference())) {
                    consumer.consumeAsync(method.getReference());
                }
            }
            if (!method.getModifiers().contains(NodeModifier.STATIC)) {
                if (method.isAsync()) {
                    consumer.consumeAsync(method.getReference().getDescriptor());
                    consumer.consumeAsync(method.getReference());
                } else {
                    consumer.consume(method.getReference().getDescriptor());
                    consumer.consume(method.getReference());
                    if (asyncFamilyMethods.contains(method.getReference())) {
                        consumer.consumeAsync(method.getReference().getDescriptor());
                        consumer.consumeAsync(method.getReference());
                    }
                }
            }
        }

        // Metadata
        consumer.consume(cls.getName());
        consumer.consume(cls.getName());
        if (cls.getParentName() != null) {
            consumer.consume(cls.getParentName());
        }
        for (String iface : cls.getInterfaces()) {
            consumer.consume(iface);
        }
    }

    @Override
    public void visit(RegularMethodNode methodNode) {
        async = false;
        methodNode.getBody().acceptVisitor(this);
    }

    @Override
    public void visit(AsyncMethodNode methodNode) {
        async = true;
        for (AsyncMethodPart part : methodNode.getBody()) {
            part.getStatement().acceptVisitor(this);
        }
    }

    @Override
    public void visit(NativeMethodNode methodNode) {
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (statement.getLeftValue() != null) {
            statement.getLeftValue().acceptVisitor(this);
        }
        statement.getRightValue().acceptVisitor(this);
    }

    @Override
    public void visit(SequentialStatement statement) {
        visit(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        visit(statement.getConsequent());
        visit(statement.getAlternative());
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getValue().acceptVisitor(this);
        for (SwitchClause clause : statement.getClauses()) {
            visit(clause.getBody());
        }
        visit(statement.getDefaultClause());
    }

    @Override
    public void visit(WhileStatement statement) {
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
        }
        visit(statement.getBody());
    }

    @Override
    public void visit(BlockStatement statement) {
        visit(statement.getBody());
    }

    @Override
    public void visit(BreakStatement statement) {
    }

    @Override
    public void visit(ContinueStatement statement) {
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (statement.getResult() != null) {
            statement.getResult().acceptVisitor(this);
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getException().acceptVisitor(this);
    }

    @Override
    public void visit(InitClassStatement statement) {
        consumer.consume(statement.getClassName());
    }

    @Override
    public void visit(TryCatchStatement statement) {
        visit(statement.getProtectedBody());
        visit(statement.getHandler());
        if (statement.getExceptionType() != null) {
            consumer.consume(statement.getExceptionType());
        }
    }

    @Override
    public void visit(RestoreAsyncStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        if (async) {
            MethodReference monitorEnterRef = new MethodReference(
                    Object.class, "monitorEnter", Object.class, void.class);
            consumer.consumeAsync(monitorEnterRef);
        } else {
            MethodReference monitorEnterRef = new MethodReference(
                    Object.class, "monitorEnterSync", Object.class, void.class);
            consumer.consume(monitorEnterRef);
        }
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        if (async) {
            MethodReference monitorEnterRef = new MethodReference(
                    Object.class, "monitorExit", Object.class, void.class);
            consumer.consumeAsync(monitorEnterRef);
        } else {
            MethodReference monitorEnterRef = new MethodReference(
                    Object.class, "monitorExitSync", Object.class, void.class);
            consumer.consume(monitorEnterRef);
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        expr.getFirstOperand().acceptVisitor(this);
        expr.getSecondOperand().acceptVisitor(this);
    }

    @Override
    public void visit(UnaryExpr expr) {
        expr.getOperand().acceptVisitor(this);
    }

    @Override
    public void visit(ConditionalExpr expr) {
        expr.getCondition().acceptVisitor(this);
        expr.getConsequent().acceptVisitor(this);
        expr.getAlternative().acceptVisitor(this);
    }

    @Override
    public void visit(ConstantExpr expr) {
        if (expr.getValue() instanceof ValueType) {
            visitType((ValueType)expr.getValue());
        }
    }

    private void visitType(ValueType type) {
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array)type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            String clsName = ((ValueType.Object)type).getClassName();
            consumer.consume(clsName);
        }
    }

    @Override
    public void visit(VariableExpr expr) {
    }

    @Override
    public void visit(SubscriptExpr expr) {
        expr.getArray().acceptVisitor(this);
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        expr.getArray().acceptVisitor(this);
    }

    @Override
    public void visit(InvocationExpr expr) {
        if (injectedMethods.contains(expr.getMethod())) {
            return;
        }
        boolean asyncCall = expr.getAsyncTarget() != null;
        switch (expr.getType()) {
            case SPECIAL:
            case STATIC:
                if (asyncCall) {
                    consumer.consumeAsync(expr.getMethod());
                } else {
                    consumer.consume(expr.getMethod());
                }
                break;
            case CONSTRUCTOR:
                consumer.consumeInit(expr.getMethod());
                break;
            case DYNAMIC:
                if (asyncCall) {
                    consumer.consumeAsync(expr.getMethod().getDescriptor());
                } else {
                    consumer.consume(expr.getMethod().getDescriptor());
                }
                break;
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        expr.getQualified().acceptVisitor(this);
        consumer.consume(expr.getField());
    }

    @Override
    public void visit(NewExpr expr) {
        consumer.consume(expr.getConstructedClass());
    }

    @Override
    public void visit(NewArrayExpr expr) {
        visitType(expr.getType());
        expr.getLength().acceptVisitor(this);
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        visitType(expr.getType());
        for (Expr dimension : expr.getDimensions()) {
            dimension.acceptVisitor(this);
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        expr.getExpr().acceptVisitor(this);
        visitType(expr.getType());
    }

    @Override
    public void visit(StaticClassExpr expr) {
        visitType(expr.getType());
    }
}
