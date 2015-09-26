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

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
class JavaInvocationProcessor implements NodeVisitor {
    private ClassReaderSource classSource;
    private JSTypeHelper typeHelper;
    private Diagnostics diagnostics;
    private CallLocation location;

    public JavaInvocationProcessor(JSTypeHelper typeHelper, ClassReaderSource classSource, Diagnostics diagnostics) {
        this.typeHelper = typeHelper;
        this.classSource = classSource;
        this.diagnostics = diagnostics;
    }

    public void validate(CallLocation location, AstNode root) {
        this.location = location;
        root.visit(this);
    }

    @Override
    public boolean visit(AstNode node) {
        if (node instanceof FunctionCall) {
            return validateCall((FunctionCall) node);
        }
        return true;
    }

    private boolean validateCall(FunctionCall call) {
        if (!(call.getTarget() instanceof PropertyGet)) {
            return true;
        }

        PropertyGet propertyGet = (PropertyGet) call.getTarget();
        MethodReference methodRef = getJavaMethodSelector(propertyGet.getTarget());
        if (methodRef == null || !propertyGet.getProperty().getIdentifier().equals("invoke")) {
            return true;
        }

        for (AstNode arg : call.getArguments()) {
            arg.visit(this);
        }

        MethodReader method = classSource.resolve(methodRef);
        if (method == null) {
            diagnostics.error(location, "Java method not found: {{m0}}", methodRef);
            return false;
        }

        int requiredParams = methodRef.parameterCount();
        if (!method.hasModifier(ElementModifier.STATIC)) {
            ++requiredParams;
        }
        if (call.getArguments().size() != requiredParams) {
            diagnostics.error(location, "Invalid number of arguments for method {{m0}}. Expected: " + requiredParams
                    + ", encountered: " + call.getArguments().size(), methodRef);
        }

        StringBuilder sb = new StringBuilder("$$JSO$$_");
        sb.append(method.hasModifier(ElementModifier.STATIC) ? 'S' : "V");
        sb.append(method.getReference().toString());
        StringLiteral newTarget = new StringLiteral();
        newTarget.setValue(sb.toString());
        propertyGet.setTarget(newTarget);

        return false;
    }

    private MethodReference getJavaMethodSelector(AstNode node) {
        if (!(node instanceof FunctionCall)) {
            return null;
        }
        FunctionCall call = (FunctionCall) node;
        if (!isJavaMethodRepository(call.getTarget())) {
            return null;
        }
        if (call.getArguments().size() != 1) {
            diagnostics.error(location, "javaMethods.get method should take exactly one argument");
            return null;
        }
        StringBuilder nameBuilder = new StringBuilder();
        if (!extractMethodName(call.getArguments().get(0), nameBuilder)) {
            diagnostics.error(location, "javaMethods.get method should take string constant");
            return null;
        }

        MethodReference method = MethodReference.parseIfPossible(nameBuilder.toString());
        if (method == null) {
            diagnostics.error(location, "Wrong method reference: " + nameBuilder);
        }
        return method;
    }

    private boolean extractMethodName(AstNode node, StringBuilder sb) {
        if (node.getType() == Token.ADD) {
            InfixExpression infix = (InfixExpression) node;
            return extractMethodName(infix.getLeft(), sb) && extractMethodName(infix.getRight(), sb);
        } else if (node.getType() == Token.STRING) {
            sb.append(((StringLiteral) node).getValue());
            return true;
        } else {
            return false;
        }
    }

    private boolean isJavaMethodRepository(AstNode node) {
        if (!(node instanceof PropertyGet)) {
            return false;
        }
        PropertyGet propertyGet = (PropertyGet) node;

        if (!(propertyGet.getLeft() instanceof Name)) {
            return false;
        }
        if (!((Name) propertyGet.getTarget()).getIdentifier().equals("javaMethods")) {
            return false;
        }
        if (!propertyGet.getProperty().getIdentifier().equals("get")) {
            return false;
        }

        return true;
    }
}
