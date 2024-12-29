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

import java.io.IOException;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.TCharsetDecoder;
import org.teavm.classlib.java.nio.charset.TCodingErrorAction;
import org.teavm.classlib.java.nio.charset.TUnsupportedCharsetException;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;

public class TInputStreamReader extends TReader {
    private TInputStream stream;
    private TCharsetDecoder decoder;
    private byte[] inData = new byte[8192];
    private TByteBuffer inBuffer = TByteBuffer.wrap(inData);
    private char[] outData = new char[1024];
    private TCharBuffer outBuffer = TCharBuffer.wrap(outData);
    private boolean streamEof;
    private boolean eof;

    public TInputStreamReader(TInputStream in, String charsetName) throws TUnsupportedEncodingException {
        this(in, getCharset(charsetName));
    }

    public TInputStreamReader(TInputStream in, TCharset charset) {
        this(in, charset.newDecoder()
                .onMalformedInput(TCodingErrorAction.REPLACE)
                .onUnmappableCharacter(TCodingErrorAction.REPLACE));
    }

    public TInputStreamReader(TInputStream in) {
        this(in, TUTF8Charset.INSTANCE);
    }

    public TInputStreamReader(TInputStream in, TCharsetDecoder decoder) {
        this.stream = in;
        this.decoder = decoder;
        outBuffer.position(outBuffer.limit());
        inBuffer.position(inBuffer.limit());
    }

    private static TCharset getCharset(String charsetName) throws TUnsupportedEncodingException  {
        try {
            return TCharset.forName(charsetName.toString());
        } catch (TUnsupportedCharsetException e) {
            throw new TUnsupportedEncodingException(charsetName);
        }
    }

    public String getEncoding() {
        return decoder.charset().name();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public int read() throws IOException {
        if (eof && !outBuffer.hasRemaining()) {
            return -1;
        }
        if (outBuffer.hasRemaining()) {
            return outBuffer.get();
        }
        return fillBuffer() ? outBuffer.get() : -1;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (!outBuffer.hasRemaining()) {
            if (eof) {
                return -1;
            }
            if (len == 0) {
                return 0;
            }
            ensureBufferHasData(true);
        }
        int bytesRead = 0;
        do {
            int sz = Math.min(len, outBuffer.remaining());
            outBuffer.get(cbuf, off + bytesRead, sz);
            len -= sz;
            bytesRead += sz;
        } while (len > 0 && ensureBufferHasData(false));

        return bytesRead;
    }

    private boolean ensureBufferHasData(boolean force) throws IOException {
        if (outBuffer.hasRemaining()) {
            return true;
        }
        return fillBuffer(force);
    }

    private boolean fillBuffer(boolean force) throws IOException {
        if (eof) {
            return false;
        }
        outBuffer.compact();
        var readSomething = false;
        var hasAvailable = true;
        while (true) {
            if (inBuffer.hasRemaining()) {
                var posBefore = outBuffer.position();
                var result = decoder.decode(inBuffer, outBuffer, streamEof);
                readSomething |= outBuffer.position() > posBefore;
                if (result.isOverflow()) {
                    break;
                } else if (!result.isUnderflow()) {
                    continue;
                }
            }
            if (stream.available() <= 0 && readSomething) {
                hasAvailable = false;
                break;
            }
            if (!fillReadBuffer()) {
                break;
            }
        }
        if (!inBuffer.hasRemaining() && streamEof && decoder.flush(outBuffer).isUnderflow()) {
            hasAvailable = false;
            eof = true;
        }
        outBuffer.flip();
        return hasAvailable;
    }

    private boolean fillReadBuffer() throws IOException {
        if (streamEof) {
            return false;
        }
        inBuffer.compact();
        while (inBuffer.hasRemaining()) {
            int bytesRead = stream.read(inBuffer.array(), inBuffer.position(), inBuffer.remaining());
            if (bytesRead == -1) {
                streamEof = true;
                break;
            } else if (bytesRead > 0) {
                inBuffer.position(inBuffer.position() + bytesRead);
                break;
            }
        }
        inBuffer.flip();
        return true;
    }

    @Override
    public boolean ready() throws IOException {
        return outBuffer.hasRemaining() || inBuffer.hasRemaining();
    }
}
