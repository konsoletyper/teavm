/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.model.instruction;

import org.teavm.model.TextLocation;

public abstract class WasmInstruction {
    WasmInstructionList owner;
    WasmInstruction next;
    WasmInstruction previous;
    private TextLocation location;

    public WasmInstructionList getOwner() {
        return owner;
    }

    public WasmInstruction getNext() {
        return next;
    }

    public WasmInstruction getPrevious() {
        return previous;
    }

    public TextLocation getLocation() {
        return location;
    }

    public void setLocation(TextLocation location) {
        this.location = location;
    }

    public abstract void acceptVisitor(WasmInstructionVisitor visitor);

    public boolean delete() {
        if (owner == null) {
            return false;
        }
        if (next != null) {
            next.previous = previous;
        } else {
            owner.last = previous;
        }
        if (previous != null) {
            previous.next = next;
        } else {
            owner.first = next;
        }
        owner = null;
        next = null;
        previous = null;
        return true;
    }

    public void insertNext(WasmInstruction other) {
        checkInList();
        other.checkAddable();
        if (next != null) {
            next.previous = other;
        } else {
            owner.last = other;
        }
        other.next = next;
        other.previous = this;
        next = other;
        other.owner = owner;
    }

    public void insertNext(WasmInstructionList other) {
        checkInList();
        var insn = this;
        while (other.getFirst() != null) {
            var next = other.getFirst();
            next.delete();
            insn.insertNext(next);
            insn = next;
        }
    }

    public void insertPrevious(WasmInstruction other) {
        checkInList();
        other.checkAddable();
        if (previous != null) {
            previous.next = other;
        } else {
            owner.first = other;
        }
        other.previous = previous;
        other.next = this;
        previous = other;
        other.owner = owner;
    }

    private void checkInList() {
        if (owner == null) {
            throw new IllegalStateException("This instruction is not in a list");
        }
    }

    void checkAddable() {
        if (owner != null) {
            throw new IllegalArgumentException("This instruction is already in a list");
        }
    }

    public boolean isSuspend() {
        return false;
    }

    public boolean isTerminating() {
        return false;
    }
}
