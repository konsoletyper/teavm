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

public class BlobReader {
    private Blob blob;
    private BinaryDataConsumer consumer;
    private int ptr;
    private int currentChunk;
    private int offsetInChunk;

    BlobReader(Blob blob, BinaryDataConsumer consumer) {
        this.blob = blob;
        this.consumer = consumer;
    }

    public int position() {
        return ptr;
    }

    public void readRemaining() {
        advance(blob.size());
    }

    public void advance(int to) {
        if (to < ptr || to > blob.size()) {
            throw new IllegalArgumentException();
        }
        if (to == ptr) {
            return;
        }

        var ptr = this.ptr;
        var currentChunk = this.currentChunk;
        var offsetInChunk = this.offsetInChunk;
        while (ptr < to) {
            var chunk = blob.chunkAt(currentChunk);
            var limit = Math.min(ptr + chunk.length - offsetInChunk, to);
            var bytesToWrite = limit - ptr;
            consumer.accept(chunk, offsetInChunk, offsetInChunk + bytesToWrite);
            offsetInChunk += bytesToWrite;
            ptr += bytesToWrite;
            if (offsetInChunk == chunk.length) {
                offsetInChunk = 0;
                currentChunk++;
            }
        }
        this.ptr = ptr;
        this.currentChunk = currentChunk;
        this.offsetInChunk = offsetInChunk;
    }
}
