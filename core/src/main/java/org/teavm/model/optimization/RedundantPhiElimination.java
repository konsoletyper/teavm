/*
 *  Copyright 2024 konsoletyper.
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

import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.util.InstructionVariableMapper;

public class RedundantPhiElimination implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        var changed = false;
        var map = new int[program.variableCount()];
        for (var i = 0; i < map.length; ++i) {
            map[i] = i;
        }
        for (var block : program.getBasicBlocks()) {
            changed |= block.getPhis().removeIf(phi -> {
                if (phi.getIncomings().isEmpty()) {
                    return true;
                }
                Variable singleInput = null;
                for (var incoming : phi.getIncomings()) {
                    if (incoming.getValue() != phi.getReceiver()) {
                        if (singleInput == null) {
                            singleInput = incoming.getValue();
                        } else if (singleInput != incoming.getValue()) {
                            return false;
                        }
                    }
                }
                if (singleInput != null) {
                    map[phi.getReceiver().getIndex()] = map[singleInput.getIndex()];
                }
                return true;
            });
        }
        if (changed) {
            var mapper = new InstructionVariableMapper(v -> program.variableAt(map(map, v.getIndex())));
            for (var block : program.getBasicBlocks()) {
                mapper.apply(block);
            }
        }
        return changed;
    }

    private static int map(int[] array, int index) {
        var result = array[index];
        if (result != index) {
            var newResult = map(array, result);
            if (newResult != result) {
                array[index] = newResult;
            }
            result = newResult;
        }
        return result;
    }
}
