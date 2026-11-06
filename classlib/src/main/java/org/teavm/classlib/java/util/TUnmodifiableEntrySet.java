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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.lang.TUnsupportedOperationException;

class TUnmodifiableEntrySet<K, V> extends TUnmodifiableSet<TMap.Entry<K, V>> {
    TUnmodifiableEntrySet(TSet<? extends TMap.Entry<? extends K, ? extends V>> underlyingSet) {
        super(cast(underlyingSet));
    }

    @SuppressWarnings("unchecked")
    private static <K, V> TSet<? extends TMap.Entry<K, V>> cast(
            TSet<? extends TMap.Entry<? extends K, ? extends V>> set) {
        return (TSet<? extends TMap.Entry<K, V>>) (TSet<?>) set;
    }

    static <K, V> TMap.Entry<K, V> wrap(TMap.Entry<? extends K, ? extends V> entry) {
        return new TAbstractMap.SimpleImmutableEntry<>(entry);
    }

    @Override
    public TIterator<TMap.Entry<K, V>> iterator() {
        var it = underlyingCollection.iterator();
        return new TIterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public TMap.Entry<K, V> next() {
                return wrap(it.next());
            }

            @Override
            public void remove() {
                throw new TUnsupportedOperationException();
            }
        };
    }
}
