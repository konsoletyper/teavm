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

public interface MouseEventTarget extends EventTarget {
    default void listenClick(EventListener<MouseEvent> listener) {
        addEventListener("click", listener);
    }

    default void neglectClick(EventListener<MouseEvent> listener) {
        removeEventListener("click", listener);
    }

    default void listenDoubleClick(EventListener<MouseEvent> listener) {
        addEventListener("dblclick", listener);
    }

    default void neglectDoubleClick(EventListener<MouseEvent> listener) {
        removeEventListener("dblclick", listener);
    }

    default void listenMouseDown(EventListener<MouseEvent> listener) {
        addEventListener("mousedown", listener);
    }

    default void neglectMouseDown(EventListener<MouseEvent> listener) {
        removeEventListener("mousedown", listener);
    }

    default void listenMouseUp(EventListener<MouseEvent> listener) {
        addEventListener("mouseup", listener);
    }

    default void neglectMouseUp(EventListener<MouseEvent> listener) {
        removeEventListener("mouseup", listener);
    }

    default void listenMouseOver(EventListener<MouseEvent> listener) {
        addEventListener("mouseover", listener);
    }

    default void neglectMouseOver(EventListener<MouseEvent> listener) {
        removeEventListener("mouseover", listener);
    }

    default void listenMouseEnter(EventListener<MouseEvent> listener) {
        addEventListener("mouseenter", listener);
    }

    default void neglectMouseEnter(EventListener<MouseEvent> listener) {
        removeEventListener("mouseenter", listener);
    }

    default void listenMouseLeaeve(EventListener<MouseEvent> listener) {
        addEventListener("mouseleave", listener);
    }

    default void neglectMouseLeave(EventListener<MouseEvent> listener) {
        removeEventListener("mouseleave", listener);
    }

    default void listenMouseOut(EventListener<MouseEvent> listener) {
        addEventListener("mouseout", listener);
    }

    default void neglectMouseOut(EventListener<MouseEvent> listener) {
        removeEventListener("mouseout", listener);
    }
}
