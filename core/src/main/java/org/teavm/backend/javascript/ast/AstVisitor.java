/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.backend.javascript.ast;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.mozilla.javascript.ast.KeywordLiteral;
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

public class AstVisitor {
    protected AstNode replacement;
    protected boolean hasReplacement;
    protected final Map<String, Scope> currentScopes = new HashMap<>();

    public final void visit(AstNode node) {
        switch (node.getType()) {
            case Token.SCRIPT:
                visit((AstRoot) node);
                break;
            case Token.CALL:
            case Token.NEW:
                visit((FunctionCall) node);
                break;
            case Token.FUNCTION:
                visit((FunctionNode) node);
                break;
            case Token.ARRAYCOMP:
                visit((ArrayComprehension) node);
                break;
            case Token.GETPROP:
                visit((PropertyGet) node);
                break;
            case Token.GENEXPR:
                visit((GeneratorExpression) node);
                break;
            case Token.NUMBER:
                visit((NumberLiteral) node);
                break;
            case Token.STRING:
                visit((StringLiteral) node);
                break;
            case Token.TRUE:
            case Token.FALSE:
            case Token.THIS:
            case Token.NULL:
                visit((KeywordLiteral) node);
                break;
            case Token.NAME:
                visit((Name) node);
                break;
            case Token.REGEXP:
                visit((RegExpLiteral) node);
                break;
            case Token.OBJECTLIT:
                visit((ObjectLiteral) node);
                break;
            case Token.ARRAYLIT:
                visit((ArrayLiteral) node);
                break;
            case Token.BLOCK:
                if (node instanceof Block) {
                    visit((Block) node);
                } else if (node instanceof Scope) {
                    visit((Scope) node);
                }
                break;
            case Token.HOOK:
                visit((ConditionalExpression) node);
                break;
            case Token.GETELEM:
                visit((ElementGet) node);
                break;
            case Token.LETEXPR:
                visit((LetNode) node);
                break;
            case Token.LP:
                visit((ParenthesizedExpression) node);
                break;
            case Token.EMPTY:
                if (node instanceof EmptyExpression) {
                    visit((EmptyExpression) node);
                } else if (node instanceof EmptyStatement) {
                    visit((EmptyStatement) node);
                }
                break;
            case Token.EXPR_VOID:
            case Token.EXPR_RESULT:
                if (node instanceof ExpressionStatement) {
                    visit((ExpressionStatement) node);
                } else if (node instanceof LabeledStatement) {
                    visit((LabeledStatement) node);
                }
                break;
            case Token.BREAK:
                visit((BreakStatement) node);
                break;
            case Token.CONTINUE:
                visit((ContinueStatement) node);
                break;
            case Token.RETURN:
                visit((ReturnStatement) node);
                break;
            case Token.DO:
                visit((DoLoop) node);
                break;
            case Token.FOR:
                if (node instanceof ForInLoop) {
                    visit((ForInLoop) node);
                } else if (node instanceof ForLoop) {
                    visit((ForLoop) node);
                }
                break;
            case Token.IF:
                visit((IfStatement) node);
                break;
            case Token.SWITCH:
                visit((SwitchStatement) node);
                break;
            case Token.THROW:
                visit((ThrowStatement) node);
                break;
            case Token.TRY:
                visit((TryStatement) node);
                break;
            case Token.CONST:
            case Token.VAR:
            case Token.LET:
                visit((VariableDeclaration) node);
                break;
            case Token.WHILE:
                visit((WhileLoop) node);
                break;
            default:
                if (node instanceof InfixExpression) {
                    visit((InfixExpression) node);
                } else if (node instanceof UnaryExpression) {
                    visit((UnaryExpression) node);
                } else if (node instanceof UpdateExpression) {
                    visit((UpdateExpression) node);
                }
                break;
        }
    }

