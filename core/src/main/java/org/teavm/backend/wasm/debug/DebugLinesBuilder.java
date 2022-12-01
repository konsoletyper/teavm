/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import org.teavm.model.MethodReference;

public class DebugLinesBuilder extends DebugSectionBuilder implements DebugLines {
    private DebugFiles files;
    private DebugMethods methods;
    private int ptr;
    private int lastWrittenPtr;
    private String file;
    private int line = 1;
    private Deque<State> states = new ArrayDeque<>();

    public DebugLinesBuilder(DebugFiles files, DebugMethods methods) {
        super(DebugConstants.SECTION_LINES);
        this.files = files;
        this.methods = methods;
    }

    @Override
    public void advance(int ptr) {
        if (ptr < this.ptr) {
            throw new IllegalArgumentException();
        }
        this.ptr = ptr;
    }

    @Override
    public void location(String file, int line) {
        if (Objects.equals(file, this.file) && this.ptr != lastWrittenPtr && this.line != line) {
            if (this.ptr - lastWrittenPtr < 32 && Math.abs(line - this.line) <= 3) {
                blob.writeByte(DebugConstants.LOC_USER + (this.ptr - lastWrittenPtr) + 32 * (line - this.line + 3));
                this.line = line;
                lastWrittenPtr = ptr;
                return;
            }
        }
        if (!Objects.equals(file, this.file)) {
            flushPtr();
            this.line = 1;
            this.file = file;
            blob.writeByte(DebugConstants.LOC_FILE).writeLEB(file != null ? files.filePtr(file) : 0);
        }
        if (this.line != line) {
            flushPtr();
            blob.writeByte(DebugConstants.LOC_LINE).writeSLEB(line - this.line);
            this.line = line;
        }
    }

    @Override
    public void emptyLocation() {
        location(null, -1);
    }

    private void flushPtr() {
        if (ptr != lastWrittenPtr) {
            blob.writeLEB(DebugConstants.LOC_PTR);
            blob.writeLEB(ptr - lastWrittenPtr);
            lastWrittenPtr = ptr;
        }
    }

    @Override
    public void start(MethodReference methodReference) {
        flushPtr();
        blob.writeLEB(DebugConstants.LOC_START);
        blob.writeLEB(methods.methodPtr(methodReference));
        states.push(new State(file, line));
        file = null;
        line = 1;
    }

    @Override
    public void end() {
        flushPtr();
        blob.writeLEB(DebugConstants.LOC_END);
        if (!states.isEmpty()) {
            var state = states.pop();
            file = state.file;
            line = state.line;
        } else {
            file = null;
            line = 1;
        }
    }

    private static class State {
        String file;
        int line;

        State(String file, int line) {
            this.file = file;
            this.line = line;
        }
    }
}
