package org.teavm.classlib.java.util;

import java.nio.ByteBuffer;

/**
 * Partial {@link java.util.Base64} implementation, including only
 * basic <a href="http://www.ietf.org/rfc/rfc4648.txt">RFC 4648</a>
 * conversion.
 * Code for conversions was taken from
 * <a href="https://github.com/apache/harmony/blob/java6/drlvm/vm/vmcore/src/kernel_classes/javasrc/org/apache/harmony/misc/Base64.java">Apache Harmony</a>
 */
public class TBase64 {
    private TBase64() {
    }

    public static Decoder getDecoder() {
        return Decoder.RFC_4648;
    }

    public static Encoder getEncoder() {
        return Encoder.RFC_4648;
    }

    public static class Decoder {
        static final Decoder RFC_4648 = new Decoder();

        private Decoder() {
        }

        public byte[] decode(byte[] src) {
            TObjects.requireNonNull(src);
            byte[] dst = new byte[outputLength(src)];
            decode(src, dst);
            return dst;
        }

        public int decode(byte[] src, byte[] dst) {
            TObjects.requireNonNull(src);
            TObjects.requireNonNull(dst);
            int length = outputLength(src);
            if (dst.length < length) {
                throw new IllegalArgumentException(
                        "Output byte array is " + dst.length + ". It needs to be at least " + length + ".");
            }

            int pad = 0, index = 0, j = 0, bits = 0;
            byte[] bytes = new byte[4];
            byte chr;
            for (int i = 0; i < src.length; i++) {
                if (src[i] == '\n' || src[i] == '\r') {
                    continue;
                }
                chr = src[i];
                // char ASCII value
                //  +    43    62
                //  /    47    63
                //  0    48    52
                //     .  .  .
                //  9    57    61 (ASCII + 4)
                //  =    61   pad
                //  A    65    0
                //     .  .  .
                //  Z    90    25 (ASCII - 65)
                //  a    97    26
                //     .  .  .
                //  z    122   51 (ASCII - 71)
                if (chr == '+') {
                    bits = 62;
                } else if (chr == '/') {
                    bits = 63;
                } else if ((chr >= '0') && (chr <= '9')) {
                    bits = chr + 4;
                } else if (chr == '=') {
                    bits = 0;
                    pad++;
                } else if ((chr >= 'A') && (chr <= 'Z')) {
                    bits = chr - 65;
                } else if ((chr >= 'a') && (chr <= 'z')) {
                    bits = chr - 71;
                } else {
                    throw new IllegalArgumentException("Invalid char: " + chr);
                }
                bytes[j % 4] = (byte) bits;
                if (j % 4 == 3) {
                    dst[index++] = (byte) (bytes[0] << 2 | bytes[1] >> 4);
                    if (pad != 2) {
                        dst[index++] = (byte) (bytes[1] << 4 | bytes[2] >> 2);
                        if (pad != 1) {
                            dst[index++] = (byte) (bytes[2] << 6 | bytes[3]);
                        }
                    }
                }
                j++;
            }
            return length;
        }

        public ByteBuffer decode(ByteBuffer buffer) {
            TObjects.requireNonNull(buffer);
            int pos = buffer.position();
            try {
                byte[] src;
                if (buffer.hasArray()) {
                    src = buffer.array();
                    buffer.position(buffer.limit());
                } else {
                    src = new byte[buffer.remaining()];
                    buffer.get(src);
                }
                byte[] dst = new byte[outputLength(src)];
                return ByteBuffer.wrap(dst, 0, decode(src, dst));
            } catch (IllegalArgumentException iae) {
                buffer.position(pos);
                throw iae;
            }
        }

        public byte[] decode(String src) {
            return decode(src.getBytes());
        }

        private int outputLength(byte[] input) {
            if (input.length == 0)
                return 0;
            if (input.length < 2) {
                throw new IllegalArgumentException("Input length should be at least 2 bytes for base64.");
            }
            int length = input.length;
            if (input[input.length - 1] == '=') {
                length--;
            }
            if (input[input.length - 2] == '=') {
                length--;
            }
            return length * 3 / 4;
        }
    }

    public static class Encoder {
        static final Encoder RFC_4648 = new Encoder();

        private static final byte[] map = new byte[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
                'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', '+', '/'};

        private Encoder() {
        }

        public byte[] encode(byte[] src) {
            TObjects.requireNonNull(src);
            byte[] dst = new byte[outputLength(src.length)];
            encode(src, dst);
            return dst;
        }

        public int encode(byte[] src, byte[] dst) {
            int length = outputLength(src.length);
            if (dst.length < length) {
                throw new IllegalArgumentException(
                        "Output byte array is " + dst.length + ". It needs to be at least " + length + ".");
            }

            length += length / 76 + 3;
            int index = 0, i, crlr = 0, end = src.length - src.length % 3;
            for (i = 0; i < end; i += 3) {
                dst[index++] = map[(src[i] & 0xff) >> 2];
                dst[index++] = map[((src[i] & 0x03) << 4) | ((src[i + 1] & 0xff) >> 4)];
                dst[index++] = map[((src[i + 1] & 0x0f) << 2) | ((src[i + 2] & 0xff) >> 6)];
                dst[index++] = map[(src[i + 2] & 0x3f)];
                if (((index - crlr) % 76 == 0) && (index != 0)) {
                    dst[index++] = '\n';
                    crlr++;
                }
            }
            switch (src.length % 3) {
                case 1:
                    dst[index++] = map[(src[end] & 0xff) >> 2];
                    dst[index++] = map[(src[end] & 0x03) << 4];
                    dst[index++] = '=';
                    dst[index++] = '=';
                    break;
                case 2:
                    dst[index++] = map[(src[end] & 0xff) >> 2];
                    dst[index++] = map[((src[end] & 0x03) << 4) | ((src[end + 1] & 0xff) >> 4)];
                    dst[index++] = map[((src[end + 1] & 0x0f) << 2)];
                    dst[index++] = '=';
                    break;
            }
            return length;
        }

        public ByteBuffer encode(ByteBuffer buffer) {
            byte[] dst = new byte[outputLength(buffer.remaining())];
            if (buffer.hasArray()) {
                encode(buffer.array(), dst);
                buffer.position(buffer.limit());
            } else {
                byte[] src = new byte[buffer.remaining()];
                buffer.get(src);
                encode(src, dst);
            }
            return ByteBuffer.wrap(dst);
        }

        public String encodeToString(byte[] src) {
            byte[] encoded = encode(src);
            return new String(encoded);
        }

        private int outputLength(int inputLength) {
            int n = inputLength % 3;
            return 4 * inputLength / 3
                    + (n == 0 ? 0 : 1)
                    + (n == 0 ? 0 : 3 - n);
        }
    }
}