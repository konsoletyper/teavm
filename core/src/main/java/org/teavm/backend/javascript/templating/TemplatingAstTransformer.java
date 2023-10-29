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
package org.teavm.backend.javascript.templating;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.UnaryExpression;
import org.teavm.backend.javascript.ast.AstVisitor;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class TemplatingAstTransformer extends AstVisitor {
    private ClassReaderSource classes;

    public TemplatingAstTransformer(ClassReaderSource classes) {
        this.classes = classes;
    }

    @Override
    public void visit(Block node) {
        super.visit(node);
        if (node.getFirstChild() == null) {
            replaceWith(null);
        } else if (node.getFirstChild() == node.getLastChild()) {
            replaceWith((AstNode) node.getFirstChild());
        }
    }

    @Override
    public void visit(IfStatement node) {
        super.visit(node);
        if (node.getCondition().getType() == Token.TRUE) {
            replaceWith(node.getThenPart());
        } else if (node.getCondition().getType() == Token.FALSE) {
            replaceWith(node.getElsePart());
        }
    }

    @Override
    public void visit(ConditionalExpression node) {
        super.visit(node);
        if (node.getTestExpression().getType() == Token.TRUE) {
            replaceWith(node.getTrueExpression());
        } else if (node.getTestExpression().getType() == Token.FALSE) {
            replaceWith(node.getFalseExpression());
        }
    }

    @Override
    public void visit(InfixExpression node) {
        super.visit(node);
        if (node.getType() == Token.AND) {
            if (node.getLeft().getType() == Token.FALSE) {
                replaceWith(node.getLeft());
            } else if (node.getRight().getType() == Token.FALSE) {
                replaceWith(node.getRight());
            } else if (node.getLeft().getType() == Token.TRUE) {
                replaceWith(node.getRight());
            } else if (node.getRight().getType() == Token.TRUE) {
                replaceWith(node.getLeft());
            }
        } else if (node.getType() == Token.OR) {
            if (node.getLeft().getType() == Token.TRUE) {
                replaceWith(node.getLeft());
            } else if (node.getRight().getType() == Token.TRUE) {
                replaceWith(node.getRight());
            } else if (node.getLeft().getType() == Token.FALSE) {
                replaceWith(node.getRight());
            } else if (node.getRight().getType() == Token.FALSE) {
                replaceWith(node.getLeft());
            }
        }
    }

    @Override
    public void visit(UnaryExpression node) {
        super.visit(node);
        if (node.getType() == Token.NOT) {
            if (node.getOperand().getType() == Token.TRUE) {
                node.getOperand().setType(Token.FALSE);
            } else if (node.getOperand().getType() == Token.FALSE) {
                node.getOperand().setType(Token.TRUE);
            }
        }
    }

    @Override
    public void visit(FunctionCall node) {
        super.visit(node);
        if (node.getTarget() instanceof Name) {
            var name = (Name) node.getTarget();
            var scope = scopeOfId(name.getIdentifier());
            if (scope == null) {
                tryIntrinsicName(node, name.getIdentifier());
            }
        }
    }

    private void tryIntrinsicName(FunctionCall node, String identifier) {
        switch (identifier) {
            case "teavm_javaClassExists":
                javaClassExists(node);
                break;
            case "teavm_javaMethodExists":
                javaMethodExists(node);
                break;
            case "teavm_javaConstructorExists":
                javaConstructorExists(node);
                break;
        }
    }

    private void javaClassExists(FunctionCall node) {
        if (node.getArguments().size() != 1) {
            return;
        }
        var classArg = node.getArguments().get(0);
        if (!(classArg instanceof StringLiteral)) {
            return;
        }
        var className = ((StringLiteral) classArg).getValue();
        var exists = classes.get(className) != null;
        replaceWith(new KeywordLiteral(0, 0, exists ? Token.TRUE : Token.FALSE));
    }

    private void javaMethodExists(FunctionCall node) {
        if (node.getArguments().size() != 2) {
            return;
        }
        var classArg = node.getArguments().get(0);
        var methodArg = node.getArguments().get(1);
        if (!(classArg instanceof StringLiteral) || !(methodArg instanceof StringLiteral)) {
            return;
        }
        var className = ((StringLiteral) classArg).getValue();
        var methodName = ((StringLiteral) methodArg).getValue();
        var method = classes.resolveImplementation(new MethodReference(className, MethodDescriptor.parse(methodName)));
        var exists = method != null && (method.getProgram() != null || method.hasModifier(ElementModifier.NATIVE));
        replaceWith(new KeywordLiteral(0, 0, exists ? Token.TRUE : Token.FALSE));
    }

    private void javaConstructorExists(FunctionCall node) {
        if (node.getArguments().size() != 2) {
            return;
        }
        var classArg = node.getArguments().get(0);
        var methodArg = node.getArguments().get(1);
        if (!(classArg instanceof StringLiteral) || !(methodArg instanceof StringLiteral)) {
            return;
        }
        var className = ((StringLiteral) classArg).getValue();
        var methodName = ((StringLiteral) methodArg).getValue();
        var method = classes.resolveImplementation(new MethodReference(className, "<init>",
                MethodDescriptor.parseSignature(methodName)));
        var exists = method != null && (method.getProgram() != null || method.hasModifier(ElementModifier.NATIVE));
        replaceWith(new KeywordLiteral(0, 0, exists ? Token.TRUE : Token.FALSE));
    }
}
