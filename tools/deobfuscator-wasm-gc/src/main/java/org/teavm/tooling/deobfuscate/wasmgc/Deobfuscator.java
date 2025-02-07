/*
 *  Copyright 2021 konsoletyper.
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

import org.teavm.backend.wasm.debug.info.LineInfo;
import org.teavm.jso.JSExport;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;

public final class Deobfuscator {
    private LineInfo debugInformation;

    Deobfuscator(LineInfo debugInformation) {
        this.debugInformation = debugInformation;
    }

    @JSExport
    public JSArrayReader<Frame> deobfuscate(int[] addresses) {
        var locations = debugInformation.deobfuscate(addresses);
        var frames = new JSArray<Frame>();
        for (var location : locations) {
            var frame = new Frame(location.method.cls().fullName(), location.method.name(),
                    location.file != null ? location.file.name() : null, location.line);
            frames.push(frame);
        }
        return frames;
    }
}
