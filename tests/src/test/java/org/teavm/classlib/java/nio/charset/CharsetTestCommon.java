/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.classlib.java.nio.charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.function.Consumer;

final class CharsetTestCommon {
    private static char[] hexDigits = "0123456789ABCDEF".toCharArray();

    static final String text = ""
            + "Здесь в моем объяснении я отмечаю все эти цифры и числа. Мне, конечно, всё равно будет, но теперь "
            + "(и, может быть, только в эту минуту) я желаю, чтобы те, которые будут судить мой поступок, могли ясно "
            + "видеть, из какой логической цепи выводов вышло мое „последнее убеждение“. Я написал сейчас выше, что "
            + "окончательная решимость, которой недоставало мне для исполнения моего „последнего убеждения“, произошла "
            + "во мне, кажется, вовсе не из логического вывода, а от какого-то странного толчка, от одного странного "
            + "обстоятельства, может быть вовсе не связанного ничем с ходом дела. "
            + "Дней десять назад зашел ко мне Рогожин, "
            + "по одному своему делу, о котором здесь лишнее распространяться. Я никогда не видал Рогожина прежде, "
            + "но слышал о нем очень многое. Я дал ему все нужные справки, и он скоро ушел, а так как он и приходил "
            + "только за справками, то тем бы дело между нами и кончилось. Но он слишком заинтересовал меня, "
            + "и весь этот день я был под влиянием странных мыслей, так что решился пойти к нему на другой день сам, "
            + "отдать визит. Рогожин был мне очевидно не рад и даже „деликатно“ намекнул, что нам нечего продолжать "
            + "знакомство; но все-таки я провел очень любопытный час, как, вероятно, и он. "
            + "Между нами был такой контраст, "
            + "который не мог не сказаться нам обоим, особенно мне: я был человек, уже сосчитавший дни свои, а он - "
            + "живущий самою полною, непосредственною жизнью, настоящею минутой, без всякой заботы о „последних“ "
            + "выводах, цифрах или о чем бы то ни было, не касающемся того, на чем... на чем... ну хоть на чем он "
            + "помешан; пусть простит мне это выражение господин Рогожин, пожалуй хоть как плохому литератору, не "
            + "умевшему выразить свою мысль. Несмотря на всю его нелюбезность, мне показалось, что он человек с умом и "
            + "может многое понимать, хотя его мало что интересует из постороннего. Я не намекал ему о моем „последнем "
            + "убеждении“, но мне почему-то показалось, что он, слушая меня, угадал его. "
            + "Он промолчал, он ужасно молчалив. "
            + "Я намекнул ему, уходя, что, несмотря на всю между нами разницу и на все противоположности, - "
            + "les extrémités se touchent 1 (я растолковал ему это по-русски), так что, может быть, он и сам вовсе не "
            + "так далек от моего „последнего убеждения“, как кажется. На это он ответил мне очень угрюмою и кислою "
            + "гримасой, встал, сам сыскал мне мою фуражку, сделав вид, будто бы я сам ухожу, и просто-запросто вывел "
            + "меня из своего мрачного дома под видом того, что провожает меня из учтивости. Дом его поразил меня; "
            + "похож на кладбище, а ему, кажется, нравится, что, впрочем, понятно: такая полная, "
            + "непосредственная жизнь, которою он живет, слишком полна сама по себе, чтобы нуждаться в обстановке.";

