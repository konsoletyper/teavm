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
package org.teavm.backend.wasm.debug.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.teavm.backend.wasm.debug.DebugConstants;
import org.teavm.backend.wasm.debug.info.FileInfo;
import org.teavm.backend.wasm.debug.info.LineInfo;
import org.teavm.backend.wasm.debug.info.LineInfoCommand;
import org.teavm.backend.wasm.debug.info.LineInfoEnterCommand;
import org.teavm.backend.wasm.debug.info.LineInfoExitCommand;
import org.teavm.backend.wasm.debug.info.LineInfoFileCommand;
import org.teavm.backend.wasm.debug.info.LineInfoLineCommand;
import org.teavm.backend.wasm.debug.info.LineInfoSequence;
import org.teavm.backend.wasm.debug.info.MethodInfo;

public class DebugLinesParser extends DebugSectionParser {
    private DebugFileParser files;
    private DebugMethodParser methods;
    private LineInfo lineInfo;
    private List<LineInfoSequence> sequences = new ArrayList<>();
    private List<LineInfoCommand> commands = new ArrayList<>();
    private Deque<State> stateStack = new ArrayDeque<>();
    private FileInfo file;
    private int line = 1;
    private int address;
    private MethodInfo currentMethod;
    private int sequenceStartAddress;

    public DebugLinesParser(
            DebugFileParser files,
            DebugMethodParser methods
    ) {
        super(DebugConstants.SECTION_LINES, files, methods);
        this.files = files;
        this.methods = methods;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    protected void doParse() {
        while (ptr < data.length) {
            var cmd = data[ptr++] & 0xFF;
            switch (cmd) {
                case DebugConstants.LOC_START:
                    start();
                    break;
                case DebugConstants.LOC_END:
                    end();
                    break;
                case DebugConstants.LOC_LINE:
                    moveToLine();
                    break;
                case DebugConstants.LOC_FILE:
                    moveToFile();
                    break;
                case DebugConstants.LOC_PTR:
                    advanceAddress();
                    break;
                default:
                    if (cmd >= DebugConstants.LOC_USER) {
                        specialCommand(cmd);
                    } else {
                        throw new IllegalStateException("Invalid command at " + ptr + ": " + cmd);
                    }
                    break;
            }
        }
        lineInfo = new LineInfo(sequences.toArray(new LineInfoSequence[0]));
        sequences = null;
        commands = null;
        stateStack = null;
        currentMethod = null;
    }

    private void start() {
        var method = methods.getMethod(readLEB());
        if (currentMethod == null) {
            currentMethod = method;
            sequenceStartAddress = address;
        } else {
            stateStack.push(new State(file, line));
            commands.add(new LineInfoEnterCommand(address, method));
        }
        file = null;
        line = 1;
    }

    private void end() {
        if (stateStack.isEmpty()) {
            if (currentMethod != null) {
                sequences.add(new LineInfoSequence(sequenceStartAddress, address, currentMethod,
                        commands.toArray(new LineInfoCommand[0])));
            }
            commands.clear();
            currentMethod = null;
            file = null;
            line = 1;
        } else {
            var state = stateStack.pop();
            file = state.file;
            line = state.line;
            commands.add(new LineInfoExitCommand(address));
        }
    }

    private void moveToLine() {
        line = line + readSignedLEB();
        if (currentMethod != null) {
            commands.add(new LineInfoLineCommand(address, line));
        }
    }

    private void moveToFile() {
        file = files.getFile(readLEB());
        line = 1;
        if (ptr < data.length && data[ptr] == DebugConstants.LOC_LINE) {
            ++ptr;
            line = line + readSignedLEB();
        }
        if (currentMethod != null) {
            commands.add(new LineInfoFileCommand(address, file, line));
        }
    }

    private void advanceAddress() {
        address += readLEB();
    }

    private void specialCommand(int cmd) {
        cmd -= DebugConstants.LOC_USER;
        address += cmd % 32;
        cmd /= 32;
        line += cmd - 3;
        if (currentMethod != null) {
            commands.add(new LineInfoFileCommand(address, file, line));
        }
    }

    private static class State {
        FileInfo file;
        int line;

        State(FileInfo file, int line) {
            this.file = file;
            this.line = line;
        }
    }
}
