/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collections;

import org.junit.Test;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

public class TestDateTimes_implementation {

    @SuppressWarnings("rawtypes")
    @Test
    public void test_constructor() throws Exception {

        for (Constructor constructor : TJdk8Methods.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            constructor.newInstance(Collections.nCopies(constructor.getParameterTypes().length, null).toArray());
        }
    }

    Object[][] safeAddIntProvider() {

        return new Object[][] { { Integer.MIN_VALUE, 1, Integer.MIN_VALUE + 1 }, { -1, 1, 0 }, { 0, 0, 0 },
        { 1, -1, 0 }, { Integer.MAX_VALUE, -1, Integer.MAX_VALUE - 1 }, };
    }

    @Test
    public void test_safeAddInt() {

        for (Object[] data : safeAddIntProvider()) {
            int a = (int) data[0];
            int b = (int) data[1];
            int expected = (int) data[2];

            assertEquals(TJdk8Methods.safeAdd(a, b), expected);
        }
    }

    Object[][] safeAddIntProviderOverflow() {

        return new Object[][] { { Integer.MIN_VALUE, -1 }, { Integer.MIN_VALUE + 1, -2 }, { Integer.MAX_VALUE - 1, 2 },
        { Integer.MAX_VALUE, 1 }, };
    }

    @Test
    public void test_safeAddInt_overflow() {

        for (Object[] data : safeAddIntProviderOverflow()) {
            int a = (int) data[0];
            int b = (int) data[1];

            try {
                TJdk8Methods.safeAdd(a, b);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    Object[][] safeAddLongProvider() {

        return new Object[][] { { Long.MIN_VALUE, 1, Long.MIN_VALUE + 1 }, { -1, 1, 0 }, { 0, 0, 0 }, { 1, -1, 0 },
        { Long.MAX_VALUE, -1, Long.MAX_VALUE - 1 }, };
    }

    @Test
    public void test_safeAddLong() {

        for (Object[] data : safeAddLongProvider()) {
            long a = ((Number) data[0]).longValue();
            long b = ((Number) data[1]).longValue();
            long expected = ((Number) data[2]).longValue();

            assertEquals(TJdk8Methods.safeAdd(a, b), expected);
        }
    }

    Object[][] safeAddLongProviderOverflow() {

        return new Object[][] { { Long.MIN_VALUE, -1 }, { Long.MIN_VALUE + 1, -2 }, { Long.MAX_VALUE - 1, 2 },
        { Long.MAX_VALUE, 1 }, };
    }

    @Test
    public void test_safeAddLong_overflow() {

        for (Object[] data : safeAddLongProviderOverflow()) {
            long a = ((Number) data[0]).longValue();
            long b = ((Number) data[1]).longValue();

            try {
                TJdk8Methods.safeAdd(a, b);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    Object[][] safeSubtractIntProvider() {

        return new Object[][] { { Integer.MIN_VALUE, -1, Integer.MIN_VALUE + 1 }, { -1, -1, 0 }, { 0, 0, 0 },
        { 1, 1, 0 }, { Integer.MAX_VALUE, 1, Integer.MAX_VALUE - 1 }, };
    }

    @Test
    public void test_safeSubtractInt() {

        for (Object[] data : safeSubtractIntProvider()) {
            int a = (int) data[0];
            int b = (int) data[1];
            int expected = (int) data[2];

            assertEquals(TJdk8Methods.safeSubtract(a, b), expected);
        }
    }

    Object[][] safeSubtractIntProviderOverflow() {

        return new Object[][] { { Integer.MIN_VALUE, 1 }, { Integer.MIN_VALUE + 1, 2 }, { Integer.MAX_VALUE - 1, -2 },
        { Integer.MAX_VALUE, -1 }, };
    }

    @Test
    public void test_safeSubtractInt_overflow() {

        for (Object[] data : safeSubtractIntProviderOverflow()) {
            int a = (int) data[0];
            int b = (int) data[1];

            try {
                TJdk8Methods.safeSubtract(a, b);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    Object[][] safeSubtractLongProvider() {

        return new Object[][] { { Long.MIN_VALUE, -1, Long.MIN_VALUE + 1 }, { -1, -1, 0 }, { 0, 0, 0 }, { 1, 1, 0 },
        { Long.MAX_VALUE, 1, Long.MAX_VALUE - 1 }, };
    }

    @Test
    public void test_safeSubtractLong() {

        for (Object[] data : safeSubtractLongProvider()) {
            long a = ((Number) data[0]).longValue();
            long b = ((Number) data[1]).longValue();
            long expected = ((Number) data[2]).longValue();

            assertEquals(TJdk8Methods.safeSubtract(a, b), expected);
        }
    }

    Object[][] safeSubtractLongProviderOverflow() {

        return new Object[][] { { Long.MIN_VALUE, 1 }, { Long.MIN_VALUE + 1, 2 }, { Long.MAX_VALUE - 1, -2 },
        { Long.MAX_VALUE, -1 }, };
    }

    @Test
    public void test_safeSubtractLong_overflow() {

        for (Object[] data : safeSubtractLongProviderOverflow()) {
            long a = ((Number) data[0]).longValue();
            long b = ((Number) data[1]).longValue();

            try {
                TJdk8Methods.safeSubtract(a, b);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    Object[][] safeMultiplyIntProvider() {

        return new Object[][] { { Integer.MIN_VALUE, 1, Integer.MIN_VALUE },
        { Integer.MIN_VALUE / 2, 2, Integer.MIN_VALUE }, { -1, -1, 1 }, { -1, 1, -1 }, { 0, -1, 0 }, { 0, 0, 0 },
        { 0, 1, 0 }, { 1, -1, -1 }, { 1, 1, 1 }, { Integer.MAX_VALUE / 2, 2, Integer.MAX_VALUE - 1 },
        { Integer.MAX_VALUE, -1, Integer.MIN_VALUE + 1 }, };
    }

    @Test
    public void test_safeMultiplyInt() {

        for (Object[] data : safeMultiplyIntProvider()) {
            int a = (int) data[0];
            int b = (int) data[1];
            int expected = (int) data[2];

            assertEquals(TJdk8Methods.safeMultiply(a, b), expected);
        }
    }

    Object[][] safeMultiplyIntProviderOverflow() {

        return new Object[][] { { Integer.MIN_VALUE, 2 }, { Integer.MIN_VALUE / 2 - 1, 2 }, { Integer.MAX_VALUE, 2 },
        { Integer.MAX_VALUE / 2 + 1, 2 }, { Integer.MIN_VALUE, -1 }, { -1, Integer.MIN_VALUE }, };
    }

    @Test
    public void test_safeMultiplyInt_overflow() {

        for (Object[] data : safeMultiplyIntProviderOverflow()) {
            int a = (int) data[0];
            int b = (int) data[1];

            try {
                TJdk8Methods.safeMultiply(a, b);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    Object[][] safeMultiplyLongProvider() {

        return new Object[][] { { Long.MIN_VALUE, 1, Long.MIN_VALUE }, { Long.MIN_VALUE / 2, 2, Long.MIN_VALUE },
        { -1, -1, 1 }, { -1, 1, -1 }, { 0, -1, 0 }, { 0, 0, 0 }, { 0, 1, 0 }, { 1, -1, -1 }, { 1, 1, 1 },
        { Long.MAX_VALUE / 2, 2, Long.MAX_VALUE - 1 }, { Long.MAX_VALUE, -1, Long.MIN_VALUE + 1 },
        { -1, Integer.MIN_VALUE, -((long) Integer.MIN_VALUE) }, };
    }

    @Test
    public void test_safeMultiplyLong() {

        for (Object[] data : safeMultiplyLongProvider()) {
            long a = ((Number) data[0]).longValue();
            int b = (int) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(TJdk8Methods.safeMultiply(a, b), expected);
        }
    }

    Object[][] safeMultiplyLongProviderOverflow() {

        return new Object[][] { { Long.MIN_VALUE, 2 }, { Long.MIN_VALUE / 2 - 1, 2 }, { Long.MAX_VALUE, 2 },
        { Long.MAX_VALUE / 2 + 1, 2 }, { Long.MIN_VALUE, -1 }, };
    }

    @Test
    public void test_safeMultiplyLong_overflow() {

        for (Object[] data : safeMultiplyLongProviderOverflow()) {
            long a = ((Number) data[0]).longValue();
            int b = (int) data[1];

            try {
                TJdk8Methods.safeMultiply(a, b);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    Object[][] safeMultiplyLongLongProvider() {

        return new Object[][] { { Long.MIN_VALUE, 1, Long.MIN_VALUE }, { Long.MIN_VALUE / 2, 2, Long.MIN_VALUE },
        { -1, -1, 1 }, { -1, 1, -1 }, { 0, -1, 0 }, { 0, 0, 0 }, { 0, 1, 0 }, { 1, -1, -1 }, { 1, 1, 1 },
        { Long.MAX_VALUE / 2, 2, Long.MAX_VALUE - 1 }, { Long.MAX_VALUE, -1, Long.MIN_VALUE + 1 }, };
    }

    @Test
    public void test_safeMultiplyLongLong() {

        for (Object[] data : safeMultiplyLongLongProvider()) {
            long a = ((Number) data[0]).longValue();
            long b = ((Number) data[1]).longValue();
            long expected = ((Number) data[2]).longValue();

            assertEquals(TJdk8Methods.safeMultiply(a, b), expected);
        }
    }

    Object[][] safeMultiplyLongLongProviderOverflow() {

        return new Object[][] { { Long.MIN_VALUE, 2 }, { Long.MIN_VALUE / 2 - 1, 2 }, { Long.MAX_VALUE, 2 },
        { Long.MAX_VALUE / 2 + 1, 2 }, { Long.MIN_VALUE, -1 }, { -1, Long.MIN_VALUE }, };
    }

    @Test
    public void test_safeMultiplyLongLong_overflow() {

        for (Object[] data : safeMultiplyLongLongProviderOverflow()) {
            long a = ((Number) data[0]).longValue();
            long b = ((Number) data[1]).longValue();

            try {
                TJdk8Methods.safeMultiply(a, b);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    Object[][] safeToIntProvider() {

        return new Object[][] { { Integer.MIN_VALUE }, { Integer.MIN_VALUE + 1 }, { -1 }, { 0 }, { 1 },
        { Integer.MAX_VALUE - 1 }, { Integer.MAX_VALUE }, };
    }

    public void test_safeToInt() {

        for (Object[] data : safeToIntProvider()) {
            long l = ((Number) data[0]).longValue();

            assertEquals(TJdk8Methods.safeToInt(l), l);
        }
    }

    Object[][] safeToIntProviderOverflow() {

        return new Object[][] { { Long.MIN_VALUE }, { Integer.MIN_VALUE - 1L }, { Integer.MAX_VALUE + 1L },
        { Long.MAX_VALUE }, };
    }

    @Test
    public void test_safeToInt_overflow() {

        for (Object[] data : safeToIntProviderOverflow()) {
            long l = ((Number) data[0]).longValue();

            try {
                TJdk8Methods.safeToInt(l);
                fail("Expected ArithmeticException");
            } catch (ArithmeticException e) {
                // expected
            }
        }
    }

    @Test
    public void test_safeCompare_int() {

        doTest_safeCompare_int(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2, -2, -1, 0, 1, 2,
                Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }

    private void doTest_safeCompare_int(int... values) {

        for (int i = 0; i < values.length; i++) {
            int a = values[i];
            for (int j = 0; j < values.length; j++) {
                int b = values[j];
                assertEquals(a + " <=> " + b, TJdk8Methods.compareInts(a, b), a < b ? -1 : (a > b ? 1 : 0));
            }
        }
    }

    @Test
    public void test_safeCompare_long() {

        doTest_safeCompare_long(Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 2, Integer.MIN_VALUE,
                Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2, -2, -1, 0, 1, 2, Integer.MAX_VALUE - 2,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE);
    }

    private void doTest_safeCompare_long(long... values) {

        for (int i = 0; i < values.length; i++) {
            long a = values[i];
            for (int j = 0; j < values.length; j++) {
                long b = values[j];
                assertEquals(a + " <=> " + b, TJdk8Methods.compareLongs(a, b), a < b ? -1 : (a > b ? 1 : 0));
            }
        }
    }

    Object[][] data_floorDiv() {

        return new Object[][] { { 5L, 4, 1L }, { 4L, 4, 1L }, { 3L, 4, 0L }, { 2L, 4, 0L }, { 1L, 4, 0L },
        { 0L, 4, 0L }, { -1L, 4, -1L }, { -2L, 4, -1L }, { -3L, 4, -1L }, { -4L, 4, -1L }, { -5L, 4, -2L }, };
    }

    @Test
    public void test_floorDiv_long() {

        for (Object[] data : data_floorDiv()) {
            long a = ((Number) data[0]).longValue();
            int b = (int) data[1];
            long expected = ((Number) data[2]).longValue();

            assertEquals(TJdk8Methods.floorDiv(a, b), expected);
        }
    }

    @Test
    public void test_floorDiv_int() {

        for (Object[] data : data_floorDiv()) {
            long a = ((Number) data[0]).longValue();
            int b = (int) data[1];
            long expected = ((Number) data[2]).longValue();

            if (a <= Integer.MAX_VALUE && a >= Integer.MIN_VALUE) {
                assertEquals(TJdk8Methods.floorDiv((int) a, b), (int) expected);
            }
        }
    }

    Object[][] data_floorMod() {

        return new Object[][] { { 5L, 4, 1 }, { 4L, 4, 0 }, { 3L, 4, 3 }, { 2L, 4, 2 }, { 1L, 4, 1 }, { 0L, 4, 0 },
        { -1L, 4, 3 }, { -2L, 4, 2 }, { -3L, 4, 1 }, { -4L, 4, 0 }, { -5L, 4, 3 }, };
    }

    @Test
    public void test_floorMod_long() {

        for (Object[] data : data_floorMod()) {
            long a = ((Number) data[0]).longValue();
            long b = ((Number) data[1]).longValue();
            int expected = (int) data[2];

            assertEquals(TJdk8Methods.floorMod(a, b), expected);
        }
    }

    @Test
    public void test_floorMod_long_int() {

        for (Object[] data : data_floorMod()) {
            long a = ((Number) data[0]).longValue();
            int b = (int) data[1];
            int expected = (int) data[2];

            assertEquals(TJdk8Methods.floorMod(a, b), expected);
        }
    }

    @Test
    public void test_floorMod_int() {

        for (Object[] data : data_floorMod()) {
            long a = ((Number) data[0]).longValue();
            int b = (int) data[1];
            int expected = (int) data[2];

            if (a <= Integer.MAX_VALUE && a >= Integer.MIN_VALUE) {
                assertEquals(TJdk8Methods.floorMod((int) a, b), expected);
            }
        }
    }

}
