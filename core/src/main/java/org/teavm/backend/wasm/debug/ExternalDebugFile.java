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
package org.teavm.backend.wasm.debug;

import java.util.List;
import org.teavm.backend.wasm.model.WasmCustomSection;
import org.teavm.backend.wasm.render.WasmBinaryWriter;

public final class ExternalDebugFile {
    private ExternalDebugFile() {
    }

    public static byte[] write(List<WasmCustomSection> sections) {
        if (sections.isEmpty()) {
            return null;
        }
        var writer = new WasmBinaryWriter();
        writer.writeInt32(0x67626474);
        writer.writeInt32(1);
        for (var section : sections) {
            var data = section.getData();
            writer.writeAsciiString(section.getName());
            writer.writeLEB(data.length);
            writer.writeBytes(data);
        }
        return writer.getData();
    }
}
