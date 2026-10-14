/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.classlib.impl.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@SkipJVM
@OnlyPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC })
public class JsConsolePrintStreamTest {
    @Test
    public void writesAfterFlush() {
        var stream = new CapturingPrintStream();
        stream.write('a');
        stream.flush();
        stream.write(new byte[] { 'b', 'c' }, 0, 2);
        stream.flush();
        stream.flush();
        assertEquals("abc", stream.output.toString());
    }

    @Test
    public void keepsCharacterSplitAcrossFlush() {
        var bytes = "a\u20acb".getBytes(StandardCharsets.UTF_8);
        var stream = new CapturingPrintStream();
        stream.write(bytes, 0, 2);
        stream.flush();
        stream.write(bytes, 2, bytes.length - 2);
        stream.flush();
        assertEquals("a\u20acb", stream.output.toString());
    }

    @Test
    public void writesArrayLongerThanBuffer() {
        // 67 UTF-8 bytes: three 32-byte encoder chunks, with a multi-byte character across the first boundary
        var text = "0123456789abcdefghijklmnopqrstu\u20acvwxyz\u00e9ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        var bytes = ("<" + text + ">").getBytes(StandardCharsets.UTF_8);
        var stream = new CapturingPrintStream();
        stream.write(bytes, 1, bytes.length - 2);
        stream.flush();
        assertEquals(text, stream.output.toString());
    }

    private static class CapturingPrintStream extends JsConsolePrintStream {
        final StringBuilder output = new StringBuilder();

        @Override
        public void print(String s) {
            output.append(s);
        }
    }
}
