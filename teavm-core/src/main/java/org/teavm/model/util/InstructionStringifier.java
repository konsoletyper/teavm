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
import org.teavm.model.Variable;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class InstructionStringifier implements InstructionVisitor {
    private StringBuilder sb;

    public InstructionStringifier(StringBuilder sb) {
        this.sb = sb;
    }

    @Override
    public void visit(EmptyInstruction insn) {
        sb.append("nop");
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := classOf ")
                .append(insn.getConstant());
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := null");
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := ")
                .append(insn.getConstant());
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := ")
                .append(insn.getConstant());
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := ")
                .append(insn.getConstant());
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := ")
                .append(insn.getConstant());
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := '")
                .append(insn.getConstant()).append("'");
    }

    @Override
    public void visit(BinaryInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := @")
                .append(insn.getFirstOperand().getIndex()).append(" ");
        switch (insn.getOperation()) {
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
        sb.append(" @").append(insn.getSecondOperand().getIndex());
    }

    @Override
    public void visit(NegateInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := -")
                .append(" @").append(insn.getOperand().getIndex());
    }

    @Override
    public void visit(AssignInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := @")
                .append(insn.getAssignee().getIndex());
    }

    @Override
    public void visit(BranchingInstruction insn) {
        sb.append("if @").append(insn.getOperand().getIndex()).append(" ");
        switch (insn.getCondition()) {
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
        sb.append(" then goto $").append(insn.getConsequent().getIndex()).append(" else goto $")
                .append(insn.getAlternative().getIndex());
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        sb.append("if @").append(insn.getFirstOperand().getIndex()).append(" ");
        switch (insn.getCondition()) {
            case EQUAL:
            case REFERENCE_EQUAL:
                sb.append("==");
                break;
            case NOT_EQUAL:
            case REFERENCE_NOT_EQUAL:
                sb.append("!=");
                break;
        }
        sb.append("@").append(insn.getSecondOperand().getIndex())
                .append(" then goto $").append(insn.getConsequent().getIndex())
                .append(" else goto $").append(insn.getAlternative().getIndex());
    }

    @Override
    public void visit(JumpInstruction insn) {
        sb.append("goto $").append(insn.getTarget().getIndex());
    }

    @Override
    public void visit(SwitchInstruction insn) {
        sb.append("switch @").append(insn.getCondition().getIndex()).append(" ");
        List<SwitchTableEntry> entries = insn.getEntries();
        for (int i = 0; i < entries.size(); ++i) {
            if (i > 0) {
                sb.append("; ");
            }
            SwitchTableEntry entry = entries.get(i);
            sb.append("case ").append(entry.getCondition()).append(": goto $")
                    .append(entry.getTarget());
        }
        sb.append(", default: goto $").append(insn.getDefaultTarget());
    }

    @Override
    public void visit(ExitInstruction insn) {
        sb.append("return");
        if (insn.getValueToReturn() != null) {
            sb.append(" @").append(insn.getValueToReturn().getIndex());
        }
    }

    @Override
    public void visit(RaiseInstruction insn) {
        sb.append("throw @").append(insn.getException().getIndex());
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" = new ")
                .append(insn.getItemType()).append("[@").append(insn.getSize().getIndex())
                .append(']');
    }

    @Override
    public void visit(ConstructInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" = new ")
                .append(insn.getType()).append("()");
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" = new ")
                .append(insn.getItemType()).append("[");
        List<Variable> dimensions = insn.getDimensions();
        for (int i = 0; i < dimensions.size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            Variable dimension = dimensions.get(i);
            sb.append("@").append(dimension.getIndex());
        }
        sb.append("]");
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := ");
        if (insn.getInstance() != null) {
            sb.append("@").append(insn.getInstance().getIndex());
        } else {
            sb.append(insn.getField().getClassName());
        }
        sb.append(".").append(insn.getField().getFieldName());
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        if (insn.getInstance() != null) {
            sb.append("@").append(insn.getInstance().getIndex());
        } else {
            sb.append(insn.getField().getClassName());
        }
        sb.append(".").append(insn.getField().getFieldName()).append(" := @").append(insn.getValue().getIndex());
    }

    @Override
    public void visit(GetElementInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := @")
                .append(insn.getArray().getIndex()).append("[@")
                .append(insn.getIndex().getIndex()).append("]");
    }

    @Override
    public void visit(PutElementInstruction insn) {
        sb.append("@").append(insn.getArray().getIndex()).append("[@")
                .append(insn.getIndex().getIndex()).append("] := @")
                .append(insn.getValue().getIndex());
    }

    @Override
    public void visit(InvokeInstruction insn) {
        if (insn.getReceiver() != null) {
            sb.append("@").append(insn.getReceiver().getIndex()).append(" := ");
        }
        if (insn.getInstance() != null) {
            sb.append("@").append(insn.getInstance().getIndex());
        } else {
            sb.append(insn.getMethod().getClassName());
        }
        sb.append(".").append(insn.getMethod().getName()).append("(");
        List<Variable> arguments = insn.getArguments();
        for (int i = 0; i < arguments.size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("@").append(arguments.get(i).getIndex());
        }
        sb.append(")");
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := @")
                .append(insn.getValue().getIndex()).append(" instanceof ").append(insn.getType());
    }

    @Override
    public void visit(CastInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := cast @")
                .append(insn.getValue().getIndex()).append(" to ")
                .append(insn.getTargetType());
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := cast @")
                .append(insn.getValue().getIndex())
                .append(" from ").append(insn.getSourceType())
                .append(" to ").append(insn.getTargetType());
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := cast @")
                .append(insn.getValue().getIndex())
                .append(" from INT to ").append(insn.getTargetType());
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := @")
                .append(insn.getArray().getIndex()).append(".data");
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append(" := @")
                .append(insn.getArray().getIndex()).append(".length");
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        sb.append("@").append(insn.getReceiver().getIndex()).append("@")
                .append(insn.getArray().getIndex()).append(".clone()");
    }

    @Override
    public void visit(InitClassInstruction insn) {
        sb.append("initclass ").append(insn.getClassName());
    }
}
