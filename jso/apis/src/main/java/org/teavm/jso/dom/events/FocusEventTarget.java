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
package org.teavm.jso.dom.events;

public interface FocusEventTarget extends EventTarget {
    default void listenFocus(EventListener<Event> listener) {
        addEventListener("focus", listener);
    }

    default void neglectFocus(EventListener<Event> listener) {
        removeEventListener("focus", listener);
    }

    default void listenBlur(EventListener<Event> listener) {
        addEventListener("blur", listener);
    }

    default void neglectBlur(EventListener<Event> listener) {
        removeEventListener("blur", listener);
    }
}
