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
import org.mozilla.javascript.ast.FunctionNode;
import org.teavm.backend.javascript.ast.AstVisitor;

public class TemplatingFunctionIndex extends AstVisitor {
    private Map<String, FunctionNode> functions = new HashMap<>();

    @Override
    public void visit(FunctionNode node) {
        if (node.getName() != null) {
            functions.put(node.getName(), node);
        }
    }

    public FunctionNode getFunction(String name) {
        return functions.get(name);
    }
}
