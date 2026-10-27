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
package org.teavm.classlib.java.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class WriterTest {
    // Writer.append(CharSequence csq): "If csq is null, then the four characters "null" are appended to this
    // writer." append(CharSequence csq, int start, int end): "If csq is null, then characters will be appended as
    // if csq contained the four characters "null"." It throws IndexOutOfBoundsException "if start or end are
    // negative, start is greater than end, or end is greater than csq.length()". StringWriter, CharArrayWriter and
    // PrintWriter state the same.
    @Test
    public void appendNullCharSequence() throws IOException {
        for (var factory : writers()) {
            var writer = factory.get();
            writer.append(null);
            writer.flush();
            assertEquals("null", writer.toString(), writer.getClass().getName());
        }
    }

    @Test
    public void appendNullCharSequenceRange() throws IOException {
        for (int start = -1; start <= 5; ++start) {
            for (int end = -1; end <= 5; ++end) {
                String expected = start >= 0 && start <= end && end <= 4 ? "null".substring(start, end) : "IOOBE";
                for (var factory : writers()) {
                    var writer = factory.get();
                    String actual;
                    try {
                        writer.append(null, start, end);
                        writer.flush();
                        actual = writer.toString();
                    } catch (IndexOutOfBoundsException e) {
                        actual = "IOOBE";
                    }
                    assertEquals(expected, actual, writer.getClass().getName() + ", start=" + start
                            + ", end=" + end);
                }
            }
        }
    }

    private static List<Supplier<Writer>> writers() {
        return List.of(
                BaseWriter::new,
                StringWriter::new,
                CharArrayWriter::new,
                () -> new PrintWriter(new StringWriter()) {
                    @Override
                    public String toString() {
                        return out.toString();
                    }
                });
    }

    // Relies on Writer's own append methods.
    static class BaseWriter extends Writer {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public void write(char[] cbuf, int off, int len) {
            sb.append(cbuf, off, len);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
