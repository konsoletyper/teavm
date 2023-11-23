/*
 *  Copyright 2023 ihromant.
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

class TLinkedHashMapIterator<K, V> {
    private final TLinkedHashMap<K, V> base;
    private final boolean reversed;
    private int expectedModCount;
    private TLinkedHashMap.LinkedHashMapEntry<K, V> futureEntry;
    TLinkedHashMap.LinkedHashMapEntry<K, V> currentEntry;

    TLinkedHashMapIterator(TLinkedHashMap<K, V> base, boolean reversed) {
        this.base = base;
        this.reversed = reversed;
        expectedModCount = base.modCount;
        futureEntry = reversed ? base.tail : base.head;
    }

    public boolean hasNext() {
        return futureEntry != null;
    }

    final void checkConcurrentMod() throws TConcurrentModificationException {
        if (expectedModCount != base.modCount) {
            throw new TConcurrentModificationException();
        }
    }

    final void makeNext() {
        checkConcurrentMod();
        if (!hasNext()) {
            throw new TNoSuchElementException();
        }
        currentEntry = futureEntry;
        futureEntry = reversed ? futureEntry.chainBackward : futureEntry.chainForward;
    }

    public void remove() {
        if (currentEntry == null) {
            throw new IllegalStateException();
        }
        checkConcurrentMod();
        base.removeLinkedEntry(currentEntry);
        currentEntry = null;
        expectedModCount++;
    }

    static class EntryIterator<K, V> extends TLinkedHashMapIterator<K, V> implements TIterator<TMap.Entry<K, V>> {
        EntryIterator(TLinkedHashMap<K, V> map, boolean reversed) {
            super(map, reversed);
        }

        @Override
        public TMap.Entry<K, V> next() {
            makeNext();
            return currentEntry;
        }
    }

    static class KeyIterator<K, V> extends TLinkedHashMapIterator<K, V> implements TIterator<K> {
        KeyIterator(TLinkedHashMap<K, V> map, boolean reversed) {
            super(map, reversed);
        }

        @Override
        public K next() {
            makeNext();
            return currentEntry.key;
        }
    }

    static class ValueIterator<K, V> extends TLinkedHashMapIterator<K, V> implements TIterator<V> {
        ValueIterator(TLinkedHashMap<K, V> map, boolean reversed) {
            super(map, reversed);
        }

        @Override
        public V next() {
            makeNext();
            return currentEntry.value;
        }
    }
}
