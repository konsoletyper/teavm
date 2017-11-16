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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.HashMap;
import java.util.Map;

public class TManifest implements Cloneable {
    static final int LINE_LENGTH_LIMIT = 72;
    private static final byte[] LINE_SEPARATOR = new byte[] { '\r', '\n' };
    private static final byte[] VALUE_SEPARATOR = new byte[] { ':', ' ' };
    private static final TAttributes.Name NAME_ATTRIBUTE = new TAttributes.Name("Name");
    private TAttributes mainAttributes = new TAttributes();
    private HashMap<String, TAttributes> entries = new HashMap<>();

    static class Chunk {
        int start;
        int end;

        Chunk(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private HashMap<String, Chunk> chunks;
    private TInitManifest im;
    private int mainEnd;

    public TManifest() {
        super();
    }

    public TManifest(InputStream is) throws IOException {
        super();
        read(is);
    }

    @SuppressWarnings("unchecked")
    public TManifest(TManifest man) {
        mainAttributes = (TAttributes) man.mainAttributes.clone();
        entries = (HashMap<String, TAttributes>) ((HashMap<String, TAttributes>) man.getEntries()).clone();
    }

    TManifest(InputStream is, boolean readChunks) throws IOException {
        if (readChunks) {
            chunks = new HashMap<>();
        }
        read(is);
    }

    public void clear() {
        im = null;
        entries.clear();
        mainAttributes.clear();
    }

    public TAttributes getAttributes(String name) {
        return getEntries().get(name);
    }

    public Map<String, TAttributes> getEntries() {
        return entries;
    }

    public TAttributes getMainAttributes() {
        return mainAttributes;
    }

    @Override
    public Object clone() {
        return new TManifest(this);
    }

    public void write(OutputStream os) throws IOException {
        write(this, os);
    }

    public void read(InputStream is) throws IOException {
        byte[] buf = readFully(is);

        if (buf.length == 0) {
            return;
        }

        // a workaround for HARMONY-5662
        // replace EOF and NUL with another new line
        // which does not trigger an error
        byte b = buf[buf.length - 1];
        if (0 == b || 26 == b) {
            buf[buf.length - 1] = '\n';
        }

        // Attributes.Name.MANIFEST_VERSION is not used for
        // the second parameter for RI compatibility
        im = new TInitManifest(buf, mainAttributes, null);
        mainEnd = im.getPos();
        // FIXME
        im.initEntries(entries, chunks);
        im = null;
    }

    private byte[] readFully(InputStream is) throws IOException {
        // Initial read
        byte[] buffer = new byte[4096];
        int count = is.read(buffer);
        int nextByte = is.read();

        // Did we get it all in one read?
        if (nextByte == -1) {
            byte[] dest = new byte[count];
            System.arraycopy(buffer, 0, dest, 0, count);
            return dest;
        }

        // Does it look like a manifest?
        if (!containsLine(buffer, count)) {
            throw new IOException("Manifest is too long");
        }

        // Requires additional reads
        ByteArrayOutputStream baos = new ByteArrayOutputStream(count * 2);
        baos.write(buffer, 0, count);
        baos.write(nextByte);
        while (true) {
            count = is.read(buffer);
            if (count == -1) {
                return baos.toByteArray();
            }
            baos.write(buffer, 0, count);
        }
    }

    private boolean containsLine(byte[] buffer, int length) {
        for (int i = 0; i < length; i++) {
            if (buffer[i] == 0x0A || buffer[i] == 0x0D) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mainAttributes.hashCode() ^ getEntries().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() != this.getClass()) {
            return false;
        }
        if (!mainAttributes.equals(((TManifest) o).mainAttributes)) {
            return false;
        }
        return getEntries().equals(((TManifest) o).getEntries());
    }

    Chunk getChunk(String name) {
        return chunks.get(name);
    }

    void removeChunks() {
        chunks = null;
    }

    int getMainAttributesEnd() {
        return mainEnd;
    }

    static void write(TManifest manifest, OutputStream out) throws IOException {
        CharsetEncoder encoder = Charset.defaultCharset().newEncoder();
        ByteBuffer buffer = ByteBuffer.allocate(512);

        String version = manifest.mainAttributes.getValue(TAttributes.Name.MANIFEST_VERSION);
        if (version != null) {
            writeEntry(out, TAttributes.Name.MANIFEST_VERSION, version, encoder, buffer);
            for (Object o : manifest.mainAttributes.keySet()) {
                TAttributes.Name name = (TAttributes.Name) o;
                if (!name.equals(TAttributes.Name.MANIFEST_VERSION)) {
                    writeEntry(out, name, manifest.mainAttributes.getValue(name), encoder, buffer);
                }
            }
        }
        out.write(LINE_SEPARATOR);
        for (String key : manifest.getEntries().keySet()) {
            writeEntry(out, NAME_ATTRIBUTE, key, encoder, buffer);
            TAttributes attrib = manifest.entries.get(key);
            for (Object o : attrib.keySet()) {
                TAttributes.Name name = (TAttributes.Name) o;
                writeEntry(out, name, attrib.getValue(name), encoder, buffer);
            }
            out.write(LINE_SEPARATOR);
        }
    }

    private static void writeEntry(OutputStream os, TAttributes.Name name,
            String value, CharsetEncoder encoder, ByteBuffer bBuf) throws IOException {
        byte[] out = name.getBytes();
        if (out.length > LINE_LENGTH_LIMIT) {
            throw new IOException();
        }

        os.write(out);
        os.write(VALUE_SEPARATOR);

        encoder.reset();
        bBuf.clear().limit(LINE_LENGTH_LIMIT - out.length - 2);

        CharBuffer cBuf = CharBuffer.wrap(value);
        CoderResult r;

        while (true) {
            r = encoder.encode(cBuf, bBuf, true);
            if (CoderResult.UNDERFLOW == r) {
                r = encoder.flush(bBuf);
            }
            os.write(bBuf.array(), bBuf.arrayOffset(), bBuf.position());
            os.write(LINE_SEPARATOR);
            if (CoderResult.UNDERFLOW == r) {
                break;
            }
            os.write(' ');
            bBuf.clear().limit(LINE_LENGTH_LIMIT - 1);
        }
    }
}
