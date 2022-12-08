/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.nio.charset.StandardCharsets;
import org.teavm.backend.wasm.blob.Blob;

public class DwarfStrings {
    final Blob blob = new Blob();
    private ObjectIntMap<String> offsets = new ObjectIntHashMap<>();

    public int stringRef(String s) {
        int ptr = offsets.getOrDefault(s, -1);
        if (ptr < 0) {
            ptr = blob.size();
            offsets.put(s, ptr);
            var bytes = s.getBytes(StandardCharsets.UTF_8);
            blob.write(bytes);
            blob.writeByte(0);
        }
        return ptr;
    }
}
