/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c.generate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.teavm.ast.ArrayType;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Address;
import org.teavm.interop.c.Include;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.classes.VirtualTable;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;

public class CodeGenerationVisitor implements ExprVisitor, StatementVisitor {
    private static final MethodReference ALLOC_METHOD = new MethodReference(Allocator.class,
            "allocate", RuntimeClass.class, Address.class);
    private static final MethodReference ALLOC_ARRAY_METHOD = new MethodReference(Allocator.class,
            "allocateArray", RuntimeClass.class, int.class, Address.class);
    private static final MethodReference ALLOC_MULTI_ARRAY_METHOD = new MethodReference(Allocator.class,
            "allocateMultiArray", RuntimeClass.class, Address.class, int.class, RuntimeArray.class);
    private static final MethodReference THROW_EXCEPTION_METHOD = new MethodReference(ExceptionHandling.class,
            "throwException", Throwable.class, void.class);

    private GenerationContext context;
    private NameProvider names;
    private CodeWriter writer;
    private int[] temporaryVariableLevel = new int[5];
    private int[] maxTemporaryVariableLevel = new int[5];
    private MethodReference callingMethod;
    private Set<? super String> includes;

    public CodeGenerationVisitor(GenerationContext context, CodeWriter writer, Set<? super String> includes) {
        this.context = context;
        this.writer = writer;
        this.names = context.getNames();
        this.includes = includes;
    }

    public int[] getTemporaries() {
        return maxTemporaryVariableLevel;
    }

    public void setCallingMethod(MethodReference callingMethod) {
        this.callingMethod = callingMethod;
    }

    @Override
    public void visit(BinaryExpr expr) {
        switch (expr.getOperation()) {
            case COMPARE:
                writer.print("compare_");
                switch (expr.getType()) {
                    case INT:
                        writer.print("i32");
                        break;
                    case LONG:
                        writer.print("i64");
                        break;
                    case FLOAT:
                        writer.print("float");
                        break;
                    case DOUBLE:
                        writer.print("double");
                        break;
                }
                writer.print("(");
                expr.getFirstOperand().acceptVisitor(this);
                writer.print(", ");
                expr.getSecondOperand().acceptVisitor(this);
                writer.print(")");
                return;
            case UNSIGNED_RIGHT_SHIFT: {
                String type = expr.getType() == OperationType.LONG ? "int64_t" : "int32_t";
                writer.print("((" + type + ") ((u" + type + ") ");

                expr.getFirstOperand().acceptVisitor(this);
                writer.print(" >> ");
                expr.getSecondOperand().acceptVisitor(this);

                writer.print("))");
                return;
            }

            case MODULO: {
                switch (expr.getType()) {
                    case FLOAT:
                        writer.print("fmodf(");
                        expr.getFirstOperand().acceptVisitor(this);
                        writer.print(", ");
                        expr.getSecondOperand().acceptVisitor(this);
                        writer.print(")");
                        return;
                    case DOUBLE:
                        writer.print("fmod(");
                        expr.getFirstOperand().acceptVisitor(this);
                        writer.print(", ");
                        expr.getSecondOperand().acceptVisitor(this);
                        writer.print(")");
                        return;
                    default:
                        break;
                }
                break;
            }

            default:
                break;
        }

        writer.print("(");
        expr.getFirstOperand().acceptVisitor(this);

        String op;
        switch (expr.getOperation()) {
            case ADD:
                op = "+";
                break;
            case SUBTRACT:
                op = "-";
                break;
            case MULTIPLY:
                op = "*";
                break;
            case DIVIDE:
                op = "/";
                break;
            case MODULO:
                op = "%";
                break;
            case BITWISE_AND:
                op = "&";
                break;
            case BITWISE_OR:
                op = "|";
                break;
            case BITWISE_XOR:
                op = "^";
                break;
            case LEFT_SHIFT:
                op = "<<";
                break;
            case RIGHT_SHIFT:
                op = ">>";
                break;
            case EQUALS:
                op = "==";
                break;
            case NOT_EQUALS:
                op = "!=";
                break;
            case GREATER:
                op = ">";
                break;
            case GREATER_OR_EQUALS:
                op = ">=";
                break;
            case LESS:
                op = "<";
                break;
            case LESS_OR_EQUALS:
                op = "<=";
                break;
            case AND:
                op = "&&";
                break;
            case OR:
                op = "||";
                break;
            default:
                throw new AssertionError();
        }

        writer.print(" ").print(op).print(" ");
        expr.getSecondOperand().acceptVisitor(this);
        writer.print(")");
    }

