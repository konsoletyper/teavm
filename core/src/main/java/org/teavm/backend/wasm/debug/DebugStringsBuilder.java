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
package org.teavm.backend.wasm.debug;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.nio.charset.StandardCharsets;

public class DebugStringsBuilder extends DebugSectionBuilder implements DebugStrings {
    private ObjectIntMap<String> strings = new ObjectIntHashMap<>();

    public DebugStringsBuilder() {
        super(DebugConstants.SECTION_STRINGS);
    }

    @Override
    public int stringPtr(String str) {
        var result = strings.getOrDefault(str, -1);
        if (result < 0) {
            result = strings.size();
            strings.put(str, result);
            blob.writeLEB(str.length());
            blob.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return result;
    }
}
