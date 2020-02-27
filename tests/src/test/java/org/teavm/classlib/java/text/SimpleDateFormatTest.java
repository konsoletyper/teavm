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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class SimpleDateFormatTest {
    public SimpleDateFormatTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @Test
    @SkipJVM
    public void firstDayOfWeekMatches() {
        assertEquals(Calendar.MONDAY, new GregorianCalendar(Locale.ENGLISH).getFirstDayOfWeek());
        assertEquals(1, new GregorianCalendar(Locale.ENGLISH).getMinimalDaysInFirstWeek());
    }

    @Test
    public void fieldsFormatted() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
    }

    @Test
    public void fieldsParsed() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49")));
    }

    @Test
    public void eraHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("G yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("AD 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("AD 2014-06-24 09:33:49")));
    }

    @Test
    public void shortYearHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("14-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("14-06-24 09:33:49")));
    }

    @Test
    @SkipJVM
    public void weekInYearHandled() throws ParseException {
        long day = 24 * 3600 * 1000;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss www", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 026", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals("2014-06-28 09:33:49 026", format.format(getDateWithZoneOffset(1403602429504L + day * 4)));
        assertEquals("2014-06-29 09:33:49 026", format.format(getDateWithZoneOffset(1403602429504L + day * 5)));
        assertEquals("2014-06-30 09:33:49 027", format.format(getDateWithZoneOffset(1403602429504L + day * 6)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 026")));
        assertEquals(1403602429000L + day * 4, getTimeWithoutZoneOffset(format.parse("2014-06-28 09:33:49 026")));
        assertEquals(1403602429000L + day * 5, getTimeWithoutZoneOffset(format.parse("2014-06-29 09:33:49 026")));
        assertEquals(1403602429000L + day * 6, getTimeWithoutZoneOffset(format.parse("2014-06-30 09:33:49 027")));
    }

    @Test
    @SkipJVM
    public void weekInMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss WW", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 05", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 05")));
    }

    @Test
    public void dayInYearHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss DD", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 175", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 175")));
    }

    @Test
    public void weekdayInMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss F", Locale.ENGLISH);
        assertEquals("2014-06-24 09:33:49 4", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 09:33:49 4")));
    }

    @Test
    public void shortWeekdayHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("E yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("Tue 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("Tue 2014-06-24 09:33:49")));
    }

    @Test
    public void longWeekdayHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("EEEE, yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("Tuesday, 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("Tuesday, 2014-06-24 09:33:49")));
    }

    @Test
    public void numericWeekdayHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("u yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        assertEquals("2 2014-06-24 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2 2014-06-24 09:33:49")));
    }

    @Test
    public void amPmHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss a", Locale.ENGLISH);
        assertEquals("2014-06-24 at 09:33:49 AM", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("2014-06-24 at 09:33:49 AM")));
    }

    @Test
    public void shortMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MMM, dd yyyy HH:mm:ss", Locale.ENGLISH);
        assertEquals("Jun, 24 2014 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("Jun, 24 2014 09:33:49")));
    }

    @Test
    public void longMonthHandled() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MMMM, dd yyyy HH:mm:ss", Locale.ENGLISH);
        assertEquals("June, 24 2014 09:33:49", format.format(getDateWithZoneOffset(1403602429504L)));
        assertEquals(1403602429000L, getTimeWithoutZoneOffset(format.parse("June, 24 2014 09:33:49")));
    }

    private Date getDateWithZoneOffset(long milliseconds) {
        Calendar calendar = new GregorianCalendar(Locale.ENGLISH);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(milliseconds);
        return new Date(milliseconds);
    }

    private long getTimeWithoutZoneOffset(Date date) {
        Calendar calendar = new GregorianCalendar(Locale.ENGLISH);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.setTime(date);
        return calendar.getTimeInMillis();
    }

    @Test
    public void fieldsParsedWithoutDelimiters() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmm", Locale.ENGLISH);
        assertEquals(1403602380000L, getTimeWithoutZoneOffset(format.parse("1406240933")));
    }
}
