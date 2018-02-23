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

/** The current state of the connection. See individual enum's for specific meanings. */
public enum ReadyState {
    /** The connection is not yet open. */
    CONNECTING,
    /** The connection is open and ready to communicate. */
    OPEN,
    /** The connection is in the process of closing. */
    CLOSING,
    /** The connection is closed or couldn't be opened. */
    CLOSED,
}