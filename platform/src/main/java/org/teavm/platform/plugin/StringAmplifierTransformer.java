/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.platform.plugin;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.platform.metadata.Resource;

public class StringAmplifierTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                transformProgram(context.getHierarchy(), method.getProgram());
            }
        }
    }

    private void transformProgram(ClassHierarchy hierarchy, Program program) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (!(instruction instanceof InvokeInstruction)) {
                    continue;
                }

                InvokeInstruction invoke = (InvokeInstruction) instruction;
                if (invoke.getReceiver() == null) {
                    continue;
                }

                MethodReference method = invoke.getMethod();
                String owningClass = method.getClassName();
                if (hierarchy.isSuperType(Resource.class.getName(), owningClass, false)) {
                    if (method.getReturnType().isObject(String.class)) {
                        Variable var = program.createVariable();
                        InvokeInstruction amplifyInstruction = new InvokeInstruction();
                        amplifyInstruction.setMethod(new MethodReference(StringAmplifier.class, "amplify",
                                String.class, String.class));
                        amplifyInstruction.setType(InvocationType.SPECIAL);
                        amplifyInstruction.setArguments(var);
                        amplifyInstruction.setReceiver(invoke.getReceiver());
                        amplifyInstruction.setLocation(invoke.getLocation());

                        invoke.setReceiver(var);
                        invoke.insertNext(amplifyInstruction);
                    }
                }
            }
        }
    }
}
