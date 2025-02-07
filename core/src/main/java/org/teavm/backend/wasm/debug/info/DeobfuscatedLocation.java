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
package org.teavm.backend.wasm.debug.info;

public class DeobfuscatedLocation {
    public final FileInfo file;
    public final MethodInfo method;
    public final int line;

    public DeobfuscatedLocation(FileInfo file, MethodInfo method, int line) {
        this.file = file;
        this.method = method;
        this.line = line;
    }

    @Override
    public String toString() {
        var sourceLocation = file == null || line < 0
                ? "Unknow source"
                : file.name() + ":" + line;
        return "   at " + method.cls().name() + "." + method.name() + "(" + sourceLocation + ")";
    }
}
