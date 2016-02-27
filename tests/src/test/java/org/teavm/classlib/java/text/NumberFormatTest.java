package org.teavm.classlib.java.text;

import static org.junit.Assert.*;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@TeaVMProperties(@TeaVMProperty(key = "java.util.Locale.available", value = "en, en_US, en_GB, ru, ru_RU"))
public class NumberFormatTest {
    @Test
    public void formatsNumber() {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("en"));
        assertEquals("123,456.789", format.format(123456.789123));

        format = NumberFormat.getNumberInstance(new Locale("ru"));
        assertEquals("123\u00A0456,789", format.format(123456.789123));
    }

    @Test
    public void formatsCurrency() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        format.setCurrency(Currency.getInstance("RUB"));
        assertEquals("RUB123,456.79", format.format(123456.789123));

        format = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
        format.setCurrency(Currency.getInstance("RUB"));
        assertEquals("123 456,79 руб.", format.format(123456.789123).replace('\u00A0', ' '));
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
