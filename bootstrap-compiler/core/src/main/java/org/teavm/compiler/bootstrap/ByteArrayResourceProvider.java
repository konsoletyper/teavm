/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.compiler.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.teavm.parsing.resource.Resource;
import org.teavm.parsing.resource.ResourceProvider;

public class ByteArrayResourceProvider implements ResourceProvider {
    private Map<String, ByteArrayResource> resources = new HashMap<>();

    public void addResource(String name, byte[] data) {
        resources.put(name, new ByteArrayResource(data));
    }

    @Override
    public Resource getResource(String name) {
        return resources.get(name);
    }

    @Override
    public Iterator<Resource> getResources(String name) {
        var resource = resources.get(name);
        return resource != null
                ? Collections.<Resource>singletonList(resource).iterator()
                : Collections.emptyIterator();
    }

    @Override
    public void close() {
    }

    public static ByteArrayResourceProvider fromArchive(InputStream inputStream) throws IOException {
        var result = new ByteArrayResourceProvider();
        var reader = new ArchiveReader(inputStream);
        while (true) {
            var name = reader.readNext();
            if (name == null) {
                break;
            }
            result.addResource(name, reader.readData());
        }
        return result;
    }
}
