/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@TeaVMProperties(@TeaVMProperty(key = "java.util.Locale.available", value = "en, en_US, en_GB, ru, ru_RU"))
public class DecimalFormatTest {
    private static DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);

    @Test
    public void parsesIntegerPattern() {
        DecimalFormat format = createFormat("00");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertFalse(format.isDecimalSeparatorAlwaysShown());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());

        format = createFormat("##");
        assertEquals(0, format.getMinimumIntegerDigits());
        assertFalse(format.isDecimalSeparatorAlwaysShown());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());

        format = createFormat("#,##0");
        assertEquals(1, format.getMinimumIntegerDigits());
        assertFalse(format.isDecimalSeparatorAlwaysShown());
        assertTrue(format.isGroupingUsed());
        assertEquals(3, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());
    }

    @Test
    public void selectsLastGrouping() {
        DecimalFormat format = new DecimalFormat("#,0,000");
        assertEquals(4, format.getMinimumIntegerDigits());
        assertTrue(format.isGroupingUsed());
        assertEquals(3, format.getGroupingSize());
    }

    @Test
    public void parsesPrefixAndSuffixInPattern() {
        DecimalFormat format = createFormat("(00)");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertEquals("(", format.getPositivePrefix());
        assertEquals(")", format.getPositiveSuffix());
        assertEquals("-(", format.getNegativePrefix());
        assertEquals(")", format.getNegativeSuffix());

        format = createFormat("+(00);-{#}");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertEquals("+(", format.getPositivePrefix());
        assertEquals(")", format.getPositiveSuffix());
        assertEquals("-{", format.getNegativePrefix());
    }

    @Test
    public void parsesFractionalPattern() {
        DecimalFormat format = createFormat("#.");
        assertEquals(1, format.getMinimumIntegerDigits());
        assertTrue(format.isDecimalSeparatorAlwaysShown());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());

        format = createFormat("#.00");
        assertEquals(0, format.getMinimumIntegerDigits());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(2, format.getMaximumFractionDigits());

        format = createFormat("#.00##");
        assertEquals(0, format.getMinimumIntegerDigits());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(4, format.getMaximumFractionDigits());

        format = createFormat("#00.00##");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(4, format.getMaximumFractionDigits());

        format = createFormat("#,#00.00##");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertTrue(format.isGroupingUsed());
        assertEquals(3, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(4, format.getMaximumFractionDigits());
    }

    @Test
    public void parsesExponentialPattern() {
        DecimalFormat format = createFormat("##0E00");
        assertEquals(1, format.getMinimumIntegerDigits());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());
    }

    @Test
    public void formatsIntegerPart() {
        DecimalFormat format = createFormat("00");
        assertEquals("02", format.format(2));
        assertEquals("23", format.format(23));
        assertEquals("23", format.format(23.2));
        assertEquals("24", format.format(23.7));
    }

    @Test
    public void formatsBigIntegerPart() {
        DecimalFormat format = createFormat("00");
        assertEquals("02", format.format(new BigInteger("2")));
        assertEquals("23", format.format(new BigInteger("23")));
        assertEquals("23", format.format(new BigDecimal("23.2")));
        assertEquals("24", format.format(new BigDecimal("23.7")));
    }

    @Test
    public void formatsNumber() {
        DecimalFormat format = createFormat("0.0");
        assertEquals("23.0", format.format(23));
        assertEquals("23.2", format.format(23.2));
        assertEquals("23.2", format.format(23.23));
        assertEquals("23.3", format.format(23.27));
        assertEquals("0.0", format.format(0.0001));

        format = createFormat("00000000000000000000000000.0");
        assertEquals("00000000000000000000000023.0", format.format(23));
        assertEquals("00002300000000000000000000.0", format.format(23E20));
        assertEquals("24000000000000000000000000.0", format.format(24E24));

        format = createFormat("0.00000000000000000000000000");
        assertEquals("23.00000000000000000000000000", format.format(23));
        assertEquals("0.23000000000000000000000000", format.format(0.23));
        assertEquals("0.00230000000000000000000000", format.format(0.0023));
        assertEquals("0.00000000000000000000230000", format.format(23E-22));
        assertEquals("0.00000000000000000000000023", format.format(23E-26));

        assertEquals("1", createFormat("#.##").format(0.9977993000000007d));
        assertEquals("1", createFormat("#.##").format(0.997799f));
    }

    @Test
    public void formatsBigNumber() {
        DecimalFormat format = createFormat("0.0");
        assertEquals("23.0", format.format(BigInteger.valueOf(23)));
        assertEquals("23.2", format.format(new BigDecimal("23.2")));
        assertEquals("23.2", format.format(new BigDecimal("23.23")));
        assertEquals("23.3", format.format(new BigDecimal("23.27")));
        assertEquals("0.0", format.format(new BigDecimal("0.0001")));

        format = createFormat("00000000000000000000000000.0");
        assertEquals("00000000000000000000000023.0", format.format(new BigInteger("23")));
        assertEquals("00002300000000000000000000.0", format.format(new BigInteger("2300000000000000000000")));
        assertEquals("23000000000000000000000000.0", format.format(new BigInteger("23000000000000000000000000")));

        format = createFormat("0.00000000000000000000000000");
        assertEquals("23.00000000000000000000000000", format.format(new BigInteger("23")));
        assertEquals("0.23000000000000000000000000", format.format(new BigDecimal("0.23")));
        assertEquals("0.00230000000000000000000000", format.format(new BigDecimal("0.0023")));
        assertEquals("0.00000000000000000000230000", format.format(new BigDecimal("0.0000000000000000000023")));
        assertEquals("0.00000000000000000000000023", format.format(new BigDecimal("0.00000000000000000000000023")));
    }

    @Test
    public void formatsFractionalPart() {
        DecimalFormat format = createFormat("0.0000####");
        assertEquals("0.00001235", format.format(0.0000123456));
        assertEquals("0.00012346", format.format(0.000123456));
        assertEquals("0.00123456", format.format(0.00123456));
        assertEquals("0.0123456", format.format(0.0123456));
        assertEquals("0.1200", format.format(0.12));
        assertEquals("0.1230", format.format(0.123));
        assertEquals("0.1234", format.format(0.1234));
        assertEquals("0.12345", format.format(0.12345));

        format = createFormat("0.##");
        assertEquals("23", format.format(23));
        assertEquals("2.3", format.format(2.3));
        assertEquals("0.23", format.format(0.23));
        assertEquals("0.02", format.format(0.023));
    }

    @Test
    public void roundingWorks() {
        DecimalFormat format = createFormat("0");

        format.setRoundingMode(RoundingMode.UP);
        assertEquals("3", format.format(2.3));
        assertEquals("3", format.format(2.7));
        assertEquals("-3", format.format(-2.3));
        assertEquals("-3", format.format(-2.7));

        format.setRoundingMode(RoundingMode.DOWN);
        assertEquals("2", format.format(2.3));
        assertEquals("2", format.format(2.7));
        assertEquals("-2", format.format(-2.3));
        assertEquals("-2", format.format(-2.7));

        format.setRoundingMode(RoundingMode.FLOOR);
        assertEquals("2", format.format(2.3));
        assertEquals("2", format.format(2.7));
        assertEquals("-3", format.format(-2.3));
        assertEquals("-3", format.format(-2.7));

        format.setRoundingMode(RoundingMode.CEILING);
        assertEquals("3", format.format(2.3));
        assertEquals("3", format.format(2.7));
        assertEquals("-2", format.format(-2.3));
        assertEquals("-2", format.format(-2.7));

        format.setRoundingMode(RoundingMode.HALF_DOWN);
        assertEquals("2", format.format(2.3));
        assertEquals("3", format.format(2.7));
        assertEquals("2", format.format(2.5));
        assertEquals("3", format.format(3.5));
        assertEquals("-2", format.format(-2.5));
        assertEquals("-3", format.format(-3.5));

        format.setRoundingMode(RoundingMode.HALF_UP);
        assertEquals("2", format.format(2.3));
        assertEquals("3", format.format(2.7));
        assertEquals("3", format.format(2.5));
        assertEquals("4", format.format(3.5));
        assertEquals("-3", format.format(-2.5));
        assertEquals("-4", format.format(-3.5));

        format.setRoundingMode(RoundingMode.HALF_EVEN);
        assertEquals("2", format.format(2.3));
        assertEquals("3", format.format(2.7));
        assertEquals("2", format.format(2.5));
        assertEquals("4", format.format(3.5));
        assertEquals("-2", format.format(-2.5));
        assertEquals("-4", format.format(-3.5));
    }

    @Test
    public void bigRoundingWorks() {
        DecimalFormat format = createFormat("0");

        format.setRoundingMode(RoundingMode.UP);
        assertEquals("3", format.format(new BigDecimal("2.3")));
        assertEquals("3", format.format(new BigDecimal("2.7")));
        assertEquals("-3", format.format(new BigDecimal("-2.3")));
        assertEquals("-3", format.format(new BigDecimal("-2.7")));

        format.setRoundingMode(RoundingMode.DOWN);
        assertEquals("2", format.format(new BigDecimal("2.3")));
        assertEquals("2", format.format(new BigDecimal("2.7")));
        assertEquals("-2", format.format(new BigDecimal("-2.3")));
        assertEquals("-2", format.format(new BigDecimal("-2.7")));

        format.setRoundingMode(RoundingMode.FLOOR);
        assertEquals("2", format.format(new BigDecimal("2.3")));
        assertEquals("2", format.format(new BigDecimal("2.7")));
        assertEquals("-3", format.format(new BigDecimal("-2.3")));
        assertEquals("-3", format.format(new BigDecimal("-2.7")));

        format.setRoundingMode(RoundingMode.CEILING);
        assertEquals("3", format.format(new BigDecimal("2.3")));
        assertEquals("3", format.format(new BigDecimal("2.7")));
        assertEquals("-2", format.format(new BigDecimal("-2.3")));
        assertEquals("-2", format.format(new BigDecimal("-2.7")));

        format.setRoundingMode(RoundingMode.HALF_DOWN);
        assertEquals("2", format.format(new BigDecimal("2.3")));
        assertEquals("3", format.format(new BigDecimal("2.7")));
        assertEquals("2", format.format(new BigDecimal("2.5")));
        assertEquals("3", format.format(new BigDecimal("3.5")));
        assertEquals("-2", format.format(new BigDecimal("-2.5")));
        assertEquals("-3", format.format(new BigDecimal("-3.5")));

        format.setRoundingMode(RoundingMode.HALF_UP);
        assertEquals("2", format.format(new BigDecimal("2.3")));
        assertEquals("3", format.format(new BigDecimal("2.7")));
        assertEquals("3", format.format(new BigDecimal("2.5")));
        assertEquals("4", format.format(new BigDecimal("3.5")));
        assertEquals("-3", format.format(new BigDecimal("-2.5")));
        assertEquals("-4", format.format(new BigDecimal("-3.5")));

        format.setRoundingMode(RoundingMode.HALF_EVEN);
        assertEquals("2", format.format(new BigDecimal("2.3")));
        assertEquals("3", format.format(new BigDecimal("2.7")));
        assertEquals("2", format.format(new BigDecimal("2.5")));
        assertEquals("4", format.format(new BigDecimal("3.5")));
        assertEquals("-2", format.format(new BigDecimal("-2.5")));
        assertEquals("-4", format.format(new BigDecimal("-3.5")));
    }

    @Test
    public void formatsWithGroups() {
        DecimalFormat format = createFormat("#,###.0");
        assertEquals("23.0", format.format(23));
        assertEquals("2,300.0", format.format(2300));
        assertEquals("2,300,000,000,000,000,000,000.0", format.format(23E20));
        assertEquals("24,000,000,000,000,000,000,000,000.0", format.format(24E24));

        format = createFormat("000,000,000,000,000,000,000");
        assertEquals("000,000,000,000,000,000,023", format.format(23));
    }

    @Test
    public void formatsBigWithGroups() {
        DecimalFormat format = createFormat("#,###.0");
        assertEquals("23.0", format.format(BigInteger.valueOf(23)));
        assertEquals("2,300.0", format.format(BigInteger.valueOf(2300)));
        assertEquals("2,300,000,000,000,000,000,000.0", format.format(new BigInteger("2300000000000000000000")));
        assertEquals("23,000,000,000,000,000,000,000,000.0", format.format(
                new BigInteger("23000000000000000000000000")));

        format = createFormat("000,000,000,000,000,000,000");
        assertEquals("000,000,000,000,000,000,023", format.format(BigInteger.valueOf(23)));
    }

    @Test
    public void formatsLargeValues() {
        DecimalFormat format = createFormat("0");
        assertEquals("9223372036854775807", format.format(9223372036854775807L));
        assertEquals("-9223372036854775808", format.format(-9223372036854775808L));
    }

    @Test
    public void formatsExponent() {
        DecimalFormat format = createFormat("000E0");
        assertEquals("230E-1", format.format(23));
        assertEquals("230E0", format.format(230));
        assertEquals("230E1", format.format(2300));
        assertEquals("123E1", format.format(1234));
        assertEquals("-123E1", format.format(-1234));

        format = createFormat("0.00E0");
        assertEquals("2.00E1", format.format(20));
        assertEquals("2.30E1", format.format(23));
        assertEquals("2.30E2", format.format(230));
        assertEquals("1.23E3", format.format(1234));

        format = createFormat("000000000000000000000.00E0");
        assertEquals("230000000000000000000.00E-19", format.format(23));

        format = createFormat("0.0000000000000000000000E0");
        assertEquals("2.3000000000000000000000E1", format.format(23));

        format = createFormat("0.0##E0");
        assertEquals("1.0E0", format.format(1));
        assertEquals("1.2E1", format.format(12));
        assertEquals("1.23E2", format.format(123));
        assertEquals("1.234E3", format.format(1234));
        assertEquals("1.234E4", format.format(12345));

        //assertEquals("1.23E4", createFormat("#.##E0").format(12345));
        assertEquals("1E5", createFormat("#.##E0").format(99999));
    }

    @Test
    public void formatsBigExponent() {
        DecimalFormat format = createFormat("000E0");
        assertEquals("230E-1", format.format(BigInteger.valueOf(23)));
        assertEquals("230E0", format.format(BigInteger.valueOf(230)));
        assertEquals("230E1", format.format(BigInteger.valueOf(2300)));
        assertEquals("123E1", format.format(BigInteger.valueOf(1234)));
        assertEquals("-123E1", format.format(BigInteger.valueOf(-1234)));

        format = createFormat("0.00E0");
        assertEquals("2.00E1", format.format(BigInteger.valueOf(20)));
        assertEquals("2.30E1", format.format(BigInteger.valueOf(23)));
        assertEquals("2.30E2", format.format(BigInteger.valueOf(230)));
        assertEquals("1.23E3", format.format(BigInteger.valueOf(1234)));

        format = createFormat("000000000000000000000.00E0");
        assertEquals("230000000000000000000.00E-19", format.format(BigInteger.valueOf(23)));

        format = createFormat("0.0000000000000000000000E0");
        assertEquals("2.3000000000000000000000E1", format.format(BigInteger.valueOf(23)));

        format = createFormat("0.0##E0");
        assertEquals("1.0E0", format.format(BigInteger.valueOf(1)));
        assertEquals("1.2E1", format.format(BigInteger.valueOf(12)));
        assertEquals("1.23E2", format.format(BigInteger.valueOf(123)));
        assertEquals("1.234E3", format.format(BigInteger.valueOf(1234)));
        assertEquals("1.234E4", format.format(BigInteger.valueOf(12345)));
    }

    @Test
    public void formatsExponentWithMultiplier() {
        DecimalFormat format = createFormat("##0.00E0");
        assertEquals("2.30E0", format.format(2.3));
        assertEquals("23.0E0", format.format(23));
        assertEquals("230E0", format.format(230));
        assertEquals("2.30E3", format.format(2300));
        assertEquals("23.0E3", format.format(23000));
    }

    @Test
    public void formatsBigExponentWithMultiplier() {
        DecimalFormat format = createFormat("##0.00E0");
        assertEquals("2.30E0", format.format(new BigDecimal("2.3")));
        assertEquals("23.0E0", format.format(new BigDecimal("23")));
        assertEquals("230E0", format.format(new BigDecimal("230")));
        assertEquals("2.30E3", format.format(new BigDecimal("2300")));
        assertEquals("23.0E3", format.format(new BigDecimal("23000")));
    }

    @Test
    public void formatsSpecialValues() {
        DecimalFormat format = createFormat("0");
        assertEquals("∞", format.format(Double.POSITIVE_INFINITY));
        assertEquals("-∞", format.format(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void formatsWithMultiplier() {
        DecimalFormat format = createFormat("0");
        format.setMultiplier(2);
        assertEquals("18446744073709551614", format.format(9223372036854775807L));
        assertEquals("46", format.format(BigInteger.valueOf(23)));

        format.setMultiplier(100);
        assertEquals("2300", format.format(23));
        assertEquals("2300", format.format(BigInteger.valueOf(23)));

        format = createFormat("00E0");
        format.setMultiplier(2);
        assertEquals("18E18", format.format(9223372036854775807L));
        assertEquals("46E0", format.format(BigInteger.valueOf(23)));

        format.setMultiplier(100);
        assertEquals("23E2", format.format(23));
        assertEquals("23E2", format.format(BigInteger.valueOf(23)));
    }

    @Test
    public void formatsSpecial() {
        DecimalFormat format = createFormat("0%");
        assertEquals("23%", format.format(0.23));

        format = createFormat("0‰");
        assertEquals("230‰", format.format(0.23));

        format = createFormat("0.00 ¤");
        format.setCurrency(Currency.getInstance("RUB"));
        assertEquals("23.00 RUB", format.format(23));
    }

    private DecimalFormat createFormat(String format) {
        return new DecimalFormat(format, symbols);
    }
}
