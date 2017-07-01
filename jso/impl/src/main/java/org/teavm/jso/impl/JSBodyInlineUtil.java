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

import java.util.HashMap;
import java.util.Map;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

final class JSBodyInlineUtil {
    private static final int COMPLEXITY_THRESHOLD = 20;
    private JSBodyInlineUtil() {
    }

    public static AstNode isSuitableForInlining(MethodReference method, String[] parameters, AstNode ast) {
        AstNode statement = isSingleStatement(ast);
        if (statement == null) {
            return null;
        }
        AstNode expression = getExpression(method, statement);
        if (expression == null) {
            return null;
        }

        ComplexityCounter complexityCounter = new ComplexityCounter();
        expression.visit(complexityCounter);
        if (complexityCounter.getComplexity() > COMPLEXITY_THRESHOLD) {
            return null;
        }

        VariableUsageCounter usageCounter = new VariableUsageCounter();
        expression.visit(usageCounter);
        for (String param : parameters) {
            if (usageCounter.getUsage(param) > 1) {
                return null;
            }
        }

        return expression;
    }

    private static AstNode getExpression(MethodReference method, AstNode statement) {
        if (method.getReturnType() == ValueType.VOID) {
            if (statement instanceof ExpressionStatement) {
                return ((ExpressionStatement) statement).getExpression();
            } else if (statement instanceof ThrowStatement) {
                return ((ThrowStatement) statement).getExpression();
            }
        } else {
            if (statement instanceof ReturnStatement) {
                return ((ReturnStatement) statement).getReturnValue();
            }
        }
        return null;
    }

    private static AstNode isSingleStatement(AstNode ast) {
        if (ast.getFirstChild() == null || ast.getFirstChild().getNext() != null) {
            return null;
        }
        if (ast.getFirstChild().getType() == Token.BLOCK) {
            return isSingleStatement((AstNode) ast.getFirstChild());
        }
        return (AstNode) ast.getFirstChild();
    }

    static class ComplexityCounter implements NodeVisitor {
        private int complexity;

        public int getComplexity() {
            return complexity;
        }

        @Override
        public boolean visit(AstNode node) {
            ++complexity;
            return true;
        }
    }

    static class VariableUsageCounter implements NodeVisitor {
        private Map<String, Integer> usages = new HashMap<>();

        public int getUsage(String varName) {
            return usages.computeIfAbsent(varName, i -> 0);
        }

        @Override
        public boolean visit(AstNode node) {
            if (node instanceof Name) {
                Name name = (Name) node;
                if (!name.isLocalName()) {
                    String id = name.getIdentifier();
                    usages.put(id, getUsage(id) + 1);
                }
            }
            return true;
        }
    }
}
