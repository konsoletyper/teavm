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

import java.util.Iterator;
import java.util.NoSuchElementException;

public class WasmInstructionList implements Iterable<WasmInstruction> {
    WasmInstruction first;
    WasmInstruction last;

    public WasmInstruction getFirst() {
        return first;
    }

    public WasmInstruction getLast() {
        return last;
    }

    public boolean isEmpty() {
        return first == null;
    }

    public void add(WasmInstruction instruction) {
        instruction.checkAddable();
        instruction.owner = this;
        if (last == null) {
            first = instruction;
            last = instruction;
        } else {
            last.next = instruction;
            instruction.previous = last;
            last = instruction;
        }
    }

    @Override
    public Iterator<WasmInstruction> iterator() {
        return new Iterator<>() {
            private WasmInstruction current = first;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public WasmInstruction next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                var result = current;
                current = current.next;
                return result;
            }
        };
    }
}
