/*
 *  Copyright 2016 Alexey Andreev.
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

import java.util.Properties;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.FieldHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

public class ScalaHacks implements ClassHolderTransformer {
    private static final String ATTR_NAME_CLASS = "java.util.jar.Attributes$Name";
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        switch (cls.getName()) {
            case "scala.util.PropertiesTrait$class":
                transformPropertiesTrait(cls, context.getHierarchy());
                break;
            case "scala.util.Properties$":
                transformProperties(cls);
                break;
        }
    }

    private void transformPropertiesTrait(ClassHolder cls, ClassHierarchy hierarchy) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getName().equals("scalaProps")) {
                ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
                pe.construct(Properties.class).returnValue();
            }
        }
    }

    private void transformProperties(ClassHolder cls) {
        for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
            if (field.getName().equals("ScalaCompilerVersion")) {
                cls.removeField(field);
            }
        }
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            if (method.getName().equals("ScalaCompilerVersion")) {
                cls.removeMethod(method);
            } else if (method.getName().equals("<init>")) {
                Program program = method.getProgram();
                for (int i = 0; i < program.basicBlockCount(); ++i) {
                    BasicBlock block = program.basicBlockAt(i);
                    for (Instruction insn : block) {
                        if (insn instanceof InvokeInstruction) {
                            if (((InvokeInstruction) insn).getMethod().getClassName().equals(ATTR_NAME_CLASS)) {
                                insn.delete();
                            }
                        } else if (insn instanceof PutFieldInstruction) {
                            if (((PutFieldInstruction) insn).getField().getFieldName().equals("ScalaCompilerVersion")) {
                                insn.delete();
                            }
                        } else if (insn instanceof ConstructInstruction) {
                            ConstructInstruction cons = (ConstructInstruction) insn;
                            if (cons.getType().equals(ATTR_NAME_CLASS)) {
                                cons.setType("java.lang.Object");
                            }
                        }
                    }
                }
            }
        }
    }
}
