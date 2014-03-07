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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.teavm.codegen.NamingException;
import org.teavm.codegen.NamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ast.*;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.javascript.ni.InjectedBy;
import org.teavm.javascript.ni.Injector;
import org.teavm.javascript.ni.InjectorContext;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class Renderer implements ExprVisitor, StatementVisitor, RenderingContext {
    private static final String variableNames = "abcdefghijkmnopqrstuvwxyz";
    private NamingStrategy naming;
    private SourceWriter writer;
    private ListableClassHolderSource classSource;
    private ClassLoader classLoader;
    private boolean minifying;
    private Map<MethodReference, InjectorHolder> injectorMap = new HashMap<>();

    private static class InjectorHolder {
        public final Injector injector;

        public InjectorHolder(Injector injector) {
            this.injector = injector;
        }
    }

    public Renderer(SourceWriter writer, ListableClassHolderSource classSource, ClassLoader classLoader) {
        this.naming = writer.getNaming();
        this.writer = writer;
        this.classSource = classSource;
        this.classLoader = classLoader;
    }

    @Override
    public SourceWriter getWriter() {
        return writer;
    }

    @Override
    public NamingStrategy getNaming() {
        return naming;
    }

    @Override
    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    @Override
    public ListableClassHolderSource getClassSource() {
        return classSource;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void renderRuntime() throws RenderingException {
        try {
            renderRuntimeCls();
            renderRuntimeString();
            renderRuntimeUnwrapString();
            renderRuntimeObjcls();
            renderRuntimeNullCheck();
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
        MethodReference createMethodRef = new MethodReference(classClass, "createNew", ValueType.object(classClass));
        writer.append("cls").ws().append("=").ws().appendMethodBody(createMethodRef).append("();").softNewLine();
        writer.append("cls.$data = clsProto;").softNewLine();
        if (classSource.get(classClass).getField("name") != null) {
            writer.append("cls.").appendField(new FieldReference(classClass, "name")).ws().append("=").ws()
                    .append("clsProto.$meta.name").ws().append("!==").ws().append("undefined").ws().append("?").ws()
                    .append("$rt_str(clsProto.$meta.name)").ws().append(":").ws().append("null;").softNewLine();
        }
        if (classSource.get(classClass).getField("primitive") != null) {
            writer.append("cls.").appendField(new FieldReference(classClass, "primitive"))
                    .append(" = clsProto.$meta.primitive ? 1 : 0;").newLine();
        }
        if (classSource.get(classClass).getField("array") != null) {
            writer.append("cls.").appendField(new FieldReference(classClass, "array")).ws()
                    .append("=").ws().append("clsProto.$meta.item").ws().append("?").ws()
                    .append("1").ws().append(":").ws().append("0;").softNewLine();
        }
        if (classSource.get(classClass).getField("isEnum") != null) {
            writer.append("cls.").appendField(new FieldReference(classClass, "isEnum")).ws()
                    .append("=").ws().append("clsProto.$meta.enum").ws().append("?").ws()
                    .append("1").ws().append(":").ws().append("0;").softNewLine();
        }
        writer.append("clsProto.classObject").ws().append("=").ws().append("cls;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return cls;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeString() throws IOException {
        String stringClass = "java.lang.String";
        MethodReference stringCons = new MethodReference(stringClass, "<init>",
                ValueType.arrayOf(ValueType.CHARACTER), ValueType.VOID);
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

    private void renderRuntimeUnwrapString() throws IOException {
        String stringClass = "java.lang.String";
        MethodReference stringLen = new MethodReference(stringClass, "length", ValueType.INTEGER);
        MethodReference getChars = new MethodReference(stringClass, "getChars", ValueType.INTEGER, ValueType.INTEGER,
                ValueType.arrayOf(ValueType.CHARACTER), ValueType.INTEGER, ValueType.VOID);
        writer.append("$rt_ustr = function(str) {").indent().softNewLine();
        writer.append("var result = \"\";").softNewLine();
        writer.append("var sz = ").appendMethodBody(stringLen).append("(str);").softNewLine();
        writer.append("var array = $rt_createCharArray(sz);").softNewLine();
        writer.appendMethodBody(getChars).append("(str, 0, sz, array, 0);").softNewLine();
        writer.append("for (var i = 0; i < sz; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(array.data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeNullCheck() throws IOException {
        String npe = "java.lang.NullPointerException";
        writer.append("$rt_nullCheck = function(val) {").indent().softNewLine();
        writer.append("if (val === null) {").indent().softNewLine();
        writer.append("$rt_throw(").appendClass(npe).append('.').appendMethod(npe, "<init>", ValueType.VOID)
                .append("());").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return val;").softNewLine();
        writer.outdent().append("}").newLine();
    }

    private void renderRuntimeObjcls() throws IOException {
        writer.append("$rt_objcls = function() { return ").appendClass("java.lang.Object").append("; }").newLine();
    }

    public void render(ClassNode cls) throws RenderingException {
        try {
            writer.append("function ").appendClass(cls.getName()).append("()").ws().append("{")
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

            writer.append("$rt_declClass(").appendClass(cls.getName()).append(",").ws().append("{")
                    .indent().softNewLine();
            writer.append("name").ws().append(":").ws().append("\"").append(escapeString(cls.getName()))
                    .append("\"");
            if (cls.getModifiers().contains(NodeModifier.ENUM)) {
                writer.append(",").softNewLine().append("enum").ws().append(":").ws().append("true");
            }
            if (!cls.getInterfaces().isEmpty()) {
                writer.append(",").softNewLine().append("interfaces").ws().append(":").ws().append("[");
                for (int i = 0; i < cls.getInterfaces().size(); ++i) {
                    String iface = cls.getInterfaces().get(i);
                    if (i > 0) {
                        writer.append(",").ws();
                    }
                    writer.appendClass(iface);
                }
                writer.append("]");
            }
            if (cls.getParentName() != null) {
                writer.append(",").softNewLine();
                writer.append("superclass").ws().append(":").ws().appendClass(cls.getParentName());
            }
            if (!cls.getModifiers().contains(NodeModifier.INTERFACE)) {
                writer.append(",").softNewLine().append("clinit").ws().append(":").ws()
                        .append("function()").ws().append("{").ws()
                        .appendClass(cls.getName()).append("_$clinit();").ws().append("}");
            }
            writer.ws().append("});").newLine().outdent();
            List<MethodNode> nonInitMethods = new ArrayList<>();
            List<MethodNode> virtualMethods = new ArrayList<>();
            if (!cls.getModifiers().contains(NodeModifier.INTERFACE)) {
                writer.append("function ").appendClass(cls.getName()).append("_$clinit()").ws()
                        .append("{").softNewLine().indent();
                writer.appendClass(cls.getName()).append("_$clinit").ws().append("=").ws()
                        .append("function(){};").newLine();
                List<String> stubNames = new ArrayList<>();
                for (MethodNode method : cls.getMethods()) {
                    if (!method.getModifiers().contains(NodeModifier.STATIC) &&
                            !method.getReference().getName().equals("<init>")) {
                        nonInitMethods.add(method);
                    } else {
                        renderBody(method, true);
                        stubNames.add(naming.getFullNameFor(method.getReference()));
                    }
                }
                MethodHolder methodHolder = classSource.get(cls.getName()).getMethod(
                        new MethodDescriptor("<clinit>", ValueType.VOID));
                if (methodHolder != null) {
                    writer.appendMethodBody(new MethodReference(cls.getName(), methodHolder.getDescriptor()))
                            .append("();").softNewLine();
                }
                writer.outdent().append("}").newLine();
                for (MethodNode method : cls.getMethods()) {
                    cls.getMethods();
                    if (!method.getModifiers().contains(NodeModifier.STATIC)) {
                        virtualMethods.add(method);
                    } else if (method.isOriginalNamePreserved()) {
                        renderStaticDeclaration(method);
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
            }
            for (MethodNode method : nonInitMethods) {
                renderBody(method, false);
            }
            renderVirtualDeclarations(cls.getName(), virtualMethods);
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

    private void renderVirtualDeclarations(String className, List<MethodNode> methods)
            throws NamingException, IOException {
        for (MethodNode method : methods) {
            MethodReference ref = method.getReference();
            if (ref.getDescriptor().getName().equals("<init>")) {
                renderInitializer(method);
            }
        }
        if (methods.isEmpty()) {
            return;
        }
        writer.append("$rt_virtualMethods(").appendClass(className).indent();
        for (MethodNode method : methods) {
            MethodReference ref = method.getReference();
            writer.append(",").newLine();
            if (method.isOriginalNamePreserved()) {
                writer.append("[\"").appendMethod(ref).append("\",").ws().append("\"").append(ref.getName())
                        .append("\"]");
            } else {
                writer.append("\"").appendMethod(ref).append("\"");
            }
            writer.append(",").ws().append("function(");
            for (int i = 1; i <= ref.parameterCount(); ++i) {
                if (i > 1) {
                    writer.append(",").ws();
                }
                writer.append(variableName(i));
            }
            writer.append(")").ws().append("{").ws();
            if (ref.getDescriptor().getResultType() != ValueType.VOID) {
                writer.append("return ");
            }
            writer.appendMethodBody(ref).append("(");
            writer.append("this");
            for (int i = 1; i <= ref.parameterCount(); ++i) {
                writer.append(",").ws().append(variableName(i));
            }
            writer.append(");").ws().append("}");
        }
        writer.append(");").newLine().outdent();
    }

    private void renderStaticDeclaration(MethodNode method) throws NamingException, IOException {
        MethodReference ref = method.getReference();
        if (ref.getDescriptor().getName().equals("<init>")) {
            renderInitializer(method);
        }
        writer.appendClass(ref.getClassName()).append(".").appendMethod(ref).ws().append("=").ws().append("function(");
        for (int i = 0; i < ref.parameterCount(); ++i) {
            if (i > 0) {
                writer.append(", ");
            }
            writer.append(variableName(i + 1));
        }
        writer.append(")").ws().append("{").softNewLine().indent();
        writer.append("return ").appendMethodBody(ref).append("(");
        for (int i = 0; i < ref.parameterCount(); ++i) {
            writer.append(",").ws().append(variableName(i + 1));
        }
        writer.append(");").softNewLine();
        writer.outdent().append("}").newLine();
        if (method.isOriginalNamePreserved()) {
            writer.appendClass(ref.getClassName()).append(".").append(ref.getName()).ws().append("=")
                    .ws().appendClass(ref.getClassName()).append(".").appendMethod(ref).append(';').newLine();
        }
    }

    public void renderBody(MethodNode method, boolean inner) throws IOException {
        MethodReference ref = method.getReference();
        if (inner) {
            writer.appendMethodBody(ref).ws().append("=").ws().append("function(");
        } else {
            writer.append("function ").appendMethodBody(ref).append("(");
        }
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
                int variableCount = 0;
                for (int var : method.getVariables()) {
                    variableCount = Math.max(variableCount, var + 1);
                }
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

        @Override
        public ListableClassReaderSource getClassSource() {
            return classSource;
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
            while (true) {
                writer.append("if").ws().append("(");
                statement.getCondition().acceptVisitor(this);
                writer.append(")").ws().append("{").softNewLine().indent();
                for (Statement part : statement.getConsequent()) {
                    part.acceptVisitor(this);
                }
                if (!statement.getAlternative().isEmpty()) {
                    writer.outdent().append("}").ws();
                    if (statement.getAlternative().size() == 1 &&
                            statement.getAlternative().get(0) instanceof ConditionalStatement) {
                        statement = (ConditionalStatement)statement.getAlternative().get(0);
                        writer.append("else ");
                        continue;
                    }
                    writer.append("else").ws().append("{").indent().softNewLine();
                    for (Statement part : statement.getAlternative()) {
                        part.acceptVisitor(this);
                    }
                }
                break;
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
                for (Statement part : clause.getBody()) {
                    part.acceptVisitor(this);
                }
                writer.outdent();
            }
            if (statement.getDefaultClause() != null) {
                writer.append("default:").softNewLine().indent();
                for (Statement part : statement.getDefaultClause()) {
                    part.acceptVisitor(this);
                }
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
            writer.append("break");
            if (statement.getTarget() != null) {
                writer.append(' ').append(statement.getTarget().getId());
            }
            writer.append(";").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        try {
            writer.append("continue");
            if (statement.getTarget() != null) {
                writer.append(' ').append(statement.getTarget().getId());
            }
            writer.append(";").softNewLine();
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

    @Override
    public void visit(InitClassStatement statement) {
        try {
            writer.appendClass(statement.getClassName()).append("_$clinit();").softNewLine();
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
                visitBinaryFunction(expr, "Long_shl");
                break;
            case RIGHT_SHIFT:
                visitBinary(expr, ">>");
                break;
            case RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_shr");
                break;
            case UNSIGNED_RIGHT_SHIFT:
                visitBinary(expr, ">>>");
                break;
            case UNSIGNED_RIGHT_SHIFT_LONG:
                visitBinaryFunction(expr, "Long_shru");
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
                case NULL_CHECK:
                    writer.append("$rt_nullCheck(");
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
        } else if (cst instanceof Character) {
            return Integer.toString((Character)cst);
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
            Injector injector = getInjector(expr.getMethod());
            if (injector != null) {
                injector.generate(new InjectorContextImpl(expr.getArguments()), expr.getMethod());
                return;
            }
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
                writer.append("$rt_createArray(").append(typeToClsString(naming, expr.getType())).append(",").ws();
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
            ValueType type = expr.getType();
            for (int i = 0; i < expr.getDimensions().size(); ++i) {
                type = ((ValueType.Array)type).getItemType();
            }
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive)type).getKind()) {
                    case BOOLEAN:
                        writer.append("$rt_createBooleanMultiArray(");
                        break;
                    case BYTE:
                        writer.append("$rt_createByteMultiArray(");
                        break;
                    case SHORT:
                        writer.append("$rt_createShortMultiArray(");
                        break;
                    case INTEGER:
                        writer.append("$rt_createIntMultiArray(");
                        break;
                    case LONG:
                        writer.append("$rt_createLongMultiArray(");
                        break;
                    case FLOAT:
                        writer.append("$rt_createFloatMultiArray(");
                        break;
                    case DOUBLE:
                        writer.append("$rt_createDoubleMultiArray(");
                        break;
                    case CHARACTER:
                        writer.append("$rt_createCharMultiArray(");
                        break;
                }
            } else {
                writer.append("$rt_createMultiArray(").append(typeToClsString(naming, expr.getType()))
                        .append(",").ws();
            }
            writer.append("[");
            boolean first = true;
            List<Expr> dimensions = new ArrayList<>(expr.getDimensions());
            Collections.reverse(dimensions);
            for (Expr dimension : dimensions) {
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
                ClassHolder cls = classSource.get(clsName);
                if (cls != null && !cls.getModifiers().contains(ElementModifier.INTERFACE)) {
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

    @Override
    public void visit(TryCatchStatement statement) {
        try {
            writer.append("try").ws().append("{").softNewLine().indent();
            List<TryCatchStatement> sequence = new ArrayList<>();
            sequence.add(statement);
            List<Statement> protectedBody = statement.getProtectedBody();
            while (protectedBody.size() == 1 && protectedBody.get(0) instanceof TryCatchStatement) {
                TryCatchStatement nextStatement = (TryCatchStatement)protectedBody.get(0);
                sequence.add(nextStatement);
                protectedBody = nextStatement.getProtectedBody();
            }
            for (Statement part : protectedBody) {
                part.acceptVisitor(this);
            }
            writer.outdent().append("}").ws().append("catch").ws().append("($e)")
                    .ws().append("{").indent().softNewLine();
            writer.append("$je").ws().append("=").ws().append("$e.$javaException;").softNewLine();
            for (TryCatchStatement catchClause : sequence) {
                writer.append("if").ws().append("($je");
                if (catchClause.getExceptionType() != null) {
                    writer.ws().append("&&").ws().append("$je instanceof ")
                            .appendClass(catchClause.getExceptionType());
                }
                writer.append(")").ws().append("{").indent().softNewLine();
                if (catchClause.getExceptionVariable() != null) {
                    writer.append(variableName(catchClause.getExceptionVariable())).ws().append("=").ws()
                            .append("$je;").softNewLine();
                }
                for (Statement part : catchClause.getHandler()) {
                    part.acceptVisitor(this);
                }
                writer.outdent().append("}").ws().append("else ");
            }
            writer.append("{").indent().softNewLine();
            writer.append("throw $e;").softNewLine();
            writer.outdent().append("}").softNewLine();
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private Injector getInjector(MethodReference ref) {
        InjectorHolder holder = injectorMap.get(ref);
        if (holder == null) {
            MethodHolder method = classSource.get(ref.getClassName()).getMethod(ref.getDescriptor());
            holder = new InjectorHolder(null);
            if (method != null) {
                AnnotationHolder injectedByAnnot = method.getAnnotations().get(InjectedBy.class.getName());
                if (injectedByAnnot != null) {
                    ValueType type = injectedByAnnot.getValues().get("value").getJavaClass();
                    holder = new InjectorHolder(instantiateInjector(((ValueType.Object)type).getClassName()));
                }
            }
            injectorMap.put(ref, holder);
        }
        return holder.injector;
    }

    private Injector instantiateInjector(String type) {
        try {
            Class<? extends Injector> cls = Class.forName(type, true, classLoader).asSubclass(Injector.class);
            Constructor<? extends Injector> cons = cls.getConstructor();
            return cons.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Illegal injector: " + type, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Default constructor was not found in the " + type + " injector", e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error instantiating injector " + type, e);
        }
    }

    private class InjectorContextImpl implements InjectorContext {
        private List<Expr> arguments;

        public InjectorContextImpl(List<Expr> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Expr getArgument(int index) {
            return arguments.get(index);
        }

        @Override
        public boolean isMinifying() {
            return minifying;
        }

        @Override
        public SourceWriter getWriter() {
            return writer;
        }

        @Override
        public void writeEscaped(String str) throws IOException {
            writer.append(escapeString(str));
        }

        @Override
        public void writeType(ValueType type) throws IOException {
            writer.append(typeToClsString(naming, type));
        }

        @Override
        public void writeExpr(Expr expr) throws IOException {
            expr.acceptVisitor(Renderer.this);
        }

        @Override
        public int argumentCount() {
            return arguments.size();
        }
    }
}
