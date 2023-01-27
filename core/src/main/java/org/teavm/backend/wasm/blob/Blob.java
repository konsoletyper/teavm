/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.blob;

import java.util.ArrayList;
import java.util.List;

public class Blob {
    private byte[] buffer = new byte[16];
    byte[] currentChunk = new byte[4096];
    List<byte[]> data = new ArrayList<>();
    int chunkIndex;
    int posInChunk;
    int ptr;
    private int size;

    public Blob() {
        data.add(currentChunk);
    }

    public Blob write(byte[] bytes) {
        return write(bytes, 0, bytes.length);
    }

    public Blob write(byte[] bytes, int offset, int limit) {
        if (offset == limit) {
            return this;
        }
        if (offset + 1 == limit) {
            write(bytes[offset]);
            return this;
        }
        while (offset < limit) {
            var remaining = Math.min(limit - offset, currentChunk.length - posInChunk);
            System.arraycopy(bytes, offset, currentChunk, posInChunk, remaining);
            posInChunk += remaining;
            offset += remaining;
            ptr += remaining;
            nextChunkIfNeeded();
        }
        size = Math.max(size, ptr);
        return this;
    }

    public Blob skip(int count) {
        while (count > 0) {
            var remaining = Math.min(count, currentChunk.length - posInChunk);
            posInChunk += remaining;
            ptr += remaining;
            count -= remaining;
            nextChunkIfNeeded();
        }
        size = Math.max(size, ptr);
        return this;
    }

    public Blob write(byte b) {
        currentChunk[posInChunk++] = b;
        ptr++;
        nextChunkIfNeeded();
        size = Math.max(size, ptr);
        return this;
    }

    public Blob writeInt(int value) {
        var buffer = this.buffer;
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >>> 8);
        buffer[2] = (byte) (value >>> 16);
        buffer[3] = (byte) (value >>> 24);
        return write(buffer, 0, 4);
    }

    public Blob writeShort(int value) {
        var buffer = this.buffer;
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >>> 8);
        return write(buffer, 0, 2);
    }

    public Blob writeByte(int value) {
        return write((byte) value);
    }

    public Blob writeLEB(int value) {
        var ptr = 0;
        var buffer = this.buffer;
        while ((value & 0x7F) != value) {
            buffer[ptr++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer[ptr++] = (byte) (value & 0x7F);
        return write(buffer, 0, ptr);
    }

    public Blob writeSLEB(int value) {
        var ptr = 0;
        var buffer = this.buffer;
        var sign = value >>> 31;
        while (true) {
            var digit = value & 0x7F;
            value >>= 7;
            var more = value != 0 && value != -1 || digit >> 6 != sign;
            if (more) {
                buffer[ptr++] = (byte) (digit | 0x80);
            } else {
                buffer[ptr++] = (byte) digit;
                break;
            }
        }
        return write(buffer, 0, ptr);
    }

    private void nextChunkIfNeeded() {
        if (posInChunk < currentChunk.length) {
            return;
        }
        posInChunk = 0;
        if (++chunkIndex >= data.size()) {
            currentChunk = new byte[currentChunk.length];
            data.add(currentChunk);
        } else {
            currentChunk = data.get(chunkIndex);
        }
    }

    public int chunkCount() {
        return data.size() + 1;
    }

    public BlobReader newReader(BinaryDataConsumer consumer) {
        return new BlobReader(this, consumer);
    }

    public BinaryDataConsumer writer() {
        return this::write;
    }

    public Marker marker() {
        return new Marker(this, chunkIndex, posInChunk, ptr);
    }

    public byte[] chunkAt(int index) {
        return index < data.size() ? data.get(index) : currentChunk;
    }

    public int ptr() {
        return ptr;
    }

    public int size() {
        return size;
    }

    public byte[] toArray() {
        var result = new byte[size];
        var ptr = 0;
        for (var chunk : data) {
            int bytesToCopy = Math.min(chunk.length, size - ptr);
            if (bytesToCopy > 0) {
                System.arraycopy(chunk, 0, result, ptr, bytesToCopy);
                ptr += bytesToCopy;
            }
        }
        return result;
    }
}
