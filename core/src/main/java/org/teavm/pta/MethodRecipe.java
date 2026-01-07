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
import java.util.List;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.util.ProgramUtils;
import org.teavm.pta.recipe.ClassConstantRecipeStep;
import org.teavm.pta.recipe.CopyRecipeStep;
import org.teavm.pta.recipe.RecipeStep;

class MethodRecipe {
     List<RecipeStep> steps = new ArrayList<>();
     ValueType[] typeFilters;

     MethodRecipe(Program program) {
         var cfg = ProgramUtils.buildControlFlowGraph(program);
         typeFilters = new ValueType[program.variableCount()];
         for (var block : program.getBasicBlocks()) {
             for (var phi : block.getPhis()) {
                 for (var incoming : phi.getIncomings()) {
                     steps.add(new CopyRecipeStep(incoming.getSource().getIndex(), phi.getReceiver().getIndex(), null));
                 }
             }
         }
     }

     class InstructionReaderImpl extends AbstractInstructionReader {
         private TextLocation location;

         @Override
         public void location(TextLocation location) {
             this.location = location;
         }

         @Override
         public void classConstant(VariableReader receiver, ValueType cst) {
             steps.add(new ClassConstantRecipeStep(cst, receiver.getIndex(), location));
         }

         @Override
         public void assign(VariableReader receiver, VariableReader assignee) {
             steps.add(new CopyRecipeStep(assignee.getIndex(), receiver.getIndex(), location));
         }

         @Override
         public void cast(VariableReader receiver, VariableReader value, ValueType targetType, boolean weak) {
             typeFilters[value.getIndex()] = targetType;
         }
     }
}
