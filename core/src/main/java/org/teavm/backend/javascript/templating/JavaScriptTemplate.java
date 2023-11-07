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
import java.util.Map;
import java.util.function.IntFunction;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.ClassReaderSource;

public class JavaScriptTemplate {
    private TemplatingFunctionIndex functionIndex = new TemplatingFunctionIndex();

    public JavaScriptTemplate(AstNode node, ClassReaderSource classSource) {
        new TemplatingAstTransformer(classSource).visit(node);
        functionIndex.visit(node);
    }

    public FragmentBuilder builder(String functionName) {
        var function = functionIndex.getFunction(functionName);
        if (function == null) {
            throw new IllegalArgumentException("Function " + functionName + " was not found in JS template");
        }
        return new FragmentBuilder(function);
    }

    public static class FragmentBuilder {
        private FunctionNode node;
        private IntFunction<SourceFragment> parameters;
        private Map<String, SourceFragment> fragments = new HashMap<>();

        private FragmentBuilder(FunctionNode node) {
            this.node = node;
        }

        public FragmentBuilder withParameters(IntFunction<SourceFragment> parameters) {
            this.parameters = parameters;
            return this;
        }

        public FragmentBuilder withContext(GeneratorContext context) {
            return withParameters(param -> (writer, precedence) -> writer.append(context.getParameterName(param)));
        }

        public FragmentBuilder withFragment(String name, SourceFragment fragment) {
            fragments.put(name, fragment);
            return this;
        }

        public SourceFragment build() {
            var intParameters = parameters;
            var nameParameters = new HashMap<String, SourceFragment>();
            for (var i = 0; i < node.getParams().size(); ++i) {
                var param = node.getParams().get(i);
                if (param instanceof Name) {
                    nameParameters.put(((Name) param).getIdentifier(), intParameters.apply(i + 1));
                }
            }
            var thisFragment = parameters.apply(0);
            var body = node.getBody();
            return (writer, precedence) -> {
                var astWriter = new TemplatingAstWriter(writer, nameParameters, node, null);
                for (var entry : fragments.entrySet()) {
                    astWriter.setFragment(entry.getKey(), entry.getValue());
                }
                if (node.getSymbolTable() != null) {
                    for (var name : node.getSymbolTable().keySet()) {
                        astWriter.currentScopes.put(name, node);
                    }
                }
                if (thisFragment != null) {
                    astWriter.declareNameEmitter("this", thisPrecedence -> thisFragment.write(writer, thisPrecedence));
                }
                for (var child = body.getFirstChild(); child != null; child = child.getNext()) {
                    astWriter.print((AstNode) child);
                    writer.softNewLine();
                }
            };
        }
    }
}
