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

import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.transformation.ClassInitInsertion;

public class Linker {
    private DependencyInfo dependency;
    private ClassInitInsertion classInitInsertion;

    public Linker(DependencyInfo dependency) {
        this.dependency = dependency;
        classInitInsertion = new ClassInitInsertion(dependency);
    }

    public void link(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            MethodReference methodRef = method.getReference();
            MethodDependencyInfo methodDep = dependency.getMethod(methodRef);
            if (methodDep == null || !methodDep.isUsed()) {
                if (method.hasModifier(ElementModifier.STATIC)) {
                    cls.removeMethod(method);
                } else {
                    method.getModifiers().add(ElementModifier.ABSTRACT);
                    method.getModifiers().remove(ElementModifier.NATIVE);
                    method.setProgram(null);
                }
            } else if (method.getProgram() != null) {
                link(method, method.getProgram());
            }
        }
        for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
            FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
            if (dependency.getField(fieldRef) == null) {
                cls.removeField(field);
            }
        }
    }

    public void link(MethodReader method, Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) insn;
                    MethodReference calledRef = invoke.getMethod();
                    if (invoke.getType() == InvocationType.SPECIAL) {
                        MethodDependencyInfo linkedMethod = dependency.getMethodImplementation(calledRef);
                        if (linkedMethod != null) {
                            invoke.setMethod(linkedMethod.getReference());
                        }
                    } else if (invoke.getType() == InvocationType.VIRTUAL) {
                        MethodDependencyInfo linkedMethod = dependency.getMethodImplementation(calledRef);
                        if (linkedMethod == null || linkedMethod.isMissing()) {
                            continue;
                        }
                        calledRef = linkedMethod.getReference();
                        ClassReader cls = dependency.getClassSource().get(calledRef.getClassName());
                        boolean isFinal = false;
                        if (cls != null) {
                            if (cls.hasModifier(ElementModifier.FINAL)) {
                                isFinal = true;
                            } else {
                                MethodReader calledMethod = cls.getMethod(calledRef.getDescriptor());
                                if (calledMethod != null) {
                                    if (calledMethod.hasModifier(ElementModifier.FINAL)
                                            || calledMethod.getLevel() == AccessLevel.PRIVATE) {
                                        isFinal = true;
                                    }
                                }
                            }
                        }
                        if (isFinal) {
                            invoke.setType(InvocationType.SPECIAL);
                            invoke.setMethod(calledRef);
                        }
                    }
                } else if (insn instanceof GetFieldInstruction) {
                    GetFieldInstruction getField = (GetFieldInstruction) insn;
                    FieldDependencyInfo linkedField = dependency.getField(getField.getField());
                    if (linkedField != null) {
                        getField.setField(linkedField.getReference());
                    }

                } else if (insn instanceof PutFieldInstruction) {
                    PutFieldInstruction putField = (PutFieldInstruction) insn;
                    FieldDependencyInfo linkedField = dependency.getField(putField.getField());
                    if (linkedField != null) {
                        putField.setField(linkedField.getReference());
                    }
                }
            }
        }

        classInitInsertion.apply(program, method);
    }
}
