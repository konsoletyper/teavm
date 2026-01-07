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

import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.pta.AnalysisState;
import org.teavm.pta.Constraint;
import org.teavm.pta.Node;
import org.teavm.pta.Source;

public class ClassConstantConstraint implements Constraint {
    private ValueType cst;
    private Node node;
    private TextLocation location;

    public ClassConstantConstraint(ValueType cst, Node node, TextLocation location) {
        this.cst = cst;
        this.node = node;
        this.location = location;
    }

    @Override
    public void apply(AnalysisState state) {
        node.takeLocations(Source.of(state.classLiteralLocation(cst)));
    }

    @Override
    public TextLocation textLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "classConstant(node=" + node + ", cst=" + cst + ")";
    }
}
