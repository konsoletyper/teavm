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
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

class HamtNode<T> {
    private static final int BITS_PER_LEVEL = 5;
    private static final int CHILD_LIMIT = 1 << BITS_PER_LEVEL;
    private static final int LEVELS = 7;
    private static HamtNode<?> empty = new HamtNode<>(0, null);
    private static Map<Object, WeakReference<HamtNode<?>>> singleCache = new WeakHashMap<>();

    private static Map<WeakKey, Ref> nodeCache = new WeakHashMap<>();
    private static ReferenceQueue<Object> nodeCleanupQueue = new ReferenceQueue<>();

    final int bitmask;
    final Object[] values;

    private HamtNode(int bitmask, Object[] values) {
        this.bitmask = bitmask;
        this.values = values;
    }

    private static <T> HamtNode<T> getNode(int bitmask, Object[] values) {
        cleanupCache();
        var key = new WeakKey(values, bitmask, nodeCleanupQueue);
        var resultRef = nodeCache.get(key);
        if (resultRef != null) {
            var result = resultRef.get();
            if (result != null) {
                //noinspection unchecked
                return (HamtNode<T>) result;
            }
        }
        var node = new HamtNode<T>(bitmask, values);
        nodeCache.put(key, new Ref(node, nodeCleanupQueue, key));
        return node;
    }

    private static void cleanupCache() {
        while (true) {
            var ref = nodeCleanupQueue.poll();
            if (ref == null) {
                break;
            }
            if (ref instanceof Ref) {
                var refImpl = (Ref) ref;
                nodeCache.remove(refImpl.key);
            }
        }
    }

    boolean contains(T value) {
        if (bitmask == 0) {
            return false;
        }
        var hash = Objects.hashCode(value);
        var node = this;
        for (var level = 0; level < LEVELS; ++level) {
            var shift = level * BITS_PER_LEVEL;
            var hashPart = (hash >>> shift) & (CHILD_LIMIT - 1);
            if ((node.bitmask >> hashPart & 1) == 0) {
                return false;
            }
            var hashPartMask = (1 << hashPart) - 1;
            var pos = Integer.bitCount(node.bitmask & hashPartMask);
            //noinspection unchecked
            node = (HamtNode<T>) node.values[pos];
        }
        if (node.values != null) {
            for (var elem : node.values) {
                if (Objects.equals(elem, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static <T> HamtNode<T> empty() {
        return (HamtNode<T>) empty;
    }

    static <T> HamtNode<T> ofSingle(T value) {
        var cachedRef = singleCache.get(value);
        if (cachedRef != null) {
            var cached = cachedRef.get();
            if (cached != null) {
                //noinspection unchecked
                return (HamtNode<T>) cached;
            }
        }

        var node = HamtNode.<T>getNode(0, new Object[] { value });
        var hash = Objects.hashCode(value);
        for (var level = LEVELS - 1; level >= 0; level--) {
            var shift = level * BITS_PER_LEVEL;
            var hashPart = (hash >>> shift) & (CHILD_LIMIT - 1);
            node = getNode(1 << hashPart, new Object[] { node });
        }

        singleCache.put(value, new WeakReference<>(node));

        return node;
    }

    private static class WeakKey {
        final Ref[] nodes;
        final int bitmask;
        final int hash;

        WeakKey(Object[] nodes, int bitmask, ReferenceQueue<Object> queue) {
            this.nodes = new Ref[nodes.length];
            var hash = bitmask;
            for (var i = 0; i < nodes.length; ++i) {
                this.nodes[i] = new Ref(nodes[i], queue, this);
                hash = hash * 31 + nodes[i].hashCode();
            }
            this.bitmask = bitmask;
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof WeakKey)) {
                return false;
            }
            var that = (WeakKey) obj;
            if (that.hash != hash || that.bitmask != bitmask) {
                return false;
            }
            if (nodes.length != that.nodes.length) {
                return false;
            }
            for (var i = 0; i < nodes.length; ++i) {
                if (nodes[i] != that.nodes[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class Ref extends WeakReference<Object> {
        WeakKey key;

        Ref(Object referent, ReferenceQueue<Object> q, WeakKey key) {
            super(referent, q);
            this.key = key;
        }
    }
}
