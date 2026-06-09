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
package org.teavm.backend.c.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.teavm.interop.Address;

public final class CResources {
    private static Map<String, Resource> resources = new HashMap<>();

    private CResources() {
    }

    static {
        for (var resource : acquireResources()) {
            resources.put(resource.name, resource);
        }
    }

    private static native Resource[] acquireResources();

    private static native Address getBaseAddress();

    private static Resource create(String name, int start, int length) {
        return new Resource(name, getBaseAddress().add(start), length);
    }

    public static InputStream getResource(String name) {
        var resource = resources.get(name);
        if (resource == null) {
            return null;
        }
        return new ResourceInputStream(resource.start, resource.length);
    }

    public static class Resource {
        public final String name;
        public final Address start;
        public final int length;

        public Resource(String name, Address start, int length) {
            this.name = name;
            this.start = start;
            this.length = length;
        }
    }

    public static final class ResourceInputStream extends InputStream {
        private Address address;
        private int remaining;

        private ResourceInputStream(Address address, int remaining) {
            this.address = address;
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int b = address.getByte() & 0xFF;
            address = address.add(1);
            remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            var bytesToRead = Math.min(len, remaining);
            Address.moveMemoryBlock(address, Address.ofData(b).add(off), bytesToRead);
            address = address.add(bytesToRead);
            remaining -= bytesToRead;
            return bytesToRead;
        }
    }
}
