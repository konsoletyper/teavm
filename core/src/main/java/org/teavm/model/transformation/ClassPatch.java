/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.model.transformation;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

public class ClassPatch implements ClassHolderTransformer {
    private FieldReference platformClassField = new FieldReference("java.lang.Class", "platformClass");
    
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!cls.getName().equals("java.lang.Class")) {
            return;
        }
        for (MethodHolder method : cls.getMethods()) {
            patchProgram(method.getProgram());
        }
    }
    
    private void patchProgram(Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction instruction : block) {
                if (instruction instanceof GetFieldInstruction) {
                    GetFieldInstruction getField = (GetFieldInstruction) instruction;
                    if (getField.getField().equals(platformClassField)) {
                        AssignInstruction replacement = new AssignInstruction();
                        replacement.setReceiver(getField.getReceiver());
                        replacement.setAssignee(getField.getInstance());
                        replacement.setLocation(instruction.getLocation());
                        instruction.replace(replacement);
                    }
                } else if (instruction instanceof PutFieldInstruction) {
                    PutFieldInstruction putField = (PutFieldInstruction) instruction;
                    if (putField.getField().equals(platformClassField)) {
                        EmptyInstruction replacement = new EmptyInstruction();
                        replacement.setLocation(instruction.getLocation());
                        instruction.replace(replacement);
                    }
                }
            }
        }
    }
}
