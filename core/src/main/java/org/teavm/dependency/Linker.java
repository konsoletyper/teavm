/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.dependency;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

public class Linker {
    public void link(DependencyInfo dependency, ClassHolder cls) {
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            MethodReference methodRef = method.getReference();
            MethodDependencyInfo methodDep = dependency.getMethod(methodRef);
            if (methodDep == null) {
                cls.removeMethod(method);
            } else if (!methodDep.isUsed()) {
                method.getModifiers().add(ElementModifier.ABSTRACT);
                method.setProgram(null);
            } else if (method.getProgram() != null) {
                link(dependency, method);
            }
        }
        for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
            FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
            if (dependency.getField(fieldRef) == null) {
                cls.removeField(field);
            }
        }
    }

    private void link(DependencyInfo dependency, MethodHolder method) {
        Program program = method.getProgram();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) insn;
                    if (invoke.getType() == InvocationType.SPECIAL) {
                        MethodDependencyInfo linkedMethod = dependency.getMethodImplementation(invoke.getMethod());
                        if (linkedMethod != null) {
                            invoke.setMethod(linkedMethod.getReference());
                        }
                    }
                } else if (insn instanceof GetFieldInstruction) {
                    GetFieldInstruction getField = (GetFieldInstruction) insn;
                    FieldDependencyInfo linkedField = dependency.getField(getField.getField());
                    if (linkedField != null) {
                        getField.setField(linkedField.getReference());
                    }

                    FieldReference fieldRef = getField.getField();
                    if (!fieldRef.getClassName().equals(method.getOwnerName())) {
                        InitClassInstruction initInsn = new InitClassInstruction();
                        initInsn.setClassName(fieldRef.getClassName());
                        initInsn.setLocation(insn.getLocation());
                        insn.insertPrevious(initInsn);
                    }

                } else if (insn instanceof PutFieldInstruction) {
                    PutFieldInstruction getField = (PutFieldInstruction) insn;
                    FieldDependencyInfo linkedField = dependency.getField(getField.getField());
                    if (linkedField != null) {
                        getField.setField(linkedField.getReference());
                    }
                }
            }
        }
    }
}
