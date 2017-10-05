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

import java.io.IOException;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.typedarrays.ArrayBuffer;

public class WebSocketExt {
    private final WebSocket webSocket;

    public static void newInstance(String url, ConsumerCallback<WebSocketExt> callback) {
        newInstance(url, callback);
    }

    public static void newInstance(String url, String[] protocols, ConsumerCallback<WebSocketExt> callback) {
        JSArray<JSString> jsProtocols = JSArray.create();
        int length = protocols == null ? 0 : protocols.length;
        while (0 < --length) {
            jsProtocols.push(JSString.valueOf(protocols[length]));
        }
        WebSocket webSocket = WebSocket.newInstance(JSString.valueOf(url), jsProtocols);
        webSocket.setOnopen(event -> {
            webSocket.setOnopen(e -> {
            });
            webSocket.setOnerror(e -> {
            });
            callback.accept(new WebSocketExt(webSocket));
        });
        webSocket.setOnerror(event -> callback.exception(new IOException("Unable to open WebSocket.")));
    }

    private WebSocketExt(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    /** @return The URL as resolved by the constructor. This is always an absolute URL. */
    public String url() {
        return webSocket.getUrl().stringValue();
    }

    /** @return A string indicating the name of the sub-protocol the server selected; this will be one of the strings
     * specified in the protocols parameter when creating the WebSocket object. */
    public String protocol() {
        return webSocket.getProtocol().stringValue();
    }

    /** @return The extensions selected by the server. This is currently only the empty string or a list of extensions
     * as negotiated by the connection. */
    public String extensions() {
        return webSocket.getExtensions().stringValue();
    }

    /** A string indicating the type of binary data being transmitted by the connection. This should be
     * either "blob" if DOM Blob objects are being used or "arraybuffer" if ArrayBuffer objects are being used. */
    public void binaryType(BinaryType binaryType) {
        webSocket.setBinaryType(JSString.valueOf(binaryType.name()));
    }

    /** See {@link WebSocket#setBinaryType(JSString)} */
    public BinaryType binaryType() {
        try {
            return BinaryType.valueOf(webSocket.getBinaryType().stringValue());
        } catch (Exception e) {
            return null;
        }
    }

    /** @return The number of bytes of data that have been queued using calls to send() but not yet transmitted to
     * the network. This value resets to zero once all queued data has been sent. This value does not reset to zero
     * when the connection is closed; if you keep calling send(), this will continue to climb. */
    public int bufferedAmount() {
        return webSocket.getBufferedAmount().intValue();
    }

    /** The current state of the connection */
    public ReadyState readyState() {
        try {
            return ReadyState.values()[webSocket.getReadyState().intValue()];
        } catch (Exception e) {
            return null;
        }
    }

    /** An event listener to be called when the WebSocket connection's readyState changes to OPEN; this indicates
     * that the connection is ready to send and receive data. The event is a simple one with the name "open". */
    public void onOpen(EventListener<Event> onOpen) {
        webSocket.setOnopen(onOpen);
    }

    /** See {@link WebSocket#setOnopen(EventListener)} */
    public EventListener<Event> onOpen() {
        return webSocket.getOnopen();
    }

    /** An event listener to be called when the WebSocket connection's readyState changes to CLOSED. The listener
     * receives a CloseEvent named "close". */
    public void onClose(EventListener<CloseEvent> onClose) {
        webSocket.setOnclose(onClose);
    }

    /** See {@link WebSocket#setOnclose(EventListener)} */
    public EventListener<CloseEvent> onClose() {
        return webSocket.getOnclose();
    }

    /** An event listener to be called when a message is received from the server. The listener receives a
     * MessageEvent named "message". */
    public void onMessage(EventListener<MessageEvent> onMessage) {
        webSocket.setOnmessage(onMessage);
    }

    /** See {@link WebSocket#setOnmessage(EventListener)} */
    public EventListener<MessageEvent> onMessage() {
        return webSocket.getOnmessage();
    }

    /** An event listener to be called when an error occurs. This is a simple event named "error". */
    public void onError(EventListener<Event> onError) {
        webSocket.setOnerror(onError);
    }

    /** See {@link WebSocket#setOnerror(EventListener)} */
    public EventListener<Event> onError() {
        return webSocket.getOnerror();
    }

    /** See {@link WebSocket#close(JSNumber, JSString)} */
    public void close() {
        webSocket.close();
    }

    /** See {@link WebSocket#close(JSNumber, JSString)} */
    public void close(short code) {
        webSocket.close(JSNumber.valueOf(code));
    }

    /** Closes the WebSocket connection or connection attempt, if any. If the connection is already CLOSED, this
     * method does nothing.
     * @param code A numeric value indicating the status code explaining why the connection is being closed. If this
     * parameter is not specified, a default value of 1000 (indicating a normal "transaction complete" closure) is
     * assumed. See the list of status codes on the CloseEvent page for permitted values.
     * @param reason A human-readable string explaining why the connection is closing. This string must be no longer
     * than 123 bytes of UTF-8 text (not characters). */
    public void close(short code, String reason) // throws InvalidAccessException, SyntaxError;
    {
        webSocket.close(JSNumber.valueOf(code), JSString.valueOf(reason));
    }

    public void send(String data) // throws InvalidStateException, SyntaxError;
    {
        webSocket.send(JSString.valueOf(data));
    }

    public void send(ArrayBuffer data) // throws InvalidStateException, SyntaxError;
    {
        webSocket.send(data);
    }

    public void send(Object blob) // throws InvalidStateException, SyntaxError;
    {
        throw new UnsupportedOperationException("Implementation required.");
    }
}