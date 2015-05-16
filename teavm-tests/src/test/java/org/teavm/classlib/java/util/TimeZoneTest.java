package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.teavm.classlib.impl.tz.DateTimeZone;
import org.teavm.classlib.impl.tz.DateTimeZoneProvider;

/**
 *
 * @author Alexey Andreev
 */
public class TimeZoneTest {
    @Test
    public void resourceProvided() {
        DateTimeZone tz = DateTimeZoneProvider.getTimeZone("Europe/Moscow");
        assertEquals(1414274399999L, tz.previousTransition(1431781727159L));
        assertEquals(1301183999999L, tz.previousTransition(1414274399999L));
        assertEquals(1288479599999L, tz.previousTransition(1301183999999L));
        System.out.println(DateTimeZoneProvider.detectTimezone().getID());
    }
}