    protected final void visitMany(List<AstNode> nodes) {
        for (var i = 0; i < nodes.size(); ++i) {
            var node = nodes.get(i);
            visit(node);
            if (hasReplacement) {
                nodes.set(i, replacement);
            }
            hasReplacement = false;
            replacement = null;
        }
    }

    protected final void visitChildren(AstNode node) {
        for (var child = node.getFirstChild(); child != null;) {
            var next = child.getNext();
            visit((AstNode) child);
            if (hasReplacement) {
                if (replacement != null) {
                    node.replaceChild(child, replacement);
                } else {
                    node.removeChild(child);
                }
                replacement = null;
                hasReplacement = false;
            }
            child = next;
        }
    }

    protected final <T extends AstNode, S extends AstNode> void visitProperty(T owner, Function<T, S> getter,
            BiConsumer<T, S> setter) {
        visitProperty(owner, getter, setter, null);
    }

    protected final <T extends AstNode, S extends AstNode> void visitProperty(T owner, Function<T, S> getter,
            BiConsumer<T, S> setter, Supplier<S> defaultValue) {
        var node = getter.apply(owner);
        if (node != null) {
            visit(node);
            if (hasReplacement) {
                if (replacement == null && defaultValue != null) {
                    replacement = defaultValue.get();
                }
                //noinspection unchecked
                setter.accept(owner, (S) replacement);
            }
            replacement = null;
            hasReplacement = false;
        }
    }

    public void visit(AstRoot node) {
        visitChildren(node);
    }

    public void visit(Block node) {
        visitChildren(node);
    }

    public void visit(Scope node) {
        var scope = enterScope(node);
        visitChildren(node);
        leaveScope(node, scope);
    }

    public void visit(LabeledStatement node) {
        visitProperty(node, LabeledStatement::getStatement, LabeledStatement::setStatement, EMPTY_DEFAULT);
    }

    public void visit(BreakStatement node) {
    }

    public void visit(ContinueStatement node) {
    }

    public void visit(ReturnStatement node) {
        visitProperty(node, ReturnStatement::getReturnValue, ReturnStatement::setReturnValue);
    }

    public void visit(ThrowStatement node) {
        visitProperty(node, ThrowStatement::getExpression, ThrowStatement::setExpression,
                () -> new KeywordLiteral(0, 0, Token.NULL));
    }

    public void visit(DoLoop node) {
        var scope = enterScope(node);
        visitProperty(node, DoLoop::getBody, DoLoop::setBody, EMPTY_DEFAULT);
        visitProperty(node, DoLoop::getCondition, DoLoop::setCondition, NULL_DEFAULT);
        leaveScope(node, scope);
    }

    public void visit(ForInLoop node) {
        var scope = enterScope(node);
        visitProperty(node, ForInLoop::getIterator, ForInLoop::setIterator, NULL_DEFAULT);
        visitProperty(node, ForInLoop::getIteratedObject, ForInLoop::setIteratedObject, NULL_DEFAULT);
        visitProperty(node, ForInLoop::getBody, ForInLoop::setBody, EMPTY_DEFAULT);
        leaveScope(node, scope);
    }

    public void visit(ForLoop node) {
        var scope = enterScope(node);
        visitProperty(node, ForLoop::getInitializer, ForLoop::setInitializer, EMPTY_EXPR_DEFAULT);
        visitProperty(node, ForLoop::getCondition, ForLoop::setCondition, EMPTY_EXPR_DEFAULT);
        visitProperty(node, ForLoop::getIncrement, ForLoop::setIncrement, EMPTY_EXPR_DEFAULT);
        visitProperty(node, ForLoop::getBody, ForLoop::setBody, EMPTY_DEFAULT);
        leaveScope(node, scope);
    }

    public void visit(WhileLoop node) {
        var scope = enterScope(node);
        visitProperty(node, WhileLoop::getCondition, WhileLoop::setCondition, NULL_DEFAULT);
        visitProperty(node, WhileLoop::getBody, WhileLoop::setBody, EMPTY_DEFAULT);
        leaveScope(node, scope);
    }

