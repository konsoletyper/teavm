/*
 *  Copyright 2015 Steve Hannah.
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
package org.teavm.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Allows for adding timezones to the runtime.
 * @author shannah
 */
public abstract class PlatformTimezone {
    
    /**
     * The default timezone ID.
     */
    private static String platformTimezoneId;
    
    /**
     * Map of registered timezones.  Maps IDs to TimeZones.
     */
    private static Map<String, PlatformTimezone> timezones;
    
    /**
     * Returns timezone map, lazily initialized.
     * @return 
     */
    private static Map<String, PlatformTimezone> timezones(){
        if (timezones==null){
            timezones = new HashMap<String, PlatformTimezone>();
        }
        return timezones;
    }
    
    /**
     * Gets a timezone with the specified ID.
     * @param id The ID of the timezone to retrieve.
     * @return The TimeZone or null if none exists with that name.
     */
    public static PlatformTimezone getTimezone(String id) {
        return timezones().get(id);
    }
    
    /**
     * Adds a TimeZone.
     * @param id The ID of the TimeZone.
     * @param tz The TimeZone to add.
     */
    public static void addTimezone(String id, PlatformTimezone tz) {
        timezones().put(id, tz);
    }
    
    /**
     * Gets a list of the available platform TimeZone IDs.
     * @return 
     */
    public static String[] getAvailableIds() {
        Set<String> keys = timezones().keySet();
        return keys.toArray(new String[keys.size()]);
    }
    
    /**
     * Sets the local TimeZone ID of the platform.  This will be used as the
     * "default" TimeZone.
     * @param id The ID of the platform timezone.
     */
    public static void setPlatformTimezoneId(String id) {
        platformTimezoneId=id;
    }
    
    /**
     * Gets the local TimeZone ID of the platform.  This will be used as the
     * "default" TimeZone.
     * @return The default TimeZone ID.
     */
    public static String getPlatformTimezoneId() {
        return platformTimezoneId;
    }
    
    /**
     * Gets the ID of the TimeZone. E.g. "EST", or "America/Toronto"
     * @return The TimeZone ID
     */
    public abstract String getTimezoneId();
    
    /**
     * Gets the timezone offset at the given date.  This will include any DST
     * offset.
     * @param year The year at which the calculation is made.  For BC dates, use negatives.
     * @param month The month.  (0-11)
     * @param day The day of the month (1-31).
     * @param timeOfDayMillis The time of the day in milliseconds.
     * @return The offset in milliseconds from GMT time.
     */
    public abstract int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis);
    
    /**
     * Gets the raw offset of the TimeZone.  This does not include any DST offsets.
     * @return The raw offset of the timezone.
     */
    public abstract int getTimezoneRawOffset();
    
    /**
     * Checks if the timezone is observing daylight savings time at the provided
     * date.
     * @param millis The unix timestamp in milliseconds.
     * @return True if the timezone is observing DST at the given date.
     */
    public abstract boolean isTimezoneDST(long millis);  
}
