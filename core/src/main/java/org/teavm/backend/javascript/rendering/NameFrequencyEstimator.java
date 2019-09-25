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
package org.teavm.backend.javascript.rendering;

import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.MethodNodeVisitor;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.backend.javascript.codegen.NameFrequencyConsumer;
import org.teavm.backend.javascript.decompile.PreparedClass;
import org.teavm.backend.javascript.decompile.PreparedMethod;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class NameFrequencyEstimator extends RecursiveVisitor implements MethodNodeVisitor {
    static final MethodReference MONITOR_ENTER_METHOD = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    static final MethodReference MONITOR_ENTER_SYNC_METHOD = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_METHOD = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_SYNC_METHOD = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);
    private static final MethodDescriptor CLINIT_METHOD = new MethodDescriptor("<clinit>", ValueType.VOID);

    private final NameFrequencyConsumer consumer;
    private final ClassReaderSource classSource;
    private boolean async;
    private final Set<MethodReference> injectedMethods;
    private final Set<MethodReference> asyncFamilyMethods;

    NameFrequencyEstimator(NameFrequencyConsumer consumer, ClassReaderSource classSource,
            Set<MethodReference> injectedMethods, Set<MethodReference> asyncFamilyMethods) {
        this.consumer = consumer;
        this.classSource = classSource;
        this.injectedMethods = injectedMethods;
        this.asyncFamilyMethods = asyncFamilyMethods;
    }

    public void estimate(PreparedClass cls) {
        // Declaration
        consumer.consume(cls.getName());
        if (cls.getParentName() != null) {
            consumer.consume(cls.getParentName());
        }
        for (FieldHolder field : cls.getClassHolder().getFields()) {
            consumer.consume(new FieldReference(cls.getName(), field.getName()));
            if (field.getModifiers().contains(ElementModifier.STATIC)) {
                consumer.consume(cls.getName());
            }
        }

        // Methods
        MethodReader clinit = classSource.get(cls.getName()).getMethod(CLINIT_METHOD);
        for (PreparedMethod method : cls.getMethods()) {
            consumer.consume(method.reference);
            if (asyncFamilyMethods.contains(method.reference)) {
                consumer.consume(method.reference);
            }
            if (clinit != null && (method.methodHolder.getModifiers().contains(ElementModifier.STATIC)
                    || method.reference.getName().equals("<init>"))) {
                consumer.consume(method.reference);
            }
            if (!method.methodHolder.getModifiers().contains(ElementModifier.STATIC)) {
                consumer.consume(method.reference.getDescriptor());
                consumer.consume(method.reference);
            }
            if (method.async) {
                consumer.consumeFunction("$rt_nativeThread");
                consumer.consumeFunction("$rt_nativeThread");
                consumer.consumeFunction("$rt_resuming");
                consumer.consumeFunction("$rt_invalidPointer");
            }

            if (method.node != null) {
                method.node.acceptVisitor(this);
            }
        }

        if (clinit != null) {
            consumer.consumeFunction("$rt_eraseClinit");
        }

        // Metadata
        consumer.consume(cls.getName());
        consumer.consume(cls.getName());
        if (cls.getParentName() != null) {
            consumer.consume(cls.getParentName());
        }
        for (String iface : cls.getClassHolder().getInterfaces()) {
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
    public void visit(AssignmentStatement statement) {
        super.visit(statement);
        if (statement.isAsync()) {
            consumer.consumeFunction("$rt_suspending");
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getException().acceptVisitor(this);
        consumer.consumeFunction("$rt_throw");
    }

    @Override
    public void visit(InitClassStatement statement) {
        consumer.consumeClassInit(statement.getClassName());
    }

    @Override
    public void visit(TryCatchStatement statement) {
        super.visit(statement);
        if (statement.getExceptionType() != null) {
            consumer.consume(statement.getExceptionType());
        }
        consumer.consumeFunction("$rt_wrapException");
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        super.visit(statement);
        if (async) {
            consumer.consume(MONITOR_ENTER_METHOD);
            consumer.consumeFunction("$rt_suspending");
        } else {
            consumer.consume(MONITOR_ENTER_SYNC_METHOD);
        }
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        super.visit(statement);
        if (async) {
            consumer.consume(MONITOR_EXIT_METHOD);
        } else {
            consumer.consume(MONITOR_EXIT_SYNC_METHOD);
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        super.visit(expr);
        switch (expr.getOperation()) {
            case COMPARE:
                consumer.consumeFunction("$rt_compare");
                break;
            case MULTIPLY:
                if (expr.getType() == OperationType.INT && !RenderingUtil.isSmallInteger(expr.getFirstOperand())
                        && !RenderingUtil.isSmallInteger(expr.getSecondOperand())) {
                    consumer.consumeFunction("$rt_imul");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        super.visit(expr);
        switch (expr.getOperation()) {
            case NULL_CHECK:
                consumer.consumeFunction("$rt_nullCheck");
                break;
            default:
                break;
        }
    }

    @Override
    public void visit(ConstantExpr expr) {
        if (expr.getValue() instanceof ValueType) {
            visitType((ValueType) expr.getValue());
        } else if (expr.getValue() instanceof String) {
            consumer.consumeFunction("$rt_s");
        }
    }

    private void visitType(ValueType type) {
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            String clsName = ((ValueType.Object) type).getClassName();
            consumer.consume(clsName);
            consumer.consumeFunction("$rt_cls");
        }
    }
    @Override
    public void visit(InvocationExpr expr) {
        super.visit(expr);
        if (injectedMethods.contains(expr.getMethod())) {
            return;
        }
        switch (expr.getType()) {
            case SPECIAL:
            case STATIC:
                consumer.consume(expr.getMethod());
                break;
            case CONSTRUCTOR:
                consumer.consumeInit(expr.getMethod());
                break;
            case DYNAMIC:
                consumer.consume(expr.getMethod().getDescriptor());
                break;
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        super.visit(expr);
        consumer.consume(expr.getField());
    }

    @Override
    public void visit(NewExpr expr) {
        super.visit(expr);
        consumer.consume(expr.getConstructedClass());
    }

    @Override
    public void visit(NewArrayExpr expr) {
        super.visit(expr);
        visitType(expr.getType());
        if (!(expr.getType() instanceof ValueType.Primitive)) {
            consumer.consumeFunction("$rt_createArray");
        }
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        super.visit(expr);
        visitType(expr.getType());
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        super.visit(expr);
        visitType(expr.getType());
        if (expr.getType() instanceof ValueType.Object) {
            String clsName = ((ValueType.Object) expr.getType()).getClassName();
            ClassReader cls = classSource.get(clsName);
            if (cls == null || cls.hasModifier(ElementModifier.INTERFACE)) {
                consumer.consumeFunction("$rt_isInstance");
            }
        } else {
            consumer.consumeFunction("$rt_isInstance");
        }
    }
}
