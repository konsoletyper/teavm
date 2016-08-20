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

import java.util.*;
import org.teavm.model.instructions.InstructionReader;

public class BasicBlock implements BasicBlockReader {
    private Program program;
    private int index;
    private List<Phi> phis = new ArrayList<>();
    private List<Instruction> instructions = new ArrayList<>();
    private List<TryCatchBlock> tryCatchBlocks = new ArrayList<>();
    private Variable exceptionVariable;

    BasicBlock(Program program, int index) {
        this.program = program;
        this.index = index;
    }

    @Override
    public Program getProgram() {
        return program;
    }

    @Override
    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    void clearProgram() {
        this.program = null;
    }

    private List<Instruction> safeInstructions = new AbstractList<Instruction>() {
        @Override
        public Instruction get(int index) {
            return instructions.get(index);
        }

        @Override
        public int size() {
            return instructions.size();
        }

        @Override
        public void add(int index, Instruction e) {
            if (e.getBasicBlock() != null) {
                throw new IllegalArgumentException("This instruction is in some basic block");
            }
            e.setBasicBlock(BasicBlock.this);
            instructions.add(index, e);
        }

        @Override
        public Instruction set(int index, Instruction element) {
            if (element.getBasicBlock() != null) {
                throw new IllegalArgumentException("This instruction is in some basic block");
            }
            Instruction oldInsn = instructions.get(index);
            oldInsn.setBasicBlock(null);
            element.setBasicBlock(BasicBlock.this);
            return instructions.set(index, element);
        }

        @Override
        public Instruction remove(int index) {
            Instruction insn = instructions.remove(index);
            insn.setBasicBlock(null);
            return insn;
        }

        @Override
        public void clear() {
            for (Instruction insn : instructions) {
                insn.setBasicBlock(null);
            }
            instructions.clear();
        }
    };

    public List<Instruction> getInstructions() {
        return safeInstructions;
    }

    public Instruction getLastInstruction() {
        return !instructions.isEmpty() ? instructions.get(instructions.size() - 1) : null;
    }

    private List<Phi> safePhis = new AbstractList<Phi>() {
        @Override
        public Phi get(int index) {
            return phis.get(index);
        }

        @Override
        public int size() {
            return phis.size();
        }

        @Override
        public void add(int index, Phi e) {
            if (e.getBasicBlock() != null) {
                throw new IllegalArgumentException("This phi is already in some basic block");
            }
            e.setBasicBlock(BasicBlock.this);
            phis.add(index, e);
        }

        @Override
        public Phi set(int index, Phi element) {
            if (element.getBasicBlock() != null) {
                throw new IllegalArgumentException("This phi is already in some basic block");
            }
            Phi oldPhi = phis.get(index);
            oldPhi.setBasicBlock(null);
            element.setBasicBlock(BasicBlock.this);
            return phis.set(index, element);
        }

        @Override
        public Phi remove(int index) {
            Phi phi = phis.remove(index);
            phi.setBasicBlock(null);
            return phi;
        }

        @Override
        public void clear() {
            for (Phi phi : phis) {
                phi.setBasicBlock(null);
            }
            phis.clear();
        }
    };

    public List<Phi> getPhis() {
        return safePhis;
    }

    private List<Phi> immutablePhis = Collections.unmodifiableList(phis);

    @Override
    public List<? extends PhiReader> readPhis() {
        return immutablePhis;
    }

    @Override
    public int instructionCount() {
        return instructions.size();
    }

    @Override
    public void readInstruction(int index, InstructionReader reader) {
        Instruction insn = instructions.get(index);
        reader.location(insn.getLocation());
        insn.acceptVisitor(new InstructionReadVisitor(reader));
    }

    @Override
    public void readAllInstructions(InstructionReader reader) {
        InstructionReadVisitor visitor = new InstructionReadVisitor(reader);
        TextLocation location = null;
        for (Instruction insn : instructions) {
            if (!Objects.equals(location, insn.getLocation())) {
                location = insn.getLocation();
                reader.location(location);
            }
            insn.acceptVisitor(visitor);
        }
    }

    public void removeIncomingsFrom(BasicBlock predecessor) {
        for (Phi phi : getPhis()) {
            List<Incoming> incomings = phi.getIncomings();
            for (int i = 0; i < incomings.size(); ++i) {
                if (incomings.get(i).getSource() == predecessor) {
                    incomings.remove(i--);
                }
            }
        }
    }

    private List<TryCatchBlock> immutableTryCatchBlocks = Collections.unmodifiableList(tryCatchBlocks);

    @Override
    public List<TryCatchBlock> readTryCatchBlocks() {
        return immutableTryCatchBlocks;
    }

    private List<TryCatchBlock> safeTryCatchBlocks = new AbstractList<TryCatchBlock>() {
        @Override public TryCatchBlock get(int index) {
            return tryCatchBlocks.get(index);
        }
        @Override public int size() {
            return tryCatchBlocks.size();
        }
        @Override public void add(int index, TryCatchBlock element) {
            if (element.protectedBlock == BasicBlock.this) {
                throw new IllegalStateException("This try/catch block is already added to basic block");
            }
            element.protectedBlock = BasicBlock.this;
            tryCatchBlocks.add(index, element);
        }
        @Override public TryCatchBlock remove(int index) {
            TryCatchBlock tryCatch = tryCatchBlocks.remove(index);
            tryCatch.protectedBlock = null;
            return tryCatch;
        }
        @Override public TryCatchBlock set(int index, TryCatchBlock element) {
            TryCatchBlock oldTryCatch = tryCatchBlocks.get(index);
            if (oldTryCatch == element) {
                return oldTryCatch;
            }
            if (element.protectedBlock == BasicBlock.this) {
                throw new IllegalStateException("This try/catch block is already added to basic block");
            }
            oldTryCatch.protectedBlock = null;
            element.protectedBlock = BasicBlock.this;
            tryCatchBlocks.set(index, element);
            return oldTryCatch;
        }
        @Override public void clear() {
            for (TryCatchBlock tryCatch : tryCatchBlocks) {
                tryCatch.protectedBlock = null;
            }
            tryCatchBlocks.clear();
        }
    };

    public List<TryCatchBlock> getTryCatchBlocks() {
        return safeTryCatchBlocks;
    }

    @Override
    public Variable getExceptionVariable() {
        return exceptionVariable;
    }

    public void setExceptionVariable(Variable exceptionVariable) {
        this.exceptionVariable = exceptionVariable;
    }
}
