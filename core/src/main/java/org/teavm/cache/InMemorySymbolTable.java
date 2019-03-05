/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemorySymbolTable implements SymbolTable {
    private List<String> symbols = new ArrayList<>();
    private Map<String, Integer> indexes = new HashMap<>();

    @Override
    public String at(int index) {
        return symbols.get(index);
    }

    @Override
    public int lookup(String symbol) {
        Integer index = indexes.get(symbol);
        if (index == null) {
            index = symbols.size();
            symbols.add(symbol);
            indexes.put(symbol, index);
        }
        return index;
    }

    public void invalidate() {
        symbols.clear();
        indexes.clear();
    }
}
