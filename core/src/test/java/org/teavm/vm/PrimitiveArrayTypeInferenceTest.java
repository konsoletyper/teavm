/*
 * Copyright 2026 Anton Banchev.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.teavm.vm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.util.TypeInferer;
import org.teavm.model.util.VariableType;

public class PrimitiveArrayTypeInferenceTest {
    @Test
    public void primitiveLoadsThroughNullableArrayPhisKeepTheirNumericType() {
        assertLoadType(ArrayElementType.INT, ValueType.INTEGER, VariableType.INT);
        assertLoadType(ArrayElementType.FLOAT, ValueType.FLOAT, VariableType.FLOAT);
        assertLoadType(ArrayElementType.LONG, ValueType.LONG, VariableType.LONG);
        assertLoadType(ArrayElementType.DOUBLE, ValueType.DOUBLE, VariableType.DOUBLE);
        assertLoadType(ArrayElementType.BYTE, ValueType.BYTE, VariableType.INT);
        assertLoadType(ArrayElementType.SHORT, ValueType.SHORT, VariableType.INT);
        assertLoadType(ArrayElementType.CHAR, ValueType.CHARACTER, VariableType.INT);
    }

    private void assertLoadType(ArrayElementType element, ValueType type, VariableType expected) {
        Program program = new Program();
        for (int i = 0; i < 7; i++) {
            program.createVariable();
        }
        BasicBlock entry = program.createBasicBlock();
        BasicBlock present = program.createBasicBlock();
        BasicBlock absent = program.createBasicBlock();
        BasicBlock join = program.createBasicBlock();
        BranchingInstruction branch = new BranchingInstruction(BranchingCondition.NOT_EQUAL);
        branch.setOperand(program.variableAt(2));
        branch.setConsequent(present);
        branch.setAlternative(absent);
        entry.add(branch);
        JumpInstruction jump = new JumpInstruction();
        jump.setTarget(join);
        present.add(jump);
        NullConstantInstruction nil = new NullConstantInstruction();
        nil.setReceiver(program.variableAt(3));
        absent.add(nil);
        JumpInstruction nullJump = new JumpInstruction();
        nullJump.setTarget(join);
        absent.add(nullJump);
        Phi phi = new Phi();
        phi.setReceiver(program.variableAt(4));
        Incoming value = new Incoming();
        value.setSource(present);
        value.setValue(program.variableAt(1));
        phi.getIncomings().add(value);
        Incoming missing = new Incoming();
        missing.setSource(absent);
        missing.setValue(program.variableAt(3));
        phi.getIncomings().add(missing);
        join.getPhis().add(phi);
        IntegerConstantInstruction index = new IntegerConstantInstruction();
        index.setReceiver(program.variableAt(5));
        index.setConstant(0);
        join.add(index);
        GetElementInstruction load = new GetElementInstruction(element);
        load.setArray(program.variableAt(4));
        load.setIndex(program.variableAt(5));
        load.setReceiver(program.variableAt(6));
        join.add(load);
        ExitInstruction exit = new ExitInstruction();
        exit.setValueToReturn(program.variableAt(6));
        join.add(exit);
        TypeInferer inferer = new TypeInferer();
        inferer.inferTypes(program, new MethodReference("Example", "read",
                ValueType.arrayOf(type), ValueType.BOOLEAN, type));
        assertEquals(element.toString(), expected, inferer.typeOf(6));
    }
}
