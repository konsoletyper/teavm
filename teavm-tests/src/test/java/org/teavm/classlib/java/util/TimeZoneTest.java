/*
 * Copyright 2015 Steve Hannah.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teavm.classlib.java.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import static org.junit.Assert.*;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import org.junit.Test;
import org.teavm.classlib.java.util.TTimeZone.JSDate;
import org.teavm.platform.PlatformTimezone;

/**
 *
 * @author shannah
 */
public class TimeZoneTest {

    private static final int ONE_HOUR = 3600000;
    
    static class EST extends PlatformTimezone {

        @Override
        public String getTimezoneId() {
            return "EST";
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            return -5*ONE_HOUR;
        }

        @Override
        public int getTimezoneRawOffset() {
            return -5*ONE_HOUR;
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            return false;
        }
        
    }
    
    static class AsiaShanghai extends PlatformTimezone {

        @Override
        public String getTimezoneId() {
            return "Asia/Shanghai";
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            return ONE_HOUR*8;
        }

        @Override
        public int getTimezoneRawOffset() {
            return ONE_HOUR*8;
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            return false;
        } 
    }
    
    static class Hongkong extends PlatformTimezone {

        @Override
        public String getTimezoneId() {
            return "Hongkong";
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            return ONE_HOUR*8;
        }

        @Override
        public int getTimezoneRawOffset() {
            return ONE_HOUR*8;
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            return false;
        } 
    }
    
    static class AmericaToronto extends PlatformTimezone {

        @Override
        public String getTimezoneId() {
            return "America/Toronto";
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            JSDate d = TTimeZone.createJSDate(year, month, day, 0, 0, 0, 0);
            return getTimezoneRawOffset() + (isTimezoneDST((long)d.getTime())?ONE_HOUR:0);
        }

        @Override
        public int getTimezoneRawOffset() {
            return -ONE_HOUR*5;
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            JSDate d = TTimeZone.createJSDate(millis);
            // This is a very crude approximation that is WRONG but will allow
            // tests to pass
            System.out.println("Checking isTimezoneDST for America/Toronto");
            System.out.println("Month is "+d.getMonth());
            System.out.println("Time is "+d.getTime());
            return d.getMonth()>2 && d.getMonth()<10;
        }
        
    }
    
    static class AustraliaLordHowe extends PlatformTimezone {

        @Override
        public String getTimezoneId() {
            return "Australia/Lord_Howe";
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            JSDate d = TTimeZone.createJSDate(year, month, day, 0, 0, 0, 0);
            return getTimezoneRawOffset() + (isTimezoneDST((long)d.getTime())?ONE_HOUR/2:0);
        }

        @Override
        public int getTimezoneRawOffset() {
            return ONE_HOUR*10 + ONE_HOUR/2;
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            JSDate d = TTimeZone.createJSDate(millis);
            // This is a very crude approximation that is WRONG but will allow
            // tests to pass
            System.out.println("Checking isTimezoneDST for Australia");
            System.out.println("Month is "+d.getMonth());
            System.out.println("Time is "+d.getTime());
            return !(d.getMonth()>=3 && d.getMonth()<9);
        }
        
    }
    
    private static class PlatformSupportTimezone extends PlatformTimezone {
        private String id;
        private long offset;
        private boolean dst;
        
        PlatformSupportTimezone(String id, long offset, boolean dst) {
            this.id=id;
            this.offset=offset;
            this.dst=dst;
        }
        
        @Override
        public String getTimezoneId() {
            return id;
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            JSDate d = TTimeZone.createJSDate(year, month, day, 0, 0, 0, 0);
            return (int)(offset + (isTimezoneDST((long)d.getTime())?ONE_HOUR:0));
        }

        @Override
        public int getTimezoneRawOffset() {
            return (int)offset;
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            if (!dst) {
                return false;
            }
            JSDate d = TTimeZone.createJSDate(millis);
            if (d.getMonth() > 4 && d.getMonth() < 10) {
                return true;
            }
            return false;
        }
    }
    