    @Override
    public void visit(UnaryExpr expr) {
        switch (expr.getOperation()) {
            case NOT:
                writer.print("(");
                writer.print("!");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case NEGATE:
                writer.print("(");
                writer.print("-");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case LENGTH:
                writer.print("ARRAY_LENGTH(");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case NULL_CHECK:
                expr.getOperand().acceptVisitor(this);
                break;
            case INT_TO_BYTE:
                writer.print("TO_BYTE(");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case INT_TO_SHORT:
                writer.print("TO_SHORT(");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case INT_TO_CHAR:
                writer.print("TO_CHAR(");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
        }
    }

    @Override
    public void visit(ConditionalExpr expr) {
        writer.print("(");
        expr.getCondition().acceptVisitor(this);
        writer.print(" ? ");
        expr.getConsequent().acceptVisitor(this);
        writer.print(" : ");
        expr.getAlternative().acceptVisitor(this);
        writer.print(")");
    }

    @Override
    public void visit(ConstantExpr expr) {
        CodeGeneratorUtil.writeValue(writer, context, expr.getValue());
    }

    @Override
    public void visit(VariableExpr expr) {
        if (expr.getIndex() == 0) {
            writer.print("_this_");
        } else {
            writer.print("local_" + expr.getIndex());
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        writer.print("ARRAY_AT(");
        expr.getArray().acceptVisitor(this);
        writer.print(", ").print(getArrayType(expr.getType())).print(", ");
        expr.getIndex().acceptVisitor(this);
        writer.print(")");
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        expr.getArray().acceptVisitor(this);
    }

    private static String getArrayType(ArrayType type) {
        switch (type) {
            case BYTE:
                return "int8_t";
            case SHORT:
                return "int16_t";
            case CHAR:
                return "char16_t";
            case INT:
                return "int32_t";
            case LONG:
                return "int64_t";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case OBJECT:
                return "void*";
            default:
                throw new AssertionError();
        }
    }

    @Override
    public void visit(InvocationExpr expr) {
        ClassReader cls = context.getClassSource().get(expr.getMethod().getClassName());
        if (cls != null) {
            processInclude(cls.getAnnotations());
            MethodReader method = cls.getMethod(expr.getMethod().getDescriptor());
            if (method != null) {
                processInclude(method.getAnnotations());
            }
        }

        Intrinsic intrinsic = context.getIntrinsic(expr.getMethod());
        if (intrinsic != null) {
            intrinsic.apply(intrinsicContext, expr);
            return;
        }

        switch (expr.getType()) {
            case CONSTRUCTOR: {
                String receiver = allocTemporaryVariable(CVariableType.PTR);
                writer.print("(" + receiver + " = ");
                allocObject(expr.getMethod().getClassName());
                writer.print(", ");

                MethodReader method = context.getClassSource().resolve(expr.getMethod());
                writer.print(names.forMethod(method.getReference()));

                writer.print("(" + receiver);
                for (Expr arg : expr.getArguments()) {
                    writer.print(", ");
                    arg.acceptVisitor(this);
                }
                writer.print("), " + receiver + ")");

                freeTemporaryVariable(CVariableType.PTR);

                break;
            }
            case SPECIAL:
            case STATIC: {
                MethodReader method = context.getClassSource().resolve(expr.getMethod());
                if (isWrappedNativeCall(method)) {
                    generateWrappedNativeCall(method, expr);
                } else {
                    writer.print(names.forMethod(method.getReference()));

                    writer.print("(");
                    if (!expr.getArguments().isEmpty()) {
                        expr.getArguments().get(0).acceptVisitor(this);
                        for (int i = 1; i < expr.getArguments().size(); ++i) {
                            writer.print(", ");
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                    }
                    writer.print(")");
                }

                break;
            }
            case DYNAMIC: {
                VirtualTable vtable = context.getVirtualTableProvider().lookup(expr.getMethod().getClassName());
                if (vtable == null || !vtable.getEntries().containsKey(expr.getMethod().getDescriptor())) {
                    writer.print("(");
                    for (Expr arg : expr.getArguments()) {
                        arg.acceptVisitor(this);
                        writer.print(", ");
                    }
                    printDefaultValue(expr.getMethod().getReturnType());
                    writer.print(")");
                } else {
                    String receiver = allocTemporaryVariable(CVariableType.PTR);
                    writer.print("((").print(receiver).print(" = ");
                    expr.getArguments().get(0).acceptVisitor(this);

                    writer.print("), METHOD(")
                            .print(receiver).print(", ")
                            .print(names.forClassClass(expr.getMethod().getClassName())).print(", ")
                            .print(names.forVirtualMethod(expr.getMethod()))
                            .print(")(").print(receiver);
                    for (int i = 1; i < expr.getArguments().size(); ++i) {
                        writer.print(", ");
                        expr.getArguments().get(i).acceptVisitor(this);
                    }
                    writer.print("))");

                    freeTemporaryVariable(CVariableType.PTR);
                }
                break;
            }
        }
    }

    private void generateWrappedNativeCall(MethodReader method, InvocationExpr expr) {
        List<String> temporaries = new ArrayList<>();
        List<String> stringTemporaries = new ArrayList<>();
        String resultTmp = null;
        if (method.getResultType() != ValueType.VOID) {
            resultTmp = allocTemporaryVariable(typeToCType(method.getResultType()));
        }

        for (int i = 0; i < expr.getArguments().size(); ++i) {
            temporaries.add(allocTemporaryVariable(CVariableType.PTR));
        }

        writer.print("(");
        for (int i = 0; i < expr.getArguments().size(); ++i) {
            String tmp = temporaries.get(i);
            writer.print(tmp + " = ");
            ValueType type = method.hasModifier(ElementModifier.STATIC)
                    ? method.parameterType(i)
                    : i == 0 ? ValueType.object(method.getOwnerName()) : method.parameterType(i - 1);
            if (type.isObject(String.class)) {
                writer.print("teavm_stringToC(");
                expr.getArguments().get(i).acceptVisitor(this);
                writer.print(")");
                stringTemporaries.add(tmp);
            } else {
                expr.getArguments().get(i).acceptVisitor(this);
            }

            writer.print(", ");
        }

        if (resultTmp != null) {
            writer.print(resultTmp + " = ");
        }
        writer.print(names.forMethod(method.getReference())).print("(");
        for (int i = 0; i < temporaries.size(); ++i) {
            if (i > 0) {
                writer.print(", ");
            }
            writer.print(temporaries.get(i));
            freeTemporaryVariable(CVariableType.PTR);
        }
        writer.print(")");

        for (String tmp : stringTemporaries) {
            writer.print(", teavm_free(" + tmp + ")");
        }

        if (resultTmp != null) {
            writer.print(", " + resultTmp);
            freeTemporaryVariable(typeToCType(method.getResultType()));
        }

        writer.print(")");
    }

    private boolean isWrappedNativeCall(MethodReader method) {
        if (!method.hasModifier(ElementModifier.NATIVE)) {
            return false;
        }
        for (ValueType type : method.getParameterTypes()) {
            if (type.isObject(String.class)) {
                return true;
            }
        }
        return false;
    }

    private String allocTemporaryVariable(CVariableType type) {
        int index = type.ordinal();
        int result = temporaryVariableLevel[index]++;
        maxTemporaryVariableLevel[index] = Math.max(maxTemporaryVariableLevel[index], temporaryVariableLevel[index]);
        return "tmp_" + type.name().toLowerCase() + "_" + result;
    }

    private void freeTemporaryVariable(CVariableType type) {
        temporaryVariableLevel[type.ordinal()]--;
    }

    private void printDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            writer.print("0");
        } else {
            writer.print("NULL");
        }
    }

