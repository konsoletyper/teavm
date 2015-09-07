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
package org.teavm.dom.browser;

import org.teavm.dom.ajax.XMLHttpRequest;
import org.teavm.dom.events.EventTarget;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.json.JSON;
import org.teavm.dom.typedarrays.TypedArrayFactory;
import org.teavm.jso.JSConstructor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public interface Window extends JSObject, EventTarget, StorageProvider, TypedArrayFactory {
    @JSProperty
    HTMLDocument getDocument();

    @JSProperty
    Screen getScreen();

    void alert(JSObject message);

    void alert(String message);

    int setTimeout(TimerHandler handler, int delay);

    int setTimeout(TimerHandler handler, double delay);

    void clearTimeout(int timeoutId);

    int setInterval(TimerHandler handler, int delay);

    int setInterval(TimerHandler handler, double delay);

    void clearInterval(int timeoutId);

    @JSProperty("JSON")
    JSON getJSON();

    @JSConstructor("XMLHttpRequest")
    XMLHttpRequest createXMLHttpRequest();
}
