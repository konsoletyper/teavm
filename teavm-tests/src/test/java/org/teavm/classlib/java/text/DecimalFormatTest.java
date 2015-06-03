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
        assertEquals("23000000000000000000000000.0", format.format(23E24));

        format = createFormat("0.00000000000000000000000000");
        assertEquals("23.00000000000000000000000000", format.format(23));
        assertEquals("0.23000000000000000000000000", format.format(0.23));
        assertEquals("0.00000000000000000000230000", format.format(23E-22));
        assertEquals("0.00000000000000000000000023", format.format(23E-26));
    }


    private DecimalFormat createFormat(String format) {
        return new DecimalFormat(format, symbols);
    }
}
