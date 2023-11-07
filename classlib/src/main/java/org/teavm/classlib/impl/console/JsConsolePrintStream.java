/*
 *  Copyright 2023 Alexey Andreev.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class JsConsolePrintStream extends PrintStream {
    private ByteEncoder byteEncoder;
    private Runnable flushAction;

    public JsConsolePrintStream() {
        super(new ByteArrayOutputStream());
    }

    @Override
    public void println(String s) {
        print(s);
        print("\n");
    }

    @Override
    public void print(int i) {
        print(Integer.toString(i));
    }

    @Override
    public void print(char c) {
        print(Character.toString(c));
    }

    @Override
    public void print(long l) {
        print(Long.toString(l));
    }

    @Override
    public void print(double d) {
        super.print(d);
    }

    @Override
    public void print(Object s) {
        print(Objects.toString(s));
    }

    @Override
    public void print(char[] s) {
        print(new String(s));
    }

    @Override
    public void println() {
        print("\n");
    }

    @Override
    public void println(int i) {
        println(Integer.toString(i));
    }

    @Override
    public void println(char c) {
        println(Character.toString(c));
    }

    @Override
    public void println(long l) {
        println(Long.toString(l));
    }

    @Override
    public void println(float d) {
        println(Float.toString(d));
    }

    @Override
    public void println(double d) {
        println(Double.toString(d));
    }

    @Override
    public void println(boolean b) {
        println(Boolean.toString(b));
    }

    @Override
    public void println(Object s) {
        println(Objects.toString(s));
    }

    @Override
    public void write(int b) {
        ensureByteEncoder().write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ensureByteEncoder().write(b, off, len);
    }

    @Override
    public void flush() {
        if (flushAction != null) {
            flushAction = null;
            flushAction.run();
        }
    }

    private ByteEncoder ensureByteEncoder() {
        if (byteEncoder == null) {
            byteEncoder = new ByteEncoder();
        }
        return byteEncoder;
    }

    @Override
    public abstract void print(String s);

    private class ByteEncoder {
        private ByteBuffer buffer = ByteBuffer.wrap(new byte[32]);
        private char[] outChars = new char[32];
        private CharBuffer outBuffer = CharBuffer.wrap(outChars);
        private CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        void write(int b) {
            postponeFlush();
            if (!buffer.hasRemaining()) {
                output();
            }
            buffer.put((byte) b);
        }

        void write(byte[] b, int off, int len) {
            postponeFlush();
            while (len > 0) {
                var count = Math.min(len, buffer.remaining());
                buffer.put(b, off, count);
                len -= count;
                output();
            }
        }

        void postponeFlush() {
            if (flushAction == null) {
                flushAction = this::flush;
            }
        }

        private void flush() {
            if (buffer.position() > 0) {
                output(true);
            }
        }

        private void output() {
            output(false);
        }

        private void output(boolean endOfInput) {
            buffer.flip();
            while (true) {
                var result = decoder.decode(buffer, outBuffer, endOfInput);
                print(new String(outChars, 0, outBuffer.position()));
                outBuffer.rewind();
                if (result != CoderResult.OVERFLOW) {
                    break;
                }
            }
            buffer.compact();
        }
    }
}
