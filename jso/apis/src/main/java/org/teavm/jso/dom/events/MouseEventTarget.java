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
    @Deprecated
    default void listenClick(EventListener<MouseEvent> listener) {
        addEventListener("click", listener);
    }

    @Deprecated
    default void neglectClick(EventListener<MouseEvent> listener) {
        removeEventListener("click", listener);
    }

    @Deprecated
    default void listenDoubleClick(EventListener<MouseEvent> listener) {
        addEventListener("dblclick", listener);
    }

    @Deprecated
    default void neglectDoubleClick(EventListener<MouseEvent> listener) {
        removeEventListener("dblclick", listener);
    }

    @Deprecated
    default void listenMouseDown(EventListener<MouseEvent> listener) {
        addEventListener("mousedown", listener);
    }

    @Deprecated
    default void neglectMouseDown(EventListener<MouseEvent> listener) {
        removeEventListener("mousedown", listener);
    }

    @Deprecated
    default void listenMouseUp(EventListener<MouseEvent> listener) {
        addEventListener("mouseup", listener);
    }

    @Deprecated
    default void neglectMouseUp(EventListener<MouseEvent> listener) {
        removeEventListener("mouseup", listener);
    }

    @Deprecated
    default void listenMouseMove(EventListener<MouseEvent> listener) {
        addEventListener("mousemove", listener);
    }

    @Deprecated
    default void neglectMouseMove(EventListener<MouseEvent> listener) {
        removeEventListener("mousemove", listener);
    }

    @Deprecated
    default void listenMouseOver(EventListener<MouseEvent> listener) {
        addEventListener("mouseover", listener);
    }

    @Deprecated
    default void neglectMouseOver(EventListener<MouseEvent> listener) {
        removeEventListener("mouseover", listener);
    }

    @Deprecated
    default void listenMouseEnter(EventListener<MouseEvent> listener) {
        addEventListener("mouseenter", listener);
    }

    @Deprecated
    default void neglectMouseEnter(EventListener<MouseEvent> listener) {
        removeEventListener("mouseenter", listener);
    }

    @Deprecated
    default void listenMouseLeave(EventListener<MouseEvent> listener) {
        addEventListener("mouseleave", listener);
    }

    @Deprecated
    default void neglectMouseLeave(EventListener<MouseEvent> listener) {
        removeEventListener("mouseleave", listener);
    }

    @Deprecated
    default void listenMouseOut(EventListener<MouseEvent> listener) {
        addEventListener("mouseout", listener);
    }

    @Deprecated
    default void neglectMouseOut(EventListener<MouseEvent> listener) {
        removeEventListener("mouseout", listener);
    }

    default Registration onClick(EventListener<MouseEvent> listener) {
        return onEvent("click", listener);
    }

    default Registration onDoubleClick(EventListener<MouseEvent> listener) {
        return onEvent("dblclick", listener);
    }

    default Registration onMouseDown(EventListener<MouseEvent> listener) {
        return onEvent("mousedown", listener);
    }

    default Registration onMouseUp(EventListener<MouseEvent> listener) {
        return onEvent("mouseup", listener);
    }

    default Registration onMouseMove(EventListener<MouseEvent> listener) {
        return onEvent("mousemove", listener);
    }

    default Registration onMouseOver(EventListener<MouseEvent> listener) {
        return onEvent("mouseover", listener);
    }

    default Registration onMouseEnter(EventListener<MouseEvent> listener) {
        return onEvent("mouseenter", listener);
    }

    default Registration onMouseLeave(EventListener<MouseEvent> listener) {
        return onEvent("mouseleave", listener);
    }

    default Registration onMouseOut(EventListener<MouseEvent> listener) {
        return onEvent("mouseout", listener);
    }
}
