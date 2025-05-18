/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.parsing.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public interface ResourceProvider extends AutoCloseable {
    default Resource getResource(String name) {
        var iter = getResources(name);
        return iter.hasNext() ? iter.next() : null;
    }

    Iterator<Resource> getResources(String name);

    @Override
    void close();

    static ResourceProvider ofClassPath(List<File> entries) {
        var providers = new ArrayList<ResourceProvider>();
        for (var entry : entries) {
            if (entry.isFile()) {
                providers.add(new ZipFileResourceProvider(entry));
            } else if (entry.isDirectory()) {
                providers.add(new DirectoryResourceProvider(entry));
            }
        }
        if (providers.isEmpty()) {
            return EMPTY;
        } else if (providers.size() == 1) {
            return providers.get(0);
        } else {
            return new CompositeResourceProvider(providers.toArray(new ResourceProvider[0]));
        }
    }

    ResourceProvider EMPTY = new ResourceProvider() {
        @Override
        public Iterator<Resource> getResources(String name) {
            return Collections.emptyIterator();
        }

        @Override
        public void close() {
        }
    };
}
