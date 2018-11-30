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
package org.teavm.dependency;

import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class DependencyTestPatcher implements ClassHolderTransformer {
    private String className;
    private String methodName;

    public DependencyTestPatcher(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(className)) {
            MethodHolder method = new MethodHolder("main", ValueType.parse(String[].class), ValueType.VOID);
            method.setLevel(AccessLevel.PUBLIC);
            method.getModifiers().add(ElementModifier.STATIC);

            Program program = new Program();
            program.createVariable();
            program.createVariable();
            BasicBlock block = program.createBasicBlock();
            method.setProgram(program);

            InvokeInstruction invoke = new InvokeInstruction();
            invoke.setType(InvocationType.SPECIAL);
            invoke.setMethod(new MethodReference(className, methodName, ValueType.VOID));
            block.add(invoke);

            block.add(new ExitInstruction());

            cls.addMethod(method);
        }
    }
}