    private void processInclude(AnnotationContainerReader container) {
        AnnotationReader annot = container.get(Include.class.getName());
        if (annot == null) {
            return;
        }
        String includeString = annot.getValue("value").getString();

        AnnotationValue systemValue = annot.getValue("isSystem");
        if (systemValue == null || systemValue.getBoolean()) {
            includeString = "<" + includeString + ">";
        } else {
            includeString = "\"" + includeString + "\"";
        }

        includes.add(includeString);
    }

    @Override
    public void visit(QualificationExpr expr) {
        if (expr.getQualified() != null) {
            writer.print("FIELD(");
            expr.getQualified().acceptVisitor(this);
            writer.print(", ").print(names.forClass(expr.getField().getClassName()) + ", "
                    + names.forMemberField(expr.getField()) + ")");
        } else {
            writer.print(names.forStaticField(expr.getField()));
        }
    }

    @Override
    public void visit(NewExpr expr) {
        allocObject(expr.getConstructedClass());
    }

    private void allocObject(String className) {
        writer.print(names.forMethod(ALLOC_METHOD)).print("(&")
                .print(names.forClassInstance(ValueType.object(className)))
                .print(")");
    }

    @Override
    public void visit(NewArrayExpr expr) {
        writer.print(names.forMethod(ALLOC_ARRAY_METHOD)).print("(&")
                .print(names.forClassInstance(ValueType.arrayOf(expr.getType()))).print(", ");
        expr.getLength().acceptVisitor(this);
        writer.print(")");
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        writer.print(names.forMethod(ALLOC_MULTI_ARRAY_METHOD)).print("(&")
                .print(names.forClassInstance(expr.getType())).print(", ");

        writer.print("(int32_t[]) {");
        expr.getDimensions().get(0).acceptVisitor(this);
        for (int i = 1; i < expr.getDimensions().size(); ++i) {
            writer.print(", ");
            expr.getDimensions().get(i).acceptVisitor(this);
        }

        writer.print("}, ").print(String.valueOf(expr.getDimensions().size())).print(")");
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        writer.print("instanceof(");
        expr.getExpr().acceptVisitor(this);
        writer.print(", ").print(names.forSupertypeFunction(expr.getType())).print(")");
    }

