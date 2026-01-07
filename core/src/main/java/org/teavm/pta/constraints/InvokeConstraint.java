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
package org.teavm.pta.constraints;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.pta.AnalysisState;
import org.teavm.pta.Constraint;
import org.teavm.pta.Node;

public class InvokeConstraint implements Constraint {
    private Object context;
    private TextLocation textLocation;
    private MethodReference methodReference;
    private List<Node> arguments;
    private Node lvalue;
    private Node catchNode;

    public InvokeConstraint(Object context, TextLocation textLocation, MethodReference methodReference,
            List<Node> arguments, Node lvalue, Node catchNode) {
        this.context = context;
        this.textLocation = textLocation;
        this.methodReference = methodReference;
        this.arguments = new ArrayList<>(arguments);
        this.lvalue = lvalue;
        this.catchNode = catchNode;
    }

    public MethodReference methodReference() {
        return methodReference;
    }

    @Override
    public void apply(AnalysisState state) {
        var methodAnalysis = state.method(methodReference);
        methodAnalysis.call(context, arguments, lvalue, catchNode);
    }

    @Override
    public TextLocation textLocation() {
        return textLocation;
    }

    @Override
    public String toString() {
        return "invoke(" + methodReference + ")";
    }
}
