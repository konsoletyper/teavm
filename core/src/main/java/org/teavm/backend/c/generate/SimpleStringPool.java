/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c.generate;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleStringPool implements StringPool {
    private ObjectIntMap<String> stringIndexes = new ObjectIntHashMap<>();
    private final List<String> strings = new ArrayList<>();
    private final List<String> readonlyStrings = Collections.unmodifiableList(strings);
    private final List<FreeIndex> freeIndexes = new ArrayList<>();
    private FreeIndex firstFreeIndex;
    private FreeIndex lastFreeIndex;
    private ObjectIntMap<String> lastStringIndexes = new ObjectIntHashMap<>();

    @Override
    public int getStringIndex(String string) {
        int index = stringIndexes.getOrDefault(string, -1);
        if (index < 0) {
            index = lastStringIndexes.getOrDefault(string, -1);
            if (index >= 0) {
                removeFreeIndex(index);
                strings.set(index, string);
            } else if (firstFreeIndex != null) {
                index = firstFreeIndex.value;
                freeIndexes.set(index, null);
                firstFreeIndex = firstFreeIndex.next;
                if (firstFreeIndex == null) {
                    lastFreeIndex = null;
                }
                strings.set(index, string);
            } else {
                index = strings.size();
                strings.add(string);
            }
            stringIndexes.put(string, index);
        }
        return index;
    }

    private void removeFreeIndex(int index) {
        FreeIndex freeIndex = freeIndexes.get(index);
        if (freeIndex == null) {
            return;
        }
        freeIndexes.set(index, null);

        if (freeIndex.previous != null) {
            freeIndex.previous.next = freeIndex.next;
        } else {
            firstFreeIndex = freeIndex.next;
        }
        if (freeIndex.next != null) {
            freeIndex.next.previous = freeIndex.previous;
        } else {
            lastFreeIndex = freeIndex.previous;
        }
    }

    @Override
    public List<String> getStrings() {
        return readonlyStrings;
    }

    public void reset() {
        for (int i = freeIndexes.size(); i < strings.size(); ++i) {
            FreeIndex freeIndex = new FreeIndex();
            freeIndexes.add(freeIndex);
            if (lastFreeIndex != null) {
                freeIndex.previous = lastFreeIndex;
                lastFreeIndex.next = freeIndex;
            }
            lastFreeIndex = freeIndex;
        }
        lastStringIndexes.clear();
        lastStringIndexes.putAll(stringIndexes);
        stringIndexes.clear();
        for (int i = 0; i < strings.size(); ++i) {
            strings.set(i, null);
        }
    }

    static class FreeIndex {
        int value;
        FreeIndex previous;
        FreeIndex next;
    }
}
