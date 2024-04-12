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
import org.teavm.jso.dom.events.GamepadEventTarget;
import org.teavm.jso.dom.events.HashChangeEvent;
import org.teavm.jso.dom.events.KeyboardEventTarget;
import org.teavm.jso.dom.events.LoadEventTarget;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.dom.events.MouseEventTarget;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.dom.events.StorageEvent;

public interface WindowEventTarget extends EventTarget, FocusEventTarget, MouseEventTarget, KeyboardEventTarget,
        LoadEventTarget, GamepadEventTarget {
    @Deprecated
    default void listenBeforeOnload(EventListener<Event> listener) {
        addEventListener("beforeunload", listener);
    }

    @Deprecated
    default void neglectBeforeOnload(EventListener<Event> listener) {
        removeEventListener("beforeunload", listener);
    }

    @Deprecated
    default void listenMessage(EventListener<MessageEvent> listener) {
        addEventListener("message", listener);
    }

    @Deprecated
    default void neglectMessage(EventListener<MessageEvent> listener) {
        removeEventListener("message", listener);
    }

    @Deprecated
    default void listenHashChange(EventListener<HashChangeEvent> listener) {
        addEventListener("hashchange", listener);
    }

    @Deprecated
    default void neglectHashChange(EventListener<HashChangeEvent> listener) {
        removeEventListener("hashchange", listener);
    }

    default Registration onBeforeUnload(EventListener<Event> listener) {
        return onEvent("beforeunload", listener);
    }

    default Registration onMessage(EventListener<MessageEvent> listener) {
        return onEvent("message", listener);
    }

    default Registration onHashChange(EventListener<HashChangeEvent> listener) {
        return onEvent("hashchange", listener);
    }

    default Registration onStorage(EventListener<StorageEvent> listener) {
        return onEvent("storage", listener);
    }
}
