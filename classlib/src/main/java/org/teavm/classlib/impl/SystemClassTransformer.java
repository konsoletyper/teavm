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
package org.teavm.classlib.impl;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class SystemClassTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                transformProgram(method.getProgram());
            }
        }
    }

    private void transformProgram(Program program) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction insn : block) {
                if (!(insn instanceof GetFieldInstruction)) {
                    continue;
                }

                GetFieldInstruction getField = (GetFieldInstruction) insn;
                FieldReference field = getField.getField();
                if (field.getClassName().equals("java.lang.System")) {
                    switch (field.getFieldName()) {
                        case "err":
                        case "out":
                        case "in": {
                            InvokeInstruction invoke = new InvokeInstruction();
                            invoke.setType(InvocationType.SPECIAL);
                            invoke.setMethod(new MethodReference("java.lang.System", field.getFieldName(),
                                    getField.getFieldType()));
                            invoke.setReceiver(getField.getReceiver());
                            invoke.setLocation(insn.getLocation());
                            insn.replace(invoke);
                            break;
                        }
                    }
                }
            }
        }
    }
}
