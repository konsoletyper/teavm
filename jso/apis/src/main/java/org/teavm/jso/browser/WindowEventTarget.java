/*
 *  Copyright 2015 Alexey Andreev.
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

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.FocusEventTarget;
import org.teavm.jso.dom.events.HashChangeEvent;
import org.teavm.jso.dom.events.KeyboardEventTarget;
import org.teavm.jso.dom.events.LoadEventTarget;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.dom.events.MouseEventTarget;

public interface WindowEventTarget extends EventTarget, FocusEventTarget, MouseEventTarget, KeyboardEventTarget,
        LoadEventTarget {
    default void listenBeforeOnload(EventListener<Event> listener) {
        addEventListener("beforeunload", listener);
    }

    default void neglectBeforeOnload(EventListener<Event> listener) {
        removeEventListener("beforeunload", listener);
    }

    default void listenMessage(EventListener<MessageEvent> listener) {
        addEventListener("message", listener);
    }

    default void neglectMessage(EventListener<MessageEvent> listener) {
        removeEventListener("message", listener);
    }

    default void listenHashChange(EventListener<HashChangeEvent> listener) {
        addEventListener("hashchange", listener);
    }

    default void neglectHashChange(EventListener<HashChangeEvent> listener) {
        removeEventListener("hashchange", listener);
    }
}
