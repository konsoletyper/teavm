/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha1Repro {
    private Sha1Repro() {
    }

    public static void main(String[] args) throws Exception {
        checkVector(new byte[0], "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709", "empty message");
        checkVector(bytes("abc"), "A9993E364706816ABA3E25717850C26C9CD0D89D", "abc");
        checkVector(bytes("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
                "84983E441C3BD26EBAAE4AA1F95129E5E54670F1", "multi-block RFC vector");
        testIncrementalAndReset();
        testOutputAndBounds();
        testCloneIndependence();
        testNamesAndUnsupportedAlgorithms();
        System.out.println("SHA-1 contract passed");
    }

    private static void checkVector(byte[] input, String expected, String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        check(hex(digest.digest(input)).equals(expected), name + " vector");
        check(digest.getDigestLength() == 20, "SHA-1 digest length");
    }

    private static void testIncrementalAndReset() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update((byte) 'a');
        digest.update(bytes("b"));
        digest.update(bytes("c"), 0, 1);
        String abc = "A9993E364706816ABA3E25717850C26C9CD0D89D";
        check(hex(digest.digest()).equals(abc), "incremental update");
        check(hex(digest.digest()).equals("DA39A3EE5E6B4B0D3255BFEF95601890AFD80709"), "digest resets");

        digest.update(bytes("discard"));
        digest.reset();
        ByteBuffer input = ByteBuffer.wrap(bytes("abc"));
        digest.update(input);
        check(!input.hasRemaining(), "ByteBuffer update consumes input");
        check(hex(digest.digest()).equals(abc), "reset and ByteBuffer update");
    }

    private static void testOutputAndBounds() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] output = new byte[22];
        output[0] = 0x55;
        output[21] = 0x55;
        digest.update(bytes("abc"));
        int written = digest.digest(output, 1, 20);
        check(written == 20, "digest output length");
        check(output[0] == 0x55 && output[21] == 0x55, "output bounds remain intact");

        digest.update(bytes("abc"));
        try {
            digest.digest(new byte[19], 0, 19);
            throw new AssertionError("short output buffer should throw DigestException");
        } catch (DigestException expected) {
            // Expected.
        }
        try {
            digest.update(bytes("abc"), -1, 1);
            throw new AssertionError("invalid update range should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // Expected.
        }
    }

    private static void testCloneIndependence() throws Exception {
        MessageDigest original = MessageDigest.getInstance("SHA-1");
        original.update((byte) 'a');
        MessageDigest copy = (MessageDigest) original.clone();
        original.update(bytes("bc"));
        copy.update(bytes("bd"));
        check(hex(original.digest()).equals("A9993E364706816ABA3E25717850C26C9CD0D89D"), "original clone state");
        check(hex(copy.digest()).equals("CB4CC28DF0FDBE0ECF9D9662E294B118092A5735"), "cloned digest state");
    }

    private static void testNamesAndUnsupportedAlgorithms() throws Exception {
        check(hex(MessageDigest.getInstance("sha-1").digest(bytes("abc")))
                .equals("A9993E364706816ABA3E25717850C26C9CD0D89D"), "case-insensitive canonical name");
        check(hex(MessageDigest.getInstance("SHA").digest(bytes("abc")))
                .equals("A9993E364706816ABA3E25717850C26C9CD0D89D"), "SHA alias");
        try {
            MessageDigest.getInstance("TEAVM-UNKNOWN-DIGEST");
            throw new AssertionError("unsupported algorithms should throw NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException expected) {
            // Expected.
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String hex(byte[] bytes) {
        char[] digits = "0123456789ABCDEF".toCharArray();
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; ++i) {
            int value = bytes[i] & 0xFF;
            result[i * 2] = digits[value >>> 4];
            result[i * 2 + 1] = digits[value & 0xF];
        }
        return new String(result);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
