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
package org.teavm.backend.wasm.disasm;

import java.io.PrintWriter;
import org.teavm.backend.wasm.debug.info.LineInfo;
import org.teavm.backend.wasm.debug.info.LineInfoCommandVisitor;
import org.teavm.backend.wasm.debug.info.LineInfoEnterCommand;
import org.teavm.backend.wasm.debug.info.LineInfoExitCommand;
import org.teavm.backend.wasm.debug.info.LineInfoFileCommand;
import org.teavm.backend.wasm.debug.info.LineInfoLineCommand;
import org.teavm.backend.wasm.parser.AddressListener;

public abstract class DisassemblyWriter {
    private PrintWriter out;
    private boolean withAddress;
    private int indentLevel;
    private int addressWithinSection;
    private int address;
    private boolean hasAddress;
    private boolean lineStarted;
    private int addressOffset;
    private LineInfo debugLines;
    private int currentSequenceIndex;
    private int currentCommandIndex = -1;
    private int lineInfoIndent;

    public DisassemblyWriter(PrintWriter out) {
        this.out = out;
    }

    public void setWithAddress(boolean withAddress) {
        this.withAddress = withAddress;
    }

    public void setAddressOffset(int addressOffset) {
        this.addressOffset = addressOffset;
    }

    public void setDebugLines(LineInfo debugLines) {
        this.debugLines = debugLines;
    }

    public void startSection() {
        addressWithinSection = -1;
        currentSequenceIndex = 0;
    }

    public DisassemblyWriter address() {
        hasAddress = true;
        return this;
    }

    public DisassemblyWriter indent() {
        indentLevel++;
        return this;
    }

    public DisassemblyWriter outdent() {
        indentLevel--;
        return this;
    }

    public DisassemblyWriter eol() {
        out.println();
        lineStarted = false;
        return this;
    }

    private void startLine() {
        if (!lineStarted) {
            lineStarted = true;
            if (debugLines != null) {
                printDebugLine();
            }
            if (withAddress) {
                if (hasAddress) {
                    hasAddress = false;
                    printAddress();
                } else {
                    out.print("                ");
                }
            }
            for (int i = 0; i < indentLevel; ++i) {
                out.print("  ");
            }
        }
    }

    private void printDebugLine() {
        if (currentSequenceIndex >= debugLines.sequences().size()) {
            return;
        }
        var force = false;
        if (currentCommandIndex < 0) {
            if (addressWithinSection < debugLines.sequences().get(currentSequenceIndex).startAddress()) {
                return;
            }
            currentCommandIndex = 0;
            force = true;
        } else {
            if (addressWithinSection >= debugLines.sequences().get(currentSequenceIndex).endAddress()) {
                printSingleDebugAnnotation("<end debug line sequence>");
                ++currentSequenceIndex;
                currentCommandIndex = -1;
                lineInfoIndent = 0;
                return;
            }
        }

        var sequence = debugLines.sequences().get(currentSequenceIndex);
        if (currentCommandIndex >= sequence.commands().size()) {
            return;
        }
        var command = sequence.commands().get(currentCommandIndex);
        if (!force) {
            if (currentCommandIndex + 1 < sequence.commands().size()
                    && addressWithinSection >= sequence.commands().get(currentCommandIndex + 1).address()) {
                command = sequence.commands().get(++currentCommandIndex);
            } else {
                return;
            }
        }

        command.acceptVisitor(new LineInfoCommandVisitor() {
            @Override
            public void visit(LineInfoEnterCommand command) {
                printSingleDebugAnnotation(" at " + command.method().fullName());
                ++lineInfoIndent;
            }

            @Override
            public void visit(LineInfoExitCommand command) {
                --lineInfoIndent;
            }

            @Override
            public void visit(LineInfoFileCommand command) {
                if (command.file() == null) {
                    printSingleDebugAnnotation("at <unknown>:" + command.line());
                } else {
                    printSingleDebugAnnotation(" at " + command.file().name() + ":" + command.line());
                }
            }

            @Override
            public void visit(LineInfoLineCommand command) {
                printSingleDebugAnnotation(" at " + command.line());
            }
        });
    }

    private void printSingleDebugAnnotation(String text) {
        out.print("                ");
        for (int i = 0; i < indentLevel; ++i) {
            out.print("  ");
        }
        for (int i = 0; i < lineInfoIndent; ++i) {
            out.print("  ");
        }
        startAnnotation();
        out.print("(;");
        write(text);
        out.print(" ;)");
        endAnnotation();
        out.print("\n");
    }

    private void printAddress() {
        out.print("(; ");
        for (int i = 7; i >= 0; --i) {
            var digit = (address >>> (i * 4)) & 0xF;
            out.print(Character.forDigit(digit, 16));
        }
        out.print(" ;)  ");
    }

    public DisassemblyWriter write(String s) {
        return writeExact(s);
    }

    protected DisassemblyWriter writeExact(String s) {
        startLine();
        out.print(s);
        return this;
    }

    public abstract DisassemblyWriter startLink(String s);

    public abstract DisassemblyWriter endLink();

    public abstract DisassemblyWriter startLinkTarget(String s);

    public abstract DisassemblyWriter endLinkTarget();

    public abstract DisassemblyWriter prologue();

    public abstract DisassemblyWriter epilogue();

    protected void startAnnotation() {
    }

    protected void endAnnotation() {
    }

    public void flush() {
        out.flush();
    }

    public final AddressListener addressListener = new AddressListener() {
        @Override
        public void address(int address) {
            addressWithinSection = address;
            DisassemblyWriter.this.address = address + addressOffset;
        }
    };
}
