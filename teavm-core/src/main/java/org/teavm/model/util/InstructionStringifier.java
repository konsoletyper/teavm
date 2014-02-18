/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.List;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class InstructionStringifier implements InstructionReader {
    private StringBuilder sb;

    public InstructionStringifier(StringBuilder sb) {
        this.sb = sb;
    }

    @Override
    public void nop() {
        sb.append("nop");
    }

    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
        sb.append("@").append(receiver.getIndex()).append(" := classOf ").append(cst);
    }

    @Override
    public void nullConstant(VariableReader receiver) {
        sb.append("@").append(receiver.getIndex()).append(" := null");
    }

    @Override
    public void integerConstant(VariableReader receiver, int cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst);
    }

    @Override
    public void longConstant(VariableReader receiver, long cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst);
    }

    @Override
    public void floatConstant(VariableReader receiver, float cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst);
    }

    @Override
    public void doubleConstant(VariableReader receiver, double cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst);
    }

    @Override
    public void stringConstant(VariableReader receiver, String cst) {
        sb.append("@").append(receiver.getIndex()).append(" := '").append(cst).append("'");
    }

    @Override
    public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
            NumericOperandType type) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(first.getIndex()).append(" ");
        switch (op) {
            case ADD:
                sb.append("+");
                break;
            case AND:
                sb.append("&");
                break;
            case COMPARE:
                sb.append("compareTo");
                break;
            case DIVIDE:
                sb.append("/");
                break;
            case MODULO:
                sb.append("%");
                break;
            case MULTIPLY:
                sb.append("*");
                break;
            case OR:
                sb.append("|");
                break;
            case SHIFT_LEFT:
                sb.append("<<");
                break;
            case SHIFT_RIGHT:
                sb.append(">>");
                break;
            case SHIFT_RIGHT_UNSIGNED:
                sb.append(">>>");
                break;
            case SUBTRACT:
                sb.append("-");
                break;
            case XOR:
                sb.append("^");
                break;
        }
        sb.append(" @").append(second.getIndex());
    }

    @Override
    public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
        sb.append("@").append(receiver.getIndex()).append(" := -").append(" @").append(operand.getIndex());
    }

    @Override
    public void assign(VariableReader receiver, VariableReader assignee) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(assignee.getIndex());
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        sb.append("@").append(receiver.getIndex()).append(" := cast @").append(value.getIndex())
                .append(" to ").append(targetType);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
            NumericOperandType targetType) {
        sb.append("@").append(receiver.getIndex()).append(" := cast @").append(value.getIndex())
                .append(" from ").append(sourceType).append(" to ").append(targetType);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
            CastIntegerDirection direction) {
        sb.append("@").append(receiver.getIndex()).append(" := cast @").append(value.getIndex());
        switch (direction) {
            case FROM_INTEGER:
                sb.append(" from INT to ").append(type);
                break;
            case TO_INTEGER:
                sb.append(" from ").append(type).append(" to INT");
                break;
        }
    }

    @Override
    public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
            BasicBlockReader alternative) {
        sb.append("if @").append(operand.getIndex()).append(" ");
        switch (cond) {
            case EQUAL:
                sb.append("== 0");
                break;
            case NOT_EQUAL:
                sb.append("!= 0");
                break;
            case GREATER:
                sb.append("> 0");
                break;
            case GREATER_OR_EQUAL:
                sb.append(">= 0");
                break;
            case LESS:
                sb.append("<= 0");
                break;
            case LESS_OR_EQUAL:
                sb.append("< 0");
                break;
            case NOT_NULL:
                sb.append("!= null");
                break;
            case NULL:
                sb.append("== null");
                break;
        }
        sb.append(" then goto $").append(consequent.getIndex()).append(" else goto $").append(alternative.getIndex());
    }

    @Override
    public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
            BasicBlockReader consequent, BasicBlockReader alternative) {
        sb.append("if @").append(first.getIndex()).append(" ");
        switch (cond) {
            case EQUAL:
            case REFERENCE_EQUAL:
                sb.append("==");
                break;
            case NOT_EQUAL:
            case REFERENCE_NOT_EQUAL:
                sb.append("!=");
                break;
        }
        sb.append("@").append(second.getIndex()).append(" then goto $").append(consequent.getIndex())
                .append(" else goto $").append(alternative.getIndex());
    }

    @Override
    public void jump(BasicBlockReader target) {
        sb.append("goto $").append(target.getIndex());
    }

    @Override
    public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
            BasicBlockReader defaultTarget) {
        sb.append("switch @").append(condition.getIndex()).append(" ");
        for (int i = 0; i < table.size(); ++i) {
            if (i > 0) {
                sb.append("; ");
            }
            SwitchTableEntryReader entry = table.get(i);
            sb.append("case ").append(entry.getCondition()).append(": goto $").append(entry.getTarget());
        }
        sb.append(", default: goto $").append(defaultTarget.getIndex());
    }

    @Override
    public void exit(VariableReader valueToReturn) {
        sb.append("return");
        if (valueToReturn != null) {
            sb.append(" @").append(valueToReturn.getIndex());
        }
    }

    @Override
    public void raise(VariableReader exception) {
        sb.append("throw @").append(exception.getIndex());
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        sb.append("@").append(receiver.getIndex()).append(" = new ").append(itemType).append("[@")
                .append(size.getIndex()).append(']');
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, List<? extends VariableReader> dimensions) {
        sb.append("@").append(receiver.getIndex()).append(" = new ").append(itemType).append("[");
        for (int i = 0; i < dimensions.size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("@").append(dimensions.get(i).getIndex());
        }
        sb.append("]");
    }

    @Override
    public void create(VariableReader receiver, String type) {
        sb.append("@").append(receiver.getIndex()).append(" = new ").append(type).append("()");
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field, ValueType fieldType) {
        sb.append("@").append(receiver.getIndex()).append(" := ");
        if (instance != null) {
            sb.append("@").append(instance.getIndex());
        } else {
            sb.append(field.getClassName());
        }
        sb.append(".").append(field.getFieldName());
    }

    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value) {
        if (instance != null) {
            sb.append("@").append(instance.getIndex());
        } else {
            sb.append(field.getClassName());
        }
        sb.append(".").append(field.getFieldName()).append(" := @").append(value.getIndex());
    }

    @Override
    public void arrayLength(VariableReader receiver, VariableReader array) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(array.getIndex()).append(".length");
    }

    @Override
    public void cloneArray(VariableReader receiver, VariableReader array) {
        sb.append("@").append(receiver.getIndex()).append("@").append(array.getIndex()).append(".clone()");
    }

    @Override
    public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(array.getIndex()).append(".data");
    }

    @Override
    public void getElement(VariableReader receiver, VariableReader array, VariableReader index) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(array.getIndex()).append("[@")
                .append(index.getIndex()).append("]");
    }

    @Override
    public void putElement(VariableReader array, VariableReader index, VariableReader value) {
        sb.append("@").append(array.getIndex()).append("[@").append(index.getIndex()).append("] := @")
                .append(value.getIndex());
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        if (receiver != null) {
            sb.append("@").append(receiver.getIndex()).append(" := ");
        }
        if (instance != null) {
            sb.append("@").append(instance.getIndex());
        } else {
            sb.append(method.getClassName());
        }
        sb.append(".").append(method.getName()).append("(");
        for (int i = 0; i < arguments.size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("@").append(arguments.get(i).getIndex());
        }
        sb.append(")");
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(value.getIndex())
                .append(" instanceof ").append(type);
    }

    @Override
    public void initClass(String className) {
        sb.append("initclass ").append(className);
    }
}
