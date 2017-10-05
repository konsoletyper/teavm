/*
 *  Copyright 2017 Adam Ryan.
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
package org.teavm.jso.websocket;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.typedarrays.ArrayBuffer;

public abstract class MessageEvent implements Event {
    /** @return the data sent by the message emitter. */
    @JSBody(script = "return this.data;")
    public native JSObject data();
    @JSBody(script = "return this.data;")
    public native JSString dataAsString();
    @JSBody(script = "return this.data;")
    public native ArrayBuffer dataAsArray();
    /** @return a String representing the origin of the message emitter. */
    @JSBody(script = "return this.origin;")
    public native JSString origin();
    /** @return a String representing a unique ID for the event. */
    @JSBody(script = "return this.lastEventId;")
    public native JSString lastEventId();
    /** @return a MessageEventSource (which can be a WindowProxy, MessagePort, or ServiceWorker object) representing the
     * message emitter. */
    @JSBody(script = "return this.source;")
    public native JSObject source(); //TODO
    /** @return an array of MessagePort objects representing the ports associated with the channel the message is being
     * sent through (where appropriate, e.g. in channel messaging or when sending a message to a shared worker). */
    @JSBody(script = "return this.ports;")
    public native JSObject[] ports(); //TODO
}