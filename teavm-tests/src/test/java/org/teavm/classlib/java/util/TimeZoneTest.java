package org.teavm.classlib.java.util;

import static org.junit.Assert.*;
import org.junit.Test;
import org.teavm.classlib.impl.tz.TimeZoneResourceProvider;

/**
 *
 * @author Alexey Andreev
 */
public class TimeZoneTest {
    @Test
    public void resourceProvided() {
        assertNotNull(TimeZoneResourceProvider.getTimeZone("Europe/Moscow"));
    }
}
