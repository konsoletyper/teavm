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
package org.teavm.model.instructions;

import java.util.List;
import org.teavm.model.*;

public interface InstructionReader {
    void location(TextLocation location);

    void nop();

    void classConstant(VariableReader receiver, ValueType cst);

    void nullConstant(VariableReader receiver);

    void integerConstant(VariableReader receiver, int cst);

    void longConstant(VariableReader receiver, long cst);

    void floatConstant(VariableReader receiver, float cst);

    void doubleConstant(VariableReader receiver, double cst);

    void stringConstant(VariableReader receiver, String cst);

    void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
            NumericOperandType type);

    void negate(VariableReader receiver, VariableReader operand, NumericOperandType type);

    void assign(VariableReader receiver, VariableReader assignee);

    void cast(VariableReader receiver, VariableReader value, ValueType targetType);

    void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
            NumericOperandType targetType);

    void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
            CastIntegerDirection targetType);

    void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
            BasicBlockReader alternative);

    void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
            BasicBlockReader consequent, BasicBlockReader alternative);

    void jump(BasicBlockReader target);

    void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
            BasicBlockReader defaultTarget);

    void exit(VariableReader valueToReturn);

    void raise(VariableReader exception);

    void createArray(VariableReader receiver, ValueType itemType, VariableReader size);

    void createArray(VariableReader receiver, ValueType itemType, List<? extends VariableReader> dimensions);

    void create(VariableReader receiver, String type);

    void getField(VariableReader receiver, VariableReader instance, FieldReference field, ValueType fieldType);

    void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType);

    void arrayLength(VariableReader receiver, VariableReader array);

    void cloneArray(VariableReader receiver, VariableReader array);

    void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType);

    void getElement(VariableReader receiver, VariableReader array, VariableReader index, ArrayElementType elementType);

    void putElement(VariableReader array, VariableReader index, VariableReader value, ArrayElementType elementType);

    void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type);

    void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
            List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
            List<RuntimeConstant> bootstrapArguments);

    void isInstance(VariableReader receiver, VariableReader value, ValueType type);

    void initClass(String className);

    void nullCheck(VariableReader receiver, VariableReader value);

    void monitorEnter(VariableReader objectRef);

    void monitorExit(VariableReader objectRef);

    void boundCheck(VariableReader receiver, VariableReader index, VariableReader array, boolean lower);
}
