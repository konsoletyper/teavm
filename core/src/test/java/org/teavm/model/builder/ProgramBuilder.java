/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.model.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.Variable;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;

public class ProgramBuilder {
    private Program program = new Program();
    private Map<String, Variable> variables = new HashMap<>();
    private Map<String, BasicBlock> blocks = new HashMap<>();
    private List<BasicBlock> blockOrder = new ArrayList<>();
    private BasicBlock currentBlock;
    private SetBuilder setBuilder = new SetBuilder();
    private static ProgramBuilder currentBuilder;
    private static ProgramBuilder instance = new ProgramBuilder();

    ProgramBuilder() {
        initProgram();
    }

    public static Program build(Runnable runnable) {
        currentBuilder = instance;
        runnable.run();
        currentBuilder = null;
        return instance.build();
    }

    Program build() {
        program.rearrangeBasicBlocks(blockOrder);
        var result = program;
        variables.clear();
        blocks.clear();
        blockOrder.clear();
        program = new Program();
        initProgram();
        return result;
    }

    private void initProgram() {
        currentBlock = program.createBasicBlock();
        blockOrder.add(currentBlock);
    }

    public static Variable var(String name) {
        return currentBuilder.variables.computeIfAbsent(name, k -> {
            var v = currentBuilder.program.createVariable();
            v.setDebugName(name);
            return v;
        });
    }

    public static BasicBlock label(String name) {
        return currentBuilder.blocks.computeIfAbsent(name, k -> currentBuilder.program.createBasicBlock());
    }

    public static SwitchTableEntry switchEntry(String label, int condition) {
        var entry = new SwitchTableEntry();
        entry.setTarget(label(label));
        entry.setCondition(condition);
        return entry;
    }

    public static void put(BasicBlock block) {
        currentBuilder.blockOrder.add(block);
        currentBuilder.currentBlock = block;
    }

    public static SetBuilder set(Variable var) {
        currentBuilder.setBuilder.receiver = var;
        return currentBuilder.setBuilder;
    }

    public static void exit() {
        var instruction = new ExitInstruction();
        append(instruction);
    }

    public static void exit(Variable value) {
        var instruction = new ExitInstruction();
        instruction.setValueToReturn(value);
        append(instruction);
    }

    public static void nop() {
        var instruction = new EmptyInstruction();
        append(instruction);
    }

    public static void invokeStaticMethod(MethodReference method, Variable... arguments) {
        var instruction = new InvokeInstruction();
        instruction.setType(InvocationType.SPECIAL);
        instruction.setMethod(method);
        instruction.setArguments(arguments);
        append(instruction);
    }

    public static void jump(BasicBlock block) {
        var instruction = new JumpInstruction();
        instruction.setTarget(block);
        append(instruction);
    }

    public static void ifLessThanZero(Variable a, BasicBlock ifTrue, BasicBlock ifFalse) {
        var instruction = new BranchingInstruction(BranchingCondition.LESS);
        instruction.setOperand(a);
        instruction.setConsequent(ifTrue);
        instruction.setAlternative(ifFalse);
        append(instruction);
    }

    public static void tableSwitch(Variable a, BasicBlock defaultTarget, SwitchTableEntry... entries) {
        var instruction = new SwitchInstruction();
        instruction.setCondition(a);
        instruction.setDefaultTarget(defaultTarget);
        instruction.getEntries().addAll(List.of(entries));
        append(instruction);
    }

    public static NumericOperandType intNum() {
        return NumericOperandType.INT;
    }

    public static NumericOperandType longNum() {
        return NumericOperandType.LONG;
    }

    public static NumericOperandType floatNum() {
        return NumericOperandType.FLOAT;
    }

    public static NumericOperandType doubleNum() {
        return NumericOperandType.DOUBLE;
    }

    private static void append(Instruction instruction) {
        currentBuilder.addInstruction(instruction);
    }

    private void addInstruction(Instruction instruction) {
        currentBlock.add(instruction);
    }

    public static void doTry(String exceptionType, BasicBlock handler) {
        var tryCatch = new TryCatchBlock();
        tryCatch.setExceptionType(exceptionType);
        tryCatch.setHandler(handler);
        currentBuilder.currentBlock.getTryCatchBlocks().add(tryCatch);
    }

    public static void doCatch(Variable variable) {
        currentBuilder.currentBlock.setExceptionVariable(variable);
    }

    public class SetBuilder {
        Variable receiver;

        SetBuilder() {
        }

        public void constant(int value) {
            var instruction = new IntegerConstantInstruction();
            instruction.setReceiver(receiver);
            instruction.setConstant(value);
            addInstruction(instruction);
        }

        public void add(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.ADD, type, a, b);
        }

        public void sub(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.SUBTRACT, type, a, b);
        }

        public void mul(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.MULTIPLY, type, a, b);
        }

        public void div(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.DIVIDE, type, a, b);
        }

        public void mod(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.MODULO, type, a, b);
        }

        public void and(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.AND, type, a, b);
        }

        public void or(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.OR, type, a, b);
        }

        public void xor(NumericOperandType type, Variable a, Variable b) {
            binary(BinaryOperation.XOR, type, a, b);
        }

        private void binary(BinaryOperation op, NumericOperandType type, Variable a, Variable b) {
            var instruction = new BinaryInstruction(op, type);
            instruction.setFirstOperand(a);
            instruction.setSecondOperand(b);
            instruction.setReceiver(receiver);
            addInstruction(instruction);
        }

        public void invokeStatic(MethodReference method, Variable... arguments) {
            var instruction = new InvokeInstruction();
            instruction.setReceiver(receiver);
            instruction.setType(InvocationType.SPECIAL);
            instruction.setMethod(method);
            instruction.setArguments(arguments);
            addInstruction(instruction);
        }
    }
}
