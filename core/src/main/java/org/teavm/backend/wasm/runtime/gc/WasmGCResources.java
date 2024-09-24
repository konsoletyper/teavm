/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.runtime.gc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class WasmGCResources {
    private static Map<String, Resource> resources = new HashMap<>();

    private WasmGCResources() {
    }

    static {
        for (var resource : acquireResources()) {
            resources.put(resource.name, resource);
        }
    }

    private static native Resource[] acquireResources();

    private static Resource create(String name, int start, int length) {
        return new Resource(name, start, start + length);
    }

    public static InputStream getResource(String name) {
        var resource = resources.get(name);
        if (resource == null) {
            return null;
        }
        return new ResourceInputStream(resource.start, resource.end);
    }

    public static class Resource {
        public final String name;
        public final int start;
        public final int end;

        public Resource(String name, int start, int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
    }

    public static final class ResourceInputStream extends InputStream {
        private int address;
        private int end;

        private ResourceInputStream(int address, int end) {
            this.address = address;
            this.end = end;
        }

        @Override
        public int read() throws IOException {
            return address < end ? readSingleByte(address++) : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (address >= end) {
                return -1;
            }
            var bytesToRead = Math.min(len, end - address);
            readBytes(b, off, bytesToRead, address);
            address += bytesToRead;
            return bytesToRead;
        }
    }

    private static void readBytes(byte[] bytes, int off, int len, int fromAddress) {
        for (int i = 0; i < len; ++i) {
            bytes[i] = (byte) readSingleByte(fromAddress++);
        }
    }

    private static native int readSingleByte(int address);
}
