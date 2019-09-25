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

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.PropertyGet;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.model.FieldReference;

public class RuntimeAstTransformer extends AstVisitor {
    private static final FieldReference MONITOR_FIELD = new FieldReference(
            "java.lang.Object", "monitor");
    private NamingStrategy names;

    public RuntimeAstTransformer(NamingStrategy names) {
        this.names = names;
    }

    @Override
    protected void visitExpressionStatement(ExpressionStatement node) {
        AstNode expression = node.getExpression();
        if (expression.getType() == Token.CALL) {
            FunctionCall call = (FunctionCall) expression;
            if (call.getTarget().getType() == Token.NAME) {
                String id = ((Name) call.getTarget()).getIdentifier();
                if (id.equals("$rt_initMonitorField")) {
                    AstNode arg = call.getArguments().get(0);
                    accept(arg);
                    String fieldId = names.getNameFor(MONITOR_FIELD);
                    PropertyGet propertyGet = new PropertyGet(arg, new Name(0, fieldId));
                    KeywordLiteral nullExpr = new KeywordLiteral(0, 0, Token.NULL);
                    InfixExpression assign = new InfixExpression(Token.ASSIGN, propertyGet, nullExpr, 0);
                    node.setExpression(assign);
                    return;
                }
            }
        }
        super.visitExpressionStatement(node);
    }
}
