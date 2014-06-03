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

import java.util.ArrayList;
import java.util.List;
import org.teavm.platform.metadata.ResourceArray;

/**
 *
 * @author Alexey Andreev
 */
class BuildTimeResourceArray<T> implements ResourceArray<T> {
    private List<T> data = new ArrayList<>();

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public T get(int i) {
        return data.get(i);
    }

    @Override
    public void add(T elem) {
        data.add(elem);
    }
}
