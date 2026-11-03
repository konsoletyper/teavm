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
package org.teavm.compiler.bootstrap.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.teavm.compiler.bootstrap.ByteArrayResource;
import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.parsing.resource.Resource;
import org.teavm.parsing.resource.ResourceProvider;

/**
 * Takes resources from the class path of tests. Actual reading of directories and JAR files is performed
 * by the JS side, see {@code junit-compiler.js}.
 */
class NodeClasspathResourceProvider implements ResourceProvider {
    private Map<String, Resource> cache = new HashMap<>();

    @Override
    public Resource getResource(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        var data = readResource(name);
        var resource = data != null ? new ByteArrayResource(data.copyToJavaArray()) : null;
        cache.put(name, resource);
        return resource;
    }

    @Override
    public Iterator<Resource> getResources(String name) {
        var data = readResources(name);
        var result = new ArrayList<Resource>(data.length);
        for (var item : data) {
            result.add(new ByteArrayResource(item.copyToJavaArray()));
        }
        return result.iterator();
    }

    @Override
    public void close() {
    }

    @JSBody(params = "name", script = "return teavmBootstrapReadResource(name);")
    private static native Int8Array readResource(String name);

    @JSBody(params = "name", script = "return teavmBootstrapReadResources(name);")
    private static native Int8Array[] readResources(String name);
}
