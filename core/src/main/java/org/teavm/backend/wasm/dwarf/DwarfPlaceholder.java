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
package org.teavm.backend.wasm.dwarf;

import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.blob.Marker;

public class DwarfPlaceholder {
    int ptr = -1;
    final int size;
    List<ForwardRef> forwardReferences;

    DwarfPlaceholder(int size) {
        this.size = size;
    }

    void addForwardRef(DwarfPlaceholderWriter writer, Marker marker) {
        if (forwardReferences == null) {
            forwardReferences = new ArrayList<>();
        }
        forwardReferences.add(new ForwardRef(writer, marker));
    }

    static class ForwardRef {
        final DwarfPlaceholderWriter writer;
        final Marker marker;

        ForwardRef(DwarfPlaceholderWriter writer, Marker marker) {
            this.writer = writer;
            this.marker = marker;
        }
    }
}
