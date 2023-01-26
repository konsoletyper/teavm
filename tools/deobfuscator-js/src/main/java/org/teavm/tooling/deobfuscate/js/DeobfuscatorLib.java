/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.tooling.deobfuscate.js;

import java.io.IOException;
import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.ArrayBuffer;

public final class DeobfuscatorLib implements DeobfuscatorJs {
    private DeobfuscatorLib() {
    }

    @Override
    public DeobfuscateFunction create(ArrayBuffer buffer, String classesFileName) {
        try {
            return new Deobfuscator(buffer, classesFileName)::deobfuscate;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        install(new DeobfuscatorLib());
    }

    @JSBody(params = "instance", script =
            "deobfuscator.create = function(buffer, classesFileName) {"
                + "return instance.create(buffer, classesFileName);"
            + "}"
    )
    private static native void install(DeobfuscatorJs js);
}

