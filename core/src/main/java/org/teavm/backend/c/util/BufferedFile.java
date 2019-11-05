/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.backend.c.util;

import java.io.IOException;
import java.io.RandomAccessFile;

class BufferedFile {
    private RandomAccessFile out;
    private byte[] buffer;
    private int pos;

    BufferedFile(RandomAccessFile out) {
        this.out = out;
        buffer = new byte[4096];
    }

    void flush() throws IOException {
        if (pos > 0) {
            out.write(buffer, 0, pos);
            pos = 0;
        }
    }

    void writeInt(int v) throws IOException {
        byte[] buffer = this.buffer;
        int localPos = pos;
        if (localPos + 4 >= buffer.length) {
            flush();
            localPos = pos;
        }

        buffer[localPos++] = (byte) (v >>> 24);
        buffer[localPos++] = (byte) ((v >>> 16) & 255);
        buffer[localPos++] = (byte) ((v >>> 8) & 255);
        buffer[localPos++] = (byte) (v & 255);

        pos = localPos;
    }

    void writeShort(int v) throws IOException {
        byte[] buffer = this.buffer;
        int localPos = pos;
        if (localPos + 2 >= buffer.length) {
            flush();
            localPos = pos;
        }

        buffer[localPos++] = (byte) ((v >>> 8) & 255);
        buffer[localPos++] = (byte) (v & 255);

        pos = localPos;
    }

    void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    void write(byte[] data, int start, int length) throws IOException {
        if (pos + length >= buffer.length) {
            flush();
        }
        if (length > buffer.length) {
            out.write(data, start, length);
        } else {
            System.arraycopy(data, start, buffer, pos, length);
            pos += length;
        }
    }

    void write(int b) throws IOException {
        if (pos + 1 >= buffer.length) {
            flush();
        }
        buffer[pos++] = (byte) b;
    }

    long getFilePointer() throws IOException {
        return out.getFilePointer() + pos;
    }

    void seek(long pointer) throws IOException {
        flush();
        out.seek(pointer);
    }
}
