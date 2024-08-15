/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.jso.workers;

import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.dom.events.Registration;

public interface MessagePort extends EventTarget {
    void postMessage(Object message);

    void start();

    void close();

    default Registration onMessage(EventListener<MessageEvent> message) {
        return onEvent("message", message);
    }

    default Registration onMessageError(EventListener<MessageEvent> message) {
        return onEvent("messageerror", message);
    }
}
