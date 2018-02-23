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

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.Event;

public abstract class CloseEvent implements Event {
    public enum Code {
        RESERVED_UNUSED(
            null,
            "Reserved and not used."
        ),
        CLOSE_NORMAL(
            (short) 1000,
            "Normal closure; the connection successfully completed whatever purpose for which it was created."
        ),
        CLOSE_GOING_AWAY(
            (short) 1001,
            "The endpoint is going away, either because of a server failure or because the browser is navigating away"
                    + " from the page that opened the connection."
        ),
        CLOSE_PROTOCOL_ERROR((short) 1002, "The endpoint is terminating the connection due to a protocol error,"),
        CLOSE_UNSUPPORTED((short) 1003, "The connection is being terminated because the endpoint received data of a"
                + " type it cannot accept (for example, a text-only endpoint received binary data),"),
        RESERVED_1004((short) 1004, "Reserved. A meaning might be defined in the future,"),
        CLOSE_NO_STATUS((short) 1005, "Reserved.  Indicates that no status code was provided even though one was"
                + " expected,"),
        CLOSE_ABNORMAL((short) 1006, "Reserved. Used to indicate that a connection was closed abnormally (that is, with"
                + " no close frame being sent) when a status code is expected,"),
        Unsupported_Data((short) 1007, "The endpoint is terminating the connection because a message was received that"
                + " contained inconsistent data (e.g., non-UTF-8 data within a text message),"),
        Policy_Violation((short) 1008, "The endpoint is terminating the connection because it received a message that"
                + " violates its policy. This is a generic status code, used when codes 1003 and 1009 are not"
                + " suitable,"),
        CLOSE_TOO_LARGE((short) 1009, "The endpoint is terminating the connection because a data frame was received"
                + " that is too large,"),
        Missing_Extension((short) 1010, "The client is terminating the connection because it expected the server to"
                + " negotiate one or more extension, but the server didn't,"),
        Internal_Error((short) 1011, "The server is terminating the connection because it encountered an unexpected"
                + " condition that prevented it from fulfilling the request,"),
        Service_Restart((short) 1012, "The server is terminating the connection because it is restarting. [Ref]"),
        Try_Again_Later((short) 1013, "The server is terminating the connection due to a temporary condition, e.g. it"
                + " is overloaded and is casting off some of its clients. [Ref]"),
        RESERVED_1014((short) 1014, "Reserved for future use by the WebSocket standard,"),
        TLS_Handshake((short) 1015, "Reserved. Indicates that the connection was closed due to a failure to perform a"
                + " TLS handshake (e.g., the server certificate can't be verified),"),
        RESERVED_FUTURE_USE(null, "Reserved for future use by the WebSocket standard,"),
        RESERVED_EXTENSIONS(null, "Reserved for use by WebSocket extensions,"),
        LIBRARY_SPECIFIED(null, "Available for use by libraries and frameworks. May not be used by applications."
                + " Available for registration at the IANA via first-come, first-serve,"),
        APPLICATION_SPECIFIED(null, "Available for use by applications,"),
        ;
        public final Short value;
        public final String description;

        Code(Short value, String description) {
            this.value = value;
            this.description = description;
        }
    }

    private static final Map<Short, Code> codeEnums = new HashMap<Short, Code>() {
        {
            put(Code.CLOSE_NORMAL);
            put(Code.CLOSE_GOING_AWAY);
            put(Code.CLOSE_PROTOCOL_ERROR);
            put(Code.CLOSE_UNSUPPORTED);
            put(Code.RESERVED_1004);
            put(Code.CLOSE_NO_STATUS);
            put(Code.CLOSE_ABNORMAL);
            put(Code.Unsupported_Data);
            put(Code.Policy_Violation);
            put(Code.CLOSE_TOO_LARGE);
            put(Code.Missing_Extension);
            put(Code.Internal_Error);
            put(Code.Service_Restart);
            put(Code.Try_Again_Later);
            put(Code.RESERVED_1014);
            put(Code.TLS_Handshake);
        }

        void put(Code code) {
            put(code.value, code);
        }
    };

    public Code codeAsEnum() {
        short codeValue = code();
        Code code = codeEnums.get(codeValue);
        if (code == null) {
            if (-1 < codeValue && codeValue < 1000) {
                return Code.RESERVED_UNUSED;
            } else if (1016 < codeValue && codeValue < 2000) {
                return Code.RESERVED_FUTURE_USE;
            } else if (codeValue < 3000) {
                return Code.RESERVED_EXTENSIONS;
            } else if (codeValue < 4000) {
                return Code.LIBRARY_SPECIFIED;
            } else if (codeValue < 5000) {
                return Code.APPLICATION_SPECIFIED;
            }
        }
        return code;
    }

    @JSBody(script = "return this.code;")
    public abstract /*unsigned*/short code();

    /** @return a String indicating the reason the server closed the connection. This is specific to the
     * particular server and sub-protocol. */
    @JSBody(script = "return this.reason;")
    public abstract /*DOM*/String reason();

    /** @return a boolean that indicates whether or not the connection was cleanly closed. */
    @JSBody(script = "return this.wasClean;")
    public abstract boolean wasClean();
}