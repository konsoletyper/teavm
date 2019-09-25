/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.incremental;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class EntryPointTransformer implements ClassHolderTransformer {
    private String entryPoint;

    public EntryPointTransformer(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!cls.getName().equals(EntryPoint.class.getName())) {
            return;
        }

        MethodHolder method = cls.getMethod(new MethodDescriptor("run", String.class));
        method.getModifiers().remove(ElementModifier.NATIVE);

        Program program = new Program();
        program.createVariable();
        BasicBlock block = program.createBasicBlock();

        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(entryPoint, "run", ValueType.object("java.lang.String")));
        invoke.setReceiver(program.createVariable());
        block.add(invoke);

        ExitInstruction exit = new ExitInstruction();
        exit.setValueToReturn(invoke.getReceiver());
        block.add(exit);

        method.setProgram(program);
    }
}
