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

import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.RaiseInstruction;

public class JCLHacks implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals("java.lang.Thread")) {
            installThreadMethods(cls);
        }
    }

    private void installThreadMethods(ClassHolder cls) {
        cls.addMethod(createMethodThrowingSecurityException(new MethodDescriptor("setName", String.class, void.class)));
        cls.addMethod(createMethodThrowingSecurityException(new MethodDescriptor("setDaemon",
                boolean.class, void.class)));
    }

    private MethodHolder createMethodThrowingSecurityException(MethodDescriptor desc) {
        MethodHolder method = new MethodHolder(desc);
        Program program = new Program();
        for (int i = 0; i < desc.parameterCount(); ++i) {
            program.createVariable();
        }
        program.createVariable();
        program.createVariable();
        Variable var = program.createVariable();
        BasicBlock block = program.createBasicBlock();
        ConstructInstruction cons = new ConstructInstruction();
        cons.setType("java.lang.SecurityException");
        cons.setReceiver(var);
        block.add(cons);
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setInstance(var);
        invoke.setMethod(new MethodReference(SecurityException.class, "<init>", void.class));
        block.add(invoke);
        RaiseInstruction raise = new RaiseInstruction();
        raise.setException(var);
        block.add(raise);
        method.setLevel(AccessLevel.PUBLIC);
        method.setProgram(program);
        return method;
    }
}
