package org.teavm.model.instructions;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class InvokeInstruction extends Instruction {
    private InvocationType type;
    private MethodReference method;
    private Variable instance;
    private List<Variable> arguments = new ArrayList<>();
    private Variable receiver;

    public InvocationType getType() {
        return type;
    }

    public void setType(InvocationType type) {
        this.type = type;
    }

    public Variable getInstance() {
        return instance;
    }

    public void setInstance(Variable instance) {
        this.instance = instance;
    }

    public List<Variable> getArguments() {
        return arguments;
    }

    public MethodReference getMethod() {
        return method;
    }

    public void setMethod(MethodReference method) {
        this.method = method;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
