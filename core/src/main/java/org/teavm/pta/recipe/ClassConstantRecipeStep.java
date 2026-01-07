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
package org.teavm.pta.recipe;

import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.pta.constraints.ClassConstantConstraint;

public class ClassConstantRecipeStep implements RecipeStep {
    public final int to;
    public final ValueType cst;
    public final TextLocation textLocation;

    public ClassConstantRecipeStep(ValueType cst, int to, TextLocation textLocation) {
        this.cst = cst;
        this.to = to;
        this.textLocation = textLocation;
    }

    @Override
    public void apply(RecipeContext context) {
        var node = context.variableNode(to);
        context.state().addConstraint(new ClassConstantConstraint(cst, node, textLocation));
    }
}
