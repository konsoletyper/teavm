package org.teavm.classlib.java.text;

import static org.junit.Assert.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class DecimalFormatTest {
    @Test
    public void parsesIntegerPattern() {
        DecimalFormat format = new DecimalFormat("00");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertFalse(format.isDecimalSeparatorAlwaysShown());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());

        format = new DecimalFormat("##");
        assertEquals(0, format.getMinimumIntegerDigits());
        assertFalse(format.isDecimalSeparatorAlwaysShown());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());

        format = new DecimalFormat("#,##0");
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
        DecimalFormat format = new DecimalFormat("(00)", new DecimalFormatSymbols(Locale.ENGLISH));
        assertEquals(2, format.getMinimumIntegerDigits());
        assertEquals("(", format.getPositivePrefix());
        assertEquals(")", format.getPositiveSuffix());
        assertEquals("-(", format.getNegativePrefix());
        assertEquals(")", format.getNegativeSuffix());

        format = new DecimalFormat("+(00);-{#}", new DecimalFormatSymbols(Locale.ENGLISH));
        assertEquals(2, format.getMinimumIntegerDigits());
        assertEquals("+(", format.getPositivePrefix());
        assertEquals(")", format.getPositiveSuffix());
        assertEquals("-{", format.getNegativePrefix());
    }

    @Test
    public void parsesFractionalPattern() {
        DecimalFormat format = new DecimalFormat("#.");
        assertEquals(1, format.getMinimumIntegerDigits());
        assertTrue(format.isDecimalSeparatorAlwaysShown());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());

        format = new DecimalFormat("#.00");
        assertEquals(0, format.getMinimumIntegerDigits());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(2, format.getMaximumFractionDigits());

        format = new DecimalFormat("#.00##");
        assertEquals(0, format.getMinimumIntegerDigits());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(4, format.getMaximumFractionDigits());

        format = new DecimalFormat("#00.00##");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertFalse(format.isGroupingUsed());
        assertEquals(0, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(4, format.getMaximumFractionDigits());

        format = new DecimalFormat("#,#00.00##");
        assertEquals(2, format.getMinimumIntegerDigits());
        assertTrue(format.isGroupingUsed());
        assertEquals(3, format.getGroupingSize());
        assertEquals(2, format.getMinimumFractionDigits());
        assertEquals(4, format.getMaximumFractionDigits());
    }

    @Test
    public void parsesExponentialPattern() {
        DecimalFormat format = new DecimalFormat("##0E00");
        assertEquals(1, format.getMinimumIntegerDigits());
        assertEquals(0, format.getGroupingSize());
        assertEquals(0, format.getMinimumFractionDigits());
        assertEquals(0, format.getMaximumFractionDigits());
    }
}
