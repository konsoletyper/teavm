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
package org.teavm.vm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MemoryBuildTarget implements BuildTarget {
    private Map<String, ByteArrayOutputStream> data = new LinkedHashMap<>();
    private Set<? extends String> names = Collections.unmodifiableSet(data.keySet());

    public Set<? extends String> getNames() {
        return names;
    }

    public byte[] getContent(String name) {
        ByteArrayOutputStream stream = data.get(name);
        return stream != null ? stream.toByteArray() : null;
    }

    public void clear() {
        data.clear();
    }

    @Override
    public OutputStream createResource(String fileName) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        data.put(fileName, stream);
        return stream;
    }

    public OutputStream appendToResource(String fileName) {
        return data.computeIfAbsent(fileName, k -> new ByteArrayOutputStream());
    }
}
