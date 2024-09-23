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

public class DisassemblyHTMLWriter extends DisassemblyWriter {
    public DisassemblyHTMLWriter(PrintWriter out) {
        super(out);
    }

    @Override
    public DisassemblyWriter prologue() {
        return writeExact("<html><body><pre>");
    }

    @Override
    public DisassemblyWriter epilogue() {
        return writeExact("</pre></body></html>");
    }

    @Override
    public DisassemblyWriter startLink(String s) {
        writeExact("<a href=\"#").writeExact(s).writeExact("\">");
        return this;
    }

    @Override
    public DisassemblyWriter endLink() {
        writeExact("</a>");
        return this;
    }

    @Override
    public DisassemblyWriter startLinkTarget(String s) {
        writeExact("<a name=\"").writeExact(s).writeExact("\">");
        return this;
    }

    @Override
    public DisassemblyWriter endLinkTarget() {
        writeExact("</a>");
        return this;
    }

    @Override
    public DisassemblyWriter write(String s) {
        StringBuilder sb = null;
        var i = 0;
        for (; i < s.length(); ++i) {
            var c = s.charAt(i);
            if (c == '<') {
                sb = new StringBuilder();
                sb.append(s, 0, i);
                break;
            }
        }
        if (sb != null) {
            for (; i < s.length(); ++i) {
                var c = s.charAt(i);
                if (c == '<') {
                    sb.append("&lt;");
                } else {
                    sb.append(c);
                }
            }
            s = sb.toString();
        }
        writeExact(s);
        return this;
    }
}
