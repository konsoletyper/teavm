/*
 *  Copyright 2022 Alexey Andreev.
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
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;

public class AssertionRemoval implements ClassHolderTransformer {
    private static final MethodReference ASSERTION_METHOD = new MethodReference(Class.class, "desiredAssertionStatus",
            boolean.class);

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                removeAssertions(method.getProgram());
            }
        }
    }

    private void removeAssertions(Program program) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) instruction;
                    if (invoke.getInstance() != null && invoke.getMethod().equals(ASSERTION_METHOD)) {
                        if (invoke.getReceiver() == null) {
                            invoke.delete();
                        } else {
                            IntegerConstantInstruction replacement = new IntegerConstantInstruction();
                            replacement.setConstant(0);
                            replacement.setReceiver(invoke.getReceiver());
                            replacement.setLocation(invoke.getLocation());
                            invoke.replace(replacement);
                        }
                    }
                }
            }
        }
    }
}
