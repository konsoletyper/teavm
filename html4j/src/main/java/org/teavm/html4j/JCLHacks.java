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
package org.teavm.html4j;

import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class JCLHacks implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (cls.getName().equals("java.lang.Thread")) {
            installThreadMethods(cls);
        }
    }

    private void installThreadMethods(ClassHolder cls) {
        cls.addMethod(createMethodThrowingSecurityException(new MethodDescriptor("setName", String.class, void.class),
                false));
        cls.addMethod(createMethodThrowingSecurityException(new MethodDescriptor("setDaemon",
                boolean.class, void.class), false));
    }

    private MethodHolder createMethodThrowingSecurityException(MethodDescriptor desc, boolean staticMethod) {
        MethodHolder method = new MethodHolder(desc);
        Program program = new Program();
        for (int i = 0; i < desc.parameterCount(); ++i) {
            program.createVariable();
        }
        if (!staticMethod) {
            program.createVariable();
        }
        program.createVariable();
        Variable var = program.createVariable();
        BasicBlock block = program.createBasicBlock();
        ConstructInstruction cons = new ConstructInstruction();
        cons.setType("java.lang.SecurityException");
        cons.setReceiver(var);
        block.getInstructions().add(cons);
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setInstance(var);
        invoke.setMethod(new MethodReference(SecurityException.class, "<init>", void.class));
        block.getInstructions().add(invoke);
        RaiseInstruction raise = new RaiseInstruction();
        raise.setException(var);
        block.getInstructions().add(raise);
        if (staticMethod) {
            method.getModifiers().add(ElementModifier.STATIC);
        }
        method.setLevel(AccessLevel.PUBLIC);
        method.setProgram(program);
        return method;
    }
}
