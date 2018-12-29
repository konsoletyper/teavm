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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.util.TransitionExtractor;

public class BasicBlock implements BasicBlockReader, Iterable<Instruction> {
    private Program program;
    private int index;
    private List<Phi> phis;
    private List<TryCatchBlock> tryCatchBlocks;
    private Variable exceptionVariable;
    private String label;
    Instruction firstInstruction;
    Instruction lastInstruction;
    int cachedSize;

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

    public Instruction getFirstInstruction() {
        return firstInstruction;
    }

    public Instruction getLastInstruction() {
        return lastInstruction;
    }

    @Override
    public Iterator<Instruction> iterator() {
        return new Iterator<Instruction>() {
            Instruction instruction = firstInstruction;
            private boolean removed;

            @Override
            public boolean hasNext() {
                return instruction != null;
            }

            @Override
            public Instruction next() {
                if (instruction == null) {
                    throw new NoSuchElementException();
                }
                Instruction result = instruction;
                instruction = instruction.next;
                removed = false;
                return result;
            }

            @Override
            public void remove() {
                if (removed) {
                    throw new IllegalStateException();
                }
                if (instruction == null) {
                    throw new NoSuchElementException();
                }
                Instruction next = instruction.next;
                instruction.delete();
                instruction = next;
                removed = true;
            }
        };
    }

    public void addFirst(Instruction instruction) {
        instruction.checkAddable();
        if (firstInstruction == null) {
            firstInstruction = instruction;
            lastInstruction = instruction;
        } else {
            instruction.next = firstInstruction;
            firstInstruction.previous = instruction;
            firstInstruction = instruction;
        }

        cachedSize++;
        instruction.basicBlock = this;
    }

