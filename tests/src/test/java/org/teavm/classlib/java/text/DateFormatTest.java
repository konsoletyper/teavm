/*
 *  Copyright 2014 Alexey Andreev.
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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class DateFormatTest {
    public DateFormatTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @Test
    public void shortDateFormatHandled() throws ParseException {
        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ENGLISH);
        assertEquals("6/23/14", format.format(getDateWithZoneOffset(1403481600000L)));
        assertEquals(1403481600000L, getTimeWithoutZoneOffset(format.parse("6/23/14")));
    }

    @Test
    public void mediumDateFormatHandled() throws ParseException {
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH);
        assertEquals("Jun 23, 2014", format.format(getDateWithZoneOffset(1403481600000L)));
        assertEquals(1403481600000L, getTimeWithoutZoneOffset(format.parse("Jun 23, 2014")));
    }

    @Test
    public void longDateFormatHandled() throws ParseException {
        DateFormat format = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
        assertEquals("June 23, 2014", format.format(getDateWithZoneOffset(1403481600000L)));
        assertEquals(1403481600000L, getTimeWithoutZoneOffset(format.parse("June 23, 2014")));
    }

    @Test
    public void fullDateFormatHandled() throws ParseException {
        DateFormat format = DateFormat.getDateInstance(DateFormat.FULL, Locale.ENGLISH);
        assertEquals("Monday, June 23, 2014", format.format(getDateWithZoneOffset(1403481600000L)));
        assertEquals(1403481600000L, getTimeWithoutZoneOffset(format.parse("Monday, June 23, 2014")));
    }

    private Date getDateWithZoneOffset(long milliseconds) {
        Calendar calendar = new GregorianCalendar(Locale.ENGLISH);
        calendar.setTimeInMillis(milliseconds);
        milliseconds -= calendar.get(Calendar.ZONE_OFFSET) - calendar.get(Calendar.DST_OFFSET);
        return new Date(milliseconds);
    }

    private long getTimeWithoutZoneOffset(Date date) {
        Calendar calendar = new GregorianCalendar(Locale.ENGLISH);
        calendar.setTime(date);
        return calendar.getTimeInMillis() + calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
    }
}
