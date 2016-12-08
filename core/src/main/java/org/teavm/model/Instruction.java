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

import java.util.Iterator;
import org.teavm.model.instructions.InstructionVisitor;

public abstract class Instruction {
    BasicBlock basicBlock;
    private TextLocation location;
    Instruction next;
    Instruction previous;

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public Program getProgram() {
        return basicBlock != null ? basicBlock.getProgram() : null;
    }

    public TextLocation getLocation() {
        return location;
    }

    public void setLocation(TextLocation location) {
        this.location = location;
    }

    public abstract void acceptVisitor(InstructionVisitor visitor);

    public Instruction getNext() {
        return next;
    }

    public Instruction getPrevious() {
        return previous;
    }

    public boolean delete() {
        if (basicBlock == null) {
            return false;
        }

        if (next != null) {
            next.previous = previous;
        } else {
            basicBlock.lastInstruction = previous;
        }

        if (previous != null) {
            previous.next = next;
        } else {
            basicBlock.firstInstruction = next;
        }

        basicBlock.cachedSize--;
        basicBlock = null;
        next = null;
        previous = null;

        return true;
    }

    public boolean replace(Instruction other) {
        checkInBasicBlock();
        other.checkAddable();

        if (next != null) {
            next.previous = other;
        } else {
            basicBlock.lastInstruction = other;
        }
        other.next = next;

        if (previous != null) {
            previous.next = other;
        } else {
            basicBlock.firstInstruction = other;
        }
        other.previous = previous;

        other.basicBlock = basicBlock;
        basicBlock = null;
        next = null;
        previous = null;

        return true;
    }

    public void insertNext(Instruction other) {
        checkInBasicBlock();
        other.checkAddable();

        if (next != null) {
            next.previous = other;
        } else {
            basicBlock.lastInstruction = other;
        }
        other.next = next;

        other.previous = this;
        next = other;

        basicBlock.cachedSize++;
        other.basicBlock = basicBlock;
    }

    public void insertNextAll(Iterable<Instruction> other) {
        Iterator<Instruction> iterator = other.iterator();
        Instruction last = this;
        while (iterator.hasNext()) {
            Instruction insn = iterator.next();
            last.insertNext(insn);
            last = insn;
        }
    }

    public void insertPrevious(Instruction other) {
        checkInBasicBlock();
        other.checkAddable();

        if (previous != null) {
            previous.next = other;
        } else {
            basicBlock.firstInstruction = other;
        }
        other.previous = previous;

        other.next = this;
        previous = other;

        basicBlock.cachedSize++;
        other.basicBlock = basicBlock;
    }

    public void insertPreviousAll(Iterable<Instruction> other) {
        for (Instruction instruction : other) {
            insertPrevious(instruction);
        }
    }

    private void checkInBasicBlock() {
        if (getBasicBlock() == null) {
            throw new IllegalArgumentException("This instruction is not in basic block");
        }
    }

    void checkAddable() {
        if (getBasicBlock() != null) {
            throw new IllegalArgumentException("This instruction is in some basic block");
        }
    }
}
