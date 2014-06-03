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
package org.teavm.platform.plugin;

import java.util.HashMap;
import java.util.Map;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
class BuildTimeResourceMap<T> implements ResourceMap<T> {
    private Map<String, T> data = new HashMap<>();

    @Override
    public boolean has(String key) {
        return data.containsKey(key);
    }

    @Override
    public T get(String key) {
        return data.get(key);
    }

    @Override
    public void put(String key, T value) {
        data.put(key, value);
    }
}
