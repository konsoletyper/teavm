/*
 *  Copyright 2026 TeaVM contributors.
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
package org.teavm.classlib.java.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSBodyImport;

public final class IOReadlnScenarios {
    private static final byte[] HOST_REFERENCE = "host 한글🙂\n".getBytes(StandardCharsets.UTF_8);
    private static int hostReferenceOffset;

    private IOReadlnScenarios() {
    }

    static void initialize() {
        System.getProperties();
        System.setProperty("stdin.encoding", "UTF-8");
    }

    static String lines() {
        var bytes = "first\r\n한글🙂\n\ntail".getBytes(StandardCharsets.UTF_8);
        System.setIn(new ByteArrayInputStream(bytes) {
            @Override
            public int read(byte[] buffer, int offset, int length) {
                return super.read(buffer, offset, Math.min(length, 1));
            }
        });
        return IO.readln() + "|" + IO.readln() + "|" + IO.readln() + "|" + IO.readln() + "|" + IO.readln();
    }

    static String bareCarriageReturns() {
        System.setIn(new ByteArrayInputStream("first\r한글🙂\r\rtail".getBytes(StandardCharsets.UTF_8)));
        return IO.readln() + "|" + IO.readln() + "|" + IO.readln() + "|" + IO.readln() + "|" + IO.readln();
    }

    static String configuredCharset() {
        System.setProperty("stdin.encoding", "ISO-8859-1");
        System.setIn(new ByteArrayInputStream(new byte[] { (byte) 0xe9, '\n' }));
        return IO.readln();
    }

    static String missingCharset() {
        System.clearProperty("stdin.encoding");
        return readUtf8();
    }

    static String unsupportedCharset() {
        System.setProperty("stdin.encoding", "missing-charset");
        return readUtf8();
    }

    static String invalidCharset() {
        System.setProperty("stdin.encoding", "?");
        return readUtf8();
    }

    private static String readUtf8() {
        System.setIn(new ByteArrayInputStream("é\n".getBytes(StandardCharsets.UTF_8)));
        return IO.readln();
    }

    static String malformedInput() {
        System.setIn(new ByteArrayInputStream(new byte[] { (byte) 0xc3, 0x28, (byte) 0xff, '\n' }));
        return IO.readln();
    }

    static String prompt(String prompt) {
        PrintStream original = System.out;
        var output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, false, StandardCharsets.UTF_8));
        try {
            System.setIn(new ByteArrayInputStream("answer\n".getBytes(StandardCharsets.UTF_8)) {
                @Override
                public int read(byte[] buffer, int offset, int length) {
                    if (!output.toString(StandardCharsets.UTF_8).equals(String.valueOf(prompt))) {
                        throw new AssertionError("Prompt was not written before reading input");
                    }
                    return super.read(buffer, offset, length);
                }
            });
            String answer = IO.readln(prompt);
            return output.toString(StandardCharsets.UTF_8) + answer;
        } finally {
            System.setOut(original);
        }
    }

    static String inputFailure() {
        System.setIn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("input-failed");
            }
        });
        try {
            IO.readln();
            throw new AssertionError("Expected IOError");
        } catch (IOError e) {
            return e.getCause().getClass().getName() + ":" + e.getCause().getMessage();
        }
    }

    static String lazyCharset() {
        System.setProperty("stdin.encoding", "ISO-8859-1");
        IO.print("");
        System.setProperty("stdin.encoding", "UTF-8");
        return readUtf8();
    }

    static String frozenCharset() {
        System.setIn(new ByteArrayInputStream("é\né\n".getBytes(StandardCharsets.UTF_8)));
        String firstLine = IO.readln();
        System.setProperty("stdin.encoding", "ISO-8859-1");
        return firstLine + "|" + IO.readln();
    }

    static String replaceInput() throws IOException {
        System.setIn(new ByteArrayInputStream(new byte[] { 'f' }));
        String first = System.in.read() == 'f' ? "first|" : "wrong|";
        System.setIn(new ByteArrayInputStream(new byte[] { 's' }));
        String second = System.in.read() == 's' ? "second|" : "wrong|";
        System.setIn(null);
        return first + second + (System.in == null);
    }

    static String hostInput() {
        System.setIn(new InputStream() {
            @Override
            public int read() {
                return readHostByte();
            }
        });
        return IO.readln() + "|" + IO.readln();
    }

    @JSBody(script = "return input.read();", imports = @JSBodyImport(alias = "input", fromModule = "./host-input.js"))
    private static int readHostByte() {
        return hostReferenceOffset < HOST_REFERENCE.length ? HOST_REFERENCE[hostReferenceOffset++] & 255 : -1;
    }

    @JSBody(script = "return input.reads;", imports = @JSBodyImport(alias = "input", fromModule = "./host-input.js"))
    static int hostReads() {
        return 0;
    }

    public static void main(String[] args) throws Exception {
        initialize();
        String result = switch (args[0]) {
            case "lines" -> lines();
            case "bare-cr" -> bareCarriageReturns();
            case "charset" -> configuredCharset();
            case "missing-charset" -> missingCharset();
            case "unsupported-charset" -> unsupportedCharset();
            case "invalid-charset" -> invalidCharset();
            case "malformed" -> malformedInput();
            case "prompt" -> prompt("name?");
            case "null-prompt" -> prompt(null);
            case "io-error" -> inputFailure();
            case "lazy-charset" -> lazyCharset();
            case "frozen-charset" -> frozenCharset();
            case "set-in" -> replaceInput();
            case "host-input" -> hostInput();
            default -> throw new AssertionError(args[0]);
        };
        System.out.println(result);
    }
}
