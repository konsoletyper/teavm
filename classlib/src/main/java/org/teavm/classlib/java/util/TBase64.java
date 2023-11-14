/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.java.util;

import org.teavm.classlib.impl.Base64Impl;

public final class TBase64 {
    private TBase64() {
    }

    public static Encoder getEncoder() {
        return new Encoder(Base64Impl.alphabet, true);
    }

    public static Encoder getUrlEncoder() {
        return new Encoder(Base64Impl.urlAlphabet, false);
    }

    public static class Encoder {
        private final byte[] mapping;
        private final boolean padding;

        private Encoder(byte[] mapping, boolean padding) {
            this.mapping = mapping;
            this.padding = padding;
        }

        public byte[] encode(byte[] src) {
            return Base64Impl.encode(src, mapping, padding);
        }

        public String encodeToString(byte[] src) {
            var bytes = encode(src);
            var chars = new char[bytes.length];
            for (var i = 0; i < bytes.length; ++i) {
                chars[i] = (char) (bytes[i] & 0xFF);
            }
            return new String(chars);
        }

        public Encoder withoutPadding() {
            return new Encoder(mapping, false);
        }
    }

    public static Decoder getDecoder() {
        return new Decoder(Base64Impl.reverse);
    }

    public static Decoder getUrlDecoder() {
        return new Decoder(Base64Impl.urlReverse);
    }

    public static class Decoder {
        private final int[] mapping;

        private Decoder(int[] mapping) {
            this.mapping = mapping;
        }

        public byte[] decode(byte[] src) {
            return Base64Impl.decode(src, mapping);
        }

        public byte[] decode(String src) {
            var bytes = new byte[src.length()];
            for (var i = 0; i < bytes.length; ++i) {
                bytes[i] = (byte) src.charAt(i);
            }
            return decode(bytes);
        }
    }
}
