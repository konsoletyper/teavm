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

import org.teavm.model.FieldReference;
import org.teavm.model.TextLocation;
import org.teavm.pta.AnalysisState;
import org.teavm.pta.Constraint;
import org.teavm.pta.Node;

public class SetFieldConstraint implements Constraint {
    private final Node instanceNode;
    private final FieldReference field;
    private final Node sourceNode;
    private final TextLocation textLocation;

    public SetFieldConstraint(Node instanceNode, FieldReference field, Node sourceNode, TextLocation textLocation) {
        this.instanceNode = instanceNode;
        this.field = field;
        this.sourceNode = sourceNode;
        this.textLocation = textLocation;
    }

    @Override
    public void apply(AnalysisState state) {
        instanceNode.locationSource()
                .map(loc -> loc.fieldNode(field))
                .sink(state.copyFromSink(sourceNode, textLocation));
    }

    @Override
    public TextLocation textLocation() {
        return textLocation;
    }


    @Override
    public String toString() {
        return "setField(instance=" + instanceNode + ", field=" + field + ", value=" + sourceNode + ")";
    }
}
