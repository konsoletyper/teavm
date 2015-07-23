/*
 *  Copyright 2015 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.*;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;

public class TOutputStreamWriter extends TWriter {
    private TOutputStream out;
    private TCharsetEncoder encoder;
    private byte[] bufferData = new byte[512];
    private TByteBuffer buffer = TByteBuffer.wrap(bufferData);
    private boolean closed;

    public TOutputStreamWriter(TOutputStream out) {
        this(nullCheck(out), new TUTF8Charset());
    }

    public TOutputStreamWriter(TOutputStream out, final String enc) throws TUnsupportedEncodingException {
        this(nullCheck(out), getCharset(enc));
    }

    public TOutputStreamWriter(TOutputStream out, TCharset charset) {
        this(nullCheck(out), charset.newEncoder()
                .onMalformedInput(TCodingErrorAction.REPLACE)
                .onUnmappableCharacter(TCodingErrorAction.REPLACE));
    }

    public TOutputStreamWriter(TOutputStream out, TCharsetEncoder encoder) {
        this.out = nullCheck(out);
        this.encoder = encoder;
    }

    private static TOutputStream nullCheck(TOutputStream stream) {
        if (stream == null) {
            throw new NullPointerException();
        }
        return stream;
    }

    private static TCharset getCharset(String charsetName) throws TUnsupportedEncodingException  {
        if (charsetName == null) {
            throw new NullPointerException();
        }
        try {
            return TCharset.forName(charsetName);
        } catch (TUnsupportedCharsetException | TIllegalCharsetNameException e) {
            throw new TUnsupportedEncodingException(TString.wrap(charsetName));
        }
    }

    @Override
    public void close() throws TIOException {
        if (!closed) {
            flush();
            closed = true;
            out.flush();
            out.close();
        }
    }

    @Override
    public void flush() throws TIOException {
        checkStatus();
        if (buffer.position() > 0) {
            out.write(bufferData, 0, buffer.position());
            buffer.clear();
        }
        out.flush();
    }

    private void checkStatus() throws TIOException {
        if (closed) {
            throw new TIOException(TString.wrap("Writer already closed"));
        }
    }

    public String getEncoding() {
        return encoder.charset().name();
    }

    @Override
    public void write(char[] buf, int offset, int count) throws TIOException {
        synchronized (lock) {
            checkStatus();
            if (buf == null) {
                throw new NullPointerException();
            }
            if (offset < 0 || offset > buf.length - count || count < 0) {
                throw new IndexOutOfBoundsException();
            }
            TCharBuffer input = TCharBuffer.wrap(buf, offset, count);
            while (input.hasRemaining()) {
                if (encoder.encode(input, buffer, false).isOverflow()) {
                    out.write(bufferData, 0, buffer.position());
                    buffer.clear();
                }
            }
        }
    }

    @Override
    public void write(int oneChar) throws TIOException {
        char[] array = { (char) oneChar };
        write(array, 0, array.length);
    }

    @Override
    public void write(String str, int offset, int count) throws TIOException {
        if (str == null) {
            throw new NullPointerException();
        }
        if (count < 0) {
            throw new IndexOutOfBoundsException("Negative count: " + count);
        }
        char[] chars = new char[count];
        str.getChars(offset, offset + count, chars, 0);
        write(chars);
    }
}
