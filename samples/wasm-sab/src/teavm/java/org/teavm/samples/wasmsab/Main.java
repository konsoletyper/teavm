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
package org.teavm.samples.wasmsab;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Atomics;
import org.teavm.jso.typedarrays.Int32Array;

public class Main {
    private static final IntBuffer buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asIntBuffer();
    private static final Int32Array jsBuffer = Int32Array.fromJavaBuffer(buffer);
    private static int lastSentValue;

    public static void main(String[] args) {
        var message = JSObjects.<JSMapLike<JSObject>>create();
        message.set("type", JSString.valueOf("init"));
        message.set("buffer", jsBuffer);
        Window.worker().postMessage(message);
        loop();
    }

    private static void loop() {
        buffer.put(1, lastSentValue++);

        // memory fence
        Atomics.add(jsBuffer, 0, 1);
        var message = JSObjects.<JSMapLike<JSObject>>create();
        message.set("type", JSString.valueOf("update"));
        Window.worker().postMessage(message);

        Window.setTimeout(Main::loop, 1000);
    }

    @JSBody(params = { "handler", "timeout" }, script = "setTimeout(handler, timeout);")
    private static native void setTimeout(TimerHandler handler, int timeout);
}
