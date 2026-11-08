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
package org.teavm.extension.spi.transformation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import org.teavm.extension.resource.Resource;

public interface ResourceTransformerEnvironment {
    Iterator<Resource> resources(String name);

    default Resource resource(String name) {
        var iter = resources(name);
        return iter.hasNext() ? iter.next() : null;
    }

    default byte[] readResource(String name) {
        var resource = resource(name);
        if (resource == null) {
            return null;
        }
        try (var input = resource.open()) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    ClassLoader classLoader();

    String property(String name);
}
