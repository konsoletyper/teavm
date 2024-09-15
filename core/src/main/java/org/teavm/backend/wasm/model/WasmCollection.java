/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class WasmCollection<T extends WasmEntity> implements Iterable<T> {
    private List<T> items = new ArrayList<>();
    private List<T> readonlyItems = Collections.unmodifiableList(items);
    private boolean indexesInvalid;

    WasmCollection() {
    }

    public T get(int index) {
        return items.get(index);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void add(T entity) {
        if (entity.collection != null) {
            throw new IllegalArgumentException("Entity already belongs some collection");
        }
        if (!indexesInvalid) {
            entity.index = items.size();
        }
        entity.collection = this;
        items.add(entity);
    }

    public void removeIf(Predicate<T> predicate) {
        for (var item : items) {
            if (predicate.test(item)) {
                item.collection = null;
            }
        }
        if (items.removeIf(predicate)) {
            invalidateIndexes();
        }
    }

    public void clear() {
        for (var item : items) {
            item.collection = null;
        }
        items.clear();
    }

    void invalidateIndexes() {
        indexesInvalid = true;
    }

    public int indexOf(T entity) {
        if (entity.collection != this) {
            throw new IllegalArgumentException("Given entity does not belong to this module");
        }
        if (indexesInvalid) {
            indexesInvalid = false;
            var index = 0;
            for (var item : items) {
                if (item.isImported()) {
                    item.index = index++;
                }
            }
            for (var item : items) {
                if (!item.isImported()) {
                    item.index = index++;
                }
            }
        }
        return entity.index;
    }

    @Override
    public Iterator<T> iterator() {
        return readonlyItems.iterator();
    }

    public Stream<T> stream() {
        return readonlyItems.stream();
    }

    public void sort(Comparator<T> comparator) {
        items.sort(comparator);
        indexesInvalid = true;
    }
}