    private static TimeZone newSupportTimeZone(int rawOffset, boolean useDaylightTime) {
        String id = "Support_TimeZone+"+rawOffset+(useDaylightTime?"DST":"");
        if (PlatformTimezone.getTimezone(id)==null) {
            PlatformTimezone.addTimezone(id, new PlatformSupportTimezone(id, rawOffset, useDaylightTime));
        }
        return TimeZone.getTimeZone(id);
    }
    

    static {
        PlatformTimezone.addTimezone("EST", new EST());
        PlatformTimezone.addTimezone("Asia/Shanghai", new AsiaShanghai());
        PlatformTimezone.addTimezone("Hongkong", new Hongkong());
        PlatformTimezone.addTimezone("America/Toronto", new AmericaToronto());
        PlatformTimezone.addTimezone("Australia/Lord_Howe", new AustraliaLordHowe());
    }
    
    /**
     * @tests java.util.TimeZone#getDefault()
     */
    //@Test
    //public void test_getDefault() {
    //    assertNotSame("returns identical",
    //            TimeZone.getDefault(), TimeZone.getDefault());
    //}

    /**
     * @tests java.util.TimeZone#getDSTSavings()
     */
    @Test
    public void test_getDSTSavings() {
		// Test for method int java.util.TimeZone.getDSTSavings()

        // test on subclass SimpleTimeZone
        TimeZone st1 = TimeZone.getTimeZone("EST");
        assertEquals("T1A. Incorrect daylight savings returned",
                0, st1.getDSTSavings());

        // a SimpleTimeZone with daylight savings different then 1 hour
        st1 = TimeZone.getTimeZone("Australia/Lord_Howe");
        assertEquals("T1B. Incorrect daylight savings returned",
                1800000, st1.getDSTSavings());

        // test on subclass Support_TimeZone, an instance with daylight savings
        TimeZone tz1 = newSupportTimeZone(-5 * ONE_HOUR, true);
        assertEquals("T2. Incorrect daylight savings returned",
                ONE_HOUR, tz1.getDSTSavings());

        // an instance without daylight savings
        tz1 = newSupportTimeZone(3 * ONE_HOUR, false);
        assertEquals("T3. Incorrect daylight savings returned, ",
                0, tz1.getDSTSavings());
    }

    /**
     * @tests java.util.TimeZone#getOffset(long)
     */
    @Test
    public void test_getOffset_long() {
		// Test for method int java.util.TimeZone.getOffset(long time)

        // test on subclass SimpleTimeZone
        TimeZone st1 = TimeZone.getTimeZone("EST");
        long time1 = new GregorianCalendar(1998, Calendar.NOVEMBER, 11)
                .getTimeInMillis();
        assertEquals("T1. Incorrect offset returned",
                -(5 * ONE_HOUR), st1.getOffset(time1));

        long time2 = new GregorianCalendar(1998, Calendar.JUNE, 11)
                .getTimeInMillis();
        st1 = TimeZone.getTimeZone("EST");
        assertEquals("T2. Incorrect offset returned",
                -(5 * ONE_HOUR), st1.getOffset(time2));

        // test on subclass Support_TimeZone, an instance with daylight savings
        TimeZone tz1 = newSupportTimeZone(-5 * ONE_HOUR, true);
        assertEquals("T3. Incorrect offset returned, ",
                -(5 * ONE_HOUR), tz1.getOffset(time1));
        assertEquals("T4. Incorrect offset returned, ",
                -(4 * ONE_HOUR), tz1.getOffset(time2));

        // an instance without daylight savings
        tz1 = newSupportTimeZone(3 * ONE_HOUR, false);
        assertEquals("T5. Incorrect offset returned, ",
                (3 * ONE_HOUR), tz1.getOffset(time1));
        assertEquals("T6. Incorrect offset returned, ",
                (3 * ONE_HOUR), tz1.getOffset(time2));
    }

