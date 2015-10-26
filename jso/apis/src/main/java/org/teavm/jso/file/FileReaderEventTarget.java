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
package org.teavm.jso.file;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;

/**
*
* @author Jan-Felix Wittmann
*/
public interface FileReaderEventTarget extends EventTarget {

    default void listenAbort(EventListener<Event> listener) {
        addEventListener("abort", listener);
    }

    default void neglectAbort(EventListener<Event> listener) {
        removeEventListener("abort", listener);
    }

    default void listenError(EventListener<Event> listener) {
        addEventListener("error", listener);
    }

    default void neglectError(EventListener<Event> listener) {
        removeEventListener("error", listener);
    }

    default void listenLoad(EventListener<Event> listener) {
        addEventListener("load", listener);
    }

    default void neglectLoad(EventListener<Event> listener) {
        removeEventListener("load", listener);
    }

    default void listenLoadStart(EventListener<Event> listener) {
        addEventListener("loadstart", listener);
    }

    default void neglectLoadStart(EventListener<Event> listener) {
        removeEventListener("loadstart", listener);
    }

    default void listenLoadEnd(EventListener<Event> listener) {
        addEventListener("loadend", listener);
    }

    default void neglectLoadEnd(EventListener<Event> listener) {
        removeEventListener("loadend", listener);
    }

    default void listenProgress(EventListener<Event> listener) {
        addEventListener("progress", listener);
    }

    default void neglectProgress(EventListener<Event> listener) {
        removeEventListener("progress", listener);
    }

}
