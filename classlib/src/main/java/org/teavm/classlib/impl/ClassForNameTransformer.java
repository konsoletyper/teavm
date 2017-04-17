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
package org.teavm.classlib.impl;

import java.util.Arrays;
import org.teavm.common.DisjointSet;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class ClassForNameTransformer implements ClassHolderTransformer {
    private static final MethodReference getNameMethod = new MethodReference(Class.class, "getName", String.class);
    private static final MethodReference forNameMethod = new MethodReference(Class.class, "forName", String.class,
            boolean.class, ClassLoader.class, Class.class);
    private static final MethodReference initMethod = new MethodReference(Class.class, "initialize", void.class);

    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program != null) {
                transformProgram(program);
            }
        }
    }

    private void transformProgram(Program program) {
        DisjointSet varSet = new DisjointSet();
        for (int i = 0; i < program.variableCount(); i++) {
            varSet.create();
        }
        int[] nameIndexes = new int[program.variableCount()];
        Arrays.fill(nameIndexes, -1);

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) instruction;
                    if (invoke.getMethod().equals(getNameMethod)) {
                        if (invoke.getReceiver() != null) {
                            nameIndexes[invoke.getReceiver().getIndex()] = invoke.getInstance().getIndex();
                        }
                    }
                } else if (instruction instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) instruction;
                    varSet.union(assign.getAssignee().getIndex(), assign.getReceiver().getIndex());
                }
            }
        }

        nameIndexes = Arrays.copyOf(nameIndexes, varSet.size());
        int[] nameRepresentatives = new int[nameIndexes.length];
        Arrays.fill(nameRepresentatives, -1);

        for (int i = 0; i < program.variableCount(); i++) {
            int varClass = varSet.find(i);
            if (nameRepresentatives[varClass] < 0) {
                nameRepresentatives[varClass] = i;
            }
            if (nameIndexes[i] >= 0) {
                nameIndexes[varClass] = varSet.find(nameIndexes[i]);
            }
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (!(instruction instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction) instruction;

                if (!invoke.getMethod().equals(forNameMethod)) {
                    continue;
                }

                int classNameIndex = invoke.getArguments().get(0).getIndex();
                int nameIndex = nameIndexes[classNameIndex];
                if (nameIndex < 0) {
                    continue;
                }

                Variable representative = program.variableAt(nameRepresentatives[nameIndex]);

                InvokeInstruction initInvoke = new InvokeInstruction();
                initInvoke.setLocation(invoke.getLocation());
                initInvoke.setType(InvocationType.SPECIAL);
                initInvoke.setMethod(initMethod);
                initInvoke.setInstance(representative);
                invoke.insertPrevious(initInvoke);

                if (invoke.getReceiver() == null) {
                    invoke.delete();
                } else {
                    AssignInstruction assign = new AssignInstruction();
                    assign.setLocation(invoke.getLocation());
                    assign.setAssignee(representative);
                    assign.setReceiver(invoke.getReceiver());
                    invoke.replace(assign);
                }
            }
        }
    }
}
