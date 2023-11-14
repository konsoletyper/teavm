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

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.teavm.backend.javascript.ast.AstVisitor;

public class LetJoiner extends AstVisitor {
    @Override
    public void visit(Block node) {
        visitMany(node);
        super.visit(node);
    }

    @Override
    public void visit(Scope node) {
        visitMany(node);
        super.visit(node);
    }

    @Override
    public void visit(AstRoot node) {
        visitMany(node);
        super.visit(node);
    }

    @Override
    public void visit(FunctionNode node) {
        visitMany(node.getBody());
        super.visit(node);
    }

    private void visitMany(AstNode node) {
        VariableDeclaration previous = null;
        for (var childNode = node.getFirstChild(); childNode != null;) {
            var nextNode = childNode.getNext();
            var child = (AstNode) childNode;
            if (child instanceof VariableDeclaration) {
                var decl = (VariableDeclaration) childNode;
                if (previous != null && previous.getType() == decl.getType()) {
                    previous.getVariables().addAll(decl.getVariables());
                    node.removeChild(decl);
                } else {
                    previous = decl;
                }
            } else {
                previous = null;
            }
            childNode = nextNode;
        }
    }
}