    static String asciiText = ""
            + "Meanwhile, the various members of Sleary`s company gradually gathered together from the upper "
            + "regions, where they were quartered, and, from standing about, talking in low voices to one another "
            + "and to Mr. Childers, gradually insinuated themselves and him into the room.  There were two or three "
            + "handsome young women among them, with their two or three husbands, and their two or three mothers, "
            + "and their eight or nine little children, who did the fairy business when required.  "
            + "The father of one of the families was in the habit of balancing the father of another of the families "
            + "on the top of a great pole; the father of a third family often made a pyramid of both those fathers, "
            + "with Master Kidderminster for the apex, and himself for the base; all the fathers could dance upon "
            + "rolling casks, stand upon bottles, catch knives and balls, twirl hand-basins, ride upon anything, "
            + "jump over everything, and stick at nothing.  All the mothers could (and did) dance, upon the slack "
            + "wire and the tight-rope, and perform rapid acts on bare-backed steeds; none of them were at all "
            + "particular in respect of showing their legs; and one of them, alone in a Greek chariot, drove six "
            + "in hand into every town they came to.  They all assumed to be mighty rakish and knowing, they were "
            + "not very tidy in their private dresses, they were not at all orderly in their domestic arrangements, "
            + "and the combined literature of the whole company would have produced but a poor letter on any subject.  "
            + "Yet there was a remarkable gentleness and childishness about these people, a special inaptitude "
            + "for any kind of sharp practice, and an untiring readiness to help and pity one another, deserving "
            + "often of as much respect, and always of as much generous construction, as the every-day virtues of "
            + "any class of people in the world.";

    private CharsetTestCommon() {
    }

    static void runEncode(String hex, String text, Charset charset, int inSize, int outSize) {
        char[] input = text.toCharArray();
        byte[] output = new byte[16384];
        int inPos = 0;
        int outPos = 0;
        CharsetEncoder encoder = charset.newEncoder();
        CoderResult result;

        do {
            int inLen = Math.min(inSize, input.length - inPos);
            CharBuffer in = CharBuffer.wrap(input, inPos, inLen);
            int outLen = Math.min(outSize, output.length - outPos);
            ByteBuffer out = ByteBuffer.wrap(output, outPos, outLen);
            result = encoder.encode(in, out, inPos + inLen >= input.length);
            inPos = in.position();
            outPos = out.position();
        } while (!result.isError() && inPos < input.length);

        assertEquals(CoderResult.UNDERFLOW, result, "Should be UNDERFLOW after encoding");

        do {
            int outLen = Math.min(outSize, output.length - outPos);
            ByteBuffer out = ByteBuffer.wrap(output, outPos, outLen);
            result = encoder.flush(out);
            outPos = out.position();
        } while (!result.isUnderflow());

        assertEquals(CoderResult.UNDERFLOW, result, "Should be UNDERFLOW after flushing");
        output = Arrays.copyOf(output, outPos);
        assertEquals(hex, bytesToHex(output));
    }

    static void runDecode(String hex, String text, Charset charset, int inSize, int outSize) {
        byte[] input = hexToBytes(hex);
        char[] output = new char[16384];
        int inPos = 0;
        int outPos = 0;
        CharsetDecoder decoder = charset.newDecoder();
        CoderResult result;

        do {
            int inLen = Math.min(inSize, input.length - inPos);
            ByteBuffer in = ByteBuffer.wrap(input, inPos, inLen);
            int outLen = Math.min(outSize, output.length - outPos);
            CharBuffer out = CharBuffer.wrap(output, outPos, outLen);
            result = decoder.decode(in, out, inPos + inLen >= input.length);
            inPos = in.position();
            outPos = out.position();
        } while (!result.isError() && inPos < input.length);

        assertEquals(CoderResult.UNDERFLOW, result, "Should be UNDERFLOW after encoding");

        do {
            int outLen = Math.min(outSize, output.length - outPos);
            CharBuffer out = CharBuffer.wrap(output, outPos, outLen);
            result = decoder.flush(out);
            outPos = out.position();
        } while (!result.isUnderflow());

        assertEquals(CoderResult.UNDERFLOW, result, "Should be UNDERFLOW after flushing");
        output = Arrays.copyOf(output, outPos);
        assertEquals(text, new String(output));
    }

