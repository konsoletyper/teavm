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

import org.teavm.platform.metadata.MetadataProvider;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
public class TimeZoneResourceProvider {
    public static TimeZoneResource getTimeZone(String id) {
        int sepIndex = id.indexOf('/');
        String areaName = id.substring(0, sepIndex);
        String locationName = id.substring(sepIndex + 1);
        ResourceMap<TimeZoneResource> area = getResource().get(areaName);
        if (area == null) {
            return null;
        }
        return area.get(locationName);
    }

    @MetadataProvider(TimeZoneGenerator.class)
    private static native ResourceMap<ResourceMap<TimeZoneResource>> getResource();
}
