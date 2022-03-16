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

import static org.junit.Assert.assertEquals;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;

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

        assertEquals("Should be UNDERFLOW after encoding", CoderResult.UNDERFLOW, result);

        do {
            int outLen = Math.min(outSize, output.length - outPos);
            ByteBuffer out = ByteBuffer.wrap(output, outPos, outLen);
            result = encoder.flush(out);
            outPos = out.position();
        } while (!result.isUnderflow());

        assertEquals("Should be UNDERFLOW after flushing", CoderResult.UNDERFLOW, result);
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

        assertEquals("Should be UNDERFLOW after encoding", CoderResult.UNDERFLOW, result);

        do {
            int outLen = Math.min(outSize, output.length - outPos);
            CharBuffer out = CharBuffer.wrap(output, outPos, outLen);
            result = decoder.flush(out);
            outPos = out.position();
        } while (!result.isUnderflow());

        assertEquals("Should be UNDERFLOW after flushing", CoderResult.UNDERFLOW, result);
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
}
