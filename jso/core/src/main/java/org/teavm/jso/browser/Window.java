/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.jso.browser;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Window implements JSObject, EventTarget, StorageProvider {
    private Window() {
    }

    @JSProperty
    public abstract HTMLDocument getDocument();

    @JSProperty
    public abstract Screen getScreen();

    @JSBody(params = "message", script = "alert(message);")
    public static native void alert(JSObject message);

    @JSBody(params = "message", script = "alert(message);")
    public static native void alert(String message);

    @JSBody(params = { "handler", "delay" }, script = "return setTimeout(handler, delay);")
    public static native int setTimeout(TimerHandler handler, int delay);

    @JSBody(params = { "handler", "delay" }, script = "return setTimeout(handler, delay);")
    public static native int setTimeout(TimerHandler handler, double delay);

    @JSBody(params = { "timeoutId" }, script = "clearTimeout(timeoutId);")
    public abstract void clearTimeout(int timeoutId);

    @JSBody(params = { "handler", "delay" }, script = "return setInverval(handler, delay);")
    public abstract int setInterval(TimerHandler handler, int delay);

    @JSBody(params = { "handler", "delay" }, script = "return setInverval(handler, delay);")
    public abstract int setInterval(TimerHandler handler, double delay);

    @JSBody(params = { "timeoutId" }, script = "clearInverval(timeoutId);")
    public abstract void clearInterval(int timeoutId);

    @JSBody(params = {}, script = "return window;")
    public static native Window current();
}
