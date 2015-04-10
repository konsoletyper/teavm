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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static org.teavm.classlib.java.util.TGregorianCalendar.AD;
import static org.teavm.classlib.java.util.TGregorianCalendar.BC;
import org.teavm.jso.JS;
import org.teavm.jso.JSConstructor;
import org.teavm.jso.JSObject;
import org.teavm.platform.PlatformTimezone;

/**
 * TimeZone represents a time zone offset, and also figures out daylight savings.
 * Typically, you get a TimeZone using getDefault which creates a TimeZone based on the 
 * time zone where the program is running. For example, for a program running in Japan, 
 * getDefault creates a TimeZone object based on Japanese Standard Time.
 * You can also get a TimeZone using getTimeZone along with a time zone ID. For instance, 
 * the time zone ID for the Pacific Standard Time zone is "PST". So, you can get a PST 
 * TimeZone object with:
 * This class is a pure subset of the java.util.TimeZone class in JDK 1.3.
 * The only time zone ID that is required to be supported is "GMT".
 * Apart from the methods and variables being subset, the semantics of the getTimeZone()
 * method may also be subset: custom IDs such as "GMT-8:00" are not required to be supported.
 * Version: CLDC 1.1 02/01/2002 (Based on JDK 1.3) See Also:Calendar, Date
 */
public abstract class TTimeZone {
    
    // For the Local Timezone we need to use the Javascript Date object
    // Directly, so here is a makeshift JSO for it.  If this object already
    // has a JSO or it is deemed adventageous to move this to a more centralized
    // location, then refactor by all means.
    interface JSDate extends JSObject {
        int getDate();
        int getDay();
        int getFullYear();
        void setFullYear(int year);
        int getHours();
        int getMilliseconds();
        int getMinutes();
        int getMonth();
        int getSeconds();
        double getTime();
        int getTimezoneOffset();
        void setDate(int day);
    }
    
    interface JSDateFactory extends JSObject {
        @JSConstructor("Date")
        JSDate createDate();
        
        @JSConstructor("Date")
        JSDate createDate(double millis);
        
        @JSConstructor("Date")
        JSDate createDate(String dateString);
        
        @JSConstructor("Date")
        JSDate createDate(int year, int month, int day, int hours, int minutes, int seconds, int milliseconds);
    }
    
    static JSDate createJSDate() {
        return ((JSDateFactory)JS.getGlobal()).createDate();
    }
    
    static JSDate createJSDate(long millis) {
        return ((JSDateFactory)JS.getGlobal()).createDate((double)millis);
    }
    
    static JSDate createJSDate(String dateString) {
        return ((JSDateFactory)JS.getGlobal()).createDate(dateString);
    }
    
    static JSDate createJSDate(int year, int month, int day, int hours, int minutes, int seconds, int milliseconds) {
        JSDate out = ((JSDateFactory)JS.getGlobal()).createDate(year, month, day, hours, minutes, seconds, milliseconds);
        out.setFullYear(year);
        return out;
    }
    
    // End Private Javascript Date Object JSO stuff
    
