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

import java.util.List;
import org.teavm.model.TextLocation;
import org.teavm.pta.AnalysisState;
import org.teavm.pta.Constraint;
import org.teavm.pta.Node;

public class IsSubtypeConstraint implements Constraint {
    private Node node;
    private List<Constraint> innerConstraints;
    private TextLocation location;

    public IsSubtypeConstraint(Node node, List<Constraint> innerConstraints, TextLocation location) {
        this.node = node;
        this.innerConstraints = innerConstraints;
        this.location = location;
    }

    @Override
    public void apply(AnalysisState state) {

    }

    @Override
    public TextLocation textLocation() {
        return null;
    }
}
