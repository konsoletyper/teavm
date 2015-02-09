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
package org.teavm.tooling;

import java.util.List;
import java.util.Set;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
class ProgramSourceAggregator implements InstructionReader {
    private Set<String> sourceFiles;

    public ProgramSourceAggregator(Set<String> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public void addLocationsOfProgram(ProgramReader program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            block.readAllInstructions(this);
        }
    }

    @Override
    public void location(InstructionLocation location) {
        if (location != null && location.getFileName() != null && !location.getFileName().isEmpty()) {
            sourceFiles.add(location.getFileName());
        }
    }

    @Override public void nop() { }
    @Override public void classConstant(VariableReader receiver, ValueType cst) { }
    @Override public void nullConstant(VariableReader receiver) { }
    @Override public void integerConstant(VariableReader receiver, int cst) { }
    @Override public void longConstant(VariableReader receiver, long cst) { }
    @Override public void floatConstant(VariableReader receiver, float cst) { }
    @Override public void doubleConstant(VariableReader receiver, double cst) { }
    @Override public void stringConstant(VariableReader receiver, String cst) { }
    @Override public void binary(BinaryOperation op, VariableReader receiver, VariableReader first,
            VariableReader second, NumericOperandType type) { }
    @Override public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) { }
    @Override public void assign(VariableReader receiver, VariableReader assignee) { }
    @Override public void cast(VariableReader receiver, VariableReader value, ValueType targetType) { }
    @Override public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
            NumericOperandType targetType) { }
    @Override public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
            CastIntegerDirection targetType) { }
    @Override public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
            BasicBlockReader alternative) { }
    @Override public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
            BasicBlockReader consequent, BasicBlockReader alternative) { }
    @Override public void jump(BasicBlockReader target) { }
    @Override public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
            BasicBlockReader defaultTarget) { }
    @Override public void exit(VariableReader valueToReturn) { }
    @Override public void raise(VariableReader exception) { }
    @Override public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) { }
    @Override public void createArray(VariableReader receiver, ValueType itemType,
            List<? extends VariableReader> dimensions) { }
    @Override public void create(VariableReader receiver, String type) { }
    @Override public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
            ValueType fieldType) { }
    @Override public void putField(VariableReader instance, FieldReference field, VariableReader value) { }
    @Override public void arrayLength(VariableReader receiver, VariableReader array) { }
    @Override public void cloneArray(VariableReader receiver, VariableReader array) { }
    @Override public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) { }
    @Override public void getElement(VariableReader receiver, VariableReader array, VariableReader index) { }
    @Override public void putElement(VariableReader array, VariableReader index, VariableReader value) { }
    @Override public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) { }
    @Override public void isInstance(VariableReader receiver, VariableReader value, ValueType type) { }
    @Override public void initClass(String className) { }
    @Override public void nullCheck(VariableReader receiver, VariableReader value) { }

    @Override
    public void monitorEnter(VariableReader objectRef) {
        
    }

    @Override
    public void monitorExit(VariableReader objectRef) {
        
    }
}
