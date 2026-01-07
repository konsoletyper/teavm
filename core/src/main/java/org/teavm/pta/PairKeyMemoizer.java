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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

abstract class PairKeyMemoizer<A, B, V> {
    private Map<WeakPairKey<A, B>, Ref<V, A, B>> map = new HashMap<>();
    private ReferenceQueue<Object> queue = new ReferenceQueue<>();

    abstract V create(A a, B b);

    V get(A a, B b) {
        cleanup();
        var key = new WeakPairKey<>(a, b, queue);
        var ref = map.get(key);
        if (ref != null) {
            var result = ref.get();
            if (result != null) {
                return result;
            }
        }
        var result = create(a, b);
        map.put(key, new Ref<>(result, queue, key));
        return result;
    }

    private void cleanup() {
        while (true) {
            var ref = queue.poll();
            if (ref == null) {
                break;
            }
            if (ref instanceof Ref<?, ?, ?>) {
                var refImpl = (Ref<?, A, B>) ref;
                map.remove(refImpl.key);
            }
        }
    }

    private static class WeakPairKey<A, B> {
        final Ref<A, A, B> a;
        final Ref<B, A, B> b;
        final int hash;

        WeakPairKey(A a, B b, ReferenceQueue<Object> queue) {
            this.a = new Ref<>(a, queue, this);
            this.b = new Ref<>(b, queue, this);
            hash = System.identityHashCode(a) ^ System.identityHashCode(b);
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
            if (!(obj instanceof WeakPairKey<?, ?>)) {
                return false;
            }
            var that = (WeakPairKey<?, ?>) obj;
            return Objects.equals(a.get(), that.a.get()) && Objects.equals(b.get(), that.b.get());
        }
    }

    private static class Ref<T, A, B> extends WeakReference<T> {
        WeakPairKey<A, B> key;

        Ref(T referent, ReferenceQueue<? super T> q, WeakPairKey<A, B> key) {
            super(referent, q);
            this.key = key;
        }
    }
}
