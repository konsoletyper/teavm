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
package org.teavm.compiler.bootstrap.extension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.teavm.extension.bridge.BridgeResource;
import org.teavm.extension.resource.Resource;

class HostResource implements BridgeResource {
    private final Resource resource;

    HostResource(Resource resource) {
        this.resource = resource;
    }

    static HostResource[] toArray(Iterator<? extends Resource> resources) {
        var result = new ArrayList<HostResource>();
        while (resources.hasNext()) {
            result.add(new HostResource(resources.next()));
        }
        return result.toArray(new HostResource[0]);
    }

    @Override
    public byte[] read() {
        try (var input = resource.open()) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