    public void visit(IfStatement node) {
        visitProperty(node, IfStatement::getCondition, IfStatement::setCondition, NULL_DEFAULT);
        visitProperty(node, IfStatement::getThenPart, IfStatement::setThenPart, EMPTY_DEFAULT);
        visitProperty(node, IfStatement::getElsePart, IfStatement::setElsePart);
    }

    public void visit(SwitchStatement node) {
        visitProperty(node, SwitchStatement::getExpression, SwitchStatement::setExpression, NULL_DEFAULT);
        for (var sc : node.getCases()) {
            visitProperty(sc, SwitchCase::getExpression, SwitchCase::setExpression);
            if (sc.getStatements() != null) {
                visitMany(sc.getStatements());
            }
        }
    }

    public void visit(TryStatement node) {
        visitProperty(node, TryStatement::getTryBlock, TryStatement::setTryBlock, NULL_DEFAULT);
        for (var cc : node.getCatchClauses()) {
            visitProperty(cc, CatchClause::getVarName, CatchClause::setVarName);
            visitProperty(cc, CatchClause::getCatchCondition, CatchClause::setCatchCondition);
            if (cc.getBody() != null) {
                visitChildren(cc.getBody());
            }
        }
        visitProperty(node, TryStatement::getFinallyBlock, TryStatement::setFinallyBlock);
    }

    public void visit(VariableDeclaration node) {
        for (var variable : node.getVariables()) {
            visit(variable);
        }
    }

    public void visit(VariableInitializer node) {
        visitProperty(node, VariableInitializer::getTarget, VariableInitializer::setTarget);
        visitProperty(node, VariableInitializer::getInitializer, VariableInitializer::setInitializer);
    }

    public void visit(ExpressionStatement node) {
        visitProperty(node, ExpressionStatement::getExpression, ExpressionStatement::setExpression, NULL_DEFAULT);
    }

    public void visit(ElementGet node) {
        visitProperty(node, ElementGet::getTarget, ElementGet::setTarget, NULL_DEFAULT);
        visitProperty(node, ElementGet::getElement, ElementGet::setElement, NULL_DEFAULT);
    }

    public void visit(PropertyGet node) {
        visitProperty(node, PropertyGet::getTarget, PropertyGet::setTarget, NULL_DEFAULT);
        visitProperty(node, PropertyGet::getProperty, PropertyGet::setProperty);
    }

    public void visit(FunctionCall node) {
        visitProperty(node, FunctionCall::getTarget, FunctionCall::setTarget);
        visitMany(node.getArguments());
        if (node instanceof NewExpression) {
            var newExpr = (NewExpression) node;
            visitProperty(newExpr, NewExpression::getInitializer, NewExpression::setInitializer);
        }
    }

    public void visit(ConditionalExpression node) {
        visitProperty(node, ConditionalExpression::getTestExpression, ConditionalExpression::setTestExpression);
        visitProperty(node, ConditionalExpression::getTrueExpression, ConditionalExpression::setTrueExpression);
        visitProperty(node, ConditionalExpression::getFalseExpression, ConditionalExpression::setFalseExpression);
    }

    public void visit(ArrayComprehension node) {
        var scope = enterScope(node);
        for (var loop : node.getLoops()) {
            visitProperty(loop, ArrayComprehensionLoop::getIterator, ArrayComprehensionLoop::setIterator);
            visitProperty(loop, ArrayComprehensionLoop::getIteratedObject, ArrayComprehensionLoop::setIteratedObject);
        }
        visitProperty(node, ArrayComprehension::getFilter, ArrayComprehension::setFilter);
        visitProperty(node, ArrayComprehension::getResult, ArrayComprehension::setResult);
        leaveScope(node, scope);
    }

