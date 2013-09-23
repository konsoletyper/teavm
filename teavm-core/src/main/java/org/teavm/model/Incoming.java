package org.teavm.model;

/**
 *
 * @author Alexey Andreev
 */
public class Incoming {
    private Phi phi;
    private Variable value;
    private BasicBlock source;

    public Variable getValue() {
        return value;
    }

    public void setValue(Variable value) {
        this.value = value;
    }

    public BasicBlock getSource() {
        return source;
    }

    public void setSource(BasicBlock source) {
        this.source = source;
    }

    public Phi getPhi() {
        return phi;
    }

    void setPhi(Phi phi) {
        this.phi = phi;
    }
}
