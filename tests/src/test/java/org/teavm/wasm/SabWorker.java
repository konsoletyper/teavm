/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.wasm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Atomics;
import org.teavm.jso.typedarrays.Int32Array;

public class SabWorker {
    private SabWorker() {
    }

    private static final IntBuffer buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asIntBuffer();
    private static final Int32Array jsBuffer = Int32Array.fromJavaBuffer(buffer);
    private static int lastSentValue = 23;
    private static int remaining = 5;

    @JSExport
    public static void test() {
        var message = JSObjects.<JSMapLike<JSObject>>create();
        message.set("type", JSString.valueOf("init"));
        message.set("buffer", jsBuffer);
        Atomics.store(jsBuffer, 0, 0);
        Window.worker().postMessage(message);
        Window.setTimeout(SabWorker::loop, 50);
    }

    private static void loop() {
        buffer.put(1, lastSentValue++);

        // memory fence
        Atomics.add(jsBuffer, 0, 1);
        var message = JSObjects.<JSMapLike<JSObject>>create();
        message.set("type", JSString.valueOf("update"));
        Window.worker().postMessage(message);

        if (--remaining > 0) {
            Window.setTimeout(SabWorker::loop, 50);
        } else {
            message = JSObjects.<JSMapLike<JSObject>>create();
            message.set("type", JSString.valueOf("done"));
            Window.worker().postMessage(message);
        }
    }
}
