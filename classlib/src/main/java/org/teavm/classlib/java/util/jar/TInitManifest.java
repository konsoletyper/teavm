/*
 *  Copyright 2017 Alexey Andreev.
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

package org.teavm.classlib.java.util.jar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Map;

class TInitManifest {

    private byte[] buf;

    private int pos;

    TAttributes.Name name;

    String value;

    CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
    CharBuffer cBuf = CharBuffer.allocate(512);

    TInitManifest(byte[] buf, TAttributes main, TAttributes.Name ver) throws IOException {
        this.buf = buf;

        // check a version attribute
        if (!readHeader() || (ver != null && !name.equals(ver))) {
            throw new IOException();
        }

        main.put(name, value);
        while (readHeader()) {
            main.put(name, value);
        }
    }

    void initEntries(Map<String, TAttributes> entries, Map<String, TManifest.Chunk> chunks) throws IOException {

        int mark = pos;
        while (readHeader()) {
            if (!TAttributes.Name.NAME.equals(name)) {
                throw new IOException();
            }
            String entryNameValue = value;

            TAttributes entry = entries.get(entryNameValue);
            if (entry == null) {
                entry = new TAttributes(12);
            }

            while (readHeader()) {
                entry.put(name, value);
            }

            if (chunks != null) {
                if (chunks.get(entryNameValue) != null) {
                    // TODO A bug: there might be several verification chunks for
                    // the same name. I believe they should be used to update
                    // signature in order of appearance; there are two ways to fix
                    // this: either use a list of chunks, or decide on used
                    // signature algorithm in advance and reread the chunks while
                    // updating the signature; for now a defensive error is thrown
                    throw new IOException();
                }
                chunks.put(entryNameValue, new TManifest.Chunk(mark, pos));
                mark = pos;
            }

            entries.put(entryNameValue, entry);
        }
    }

    int getPos() {
        return pos;
    }

    /**
     * Number of subsequent line breaks.
     */
    int linebreak;

    /**
     * Read a single line from the manifest buffer.
     */
    private boolean readHeader() throws IOException {
        if (linebreak > 1) {
            // break a section on an empty line
            linebreak = 0;
            return false;
        }
        readName();
        linebreak = 0;
        readValue();
        // if the last line break is missed, the line
        // is ignored by the reference implementation
        return linebreak > 0;
    }

    private byte[] wrap(int mark, int pos) {
        byte[] buffer = new byte[pos - mark];
        System.arraycopy(buf, mark, buffer, 0, pos - mark);
        return buffer;
    }

    private void readName() throws IOException {
        int i = 0;
        int mark = pos;

        while (pos < buf.length) {
            byte b = buf[pos++];

            if (b == ':') {
                byte[] nameBuffer = wrap(mark, pos - 1);

                if (buf[pos++] != ' ') {
                    throw new IOException();
                }

                name = new TAttributes.Name(nameBuffer);
                return;
            }

            if (!((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || b == '_'
                    || b == '-' || (b >= '0' && b <= '9'))) {
                throw new IOException();
            }
        }
        if (i > 0) {
            throw new IOException();
        }
    }

    private void readValue() throws IOException {
        byte next;
        boolean lastCr = false;
        int mark = pos;
        int last = pos;

        decoder.reset();
        cBuf.clear();

        while (pos < buf.length) {
            next = buf[pos++];

            switch (next) {
            case 0:
                throw new IOException();
            case '\n':
                if (lastCr) {
                    lastCr = false;
                } else {
                    linebreak++;
                }
                continue;
            case '\r':
                lastCr = true;
                linebreak++;
                continue;
            case ' ':
                if (linebreak == 1) {
                    decode(mark, last, false);
                    mark = pos;
                    last = mark;
                    linebreak = 0;
                    continue;
                }
            }

            if (linebreak >= 1) {
                pos--;
                break;
            }
            last = pos;
        }

        decode(mark, last, true);
        while (CoderResult.OVERFLOW == decoder.flush(cBuf)) {
            enlargeBuffer();
        }
        value = new String(cBuf.array(), cBuf.arrayOffset(), cBuf.position());
    }

    private void decode(int mark, int pos, boolean endOfInput)
            throws IOException {
        ByteBuffer bBuf = ByteBuffer.wrap(buf, mark, pos - mark);
        while (CoderResult.OVERFLOW == decoder.decode(bBuf, cBuf, endOfInput)) {
            enlargeBuffer();
        }
    }

    private void enlargeBuffer() {
        CharBuffer newBuf = CharBuffer.allocate(cBuf.capacity() * 2);
        newBuf.put(cBuf.array(), cBuf.arrayOffset(), cBuf.position());
        cBuf = newBuf;
    }
}
