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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class LocationSets {
    private final List<Location> locations;

    private final Map<LocationSet.Chunk, ChunkRef> chunkCache = new HashMap<>();
    private final ReferenceQueue<LocationSet.Chunk> chunkRefQueue = new ReferenceQueue<>();
    private final Map<LocationSetKey, SetRef> setCache = new HashMap<>();
    private final ReferenceQueue<LocationSet> setRefQueue = new ReferenceQueue<>();
    private LocationSet empty;

    LocationSets(List<Location> locations) {
        this.locations = locations;
    }

    public LocationSet ofSingle(Location location) {
        var id = location.id();
        var chunkIndex = id / LocationSet.BITS_IN_CHUNK;
        var chunks = new LocationSet.Chunk[chunkIndex + 1];
        var chunkData = new long[LocationSet.WORDS_IN_CHUNK];
        var indexInChunk = id % LocationSet.BITS_IN_CHUNK;
        var wordInChunk = indexInChunk / Long.SIZE;
        var indexInWord = indexInChunk % Long.SIZE;
        chunkData[wordInChunk] = 1L << indexInWord;
        chunks[chunkIndex] = getChunk(chunkData);
        return get(chunks);
    }

    public LocationSet ofCollection(Collection<? extends Location> locations) {
        var maxId = 0;
        for (var loc : locations) {
            maxId = Math.max(maxId, loc.id());
        }
        var chunkCount = (maxId / LocationSet.BITS_IN_CHUNK) + 1;

        var chunksProto = new long[chunkCount][];
        for (var loc : locations) {
            var id = loc.id();
            var chunkIndex = id / LocationSet.BITS_IN_CHUNK;
            var chunkData = chunksProto[chunkIndex];
            if (chunkData == null) {
                chunkData = new long[LocationSet.WORDS_IN_CHUNK];
                chunksProto[chunkIndex] = chunkData;
            }
            var indexInChunk = id % LocationSet.BITS_IN_CHUNK;
            var wordIndex = indexInChunk / Long.SIZE;
            var bitIndex = indexInChunk % Long.SIZE;
            chunkData[wordIndex] |= 1L << bitIndex;
        }

        var chunks = new LocationSet.Chunk[chunkCount];
        for (var i = 0; i < chunkCount; ++i) {
            var data = chunksProto[i];
            if (data != null) {
                chunks[i] = getChunk(data);
            }
        }

        return get(chunks);
    }

    public LocationSet empty() {
        if (empty == null) {
            empty = get(new LocationSet.Chunk[0]);
        }
        return empty;
    }

    public LocationSet union(LocationSet a, LocationSet b) {
        if (a == b) {
            return a;
        }
        var resultChunksSize = Math.max(a.chunks.length, b.chunks.length);
        var chunks = new LocationSet.Chunk[resultChunksSize];
        var min = Math.min(a.chunks.length, b.chunks.length);
        var firstChanged = false;
        var secondChanged = false;
        for (int i = 0; i < min; ++i) {
            var first = a.chunks[i];
            var second = b.chunks[i];
            var result = union(first, second);
            if (result != first) {
                firstChanged = true;
            }
            if (result != second) {
                secondChanged = true;
            }
            chunks[i] = result;
        }
        if (a.chunks.length == b.chunks.length) {
            if (!firstChanged) {
                return a;
            }
            if (!secondChanged) {
                return b;
            }
        } else if (a.chunks.length < b.chunks.length) {
            for (var i = min; i < chunks.length; ++i) {
                chunks[i] = b.chunks[i];
            }
        } else {
            for (var i = min; i < chunks.length; ++i) {
                chunks[i] = a.chunks[i];
            }
        }
        return get(chunks);
    }

    public LocationSet diff(LocationSet a, LocationSet b) {
        if (a == b || a.isEmpty()) {
            return empty();
        }
        if (b.isEmpty()) {
            return a;
        }
        var chunks = new LocationSet.Chunk[a.chunks.length];
        var min = Math.min(a.chunks.length, b.chunks.length);
        var changed = false;
        for (int i = 0; i < min; ++i) {
            var first = a.chunks[i];
            var second = b.chunks[i];
            var result = diff(first, second);
            if (result != first) {
                changed = true;
            }
            chunks[i] = result;
        }
        if (!changed) {
            return a;
        }
        if (a.chunks.length == b.chunks.length) {
            var newLength = chunks.length;
            while (newLength > 0 && chunks[newLength - 1] == null) {
                --newLength;
            }
            if (newLength == 0) {
                return empty();
            }
            chunks = Arrays.copyOf(chunks, newLength);
        }

        return get(chunks);
    }

    private LocationSet.Chunk union(LocationSet.Chunk a, LocationSet.Chunk b) {
        if (a == b || b == null) {
            return a;
        }
        if (a == null) {
            return b;
        }

        var newData = new long[LocationSet.WORDS_IN_CHUNK];
        var firstChanged = false;
        var secondChanged = false;
        for (int i = 0; i < LocationSet.WORDS_IN_CHUNK; ++i) {
            var first = a.data[i];
            var second = b.data[i];
            if (first == second) {
                newData[i] = first;
            } else {
                var result = first | second;
                if (first != result) {
                    firstChanged = true;
                }
                if (second != result) {
                    secondChanged = true;
                }
                newData[i] = result;
            }
        }
        if (!firstChanged) {
            return a;
        }
        if (!secondChanged) {
            return b;
        }
        return getChunk(newData);
    }

    private LocationSet.Chunk diff(LocationSet.Chunk a, LocationSet.Chunk b) {
        if (b == null) {
            return a;
        }
        if (a == b || a == null) {
            return null;
        }

        var newData = new long[LocationSet.WORDS_IN_CHUNK];
        var changed = false;
        var allZero = true;
        for (int i = 0; i < LocationSet.WORDS_IN_CHUNK; ++i) {
            var first = a.data[i];
            var second = b.data[i];
            var result = first & ~second;
            newData[i] = result;
            if (result != first) {
                changed = true;
            }
            newData[i] = result;
            if (result != 0) {
                allZero = false;
            }
        }
        if (!changed) {
            return a;
        }
        if (allZero) {
            return null;
        }
        return getChunk(newData);
    }

    public LocationSet filter(LocationSet set, Predicate<Location> predicate) {
        if (set.isEmpty()) {
            return set;
        }
        var newChunks = new LocationSet.Chunk[set.chunks.length];
        var changed = false;
        for (var i = 0; i < newChunks.length; ++i) {
            var chunk = set.chunks[i];
            if (chunk != null) {
                var chunkChanged = false;
                var newChunkData = new long[LocationSet.WORDS_IN_CHUNK];
                var hasNonZero = false;
                for (var j = 0; j < LocationSet.WORDS_IN_CHUNK; ++j) {
                    var word = chunk.data[j];
                    var newWord = 0L;
                    var k = 0;
                    if (word != 0) {
                        while (word != 0) {
                            var shift = Long.numberOfTrailingZeros(word);
                            k += shift;
                            var index = i * LocationSet.BITS_IN_CHUNK + j * Long.SIZE + k;
                            if (predicate.test(locations.get(index))) {
                                newWord |= 1L << k;
                            }
                            word >>>= (shift + 1);
                            ++k;
                        }
                        if (newWord != chunk.data[j]) {
                            chunkChanged = true;
                        }
                        newChunkData[j] = newWord;
                        if (newWord != 0) {
                            hasNonZero = true;
                        }
                    }
                }
                if (chunkChanged) {
                    changed = true;
                    if (hasNonZero) {
                        newChunks[i] = getChunk(newChunkData);
                    }
                } else {
                    newChunks[i] = chunk;
                }
            }
        }
        if (!changed) {
            return set;
        }
        var size = newChunks.length;
        while (size > 0 && newChunks[size - 1] == null) {
            --size;
        }
        if (size < newChunks.length) {
            newChunks = Arrays.copyOf(newChunks, size);
        }
        return get(newChunks);
    }

    private LocationSet get(LocationSet.Chunk[] chunks) {
        cleanupSetCache();
        var key = new LocationSetKey(chunks);
        var resultRef = setCache.get(key);
        if (resultRef != null) {
            var result = resultRef.get();
            if (result != null) {
                return result;
            }
        }
        var value = new LocationSet(locations, chunks);
        setCache.put(key, new SetRef(value, setRefQueue, key));
        return value;
    }

    private void cleanupSetCache() {
        while (true) {
            var setRef = setRefQueue.poll();
            if (setRef == null) {
                break;
            }
            setCache.remove(((SetRef) setRef).key);
        }
    }

    private LocationSet.Chunk getChunk(long[] data) {
        cleanupChunkCache();
        var key = new LocationSet.Chunk(data);
        var resultRef = chunkCache.get(key);
        if (resultRef != null) {
            var result = resultRef.get();
            if (result != null) {
                return result;
            }
        }
        var value = new LocationSet.Chunk(data);
        chunkCache.put(key, new ChunkRef(value, chunkRefQueue, key));
        return value;
    }

    private void cleanupChunkCache() {
        while (true) {
            var chunkRef = chunkRefQueue.poll();
            if (chunkRef == null) {
                break;
            }
            chunkCache.remove(((ChunkRef) chunkRef).key);
        }
    }

    private static class ChunkRef extends WeakReference<LocationSet.Chunk> {
        LocationSet.Chunk key;

        ChunkRef(LocationSet.Chunk referent, ReferenceQueue<? super LocationSet.Chunk> q, LocationSet.Chunk key) {
            super(referent, q);
            this.key = key;
        }
    }

    private static class SetRef extends WeakReference<LocationSet> {
        LocationSetKey key;

        SetRef(LocationSet referent, ReferenceQueue<? super LocationSet> q, LocationSetKey key) {
            super(referent, q);
            this.key = key;
        }
    }

    static class LocationSetKey {
        private final LocationSet.Chunk[] chunks;
        private int hash;

        public LocationSetKey(LocationSet.Chunk[] chunks) {
            this.chunks = chunks;
            var hash = 0;
            for (var chunk : chunks) {
                hash = hash * 32 + Objects.hashCode(chunk);
            }
            this.hash = hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof LocationSetKey)) {
                return false;
            }
            var that = (LocationSetKey) obj;
            if (chunks.length != that.chunks.length) {
                return false;
            }
            for (var i = 0; i < chunks.length; i++) {
                if (chunks[i] != that.chunks[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
