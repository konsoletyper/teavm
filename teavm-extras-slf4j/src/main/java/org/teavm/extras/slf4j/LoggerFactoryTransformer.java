/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.extras.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLoggerFactory;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class LoggerFactoryTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (!cls.getName().equals(LoggerFactory.class.getName())) {
            return;
        }
        addCacheField(cls);
        modifyClinit(cls);
        replaceGetFactory(cls);
        cls.removeField(cls.getField("TEMP_FACTORY"));
    }

    private void addCacheField(ClassHolder cls) {
        FieldHolder cacheField = new FieldHolder("loggerFactoryCache");
        cacheField.setLevel(AccessLevel.PRIVATE);
        cacheField.getModifiers().add(ElementModifier.STATIC);
        cacheField.setType(ValueType.object(TeaVMLoggerFactory.class.getName()));
        cls.addField(cacheField);
    }

    private void modifyClinit(ClassHolder cls) {
        MethodHolder clinit = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
        BasicBlock clinitBlock = clinit.getProgram().basicBlockAt(0);
        Variable factoryVar = clinit.getProgram().createVariable();
        ConstructInstruction construct = new ConstructInstruction();
        construct.setType(TeaVMLoggerFactory.class.getName());
        construct.setReceiver(factoryVar);
        clinitBlock.getInstructions().add(0, construct);
        InvokeInstruction init = new InvokeInstruction();
        init.setInstance(factoryVar);
        init.setMethod(new MethodReference(TeaVMLoggerFactory.class, "<init>", void.class));
        init.setType(InvocationType.SPECIAL);
        clinitBlock.getInstructions().add(1, init);
        PutFieldInstruction put = new PutFieldInstruction();
        put.setValue(factoryVar);
        put.setField(new FieldReference(LoggerFactory.class.getName(), "loggerFactoryCache"));
        clinitBlock.getInstructions().add(2, put);

        Program program = clinit.getProgram();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (int j = 0; j < block.getInstructions().size(); ++j) {
                Instruction insn = block.getInstructions().get(j);
                if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction)insn;
                    if (invoke.getMethod().getClassName().equals(SubstituteLoggerFactory.class.getName())) {
                        block.getInstructions().set(j, new EmptyInstruction());
                    }
                } else if (insn instanceof PutFieldInstruction) {
                    PutFieldInstruction putField = (PutFieldInstruction)insn;
                    if (putField.getField().getFieldName().equals("TEMP_FACTORY")) {
                        block.getInstructions().set(j, new EmptyInstruction());
                    }
                }
            }
        }
    }

    private void replaceGetFactory(ClassHolder cls) {
        MethodHolder method = cls.getMethod(new MethodDescriptor("getILoggerFactory", ILoggerFactory.class));
        Program program = new Program();
        BasicBlock block = program.createBasicBlock();
        Variable cacheVar = program.createVariable();
        GetFieldInstruction get = new GetFieldInstruction();
        get.setField(new FieldReference(LoggerFactory.class.getName(), "loggerFactoryCache"));
        get.setFieldType(ValueType.object(ILoggerFactory.class.getName()));
        get.setReceiver(cacheVar);
        block.getInstructions().add(get);
        ExitInstruction exit = new ExitInstruction();
        exit.setValueToReturn(cacheVar);
        block.getInstructions().add(exit);
        method.setProgram(program);
    }
}
