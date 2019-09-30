/*
 *  Copyright 2019 konsoletyper.
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

import org.mozilla.javascript.Node;
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
import org.mozilla.javascript.ast.EmptyExpression;
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

public class AstVisitor {
    public void accept(AstNode node) {
        switch (node.getType()) {
            case Token.SCRIPT:
                visitRoot((AstRoot) node);
                break;
            case Token.CALL:
            case Token.NEW:
                visitFunctionCall((FunctionCall) node);
                break;
            case Token.FUNCTION:
                visitFunction((FunctionNode) node);
                break;
            case Token.ARRAYCOMP:
                visitArrayComprehension((ArrayComprehension) node);
                break;
            case Token.GETPROP:
                visitPropertyGet((PropertyGet) node);
                break;
            case Token.GENEXPR:
                visitGenerator((GeneratorExpression) node);
                break;
            case Token.NUMBER:
                visitNumber((NumberLiteral) node);
                break;
            case Token.STRING:
                visitString((StringLiteral) node);
                break;
            case Token.TRUE:
                visitTrue(node);
                break;
            case Token.FALSE:
                visitFalse(node);
                break;
            case Token.THIS:
                visitThis(node);
                break;
            case Token.NULL:
                visitNull(node);
                break;
            case Token.NAME:
                visitName((Name) node);
                break;
            case Token.REGEXP:
                visitRegexp((RegExpLiteral) node);
                break;
            case Token.OBJECTLIT:
                visitObjectLiteral((ObjectLiteral) node);
                break;
            case Token.ARRAYLIT:
                visitArrayLiteral((ArrayLiteral) node);
                break;
            case Token.BLOCK:
                if (node instanceof Block) {
                    visitBlock((Block) node);
                } else if (node instanceof Scope) {
                    visitScope((Scope) node);
                }
                break;
            case Token.HOOK:
                visitConditionalExpr((ConditionalExpression) node);
                break;
            case Token.GETELEM:
                visitElementGet((ElementGet) node);
                break;
            case Token.LETEXPR:
                visitLet((LetNode) node);
                break;
            case Token.LP:
                visitParenthesized((ParenthesizedExpression) node);
                break;
            case Token.EMPTY:
                if (node instanceof EmptyStatement) {
                    visitEmpty((EmptyStatement) node);
                } else {
                    visitEmpty((EmptyExpression) node);
                }
                break;
            case Token.EXPR_VOID:
            case Token.EXPR_RESULT:
                if (node instanceof ExpressionStatement) {
                    visitExpressionStatement((ExpressionStatement) node);
                } else if (node instanceof LabeledStatement) {
                    visitLabeledStatement((LabeledStatement) node);
                }
                break;
            case Token.BREAK:
                visitBreak((BreakStatement) node);
                break;
            case Token.CONTINUE:
                visitContinue((ContinueStatement) node);
                break;
            case Token.RETURN:
                visitReturn((ReturnStatement) node);
                break;
            case Token.DO:
                visitDo((DoLoop) node);
                break;
            case Token.FOR:
                if (node instanceof ForInLoop) {
                    visitForIn((ForInLoop) node);
                } else if (node instanceof ForLoop) {
                    visitFor((ForLoop) node);
                }
                break;
            case Token.IF:
                visitIf((IfStatement) node);
                break;
            case Token.SWITCH:
                visitSwitch((SwitchStatement) node);
                break;
            case Token.THROW:
                visitThrow((ThrowStatement) node);
                break;
            case Token.TRY:
                visitTry((TryStatement) node);
                break;
            case Token.CONST:
            case Token.VAR:
            case Token.LET:
                visitVariableDeclaration((VariableDeclaration) node);
                break;
            case Token.WHILE:
                visitWhile((WhileLoop) node);
                break;
            default:
                if (node instanceof InfixExpression) {
                    visitInfix((InfixExpression) node);
                } else if (node instanceof UnaryExpression) {
                    visitUnary((UnaryExpression) node);
                }
                break;
        }
    }

    protected void visitRoot(AstRoot node) {
        for (Node child : node) {
            accept((AstNode) child);
        }
    }

    protected void visitBlock(Block node) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            accept((AstNode) child);
        }
    }

    protected void visitScope(Scope node) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            accept((AstNode) child);
        }
    }

    protected void visitLabeledStatement(LabeledStatement node) {
        accept(node.getStatement());
    }

    protected void visitBreak(BreakStatement node) {
    }

    protected void visitContinue(ContinueStatement node) {
    }

    protected void visitReturn(ReturnStatement node)  {
        if (node.getReturnValue() != null) {
            accept(node.getReturnValue());
        }
    }

    protected void visitThrow(ThrowStatement node) {
        accept(node.getExpression());
    }

    protected void visitDo(DoLoop node) {
        accept(node.getBody());
        accept(node.getCondition());
    }

    protected void visitForIn(ForInLoop node) {
        accept(node.getIterator());
        accept(node.getIteratedObject());
        accept(node.getBody());
    }

    protected void visitFor(ForLoop node) {
        accept(node.getInitializer());
        accept(node.getCondition());
        accept(node.getIncrement());
        accept(node.getBody());
    }

    protected void visitWhile(WhileLoop node) {
        accept(node.getCondition());
        accept(node.getBody());
    }

    protected void visitIf(IfStatement node) {
        accept(node.getCondition());
        accept(node.getThenPart());
        if (node.getElsePart() != null) {
            accept(node.getElsePart());
        }
    }

    protected void visitSwitch(SwitchStatement node) {
        accept(node.getExpression());
        for (SwitchCase sc : node.getCases()) {
            if (sc.getExpression() != null) {
                accept(sc.getExpression());
            }
            if (sc.getStatements() != null) {
                for (AstNode stmt : sc.getStatements()) {
                    accept(stmt);
                }
            }
        }
    }

    protected void visitTry(TryStatement node)  {
        accept(node.getTryBlock());
        for (CatchClause cc : node.getCatchClauses()) {
            if (cc.getCatchCondition() != null) {
                accept(cc.getCatchCondition());
            }
            accept(cc.getBody());
        }
        if (node.getFinallyBlock() != null) {
            accept(node.getFinallyBlock());
        }
    }

    protected void visitVariableDeclaration(VariableDeclaration node) {
        for (int i = 1; i < node.getVariables().size(); ++i) {
            visitVariableInitializer(node.getVariables().get(i));
        }
    }

    protected void visitVariableInitializer(VariableInitializer node) {
        accept(node.getTarget());
        if (node.getInitializer() != null) {
            accept(node.getInitializer());
        }
    }

    protected void visitExpressionStatement(ExpressionStatement node) {
        accept(node.getExpression());
    }

    protected void visitElementGet(ElementGet node) {
        accept(node.getTarget());
        accept(node.getElement());
    }

    protected void visitPropertyGet(PropertyGet node) {
        accept(node.getLeft());
        accept(node.getRight());
    }

    protected void visitFunctionCall(FunctionCall node) {
        accept(node.getTarget());
        for (AstNode arg : node.getArguments()) {
            accept(arg);
        }
        if (node instanceof NewExpression) {
            NewExpression newExpr = (NewExpression) node;
            if (newExpr.getInitializer() != null) {
                accept(newExpr.getInitializer());
            }
        }
    }

    protected void visitConditionalExpr(ConditionalExpression node) {
        accept(node.getTestExpression());
        accept(node.getTrueExpression());
        accept(node.getFalseExpression());
    }

    protected void visitArrayComprehension(ArrayComprehension node) {
        for (ArrayComprehensionLoop loop : node.getLoops()) {
            accept(loop.getIterator());
            accept(loop.getIteratedObject());
        }
        if (node.getFilter() != null) {
            accept(node.getFilter());
        }
        accept(node.getResult());
    }

    protected void visitGenerator(GeneratorExpression node) {
        for (GeneratorExpressionLoop loop : node.getLoops()) {
            accept(loop.getIterator());
            accept(loop.getIteratedObject());
        }
        if (node.getFilter() != null) {
            accept(node.getFilter());
        }
        accept(node.getResult());
    }

    protected void visitNumber(NumberLiteral node) {
    }

    protected void visitString(StringLiteral node) {
    }

    protected void visitThis(AstNode node) {
    }

    protected void visitTrue(AstNode node) {
    }

    protected void visitFalse(AstNode node) {
    }

    protected void visitNull(AstNode node) {
    }

    protected void visitEmpty(EmptyStatement node) {
    }

    protected void visitEmpty(EmptyExpression node) {
    }

    protected void visitName(Name node) {
    }

    protected void visitRegexp(RegExpLiteral node) {
    }

    protected void visitArrayLiteral(ArrayLiteral node) {
        for (AstNode element : node.getElements()) {
            accept(element);
        }
    }

    protected void visitObjectLiteral(ObjectLiteral node) {
        if (node.getElements() != null) {
            for (ObjectProperty property : node.getElements()) {
                visitObjectProperty(property);
            }
        }
    }

    protected void visitObjectProperty(ObjectProperty node) {
        accept(node.getLeft());
        accept(node.getRight());
    }

    protected void visitFunction(FunctionNode node) {
        if (node.getFunctionName() != null) {
            accept(node.getFunctionName());
        }
        for (AstNode param : node.getParams()) {
            accept(param);
        }

        accept(node.getBody());
    }

    protected void visitLet(LetNode node) {
        accept(node.getVariables());
        accept(node.getBody());
    }

    protected void visitParenthesized(ParenthesizedExpression node) {
        accept(node.getExpression());
    }

    protected void visitUnary(UnaryExpression node) {
        accept(node.getOperand());
    }

    protected void visitInfix(InfixExpression node) {
        accept(node.getLeft());
        accept(node.getRight());
    }
}
