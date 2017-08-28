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

/** A string indicating the type of binary data being transmitted by the connection. This should be
 * either "blob" if DOM Blob objects are being used or "arraybuffer" if ArrayBuffer objects are being used. */
public enum BinaryType {
    blob,
    arraybuffer,
}