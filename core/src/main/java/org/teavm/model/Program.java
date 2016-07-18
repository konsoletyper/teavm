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
package org.teavm.model;

import java.util.ArrayList;
import java.util.List;

public class Program implements ProgramReader {
    private List<BasicBlock> basicBlocks = new ArrayList<>();
    private List<Variable> variables = new ArrayList<>();
    private MethodHolder method;
    private boolean packed;
    private int lastUsedRegister;

    public BasicBlock createBasicBlock() {
        BasicBlock block = new BasicBlock(this, basicBlocks.size());
        basicBlocks.add(block);
        return block;
    }

    public Variable createVariable() {
        Variable variable = new Variable(this);
        variable.setIndex(variables.size());
        variables.add(variable);
        variable.setRegister(lastUsedRegister++);
        return variable;
    }

    public void deleteBasicBlock(int index) {
        BasicBlock basicBlock = basicBlocks.get(index);
        if (basicBlock == null) {
            return;
        }
        basicBlocks.set(index, null);
        basicBlock.setIndex(-1);
        basicBlock.clearProgram();
        packed = false;
    }

    @Override
    public int basicBlockCount() {
        return basicBlocks.size();
    }

    @Override
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
        packed = false;
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
                block.setIndex(sz);
                basicBlocks.set(sz++, block);
            }
        }
        while (basicBlocks.size() > sz) {
            basicBlocks.remove(basicBlocks.size() - 1);
        }
        sz = 0;
        for (int i = 0; i < variables.size(); ++i) {
            Variable var = variables.get(i);
            if (var != null) {
                var.setIndex(sz);
                variables.set(sz++, var);
            }
        }
        while (variables.size() > sz) {
            variables.remove(variables.size() - 1);
        }
        packed = true;
    }

    @Override
    public int variableCount() {
        return variables.size();
    }

    @Override
    public Variable variableAt(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index " + index + " is negative");
        }
        return variables.get(index);
    }

    @Override
    public MethodReference getMethodReference() {
        return method != null ? method.getReference() : null;
    }

    MethodHolder getMethod() {
        return method;
    }

    void setMethod(MethodHolder method) {
        this.method = method;
    }
}
