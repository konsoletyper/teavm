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

public interface KeyboardEventTarget extends EventTarget {
    default void listenKeyDown(EventListener<KeyboardEvent> listener) {
        addEventListener("keydown", listener);
    }

    default void neglectKeyDown(EventListener<KeyboardEvent> listener) {
        removeEventListener("keydown", listener);
    }

    default void listenKeyUp(EventListener<KeyboardEvent> listener) {
        addEventListener("keyup", listener);
    }

    default void neglectKeyUp(EventListener<KeyboardEvent> listener) {
        removeEventListener("keyup", listener);
    }

    default void listenKeyPress(EventListener<KeyboardEvent> listener) {
        addEventListener("keypress", listener);
    }

    default void neglectKeyPress(EventListener<KeyboardEvent> listener) {
        removeEventListener("keypress", listener);
    }
}
