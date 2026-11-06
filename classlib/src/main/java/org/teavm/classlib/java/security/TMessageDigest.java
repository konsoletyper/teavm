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
package org.teavm.classlib.java.security;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import org.teavm.classlib.java.lang.TCloneNotSupportedException;
import org.teavm.classlib.java.lang.TCloneable;

public abstract class TMessageDigest implements TCloneable {
    private final String algorithm;

    protected TMessageDigest(String algorithm) {
        this.algorithm = Objects.requireNonNull(algorithm);
    }

    public static TMessageDigest getInstance(String algorithm) throws TNoSuchAlgorithmException {
        Objects.requireNonNull(algorithm);
        if (algorithm.equalsIgnoreCase("SHA-1") || algorithm.equalsIgnoreCase("SHA")) {
            return new Sha1MessageDigest(algorithm);
        }
        throw new TNoSuchAlgorithmException("Unsupported message digest algorithm: " + algorithm);
    }

    public final String getAlgorithm() {
        return algorithm;
    }

    public final int getDigestLength() {
        return engineGetDigestLength();
    }

    public final void update(byte input) {
        engineUpdate(input);
    }

    public final void update(byte[] input) {
        Objects.requireNonNull(input);
        update(input, 0, input.length);
    }

    public final void update(byte[] input, int offset, int length) {
        Objects.requireNonNull(input);
        if (offset < 0 || length < 0 || offset > input.length - length) {
            throw new IndexOutOfBoundsException();
        }
        engineUpdate(input, offset, length);
    }

    public final void update(ByteBuffer input) {
        Objects.requireNonNull(input);
        while (input.hasRemaining()) {
            update(input.get());
        }
    }

    public final byte[] digest() {
        return engineDigest();
    }

    public final byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    public final int digest(byte[] output, int offset, int length) throws TDigestException {
        Objects.requireNonNull(output);
        if (offset < 0 || length < 0 || offset > output.length - length) {
            throw new IllegalArgumentException();
        }
        int digestLength = getDigestLength();
        if (length < digestLength) {
            throw new TDigestException("Output buffer too small for digest");
        }
        byte[] value = digest();
        System.arraycopy(value, 0, output, offset, digestLength);
        return digestLength;
    }

    public final void reset() {
        engineReset();
    }

    public static boolean isEqual(byte[] digestA, byte[] digestB) {
        if (digestA == digestB) {
            return true;
        }
        if (digestA == null || digestB == null) {
            return false;
        }
        int result = digestA.length ^ digestB.length;
        int length = Math.max(digestA.length, digestB.length);
        for (int i = 0; i < length; ++i) {
            int a = i < digestA.length ? digestA[i] & 0xFF : 0;
            int b = i < digestB.length ? digestB[i] & 0xFF : 0;
            result |= a ^ b;
        }
        return result == 0;
    }

    @Override
    public Object clone() throws TCloneNotSupportedException {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new TCloneNotSupportedException(e.getMessage());
        }
    }

    protected abstract void engineUpdate(byte input);

    protected abstract void engineUpdate(byte[] input, int offset, int length);

    protected abstract byte[] engineDigest();

    protected abstract void engineReset();

    protected abstract int engineGetDigestLength();

    private static final class Sha1MessageDigest extends TMessageDigest {
        private static final int BLOCK_SIZE = 64;
        private int[] state = new int[5];
        private byte[] block = new byte[BLOCK_SIZE];
        private int[] schedule = new int[80];
        private int blockLength;
        private long byteCount;

        Sha1MessageDigest(String algorithm) {
            super(algorithm);
            engineReset();
        }

        @Override
        protected void engineUpdate(byte input) {
            block[blockLength++] = input;
            ++byteCount;
            if (blockLength == BLOCK_SIZE) {
                processBlock();
                blockLength = 0;
            }
        }

        @Override
        protected void engineUpdate(byte[] input, int offset, int length) {
            byteCount += length;
            while (length > 0) {
                int chunk = Math.min(length, BLOCK_SIZE - blockLength);
                System.arraycopy(input, offset, block, blockLength, chunk);
                offset += chunk;
                length -= chunk;
                blockLength += chunk;
                if (blockLength == BLOCK_SIZE) {
                    processBlock();
                    blockLength = 0;
                }
            }
        }

        @Override
        protected byte[] engineDigest() {
            long bitLength = byteCount << 3;
            block[blockLength++] = (byte) 0x80;
            if (blockLength > 56) {
                Arrays.fill(block, blockLength, BLOCK_SIZE, (byte) 0);
                processBlock();
                blockLength = 0;
            }
            Arrays.fill(block, blockLength, 56, (byte) 0);
            for (int i = 0; i < 8; ++i) {
                block[63 - i] = (byte) (bitLength >>> (i * 8));
            }
            processBlock();

            byte[] digest = new byte[20];
            for (int i = 0; i < state.length; ++i) {
                int word = state[i];
                digest[i * 4] = (byte) (word >>> 24);
                digest[i * 4 + 1] = (byte) (word >>> 16);
                digest[i * 4 + 2] = (byte) (word >>> 8);
                digest[i * 4 + 3] = (byte) word;
            }
            engineReset();
            return digest;
        }

        @Override
        protected void engineReset() {
            state[0] = 0x67452301;
            state[1] = 0xEFCDAB89;
            state[2] = 0x98BADCFE;
            state[3] = 0x10325476;
            state[4] = 0xC3D2E1F0;
            blockLength = 0;
            byteCount = 0;
        }

        @Override
        protected int engineGetDigestLength() {
            return 20;
        }

        private void processBlock() {
            for (int i = 0; i < 16; ++i) {
                int offset = i * 4;
                schedule[i] = ((block[offset] & 0xFF) << 24)
                        | ((block[offset + 1] & 0xFF) << 16)
                        | ((block[offset + 2] & 0xFF) << 8)
                        | (block[offset + 3] & 0xFF);
            }
            for (int i = 16; i < schedule.length; ++i) {
                schedule[i] = Integer.rotateLeft(schedule[i - 3] ^ schedule[i - 8]
                        ^ schedule[i - 14] ^ schedule[i - 16], 1);
            }

            int a = state[0];
            int b = state[1];
            int c = state[2];
            int d = state[3];
            int e = state[4];
            for (int i = 0; i < schedule.length; ++i) {
                int function;
                int constant;
                if (i < 20) {
                    function = (b & c) | (~b & d);
                    constant = 0x5A827999;
                } else if (i < 40) {
                    function = b ^ c ^ d;
                    constant = 0x6ED9EBA1;
                } else if (i < 60) {
                    function = (b & c) | (b & d) | (c & d);
                    constant = 0x8F1BBCDC;
                } else {
                    function = b ^ c ^ d;
                    constant = 0xCA62C1D6;
                }
                int next = Integer.rotateLeft(a, 5) + function + e + constant + schedule[i];
                e = d;
                d = c;
                c = Integer.rotateLeft(b, 30);
                b = a;
                a = next;
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
            state[4] += e;
        }

        @Override
        public Object clone() throws TCloneNotSupportedException {
            Sha1MessageDigest copy = (Sha1MessageDigest) super.clone();
            copy.state = state.clone();
            copy.block = block.clone();
            copy.schedule = schedule.clone();
            return copy;
        }
    }
}
