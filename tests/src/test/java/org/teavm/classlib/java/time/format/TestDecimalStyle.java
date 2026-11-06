/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.classlib.java.time.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.format.DecimalStyle;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTest;

/**
 * Test DecimalStyle.
 */
@TeaVMTest
@TeaVMProperties(@TeaVMProperty(key = "java.util.Locale.available", value = "en, en_US"))
public class TestDecimalStyle {

    @Test
    public void test_getAvailableLocales() {
        Set<Locale> locales = DecimalStyle.getAvailableLocales();
        assertTrue(locales.size() > 0);
        assertTrue(locales.contains(Locale.US));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_of_Locale() {
        DecimalStyle loc1 = DecimalStyle.of(Locale.CANADA);
        assertEquals('0', loc1.getZeroDigit());
        assertEquals('+', loc1.getPositiveSign());
        assertEquals('-', loc1.getNegativeSign());
        assertEquals('.', loc1.getDecimalSeparator());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_STANDARD() {
        DecimalStyle loc1 = DecimalStyle.STANDARD;
        assertEquals('0', loc1.getZeroDigit());
        assertEquals('+', loc1.getPositiveSign());
        assertEquals('-', loc1.getNegativeSign());
        assertEquals('.', loc1.getDecimalSeparator());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_zeroDigit() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals('A', base.withZeroDigit('A').getZeroDigit());
    }

    @Test
    public void test_positiveSign() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals('A', base.withPositiveSign('A').getPositiveSign());
    }

    @Test
    public void test_negativeSign() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals('A', base.withNegativeSign('A').getNegativeSign());
    }

    @Test
    public void test_decimalSeparator() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals('A', base.withDecimalSeparator('A').getDecimalSeparator());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_equalsHashCode1() {
        DecimalStyle a = DecimalStyle.STANDARD;
        DecimalStyle b = DecimalStyle.STANDARD;
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(b.hashCode(), a.hashCode());
    }

    @Test
    public void test_equalsHashCode2() {
        DecimalStyle a = DecimalStyle.STANDARD.withZeroDigit('A');
        DecimalStyle b = DecimalStyle.STANDARD.withZeroDigit('A');
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(b.hashCode(), a.hashCode());
    }

    @Test
    public void test_equalsHashCode3() {
        DecimalStyle a = DecimalStyle.STANDARD.withZeroDigit('A');
        DecimalStyle b = DecimalStyle.STANDARD.withDecimalSeparator('A');
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
    }

    @Test
    public void test_equalsHashCode_bad() {
        DecimalStyle a = DecimalStyle.STANDARD;
        assertFalse(a.equals(""));
        assertFalse(a.equals(null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toString_base() {
        DecimalStyle base = DecimalStyle.STANDARD;
        assertEquals("DecimalStyle[0+-.]", base.toString());
    }

    @Test
    public void test_toString_altered() {
        DecimalStyle base = DecimalStyle.of(Locale.US).withZeroDigit('A').withDecimalSeparator('@');
        assertEquals("DecimalStyle[A+-@]", base.toString());
    }

}
