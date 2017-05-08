/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model.text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

class InstructionStringifier implements InstructionReader {
    private TextLocation location;
    private StringBuilder sb;
    private String[] variableLabels;

    InstructionStringifier(StringBuilder sb, ProgramReader program) {
        this.sb = sb;

        variableLabels = new String[program.variableCount()];
        Set<String> occupiedLabels = new HashSet<>();
        for (int i = 0; i < program.variableCount(); ++i) {
            VariableReader var = program.variableAt(i);
            String suggestedName = var.getLabel() != null ? var.getLabel() : Integer.toString(i);
            if (!occupiedLabels.add(suggestedName)) {
                int suffix = 1;
                String base = suggestedName + "_";
                do {
                    suggestedName = base + suffix++;
                } while (!occupiedLabels.add(suggestedName));
            }
            variableLabels[i] = suggestedName;
        }
    }

    public String getVariableLabel(int index) {
        return variableLabels[index];
    }

    public TextLocation getLocation() {
        return location;
    }

    @Override
    public void location(TextLocation location) {
        this.location = location;
    }

    @Override
    public void nop() {
        sb.append("nop");
    }

    InstructionStringifier append(String str) {
        sb.append(str);
        return this;
    }

    InstructionStringifier append(int value) {
        sb.append(value);
        return this;
    }

    InstructionStringifier append(char value) {
        sb.append(value);
        return this;
    }

