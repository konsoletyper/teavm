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
package org.teavm.jso.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayComprehension;
import org.mozilla.javascript.ast.ArrayComprehensionLoop;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.GeneratorExpression;
import org.mozilla.javascript.ast.GeneratorExpressionLoop;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Label;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.LetNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.model.MethodReference;

public class AstWriter {
    public static final int PRECEDENCE_MEMBER = 2;
    public static final int PRECEDENCE_FUNCTION = 3;
    public static final int PRECEDENCE_POSTFIX = 4;
    public static final int PRECEDENCE_PREFIX = 5;
    public static final int PRECEDENCE_MUL = 6;
    public static final int PRECEDENCE_ADD = 7;
    public static final int PRECEDENCE_SHIFT = 8;
    public static final int PRECEDENCE_RELATION = 9;
    public static final int PRECEDENCE_EQUALITY = 10;
    public static final int PRECEDENCE_BITWISE_AND = 11;
    public static final int PRECEDENCE_BITWISE_XOR = 12;
    public static final int PRECEDENCE_BITWISE_OR = 13;
    public static final int PRECEDENCE_AND = 14;
    public static final int PRECEDENCE_OR = 15;
    public static final int PRECEDENCE_COND = 16;
    public static final int PRECEDENCE_ASSIGN = 17;
    public static final int PRECEDENCE_COMMA = 18;
    private SourceWriter writer;
    private Map<String, NameEmitter> nameMap = new HashMap<>();
    private Set<String> aliases = new HashSet<>();

    public AstWriter(SourceWriter writer) {
        this.writer = writer;
    }

    private void declareName(String name) {
        if (nameMap.containsKey(name)) {
            return;
        }
        if (aliases.add(name)) {
            nameMap.put(name, p -> writer.append(name));
            return;
        }
        for (int i = 0;; ++i) {
            String alias = name + "_" + i;
            if (aliases.add(alias)) {
                nameMap.put(name, p -> writer.append(alias));
                return;
            }
        }
    }

    public void declareNameEmitter(String name, NameEmitter emitter) {
        nameMap.put(name, emitter);
    }

    public void hoist(AstNode node) {
        node.visit(n -> {
            if (n instanceof Scope) {
                Scope scope = (Scope) n;
                if (scope.getSymbolTable() != null) {
                    for (String name : scope.getSymbolTable().keySet()) {
                        declareName(name);
                    }
                }
            }
            return true;
        });
    }

    public void print(AstNode node) throws IOException {
        print(node, PRECEDENCE_COMMA);
    }

    public void print(AstNode node, int precedence) throws IOException {
        switch (node.getType()) {
            case Token.SCRIPT:
                print((AstRoot) node);
                break;
            case Token.CALL:
            case Token.NEW:
                print((FunctionCall) node, precedence);
                break;
            case Token.FUNCTION:
                print((FunctionNode) node);
                break;
            case Token.ARRAYCOMP:
                print((ArrayComprehension) node);
                break;
            case Token.GETPROP:
                print((PropertyGet) node);
                break;
            case Token.GENEXPR:
                print((GeneratorExpression) node);
                break;
            case Token.NUMBER:
                print((NumberLiteral) node);
                break;
            case Token.STRING:
                print((StringLiteral) node);
                break;
            case Token.TRUE:
                writer.append("true");
                break;
            case Token.FALSE:
                writer.append("false");
                break;
            case Token.THIS:
                if (nameMap.containsKey("this")) {
                    nameMap.get("this").emit(precedence);
                } else {
                    writer.append("this");
                }
                break;
            case Token.NULL:
                writer.append("null");
                break;
            case Token.NAME:
                print((Name) node, precedence);
                break;
            case Token.REGEXP:
                print((RegExpLiteral) node);
                break;
            case Token.OBJECTLIT:
                print((ObjectLiteral) node);
                break;
            case Token.ARRAYLIT:
                print((ArrayLiteral) node);
                break;
            case Token.BLOCK:
                if (node instanceof Block) {
                    print((Block) node);
                } else if (node instanceof Scope) {
                    print((Scope) node);
                }
                break;
            case Token.HOOK:
                print((ConditionalExpression) node, precedence);
                break;
            case Token.GETELEM:
                print((ElementGet) node);
                break;
            case Token.LETEXPR:
                print((LetNode) node);
                break;
            case Token.LP:
                print((ParenthesizedExpression) node, precedence);
                break;
            case Token.EMPTY:
                if (node instanceof EmptyStatement) {
                    writer.append(';');
                }
                break;
            case Token.EXPR_VOID:
            case Token.EXPR_RESULT:
                if (node instanceof ExpressionStatement) {
                    print((ExpressionStatement) node);
                } else if (node instanceof LabeledStatement) {
                    print((LabeledStatement) node);
                }
                break;
            case Token.BREAK:
                print((BreakStatement) node);
                break;
            case Token.CONTINUE:
                print((ContinueStatement) node);
                break;
            case Token.RETURN:
                print((ReturnStatement) node);
                break;
            case Token.DO:
                print((DoLoop) node);
                break;
            case Token.FOR:
                if (node instanceof ForInLoop) {
                    print((ForInLoop) node);
                } else if (node instanceof ForLoop) {
                    print((ForLoop) node);
                }
                break;
            case Token.IF:
                print((IfStatement) node);
                break;
            case Token.SWITCH:
                print((SwitchStatement) node);
                break;
            case Token.THROW:
                print((ThrowStatement) node);
                break;
            case Token.TRY:
                print((TryStatement) node);
                break;
            case Token.CONST:
            case Token.VAR:
            case Token.LET:
                print((VariableDeclaration) node);
                break;
            case Token.WHILE:
                print((WhileLoop) node);
                break;
            default:
                if (node instanceof InfixExpression) {
                    printInfix((InfixExpression) node, precedence);
                } else if (node instanceof UnaryExpression) {
                    printUnary((UnaryExpression) node, precedence);
                }
                break;
        }
    }

