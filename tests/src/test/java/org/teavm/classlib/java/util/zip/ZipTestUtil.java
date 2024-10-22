/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.classlib.java.util.zip;

final class ZipTestUtil {
    private ZipTestUtil() {
    }

    static byte[] readHex(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; ++i) {
            int h = Character.digit(hex.charAt(i * 2), 16);
            int l = Character.digit(hex.charAt(i * 2 + 1), 16);
            data[i] = (byte) ((h << 4) | l);
        }
        return data;
    }

    static String writeHex(byte[] data) {
        var hex = new char[data.length * 2];
        for (int i = 0; i < data.length; ++i) {
            var b = data[i];
            var h = Character.forDigit((b & 255) / 16, 16);
            var l = Character.forDigit((b & 255) % 16, 16);
            hex[i * 2] = h;
            hex[i * 2 + 1] = l;
        }
        return new String(hex);
    }
}