    public void addAll(Iterable<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            add(instruction);
        }
    }

    public void add(Instruction instruction) {
        instruction.checkAddable();
        if (firstInstruction == null) {
            firstInstruction = instruction;
            lastInstruction = instruction;
        } else {
            instruction.previous = lastInstruction;
            lastInstruction.next = instruction;
            lastInstruction = instruction;
        }

        cachedSize++;
        instruction.basicBlock = this;
    }

    public void addFirstAll(Iterable<Instruction> instructions) {
        Iterator<Instruction> iterator = instructions.iterator();
        if (!iterator.hasNext()) {
            return;
        }
        Instruction last = iterator.next();
        addFirst(last);
        while (iterator.hasNext()) {
            Instruction insn = iterator.next();
            last.insertNext(insn);
            last = insn;
        }
    }

    public void removeAllInstructions() {
        for (Instruction instruction = firstInstruction; instruction != null;) {
            Instruction next = instruction.next;
            instruction.previous = null;
            instruction.next = null;
            instruction.basicBlock = null;
            instruction = next;
        }
        firstInstruction = null;
        lastInstruction = null;

        cachedSize = 0;
    }

    private List<Phi> safePhis = new AbstractList<Phi>() {
        @Override
        public Phi get(int index) {
            if (phis == null) {
                throw new IndexOutOfBoundsException();
            }
            return phis.get(index);
        }

        @Override
        public int size() {
            return phis != null ? phis.size() : 0;
        }

        @Override
        public void add(int index, Phi e) {
            if (e.getBasicBlock() != null) {
                throw new IllegalArgumentException("This phi is already in some basic block");
            }
            e.setBasicBlock(BasicBlock.this);
            if (phis == null) {
                phis = new ArrayList<>(1);
            }
            phis.add(index, e);
        }

        @Override
        public Phi set(int index, Phi element) {
            if (element.getBasicBlock() != null) {
                throw new IllegalArgumentException("This phi is already in some basic block");
            }
            if (phis == null) {
                phis = new ArrayList<>(1);
            }
            Phi oldPhi = phis.get(index);
            oldPhi.setBasicBlock(null);
            element.setBasicBlock(BasicBlock.this);
            return phis.set(index, element);
        }

        @Override
        public Phi remove(int index) {
            if (phis == null) {
                throw new IndexOutOfBoundsException();
            }
            Phi phi = phis.remove(index);
            phi.setBasicBlock(null);
            return phi;
        }

        @Override
        public void clear() {
            if (phis == null) {
                return;
            }
            for (Phi phi : phis) {
                phi.setBasicBlock(null);
            }
            phis = null;
        }
    };

    public List<Phi> getPhis() {
        return safePhis;
    }

    private List<Phi> immutablePhis = new AbstractList<Phi>() {
        @Override
        public Phi get(int index) {
            if (phis == null) {
                throw new IndexOutOfBoundsException();
            }
            return phis.get(index);
        }

        @Override
        public int size() {
            return phis != null ? phis.size() : 0;
        }
    };

    @Override
    public List<? extends PhiReader> readPhis() {
        return immutablePhis;
    }

    @Override
    public int instructionCount() {
        return cachedSize;
    }

    @Override
    public InstructionIterator iterateInstructions() {
        return new InstructionIterator() {
            TextLocation location;
            Instruction instruction = firstInstruction;
            Instruction readInstruction;
            InstructionReadVisitor visitor = new InstructionReadVisitor(null);

            @Override
            public boolean hasNext() {
                return instruction != null;
            }

            @Override
            public void next() {
                readInstruction = instruction;
                instruction = instruction.next;
            }

            @Override
            public boolean hasPrevious() {
                return instruction != null && instruction.previous != null;
            }

            @Override
            public void previous() {
                instruction = instruction.previous;
                readInstruction = instruction;
            }

            @Override
            public void read(InstructionReader reader) {
                visitor.reader = reader;
                if (!Objects.equals(readInstruction.getLocation(), location)) {
                    location = readInstruction.getLocation();
                    reader.location(location);
                }
                readInstruction.acceptVisitor(visitor);
                visitor.reader = null;
            }
        };
    }

    @Override
    public void readAllInstructions(InstructionReader reader) {
        InstructionReadVisitor visitor = new InstructionReadVisitor(reader);
        TextLocation location = null;
        for (Instruction insn : this) {
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

    private List<TryCatchBlock> immutableTryCatchBlocks = new AbstractList<TryCatchBlock>() {
        @Override
        public TryCatchBlock get(int index) {
            if (tryCatchBlocks == null) {
                throw new IndexOutOfBoundsException();
            }
            return tryCatchBlocks.get(index);
        }

        @Override
        public int size() {
            return tryCatchBlocks != null ? tryCatchBlocks.size() : 0;
        }
    };

    @Override
    public List<TryCatchBlock> readTryCatchBlocks() {
        return immutableTryCatchBlocks;
    }

    private List<TryCatchBlock> safeTryCatchBlocks = new AbstractList<TryCatchBlock>() {
        @Override
        public TryCatchBlock get(int index) {
            if (tryCatchBlocks == null) {
                throw new IndexOutOfBoundsException();
            }
            return tryCatchBlocks.get(index);
        }

        @Override
        public int size() {
            return tryCatchBlocks != null ? tryCatchBlocks.size() : 0;
        }

        @Override
        public void add(int index, TryCatchBlock element) {
            if (element.protectedBlock == BasicBlock.this) {
                throw new IllegalStateException("This try/catch block is already added to basic block");
            }
            element.protectedBlock = BasicBlock.this;
            if (tryCatchBlocks == null) {
                tryCatchBlocks = new ArrayList<>(1);
            }
            tryCatchBlocks.add(index, element);
        }

        @Override
        public TryCatchBlock remove(int index) {
            if (tryCatchBlocks == null) {
                throw new IndexOutOfBoundsException();
            }
            TryCatchBlock tryCatch = tryCatchBlocks.remove(index);
            tryCatch.protectedBlock = null;
            return tryCatch;
        }

        @Override
        public TryCatchBlock set(int index, TryCatchBlock element) {
            TryCatchBlock oldTryCatch = tryCatchBlocks.get(index);
            if (oldTryCatch == element) {
                return oldTryCatch;
            }
            if (element.protectedBlock == BasicBlock.this) {
                throw new IllegalStateException("This try/catch block is already added to basic block");
            }
            oldTryCatch.protectedBlock = null;
            element.protectedBlock = BasicBlock.this;
            if (tryCatchBlocks == null) {
                tryCatchBlocks = new ArrayList<>(1);
            }
            tryCatchBlocks.set(index, element);
            return oldTryCatch;
        }

        @Override
        public void clear() {
            if (tryCatchBlocks == null) {
                return;
            }
            for (TryCatchBlock tryCatch : tryCatchBlocks) {
                tryCatch.protectedBlock = null;
            }
            tryCatchBlocks = null;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void detachSuccessors() {
        Instruction lastInstruction = getLastInstruction();
        if (lastInstruction == null) {
            return;
        }

        TransitionExtractor transitionExtractor = new TransitionExtractor();
        lastInstruction.acceptVisitor(transitionExtractor);
        if (transitionExtractor.getTargets() == null) {
            return;
        }

        for (BasicBlock successor : transitionExtractor.getTargets()) {
            List<Phi> phis = successor.getPhis();
            for (int i = 0; i < phis.size(); i++) {
                Phi phi = phis.get(i);
                for (int j = 0; j < phi.getIncomings().size(); ++j) {
                    if (phi.getIncomings().get(j).getSource() == this) {
                        phi.getIncomings().remove(j--);
                    }
                }
                if (phi.getIncomings().isEmpty()) {
                    phis.remove(i--);
                }
            }
        }
    }
}
