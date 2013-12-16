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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.teavm.codegen.NamingException;
import org.teavm.codegen.NamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ast.*;
import org.teavm.javascript.ni.GeneratorContext;
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
    private boolean minifying;

    public Renderer(SourceWriter writer, ClassHolderSource classSource) {
        this.naming = writer.getNaming();
        this.writer = writer;
        this.classSource = classSource;
    }

    public SourceWriter getWriter() {
        return writer;
    }

    public NamingStrategy getNaming() {
        return naming;
    }

    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public void renderRuntime() throws RenderingException {
        try {
            renderRuntimeCls();
            renderRuntimeString();
            renderRuntimeObjcls();
        } catch (NamingException e) {
            throw new RenderingException("Error rendering runtime methods. See a cause for details", e);
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    private void renderRuntimeCls() throws IOException {
        writer.append("$rt_cls").ws().append("=").ws().append("function(clsProto)").ws().append("{")
                .indent().softNewLine();
        String classClass = "java.lang.Class";
        writer.append("var cls").ws().append("=").ws().append("clsProto.classObject;").softNewLine();
        writer.append("if").ws().append("(cls").ws().append("===").ws().append("undefined)").ws()
                .append("{").softNewLine().indent();
        MethodReference createMethodRef = new MethodReference(classClass, new MethodDescriptor("createNew",
                ValueType.object(classClass)));
        writer.append("cls").ws().append("=").ws().appendMethodBody(createMethodRef).append("();").softNewLine();
        writer.append("cls.$data = clsProto;").softNewLine();
        if (classSource.getClassHolder(classClass).getField("name") != null) {
            writer.append("cls.").appendField(new FieldReference(classClass, "name")).ws().append("=").ws()
                    .append("clsProto.$meta.name").ws().append("!==").ws().append("undefined").ws().append("?").ws()
                    .append("$rt_str(clsProto.$meta.name)").ws().append(":").ws().append("null;").softNewLine();
        }
        if (classSource.getClassHolder(classClass).getField("primitive") != null) {
            writer.append("cls.").appendField(new FieldReference(classClass, "primitive"))
                    .append(" = clsProto.$meta.primitive ? 1 : 0;").newLine();
        }
        if (classSource.getClassHolder(classClass).getField("array") != null) {
            writer.append("cls.").appendField(new FieldReference(classClass, "array")).ws()
                    .append("=").ws().append("clsProto.$meta.item").ws().append("?").ws()
                    .append("1").ws().append(":").ws().append("0;").softNewLine();
        }
        writer.append("clsProto.classObject").ws().append("=").ws().append("cls;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return cls;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeString() throws IOException {
        String stringClass = "java.lang.String";
        MethodReference stringCons = new MethodReference(stringClass, new MethodDescriptor("<init>",
                ValueType.arrayOf(ValueType.CHARACTER), ValueType.VOID));
        writer.append("$rt_str = function(str) {").indent().softNewLine();
        writer.append("var characters = $rt_createCharArray(str.length);").softNewLine();
        writer.append("var charsBuffer = characters.data;").softNewLine();
        writer.append("for (var i = 0; i < str.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("charsBuffer[i] = str.charCodeAt(i) & 0xFFFF;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return ").appendClass("java.lang.String").append(".")
                .appendMethod(stringCons).append("(characters);").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeObjcls() throws IOException {
        writer.append("$rt_objcls = function() { return ").appendClass("java.lang.Object").append("; }").newLine();
    }

    public void render(ClassNode cls) throws RenderingException {
        try {
            writer.appendClass(cls.getName()).ws().append("=").ws().append("function()").ws().append("{")
                    .indent().softNewLine();
            for (FieldNode field : cls.getFields()) {
                if (field.getModifiers().contains(NodeModifier.STATIC)) {
                    continue;
                }
                Object value = field.getInitialValue();
                if (value == null) {
                    value = getDefaultValue(field.getType());
                }
                writer.append("this.").appendField(new FieldReference(cls.getName(), field.getName())).ws()
                        .append("=").ws().append(constantToString(value)).append(";").softNewLine();
            }
            writer.append("this.$class").ws().append("=").ws().appendClass(cls.getName()).append(";").softNewLine();
            writer.outdent().append("}").newLine();

            for (FieldNode field : cls.getFields()) {
                if (!field.getModifiers().contains(NodeModifier.STATIC)) {
                    continue;
                }
                Object value = field.getInitialValue();
                if (value == null) {
                    value = getDefaultValue(field.getType());
                }
                writer.appendClass(cls.getName()).append('.')
                        .appendField(new FieldReference(cls.getName(), field.getName())).ws().append("=").ws()
                        .append(constantToString(value)).append(";").softNewLine();
            }

            writer.appendClass(cls.getName()).append(".prototype").ws().append("=").ws().append("new ")
                    .append(cls.getParentName() != null ? naming.getNameFor(cls.getParentName()) :
                    "Object").append("();").softNewLine();
            writer.appendClass(cls.getName()).append(".$meta").ws().append("=").ws().append("{").ws();
            writer.append("name").ws().append(":").ws().append("\"").append(cls.getName()).append("\",").ws();
            writer.append("primitive").ws().append(":").ws().append("false,").ws();
            writer.append("supertypes").ws().append(":").ws().append("[");
            boolean first = true;
            if (cls.getParentName() != null) {
                writer.appendClass(cls.getParentName());
                first = false;
            }
            for (String iface : cls.getInterfaces()) {
                if (!first) {
                    writer.append(",").ws();
                }
                first = false;
                writer.appendClass(iface);
            }
            writer.append("]");
            writer.ws().append("};").softNewLine();
            writer.appendClass(cls.getName()).append("_$clinit").ws().append("=").ws().append("function()").ws()
                    .append("{").softNewLine().indent();
            writer.appendClass(cls.getName()).append("_$clinit").ws().append("=").ws().append("null;").newLine();
            List<String> stubNames = new ArrayList<>();
            for (MethodNode method : cls.getMethods()) {
                renderBody(method);
                stubNames.add(naming.getFullNameFor(method.getReference()));
            }
            MethodHolder methodHolder = classSource.getClassHolder(cls.getName()).getMethod(
                    new MethodDescriptor("<clinit>", ValueType.VOID));
            if (methodHolder != null) {
                writer.appendMethodBody(new MethodReference(cls.getName(), methodHolder.getDescriptor()))
                        .append("();").softNewLine();
            }
            writer.outdent().append("}").newLine();
            for (MethodNode method : cls.getMethods()) {
                if (!method.getModifiers().contains(NodeModifier.STATIC)) {
                    renderDeclaration(method);
                }
            }
            if (stubNames.size() > 0) {
                writer.append("$rt_methodStubs(").appendClass(cls.getName()).append("_$clinit")
                        .append(",").ws().append("[");
                for (int i = 0; i < stubNames.size(); ++i) {
                    if (i > 0) {
                        writer.append(",").ws();
                    }
                    writer.append("'").append(stubNames.get(i)).append("'");
                }
                writer.append("]);").newLine();
            }
        } catch (NamingException e) {
            throw new RenderingException("Error rendering class " + cls.getName() + ". See a cause for details", e);
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
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

    private void renderInitializer(MethodNode method) throws IOException {
        MethodReference ref = method.getReference();
        writer.appendClass(ref.getClassName()).append(".").appendMethod(ref).ws().append("=").ws().append("function(");
        for (int i = 1; i <= ref.parameterCount(); ++i) {
            if (i > 1) {
                writer.append(",").ws();
            }
            writer.append(variableName(i));
        }
        writer.append(")").ws().append("{").newLine().indent();
        writer.append("var result").ws().append("=").ws().append("new ").appendClass(
                ref.getClassName()).append("();").softNewLine();
        writer.append("result.").appendMethod(ref).append("(");
        for (int i = 1; i <= ref.parameterCount(); ++i) {
            if (i > 1) {
                writer.append(",").ws();
            }
            writer.append(variableName(i));
        }
        writer.append(");").softNewLine();
        writer.append("return result;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    public void renderDeclaration(MethodNode method) throws RenderingException, IOException {
        try {
            MethodReference ref = method.getReference();
            if (ref.getDescriptor().getName().equals("<init>")) {
                renderInitializer(method);
            }
            int startParam = 0;
            if (method.getModifiers().contains(NodeModifier.STATIC)) {
                startParam = 1;
            }
            writer.appendClass(ref.getClassName()).append('.');
            if (startParam == 0) {
                writer.append("prototype.");
            }
            writer.appendMethod(ref).ws().append("=").ws().append("function(");
            for (int i = 1; i <= ref.parameterCount(); ++i) {
                if (i > 1) {
                    writer.append(", ");
                }
                writer.append(variableName(i));
            }
            writer.append(")").ws().append("{").softNewLine().indent();
            writer.append("return ").appendMethodBody(ref).append("(");
            if (startParam == 0) {
                writer.append("this");
            }
            for (int i = 1; i <= ref.parameterCount(); ++i) {
                if (i > 1 || startParam == 0) {
                    writer.append(",").ws();
                }
                writer.append(variableName(i));
            }
            writer.append(");").softNewLine();
            writer.outdent().append("}").newLine();
        } catch (NamingException e) {
            throw new RenderingException("Error rendering method " + method.getReference() + ". " +
                    "See cause for details", e);
        }
    }

    public void renderBody(MethodNode method) throws IOException {
        MethodReference ref = method.getReference();
        writer.appendMethodBody(ref).ws().append("=").ws().append("function(");
        int startParam = 0;
        if (method.getModifiers().contains(NodeModifier.STATIC)) {
            startParam = 1;
        }
        for (int i = startParam; i <= ref.parameterCount(); ++i) {
            if (i > startParam) {
                writer.append(",").ws();
            }
            writer.append(variableName(i));
        }
        writer.append(")").ws().append("{").softNewLine().indent();
        method.acceptVisitor(new MethodBodyRenderer());
        writer.outdent().append("}").newLine();
    }

    private class MethodBodyRenderer implements MethodNodeVisitor, GeneratorContext {
        @Override
        public void visit(NativeMethodNode methodNode) {
            try {
                methodNode.getGenerator().generate(this, writer, methodNode.getReference());
            } catch (IOException e) {
                throw new RenderingException("IO error occured", e);
            }
        }

        @Override
        public void visit(RegularMethodNode method) {
            try {
                MethodReference ref = method.getReference();
                int variableCount = method.getVariableCount();
                boolean hasVars = variableCount > ref.parameterCount() + 1;
                if (hasVars) {
                    writer.append("var ");
                    boolean first = true;
                    for (int i = ref.parameterCount() + 1; i < variableCount; ++i) {
                        if (!first) {
                            writer.append(",").ws();
                        }
                        first = false;
                        writer.append(variableName(i));
                    }
                    writer.append(";").softNewLine();
                }
                method.getBody().acceptVisitor(Renderer.this);
            } catch (IOException e) {
                throw new RenderingException("IO error occured", e);
            }
        }

        @Override
        public String getParameterName(int index) {
            return variableName(index);
        }
    }

    @Override
    public void visit(AssignmentStatement statement) throws RenderingException {
        try {
            if (statement.getLeftValue() != null) {
                statement.getLeftValue().acceptVisitor(this);
                writer.ws().append("=").ws();
            }
            statement.getRightValue().acceptVisitor(this);
            writer.append(";").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        for (Statement part : statement.getSequence()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        try {
            writer.append("if").ws().append("(");
            statement.getCondition().acceptVisitor(this);
            writer.append(")").ws().append("{").softNewLine().indent();
            statement.getConsequent().acceptVisitor(this);
            if (statement.getAlternative() != null) {
                writer.outdent().append("}").ws().append("else").ws().append("{").indent().softNewLine();
                statement.getAlternative().acceptVisitor(this);
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        try {
            if (statement.getId() != null) {
                writer.append(statement.getId()).append(": ");
            }
            writer.append("switch").ws().append("(");
            statement.getValue().acceptVisitor(this);
            writer.append(")").ws().append("{").softNewLine().indent();
            for (SwitchClause clause : statement.getClauses()) {
                for (int condition : clause.getConditions()) {
                    writer.append("case ").append(condition).append(":").softNewLine();
                }
                writer.indent();
                clause.getStatement().acceptVisitor(this);
                writer.outdent();
            }
            if (statement.getDefaultClause() != null) {
                writer.append("default:").softNewLine().indent();
                statement.getDefaultClause().acceptVisitor(this);
                writer.outdent();
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        try {
            if (statement.getId() != null) {
                writer.append(statement.getId()).append(":").ws();
            }
            writer.append("while").ws().append("(");
            if (statement.getCondition() != null) {
                statement.getCondition().acceptVisitor(this);
            } else {
                writer.append("true");
            }
            writer.append(")").ws().append("{").softNewLine().indent();
            for (Statement part : statement.getBody()) {
                part.acceptVisitor(this);
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        try {
            writer.append(statement.getId()).append(":").ws().append("{").softNewLine().indent();
            for (Statement part : statement.getBody()) {
                part.acceptVisitor(this);
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ForStatement statement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(BreakStatement statement) {
        try {
            writer.append("break ").append(statement.getTarget().getId()).append(";").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        try {
            writer.append("continue ").append(statement.getTarget().getId()).append(";").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
        try {
            writer.append("return");
            if (statement.getResult() != null) {
                writer.append(' ');
                statement.getResult().acceptVisitor(this);
            }
            writer.append(";").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        try {
            writer.append("$rt_throw(");
            statement.getException().acceptVisitor(this);
            writer.append(");").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(IncrementStatement statement) {
        try {
            writer.append(variableName(statement.getVar()));
            if (statement.getAmount() > 0) {
                if (statement.getAmount() == 1) {
                    writer.append("++");
                } else {
                    writer.ws().append("+=").ws().append(statement.getAmount());
                }
            } else {
                if (statement.getAmount() == -1) {
                    writer.append("--");
                } else {
                    writer.ws().append("-=").ws().append(statement.getAmount());
                }
            }
            writer.append(";").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    public String variableName(int index) {
        if (index == 0) {
            return minifying ? "$t" : "$this";
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
        try {
            writer.append('(');
            expr.getFirstOperand().acceptVisitor(this);
            writer.ws().append(op).ws();
            expr.getSecondOperand().acceptVisitor(this);
            writer.append(')');
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private void visitBinaryFunction(BinaryExpr expr, String function) {
        try {
            writer.append(function);
            writer.append('(');
            expr.getFirstOperand().acceptVisitor(this);
            writer.append(",").ws();
            expr.getSecondOperand().acceptVisitor(this);
            writer.append(')');
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        switch (expr.getOperation()) {
            case ADD:
                visitBinary(expr, "+");
                break;
            case ADD_LONG:
                visitBinaryFunction(expr, "Long_add");
                break;
            case SUBTRACT:
                visitBinary(expr, "-");
                break;
            case SUBTRACT_LONG:
                visitBinaryFunction(expr, "Long_sub");
                break;
            case MULTIPLY:
                visitBinary(expr, "*");
                break;
            case MULTIPLY_LONG:
                visitBinaryFunction(expr, "Long_mul");
                break;
            case DIVIDE:
                visitBinary(expr, "/");
                break;
            case DIVIDE_LONG:
                visitBinaryFunction(expr, "Long_div");
                break;
            case MODULO:
                visitBinary(expr, "%");
                break;
            case MODULO_LONG:
                visitBinaryFunction(expr, "Long_rem");
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
                visitBinaryFunction(expr, "$rt_compare");
                break;
            case COMPARE_LONG:
                visitBinaryFunction(expr, "Long_compare");
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
                visitBinaryFunction(expr, "Long_or");
                break;
            case BITWISE_AND:
                visitBinary(expr, "&");
                break;
            case BITWISE_AND_LONG:
                visitBinaryFunction(expr, "Long_and");
                break;
            case BITWISE_XOR:
                visitBinary(expr, "^");
                break;
            case BITWISE_XOR_LONG:
                visitBinaryFunction(expr, "Long_xor");
                break;
            case LEFT_SHIFT:
                visitBinary(expr, "<<");
                break;
            case LEFT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_lsh");
                break;
            case RIGHT_SHIFT:
                visitBinary(expr, ">>");
                break;
            case RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_rsh");
                break;
            case UNSIGNED_RIGHT_SHIFT:
                visitBinary(expr, ">>>");
                break;
            case UNSIGNED_RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_rshu");
                break;
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        try {
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
                    writer.append("Long_fromInt(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
                case NUM_TO_LONG:
                    writer.append("Long_fromNumber(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
                case LONG_TO_NUM:
                    writer.append("Long_toNumber(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
                case NEGATE_LONG:
                    writer.append("Long_neg(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
                case NOT_LONG:
                    writer.append("Long_not(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
                case BYTE_TO_INT:
                    writer.append("$rt_byteToInt(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
                case SHORT_TO_INT:
                    writer.append("$rt_shortToInt(");
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ConditionalExpr expr) {
        try {
            writer.append('(');
            expr.getCondition().acceptVisitor(this);
            writer.ws().append("?").ws();
            expr.getConsequent().acceptVisitor(this);
            writer.ws().append(":").ws();
            expr.getAlternative().acceptVisitor(this);
            writer.append(')');
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ConstantExpr expr) {
        try {
            writer.append(constantToString(expr.getValue()));
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    public String constantToString(Object cst) {
        if (cst == null) {
            return "null";
        }
        if (cst instanceof ValueType) {
            ValueType type = (ValueType)cst;
            return "$rt_cls(" + typeToClsString(naming, type) + ")";
        } else if (cst instanceof String) {
            return "$rt_str(\"" + escapeString((String)cst) + "\")";
        } else if (cst instanceof Long) {
            long value = (Long)cst;
            if (value == 0) {
                return "Long_ZERO";
            } else if ((int)value == value) {
                return "Long_fromInt(" + value + ")";
            } else {
                return "new Long(" + (value & 0xFFFFFFFFL) + ", " + (value >>> 32) + ")";
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
            value = "$rt_voidcls()";
        } else if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitiveType = (ValueType.Primitive)type;
            switch (primitiveType.getKind()) {
                case BOOLEAN:
                    value = "$rt_booleancls()";
                    break;
                case CHARACTER:
                    value = "$rt_charcls()";
                    break;
                case BYTE:
                    value = "$rt_bytecls()";
                    break;
                case SHORT:
                    value = "$rt_shortcls()";
                    break;
                case INTEGER:
                    value = "$rt_intcls()";
                    break;
                case LONG:
                    value = "$rt_longcls()";
                    break;
                case FLOAT:
                    value = "$rt_floatcls()";
                    break;
                case DOUBLE:
                    value = "$rt_doublecls()";
                    break;
                default:
                    throw new IllegalArgumentException("The type is not renderable");
            }
        } else {
            throw new IllegalArgumentException("The type is not renderable");
        }

        for (int i = 0; i < arrayCount; ++i) {
            value = "$rt_arraycls(" + value + ")";
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
        try {
            writer.append(variableName(expr.getIndex()));
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        try {
            expr.getArray().acceptVisitor(this);
            writer.append('[');
            expr.getIndex().acceptVisitor(this);
            writer.append(']');
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        try {
            expr.getArray().acceptVisitor(this);
            writer.append(".data");
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(InvocationExpr expr) {
        try {
            String className = naming.getNameFor(expr.getMethod().getClassName());
            String name = naming.getNameFor(expr.getMethod());
            String fullName = naming.getFullNameFor(expr.getMethod());
            switch (expr.getType()) {
                case STATIC:
                    writer.append(fullName).append("(");
                    for (int i = 0; i < expr.getArguments().size(); ++i) {
                        if (i > 0) {
                            writer.append(",").ws();
                        }
                        expr.getArguments().get(i).acceptVisitor(this);
                    }
                    writer.append(')');
                    break;
                case SPECIAL:
                    writer.append(fullName).append("(");
                    expr.getArguments().get(0).acceptVisitor(this);
                    for (int i = 1; i < expr.getArguments().size(); ++i) {
                        writer.append(",").ws();
                        expr.getArguments().get(i).acceptVisitor(this);
                    }
                    writer.append(")");
                    break;
                case DYNAMIC:
                    expr.getArguments().get(0).acceptVisitor(this);
                    writer.append(".").append(name).append("(");
                    for (int i = 1; i < expr.getArguments().size(); ++i) {
                        if (i > 1) {
                            writer.append(",").ws();
                        }
                        expr.getArguments().get(i).acceptVisitor(this);
                    }
                    writer.append(')');
                    break;
                case CONSTRUCTOR:
                    writer.append(className).append(".").append(name).append("(");
                    for (int i = 0; i < expr.getArguments().size(); ++i) {
                        if (i > 0) {
                            writer.append(",").ws();
                        }
                        expr.getArguments().get(i).acceptVisitor(this);
                    }
                    writer.append(')');
                    break;
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        try {
            expr.getQualified().acceptVisitor(this);
            writer.append('.').appendField(expr.getField());
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(NewExpr expr) {
        try {
            writer.append("new ").append(naming.getNameFor(expr.getConstructedClass())).append("()");
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(NewArrayExpr expr) {
        try {
            ValueType type = expr.getType();
            while (type instanceof ValueType.Array) {
                type = ((ValueType.Array)type).getItemType();
            }
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive)type).getKind()) {
                    case BOOLEAN:
                        writer.append("$rt_createBooleanArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case BYTE:
                        writer.append("$rt_createByteArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case SHORT:
                        writer.append("$rt_createShortArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case INTEGER:
                        writer.append("$rt_createIntArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case LONG:
                        writer.append("$rt_createLongArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case FLOAT:
                        writer.append("$rt_createFloatArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case DOUBLE:
                        writer.append("$rt_createDoubleArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case CHARACTER:
                        writer.append("$rt_createCharArray(");
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                }
            } else {
                writer.append("$rt_createArray(").append(typeToClsString(naming, expr.getType())).append(", ");
                expr.getLength().acceptVisitor(this);
                writer.append(")");
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        try {
            writer.append("$rt_createMultiArray(").append(typeToClsString(naming, expr.getType())).append(",")
                    .ws().append("[");
            boolean first = true;
            for (Expr dimension : expr.getDimensions()) {
                if (!first) {
                    writer.append(",").ws();
                }
                first = false;
                dimension.acceptVisitor(this);
            }
            writer.append("])");
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        try {
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
            writer.append("$rt_isInstance(");
            expr.getExpr().acceptVisitor(this);
            writer.append(",").ws().append(typeToClsString(naming, expr.getType())).append(")");
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(StaticClassExpr expr) {
        try {
            writer.append(typeToClsString(naming, expr.getType()));
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }
}
