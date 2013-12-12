package org.teavm.model;

import java.util.*;

/**
 *
 * @author Alexey Andreev
 */
public class BasicBlock  {
    private Program program;
    private int index;
    private List<Phi> phis = new ArrayList<>();
    private List<Instruction> instructions = new ArrayList<>();

    BasicBlock(Program program, int index) {
        this.program = program;
        this.index = index;
    }

    public Program getProgram() {
        return program;
    }

    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    void setProgram(Program program) {
        this.program = program;
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
}
