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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSBodyImport;

public class Main {
    private static final byte[] HOST_REFERENCE = "host 한글🙂\n".getBytes(StandardCharsets.UTF_8);
    private static int hostReferenceOffset;

    public static void main(String[] args) throws Exception {
        System.getProperties();
        System.setProperty("stdin.encoding", "UTF-8");
        String mode = args[0];
        switch (mode) {
            case "lines":
                byte[] bytes = "first\r\n한글🙂\n\ntail".getBytes(StandardCharsets.UTF_8);
                System.setIn(new ByteArrayInputStream(bytes) {
                    @Override
                    public int read(byte[] buffer, int offset, int length) {
                        return super.read(buffer, offset, Math.min(length, 1));
                    }
                });
                System.out.println(IO.readln() + "|" + IO.readln() + "|" + IO.readln() + "|"
                        + IO.readln() + "|" + IO.readln());
                break;
            case "bare-cr":
                System.setIn(new ByteArrayInputStream("first\r한글🙂\r\rtail".getBytes(StandardCharsets.UTF_8)));
                System.out.println(IO.readln() + "|" + IO.readln() + "|" + IO.readln() + "|"
                        + IO.readln() + "|" + IO.readln());
                break;
            case "charset":
                System.setProperty("stdin.encoding", "ISO-8859-1");
                System.setIn(new ByteArrayInputStream(new byte[] { (byte) 0xe9, '\n' }));
                System.out.println(IO.readln());
                break;
            case "missing-charset":
            case "unsupported-charset":
            case "invalid-charset":
                if (mode.equals("missing-charset")) {
                    System.clearProperty("stdin.encoding");
                } else {
                    System.setProperty("stdin.encoding", mode.equals("invalid-charset") ? "?" : "missing-charset");
                }
                System.setIn(new ByteArrayInputStream("é\n".getBytes(StandardCharsets.UTF_8)));
                System.out.println(IO.readln());
                break;
            case "malformed":
                System.setIn(new ByteArrayInputStream(new byte[] { (byte) 0xc3, 0x28, (byte) 0xff, '\n' }));
                System.out.println(IO.readln());
                break;
            case "prompt":
            case "null-prompt":
                PrintStream original = System.out;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                System.setOut(new PrintStream(output, false, StandardCharsets.UTF_8));
                String prompt = mode.equals("null-prompt") ? null : "name?";
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
                System.setOut(original);
                System.out.println(output.toString(StandardCharsets.UTF_8) + answer);
                break;
            case "io-error":
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
                    System.out.println(e.getCause().getClass().getName() + ":" + e.getCause().getMessage());
                }
                break;
            case "lazy-charset":
                System.setProperty("stdin.encoding", "ISO-8859-1");
                IO.print("");
                System.setProperty("stdin.encoding", "UTF-8");
                System.setIn(new ByteArrayInputStream("é\n".getBytes(StandardCharsets.UTF_8)));
                System.out.println(IO.readln());
                break;
            case "frozen-charset":
                System.setIn(new ByteArrayInputStream("é\né\n".getBytes(StandardCharsets.UTF_8)));
                String firstLine = IO.readln();
                System.setProperty("stdin.encoding", "ISO-8859-1");
                System.out.println(firstLine + "|" + IO.readln());
                break;
            case "set-in":
                System.setIn(new ByteArrayInputStream(new byte[] { 'f' }));
                System.out.print(System.in.read() == 'f' ? "first|" : "wrong|");
                System.setIn(new ByteArrayInputStream(new byte[] { 's' }));
                System.out.print(System.in.read() == 's' ? "second|" : "wrong|");
                System.setIn(null);
                System.out.println(System.in == null);
                break;
            case "host-input":
                System.setIn(new InputStream() {
                    @Override
                    public int read() {
                        return readHostByte();
                    }
                });
                System.out.println(IO.readln() + "|" + IO.readln());
                break;
            default:
                throw new AssertionError(mode);
        }
    }

    @JSBody(script = "return input.read();", imports = @JSBodyImport(alias = "input",
            fromModule = "/resources/org/teavm/classlib/java/lang/io-readln/host-input.js"))
    private static int readHostByte() {
        return hostReferenceOffset < HOST_REFERENCE.length ? HOST_REFERENCE[hostReferenceOffset++] & 255 : -1;
    }
}
