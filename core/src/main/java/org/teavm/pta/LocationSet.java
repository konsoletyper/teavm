/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.pta;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class LocationSet extends AbstractCollection<Location> {
    static final int BITS_IN_CHUNK = 2048;
    static final int WORDS_IN_CHUNK = BITS_IN_CHUNK / Long.SIZE;

    private final List<Location> locations;
    final Chunk[] chunks;
    private int size = -1;

    LocationSet(List<Location> locations, Chunk[] chunks) {
        this.locations = locations;
        this.chunks = chunks;
    }

    public boolean contains(Location location) {
        var id = location.id();
        var chunkIndex = id / BITS_IN_CHUNK;
        if (chunkIndex >= chunks.length) {
            return false;
        }
        var chunk = chunks[chunkIndex];
        if (chunk == null) {
            return false;
        }
        var indexInChunk = id % BITS_IN_CHUNK;
        var wordIndex = indexInChunk / Long.SIZE;
        var bitIndex = indexInChunk % Long.SIZE;
        return ((chunk.data[wordIndex] >> bitIndex) & 1) == 1;
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Location && contains((Location) o);
    }

    @Override
    public int size() {
        if (size < 0) {
            var result = 0;
            for (var chunk : chunks) {
                if (chunk != null) {
                    result += chunk.size();
                }
            }
            size = result;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return chunks.length == 0;
    }

    @Override
    public Iterator<Location> iterator() {
        return new Iterator<>() {
            int chunkIndex = -1;
            LocationSet.Chunk chunk;
            int wordInChunk = -1;
            int bitInChunk;
            boolean valid;
            boolean done;

            @Override
            public boolean hasNext() {
                fill();
                return !done;
            }

            @Override
            public Location next() {
                fill();
                if (done) {
                    throw new NoSuchElementException();
                }
                var result = locations.get(chunkIndex * BITS_IN_CHUNK + wordInChunk * Long.SIZE + bitInChunk);
                valid = false;
                return result;
            }

            private void fill() {
                if (valid) {
                    return;
                }
                valid = true;
                if (chunk == null) {
                    nextChunk();
                    if (!done) {
                        nextWord();
                        firstBit();
                    }
                } else {
                    nextBit();
                    if (bitInChunk >= Long.SIZE) {
                        nextWord();
                        if (wordInChunk >= WORDS_IN_CHUNK) {
                            nextChunk();
                            if (done) {
                                return;
                            }
                            wordInChunk = 0;
                            nextWord();
                        }
                        firstBit();
                    }
                }
            }

            private void firstBit() {
                var word = chunk.data[wordInChunk];
                bitInChunk = Long.numberOfTrailingZeros(word);
            }

            private void nextBit() {
                var word = chunk.data[wordInChunk];
                bitInChunk += 1 + Long.numberOfTrailingZeros(word >>> bitInChunk);
            }

            private void nextWord() {
                while (++wordInChunk < WORDS_IN_CHUNK) {
                    if (chunk.data[wordInChunk] != 0) {
                        return;
                    }
                }
            }

            private void nextChunk() {
                while (++chunkIndex < chunks.length) {
                    chunk = chunks[chunkIndex];
                    if (chunk != null) {
                        return;
                    }
                }
                done = true;
            }
        };
    }

    static class Chunk {
        long[] data;
        int hash;
        int size = -1;

        Chunk(long[] data) {
            this.data = data;
            var hash = 0;
            for (var e : data) {
                hash = hash * 31 + Long.hashCode(e);
            }
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Chunk)) {
                return false;
            }
            var that = (Chunk) obj;
            for (var i = 0; i < data.length; ++i) {
                if (that.data[i] != data[i]) {
                    return false;
                }
            }
            return true;
        }

        int size() {
            if (size < 0) {
                var result = 0;
                for (var word : data) {
                    result += Long.bitCount(word);
                }
                this.size = result;
            }
            return size;
        }
    }
}
