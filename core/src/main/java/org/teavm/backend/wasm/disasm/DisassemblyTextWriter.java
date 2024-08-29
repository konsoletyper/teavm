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
package org.teavm.backend.wasm.disasm;

import java.io.PrintWriter;

public class DisassemblyTextWriter extends DisassemblyWriter {
    public DisassemblyTextWriter(PrintWriter out) {
        super(out);
    }

    @Override
    public DisassemblyWriter startLink(String s) {
        return this;
    }

    @Override
    public DisassemblyWriter endLink() {
        return this;
    }

    @Override
    public DisassemblyWriter startLinkTarget(String s) {
        return this;
    }

    @Override
    public DisassemblyWriter endLinkTarget() {
        return this;
    }

    @Override
    public DisassemblyWriter prologue() {
        return this;
    }

    @Override
    public DisassemblyWriter epilogue() {
        return this;
    }
}
