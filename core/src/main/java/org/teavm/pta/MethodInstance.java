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
import java.util.List;

public class MethodInstance {
    private AnalysisState state;
    private MethodAnalysis methodAnalysis;
    private String baseDesc;
    private Object context;
    private List<Node> variables = new ArrayList<>();
    private Node returnNode;
    private Node throwsNode;

    MethodInstance(AnalysisState state, MethodAnalysis methodAnalysis, int variableCount, String baseDesc,
            Object context) {
        this.state = state;
        this.methodAnalysis = methodAnalysis;
        variables.addAll(Collections.nCopies(variableCount, null));
        this.baseDesc = baseDesc;
        this.context = context;
    }

    public AnalysisState state() {
        return state;
    }

    public MethodAnalysis methodAnalysis() {
        return methodAnalysis;
    }

    public Node variableNode(int index) {
        var node = variables.get(index);
        if (node == null) {
            node = state.createNode(baseDesc + ".var" + index + "@" + context);
            variables.set(index, node);
            state.addCopyConstraint(node, methodAnalysis.variableNode(index), null);
        }
        return node;
    }

    public Node returnNode() {
        if (returnNode == null) {
            returnNode = state.createNode(baseDesc + ".ret@" + context);
            state.addCopyConstraint(returnNode, methodAnalysis.returnNode(), null);
        }
        return returnNode;
    }

    public Node throwsNode() {
        if (throwsNode == null) {
            throwsNode = state.createNode(baseDesc + ".throws@" + context);
            state.addCopyConstraint(returnNode, methodAnalysis.throwsNode(), null);
        }
        return throwsNode;
    }
}
