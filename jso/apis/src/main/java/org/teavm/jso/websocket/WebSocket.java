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
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.typedarrays.ArrayBuffer;

public abstract class WebSocket implements JSObject {
    public static WebSocket newInstance(JSString url, JSString...protocols) {
        return protocols == null ? newInstance(url) : newInstance(url, JSArray.of(protocols));
    }

    public static WebSocket newInstance(JSString url, JSArray<JSString> protocols) {
        return protocols == null ? newInstance(url) : newInstance_(url, protocols);
    }

    @JSBody(params = { "url" }, script = "return new WebSocket(url);")
    public static native WebSocket newInstance(JSString url);

    private WebSocket() {
    }

    /** @return The URL as resolved by the constructor. This is always an absolute URL. */
    @JSProperty
    public native JSString getUrl();

    /** @return A string indicating the name of the sub-protocol the server selected; this will be one of the strings
     * specified in the protocols parameter when creating the WebSocket object. */
    @JSProperty
    public native JSString getProtocol();

    /** @return The extensions selected by the server. This is currently only the empty string or a list of extensions
     * as negotiated by the connection. */
    @JSProperty
    public native JSString getExtensions();

    /** A string indicating the type of binary data being transmitted by the connection. This should be
     * either "blob" if DOM Blob objects are being used or "arraybuffer" if ArrayBuffer objects are being used. */
    @JSProperty
    public native void setBinaryType(JSString binaryType);

    /** See {@link WebSocket#setBinaryType(JSString)} */
    @JSProperty
    public native JSString getBinaryType();

    /** @return The number of bytes of data that have been queued using calls to send() but not yet transmitted to
     * the network. This value resets to zero once all queued data has been sent. This value does not reset to zero
     * when the connection is closed; if you keep calling send(), this will continue to climb. */
    @JSProperty
    public native JSNumber getBufferedAmount();

    /** The current state of the connection, can be one of:<br>
     * 0 CONNECTING: The connection is not yet open.<br>
     * 1 OPEN: The connection is open and ready to communicate.<br>
     * 2 CLOSING: The connection is in the process of closing.<br>
     * 3 CLOSED: The connection is closed or couldn't be opened. */
    @JSProperty
    public native JSNumber getReadyState();

    /** An event listener to be called when the WebSocket connection's readyState changes to OPEN; this indicates
     * that the connection is ready to send and receive data. The event is a simple one with the name "open". */
    @JSProperty
    public native void setOnopen(EventListener<Event> onOpen);

    /** See {@link WebSocket#setOnopen(EventListener)} */
    @JSProperty
    public native EventListener<Event> getOnopen();

    /** An event listener to be called when the WebSocket connection's readyState changes to CLOSED. The listener
     * receives a CloseEvent named "close". */
    @JSProperty
    public native void setOnclose(EventListener<CloseEvent> onClose);

    /** See {@link WebSocket#setOnclose(EventListener)} */
    @JSProperty
    public native EventListener<CloseEvent> getOnclose();

    /** An event listener to be called when a message is received from the server. The listener receives a
     * MessageEvent named "message". */
    @JSProperty
    public native void setOnmessage(EventListener<MessageEvent> onMessage);

    /** See {@link WebSocket#setOnmessage(EventListener)} */
    @JSProperty
    public native EventListener<MessageEvent> getOnmessage();

    /** An event listener to be called when an error occurs. This is a simple event named "error". */
    @JSProperty
    public native void setOnerror(EventListener<Event> onError);

    /** See {@link WebSocket#setOnerror(EventListener)} */
    @JSProperty
    public native EventListener<Event> getOnerror();

    /** See {@link WebSocket#close(JSNumber, JSString)} */
    public abstract void close();

    /** See {@link WebSocket#close(JSNumber, JSString)} */
    public abstract void close(JSNumber code);

    /** Closes the WebSocket connection or connection attempt, if any. If the connection is already CLOSED, this
     * method does nothing.
     * @param code A numeric value indicating the status code explaining why the connection is being closed. If this
     * parameter is not specified, a default value of 1000 (indicating a normal "transaction complete" closure) is
     * assumed. See the list of status codes on the CloseEvent page for permitted values.
     * @param reason A human-readable string explaining why the connection is closing. This string must be no longer
     * than 123 bytes of UTF-8 text (not characters). */
    public abstract void close(JSNumber code, JSString reason); //throw InvalidAccessException,SyntaxError

    public abstract void send(JSString data); // throws InvalidStateException, SyntaxError;

    public abstract void send(ArrayBuffer data); // throws InvalidStateException, SyntaxError;

//    public abstract void send( Blob data ); // throws InvalidStateException, SyntaxError; //TODO

    @JSBody(params = { "url", "protocols" }, script = "return new WebSocket( url, protocols );")
    private static native WebSocket newInstance_(JSString url, JSArray<JSString> protocols);
}