    /**
     * A special "local" timezone that represents the timezone of the Javascript
     * environment.  Javascript doesn't allow us to see the name of this timezone.
     * We use this as the default platform timezone and set its ID as "Local".
     */
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
            JSDate d = createJSDate(year, month, day, hours, minutes, seconds, timeOfDayMillis % 1000);
            return d.getTimezoneOffset();
        }

        @Override
        public int getTimezoneRawOffset() {
            JSDate now = createJSDate();
            JSDate jan = createJSDate(now.getFullYear(), 0, 1, 0, 0, 0, 0);
            JSDate jul = createJSDate(now.getFullYear(), 6, 1, 0, 0, 0, 0);
            if (isTimezoneDST((long)jan.getTime())) {
                return jul.getTimezoneOffset();
            } else {
                return jan.getTimezoneOffset();
            }
        }

        @Override
        public boolean isTimezoneDST(long millis) {
            
            JSDate now = createJSDate();
            JSDate jan = createJSDate(now.getFullYear(), 0, 1, 0, 0, 0, 0);
            JSDate jul = createJSDate(now.getFullYear(), 6, 1, 0, 0, 0, 0);
            int maxOffset = Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset());
            return createJSDate(millis).getTimezoneOffset()<maxOffset;
        }
    }
    
    static {
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
    
    private static TTimeZone systemTimeZone;
    
    private String ID;

    public TTimeZone() {         
    }

    void setID(String id) {
        ID = id;
    }
    
    /**
     * Gets all the available IDs supported.
     */
    public static java.lang.String[] getAvailableIDs() {
        return PlatformTimezone.getAvailableIds();
    }
    
    public static java.lang.String[] getAvailableIDs(int rawOffset) {
        List<String> out = new ArrayList<String>();
        for (String id : getAvailableIDs()) {
            PlatformTimezone tz = PlatformTimezone.getTimezone(id);
            if (tz.getTimezoneRawOffset()==rawOffset) {
                out.add(id);
            }
        }
        return out.toArray(new String[out.size()]);
    }
    
    private static String getTimezoneId() {
        return PlatformTimezone.getPlatformTimezoneId();
    }
    
    private static int getTimezoneOffset(String name, int year, int month, int day, int timeOfDayMillis) {
        PlatformTimezone tz = PlatformTimezone.getTimezone(name);
        if (tz==null) {
            throw new RuntimeException("Timezone not found: "+name);
        }
        return tz.getTimezoneOffset(year, month, day, timeOfDayMillis);
    }
    
    private static int getTimezoneRawOffset(String name) {
        PlatformTimezone tz = PlatformTimezone.getTimezone(name);
        if (tz==null) {
            throw new RuntimeException("Timezone not found: "+name);
        }
        return tz.getTimezoneRawOffset();
    }
    
    private static boolean isTimezoneDST(String name, long millis) {
        PlatformTimezone tz = PlatformTimezone.getTimezone(name);
        if (tz==null) {
            throw new RuntimeException("Timezone not found: "+name);
        }
        return tz.isTimezoneDST(millis);
    }

    private static TTimeZone getSystemTimeZone() {
        if (systemTimeZone == null) {
            systemTimeZone = getTimeZone(PlatformTimezone.getPlatformTimezoneId());
        }
        return systemTimeZone;
    }
    
    /**
     * Gets the default TimeZone for this host. The source of the default TimeZone may vary with implementation.
     */
    public static TTimeZone getDefault() {
        if (defaultTimeZone == null) {
            defaultTimeZone = getSystemTimeZone();
        }
        return defaultTimeZone;
    }
    
    public static void setDefault(TTimeZone tz) {
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
    public java.lang.String getID() {
        return ID;
    }

    public int getOffset(long millis) {
        Date d = new Date(millis);
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        return getOffset(d.getYear()>=-1900?AD:BC, d.getYear()+1900, d.getMonth(), d.getDate(), d.getDay(), (int)(millis-d.getTime()));
    }
    
    /**
     * Gets offset, for current date, modified in case of daylight savings. This is the offset to add *to* GMT to get local time. Gets the time zone offset, for current date, modified in case of daylight savings. This is the offset to add *to* GMT to get local time. Assume that the start and end month are distinct. This method may return incorrect results for rules that start at the end of February (e.g., last Sunday in February) or the beginning of March (e.g., March 1).
     */
    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis);

    /**
     * Gets the GMT offset for this time zone.
     */
    public abstract int getRawOffset();

    private static String normalizeGMTOffset(String offset){
        int pos;
        int len = offset.length();
        if (len == 1){
            // Should be simple integer of hours 0-9
            char c = offset.charAt(0);
            if (c < '0' || c > '9') {
                return "";
            }
            return "0"+offset+":00";
        } else if (len == 2){
            // Should be a 2-digit representation of hours 00-23
            char c1 = offset.charAt(0);
            if (c1 < '0' || c1 > '2') {
                return "";   
            }
            char c2 = offset.charAt(1);
            if (c2 < '0' || (c1 == '2' && c2 > '3') || c2 > '9') {
                return "";
            }
            return offset+":00";
            
        } else if (len == 3) {
            char c1 = offset.charAt(0);
            if (c1 < '0' || c1 > '9') {
                return "";
            }
            
            char c2 = offset.charAt(1);
            if (c2 < '0' || c2 > '5') {
                return "";
            }
            
            char c3 = offset.charAt(2);
            if (c3 < '0' || c3 > '9') {
                return "";
            }
            
            return "0"+c1+":"+c2+c3;
        } else if (len == 4 && offset.charAt(1) == ':'){
            char c1 = offset.charAt(0);
            if (c1 < '0' || c1 > '9') {
                return "";   
            }
            char c2 = offset.charAt(2);
            if (c2 < '0' || c2 > '5') {
                return "";
            }
            
            char c3 = offset.charAt(3);
            if (c3 < '0' || c3 > '9') {
                return "";
            }
            
            return "0"+c1+":"+c2+c3;
        } else if (len==4) {
            char c1 = offset.charAt(0);
            if (c1 < '0' || c1 > '2') {
                return "";   
            }
            char c2 = offset.charAt(1);
            if (c2 < '0' || (c1 == '2' && c2 > '3') || c2 > '9') {
                return "";
            }
            
            char c3 = offset.charAt(2);
            if (c3 < '0' || c3 > '5') {
                return "";
            }
            
            char c4 = offset.charAt(3);
            if (c4 < '0' || c3 > '9') {
                return "";
            }
            
            return ""+c1+c2+":"+c3+c4;
        } else if (len == 5 && offset.charAt(2) == ':'){
            char c1 = offset.charAt(0);
            if (c1 < '0' || c1 > '2') {
                return "";   
            }
            char c2 = offset.charAt(1);
            if (c2 < '0' || (c1 == '2' && c2 > '3') || c2 > '9') {
                return "";
            }
            
            char c3 = offset.charAt(3);
            if (c3 < '0' || c3 > '5') {
                return "";
            }
            
            char c4 = offset.charAt(4);
            if (c4 < '0' || c3 > '9') {
                return "";
            }
            
            return ""+c1+c2+":"+c3+c4;
        } else {
            return "";
        }
        
    }
    
    /**
     * Gets the TimeZone for the given ID.
     */
    public static TTimeZone getTimeZone(java.lang.String ID) {
        if (PlatformTimezone.getTimezone(ID)!=null) {
            final TTimeZone tz = new TTimeZone() {
                
                private int dstSavings=-1;
                
                @Override
                public int getOffset(int era, int year, int month, int day, int dayOfWeek, int timeOfDayMillis) {
                    if (era==BC) {
                        year = -year;
                    }
                    return getTimezoneOffset(this.getID(), year, month, day, timeOfDayMillis);
                }

                @Override
                public int getRawOffset() {
                    return getTimezoneRawOffset(this.getID());
                }

                @Override
                boolean inDaylightTime(TDate time) {
                    return isTimezoneDST(this.getID(), time.getTime());
                }

                @Override
                public boolean useDaylightTime() {
                    TDate now = new TDate();
                    TDate jan = new TDate(now.getYear(), 0, 1);
                    TDate jul = new TDate(now.getYear(), 6, 1);
                    return inDaylightTime(jan) || inDaylightTime(jul);
                }
                
                @Override
                int getDSTSavings() {
                    if (useDaylightTime()) {
                        if (dstSavings==-1) {
                            TDate now = new TDate();
                            TDate jan = new TDate(now.getYear(), 0, 1);
                            TDate jul = new TDate(now.getYear(), 6, 1);
                            dstSavings = Math.abs(this.getOffset(jan.getTime())-this.getOffset(jul.getTime()));
                        }
                        return dstSavings;
                    }
                    return 0;
                }
            };
            tz.ID = ID;
            return tz;
        }
        if (ID.startsWith("GMT")) {
            if (ID.length()==3) {
                return GMT;
            } else if (ID.charAt(3) == '+' || ID.charAt(3) == '-') {
                String strOffset = ID.substring(4);
                String normalizedOffset = normalizeGMTOffset(strOffset);
                if (normalizedOffset == null || "".equals(normalizedOffset)) {
                    return GMT;
                }
                int hours = Integer.parseInt(normalizedOffset.substring(0,2));
                int minutes = Integer.parseInt(normalizedOffset.substring(3));
                int offset = hours * 60 * 60 * 1000 + minutes * 60 * 1000;
                if (ID.charAt(3) == '-') {
                    offset = -offset;
                }
                return new TSimpleTimeZone(offset, "GMT"+ID.charAt(3)+normalizedOffset);
            }
        }
        return GMT;
    }

    /**
     * Queries if this time zone uses Daylight Savings Time.
     */
    public abstract boolean useDaylightTime();
}


