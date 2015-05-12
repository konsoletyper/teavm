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

import org.joda.time.DateTimeZone;
import org.teavm.classlib.impl.Base46;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
public class TimeZoneGenerator implements MetadataGenerator {
    private static int[] divisors = { 60_000, 300_000, 1800_000, 3600_000 };
    public static int RESOLUTION_MINUTE = 0;
    public static int RESOLUTION_5_MINUTES = 1;
    public static int RESOLUTION_HALF_HOUR = 2;
    public static int RESOLUTION_HOUR = 3;


    @Override
    public ResourceMap<ResourceMap<TimeZoneResource>> generateMetadata(
            MetadataGeneratorContext context, MethodReference method) {
        ResourceMap<ResourceMap<TimeZoneResource>> result = context.createResourceMap();
        for (String id : DateTimeZone.getAvailableIDs()) {
            int sepIndex = id.indexOf('/');
            String areaName = id.substring(0, sepIndex);
            String locationName = id.substring(sepIndex + 1);
            ResourceMap<TimeZoneResource> area = result.get(areaName);
            if (area == null) {
                area = context.createResourceMap();
                result.put(areaName, area);
            }

            DateTimeZone tz = DateTimeZone.forID(id);
            TimeZoneResource tzRes = context.createResource(TimeZoneResource.class);
            tzRes.setAbbreviation(locationName);
            tzRes.setData(encodeData(tz));
            area.put(locationName, tzRes);
        }

        return result;
    }

    public String encodeData(DateTimeZone tz) {
        // Find resolution
        int resolution = RESOLUTION_HOUR;
        long current = 0;
        long offset = tz.getOffset(0);
        while (true) {
            long next = tz.nextTransition(current);
            if (next == current) {
                break;
            }
            current = next;

            int nextOffset = tz.getOffset(next);
            if (nextOffset == offset) {
                continue;
            }

            offset = nextOffset;
            resolution = getResolution(resolution, current);
            resolution = getResolution(resolution, offset);
            if (resolution == 0) {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        Base46.encode(sb, resolution);

        current = 0;
        offset = tz.getOffset(0);
        int divisor = divisors[resolution];
        long last = 0;
        long lastOffset = offset / divisor;
        Base46.encode(sb, lastOffset);
        while (true) {
            long next = tz.nextTransition(current);
            if (next == current) {
                break;
            }
            current = next;

            int nextOffset = tz.getOffset(next);
            if (nextOffset == offset) {
                continue;
            }

            offset = nextOffset;
            long newTime = current / divisor;
            long newOffset = offset / divisor;
            Base46.encodeUnsigned(sb, newTime - last);
            Base46.encode(sb, newOffset - lastOffset);
            last = newTime;
            lastOffset = newOffset;
        }

        return sb.toString();
    }

    private int getResolution(int currentResolution, long value) {
        while (currentResolution > 0 && value % divisors[currentResolution] != 0) {
            --currentResolution;
        }
        return currentResolution;
    }
}
