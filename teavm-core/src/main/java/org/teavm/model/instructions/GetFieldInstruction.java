package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class GetFieldInstruction extends Instruction {
    private Variable instance;
    private String className;
    private String field;
    private ValueType fieldType;
    private Variable receiver;

    public Variable getInstance() {
        return instance;
    }

    public void setInstance(Variable instance) {
        this.instance = instance;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public ValueType getFieldType() {
        return fieldType;
    }

    public void setFieldType(ValueType fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
