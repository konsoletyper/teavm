/*
 *  Copyright 2014 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TBoolean;

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
class TSetFromMap<E> extends TAbstractSet<E> {
    private TMap<E, TBoolean> map;

    TSetFromMap(TMap<E, TBoolean> map) {
        this.map = map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, TBoolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean removeAll(TCollection<?> c) {
        return map.keySet().removeAll(c);
    }

    @Override
    public boolean retainAll(TCollection<?> c) {
        return map.keySet().retainAll(c);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public TIterator<E> iterator() {
        return map.keySet().iterator();
    }

    static class SequencedSetFromMap<E> extends TSetFromMap<E> implements TSequencedSet<E> {
        SequencedSetFromMap(TSequencedMap<E, TBoolean> map) {
            super(map);
        }

        private TSequencedMap<E, TBoolean> map() {
            return (TSequencedMap<E, TBoolean>) super.map;
        }

        @Override
        public TSequencedSet<E> reversed() {
            return new SequencedSetFromMap<>(map().reversed());
        }

        @Override
        public void addFirst(E e) {
            map().putFirst(e, TBoolean.TRUE);
        }

        @Override
        public void addLast(E e) {
            map().putLast(e, TBoolean.TRUE);
        }

        @Override
        public E getFirst() {
            return TLinkedHashMap.checkNotNull(map().firstEntry()).getKey();
        }

        @Override
        public E getLast() {
            return TLinkedHashMap.checkNotNull(map().lastEntry()).getKey();
        }

        @Override
        public E removeFirst() {
            return TLinkedHashMap.checkNotNull(map().pollFirstEntry()).getKey();
        }

        @Override
        public E removeLast() {
            return TLinkedHashMap.checkNotNull(map().pollLastEntry()).getKey();
        }
    }
}
