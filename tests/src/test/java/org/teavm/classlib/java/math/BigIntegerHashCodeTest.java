/*
 *  Copyright 2014 Alexey Andreev.
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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Elena Semukhina
 */

package org.teavm.classlib.java.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class BigIntegerHashCodeTest {
    /**
     * Test hash codes for the same object
     */
    @Test
    public void testSameObject() {
        String value1 = "12378246728727834290276457386374882976782849";
        String value2 = "-5634562095872038262928728727834290276457386374882976782849";
        BigInteger aNumber1 = new BigInteger(value1);
        BigInteger aNumber2 = new BigInteger(value2);
        int code1 = aNumber1.hashCode();
        aNumber1.add(aNumber2).shiftLeft(125);
        aNumber1.subtract(aNumber2).shiftRight(125);
        aNumber1.multiply(aNumber2).toByteArray();
        aNumber1.divide(aNumber2).bitLength();
        aNumber1.gcd(aNumber2).pow(7);
        int code2 = aNumber1.hashCode();
        assertTrue(code1 == code2, "hash codes for the same object differ");
    }

    /**
     * Test hash codes for equal objects.
     */
    @Test
    public void testEqualObjects() {
        String value1 = "12378246728727834290276457386374882976782849";
        String value2 = "12378246728727834290276457386374882976782849";
        BigInteger aNumber1 = new BigInteger(value1);
        BigInteger aNumber2 = new BigInteger(value2);
        int code1 = aNumber1.hashCode();
        int code2 = aNumber2.hashCode();
        if (aNumber1.equals(aNumber2)) {
            assertTrue(code1 == code2, "hash codes for equal objects are unequal");
        }
    }

    /**
     * Test hash codes for unequal objects.
     * The codes are unequal.
     */
    @Test
    public void testUnequalObjectsUnequal() {
        String value1 = "12378246728727834290276457386374882976782849";
        String value2 = "-5634562095872038262928728727834290276457386374882976782849";
        BigInteger aNumber1 = new BigInteger(value1);
        BigInteger aNumber2 = new BigInteger(value2);
        int code1 = aNumber1.hashCode();
        int code2 = aNumber2.hashCode();
        if (!aNumber1.equals(aNumber2)) {
            assertTrue(code1 != code2, "hash codes for unequal objects are equal");
        }
    }

    @Test
    public void equalValuesBuiltDifferentlyHashAlike() {
        BigInteger p = new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853951");
        BigInteger[] equal = {
                new BigInteger(p.toString(16), 16),
                new BigInteger(1, p.toByteArray()),
                BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE.shiftLeft(224)).add(BigInteger.ONE.shiftLeft(192))
                        .add(BigInteger.ONE.shiftLeft(96)).subtract(BigInteger.ONE),
                p.add(BigInteger.TEN).subtract(BigInteger.TEN)
        };
        for (BigInteger x : equal) {
            assertEquals(p, x);
            assertEquals(p.hashCode(), x.hashCode());
        }
        BigInteger twoTo100 = BigInteger.ONE.shiftLeft(100);
        assertEquals(3, twoTo100.add(BigInteger.valueOf(3)).subtract(twoTo100).hashCode());
        assertEquals(0, twoTo100.subtract(twoTo100).hashCode());
    }

    @Test
    public void hashCodeIsTheJdkOne() {
        assertEquals(-1, BigInteger.ONE.negate().hashCode());
        assertEquals(16 * 31 * 31 * 31, BigInteger.ONE.shiftLeft(100).hashCode());
        assertEquals(31 * 7 + 5, new BigInteger("30064771077").hashCode());
    }

    // Object.hashCode: "If two objects are equal according to the equals method, then calling the hashCode
    // method on each of the two objects must produce the same integer result."
    @Test
    public void equalValuesHashAlikeWhateverTheirHistory() {
        Random random = new Random(44);
        BigInteger big = BigInteger.ONE.shiftLeft(500).subtract(BigInteger.ONE);
        for (int i = 0; i < 300; i++) {
            BigInteger x = randomValue(random);
            BigInteger y = new BigInteger(1 + random.nextInt(200), random).add(BigInteger.ONE);
            int k = random.nextInt(200);
            BigInteger[] equal = {
                    new BigInteger(x.toString()),
                    new BigInteger(x.toString(16), 16),
                    new BigInteger(x.toByteArray()),
                    x.add(big).subtract(big),
                    big.add(x).subtract(big),
                    x.subtract(big).add(big),
                    x.multiply(y).divide(y),
                    x.multiply(big).divide(big),
                    x.shiftLeft(k).shiftRight(k),
                    x.xor(big).xor(big),
                    x.or(big).and(x),
                    x.negate().negate(),
                    x.not().not(),
                    x.multiply(y).add(x).divide(y.add(BigInteger.ONE)),
                    big.multiply(big).add(x).mod(big.multiply(big)).subtract(
                            x.signum() < 0 ? big.multiply(big) : BigInteger.ZERO)
            };
            for (BigInteger e : equal) {
                assertEquals(x, e);
                assertEquals(x.hashCode(), e.hashCode(), "hash code of " + x);
            }
        }
    }

    @Test
    public void hashCodeFollowsTheJdkFormula() {
        Random random = new Random(45);
        assertEquals(0, BigInteger.ZERO.hashCode());
        for (int i = 0; i < 1000; i++) {
            BigInteger x = randomValue(random);
            assertEquals(jdkHashCode(x), x.hashCode(), "hash code of " + x);
            BigInteger fromArithmetic = x.add(BigInteger.ONE.shiftLeft(400)).subtract(BigInteger.ONE.shiftLeft(400));
            assertEquals(jdkHashCode(x), fromArithmetic.hashCode(), "hash code of " + x);
        }
        for (int bits = 0; bits < 200; bits++) {
            for (BigInteger x : new BigInteger[] { BigInteger.ONE.shiftLeft(bits),
                    BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE) }) {
                assertEquals(jdkHashCode(x), x.hashCode(), "hash code of " + x);
                assertEquals(jdkHashCode(x.negate()), x.negate().hashCode(), "hash code of -" + x);
            }
        }
    }

    // BigDecimal.equals: "two BigDecimal objects equal only if they are equal in value and scale", so equal
    // BigDecimals with large unscaled values must hash alike too.
    @Test
    public void equalBigDecimalsHashAlike() {
        Random random = new Random(46);
        BigInteger big = BigInteger.ONE.shiftLeft(300);
        for (int i = 0; i < 200; i++) {
            BigInteger x = randomValue(random);
            int scale = random.nextInt(40) - 20;
            BigDecimal a = new BigDecimal(x, scale);
            BigDecimal b = new BigDecimal(x.add(big).subtract(big), scale);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode(), "hash code of " + a);
        }
    }

    private static BigInteger randomValue(Random random) {
        BigInteger x = new BigInteger(random.nextInt(300), random);
        return random.nextBoolean() ? x.negate() : x;
    }

    // An independent reference: hash the 32-bit words of the magnitude from the most significant one with
    // h = 31 * h + word, then multiply by the signum. On the JVM this checks the reference against the JDK.
    private static int jdkHashCode(BigInteger x) {
        byte[] bytes = x.abs().toByteArray();
        int h = 0;
        int word = 0;
        int pad = (4 - bytes.length % 4) % 4;
        for (int i = 0; i < bytes.length; i++) {
            word = (word << 8) | (bytes[i] & 0xFF);
            if ((i + pad) % 4 == 3) {
                h = 31 * h + word;
                word = 0;
            }
        }
        return h * x.signum();
    }
}
