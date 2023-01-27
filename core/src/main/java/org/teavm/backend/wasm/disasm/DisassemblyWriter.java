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

public class DisassemblyWriter {
    private PrintWriter out;
    private boolean withAddress;
    private int indentLevel;
    private int address;
    private boolean hasAddress;
    private boolean lineStarted;

    public DisassemblyWriter(PrintWriter out, boolean withAddress) {
        this.out = out;
        this.withAddress = withAddress;
    }

    public DisassemblyWriter address(int address) {
        this.address = address;
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

    private void printAddress() {
        out.print("(; ");
        for (int i = 7; i >= 0; --i) {
            var digit = (address >>> (i * 4)) & 0xF;
            out.print(Character.forDigit(digit, 16));
        }
        out.print(" ;)  ");
    }

    public DisassemblyWriter write(String s) {
        startLine();
        out.print(s);
        return this;
    }
}
