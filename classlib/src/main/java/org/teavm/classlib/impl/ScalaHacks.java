package org.teavm.classlib.impl;

import java.util.List;
import java.util.Properties;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class ScalaHacks implements ClassHolderTransformer {
    private static final String ATTR_NAME_CLASS = "java.util.jar.Attributes$Name";
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        switch (cls.getName()) {
            case "scala.util.PropertiesTrait$class":
                transformPropertiesTrait(cls, innerSource);
                break;
            case "scala.util.Properties$":
                transformProperties(cls);
                break;
        }
    }

    private void transformPropertiesTrait(ClassHolder cls, ClassReaderSource innerSource) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getName().equals("scalaProps")) {
                ProgramEmitter pe = ProgramEmitter.create(method, innerSource);
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
                    List<Instruction> instructions = block.getInstructions();
                    for (int j = 0; j < instructions.size(); ++j) {
                        Instruction insn = instructions.get(j);
                        if (insn instanceof InvokeInstruction) {
                            if (((InvokeInstruction) insn).getMethod().getClassName().equals(ATTR_NAME_CLASS)) {
                                instructions.remove(j--);
                            }
                        } else if (insn instanceof PutFieldInstruction) {
                            if (((PutFieldInstruction) insn).getField().getFieldName().equals("ScalaCompilerVersion")) {
                                instructions.remove(j--);
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
