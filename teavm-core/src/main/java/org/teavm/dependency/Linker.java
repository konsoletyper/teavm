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

import org.teavm.model.*;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class Linker {
    private DependencyInfo dependency;

    public Linker(DependencyInfo dependency) {
        this.dependency = dependency;
    }

    public ListableClassHolderSource link(ClassHolderSource classes) {
        MutableClassHolderSource cutClasses = new MutableClassHolderSource();
        for (String className : dependency.getAchievableClasses()) {
            ClassHolder classHolder = classes.get(className);
            cutClasses.putClassHolder(classHolder);
            for (MethodHolder method : classHolder.getMethods().toArray(new MethodHolder[0])) {
                MethodReference methodRef = new MethodReference(className, method.getDescriptor());
                MethodDependencyInfo methodDep = dependency.getMethod(methodRef);
                if (methodDep == null) {
                    classHolder.removeMethod(method);
                } else if (!methodDep.isUsed()) {
                    method.getModifiers().add(ElementModifier.ABSTRACT);
                    method.setProgram(null);
                } else if (method.getProgram() != null) {
                    link(method);
                }
            }
            for (FieldHolder field : classHolder.getFields().toArray(new FieldHolder[0])) {
                FieldReference fieldRef = new FieldReference(className, field.getName());
                if (dependency.getField(fieldRef) == null) {
                    classHolder.removeField(field);
                }
            }
        }
        return cutClasses;
    }

    public void link(MethodHolder cls) {
        Program program = cls.getProgram();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction)insn;
                    MethodDependencyInfo linkedMethod = dependency.getMethod(invoke.getMethod());
                    if (linkedMethod != null) {
                        invoke.setMethod(linkedMethod.getReference());
                    }
                } else if (insn instanceof GetFieldInstruction) {
                    GetFieldInstruction getField = (GetFieldInstruction)insn;
                    FieldDependencyInfo linkedField = dependency.getField(getField.getField());
                    if (linkedField != null) {
                        getField.setField(linkedField.getReference());
                    }
                } else if (insn instanceof PutFieldInstruction) {
                    PutFieldInstruction getField = (PutFieldInstruction)insn;
                    FieldDependencyInfo linkedField = dependency.getField(getField.getField());
                    if (linkedField != null) {
                        getField.setField(linkedField.getReference());
                    }
                }
            }
        }
    }
}
