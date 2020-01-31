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
package org.threeten.bp.format;

import static org.testng.Assert.assertEquals;

import java.util.Locale;
import java.util.Set;

import org.testng.annotations.Test;

/**
 * Test DecimalStyle.
 */
@Test
public class TestDecimalStyle {

    @Test
    public void test_getAvailableLocales() {
        Set<Locale> locales = DecimalStyle.getAvailableLocales();
        assertEquals(locales.size() > 0, true);
        assertEquals(locales.contains(Locale.US), true);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_of_Locale() {
        DecimalStyle loc1 = DecimalStyle.of(Locale.CANADA);
        assertEquals(loc1.getZeroDigit(), '0');
        assertEquals(loc1.getPositiveSign(), '+');
        assertEquals(loc1.getNegativeSign(), '-');
        assertEquals(loc1.getDecimalSeparator(), '.');
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_STANDARD() {
        DecimalStyle loc1 = DecimalStyle.STANDARD;
        assertEquals(loc1.getZeroDigit(), '0');
        assertEquals(loc1.getPositiveSign(), '+');
        assertEquals(loc1.getNegativeSign(), '-');
        assertEquals(loc1.getDecimalSeparator(), '.');
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_zeroDigit() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals(base.withZeroDigit('A').getZeroDigit(), 'A');
    }

    @Test
    public void test_positiveSign() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals(base.withPositiveSign('A').getPositiveSign(), 'A');
    }

    @Test
    public void test_negativeSign() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals(base.withNegativeSign('A').getNegativeSign(), 'A');
    }

    @Test
    public void test_decimalSeparator() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals(base.withDecimalSeparator('A').getDecimalSeparator(), 'A');
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_convertToDigit_base() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals(base.convertToDigit('0'), 0);
        assertEquals(base.convertToDigit('1'), 1);
        assertEquals(base.convertToDigit('9'), 9);
        assertEquals(base.convertToDigit(' '), -1);
        assertEquals(base.convertToDigit('A'), -1);
    }

    @Test
    public void test_convertToDigit_altered() {
        DecimalStyle base = DecimalStyle.STANDARD.withZeroDigit('A');
        assertEquals(base.convertToDigit('A'), 0);
        assertEquals(base.convertToDigit('B'), 1);
        assertEquals(base.convertToDigit('J'), 9);
        assertEquals(base.convertToDigit(' '), -1);
        assertEquals(base.convertToDigit('0'), -1);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_convertNumberToI18N_base() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals(base.convertNumberToI18N("134"), "134");
    }

    @Test
    public void test_convertNumberToI18N_altered() {
        DecimalStyle base = DecimalStyle.STANDARD.withZeroDigit('A');
        assertEquals(base.convertNumberToI18N("134"), "BDE");
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_equalsHashCode1() {
        DecimalStyle a = DecimalStyle.STANDARD;
        DecimalStyle b = DecimalStyle.STANDARD;
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_equalsHashCode2() {
        DecimalStyle a = DecimalStyle.STANDARD.withZeroDigit('A');
        DecimalStyle b = DecimalStyle.STANDARD.withZeroDigit('A');
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_equalsHashCode3() {
        DecimalStyle a = DecimalStyle.STANDARD.withZeroDigit('A');
        DecimalStyle b = DecimalStyle.STANDARD.withDecimalSeparator('A');
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
    }

    @Test
    public void test_equalsHashCode_bad() {
        DecimalStyle a = DecimalStyle.STANDARD;
        assertEquals(a.equals(""), false);
        assertEquals(a.equals(null), false);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toString_base() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals(base.toString(), "DecimalStyle[0+-.]");
    }

    @Test
    public void test_toString_altered() {
        DecimalStyle base = DecimalStyle.of(Locale.US).withZeroDigit('A').withDecimalSeparator('@');
        assertEquals(base.toString(), "DecimalStyle[A+-@]");
    }

}
