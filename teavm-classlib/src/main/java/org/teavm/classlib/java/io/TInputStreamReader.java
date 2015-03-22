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

import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.TCharsetDecoder;
import org.teavm.classlib.java.nio.charset.TCodingErrorAction;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;

/**
 *
 * @author Alexey Andreev
 */
public class TInputStreamReader extends TReader {
    private TInputStream stream;
    private TCharset charset;
    private TString charsetName;
    private byte[] inData = new byte[8192];
    private TByteBuffer inBuffer = TByteBuffer.wrap(inData);
    private char[] outData = new char[1024];
    private TCharBuffer outBuffer = TCharBuffer.wrap(outData);
    private boolean streamEof;
    private boolean eof;

    public TInputStreamReader(TInputStream in, TString charsetName) {
        this(in, TCharset.forName(charsetName.toString()));
        this.charsetName = charsetName;
    }

    public TInputStreamReader(TInputStream in) {
        this(in, new TUTF8Charset());
        charsetName = TString.wrap("UTF-8");
    }

    public TInputStreamReader(TInputStream in, TCharset charset) {
        this.stream = in;
        this.charset = charset;
        outBuffer.position(outBuffer.limit());
        inBuffer.position(inBuffer.limit());
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
        if (eof && !outBuffer.hasRemaining()) {
            return -1;
        }
        if (outBuffer.hasRemaining()) {
            return outBuffer.get();
        }
        return fillBuffer() ? outBuffer.get() : -1;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws TIOException {
        if (eof && !outBuffer.hasRemaining()) {
            return -1;
        }
        int bytesRead = 0;
        while (len > 0) {
            int sz = Math.min(len, outBuffer.remaining());
            outBuffer.get(cbuf, off + bytesRead, sz);
            len -= sz;
            bytesRead += sz;
            if (!outBuffer.hasRemaining() && !fillBuffer()) {
                break;
            }
        }
        return bytesRead;
    }

    private boolean fillBuffer() throws TIOException {
        if (eof) {
            return false;
        }
        outBuffer.compact();
        TCharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(TCodingErrorAction.REPLACE)
                .onUnmappableCharacter(TCodingErrorAction.IGNORE);
        while (true) {
            if (!inBuffer.hasRemaining() && !fillReadBuffer()) {
                eof = true;
                break;
            }
            if (decoder.decode(inBuffer, outBuffer, eof).isOverflow()) {
                break;
            }
        }
        outBuffer.flip();
        return true;
    }

    private boolean fillReadBuffer() throws TIOException {
        if (streamEof) {
            return false;
        }
        inBuffer.compact();
        while (inBuffer.hasRemaining()) {
            int bytesRead = stream.read(inBuffer.array(), inBuffer.position(), inBuffer.remaining());
            if (bytesRead == -1) {
                streamEof = true;
                break;
            } else {
                inBuffer.position(inBuffer.position() + bytesRead);
                if (bytesRead == 0) {
                    break;
                }
            }
        }
        inBuffer.flip();
        return true;
    }

    @Override
    public boolean ready() throws TIOException {
        return outBuffer.hasRemaining() || inBuffer.hasRemaining();
    }
}
