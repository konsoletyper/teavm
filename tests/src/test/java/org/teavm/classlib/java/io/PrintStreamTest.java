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
package org.teavm.classlib.java.io;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.testng.annotations.Test;

@RunWith(TeaVMTestRunner.class)
public class PrintStreamTest {
    @Test
    public void format() {
        var bytes = new ByteArrayOutputStream();
        var stream = new PrintStream(bytes, false, StandardCharsets.UTF_8);
        stream.format("n=%d; ", 23);
        stream.format("s=%s", null);
        stream.flush();
        assertEquals("n=23; s=null", bytes.toString(StandardCharsets.UTF_8));
    }

    @org.junit.Test
    public void append() {
        var bytes = new ByteArrayOutputStream();
        var stream = new PrintStream(bytes, false, StandardCharsets.UTF_8);

        stream.append('H');
        stream.append("el");
        stream.append("Hello", 3, 5);
        stream.flush();
        assertEquals("Hello", bytes.toString(StandardCharsets.UTF_8));
    }

    @org.junit.Test
    public void append_null() {
        var bytes = new ByteArrayOutputStream();
        var stream = new PrintStream(bytes, false, StandardCharsets.UTF_8);

        stream.append(null);
        stream.append(null, 1, 2);
        stream.flush();
        assertEquals("nullu", bytes.toString(StandardCharsets.UTF_8));
    }

}