    static String bytesToHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        int j = 0;
        for (int i = 0; i < bytes.length; ++i) {
            int b = bytes[i] & 0xFF;
            result[j++] = hexDigits[b >> 4];
            result[j++] = hexDigits[b & 0xF];
        }
        return new String(result);
    }

    static byte[] hexToBytes(String hex) {
        char[] chars = hex.toCharArray();
        byte[] result = new byte[chars.length / 2];
        int j = 0;
        for (int i = 0; i < chars.length; i += 2) {
            char hi = chars[i];
            char lo = chars[i + 1];
            result[j++] = (byte) ((digit(hi) << 4) | digit(lo));
        }
        return result;
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        return c - 'A' + 10;
    }

    static void checkUnmappable(Charset charset, String text, int length) {
        CharBuffer input = CharBuffer.wrap(text);
        byte[] result = new byte[100];
        ByteBuffer output = ByteBuffer.wrap(result);
        CoderResult coderResult = charset.newEncoder().encode(input, output, true);
        assertEquals(CoderResult.unmappableForLength(length), coderResult);
        assertEquals(0, input.position());
        assertEquals(0, output.position());
    }

    static void checkMalformed(Charset charset, String text, int length) {
        CharBuffer input = CharBuffer.wrap(text);
        byte[] result = new byte[100];
        ByteBuffer output = ByteBuffer.wrap(result);
        CoderResult coderResult = charset.newEncoder().encode(input, output, true);
        assertEquals(CoderResult.malformedForLength(length), coderResult);
        assertEquals(0, input.position());
        assertEquals(0, output.position());
    }

    static void checkMalformed(Charset charset, byte[] data, int length) {
        ByteBuffer input = ByteBuffer.wrap(data);
        CharBuffer output = CharBuffer.wrap(new char[100]);
        CoderResult coderResult = charset.newDecoder().decode(input, output, true);
        assertEquals(CoderResult.malformedForLength(length), coderResult);
        assertEquals(0, input.position());
        assertEquals(0, output.position());
    }

    // The checks below feed the same input to a coder in several ways. CharsetDecoder.decode(ByteBuffer, CharBuffer,
    // boolean) says that "care should be taken to preserve any bytes remaining in the input buffer so that they are
    // available to the next invocation", and CharsetEncoder.encode has the same rule for characters. So the result
    // must not depend on how the input is split into chunks, or on how small the output buffer is. TeaVM's coders
    // also copy their input into a 512-element array, so the checks with a prefix place the input across the end of
    // that array, in a single call with endOfInput set.

    /** Characters of each UTF-8 length and both halves of a surrogate pair, at the edges of their ranges. */
    static final int[] CHARS = { 'a', 0xE9, 0x100, 0x20AC, 0xD800, 0xDBFF, 0xDC00, 0xDFFF };

    static CharsetDecoder replacingDecoder(Charset charset) {
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    static CharsetEncoder replacingEncoder(Charset charset) {
        return charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    static void checkDecode(CharsetDecoder decoder, byte[] input, String expected) {
        String context = bytesToHex(input);
        assertEquals(escape(expected), escape(decodeAtOnce(decoder, input)), "decoded at once: " + context);
        assertEquals(escape(expected), escape(decodeByteByByte(decoder, input)), "decoded byte by byte: " + context);
        assertEquals(escape(expected), escape(decodeToSmallBuffer(decoder, input)),
                "decoded to a small buffer: " + context);
    }

    /** Decodes the input after 508 to 512 bytes of 0x61 (one char per bytesPerPrefixChar bytes), in one call. */
    static void checkDecodeAfterPrefix(CharsetDecoder decoder, byte[] input, String expected,
            int bytesPerPrefixChar) {
        for (int length = 508; length <= 512; length += bytesPerPrefixChar) {
            byte[] data = new byte[length + input.length];
            Arrays.fill(data, 0, length, (byte) 'a');
            System.arraycopy(input, 0, data, length, input.length);
            String actual = decodeAtOnce(decoder, data);
            int start = Math.min(length / bytesPerPrefixChar, actual.length());
            assertEquals(escape(expected), escape(actual.substring(start)),
                    bytesToHex(input) + " after " + length + " bytes");
        }
    }

    private static String decodeAtOnce(CharsetDecoder decoder, byte[] input) {
        decoder.reset();
        ByteBuffer in = ByteBuffer.wrap(input);
        CharBuffer out = CharBuffer.allocate(input.length + 4);
        assertEquals(CoderResult.UNDERFLOW, decoder.decode(in, out, true));
        assertEquals(CoderResult.UNDERFLOW, decoder.flush(out));
        out.flip();
        return out.toString();
    }

    private static String decodeByteByByte(CharsetDecoder decoder, byte[] input) {
        decoder.reset();
        ByteBuffer in = ByteBuffer.wrap(input);
        CharBuffer out = CharBuffer.allocate(input.length + 4);
        for (int i = 0; i <= input.length; ++i) {
            in.limit(i);
            assertEquals(CoderResult.UNDERFLOW, decoder.decode(in, out, false));
        }
        assertEquals(CoderResult.UNDERFLOW, decoder.decode(in, out, true));
        assertEquals(CoderResult.UNDERFLOW, decoder.flush(out));
        out.flip();
        return out.toString();
    }

    private static String decodeToSmallBuffer(CharsetDecoder decoder, byte[] input) {
        decoder.reset();
        ByteBuffer in = ByteBuffer.wrap(input);
        CharBuffer out = CharBuffer.allocate(2);
        StringBuilder sb = new StringBuilder();
        while (true) {
            int start = in.position();
            CoderResult result = decoder.decode(in, out, true);
            boolean progress = in.position() > start || out.position() > 0;
            out.flip();
            sb.append(out);
            out.clear();
            if (result.isUnderflow()) {
                break;
            }
            assertEquals(CoderResult.OVERFLOW, result);
            assertTrue(progress, "no progress decoding " + bytesToHex(input));
        }
        assertEquals(CoderResult.UNDERFLOW, decoder.flush(out));
        return sb.toString();
    }

    static void checkEncode(CharsetEncoder encoder, String input, byte[] expected) {
        String context = escape(input);
        assertEquals(bytesToHex(expected), bytesToHex(encodeAtOnce(encoder, input)), "encoded at once: " + context);
        assertEquals(bytesToHex(expected), bytesToHex(encodeCharByChar(encoder, input)),
                "encoded char by char: " + context);
        assertEquals(bytesToHex(expected), bytesToHex(encodeToSmallBuffer(encoder, input)),
                "encoded to a small buffer: " + context);
    }

    /** Encodes the input after 508 to 512 ASCII characters, in one call. */
    static void checkEncodeAfterPrefix(CharsetEncoder encoder, String input, byte[] expected, int bytesPerPrefixChar) {
        for (int length = 508; length <= 512; ++length) {
            char[] prefix = new char[length];
            Arrays.fill(prefix, 'a');
            byte[] actual = encodeAtOnce(encoder, new String(prefix) + input);
            int start = Math.min(length * bytesPerPrefixChar, actual.length);
            assertEquals(bytesToHex(expected), bytesToHex(Arrays.copyOfRange(actual, start, actual.length)),
                    escape(input) + " after " + length + " chars");
        }
    }

    private static byte[] encodeAtOnce(CharsetEncoder encoder, String input) {
        encoder.reset();
        CharBuffer in = CharBuffer.wrap(input);
        ByteBuffer out = ByteBuffer.allocate(input.length() * 4 + 4);
        assertEquals(CoderResult.UNDERFLOW, encoder.encode(in, out, true));
        assertEquals(CoderResult.UNDERFLOW, encoder.flush(out));
        return Arrays.copyOf(out.array(), out.position());
    }

    private static byte[] encodeCharByChar(CharsetEncoder encoder, String input) {
        encoder.reset();
        CharBuffer in = CharBuffer.wrap(input);
        ByteBuffer out = ByteBuffer.allocate(input.length() * 4 + 4);
        for (int i = 0; i <= input.length(); ++i) {
            in.limit(i);
            assertEquals(CoderResult.UNDERFLOW, encoder.encode(in, out, false));
        }
        assertEquals(CoderResult.UNDERFLOW, encoder.encode(in, out, true));
        assertEquals(CoderResult.UNDERFLOW, encoder.flush(out));
        return Arrays.copyOf(out.array(), out.position());
    }

    private static byte[] encodeToSmallBuffer(CharsetEncoder encoder, String input) {
        encoder.reset();
        CharBuffer in = CharBuffer.wrap(input);
        ByteBuffer out = ByteBuffer.allocate(4);
        byte[] result = new byte[0];
        while (true) {
            int inStart = in.position();
            CoderResult coderResult = encoder.encode(in, out, true);
            assertTrue(coderResult.isUnderflow() || in.position() > inStart || out.position() > 0,
                    "no progress encoding " + escape(input));
            int start = result.length;
            result = Arrays.copyOf(result, start + out.position());
            System.arraycopy(out.array(), 0, result, start, out.position());
            out.clear();
            if (coderResult.isUnderflow()) {
                break;
            }
            assertEquals(CoderResult.OVERFLOW, coderResult);
        }
        assertEquals(CoderResult.UNDERFLOW, encoder.flush(out));
        return result;
    }

    /** Calls the action with every sequence of 1 to maxLength elements of the alphabet. */
    static void forEachSequence(int[] alphabet, int maxLength, Consumer<int[]> action) {
        for (int length = 1; length <= maxLength; ++length) {
            int[] indexes = new int[length];
            int[] sequence = new int[length];
            while (true) {
                for (int i = 0; i < length; ++i) {
                    sequence[i] = alphabet[indexes[i]];
                }
                action.accept(sequence);
                int i = length - 1;
                while (i >= 0 && ++indexes[i] == alphabet.length) {
                    indexes[i--] = 0;
                }
                if (i < 0) {
                    break;
                }
            }
        }
    }

    static byte[] toBytes(int[] values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; ++i) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    static String toChars(int[] values) {
        char[] result = new char[values.length];
        for (int i = 0; i < values.length; ++i) {
            result[i] = (char) values[i];
        }
        return new String(result);
    }

    /**
     * Encodes as the JDK does with the '?' replacement: a surrogate pair is one code point, any other surrogate is
     * malformed, and a code point at or above the limit is unmappable.
     */
    static byte[] referenceEncode(String input, int limit, boolean utf8) {
        byte[] result = new byte[input.length() * 4];
        int length = 0;
        for (int i = 0; i < input.length(); ++i) {
            int c = input.charAt(i);
            if (Character.isHighSurrogate((char) c) && i + 1 < input.length()
                    && Character.isLowSurrogate(input.charAt(i + 1))) {
                c = Character.toCodePoint((char) c, input.charAt(++i));
            } else if (Character.isSurrogate((char) c)) {
                result[length++] = '?';
                continue;
            }
            if (c >= limit) {
                result[length++] = '?';
            } else if (!utf8 || c < 0x80) {
                result[length++] = (byte) c;
            } else if (c < 0x800) {
                result[length++] = (byte) (0xC0 | (c >> 6));
                result[length++] = (byte) (0x80 | (c & 0x3F));
            } else if (c < 0x10000) {
                result[length++] = (byte) (0xE0 | (c >> 12));
                result[length++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                result[length++] = (byte) (0x80 | (c & 0x3F));
            } else {
                result[length++] = (byte) (0xF0 | (c >> 18));
                result[length++] = (byte) (0x80 | ((c >> 12) & 0x3F));
                result[length++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                result[length++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return Arrays.copyOf(result, length);
    }

    /**
     * Encodes UTF-16 as the JDK does (sun.nio.cs.UnicodeEncoder): a surrogate that is not part of a pair is
     * malformed, and the replacement is U+FFFD in the byte order of the charset.
     */
    static byte[] referenceEncodeUtf16(String input, boolean littleEndian) {
        byte[] result = new byte[input.length() * 2];
        int length = 0;
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < input.length()
                    && Character.isLowSurrogate(input.charAt(i + 1))) {
                length = putUtf16Unit(result, length, c, littleEndian);
                c = input.charAt(++i);
            } else if (Character.isSurrogate(c)) {
                c = '�';
            }
            length = putUtf16Unit(result, length, c, littleEndian);
        }
        return Arrays.copyOf(result, length);
    }

    private static int putUtf16Unit(byte[] output, int index, char c, boolean littleEndian) {
        output[index++] = (byte) (littleEndian ? c : c >> 8);
        output[index++] = (byte) (littleEndian ? c >> 8 : c);
        return index;
    }

    /**
     * Decodes UTF-8 as the JDK does (sun.nio.cs.UTF_8): each maximal subpart of an ill-formed sequence becomes one
     * U+FFFD, as the Unicode Standard recommends (chapter 3, "U+FFFD Substitution of Maximal Subparts"). So overlong
     * forms and code points above U+10FFFF are malformed. One difference: the JDK takes any continuation byte after
     * ED, and rejects the encoded surrogate only after the third byte, so ED A0 80 is a single malformed sequence.
     */
    static String referenceDecodeUtf8(byte[] input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length) {
            int b = input[i] & 0xFF;
            int trailing;
            int secondMin = 0x80;
            int secondMax = 0xBF;
            if (b < 0x80) {
                sb.append((char) b);
                ++i;
                continue;
            } else if (b >= 0xC2 && b <= 0xDF) {
                trailing = 1;
            } else if (b >= 0xE0 && b <= 0xEF) {
                trailing = 2;
                if (b == 0xE0) {
                    secondMin = 0xA0;
                }
            } else if (b >= 0xF0 && b <= 0xF4) {
                trailing = 3;
                if (b == 0xF0) {
                    secondMin = 0x90;
                } else if (b == 0xF4) {
                    secondMax = 0x8F;
                }
            } else {
                sb.append('�');
                ++i;
                continue;
            }
            int code = b & (0x3F >> trailing);
            int read = 0;
            while (read < trailing && i + 1 + read < input.length) {
                int next = input[i + 1 + read] & 0xFF;
                if (next < (read == 0 ? secondMin : 0x80) || next > (read == 0 ? secondMax : 0xBF)) {
                    break;
                }
                code = (code << 6) | (next & 0x3F);
                ++read;
            }
            if (read < trailing || (code >= 0xD800 && code <= 0xDFFF)) {
                sb.append('�');
            } else {
                sb.appendCodePoint(code);
            }
            i += 1 + read;
        }
        return sb.toString();
    }

    /**
     * Decodes UTF-16 as the JDK does (sun.nio.cs.UnicodeDecoder): a high surrogate followed by a unit that is not
     * a low surrogate is one malformed sequence of four bytes, a lone low surrogate is malformed, and an incomplete
     * unit or pair at the end of the input becomes one U+FFFD.
     */
    static String referenceDecodeUtf16(byte[] input, boolean littleEndian) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length) {
            if (input.length - i < 2) {
                sb.append('�');
                break;
            }
            char c = utf16Unit(input, i, littleEndian);
            if (Character.isHighSurrogate(c)) {
                if (input.length - i < 4) {
                    sb.append('�');
                    break;
                }
                char next = utf16Unit(input, i + 2, littleEndian);
                if (Character.isLowSurrogate(next)) {
                    sb.append(c).append(next);
                } else {
                    sb.append('�');
                }
                i += 4;
            } else {
                sb.append(Character.isLowSurrogate(c) ? '�' : c);
                i += 2;
            }
        }
        return sb.toString();
    }

    private static char utf16Unit(byte[] input, int index, boolean littleEndian) {
        int first = input[index] & 0xFF;
        int second = input[index + 1] & 0xFF;
        return (char) (littleEndian ? first | (second << 8) : (first << 8) | second);
    }

    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= 0x20 && c < 0x7F) {
                sb.append(c);
            } else {
                sb.append("\\u").append(hexDigits[c >> 12]).append(hexDigits[(c >> 8) & 0xF])
                        .append(hexDigits[(c >> 4) & 0xF]).append(hexDigits[c & 0xF]);
            }
        }
        return sb.toString();
    }
}
