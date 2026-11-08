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
package org.teavm.model.optimization;

import java.util.List;
import org.teavm.model.Program;
import org.teavm.model.analysis.NullnessInformation;

/*
   Building nullness information (extended-SSA representation) is expensive, so it makes sense to
   run all optimizations, dependent on it, in a single pass.

   This is a candidate for further improvement, when the optimizer starts to collect more facts in extended-SSA,
   for example, types and int value ranges.
 */
public class NullnessBasedOptimizations implements MethodOptimization {
    private List<NullnessAwareOptimization> optimizations;

    public NullnessBasedOptimizations(List<NullnessAwareOptimization> optimizations) {
        this.optimizations = optimizations;
    }

    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        var anythingChanged = false;
        while (true) {
            var nullness = NullnessInformation.build(program, context.getMethod().getDescriptor());
            var changed = false;
            for (var optimization : optimizations) {
                changed |= optimization.optimize(nullness, context, program);
                anythingChanged |= changed;
            }
            if (changed) {
                new UnreachableBasicBlockEliminator().optimize(program, context.getMethod().parameterCount());
                nullness.dispose();
            } else {
                nullness.dispose();
                break;
            }
        }
        return anythingChanged;
    }
}
