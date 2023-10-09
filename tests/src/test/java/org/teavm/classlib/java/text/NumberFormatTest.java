/*
 *  Copyright 2017 Alexey Andreev.
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
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@TeaVMProperties(@TeaVMProperty(key = "java.util.Locale.available", value = "en, en_US, en_GB, ru, ru_RU"))
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.WASI)
public class NumberFormatTest {
    @Test
    public void formatsNumber() {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("en"));
        assertEquals("123,456.789", format.format(123456.789123));

        format = NumberFormat.getNumberInstance(new Locale("ru"));
        assertEquals("123\u00A0456,789", format.format(123456.789123));
    }

    @Test
    @SkipJVM
    public void formatsCurrency() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        format.setCurrency(Currency.getInstance("RUB"));
        assertEquals("RUB123,456.79", format.format(123456.789123));

        format = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
        format.setCurrency(Currency.getInstance("RUB"));
        assertEquals("123 456,79 \u20BD", format.format(123456.789123).replace('\u00A0', ' '));
    }

    @Test
    @SkipJVM
    public void formatsPercent() {
        NumberFormat format = NumberFormat.getPercentInstance(new Locale("en", "US"));
        assertEquals("12,345,679%", format.format(123456.789123));

        format = NumberFormat.getPercentInstance(new Locale("ru", "RU"));
        assertEquals("12 345 679 %", format.format(123456.789123).replace('\u00A0', ' '));
    }
}