    public void visit(GeneratorExpression node) {
        var scope = enterScope(node);
        for (var loop : node.getLoops()) {
            visitProperty(loop, GeneratorExpressionLoop::getIterator, GeneratorExpressionLoop::setIterator);
            visitProperty(loop, GeneratorExpressionLoop::getIteratedObject,
                    GeneratorExpressionLoop::setIteratedObject);
        }
        visitProperty(node, GeneratorExpression::getFilter, GeneratorExpression::setFilter);
        visitProperty(node, GeneratorExpression::getResult, GeneratorExpression::setResult);
        leaveScope(node, scope);
    }

    public void visit(NumberLiteral node) {
    }

    public void visit(StringLiteral node) {
    }

    public void visit(KeywordLiteral node) {
    }

    public void visit(Name node) {
    }

    public void visit(RegExpLiteral node) {
    }

    public void visit(ArrayLiteral node) {
        visitMany(node.getElements());
    }

    public void visit(ObjectLiteral node) {
        if (node.getElements() != null) {
            for (var element : node.getElements()) {
                visit(element);
            }
        }
    }

    public void visit(ObjectProperty node) {
        visitProperty(node, ObjectProperty::getLeft, ObjectProperty::setLeft);
        visitProperty(node, ObjectProperty::getRight, ObjectProperty::setRight);
    }

    public void visit(FunctionNode node) {
        var scope = enterScope(node);
        if (node.getFunctionType() != FunctionNode.ARROW_FUNCTION) {
            currentScopes.put("arguments", node);
        }
        visitProperty(node, FunctionNode::getFunctionName, FunctionNode::setFunctionName);
        visitMany(node.getParams());
        visitChildren(node.getBody());
        leaveScope(node, scope);
    }

    public void visit(LetNode node) {
        var scope = enterScope(node);
        visitProperty(node, LetNode::getVariables, LetNode::setVariables);
        visitProperty(node, LetNode::getBody, LetNode::setBody);
        leaveScope(node, scope);
    }

    public void visit(ParenthesizedExpression node) {
        visitProperty(node, ParenthesizedExpression::getExpression, ParenthesizedExpression::setExpression);
    }

    public void visit(EmptyExpression node) {
    }

    public void visit(EmptyStatement node) {
    }

    public void visit(InfixExpression node) {
        visitProperty(node, InfixExpression::getLeft, InfixExpression::setLeft);
        visitProperty(node, InfixExpression::getRight, InfixExpression::setRight);
    }

    public void visit(UnaryExpression node) {
        visitProperty(node, UnaryExpression::getOperand, UnaryExpression::setOperand);
    }

    public void visit(UpdateExpression node) {
        visitProperty(node, UpdateExpression::getOperand, UpdateExpression::setOperand);
    }

    protected final void replaceWith(AstNode node) {
        hasReplacement = true;
        replacement = node;
    }


    private Map<String, Scope> enterScope(Scope scope) {
        onEnterScope(scope);
        if (scope.getSymbolTable() == null) {
            return Collections.emptyMap();
        }
        var map = new LinkedHashMap<String, Scope>();
        for (var name : scope.getSymbolTable().keySet()) {
            map.put(name, currentScopes.get(name));
            currentScopes.put(name, scope);
        }
        return map;
    }

    protected void onEnterScope(Scope scope) {
    }

    private void leaveScope(Scope scope, Map<String, Scope> backup) {
        onLeaveScope(scope);
        for (var entry : backup.entrySet()) {
            if (entry.getValue() == null) {
                currentScopes.remove(entry.getKey());
            } else {
                currentScopes.put(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void onLeaveScope(Scope scope) {
    }

    protected Scope scopeOfId(String id) {
        return currentScopes.get(id);
    }

    private static final Supplier<AstNode> NULL_DEFAULT = () -> new KeywordLiteral(0, 0, Token.NULL);
    private static final Supplier<AstNode> EMPTY_DEFAULT = () -> new EmptyStatement(0, 0);
    private static final Supplier<AstNode> EMPTY_EXPR_DEFAULT = () -> new EmptyExpression(0, 0);
}
