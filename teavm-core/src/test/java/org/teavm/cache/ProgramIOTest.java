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
package org.teavm.cache;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class ProgramIOTest {
    @Test
    public void emptyInstruction() {
        Program program = new Program();
        BasicBlock block = program.createBasicBlock();
        block.getInstructions().add(new EmptyInstruction());

        program = inputOutput(program);
        block = program.basicBlockAt(0);

        assertThat(block.getInstructions().size(), is(1));
        assertThat(block.getInstructions().get(0), instanceOf(EmptyInstruction.class));
    }

    @Test
    public void constants() {
        Program program = new Program();
        BasicBlock block = program.createBasicBlock();
        ClassConstantInstruction classConstInsn = new ClassConstantInstruction();
        classConstInsn.setConstant(ValueType.BYTE);
        classConstInsn.setReceiver(program.createVariable());
        block.getInstructions().add(classConstInsn);
        NullConstantInstruction nullConstInsn = new NullConstantInstruction();
        nullConstInsn.setReceiver(program.createVariable());
        block.getInstructions().add(nullConstInsn);
        IntegerConstantInstruction intConsInsn = new IntegerConstantInstruction();
        intConsInsn.setReceiver(program.createVariable());
        intConsInsn.setConstant(23);
        block.getInstructions().add(intConsInsn);
        LongConstantInstruction longConsInsn = new LongConstantInstruction();
        longConsInsn.setReceiver(program.createVariable());
        longConsInsn.setConstant(234);
        block.getInstructions().add(longConsInsn);
        FloatConstantInstruction floatConsInsn = new FloatConstantInstruction();
        floatConsInsn.setReceiver(program.createVariable());
        floatConsInsn.setConstant(3.14f);
        block.getInstructions().add(floatConsInsn);
        DoubleConstantInstruction doubleConsInsn = new DoubleConstantInstruction();
        doubleConsInsn.setReceiver(program.createVariable());
        doubleConsInsn.setConstant(3.14159);
        block.getInstructions().add(doubleConsInsn);
        StringConstantInstruction stringConsInsn = new StringConstantInstruction();
        stringConsInsn.setReceiver(program.createVariable());
        stringConsInsn.setConstant("foo");
        block.getInstructions().add(stringConsInsn);

        program = inputOutput(program);
        block = program.basicBlockAt(0);

        assertThat(block.getInstructions().size(), is(7));
        assertThat(block.getInstructions().get(0), instanceOf(ClassConstantInstruction.class));
        assertThat(block.getInstructions().get(1), instanceOf(NullConstantInstruction.class));
        assertThat(block.getInstructions().get(2), instanceOf(IntegerConstantInstruction.class));
        assertThat(block.getInstructions().get(3), instanceOf(LongConstantInstruction.class));
        assertThat(block.getInstructions().get(4), instanceOf(FloatConstantInstruction.class));
        assertThat(block.getInstructions().get(5), instanceOf(DoubleConstantInstruction.class));
        assertThat(block.getInstructions().get(6), instanceOf(StringConstantInstruction.class));

        classConstInsn = (ClassConstantInstruction)block.getInstructions().get(0);
        assertThat(classConstInsn.getReceiver().getIndex(), is(0));
        assertThat(classConstInsn.getConstant().toString(), is(ValueType.BYTE.toString()));

        nullConstInsn = (NullConstantInstruction)block.getInstructions().get(1);
        assertThat(nullConstInsn.getReceiver().getIndex(), is(1));

        intConsInsn = (IntegerConstantInstruction)block.getInstructions().get(2);
        assertThat(intConsInsn.getConstant(), is(23));
        assertThat(intConsInsn.getReceiver().getIndex(), is(2));

        longConsInsn = (LongConstantInstruction)block.getInstructions().get(3);
        assertThat(longConsInsn.getConstant(), is(234L));
        assertThat(longConsInsn.getReceiver().getIndex(), is(3));

        floatConsInsn = (FloatConstantInstruction)block.getInstructions().get(4);
        assertThat(floatConsInsn.getConstant(), is(3.14f));
        assertThat(floatConsInsn.getReceiver().getIndex(), is(4));

        doubleConsInsn = (DoubleConstantInstruction)block.getInstructions().get(5);
        assertThat(doubleConsInsn.getConstant(), is(3.14159));
        assertThat(doubleConsInsn.getReceiver().getIndex(), is(5));

        stringConsInsn = (StringConstantInstruction)block.getInstructions().get(6);
        assertThat(stringConsInsn.getConstant(), is("foo"));
        assertThat(stringConsInsn.getReceiver().getIndex(), is(6));
    }

    @Test
    public void binaryOperation() {
        Program program = new Program();
        BasicBlock block = program.createBasicBlock();
        IntegerConstantInstruction constInsn = new IntegerConstantInstruction();
        constInsn.setConstant(3);
        constInsn.setReceiver(program.createVariable());
        block.getInstructions().add(constInsn);
        constInsn = new IntegerConstantInstruction();
        constInsn.setConstant(2);
        constInsn.setReceiver(program.createVariable());
        block.getInstructions().add(constInsn);
        BinaryInstruction addInsn = new BinaryInstruction(BinaryOperation.ADD, NumericOperandType.INT);
        addInsn.setFirstOperand(program.variableAt(0));
        addInsn.setSecondOperand(program.variableAt(1));
        addInsn.setReceiver(program.createVariable());
        block.getInstructions().add(addInsn);
        BinaryInstruction subInsn = new BinaryInstruction(BinaryOperation.SUBTRACT, NumericOperandType.INT);
        subInsn.setFirstOperand(program.variableAt(2));
        subInsn.setSecondOperand(program.variableAt(0));
        subInsn.setReceiver(program.createVariable());
        block.getInstructions().add(subInsn);

        assertThat(block.getInstructions().size(), is(4));
        assertThat(block.getInstructions().get(2), instanceOf(BinaryInstruction.class));
        assertThat(block.getInstructions().get(3), instanceOf(BinaryInstruction.class));

        addInsn = (BinaryInstruction)block.getInstructions().get(2);
        assertThat(addInsn.getOperation(), is(BinaryOperation.ADD));
        assertThat(addInsn.getOperandType(), is(NumericOperandType.INT));
        assertThat(addInsn.getFirstOperand().getIndex(), is(0));
        assertThat(addInsn.getSecondOperand().getIndex(), is(1));
        assertThat(addInsn.getReceiver().getIndex(), is(2));

        subInsn = (BinaryInstruction)block.getInstructions().get(3);
        assertThat(subInsn.getOperation(), is(BinaryOperation.SUBTRACT));
        assertThat(subInsn.getOperandType(), is(NumericOperandType.INT));
        assertThat(subInsn.getFirstOperand().getIndex(), is(2));
        assertThat(subInsn.getSecondOperand().getIndex(), is(0));
        assertThat(subInsn.getReceiver().getIndex(), is(3));
    }

    private Program inputOutput(Program program) {
        InMemorySymbolTable symbolTable = new InMemorySymbolTable();
        InMemorySymbolTable fileTable = new InMemorySymbolTable();
        ProgramIO programIO = new ProgramIO(symbolTable, fileTable);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            programIO.write(program, output);
            try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
                return programIO.read(input);
            }
        } catch (IOException e) {
            throw new AssertionError("This exception should not be thrown", e);
        }
    }

    private static class InMemorySymbolTable implements SymbolTable {
        private List<String> symbols = new ArrayList<>();
        private Map<String, Integer> indexes = new HashMap<>();

        @Override
        public String at(int index) {
            return symbols.get(index);
        }

        @Override
        public int lookup(String symbol) {
            Integer index = indexes.get(symbol);
            if (index == null) {
                index = symbols.size();
                symbols.add(symbol);
                indexes.put(symbol, index);
            }
            return index;
        }
    }
}
