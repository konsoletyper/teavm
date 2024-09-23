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
package org.teavm.platform.plugin.wasmgc;

import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceMap;

public final class ResourceMapHelper {
    private ResourceMapHelper() {
    }

    private static native ResourceMapEntry entry(ResourceMap<?> map, int index);

    private static native int entryCount(ResourceMap<?> map);

    public static Resource get(ResourceMap<?> map, String key) {
        var count = entryCount(map);
        var initialIndex = Integer.remainderUnsigned(key.hashCode(), count);
        for (var i = initialIndex; i < count; ++i) {
            var entry = entry(map, i);
            if (entry == null) {
                return null;
            }
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        for (var i = 0; i < initialIndex; ++i) {
            var entry = entry(map, i);
            if (entry == null) {
                return null;
            }
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static boolean has(ResourceMap<?> map, String key) {
        return get(map, key) != null;
    }

    public static String[] keys(ResourceMap<?> map) {
        var count = 0;
        var entryCount = entryCount(map);
        for (var i = 0; i < entryCount; ++i) {
            var entry = entry(map, i);
            if (entry != null) {
                ++count;
            }
        }

        var result = new String[count];
        var index = 0;
        for (var i = 0; i < entryCount; ++i) {
            var entry = entry(map, i);
            if (entry != null) {
                result[index++] = entry.getKey();
            }
        }

        return result;
    }
}
