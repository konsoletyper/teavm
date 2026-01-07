/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.pta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;
import org.teavm.pta.recipe.RecipeContext;

public class MethodAnalysis {
    private AnalysisState state;
    private MethodHolder method;
    private List<Node> variableNodes = new ArrayList<>();
    private Node returnNode;
    private Node throwsNode;
    private Map<Object, MethodInstance> callsByContext = new HashMap<>();
    private MethodRecipe recipe;

    MethodAnalysis(AnalysisState state, MethodHolder method) {
        this.state = state;
        this.method = method;
        var varCount = method.parameterCount() + 1;
        if (method.getProgram() != null) {
            varCount = Math.max(varCount, method.getProgram().variableCount());
        }
        variableNodes.addAll(Collections.nCopies(varCount, null));
    }

    public MethodReader method() {
        return method;
    }

    public AnalysisState state() {
        return state;
    }

    public Node variableNode(int index) {
        var result = variableNodes.get(index);
        if (result == null) {
            result = state.createNode(method.getReference() + ".var" + index);
            variableNodes.set(index, result);
            if (index == 0) {
                result.typeFilter = ValueType.object(method.getOwnerName());
            } else if (index <= method.parameterCount()) {
                result.typeFilter = method.parameterType(index - 1);
            }
        }
        return result;
    }

    public Node returnNode() {
        if (returnNode == null && method.getResultType() != ValueType.VOID) {
            returnNode = state.createNode(method.getReference() + ".ret");
            returnNode.typeFilter = method.getResultType();
        }
        return returnNode;
    }

    public Node throwsNode() {
        if (throwsNode == null) {
            throwsNode = state.createNode(method.getReference() + ".throws");
            returnNode.typeFilter = ValueType.object("java.lang.Throwable");
        }
        return throwsNode;
    }

    public MethodInstance createInstance(Object context) {
        if (recipe == null) {
            createRecipe();
        }
        var instance = new MethodInstance(state, this, variableNodes.size(), method.getReference().toString(),
                context);
        var recipeContext = new RecipeContext() {
            @Override
            public Node variableNode(int index) {
                return instance.variableNode(index);
            }

            @Override
            public AnalysisState state() {
                return state;
            }

            @Override
            public Node returnNode() {
                return instance.returnNode();
            }

            @Override
            public Node throwsNode() {
                return instance.throwsNode();
            }
        };
        for (var recipePart : recipe.steps) {
            recipePart.apply(recipeContext);
        }
        for (var i = 0; i < recipe.typeFilters.length; i++) {
            var filter = recipe.typeFilters[i];
            if (filter != null) {
                recipe.typeFilters[i] = filter;
            }
        }
        return instance;
    }

    private void createRecipe() {
        recipe = new MethodRecipe(method.getProgram());
    }

    public MethodInstance getInstanceWithContext(Object context) {
        return callsByContext.computeIfAbsent(context, k -> createInstance(context));
    }
}
