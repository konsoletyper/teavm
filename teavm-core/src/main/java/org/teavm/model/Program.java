package org.teavm.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class Program {
    private List<BasicBlock> basicBlocks = new ArrayList<>();
    private List<Variable> variables = new ArrayList<>();
    private MethodHolder method;
    private boolean packed;

    public BasicBlock createBasicBlock() {
        BasicBlock block = new BasicBlock(this, basicBlocks.size());
        basicBlocks.add(block);
        return block;
    }

    public Variable createVariable() {
        Variable variable = new Variable(this);
        variable.setIndex(variables.size());
        variables.add(variable);
        return variable;
    }

    public void deleteBasicBlock(int index) {
        BasicBlock basicBlock = basicBlocks.get(index);
        if (basicBlock == null) {
            return;
        }
        basicBlocks.set(index, null);
        basicBlock.setIndex(-1);
        basicBlock.setProgram(null);
        packed = false;
    }

    public int basicBlockCount() {
        return basicBlocks.size();
    }

    public BasicBlock basicBlockAt(int index) {
        return basicBlocks.get(index);
    }

    public void deleteVariable(int index) {
        Variable variable = variables.get(index);
        if (variable == null) {
            return;
        }
        variables.set(index, null);
        variable.setIndex(-1);
        variable.setProgram(null);
    }

    public boolean isPacked() {
        return packed;
    }

    public void pack() {
        if (packed) {
            return;
        }
        int sz = 0;
        for (int i = 0; i < basicBlocks.size(); ++i) {
            BasicBlock block = basicBlocks.get(i);
            if (block != null) {
                basicBlocks.set(sz++, block);
            }
        }
        while (basicBlocks.size() > sz) {
            basicBlocks.remove(basicBlocks.size() - 1);
        }
    }

    public int variableCount() {
        return variables.size();
    }

    public Variable variableAt(int index) {
        return variables.get(index);
    }

    public MethodHolder getMethod() {
        return method;
    }

    void setMethod(MethodHolder method) {
        this.method = method;
    }
}
