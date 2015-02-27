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

import org.teavm.classlib.impl.charset.ByteBuffer;
import org.teavm.classlib.impl.charset.CharBuffer;
import org.teavm.classlib.impl.charset.Charset;
import org.teavm.classlib.java.lang.TString;

public class TOutputStreamWriter extends TWriter {
    private TOutputStream out;
    private String encoding;
    private Charset charset;
    private byte[] bufferData = new byte[512];
    private ByteBuffer buffer = new ByteBuffer(bufferData);

    public TOutputStreamWriter(TOutputStream out) {
        this(out, "UTF-8");
    }

    public TOutputStreamWriter(TOutputStream out, final String enc) throws TUnsupportedEncodingException {
        super(out);
        if (enc == null) {
            throw new NullPointerException();
        }
        this.out = out;
        charset = Charset.get(enc);
        if (charset == null) {
            throw new TUnsupportedEncodingException(TString.wrap(enc));
        }
        encoding = enc;
    }

    @Override
    public void close() throws TIOException {
        if (charset != null) {
            flush();
            charset = null;
            out.flush();
            out.close();
        }
    }

    @Override
    public void flush() throws TIOException {
        checkStatus();
        if (buffer.position() > 0) {
            out.write(bufferData, 0, buffer.position());
            buffer.rewind(0);
        }
        out.flush();
    }

    private void checkStatus() throws TIOException {
        if (charset == null) {
            throw new TIOException(TString.wrap("Writer already closed"));
        }
    }

    public String getEncoding() {
        return encoding;
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
            CharBuffer input = new CharBuffer(buf, offset, offset + count);
            while (!input.end()) {
                if (buffer.available() < 6) {
                    out.write(bufferData, 0, buffer.position());
                    buffer.rewind(0);
                }
                charset.encode(input, buffer);
            }
        }
    }

    @Override
    public void write(int oneChar) throws TIOException {
        synchronized (lock) {
            checkStatus();
            CharBuffer input = new CharBuffer(new char[] { (char)oneChar }, 0, 1);
            while (!input.end()) {
                if (buffer.available() < 6) {
                    out.write(bufferData, 0, buffer.position());
                    buffer.rewind(0);
                }
                charset.encode(input, buffer);
            }
        }
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
