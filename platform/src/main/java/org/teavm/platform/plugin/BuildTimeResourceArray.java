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
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;

class BuildTimeResourceArray<T extends Resource> implements ResourceArray<T>, ResourceWriter {
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

    @Override
    public void write(SourceWriter writer) {
        writer.append('[').tokenBoundary();
        for (int i = 0; i < data.size(); ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            ResourceWriterHelper.write(writer, data.get(i));
        }
        writer.append(']').tokenBoundary();
    }
}
