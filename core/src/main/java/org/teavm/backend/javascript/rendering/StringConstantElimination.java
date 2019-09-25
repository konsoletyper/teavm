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

import org.mozilla.javascript.Node;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;

public class StringConstantElimination implements NodeVisitor {
    @Override
    public boolean visit(AstNode astNode) {
        if (astNode instanceof Block || astNode instanceof Scope || astNode instanceof AstRoot) {
            handle(astNode);
        }
        return true;
    }

    private void handle(AstNode block) {
        Node child = block.getFirstChild();
        while (child != null) {
            Node next = child.getNext();
            if (child instanceof ExpressionStatement) {
                ExpressionStatement statement = (ExpressionStatement) child;
                if (statement.getExpression() instanceof StringLiteral) {
                    block.removeChild(child);
                }
            }
            child = next;
        }
    }
}