    /**
     * @tests java.util.TimeZone#getTimeZone(java.lang.String)
     */
    @Test
    public void test_getTimeZoneLjava_lang_String() {
        assertEquals("Must return GMT when given an invalid TimeZone id SMT-8.",
                "GMT", TimeZone.getTimeZone("SMT-8").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+28:70.",
                "GMT", TimeZone.getTimeZone("GMT+28:70").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+28:30.",
                "GMT", TimeZone.getTimeZone("GMT+28:30").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+8:70.",
                "GMT", TimeZone.getTimeZone("GMT+8:70").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+3:.",
                "GMT", TimeZone.getTimeZone("GMT+3:").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+3:0.",
                "GMT", TimeZone.getTimeZone("GMT+3:0").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+2360.",
                "GMT", TimeZone.getTimeZone("GMT+2360").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+892.",
                "GMT", TimeZone.getTimeZone("GMT+892").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+082.",
                "GMT", TimeZone.getTimeZone("GMT+082").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+28.",
                "GMT", TimeZone.getTimeZone("GMT+28").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT+30.",
                "GMT", TimeZone.getTimeZone("GMT+30").getID());
        assertEquals("Must return GMT when given TimeZone GMT.",
                "GMT", TimeZone.getTimeZone("GMT").getID());
        assertEquals("Must return GMT when given TimeZone GMT+.",
                "GMT", TimeZone.getTimeZone("GMT+").getID());
        assertEquals("Must return GMT when given TimeZone GMT-.",
                "GMT", TimeZone.getTimeZone("GMT-").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT-8.45.",
                "GMT", TimeZone.getTimeZone("GMT-8.45").getID());
        assertEquals("Must return GMT when given an invalid TimeZone time GMT-123:23.",
                "GMT", TimeZone.getTimeZone("GMT-123:23").getID());
        assertEquals("Must return proper GMT formatted string for GMT+8:30 (eg. GMT+08:20).",
                "GMT+08:30", TimeZone.getTimeZone("GMT+8:30").getID());
        assertEquals("Must return proper GMT formatted string for GMT+3 (eg. GMT+08:20).",
                "GMT+03:00", TimeZone.getTimeZone("GMT+3").getID());
        assertEquals("Must return proper GMT formatted string for GMT+3:02 (eg. GMT+08:20).",
                "GMT+03:02", TimeZone.getTimeZone("GMT+3:02").getID());
        assertEquals("Must return proper GMT formatted string for GMT+2359 (eg. GMT+08:20).",
                "GMT+23:59", TimeZone.getTimeZone("GMT+2359").getID());
        assertEquals("Must return proper GMT formatted string for GMT+520 (eg. GMT+08:20).",
                "GMT+05:20", TimeZone.getTimeZone("GMT+520").getID());
        assertEquals("Must return proper GMT formatted string for GMT+052 (eg. GMT+08:20).",
                "GMT+00:52", TimeZone.getTimeZone("GMT+052").getID());
        // GMT-0 is an available ID in ICU, so replace it with GMT-00
        assertEquals("Must return proper GMT formatted string for GMT-00 (eg. GMT+08:20).",
                "GMT-00:00", TimeZone.getTimeZone("GMT-00").getID());
    }

    /**
     * @tests java.util.TimeZone#setDefault(java.util.TimeZone)
     */
    @Test
    public void test_setDefaultLjava_util_TimeZone() {
        TimeZone oldDefault = TimeZone.getDefault();
        TimeZone zone = new SimpleTimeZone(45, "TEST");
        TimeZone.setDefault(zone);
        assertEquals("timezone not set", zone, TimeZone.getDefault());
        TimeZone.setDefault(null);
        assertEquals("default not restored",
                oldDefault, TimeZone.getDefault());
    }

    /**
     * @tests java.util.TimeZone#getDisplayName(java.util.Locale)
     */
    //@Test
    //public void test_getDisplayNameLjava_util_Locale() {
    //    TimeZone timezone = TimeZone.getTimeZone("Asia/Shanghai");
    //    assertEquals("\u4e2d\u56fd\u6807\u51c6\u65f6\u95f4", timezone
    //            .getDisplayName(Locale.CHINA));
    // }

