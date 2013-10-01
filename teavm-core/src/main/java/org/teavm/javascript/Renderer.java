/*
 *  Copyright 2012 Alexey Andreev.
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

import java.util.Set;
import org.teavm.codegen.NamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ast.*;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class Renderer implements ExprVisitor, StatementVisitor {
    private static final String variableNames = "abcdefghijkmnopqrstuvwxyz";
    private NamingStrategy naming;
    private SourceWriter writer;
    private ClassHolderSource classSource;

    public Renderer(SourceWriter writer, ClassHolderSource classSource) {
        this.naming = writer.getNaming();
        this.writer = writer;
        this.classSource = classSource;
    }

    public void render(RenderableClass cls) {
        ClassHolder metadata = cls.getMetadata();
        writer.appendClass(metadata.getName()).append(" = function() {\n").indent().newLine();
        for (FieldHolder field : cls.getFields()) {
            if (field.getModifiers().contains(ElementModifier.STATIC)) {
                continue;
            }
            Object value = field.getInitialValue();
            if (value == null) {
                value = getDefaultValue(field.getType());
            }
            writer.append("this.").appendField(metadata.getName(), field.getName()).append(" = ")
                    .append(constantToString(value)).append(";").newLine();
        }
        writer.append("this.$class = ").appendClass(metadata.getName()).append(";").newLine();
        writer.outdent().append("}").newLine();

        for (FieldHolder field : cls.getFields()) {
            if (!field.getModifiers().contains(ElementModifier.STATIC)) {
                continue;
            }
            Object value = field.getInitialValue();
            if (value == null) {
                value = getDefaultValue(field.getType());
            }
            writer.appendClass(metadata.getName()).append('.')
                    .appendField(metadata.getName(), field.getName()).append(" = ")
                    .append(constantToString(value)).append(";").newLine();
        }

        writer.appendClass(metadata.getName()).append(".prototype = new ")
                .append(metadata.getParent() != null ? naming.getNameFor(metadata.getParent()) :
                "Object").append("();").newLine();
        writer.appendClass(metadata.getName()).append(".$meta = { ");
        writer.append("supertypes : [");
        boolean first = true;
        if (metadata.getParent() != null) {
            writer.appendClass(metadata.getParent());
            first = false;
        }
        for (String iface : metadata.getInterfaces()) {
            if (!first) {
                writer.append(", ");
            }
            first = false;
            writer.appendClass(iface);
        }
        writer.append("]");
        writer.append(" };").newLine();
        for (RenderableMethod method : cls.getMethods()) {
            MethodHolder methodMetadata = method.getMetadata();
            Set<ElementModifier> modifiers = methodMetadata.getModifiers();
            if (modifiers.contains(ElementModifier.ABSTRACT)) {
                continue;
            }
            render(method);
        }
    }

    private static Object getDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitive = (ValueType.Primitive)type;
            switch (primitive.getKind()) {
                case BOOLEAN:
                    return false;
                case BYTE:
                    return (byte)0;
                case SHORT:
                    return (short)0;
                case INTEGER:
                    return 0;
                case CHARACTER:
                    return '\0';
                case LONG:
                    return 0L;
                case FLOAT:
                    return 0F;
                case DOUBLE:
                    return 0.0;
            }
        }
        return null;
    }

    private void renderInitializer(RenderableMethod method) {
        MethodHolder metadata = method.getMetadata();
        writer.appendClass(metadata.getOwner().getName()).append(".")
                .appendMethod(metadata.getOwner().getName(), metadata.getDescriptor())
                .append(" = function(");
        for (int i = 1; i <= metadata.parameterCount(); ++i) {
            if (i > 1) {
                writer.append(", ");
            }
            writer.append(variableName(i));
        }
        writer.append(") {").newLine().indent();
        writer.append("var result = new ").appendClass(metadata.getOwner().getName()).append("();").newLine();
        writer.append("result.").appendMethod(metadata.getOwner().getName(), metadata.getDescriptor()).append("(");
        for (int i = 1; i <= metadata.parameterCount(); ++i) {
            if (i > 1) {
                writer.append(", ");
            }
            writer.append(variableName(i));
        }
        writer.append(");").newLine();
        writer.append("return result;").newLine();
        writer.outdent().append("}").newLine();
    }

    public void render(RenderableMethod method) {
        MethodHolder metadata = method.getMetadata();
        if (metadata.getName().equals("<init>")) {
            renderInitializer(method);
        }
        renderWorkingMethod(method);
        int startParam = 0;
        if (metadata.getModifiers().contains(ElementModifier.STATIC)) {
            startParam = 1;
        }
        writer.appendClass(metadata.getOwner().getName()).append('.');
        if (startParam == 0) {
            writer.append("prototype.");
        }
        writer.appendMethod(metadata.getOwner().getName(), metadata.getDescriptor()).append(" = function(");
        for (int i = 1; i <= metadata.parameterCount(); ++i) {
            if (i > 1) {
                writer.append(", ");
            }
            writer.append(variableName(i));
        }
        writer.append(") {").newLine().indent();
        writer.append("return ").appendClass(metadata.getOwner().getName()).append('_')
                .appendMethod(metadata.getOwner().getName(), metadata.getDescriptor()).append("(");
        if (startParam == 0) {
            writer.append("this");
        }
        for (int i = 1; i <= metadata.parameterCount(); ++i) {
            if (i > 1 || startParam == 0) {
                writer.append(", ");
            }
            writer.append(variableName(i));
        }
        writer.append(");").newLine();
        writer.outdent().append("}").newLine();
    }

    private void renderWorkingMethod(RenderableMethod method) {
        MethodHolder metadata = method.getMetadata();
        writer.append("function ").appendClass(metadata.getOwner().getName()).append('_')
                .appendMethod(metadata.getOwner().getName(), metadata.getDescriptor()).append('(');
        int startParam = 0;
        if (metadata.getModifiers().contains(ElementModifier.STATIC)) {
            startParam = 1;
        }
        for (int i = startParam; i <= metadata.parameterCount(); ++i) {
            if (i > startParam) {
                writer.append(", ");
            }
            writer.append(variableName(i));
        }
        writer.append(") {").newLine().indent();
        int variableCount = method.getVariableCount();
        boolean hasVars = variableCount > metadata.parameterCount() + 1;
        if (hasVars) {
            writer.append("var ");
            boolean first = true;
            for (int i = metadata.parameterCount() + 1; i < variableCount; ++i) {
                if (!first) {
                    writer.append(", ");
                }
                first = false;
                writer.append(variableName(i));
            }
            writer.append(";").newLine();
        }
        method.getBody().acceptVisitor(this);
        writer.outdent().append("}").newLine();
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (statement.getLeftValue() != null) {
            statement.getLeftValue().acceptVisitor(this);
            writer.append(" = ");
        }
        statement.getRightValue().acceptVisitor(this);
        writer.append(";").newLine();
    }

    @Override
    public void visit(SequentialStatement statement) {
        for (Statement part : statement.getSequence()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        writer.append("if (");
        statement.getCondition().acceptVisitor(this);
        writer.append(") {").newLine().indent();
        statement.getConsequent().acceptVisitor(this);
        if (statement.getAlternative() != null) {
            writer.outdent().append("} else {").indent().newLine();
            statement.getAlternative().acceptVisitor(this);
        }
        writer.outdent().append("}").newLine();
    }

    @Override
    public void visit(SwitchStatement statement) {
        if (statement.getId() != null) {
            writer.append(statement.getId()).append(": ");
        }
        writer.append("switch (");
        statement.getValue().acceptVisitor(this);
        writer.append(") {").newLine().indent();
        for (SwitchClause clause : statement.getClauses()) {
            for (int condition : clause.getConditions()) {
                writer.append("case ").append(condition).append(":").newLine();
            }
            writer.indent();
            clause.getStatement().acceptVisitor(this);
            writer.outdent();
        }
        if (statement.getDefaultClause() != null) {
            writer.append("default:").newLine().indent();
            statement.getDefaultClause().acceptVisitor(this);
            writer.outdent();
        }
        writer.outdent().append("}").newLine();
    }

    @Override
    public void visit(WhileStatement statement) {
        if (statement.getId() != null) {
            writer.append(statement.getId()).append(": ");
        }
        writer.append("while (");
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
        } else {
            writer.append("true");
        }
        writer.append(") {").newLine().indent();
        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
        }
        writer.outdent().append("}").newLine();
    }

    @Override
    public void visit(BlockStatement statement) {
        writer.append(statement.getId()).append(": {").newLine().indent();
        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
        }
        writer.outdent().append("}").newLine();
    }

    @Override
    public void visit(ForStatement statement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(BreakStatement statement) {
        writer.append("break ").append(statement.getTarget().getId()).append(";").newLine();
    }

    @Override
    public void visit(ContinueStatement statement) {
        writer.append("continue ").append(statement.getTarget().getId()).append(";").newLine();
    }

    @Override
    public void visit(ReturnStatement statement) {
        writer.append("return");
        if (statement.getResult() != null) {
            writer.append(' ');
            statement.getResult().acceptVisitor(this);
        }
        writer.append(";").newLine();
    }

    @Override
    public void visit(ThrowStatement statement) {
        writer.append("throw ");
        statement.getException().acceptVisitor(this);
        writer.append(";").newLine();
    }

    @Override
    public void visit(IncrementStatement statement) {
        writer.append(variableName(statement.getVar()));
        if (statement.getAmount() > 0) {
            if (statement.getAmount() == 1) {
                writer.append("++");
            } else {
                writer.append(" += ").append(statement.getAmount());
            }
        } else {
            if (statement.getAmount() == -1) {
                writer.append("--");
            } else {
                writer.append(" -= ").append(statement.getAmount());
            }
        }
        writer.append(";").newLine();
    }

    public String variableName(int index) {
        if (index == 0) {
            return "$this";
        }
        --index;
        if (index < variableNames.length()) {
            return Character.toString(variableNames.charAt(index));
        } else {
            return Character.toString(variableNames.charAt(index % variableNames.length())) +
                    index / variableNames.length();
        }
    }

    private void visitBinary(BinaryExpr expr, String op) {
        writer.append('(');
        expr.getFirstOperand().acceptVisitor(this);
        writer.append(' ').append(op).append(' ');
        expr.getSecondOperand().acceptVisitor(this);
        writer.append(')');
    }

    private void visitBinaryFunction(BinaryExpr expr, String function) {
        writer.append(function);
        writer.append('(');
        expr.getFirstOperand().acceptVisitor(this);
        writer.append(", ");
        expr.getSecondOperand().acceptVisitor(this);
        writer.append(')');
    }

    @Override
    public void visit(BinaryExpr expr) {
        switch (expr.getOperation()) {
            case ADD:
                visitBinary(expr, "+");
                break;
            case ADD_LONG:
                visitBinaryFunction(expr, "Long.add");
                break;
            case SUBTRACT:
                visitBinary(expr, "-");
                break;
            case SUBTRACT_LONG:
                visitBinaryFunction(expr, "Long.sub");
                break;
            case MULTIPLY:
                visitBinary(expr, "*");
                break;
            case MULTIPLY_LONG:
                visitBinaryFunction(expr, "Long.mul");
                break;
            case DIVIDE:
                visitBinary(expr, "/");
                break;
            case DIVIDE_LONG:
                visitBinaryFunction(expr, "Long.div");
                break;
            case MODULO:
                visitBinary(expr, "%");
                break;
            case MODULO_LONG:
                visitBinaryFunction(expr, "Long.rem");
                break;
            case EQUALS:
                visitBinary(expr, "==");
                break;
            case NOT_EQUALS:
                visitBinary(expr, "!=");
                break;
            case GREATER:
                visitBinary(expr, ">");
                break;
            case GREATER_OR_EQUALS:
                visitBinary(expr, ">=");
                break;
            case LESS:
                visitBinary(expr, "<");
                break;
            case LESS_OR_EQUALS:
                visitBinary(expr, "<=");
                break;
            case STRICT_EQUALS:
                visitBinary(expr, "===");
                break;
            case STRICT_NOT_EQUALS:
                visitBinary(expr, "!==");
                break;
            case COMPARE:
                visitBinaryFunction(expr, "$rt.compare");
                break;
            case COMPARE_LONG:
                visitBinaryFunction(expr, "Long.compare");
                break;
            case OR:
                visitBinary(expr, "||");
                break;
            case AND:
                visitBinary(expr, "&&");
                break;
            case BITWISE_OR:
                visitBinary(expr, "|");
                break;
            case BITWISE_OR_LONG:
                visitBinaryFunction(expr, "Long.or");
                break;
            case BITWISE_AND:
                visitBinary(expr, "&");
                break;
            case BITWISE_AND_LONG:
                visitBinaryFunction(expr, "Long.and");
                break;
            case BITWISE_XOR:
                visitBinary(expr, "^");
                break;
            case BITWISE_XOR_LONG:
                visitBinaryFunction(expr, "Long.xor");
                break;
            case LEFT_SHIFT:
                visitBinary(expr, "<<");
                break;
            case LEFT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long.lsh");
                break;
            case RIGHT_SHIFT:
                visitBinary(expr, ">>");
                break;
            case RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long.rsh");
                break;
            case UNSIGNED_RIGHT_SHIFT:
                visitBinary(expr, ">>>");
                break;
            case UNSIGNED_RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long.rshu");
                break;
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        switch (expr.getOperation()) {
            case NOT:
                writer.append("(!");
                expr.getOperand().acceptVisitor(this);
                writer.append(')');
                break;
            case NEGATE:
                writer.append("(-");
                expr.getOperand().acceptVisitor(this);
                writer.append(')');
                break;
            case LENGTH:
                expr.getOperand().acceptVisitor(this);
                writer.append(".length");
                break;
            case INT_TO_LONG:
                writer.append("Long.fromInt(");
                expr.getOperand().acceptVisitor(this);
                writer.append(')');
                break;
            case NUM_TO_LONG:
                writer.append("Long.fromNumber(");
                expr.getOperand().acceptVisitor(this);
                writer.append(')');
                break;
            case LONG_TO_NUM:
                writer.append("Long.toNumber(");
                expr.getOperand().acceptVisitor(this);
                writer.append(')');
                break;
            case NEGATE_LONG:
                writer.append("Long.neg(");
                expr.getOperand().acceptVisitor(this);
                writer.append(')');
                break;
            case NOT_LONG:
                writer.append("Long.not(");
                expr.getOperand().acceptVisitor(this);
                writer.append(')');
                break;
        }
    }

    @Override
    public void visit(ConditionalExpr expr) {
        writer.append('(');
        expr.getCondition().acceptVisitor(this);
        writer.append(" ? ");
        expr.getConsequent().acceptVisitor(this);
        writer.append(" : ");
        expr.getAlternative().acceptVisitor(this);
        writer.append(')');
    }

    @Override
    public void visit(ConstantExpr expr) {
        writer.append(constantToString(expr.getValue()));
    }

    public String constantToString(Object cst) {
        if (cst == null) {
            return "null";
        }
        if (cst instanceof ValueType) {
            ValueType type = (ValueType)cst;
            return "$rt.cls(" + typeToClsString(naming, type) + ")";
        } else if (cst instanceof String) {
            return "$rt.str(\"" + escapeString((String)cst) + "\")";
        } else if (cst instanceof Long) {
            long value = (Long)cst;
            if (value == 0) {
                return "Long.ZERO";
            } else if ((int)value == value) {
                return "Long.fromInt(" + value + ")";
            } else {
                return "new Long(" + (value & 0xFFFFFFFF) + ", " + (value >>> 32) + ")";
            }
        } else {
            return cst.toString();
        }
    }

    public static String typeToClsString(NamingStrategy naming, ValueType type) {
        int arrayCount = 0;
        while (type instanceof ValueType.Array) {
            arrayCount++;
            type = ((ValueType.Array)type).getItemType();
        }
        String value;
        if (type instanceof ValueType.Object) {
            ValueType.Object objType = (ValueType.Object)type;
            value = naming.getNameFor(objType.getClassName());
        } else if (type instanceof ValueType.Void) {
            value = "$rt.voidcls()";
        } else if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitiveType = (ValueType.Primitive)type;
            switch (primitiveType.getKind()) {
                case BOOLEAN:
                    value = "$rt.booleancls()";
                    break;
                case CHARACTER:
                    value = "$rt.charcls()";
                    break;
                case BYTE:
                    value = "$rt.bytecls()";
                    break;
                case SHORT:
                    value = "$rt.shortcls()";
                    break;
                case INTEGER:
                    value = "$rt.intcls()";
                    break;
                case LONG:
                    value = "$rt.longcls()";
                    break;
                case FLOAT:
                    value = "$rt.floatcls()";
                    break;
                case DOUBLE:
                    value = "$rt.doublecls()";
                    break;
                default:
                    throw new IllegalArgumentException("The type is not renderable");
            }
        } else {
            throw new IllegalArgumentException("The type is not renderable");
        }

        for (int i = 0; i < arrayCount; ++i) {
            value = "$rt.arraycls(" + value + ")";
        }
        return value;
    }

    public static String escapeString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '\r':
                    sb.append("\\r");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < ' ') {
                        sb.append("\\u00").append(Character.forDigit(c / 16, 16))
                                .append(Character.forDigit(c % 16, 16));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    public void visit(VariableExpr expr) {
        writer.append(variableName(expr.getIndex()));
    }

    @Override
    public void visit(SubscriptExpr expr) {
        expr.getArray().acceptVisitor(this);
        writer.append('[');
        expr.getIndex().acceptVisitor(this);
        writer.append(']');
    }

    @Override
    public void visit(InvocationExpr expr) {
        String className = naming.getNameFor(expr.getClassName());
        String name = naming.getNameFor(expr.getClassName(), expr.getMethod());
        switch (expr.getType()) {
            case STATIC:
                writer.append(className).append("_").append(name).append("(");
                for (int i = 0; i < expr.getArguments().size(); ++i) {
                    if (i > 0) {
                        writer.append(", ");
                    }
                    expr.getArguments().get(i).acceptVisitor(this);
                }
                writer.append(')');
                break;
            case SPECIAL:
                writer.append(className).append("_").append(name).append("(");
                expr.getArguments().get(0).acceptVisitor(this);
                for (int i = 1; i < expr.getArguments().size(); ++i) {
                    writer.append(", ");
                    expr.getArguments().get(i).acceptVisitor(this);
                }
                writer.append(")");
                break;
            case DYNAMIC:
                expr.getArguments().get(0).acceptVisitor(this);
                writer.append(".").append(name).append("(");
                for (int i = 1; i < expr.getArguments().size(); ++i) {
                    if (i > 1) {
                        writer.append(", ");
                    }
                    expr.getArguments().get(i).acceptVisitor(this);
                }
                writer.append(')');
                break;
            case CONSTRUCTOR:
                writer.append(className).append(".").append(name).append("(");
                for (int i = 0; i < expr.getArguments().size(); ++i) {
                    if (i > 0) {
                        writer.append(", ");
                    }
                    expr.getArguments().get(i).acceptVisitor(this);
                }
                writer.append(')');
                break;
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        expr.getQualified().acceptVisitor(this);
        writer.append('.');
        writer.append(naming.getNameFor(expr.getClassName(), expr.getField()));
    }

    @Override
    public void visit(NewExpr expr) {
        writer.append("new ").append(naming.getNameFor(expr.getConstructedClass())).append("()");
    }

    @Override
    public void visit(NewArrayExpr expr) {
        ValueType type = expr.getType();
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array)type).getItemType();
        }
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    writer.append("$rt.createBooleanArray($rt.booleancls(), ");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
                case BYTE:
                    writer.append("$rt.createNumericArray($rt.bytecls(), ");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
                case SHORT:
                    writer.append("$rt.createNumericArray($rt.shortcls(), ");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
                case INTEGER:
                    writer.append("$rt.createNumericArray($rt.intcls(), ");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
                case LONG:
                    writer.append("$rt.createLongArray(");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
                case FLOAT:
                    writer.append("$rt.createNumericArray($rt.floatcls(), ");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
                case DOUBLE:
                    writer.append("$rt.createNumericArray($rt.doublecls(), ");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
                case CHARACTER:
                    writer.append("$rt.createNumericArray($rt.charcls(), ");
                    expr.getLength().acceptVisitor(this);
                    writer.append(")");
                    break;
            }
        } else {
            writer.append("$rt.createArray(").append(typeToClsString(naming, expr.getType())).append(", ");
            expr.getLength().acceptVisitor(this);
            writer.append(")");
        }
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        writer.append("$rt.createMultiArray(").append(typeToClsString(naming, expr.getType()))
                .append(", [");
        boolean first = true;
        for (Expr dimension : expr.getDimensions()) {
            if (!first) {
                writer.append(", ");
            }
            first = false;
            dimension.acceptVisitor(this);
        }
        writer.append("])");
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        if (expr.getType() instanceof ValueType.Object) {
            String clsName = ((ValueType.Object)expr.getType()).getClassName();
            ClassHolder cls = classSource.getClassHolder(clsName);
            if (!cls.getModifiers().contains(ElementModifier.INTERFACE)) {
                writer.append("(");
                expr.getExpr().acceptVisitor(this);
                writer.append(" instanceof ").appendClass(clsName).append(")");
                return;
            }
        }
        writer.append("$rt.isInstance(");
        expr.getExpr().acceptVisitor(this);
        writer.append(", ").append(typeToClsString(naming, expr.getType())).append(")");
    }

    @Override
    public void visit(StaticClassExpr expr) {
        writer.append(typeToClsString(naming, expr.getType()));
    }
}