    InstructionStringifier appendLocalVar(VariableReader var) {
        return append("@").append(variableLabels[var.getIndex()]);
    }

    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
        appendLocalVar(receiver).append(" := classOf ").escapeIdentifierIfNeeded(cst.toString());
    }

    @Override
    public void nullConstant(VariableReader receiver) {
        appendLocalVar(receiver).append(" := null");
    }

    @Override
    public void integerConstant(VariableReader receiver, int cst) {
        appendLocalVar(receiver).append(" := " + cst);
    }

    @Override
    public void longConstant(VariableReader receiver, long cst) {
        appendLocalVar(receiver).append(" := " + cst + "L");
    }

    @Override
    public void floatConstant(VariableReader receiver, float cst) {
        appendLocalVar(receiver).append(" := " + cst + 'F');
    }

    @Override
    public void doubleConstant(VariableReader receiver, double cst) {
        appendLocalVar(receiver).append(" := " + cst);
    }

    @Override
    public void stringConstant(VariableReader receiver, String cst) {
        appendLocalVar(receiver).append(" := '");
        escapeStringLiteral(cst, sb);
        sb.append("'");
    }

    static void escapeStringLiteral(String s, StringBuilder sb) {
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < ' ') {
                        sb.append("\\u");
                        int pos = 12;
                        for (int j = 0; j < 4; ++j) {
                            sb.append(Character.forDigit((c >> pos) & 0xF, 16));
                            pos -= 4;
                        }
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
    }

    private InstructionStringifier escapeIdentifierIfNeeded(String s) {
        boolean needsEscaping = false;
        if (s.isEmpty()) {
            needsEscaping = true;
        } else if (!ListingLexer.isIdentifierStart(s.charAt(0))) {
                needsEscaping = true;
        } else {
            for (int i = 1; i < s.length(); ++i) {
                if (!ListingLexer.isIdentifierPart(s.charAt(i))) {
                    needsEscaping = true;
                    break;
                }
            }
        }
        if (needsEscaping) {
            sb.append('`').append(s).append('`');
        } else {
            sb.append(s);
        }

        return this;
    }

    @Override
    public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
            NumericOperandType type) {
        appendLocalVar(receiver).append(" := ").appendLocalVar(first).append(" ");
        switch (op) {
            case ADD:
                append("+");
                break;
            case AND:
                append("&");
                break;
            case COMPARE:
                append("compareTo");
                break;
            case DIVIDE:
                append("/");
                break;
            case MODULO:
                append("%");
                break;
            case MULTIPLY:
                append("*");
                break;
            case OR:
                append("|");
                break;
            case SHIFT_LEFT:
                append("<<");
                break;
            case SHIFT_RIGHT:
                append(">>");
                break;
            case SHIFT_RIGHT_UNSIGNED:
                append(">>>");
                break;
            case SUBTRACT:
                append("-");
                break;
            case XOR:
                append("^");
                break;
        }
        append(" ").appendLocalVar(second);
        append(" as ").append(type.name().toLowerCase());
    }

    @Override
    public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
        appendLocalVar(receiver).append(" := -").append(" ").appendLocalVar(operand);
    }

    @Override
    public void assign(VariableReader receiver, VariableReader assignee) {
        appendLocalVar(receiver).append(" := ").appendLocalVar(assignee);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        appendLocalVar(receiver).append(" := cast ").appendLocalVar(value).append(" to ")
                .escapeIdentifierIfNeeded(targetType.toString());
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
            NumericOperandType targetType) {
        appendLocalVar(receiver).append(" := cast ").appendLocalVar(value)
                .append(" from ").append(sourceType.toString().toLowerCase(Locale.ROOT)).append(" to ")
                .append(targetType.toString().toLowerCase(Locale.ROOT));
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
            CastIntegerDirection direction) {
        appendLocalVar(receiver).append(" := cast ").appendLocalVar(value);
        switch (direction) {
            case FROM_INTEGER:
                append(" from int to ").append(type.name().toLowerCase(Locale.ROOT));
                break;
            case TO_INTEGER:
                append(" from ").append(type.name().toLowerCase(Locale.ROOT)).append(" to int");
                break;
        }
    }

    @Override
    public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
            BasicBlockReader alternative) {
        append("if ").appendLocalVar(operand).append(" ");
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
                sb.append("!== null");
                break;
            case NULL:
                sb.append("=== null");
                break;
        }
        append(" then goto $").append(consequent.getIndex()).append(" else goto $").append(alternative.getIndex());
    }

    @Override
    public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
            BasicBlockReader consequent, BasicBlockReader alternative) {
        append("if ").appendLocalVar(first).append(" ");
        switch (cond) {
            case EQUAL:
                append("==");
                break;
            case REFERENCE_EQUAL:
                append("===");
                break;
            case NOT_EQUAL:
                append("!=");
                break;
            case REFERENCE_NOT_EQUAL:
                append("!==");
                break;
        }
        appendLocalVar(second).append(" then goto $").append(consequent.getIndex())
                .append(" else goto $").append(alternative.getIndex());
    }

    @Override
    public void jump(BasicBlockReader target) {
        append("goto $").append(target.getIndex());
    }

    @Override
    public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
            BasicBlockReader defaultTarget) {
        append("switch ").appendLocalVar(condition).append(" ");
        for (int i = 0; i < table.size(); ++i) {
            if (i > 0) {
                append(" ");
            }
            SwitchTableEntryReader entry = table.get(i);
            append("case ").append(entry.getCondition()).append(" goto $").append(entry.getTarget().getIndex());
        }
        sb.append(" else goto $").append(defaultTarget.getIndex());
    }

    @Override
    public void exit(VariableReader valueToReturn) {
        append("return");
        if (valueToReturn != null) {
            append(" ").appendLocalVar(valueToReturn);
        }
    }

    @Override
    public void raise(VariableReader exception) {
        append("throw ").appendLocalVar(exception);
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        appendLocalVar(receiver).append(" := newArray ").escapeIdentifierIfNeeded(itemType.toString())
                .append("[").appendLocalVar(size).append(']');
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, List<? extends VariableReader> dimensions) {
        appendLocalVar(receiver).append(" := newArray ").escapeIdentifierIfNeeded(itemType.toString());
        append("[");

        for (int i = 0; i < dimensions.size(); ++i) {
            if (i > 0) {
                append(", ");
            }
            appendLocalVar(dimensions.get(i));
        }
        append("]");
    }

    @Override
    public void create(VariableReader receiver, String type) {
        appendLocalVar(receiver).append(" := new ").escapeIdentifierIfNeeded(type);
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field, ValueType fieldType) {
        appendLocalVar(receiver).append(" := field ").escapeIdentifierIfNeeded(field.toString());
        if (instance != null) {
            append(" ").appendLocalVar(instance);
        }
        append(" as ").escapeIdentifierIfNeeded(fieldType.toString());
    }

    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
        append("field ").escapeIdentifierIfNeeded(field.toString());
        if (instance != null) {
            append(" ").appendLocalVar(instance);
        }
        append(" := ").appendLocalVar(value).append(" as ").escapeIdentifierIfNeeded(fieldType.toString());
    }

    @Override
    public void arrayLength(VariableReader receiver, VariableReader array) {
        appendLocalVar(receiver).append(" := lengthOf ").appendLocalVar(array);
    }

    @Override
    public void cloneArray(VariableReader receiver, VariableReader array) {
        appendLocalVar(receiver).append(" := clone ").appendLocalVar(array);
    }

    @Override
    public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
        appendLocalVar(receiver).append(" := data ").appendLocalVar(array).append(" as ")
                .append(elementType.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
            ArrayElementType type) {
        appendLocalVar(receiver).append(" := ").appendLocalVar(array).append("[").appendLocalVar(index).append("]")
                .append(" as " + type.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void putElement(VariableReader array, VariableReader index, VariableReader value, ArrayElementType type) {
        appendLocalVar(array).append("[").appendLocalVar(index).append("] := ").appendLocalVar(value)
                .append(" as " + type.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        if (receiver != null) {
            appendLocalVar(receiver).append(" := ");
        }
        if (instance == null) {
            append("invokeStatic ");
        } else {
            switch (type) {
                case SPECIAL:
                    append("invoke ");
                    break;
                case VIRTUAL:
                    append("invokeVirtual ");
                    break;
            }
        }

        escapeIdentifierIfNeeded(method.toString());
        if (instance != null) {
            append(' ').appendLocalVar(instance);
        }

        for (int i = 0; i < arguments.size(); ++i) {
            if (instance != null || i > 0) {
                append(",");
            }
            append(' ').appendLocalVar(arguments.get(i));
        }
    }

    @Override
    public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
            List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
            List<RuntimeConstant> bootstrapArguments) {
        if (receiver != null) {
            appendLocalVar(receiver).append(" := ");
        }
        if (instance != null) {
            appendLocalVar(instance).append(".");
        }
        append(method.getName()).append("(");
        append(arguments.stream().map(arg -> "@" + arg.getIndex()).collect(Collectors.joining(", ")));
        append(") ");
        append("[").append(convert(bootstrapMethod)).append('(');
        append(bootstrapArguments.stream().map(this::convert).collect(Collectors.joining(", ")));
        append(")");
    }

    private String convert(MethodHandle handle) {
        switch (handle.getKind()) {
            case INVOKE_VIRTUAL:
            case INVOKE_SPECIAL:
            case INVOKE_INTERFACE:
                return new MethodDescriptor(handle.getName(), handle.signature()).toString();
            case INVOKE_CONSTRUCTOR:
                return "new" + handle.getClassName() + "." + new MethodDescriptor(handle.getName(),
                        handle.signature()).toString();
            case INVOKE_STATIC:
                return handle.getClassName() + "." + new MethodDescriptor(handle.getName(),
                        handle.signature()).toString();
            case GET_FIELD:
                return "GET " + handle.getName();
            case GET_STATIC_FIELD:
                return "GET " + handle.getClassName() + "." + handle.getName();
            case PUT_FIELD:
                return "PUT " + handle.getName();
            case PUT_STATIC_FIELD:
                return "PUT " + handle.getClassName() + "." + handle.getName();
        }
        throw new IllegalArgumentException("Unexpected handle type: " + handle.getKind());
    }

    private String convert(RuntimeConstant cst) {
        switch (cst.getKind()) {
            case RuntimeConstant.INT:
                return String.valueOf(cst.getInt());
            case RuntimeConstant.LONG:
                return String.valueOf(cst.getLong());
            case RuntimeConstant.FLOAT:
                return String.valueOf(cst.getFloat());
            case RuntimeConstant.DOUBLE:
                return String.valueOf(cst.getDouble());
            case RuntimeConstant.STRING:
                return String.valueOf(cst.getString());
            case RuntimeConstant.TYPE:
                return String.valueOf(cst.getValueType());
            case RuntimeConstant.METHOD: {
                ValueType[] methodType = cst.getMethodType();
                return "(" + Arrays.stream(methodType, 0, methodType.length - 1).map(Object::toString)
                            .collect(Collectors.joining()) + ")" + methodType[methodType.length - 1];
            }
            case RuntimeConstant.METHOD_HANDLE:
                return convert(cst.getMethodHandle());
        }
        throw new IllegalArgumentException("Unexpected runtime constant type: " + cst.getKind());
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        appendLocalVar(receiver).append(" := ").appendLocalVar(value).append(" instanceOf ")
                .escapeIdentifierIfNeeded(type.toString());
    }

    @Override
    public void initClass(String className) {
        append("initClass ").append(className);
    }

    @Override
    public void nullCheck(VariableReader receiver, VariableReader value) {
        appendLocalVar(receiver).append(" := nullCheck ").appendLocalVar(value);
    }

    @Override
    public void monitorEnter(VariableReader objectRef) {
        append("monitorEnter ").appendLocalVar(objectRef);
    }

    @Override
    public void monitorExit(VariableReader objectRef) {
        append("monitorExit ").appendLocalVar(objectRef);
    }
}
