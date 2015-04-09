/*
 * Copyright 2015 shannah.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.teavm.jso.JSBody;
import org.teavm.platform.PlatformTimezone;

/**
 * TimeZone represents a time zone offset, and also figures out daylight savings.
 * Typically, you get a TimeZone using getDefault which creates a TimeZone based on the time zone where the program is running. For example, for a program running in Japan, getDefault creates a TimeZone object based on Japanese Standard Time.
 * You can also get a TimeZone using getTimeZone along with a time zone ID. For instance, the time zone ID for the Pacific Standard Time zone is "PST". So, you can get a PST TimeZone object with:
 * This class is a pure subset of the java.util.TimeZone class in JDK 1.3.
 * The only time zone ID that is required to be supported is "GMT".
 * Apart from the methods and variables being subset, the semantics of the getTimeZone() method may also be subset: custom IDs such as "GMT-8:00" are not required to be supported.
 * Version: CLDC 1.1 02/01/2002 (Based on JDK 1.3) See Also:Calendar, Date
 */
public abstract class TTimeZone {

    
    public static class GMT extends PlatformTimezone {

        @Override
        public String getTimezoneId() {
            return "GMT";
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            return 0;
        }

        @Override
        public int getTimezoneRawOffset() {
            return 0;
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            return false;
        }
        
    }
    
    public static class Local extends PlatformTimezone {

        @Override
        public String getTimezoneId() {
            return "Local";
        }

        @Override
        public int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis) {
            int hours = (int)Math.floor(timeOfDayMillis/1000/60/60);
            int minutes = (int)Math.floor(timeOfDayMillis/1000/60)%60;
            int seconds = (int)Math.floor(timeOfDayMillis/1000)%60;
            TDate d = new TDate(year, month, day, hours, minutes, seconds);
            return -TDate.getTimezoneOffset(d.getTime()) * 1000 * 60;
        }

        @Override
        public int getTimezoneRawOffset() {
            TDate now = new TDate();
            TDate jan = new TDate(now.getYear(), 0, 1);
            TDate jul = new TDate(now.getYear(), 6, 1);
            if (isTimezoneDST(jan.getTime())){
                return jul.getTimezoneOffset();
            } else {
                return jan.getTimezoneOffset();
            }
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            
            TDate now = new TDate();
            TDate jan = new TDate(now.getYear(), 0, 1);
            TDate jul = new TDate(now.getYear(), 6, 1);
            int maxOffset = Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset());
            return new Date(millis).getTimezoneOffset()<maxOffset;
        }
        
    }
    
    static {
        PlatformTimezone.addTimezone("GMT", new GMT());
        PlatformTimezone.addTimezone("Local", new Local());
        PlatformTimezone.setPlatformTimezoneId("Local");
    }
    /**
     * The short display name style, such as {@code PDT}. Requests for this
     * style may yield GMT offsets like {@code GMT-08:00}.
     */
    public static final int SHORT = 0;
    
    /**
     * The long display name style, such as {@code Pacific Daylight Time}.
     * Requests for this style may yield GMT offsets like {@code GMT-08:00}.
     */
    public static final int LONG = 1;
    
    static final TTimeZone GMT = new TSimpleTimeZone(0, "GMT"); // Greenwich Mean Time
    
    private static TTimeZone defaultTimeZone;
    
    private String ID;

    public TTimeZone(){         
    }

    void setID(String id) {
        ID = id;
    }
    
    /**
     * Gets all the available IDs supported.
     */
    public static java.lang.String[] getAvailableIDs(){
        return PlatformTimezone.getAvailableIds();
    }
    
    public static java.lang.String[] getAvailableIDs(int rawOffset){
        List<String> out = new ArrayList<String>();
        for (String id : getAvailableIDs()){
            PlatformTimezone tz = PlatformTimezone.getTimezone(id);
            if (tz.getTimezoneRawOffset()==rawOffset){
                out.add(id);
            }
        }
        return out.toArray(new String[out.size()]);
    }
    private static String getTimezoneId(){
        return PlatformTimezone.getPlatformTimezoneId();
    }
    
    
    private static int getTimezoneOffset(String name, int year, int month, int day, int timeOfDayMillis){
        PlatformTimezone tz = PlatformTimezone.getTimezone(name);
        if (tz==null){
            throw new RuntimeException("Timezone not found: "+name);
        }
        return tz.getTimezoneOffset(year, month, day, timeOfDayMillis);
    }
    
    private static int getTimezoneRawOffset(String name){
        PlatformTimezone tz = PlatformTimezone.getTimezone(name);
        if (tz==null){
            throw new RuntimeException("Timezone not found: "+name);
        }
        return tz.getTimezoneRawOffset();
    }
    
    private static boolean isTimezoneDST(String name, long millis){
        PlatformTimezone tz = PlatformTimezone.getTimezone(name);
        if (tz==null){
            throw new RuntimeException("Timezone not found: "+name);
        }
        return tz.isTimezoneDST(millis);
    }

    /**
     * Gets the default TimeZone for this host. The source of the default TimeZone may vary with implementation.
     */
    public static TTimeZone getDefault(){
        if (defaultTimeZone == null) {
            final String tzone = getTimezoneId();
            defaultTimeZone = new TTimeZone() {
                @Override
                public int getOffset(int era, int year, int month, int day, int dayOfWeek, int timeOfDayMillis) {
                    return getTimezoneOffset(tzone, year, month + 1, day, timeOfDayMillis);
                }

                @Override
                public int getRawOffset() {
                    return getTimezoneRawOffset(tzone);
                }

                boolean inDaylightTime(TDate time) {
                    return isTimezoneDST(tzone, time.getTime());
                }

                @Override
                public boolean useDaylightTime() {
                    return true;
                }
            };
            defaultTimeZone.ID = tzone;
        }
        return defaultTimeZone;
    }
    
    public void setDefault(TTimeZone tz){
        defaultTimeZone=tz;
    }

    int getDSTSavings() {
        return useDaylightTime() ? 3600000 : 0;
    }
    
    
    boolean inDaylightTime(TDate time) {
        return false;
    }
    
    /**
     * Gets the ID of this time zone.
     */
    public java.lang.String getID(){
        return ID;
    }

    public int getOffset(long millis){
        Date d = new Date(millis);
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        
        return getOffset(0, d.getYear(), d.getMonth(), d.getDate(), d.getDay(), (int)(millis-d.getTime()));
    }
    
    /**
     * Gets offset, for current date, modified in case of daylight savings. This is the offset to add *to* GMT to get local time. Gets the time zone offset, for current date, modified in case of daylight savings. This is the offset to add *to* GMT to get local time. Assume that the start and end month are distinct. This method may return incorrect results for rules that start at the end of February (e.g., last Sunday in February) or the beginning of March (e.g., March 1).
     */
    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis);

    /**
     * Gets the GMT offset for this time zone.
     */
    public abstract int getRawOffset();

    /**
     * Gets the TimeZone for the given ID.
     */
    public static TTimeZone getTimeZone(java.lang.String ID){
        // TODO
        return getDefault();
    }

    /**
     * Queries if this time zone uses Daylight Savings Time.
     */
    public abstract boolean useDaylightTime();

}


