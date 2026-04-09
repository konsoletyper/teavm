/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.samples.emscripten;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.teavm.interop.Import;

public class Main {
    public static void main(String[] args) {
        var buffer = ByteBuffer.allocateDirect(32).asIntBuffer();
        buffer.put(0, 23);
        buffer.put(1, 42);
        cppAdd(buffer);
        System.out.println(buffer.get(2));
    }

    @Import(module = "native", name = "addInBuffer")
    private static native void cppAdd(IntBuffer buffer);
}