    @Override
    public void visit(CastExpr expr) {
        if (expr.getTarget() instanceof ValueType.Object) {
            String className = ((ValueType.Object) expr.getTarget()).getClassName();
            if (context.getCharacteristics().isStructure(className)
                    || className.equals(Address.class.getName())) {
                expr.getValue().acceptVisitor(this);
                return;
            }
        }
        writer.print("checkcast(");
        expr.getValue().acceptVisitor(this);
        writer.print(", ").print(names.forSupertypeFunction(expr.getTarget())).print(")");
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        writer.print("((");
        switch (expr.getTarget()) {
            case INT:
                writer.print("int32_t");
                break;
            case LONG:
                writer.print("int64_t");
                break;
            case FLOAT:
                writer.print("float");
                break;
            case DOUBLE:
                writer.print("double");
                break;
        }
        writer.print(") ");
        expr.getValue().acceptVisitor(this);
        writer.print(")");
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (statement.getLeftValue() != null) {
            statement.getLeftValue().acceptVisitor(this);
            writer.print(" = ");
        }
        statement.getRightValue().acceptVisitor(this);
        writer.println(";");
    }

    @Override
    public void visit(SequentialStatement statement) {
        visitMany(statement.getSequence());
    }

    private void visitMany(List<Statement> statements) {
        for (Statement statement : statements) {
            statement.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        while (true) {
            writer.print("if (");
            statement.getCondition().acceptVisitor(this);
            writer.println(") {").indent();

            visitMany(statement.getConsequent());
            writer.outdent().print("}");

            if (statement.getAlternative().isEmpty()) {
                writer.println();
                break;
            }

            writer.print(" else ");
            if (statement.getAlternative().size() == 1
                    && statement.getAlternative().get(0) instanceof ConditionalStatement) {
                statement = (ConditionalStatement) statement.getAlternative().get(0);
            } else {
                writer.println("{").indent();
                visitMany(statement.getAlternative());
                writer.outdent().println("}");
                break;
            }
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        writer.print("switch (");
        statement.getValue().acceptVisitor(this);
        writer.print(") {").println().indent();

        for (SwitchClause clause : statement.getClauses()) {
            for (int condition : clause.getConditions()) {
                writer.println("case " + condition + ":");
            }

            writer.indent();
            visitMany(clause.getBody());
            writer.println("break;");
            writer.outdent();
        }

        if (!statement.getDefaultClause().isEmpty()) {
            writer.println("default:").indent();
            visitMany(statement.getDefaultClause());
            writer.outdent();
        }

        writer.outdent().println("}");

        if (statement.getId() != null) {
            writer.outdent().println("label_" + statement.getId() + ":;").indent();
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        writer.print("while (");
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
        } else {
            writer.print("1");
        }
        writer.println(") {").indent();

        visitMany(statement.getBody());

        if (statement.getId() != null) {
            writer.outdent().println("cnt_" + statement.getId() + ":;").indent();
        }
        writer.outdent().println("}");

        if (statement.getId() != null) {
            writer.outdent().println("label_" + statement.getId() + ":;").indent();
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        visitMany(statement.getBody());

        if (statement.getId() != null) {
            writer.outdent().println("label_" + statement.getId() + ":;").indent();
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        if (statement.getTarget() == null || statement.getTarget().getId() == null) {
            writer.println("break;");
        } else {
            writer.println("goto label_" + statement.getTarget().getId() + ";");
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        if (statement.getTarget() == null || statement.getTarget().getId() == null) {
            writer.println("continue;");
        } else {
            writer.println("goto cnt_" + statement.getTarget().getId() + ";");
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
        writer.print("return");
        if (statement.getResult() != null) {
            writer.print(" ");
            statement.getResult().acceptVisitor(this);
        }
        writer.println(";");
    }

    @Override
    public void visit(ThrowStatement statement) {
        writer.print(names.forMethod(THROW_EXCEPTION_METHOD)).print("(");
        statement.getException().acceptVisitor(this);
        writer.println(");");
    }

    @Override
    public void visit(InitClassStatement statement) {
        writer.println(names.forClassInitializer(statement.getClassName()) + "();");
    }

    @Override
    public void visit(TryCatchStatement statement) {

    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
    }

    @Override
    public void visit(MonitorExitStatement statement) {
    }

    private IntrinsicContext intrinsicContext = new IntrinsicContext() {
        @Override
        public CodeWriter writer() {
            return writer;
        }

        @Override
        public NameProvider names() {
            return names;
        }

        @Override
        public void emit(Expr expr) {
            expr.acceptVisitor(CodeGenerationVisitor.this);
        }

        @Override
        public Diagnostics getDiagnotics() {
            return context.getDiagnostics();
        }

        @Override
        public MethodReference getCallingMethod() {
            return callingMethod;
        }

        @Override
        public StringPool getStringPool() {
            return context.getStringPool();
        }
    };

    private static CVariableType typeToCType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case CHARACTER:
                case BYTE:
                case SHORT:
                case INTEGER:
                    return CVariableType.INT;
                case LONG:
                    return CVariableType.LONG;
                case FLOAT:
                    return CVariableType.FLOAT;
                case DOUBLE:
                    return CVariableType.DOUBLE;
            }
        }
        return CVariableType.PTR;
    }
}