    /**
     * @tests java.util.TimeZone#getDisplayName(boolean, int, java.util.Locale)
     */
    //@Test
    //public void test_getDisplayNameZILjava_util_Locale() {
    //    TimeZone timezone = TimeZone.getTimeZone("Asia/Shanghai");
    //    assertEquals("\u683c\u6797\u5c3c\u6cbb\u6807\u51c6\u65f6\u95f4+0800",
    //            timezone.getDisplayName(false, TimeZone.SHORT, Locale.CHINA));
    //    try {
    //        timezone.getDisplayName(false, 100, Locale.CHINA);
    //        fail("should throw IllegalArgumentException");
    //    } catch (IllegalArgumentException e) {
    //        // expected
    //    }
    //}

    /*
     * Regression for HARMONY-5860
     */
    @Test
    public void test_GetTimezoneOffset() {
        // America/Toronto is lazy initialized 
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"));
        Date date = new Date(07, 2, 24);
        assertEquals(300, date.getTimezoneOffset());
        date = new Date(99, 8, 1);
        assertEquals(240, date.getTimezoneOffset());
    }

    protected void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(PlatformTimezone.getPlatformTimezoneId()));
    }

    protected void tearDown() {
        TimeZone.setDefault(TimeZone.getTimeZone(PlatformTimezone.getPlatformTimezoneId()));
    }

    /**
     * @add test {@link java.util.TimeZone#getAvailableIDs(int)}
     */
    @Test
    public void test_getAvailableIDs_I() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        int rawoffset = tz.getRawOffset();
        String[] ids = TimeZone.getAvailableIDs(rawoffset);
        List<String> idList = Arrays.asList(ids);
        assertTrue("Asia/shanghai and Hongkong should have the same rawoffset",
                idList.contains("Hongkong"));
    }

    /**
     * @add test {@link java.util.TimeZone#getDisplayName()}
     */
    //@Test
    //public void test_getDisplayName() {
    //    TimeZone defaultZone = TimeZone.getDefault();
    //    Locale defaulLocal = Locale.getDefault();
    //    String defaultName = defaultZone.getDisplayName();
    //    String expectedName = defaultZone.getDisplayName(defaulLocal);
    //    assertEquals(
    //            "getDispalyName() did not return the default Locale suitable name",
    //            expectedName, defaultName);
    //}

    /**
     * @add test {@link java.util.TimeZone#getDisplayName(boolean, int)}
     */
    //@Test
    //public void test_getDisplayName_ZI() {
    //    TimeZone defaultZone = TimeZone.getDefault();
    //    Locale defaultLocale = Locale.getDefault();
    //    String actualName = defaultZone.getDisplayName(false, TimeZone.LONG);
    //    String expectedName = defaultZone.getDisplayName(false, TimeZone.LONG,
    //            defaultLocale);
    //    assertEquals(
    //            "getDisplayName(daylight,style) did not return the default locale suitable name",
    //            expectedName, actualName);
    //}

    /**
     * @add test {@link java.util.TimeZone#hasSameRules(TimeZone)}
     */
    //@Test
    //public void test_hasSameRules_Ljava_util_TimeZone() {
    //    TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
    //    int offset = tz.getRawOffset();
    //
    //    String[] ids = TimeZone.getAvailableIDs(offset);
    //    int i = 0;
    //    if (ids.length != 0) {
    //        while (true) {
    //            if (!(ids[i].equalsIgnoreCase(tz.getID()))) {
    //                TimeZone sameZone = TimeZone.getTimeZone(ids[i]);
    //                assertTrue(tz.hasSameRules(sameZone));
    //                break;
    //            } else {
    //                i++;
    //            }
    //        }
    //    }
    //    assertFalse("should return false when parameter is null", tz
    //            .hasSameRules(null));
    //}
}