    private void print(AstRoot node) throws IOException {
        for (Node child : node) {
            print((AstNode) child);
        }
    }

    private void print(Block node) throws IOException {
        writer.append('{').softNewLine().indent();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            print((AstNode) child);
            writer.softNewLine();
        }
        writer.outdent().append('}');
    }

    private void print(Scope node) throws IOException {
        writer.append('{').softNewLine().indent();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            print((AstNode) child);
            writer.softNewLine();
        }
        writer.outdent().append('}');
    }

    private void print(LabeledStatement node) throws IOException {
        for (Label label : node.getLabels()) {
            writer.append(label.getName()).append(':').ws();
        }
        print(node.getStatement());
    }

    private void print(BreakStatement node) throws IOException {
        writer.append("break");
        if (node.getBreakLabel() != null) {
            writer.append(' ').append(node.getBreakLabel().getString());
        }
        writer.append(';');
    }

    private void print(ContinueStatement node) throws IOException {
        writer.append("continue");
        if (node.getLabel() != null) {
            writer.append(' ').append(node.getLabel().getString());
        }
        writer.append(';');
    }

    private void print(ReturnStatement node) throws IOException {
        writer.append("return");
        if (node.getReturnValue() != null) {
            writer.append(' ');
            print(node.getReturnValue());
        }
        writer.append(';');
    }

    private void print(ThrowStatement node) throws IOException {
        writer.append("throw ");
        print(node.getExpression());
        writer.append(';');
    }

    private void print(DoLoop node) throws IOException {
        writer.append("do ").ws();
        print(node.getBody());
        writer.append("while").ws().append('(');
        print(node.getCondition());
        writer.append(");");
    }

    private void print(ForInLoop node) throws IOException {
        writer.append("for");
        if (node.isForEach()) {
            writer.append(" each");
        }
        writer.ws().append("(");
        print(node.getIterator());
        writer.append(" in ");
        print(node.getIteratedObject());
        writer.append(')').ws();
        print(node.getBody());
    }

    private void print(ForLoop node) throws IOException {
        writer.append("for").ws().append('(');
        print(node.getInitializer());
        writer.append(';');
        print(node.getCondition());
        writer.append(';');
        print(node.getIncrement());
        writer.append(')').ws();
        print(node.getBody());
    }

    private void print(WhileLoop node) throws IOException {
        writer.append("while").ws().append('(');
        print(node.getCondition());
        writer.append(')').ws();
        print(node.getBody());
    }

    private void print(IfStatement node) throws IOException {
        writer.append("if").ws().append('(');
        print(node.getCondition());
        writer.append(')').ws();
        print(node.getThenPart());
        if (node.getElsePart() != null) {
            writer.ws().append("else ");
            print(node.getElsePart());
        }
    }

    private void print(SwitchStatement node) throws IOException {
        writer.append("switch").ws().append('(');
        print(node.getExpression());
        writer.append(')').ws().append('{').indent().softNewLine();
        for (SwitchCase sc : node.getCases()) {
            if (sc.getExpression() == null) {
                writer.append("default:");
            } else {
                writer.append("case ");
                print(sc.getExpression());
                writer.append(':');
            }
            writer.indent().softNewLine();
            if (sc.getStatements() != null) {
                for (AstNode stmt : sc.getStatements()) {
                    print(stmt);
                    writer.softNewLine();
                }
            }
            writer.outdent();
        }
        writer.append('}');
    }

    private void print(TryStatement node) throws IOException {
        writer.append("try ");
        print(node.getTryBlock());
        for (CatchClause cc : node.getCatchClauses()) {
            writer.ws().append("catch").ws().append('(');
            print(cc.getVarName());
            if (cc.getCatchCondition() != null) {
                writer.append(" if ");
                print(cc.getCatchCondition());
            }
            writer.append(')');
            print(cc.getBody());
        }
        if (node.getFinallyBlock() != null) {
            writer.ws().append("finally ");
            print(node.getFinallyBlock());
        }
    }

    private void print(VariableDeclaration node) throws IOException {
        switch (node.getType()) {
            case Token.VAR:
                writer.append("var ");
                break;
            case Token.LET:
                writer.append("let ");
                break;
            case Token.CONST:
                writer.append("const ");
                break;
            default:
                break;
        }
        print(node.getVariables().get(0));
        for (int i = 1; i < node.getVariables().size(); ++i) {
            writer.append(',').ws();
            print(node.getVariables().get(i));
        }
        if (node.isStatement()) {
            writer.append(';');
        }
    }

    private void print(VariableInitializer node) throws IOException {
        print(node.getTarget());
        if (node.getInitializer() != null) {
            writer.ws().append('=').ws();
            print(node.getInitializer());
        }
    }

    private void print(ExpressionStatement node) throws IOException {
        print(node.getExpression());
        writer.append(';');
    }

    private void print(ElementGet node) throws IOException {
        print(node.getTarget(), PRECEDENCE_MEMBER);
        writer.append('[');
        print(node.getElement());
        writer.append(']');
    }

    private void print(PropertyGet node) throws IOException {
        print(node.getLeft(), PRECEDENCE_MEMBER);
        writer.append('.');
        Map<String, NameEmitter> oldNameMap = nameMap;
        nameMap = Collections.emptyMap();
        print(node.getRight());
        nameMap = oldNameMap;
    }

    private void print(FunctionCall node, int precedence) throws IOException {
        if (tryJavaInvocation(node)) {
            return;
        }

        if (precedence < PRECEDENCE_FUNCTION) {
            writer.append('(');
        }
        int innerPrecedence = node instanceof NewExpression ? PRECEDENCE_FUNCTION - 1 : PRECEDENCE_FUNCTION;
        if (node instanceof NewExpression) {
            writer.append("new ");
        }
        print(node.getTarget(), innerPrecedence);
        writer.append('(');
        printList(node.getArguments());
        writer.append(')');
        if (node instanceof NewExpression) {
            writer.ws();
            NewExpression newExpr = (NewExpression) node;
            if (newExpr.getInitializer() != null) {
                print(newExpr.getInitializer());
            }
        }
        if (precedence < PRECEDENCE_FUNCTION) {
            writer.append(')');
        }
    }

    private boolean tryJavaInvocation(FunctionCall node) throws IOException {
        if (!(node.getTarget() instanceof PropertyGet)) {
            return false;
        }

        PropertyGet propertyGet = (PropertyGet) node.getTarget();
        String callMethod = getJavaMethod(propertyGet.getTarget());
        if (callMethod == null || !propertyGet.getProperty().getIdentifier().equals("invoke")) {
            return false;
        }

        MethodReference method = MethodReference.parseIfPossible(callMethod);
        if (method == null) {
            return false;
        }

        writer.appendMethodBody(method).append('(');
        printList(node.getArguments());
        writer.append(')');
        return true;
    }

    private String getJavaMethod(AstNode node) {
        if (!(node instanceof StringLiteral)) {
            return null;
        }
        String str = ((StringLiteral) node).getValue();
        if (!str.startsWith("$$JSO$$_")) {
            return null;
        }
        return str.substring("$$JSO$$_".length());
    }

    private void print(ConditionalExpression node, int precedence) throws IOException {
        if (precedence < PRECEDENCE_COND) {
            writer.append('(');
        }
        print(node.getTestExpression(), PRECEDENCE_COND - 1);
        writer.ws().append('?').ws();
        print(node.getTrueExpression(), PRECEDENCE_COND - 1);
        writer.ws().append(':').ws();
        print(node.getFalseExpression(), PRECEDENCE_COND);
        if (precedence < PRECEDENCE_COND) {
            writer.append(')');
        }
    }

    private void printList(List<? extends AstNode> nodes) throws IOException {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        print(nodes.get(0));
        for (int i = 1; i < nodes.size(); ++i) {
            writer.append(',').ws();
            print(nodes.get(i));
        }
    }

    private void print(ArrayComprehension node) throws IOException {
        writer.append("[");
        for (ArrayComprehensionLoop loop : node.getLoops()) {
            writer.append("for").ws().append("(");
            print(loop.getIterator());
            writer.append(" of ");
            print(loop.getIteratedObject());
            writer.append(')');
        }
        if (node.getFilter() != null) {
            writer.append("if").ws().append("(");
            print(node.getFilter());
            writer.append(")");
        }
        print(node.getResult());
        writer.append(']');
    }

    private void print(GeneratorExpression node) throws IOException {
        writer.append("(");
        for (GeneratorExpressionLoop loop : node.getLoops()) {
            writer.append("for").ws().append("(");
            print(loop.getIterator());
            writer.append(" of ");
            print(loop.getIteratedObject());
            writer.append(')');
        }
        if (node.getFilter() != null) {
            writer.append("if").ws().append("(");
            print(node.getFilter());
            writer.append(")");
        }
        print(node.getResult());
        writer.append(')');
    }

    private void print(NumberLiteral node) throws IOException {
        writer.append(node.getValue());
    }

    private void print(StringLiteral node) throws IOException {
        writer.append(node.getQuoteCharacter());
        writer.append(ScriptRuntime.escapeString(node.getValue(), node.getQuoteCharacter()));
        writer.append(node.getQuoteCharacter());
    }

    private void print(Name node, int precedence) throws IOException {
        NameEmitter alias = nameMap.get(node.getIdentifier());
        if (alias == null) {
            alias = prec -> writer.append(node.getIdentifier());
        }
        alias.emit(precedence);
    }

    private void print(RegExpLiteral node) throws IOException {
        writer.append('/').append(node.getValue()).append('/').append(node.getFlags());
    }

    private void print(ArrayLiteral node) throws IOException {
        writer.append('[');
        printList(node.getElements());
        writer.append(']');
    }

    private void print(ObjectLiteral node) throws IOException {
        writer.append('{').ws();
        if (node.getElements() != null && !node.getElements().isEmpty()) {
            print(node.getElements().get(0));
            for (int i = 1; i < node.getElements().size(); ++i) {
                writer.append(',').ws();
                print(node.getElements().get(i));
            }
        }
        writer.ws().append('}');
    }

    private void print(ObjectProperty node) throws IOException {
        if (node.isGetterMethod()) {
            writer.append("get ");
        } else if (node.isSetterMethod()) {
            writer.append("set ");
        }
        Map<String, NameEmitter> oldNameMap = nameMap;
        nameMap = Collections.emptyMap();
        print(node.getLeft());
        nameMap = oldNameMap;
        if (!node.isMethod()) {
            writer.ws().append(':').ws();
        }
        print(node.getRight());
    }

    private void print(FunctionNode node) throws IOException {
        if (!node.isMethod()) {
            writer.append("function");
        }
        if (node.getFunctionName() != null) {
            writer.append(' ');
            print(node.getFunctionName());
        }
        writer.append('(');
        printList(node.getParams());
        writer.append(')').ws();

        if (node.isExpressionClosure()) {
            if (node.getBody().getLastChild() instanceof ReturnStatement) {
                print(((ReturnStatement) node.getBody().getLastChild()).getReturnValue());
                if (node.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
                    writer.append(";");
                }
            }
        } else {
            print(node.getBody());
        }
    }

    private void print(LetNode node) throws IOException {
        writer.append("let").ws().append('(');
        printList(node.getVariables().getVariables());
        writer.append(')');
        writer.append(node.getBody());
    }

    private void print(ParenthesizedExpression node, int precedence) throws IOException {
        print(node.getExpression(), precedence);
    }

    private void printUnary(UnaryExpression node, int precedence) throws IOException {
        int innerPrecedence = node.isPostfix() ? PRECEDENCE_POSTFIX : PRECEDENCE_PREFIX;

        if (innerPrecedence > precedence) {
            writer.append('(');
        }

        if (!node.isPostfix()) {
            String op = AstNode.operatorToString(node.getType());
            if (op.startsWith("-")) {
                writer.append(' ');
            }
            writer.append(op);
            if (requiresWhitespaces(node.getType())) {
                writer.append(' ');
            }
        }

        print(node.getOperand(), innerPrecedence);

        if (node.isPostfix()) {
            writer.append(AstNode.operatorToString(node.getType()));
        }

        if (innerPrecedence > precedence) {
            writer.append(')');
        }
    }

    private void printInfix(InfixExpression node, int precedence) throws IOException {
        int innerPrecedence = getPrecedence(node.getType());

        if (innerPrecedence > precedence) {
            writer.append('(');
        }

        int leftPrecedence;
        switch (node.getType()) {
            case Token.ASSIGN:
            case Token.ASSIGN_ADD:
            case Token.ASSIGN_SUB:
            case Token.ASSIGN_MUL:
            case Token.ASSIGN_DIV:
            case Token.ASSIGN_MOD:
            case Token.ASSIGN_BITAND:
            case Token.ASSIGN_BITXOR:
            case Token.ASSIGN_BITOR:
            case Token.ASSIGN_LSH:
            case Token.ASSIGN_RSH:
            case Token.ASSIGN_URSH:
                leftPrecedence = innerPrecedence - 1;
                break;
            default:
                leftPrecedence = innerPrecedence;
        }
        print(node.getLeft(), leftPrecedence);

        String op = AstNode.operatorToString(node.getType());
        boolean ws = requiresWhitespaces(node.getType());
        if (ws || op.startsWith("-")) {
            writer.append(' ');
        } else {
            writer.ws();
        }
        writer.append(op);
        if (ws) {
            writer.append(' ');
        } else {
            writer.ws();
        }

        int rightPrecedence;
        switch (node.getType()) {
            case Token.DIV:
            case Token.MOD:
            case Token.SUB:
            case Token.LSH:
            case Token.RSH:
            case Token.URSH:
            case Token.IN:
            case Token.INSTANCEOF:
            case Token.EQ:
            case Token.NE:
            case Token.SHEQ:
            case Token.SHNE:
            case Token.GT:
            case Token.GE:
            case Token.LT:
            case Token.LE:
                rightPrecedence = innerPrecedence - 1;
                break;
            default:
                rightPrecedence = innerPrecedence;
        }
        print(node.getRight(), rightPrecedence);

        if (innerPrecedence > precedence) {
            writer.append(')');
        }
    }

    private int getPrecedence(int token) {
        switch (token) {
            case Token.MUL:
            case Token.DIV:
            case Token.MOD:
                return PRECEDENCE_MUL;
            case Token.ADD:
            case Token.SUB:
                return PRECEDENCE_ADD;
            case Token.LSH:
            case Token.RSH:
            case Token.URSH:
                return PRECEDENCE_SHIFT;
            case Token.LT:
            case Token.LE:
            case Token.GT:
            case Token.GE:
            case Token.IN:
            case Token.INSTANCEOF:
                return PRECEDENCE_RELATION;
            case Token.EQ:
            case Token.NE:
            case Token.SHEQ:
            case Token.SHNE:
                return PRECEDENCE_EQUALITY;
            case Token.BITAND:
                return PRECEDENCE_BITWISE_AND;
            case Token.BITXOR:
                return PRECEDENCE_BITWISE_XOR;
            case Token.BITOR:
                return PRECEDENCE_BITWISE_OR;
            case Token.AND:
                return PRECEDENCE_AND;
            case Token.OR:
                return PRECEDENCE_OR;
            case Token.ASSIGN:
            case Token.ASSIGN_ADD:
            case Token.ASSIGN_SUB:
            case Token.ASSIGN_MUL:
            case Token.ASSIGN_DIV:
            case Token.ASSIGN_MOD:
            case Token.ASSIGN_BITAND:
            case Token.ASSIGN_BITXOR:
            case Token.ASSIGN_BITOR:
            case Token.ASSIGN_LSH:
            case Token.ASSIGN_RSH:
            case Token.ASSIGN_URSH:
                return PRECEDENCE_ASSIGN;
            default:
                return PRECEDENCE_COMMA;
        }
    }

    private boolean requiresWhitespaces(int token) {
        switch (token) {
            case Token.IN:
            case Token.TYPEOF:
            case Token.INSTANCEOF:
            case Token.VOID:
            case Token.DEL_REF:
            case Token.DELPROP:
                return true;
            default:
                return false;
        }
    }
}
