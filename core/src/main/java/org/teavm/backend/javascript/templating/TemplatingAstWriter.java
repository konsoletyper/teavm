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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.AstWriter;
import org.teavm.backend.javascript.rendering.DefaultGlobalNameWriter;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.analysis.ClassInitializerInfo;

public class TemplatingAstWriter extends AstWriter {
    private Map<String, SourceFragment> names;
    private Scope scope;
    private Map<String, SourceFragment> fragments = new HashMap<>();
    private ClassInitializerInfo classInitializerInfo;
    private Set<Scope> topLevelScopes = new HashSet<>();
    private boolean inFunction;

    public TemplatingAstWriter(SourceWriter writer, Map<String, SourceFragment> names, Scope scope,
            ClassInitializerInfo classInitializerInfo) {
        super(writer, new DefaultGlobalNameWriter(writer));
        this.classInitializerInfo = classInitializerInfo;
        this.names = names;
        this.scope = scope;
        if (names != null) {
            for (var name : names.keySet()) {
                currentScopes.put(name, scope);
            }
        }
        if (scope instanceof FunctionNode) {
            currentScopes.put("arguments", scope);
        }
    }

    public void setFragment(String name, SourceFragment fragment) {
        fragments.put(name, fragment);
    }

    @Override
    protected boolean intrinsic(FunctionCall node, int precedence) {
        if (node.getTarget() instanceof Name) {
            var name = (Name) node.getTarget();
            if (scopeOfId(name.getIdentifier()) == null) {
                return tryIntrinsicName(node, name.getIdentifier());
            }
        }
        return super.intrinsic(node, precedence);
    }

    private boolean tryIntrinsicName(FunctionCall node, String name) {
        switch (name) {
            case "teavm_javaClass":
                return writeJavaClass(node);
            case "teavm_javaMethod":
                return writeJavaMethod(node);
            case "teavm_javaConstructor":
                return writeJavaConstructor(node);
            case "teavm_javaClassInit":
                return writeJavaClassInit(node);
            case "teavm_fragment":
                return writeFragment(node);
            default:
                return false;
        }
    }

    private boolean writeJavaClass(FunctionCall node) {
        if (node.getArguments().size() != 1) {
            return false;
        }
        var classArg = node.getArguments().get(0);
        if (!(classArg instanceof StringLiteral)) {
            return false;
        }
        writer.appendClass(((StringLiteral) classArg).getValue());
        return true;
    }

    private boolean writeJavaMethod(FunctionCall node) {
        if (node.getArguments().size() != 2) {
            return false;
        }
        var classArg = node.getArguments().get(0);
        var methodArg = node.getArguments().get(1);
        if (!(classArg instanceof StringLiteral) || !(methodArg instanceof StringLiteral)) {
            return false;
        }
        var method = new MethodReference(((StringLiteral) classArg).getValue(),
                MethodDescriptor.parse(((StringLiteral) methodArg).getValue()));
        writer.appendMethodBody(method);
        return true;
    }

    private boolean writeJavaConstructor(FunctionCall node) {
        if (node.getArguments().size() != 2) {
            return false;
        }
        var classArg = node.getArguments().get(0);
        var methodArg = node.getArguments().get(1);
        if (!(classArg instanceof StringLiteral) || !(methodArg instanceof StringLiteral)) {
            return false;
        }
        var method = new MethodReference(((StringLiteral) classArg).getValue(), "<init>",
                MethodDescriptor.parseSignature(((StringLiteral) methodArg).getValue()));
        writer.appendInit(method);
        return true;
    }

    private boolean writeJavaClassInit(FunctionCall node) {
        if (node.getArguments().size() != 1) {
            return false;
        }
        var classArg = node.getArguments().get(0);
        if (!(classArg instanceof StringLiteral)) {
            return false;
        }
        var className = ((StringLiteral) classArg).getValue();
        if (classInitializerInfo == null || classInitializerInfo.isDynamicInitializer(className)) {
            writer.appendClassInit(className);
        } else {
            writer.append("(()").ws().append("=>").ws().append("{})");
        }
        return true;
    }

    private boolean writeFragment(FunctionCall node) {
        if (node.getArguments().size() != 1) {
            return false;
        }
        var fragmentArg = node.getArguments().get(0);
        if (!(fragmentArg instanceof StringLiteral)) {
            return false;
        }
        var fragment = fragments.get(((StringLiteral) fragmentArg).getValue());
        fragment.write(writer, AstWriter.PRECEDENCE_COMMA + 1);
        return true;
    }

    @Override
    protected void print(ElementGet node) {
        if (node.getElement() instanceof FunctionCall) {
            var call = (FunctionCall) node.getElement();
            if (call.getTarget() instanceof Name) {
                var name = (Name) call.getTarget();
                if (scopeOfId(name.getIdentifier()) == null) {
                    switch (name.getIdentifier()) {
                        case "teavm_javaVirtualMethod":
                            if (writeJavaVirtualMethod(node, call)) {
                                return;
                            }
                            break;
                        case "teavm_javaField":
                            if (writeJavaField(node, call)) {
                                return;
                            }
                            break;
                    }
                }
            }
        }
        super.print(node);
    }

    @Override
    public void print(PropertyGet node) {
        if (node.getTarget() instanceof Name) {
            var name = (Name) node.getTarget();
            var scope = scopeOfId(name.getIdentifier());
            if (scope == null && name.getIdentifier().equals("teavm_globals")) {
                var oldRootScope = rootScope;
                rootScope = false;
                writer.appendGlobal(node.getProperty().getIdentifier());
                rootScope = oldRootScope;
                return;
            }
        }
        super.print(node);
    }

    private boolean writeJavaVirtualMethod(ElementGet get, FunctionCall call) {
        var arg = call.getArguments().get(0);
        if (!(arg instanceof StringLiteral)) {
            return false;
        }
        var method = MethodDescriptor.parse(((StringLiteral) arg).getValue());
        print(get.getTarget());
        writer.append('.').appendMethod(method);
        return true;
    }

    private boolean writeJavaField(ElementGet get, FunctionCall call) {
        if (call.getArguments().size() != 2) {
            return false;
        }
        var classArg = call.getArguments().get(0);
        var fieldArg = call.getArguments().get(1);
        if (!(classArg instanceof StringLiteral) || !(fieldArg instanceof StringLiteral)) {
            return false;
        }
        var className = ((StringLiteral) classArg).getValue();
        var fieldName = ((StringLiteral) fieldArg).getValue();
        print(get.getTarget());
        writer.append('.').appendField(new FieldReference(className, fieldName));
        return true;
    }

    @Override
    public void print(Name node, int precedence) {
        var definingScope = scopeOfId(node.getIdentifier());
        if (rootScope) {
            if (names != null && definingScope == scope) {
                var fragment = names.get(node.getIdentifier());
                if (fragment != null) {
                    fragment.write(writer, precedence);
                    return;
                }
            }
            if (definingScope == null || topLevelScopes.contains(definingScope)) {
                writer.appendFunction(node.getIdentifier());
                return;
            }
        }
        super.print(node, precedence);
    }

    @Override
    protected void print(FunctionNode node) {
        if (inFunction) {
            super.print(node);
        } else {
            inFunction = true;
            super.print(node);
            inFunction = false;
        }
    }

    @Override
    protected void onEnterScope(Scope scope) {
        if (names == null && !inFunction) {
            topLevelScopes.add(scope);
        }
    }

    @Override
    protected void onLeaveScope(Scope scope) {
        if (names == null && !inFunction) {
            topLevelScopes.remove(scope);
        }
    }
}
