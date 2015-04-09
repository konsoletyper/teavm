/*
 *  Copyright 2015 Alexey Andreev.
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
    private static String platformTimezoneId;
    
    
    private static Map<String, PlatformTimezone> timezones;
    private static Map<String, PlatformTimezone> timezones(){
        if (timezones==null){
            timezones = new HashMap<String, PlatformTimezone>();
        }
        return timezones;
    }
    public static PlatformTimezone getTimezone(String id){
        return timezones().get(id);
    }
    
    public static void addTimezone(String id, PlatformTimezone tz){
        timezones().put(id, tz);
    }
    
    public static String[] getAvailableIds(){
        Set<String> keys = timezones().keySet();
        return keys.toArray(new String[keys.size()]);
    }
    
    public static void setPlatformTimezoneId(String id){
        platformTimezoneId=id;
    }
    
    public static String getPlatformTimezoneId(){
        return platformTimezoneId;
    }
    
    public abstract String getTimezoneId();
    public abstract int getTimezoneOffset(int year, int month, int day, int timeOfDayMillis);
    public abstract int getTimezoneRawOffset();
    public abstract boolean isTimezoneDST(long millis);
    
    
}
