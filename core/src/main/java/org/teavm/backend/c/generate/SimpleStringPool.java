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

    @Override
    public int getStringIndex(String string) {
        int index = stringIndexes.getOrDefault(string, -1);
        if (index < 0) {
            index = strings.size();
            strings.add(string);
            stringIndexes.put(string, index);
        }
        return index;
    }

    @Override
    public List<? extends String> getStrings() {
        return readonlyStrings;
    }

    public void reset() {
        strings.clear();
        stringIndexes.clear();
    }
}
