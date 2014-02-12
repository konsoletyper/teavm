/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.dependency;

import java.util.Arrays;

/**
 *
 * @author Alexey Andreev
 */
public class MethodGraph implements DependencyMethodInformation {
    private DependencyNode[] variableNodes;
    private int parameterCount;
    private DependencyNode resultNode;

    MethodGraph(DependencyNode[] variableNodes, int parameterCount, DependencyNode resultNode) {
        this.variableNodes = Arrays.copyOf(variableNodes, variableNodes.length);
        this.parameterCount = parameterCount;
        this.resultNode = resultNode;
    }

    @Override
    public DependencyNode[] getVariables() {
        return Arrays.copyOf(variableNodes, variableNodes.length);
    }

    @Override
    public int getVariableCount() {
        return variableNodes.length;
    }

    @Override
    public DependencyNode getVariable(int index) {
        return variableNodes[index];
    }

    @Override
    public int getParameterCount() {
        return parameterCount;
    }

    @Override
    public DependencyNode getResult() {
        return resultNode;
    }
}
