package org.teavm.model;


/**
 *
 * @author Alexey Andreev
 */
public class Variable {
    private Program program;
    private int index;

    Variable(Program program) {
        this.program = program;
    }

    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    public Program getProgram() {
        return program;
    }

    void setProgram(Program program) {
        this.program = program;
    }
}
