/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.common.binary;

import java.nio.charset.StandardCharsets;

public class BinaryParser {
    public byte[] data;
    public int ptr;

    public int readLEB() {
        var result = 0;
        var shift = 0;
        while (true) {
            var b = data[ptr++];
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return result;
    }

    public int readSignedLEB() {
        var result = 0;
        var shift = 0;
        while (true) {
            var b = data[ptr++];
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                if ((b & 0x40) != 0) {
                    result |= -1 << (shift + 7);
                }
                break;
            }
            shift += 7;
        }
        return result;
    }

    public String readString() {
        var length = readLEB();
        var result = new String(data, ptr, length, StandardCharsets.UTF_8);
        ptr += length;
        return result;
    }
}
