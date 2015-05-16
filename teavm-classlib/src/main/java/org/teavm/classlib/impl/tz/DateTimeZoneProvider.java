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
package org.teavm.classlib.impl.tz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.classlib.impl.Base46;
import org.teavm.classlib.impl.CharFlow;
import org.teavm.platform.metadata.MetadataProvider;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
public class DateTimeZoneProvider {
    private static Map<String, DateTimeZone> cache = new HashMap<>();

    public static DateTimeZone getTimeZone(String id) {
        if (!cache.containsKey(id)) {
            cache.put(id, createTimeZone(id));
        }
        return cache.get(id);
    }

    private static DateTimeZone createTimeZone(String id) {
        TimeZoneResource res = getTimeZoneResource(id);
        if (res == null) {
            return null;
        }
        String data = res.getData();
        CharFlow flow = new CharFlow(data.toCharArray());
        if (Base46.decode(flow) == StorableDateTimeZone.ALIAS) {
            String aliasId = data.substring(flow.pointer);
            return getTimeZone(aliasId);
        } else {
            return StorableDateTimeZone.read(id, data);
        }
    }

    public static String[] getIds() {
        List<String> ids = new ArrayList<>();
        for (String areaName : getResource().keys()) {
            ResourceMap<TimeZoneResource> area = getResource().get(areaName);
            for (String id : area.keys()) {
                if (!areaName.isEmpty()) {
                    id = areaName + "/" + id;
                }
                ids.add(id);
            }
        }
        return ids.toArray(new String[ids.size()]);
    }

    private static TimeZoneResource getTimeZoneResource(String id) {
        String areaName;
        String locationName;
        int sepIndex = id.indexOf('/');
        if (sepIndex >= 0) {
            areaName = id.substring(0, sepIndex);
            locationName = id.substring(sepIndex + 1);
        } else {
            areaName = "";
            locationName = id;
        }
        ResourceMap<TimeZoneResource> area = getResource().get(areaName);
        if (area == null) {
            return null;
        }
        return area.get(locationName);
    }

    @MetadataProvider(TimeZoneGenerator.class)
    private static native ResourceMap<ResourceMap<TimeZoneResource>> getResource();
}
