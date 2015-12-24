package org.teavm.classlib.java.util;

import static org.junit.Assert.*;
import java.util.Date;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class DateTest {
    @SuppressWarnings("deprecation")
    @Test
    public void setsDateAndMonth() {
        Date date = new Date();
        date.setMonth(0);
        date.setDate(4);
        date.setYear(115);
        assertEquals(0, date.getMonth());
        assertEquals(4, date.getDate());
        assertEquals(115, date.getYear());
    }
}
