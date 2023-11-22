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
package org.teavm.backend.javascript.rendering;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
import org.mozilla.javascript.ast.UpdateExpression;
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
    protected final SourceWriter writer;
    private Map<String, NameEmitter> nameMap = new HashMap<>();
    protected boolean rootScope = true;
    private Set<String> aliases = new HashSet<>();
    private Function<String, NameEmitter> globalNameWriter;
    public final Map<String, Scope> currentScopes = new HashMap<>();

    public AstWriter(SourceWriter writer, Function<String, NameEmitter> globalNameWriter) {
        this.writer = writer;
        this.globalNameWriter = globalNameWriter;
    }

    public void declareName(String name) {
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

    public void hoist(Object node) {
        hoist((AstNode) node);
    }

    public void hoist(AstNode node) {
        declareName("arguments");
        node.visit(n -> {
            if (n instanceof Scope) {
                var scope = (Scope) n;
                if (scope.getSymbolTable() != null) {
                    for (var name : scope.getSymbolTable().keySet()) {
                        declareName(name);
                    }
                }
            } else if (n instanceof CatchClause) {
                var clause = (CatchClause) n;
                var name = clause.getVarName().getIdentifier();
                declareName(name);
            } else if (n instanceof VariableInitializer) {
                var initializer = (VariableInitializer) n;
                if (initializer.getTarget() instanceof Name) {
                    var id = ((Name) initializer.getTarget()).getIdentifier();
                    declareName(id);
                }
            }
            return true;
        });
    }

    public void print(Object node) {
        print((AstNode) node);
    }

    public void print(Object node, int precedence) {
        print((AstNode) node, precedence);
    }

    public void print(AstNode node) {
        print(node, PRECEDENCE_COMMA);
    }

    public void print(AstNode node, int precedence) {
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
                } else if (node instanceof UpdateExpression) {
                    printUnary((UpdateExpression) node, precedence);
                }
                break;
        }
    }

    private void print(AstRoot node) {
        for (Node child : node) {
            print((AstNode) child);
            writer.softNewLine();
        }
    }

    private void print(Block node) {
        writer.append('{').softNewLine().indent();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            print((AstNode) child);
            writer.softNewLine();
        }
        writer.outdent().append('}');
    }

    private void print(Scope node) {
        var scope = enterScope(node);
        writer.append('{').softNewLine().indent();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            print((AstNode) child);
            writer.softNewLine();
        }
        writer.outdent().append('}');
        leaveScope(scope, node);
    }

    private void print(LabeledStatement node) {
        for (Label label : node.getLabels()) {
            writer.append(label.getName()).append(':').ws();
        }
        print(node.getStatement());
    }

    private void print(BreakStatement node) {
        writer.append("break");
        if (node.getBreakLabel() != null) {
            writer.append(' ').append(node.getBreakLabel().getString());
        }
        writer.append(';');
    }

    private void print(ContinueStatement node) {
        writer.append("continue");
        if (node.getLabel() != null) {
            writer.append(' ').append(node.getLabel().getString());
        }
        writer.append(';');
    }

    private void print(ReturnStatement node) {
        writer.append("return");
        if (node.getReturnValue() != null) {
            writer.append(' ');
            print(node.getReturnValue());
        }
        writer.append(';');
    }

    private void print(ThrowStatement node) {
        writer.append("throw ");
        print(node.getExpression());
        writer.append(';');
    }

    private void print(DoLoop node) {
        var scope = enterScope(node);
        writer.append("do ").ws();
        print(node.getBody());
        writer.append("while").ws().append('(');
        print(node.getCondition());
        writer.append(");");
        leaveScope(scope, node);
    }

    private void print(ForInLoop node) {
        var scope = enterScope(node);
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
        leaveScope(scope, node);
    }

    private void print(ForLoop node) {
        var scope = enterScope(node);
        writer.append("for").ws().append('(');
        print(node.getInitializer());
        writer.append(';');
        print(node.getCondition());
        writer.append(';');
        print(node.getIncrement());
        writer.append(')').ws();
        print(node.getBody());
        leaveScope(scope, node);
    }

    private void print(WhileLoop node) {
        var scope = enterScope(node);
        writer.append("while").ws().append('(');
        print(node.getCondition());
        writer.append(')').ws();
        print(node.getBody());
        leaveScope(scope, node);
    }

    private void print(IfStatement node) {
        writer.append("if").ws().append('(');
        print(node.getCondition());
        writer.append(')').ws();
        print(node.getThenPart());
        if (node.getElsePart() != null) {
            writer.ws().append("else ");
            print(node.getElsePart());
        }
    }

    private void print(SwitchStatement node) {
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
        writer.outdent().append('}');
    }

    private void print(TryStatement node) {
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

    private void print(VariableDeclaration node) {
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

    private void print(VariableInitializer node) {
        print(node.getTarget());
        if (node.getInitializer() != null) {
            writer.ws().append('=').ws();
            print(node.getInitializer());
        }
    }

    private void print(ExpressionStatement node) {
        print(node.getExpression());
        writer.append(';');
    }

    protected void print(ElementGet node) {
        print(node.getTarget(), PRECEDENCE_MEMBER);
        writer.append('[');
        print(node.getElement());
        writer.append(']');
    }

    public void print(PropertyGet node) {
        print(node.getLeft(), PRECEDENCE_MEMBER);
        writer.append('.');
        var oldRootScope = rootScope;
        rootScope = false;
        print(node.getRight());
        rootScope = oldRootScope;
    }

    private void print(FunctionCall node, int precedence) {
        if (intrinsic(node, precedence)) {
            return;
        }

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
            NewExpression newExpr = (NewExpression) node;
            if (newExpr.getInitializer() != null) {
                writer.ws();
                print(newExpr.getInitializer());
            }
        }
        if (precedence < PRECEDENCE_FUNCTION) {
            writer.append(')');
        }
    }

    protected boolean intrinsic(FunctionCall node, int precedence) {
        return false;
    }

    private boolean tryJavaInvocation(FunctionCall node) {
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

    private void print(ConditionalExpression node, int precedence) {
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

    private void printList(List<? extends AstNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        print(nodes.get(0));
        for (int i = 1; i < nodes.size(); ++i) {
            writer.append(',').ws();
            print(nodes.get(i));
        }
    }

    private void print(ArrayComprehension node) {
        var scope = enterScope(node);
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
        leaveScope(scope, node);
    }

    private void print(GeneratorExpression node) {
        var scope = enterScope(node);
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
        leaveScope(scope, node);
    }

    private void print(NumberLiteral node) {
        writer.append(node.getValue());
    }

    private void print(StringLiteral node) {
        writer.append(node.getQuoteCharacter());
        writer.append(ScriptRuntime.escapeString(node.getValue(), node.getQuoteCharacter()));
        writer.append(node.getQuoteCharacter());
    }

    public void print(Name node, int precedence) {
        var definingScope = scopeOfId(node.getIdentifier());
        if (rootScope && definingScope == null) {
            var alias = nameMap.get(node.getIdentifier());
            if (alias == null) {
                if (globalNameWriter != null) {
                    alias = globalNameWriter.apply(node.getIdentifier());
                } else {
                    alias = prec -> writer.append(node.getIdentifier());
                }
            }
            alias.emit(precedence);
        } else {
            writer.append(node.getIdentifier());
        }
    }

    private void print(RegExpLiteral node) {
        writer.append('/').append(node.getValue()).append('/').append(node.getFlags());
    }

    private void print(ArrayLiteral node) {
        writer.append('[');
        printList(node.getElements());
        writer.append(']');
    }

    private void print(ObjectLiteral node) {
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

    private void print(ObjectProperty node) {
        if (node.isGetterMethod()) {
            writer.append("get ");
        } else if (node.isSetterMethod()) {
            writer.append("set ");
        }
        var oldRootScope = rootScope;
        rootScope = false;
        print(node.getLeft());
        rootScope = oldRootScope;
        if (!node.isMethod()) {
            writer.ws().append(':').ws();
        }
        print(node.getRight());
    }

    protected void print(FunctionNode node) {
        var scope = enterScope(node);
        var isArrow = node.getFunctionType() == FunctionNode.ARROW_FUNCTION;
        if (!isArrow) {
            currentScopes.put("arguments", node);
        }
        if (!node.isMethod() && !isArrow) {
            writer.append("function");
        }
        if (node.getFunctionName() != null) {
            writer.append(' ');
            print(node.getFunctionName());
        }
        if (!isArrow || node.getParams().size() != 1) {
            writer.append('(');
            printList(node.getParams());
            writer.append(')');
        } else {
            print(node.getParams().get(0));
        }
        if (isArrow) {
            writer.sameLineWs().append("=>").ws();
        } else {
            writer.ws();
        }

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

        leaveScope(scope, node);
    }

    private void print(LetNode node) {
        var scope = enterScope(node);
        writer.append("let").ws().append('(');
        printList(node.getVariables().getVariables());
        writer.append(')');
        print(node.getBody());
        leaveScope(scope, node);
    }

    private void print(ParenthesizedExpression node, int precedence) {
        print(node.getExpression(), precedence);
    }

    private void printUnary(UnaryExpression node, int precedence) {
        int innerPrecedence = PRECEDENCE_PREFIX;

        if (innerPrecedence > precedence) {
            writer.append('(');
        }

        String op = AstNode.operatorToString(node.getType());
        if (op.startsWith("-")) {
            writer.append(' ');
        }
        writer.append(op);
        if (requiresWhitespaces(node.getType())) {
            writer.append(' ');
        }

        print(node.getOperand(), innerPrecedence);

        if (innerPrecedence > precedence) {
            writer.append(')');
        }
    }

    private void printUnary(UpdateExpression node, int precedence) {
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

    private void printInfix(InfixExpression node, int precedence) {
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

    private Map<String, Scope> enterScope(Scope scope) {
        if (scope.getSymbolTable() == null) {
            return Collections.emptyMap();
        }
        var map = new LinkedHashMap<String, Scope>();
        for (var name : scope.getSymbolTable().keySet()) {
            map.put(name, currentScopes.get(name));
            currentScopes.put(name, scope);
        }
        onEnterScope(scope);
        return map;
    }

    protected void onEnterScope(Scope scope) {
    }

    private void leaveScope(Map<String, Scope> backup, Scope scope) {
        for (var entry : backup.entrySet()) {
            if (entry.getValue() == null) {
                currentScopes.remove(entry.getKey());
            } else {
                currentScopes.put(entry.getKey(), entry.getValue());
            }
        }
        onLeaveScope(scope);
    }

    protected void onLeaveScope(Scope scope) {
    }

    protected Scope scopeOfId(String id) {
        return currentScopes.get(id);
    }
}
