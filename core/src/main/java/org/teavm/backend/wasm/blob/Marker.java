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

public class Marker {
    private Blob blob;
    private int chunkIndex;
    private int posInChunk;
    private int ptr;

    Marker(Blob blob, int chunkIndex, int posInChunk, int ptr) {
        this.blob = blob;
        this.chunkIndex = chunkIndex;
        this.posInChunk = posInChunk;
        this.ptr = ptr;
    }

    public void rewind() {
        blob.chunkIndex = chunkIndex;
        blob.posInChunk = posInChunk;
        blob.ptr = ptr;
        blob.currentChunk = blob.data.get(chunkIndex);
    }

    public void update() {
        chunkIndex = blob.chunkIndex;
        posInChunk = blob.posInChunk;
        ptr = blob.ptr;
    }

    public int ptr() {
        return ptr;
    }
}
