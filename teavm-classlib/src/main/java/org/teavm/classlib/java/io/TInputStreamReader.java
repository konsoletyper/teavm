/*
 *  Copyright 2014 Alexey Andreev.
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
import org.teavm.classlib.impl.charset.UTF8Charset;
import org.teavm.classlib.java.lang.TString;

/**
 *
 * @author Alexey Andreev
 */
public class TInputStreamReader extends TReader {
    private TInputStream stream;
    private Charset charset;
    private TString charsetName;
    private byte[] inData = new byte[8192];
    private ByteBuffer inBuffer = new ByteBuffer(inData);
    private char[] outData = new char[1024];
    private CharBuffer outBuffer = new CharBuffer(outData);
    private boolean streamEof;
    private boolean eof;

    public TInputStreamReader(TInputStream in, TString charsetName) {
        this(in, Charset.get(charsetName.toString()));
        this.charsetName = charsetName;
    }

    public TInputStreamReader(TInputStream in) {
        this(in, new UTF8Charset());
        charsetName = TString.wrap("UTF-8");
    }

    private TInputStreamReader(TInputStream in, Charset charset) {
        this.stream = in;
        this.charset = charset;
        outBuffer.skip(outBuffer.available());
        inBuffer.skip(inBuffer.available());
    }

    public TString getEncoding() {
        return charsetName;
    }

    @Override
    public void close() throws TIOException {
        stream.close();
    }

    @Override
    public int read() throws TIOException {
        if (eof && outBuffer.end()) {
            return -1;
        }
        if (!outBuffer.end()) {
            return outBuffer.get();
        }
        return fillBuffer() ? outBuffer.get() : -1;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws TIOException {
        if (eof && outBuffer.end()) {
            return -1;
        }
        CharBuffer wrapBuffer = new CharBuffer(cbuf, off, off + len);
        while (!wrapBuffer.end()) {
            wrapBuffer.put(outBuffer);
            if (outBuffer.end() && !fillBuffer()) {
                break;
            }
        }
        return wrapBuffer.position() - off;
    }

    private boolean fillBuffer() throws TIOException {
        if (eof) {
            return false;
        }
        CharBuffer newBuffer = new CharBuffer(outData);
        newBuffer.put(outBuffer);
        while (true) {
            if (inBuffer.end() && !fillReadBuffer()) {
                eof = true;
                break;
            }
            int oldAvail = newBuffer.available();
            charset.decode(inBuffer, newBuffer);
            if (oldAvail == newBuffer.available()) {
                break;
            }
        }
        outBuffer = new CharBuffer(outData, 0, newBuffer.position());
        return true;
    }

    private boolean fillReadBuffer() throws TIOException {
        if (streamEof) {
            return false;
        }
        int off = 0;
        while (!inBuffer.end()) {
            inData[off] = inBuffer.get();
        }
        inBuffer.rewind(0);
        while (off < inData.length) {
            int bytesRead = stream.read(inData, off, inData.length - off);
            if (bytesRead == -1) {
                streamEof = true;
                inBuffer = new ByteBuffer(inData, 0, inBuffer.position());
                break;
            } else {
                off += bytesRead;
                if (bytesRead == 0) {
                    break;
                }
            }
        }
        inBuffer = new ByteBuffer(inData, 0, off);
        return true;
    }

    @Override
    public boolean ready() throws TIOException {
        return !outBuffer.end() || inBuffer.end();
    }
}
