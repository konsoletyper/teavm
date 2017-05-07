/*
 *  Copyright 2017 konsoletyper.
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
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.NumericOperandType;

public class NumericClassTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        switch (cls.getName()) {
            case "java.lang.Integer":
                transformInteger(cls);
                break;
            case "java.lang.Long":
                transformLong(cls);
                break;
            case "java.lang.Float":
                transformFloat(cls);
                break;
            case "java.lang.Double":
                transformDouble(cls);
                break;
        }
    }

    private void transformInteger(ClassHolder cls) {
        transformCompareMethod(cls, ValueType.INTEGER, NumericOperandType.INT);
    }

    private void transformLong(ClassHolder cls) {
        transformCompareMethod(cls, ValueType.LONG, NumericOperandType.LONG);
    }

    private void transformFloat(ClassHolder cls) {
        transformCompareMethod(cls, ValueType.FLOAT, NumericOperandType.FLOAT);
    }

    private void transformDouble(ClassHolder cls) {
        transformCompareMethod(cls, ValueType.DOUBLE, NumericOperandType.DOUBLE);
    }

    private void transformCompareMethod(ClassHolder cls, ValueType type, NumericOperandType insnType) {
        MethodHolder method = cls.getMethod(new MethodDescriptor("compare", type, type, ValueType.INTEGER));
        Program program = new Program();

        program.createVariable();
        Variable firstArg = program.createVariable();
        Variable secondArg = program.createVariable();
        Variable result = program.createVariable();

        BasicBlock block = program.createBasicBlock();

        BinaryInstruction insn = new BinaryInstruction(BinaryOperation.COMPARE, insnType);
        insn.setFirstOperand(firstArg);
        insn.setSecondOperand(secondArg);
        insn.setReceiver(result);
        block.add(insn);

        ExitInstruction exit = new ExitInstruction();
        exit.setValueToReturn(result);
        block.add(exit);

        method.setProgram(program);
        method.getModifiers().remove(ElementModifier.NATIVE);
    }
}
