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
package org.teavm.classlib.impl;

import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class JavacSupport implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (cls.getName().equals("javax.tools.ToolProvider")) {
            MethodHolder method = cls.getMethod(new MethodDescriptor("getSystemJavaCompiler",
                    ValueType.object("javax.tools.JavaCompiler")));
            Program program = new Program();
            BasicBlock block = program.createBasicBlock();
            program.createVariable();
            Variable var = program.createVariable();
            ConstructInstruction construct = new ConstructInstruction();
            construct.setReceiver(var);
            construct.setType("com.sun.tools.javac.api.JavacTool");
            block.getInstructions().add(construct);
            InvokeInstruction init = new InvokeInstruction();
            init.setInstance(var);
            init.setType(InvocationType.SPECIAL);
            init.setMethod(new MethodReference("com.sun.tools.javac.api.JavacTool", "<init>", ValueType.VOID));
            block.getInstructions().add(init);
            ExitInstruction exit = new ExitInstruction();
            exit.setValueToReturn(var);
            block.getInstructions().add(exit);
            method.setProgram(program);
        }
    }
}
