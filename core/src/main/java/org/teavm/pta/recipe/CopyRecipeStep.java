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

public class CopyRecipeStep implements RecipeStep {
    public final int from;
    public final int to;
    public final TextLocation textLocation;

    public CopyRecipeStep(int from, int to, TextLocation textLocation) {
        this.from = from;
        this.to = to;
        this.textLocation = textLocation;
    }

    @Override
    public void apply(RecipeContext context) {
        context.state().addCopyConstraint(context.variableNode(from), context.variableNode(to), textLocation);
    }
}
