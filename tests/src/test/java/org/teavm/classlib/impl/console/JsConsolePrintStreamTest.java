/*
 *  Copyright 2026 Ethan Resnick.
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
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
@SkipJVM // java.io.PrintStream on the JVM rejects the null stream that JsConsolePrintStream passes to it
public class JsConsolePrintStreamTest {
    @Test
    public void flushesSingleByte() {
        var out = new JSConsoleStringPrintStream();
        out.write('a');
        out.flush();
        assertEquals("a", out.toString());
    }

    @Test
    public void writesByteArrayLongerThanBuffer() {
        var text = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789";
        var bytes = ("#" + text + "#").getBytes(StandardCharsets.UTF_8);
        var out = new JSConsoleStringPrintStream();
        out.write(bytes, 1, bytes.length - 2);
        out.flush();
        assertEquals(text, out.toString());
    }

    @Test
    public void writesBytesAfterFlush() {
        var out = new JSConsoleStringPrintStream();
        out.write('f');
        out.write('o');
        out.write('o');
        out.flush();
        out.write("baré".getBytes(StandardCharsets.UTF_8), 0, 5);
        out.flush();
        assertEquals("foobaré", out.toString());
    }
}
