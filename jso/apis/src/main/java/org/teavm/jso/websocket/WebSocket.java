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
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.typedarrays.ArrayBuffer;

public abstract class WebSocket implements JSObject {
    public static WebSocket newInstance(String url, String... protocols) {
        return protocols == null ? newInstance(url) : newInstance_(url, protocols);
    }

    @JSBody(params = { "url" }, script = "return new WebSocket(url);")
    public static native WebSocket newInstance(String url);

    private WebSocket() {
    }

    /** @return The URL as resolved by the constructor. This is always an absolute URL. */
    @JSBody(script = "return this.url;")
    public native /*DOM*/String url();

    /** @return A string indicating the name of the sub-protocol the server selected; this will be one of the strings
     * specified in the protocols parameter when creating the WebSocket object. */
    @JSBody(script = "return this.protocol;")
    public native /*DOM*/String protocol();

    /** @return The extensions selected by the server. This is currently only the empty string or a list of extensions
     * as negotiated by the connection. */
    @JSBody(script = "return this.extensions;")
    public native /*DOM*/String extensions();

    /** A string indicating the type of binary data being transmitted by the connection. This should be
     * either "blob" if DOM Blob objects are being used or "arraybuffer" if ArrayBuffer objects are being used. */
    @JSBody(params = "binaryType", script = "this.binaryType = binaryType;")
    public native void binaryType(/*DOM*/String binaryType);

    /** See {@link WebSocket#binaryType(String)} */
    @JSBody(script = "return this.binaryType;")
    public native /*DOM*/String binaryType();

    /** @return The number of bytes of data that have been queued using calls to send() but not yet transmitted to
     * the network. This value resets to zero once all queued data has been sent. This value does not reset to zero
     * when the connection is closed; if you keep calling send(), this will continue to climb. */
    @JSBody(script = "return this.bufferedAmount;")
    public native /*unsigned long*/int bufferedAmount();

    /** The current state of the connection, can be one of:<br>
     * 0 CONNECTING: The connection is not yet open.<br>
     * 1 OPEN: The connection is open and ready to communicate.<br>
     * 2 CLOSING: The connection is in the process of closing.<br>
     * 3 CLOSED: The connection is closed or couldn't be opened. */
    @JSBody(script = "return this.readyState;")
    public native /*unsigned*/short readyState();

    /** An event listener to be called when the WebSocket connection's readyState changes to OPEN; this indicates
     * that the connection is ready to send and receive data. The event is a simple one with the name "open". */
    @JSBody(params = "onOpen", script = "this.onopen = onOpen;")
    public native void onOpen(EventListener<Event> onOpen);

    /** See {@link WebSocket#onOpen(EventListener)} */
    @JSBody(script = "return this.onopen;")
    public native EventListener<Event> onOpen();

    /** An event listener to be called when the WebSocket connection's readyState changes to CLOSED. The listener
     * receives a CloseEvent named "close". */
    @JSBody(params = "onClose", script = "this.onclose = onClose;")
    public native void onClose(EventListener<CloseEvent> onClose);

    /** See {@link WebSocket#onClose(EventListener)} */
    @JSBody(script = "return this.onclose;")
    public native EventListener<CloseEvent> onClose();

    /** An event listener to be called when a message is received from the server. The listener receives a
     * MessageEvent named "message". */
    @JSBody(params = "onMessage", script = "this.onmessage = onMessage;")
    public native void onMessage(EventListener<MessageEvent> onMessage);

    /** See {@link WebSocket#onMessage(EventListener)} */
    @JSBody(script = "return this.onmessage;")
    public native EventListener<MessageEvent> onMessage();

    /** An event listener to be called when an error occurs. This is a simple event named "error". */
    @JSBody(params = "onError", script = "this.onerror = onError;")
    public native void onError(EventListener<Event> onError);

    /** See {@link WebSocket#onError(EventListener)} */
    @JSBody(script = "return this.onerror;")
    public native EventListener<Event> onError();

    /** See {@link WebSocket#close(short, String)} */
    public abstract void close();

    /** See {@link WebSocket#close(short, String)} */
    public abstract void close(/*unsigned*/short code);

    /** Closes the WebSocket connection or connection attempt, if any. If the connection is already CLOSED, this
     * method does nothing.
     * @param code A numeric value indicating the status code explaining why the connection is being closed. If this
     * parameter is not specified, a default value of 1000 (indicating a normal "transaction complete" closure) is
     * assumed. See the list of status codes on the CloseEvent page for permitted values.
     * @param reason A human-readable string explaining why the connection is closing. This string must be no longer
     * than 123 bytes of UTF-8 text (not characters). */
    public abstract void close(/*unsigned*/short code, /*DOM*/String reason); //throw InvalidAccessException,SyntaxError

    public abstract void send(/*DOM*/String data); // throws InvalidStateException, SyntaxError;

    public abstract void send(ArrayBuffer data); // throws InvalidStateException, SyntaxError;

//    public abstract void send( Blob data ); // throws InvalidStateException, SyntaxError; //TODO

    @JSBody(params = { "url", "protocols" }, script = "return new WebSocket( url, protocols );")
    private static native WebSocket newInstance_(String url, String... protocols);
}