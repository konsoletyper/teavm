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
package org.teavm.tooling.deobfuscate.wasmgc;

import java.util.Arrays;
import org.teavm.backend.wasm.debug.ExternalDebugFile;
import org.teavm.backend.wasm.debug.parser.LinesDeobfuscationParser;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

public final class DeobfuscatorFactory {
    private DeobfuscatorFactory() {
    }

    @JSExport
    public static Deobfuscator createForModule(JSObject module) {
        var parser = new LinesDeobfuscationParser();
        parser.pullSections(name -> {
            var result = getSection(module, name);
            if (result == null || result.getLength() != 1) {
                return null;
            }
            return new Int8Array(result.get(0)).copyToJavaArray();
        });
        var lineInfo = parser.getLineInfo();
        return lineInfo != null ? new Deobfuscator(parser.getLineInfo()) : null;
    }

    @JSBody(params = { "module", "name"}, script = "return WebAssembly.Module.customSections(module, name);")
    private static native JSArrayReader<ArrayBuffer> getSection(JSObject module, String name);

    @JSExport
    public static Deobfuscator createFromExternalFile(byte[] data) {
        var parser = new LinesDeobfuscationParser();
        var success = ExternalDebugFile.read(data, sectionName -> {
            if (!parser.canHandleSection(sectionName)) {
                return null;
            }
            return (bytes, start, end) -> parser.applySection(sectionName, Arrays.copyOfRange(bytes, start, end));
        });
        if (!success) {
            return null;
        }
        var lineInfo = parser.getLineInfo();
        if (lineInfo == null) {
            return null;
        }
        return new Deobfuscator(lineInfo);
    }
}
