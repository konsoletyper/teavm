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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
    private static final String tzPath = "org/teavm/classlib/impl/tz/tzdata2015d.zip";

    @Override
    public ResourceMap<ResourceMap<TimeZoneResource>> generateMetadata(
            MetadataGeneratorContext context, MethodReference method) {
        ResourceMap<ResourceMap<TimeZoneResource>> result = context.createResourceMap();
        ZoneInfoCompiler compiler = new ZoneInfoCompiler();
        try (InputStream input = context.getClassLoader().getResourceAsStream(tzPath)) {
            try (ZipInputStream zip = new ZipInputStream(input)) {
                while (true) {
                    ZipEntry entry = zip.getNextEntry();
                    if (entry == null) {
                        break;
                    }
                    switch (entry.getName().substring("tzdata2015d/".length())) {
                        case "africa":
                        case "antarctica":
                        case "asia":
                        case "australasia":
                        case "etcetera":
                        case "europe":
                        case "northamerica":
                        case "pacificnew":
                        case "southamerica":
                            compiler.parseDataFile(new BufferedReader(new InputStreamReader(zip, "UTF-8")), false);
                            break;
                        case "backward":
                        case "backzone":
                            compiler.parseDataFile(new BufferedReader(new InputStreamReader(zip, "UTF-8")), true);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generating time zones", e);
        }

        Map<String, StorableDateTimeZone> zoneMap = compiler.compile();
        Map<StorableDateTimeZone, String> zones = new HashMap<>();
        for (String id : zoneMap.keySet()) {
            int sepIndex = id.indexOf('/');
            String areaName;
            String locationName;
            if (sepIndex < 0) {
                areaName = "";
                locationName = id;
            } else {
                areaName = id.substring(0, sepIndex);
                locationName = id.substring(sepIndex + 1);
            }
            ResourceMap<TimeZoneResource> area = result.get(areaName);
            if (area == null) {
                area = context.createResourceMap();
                result.put(areaName, area);
            }

            StorableDateTimeZone tz = zoneMap.get(id);
            TimeZoneResource tzRes = context.createResource(TimeZoneResource.class);
            StringBuilder data = new StringBuilder();
            String knownId = zones.get(tz);
            if (knownId == null) {
                tz.write(data);
                zones.put(tz, id);
            } else {
                Base46.encode(data, StorableDateTimeZone.ALIAS);
                data.append(knownId);
            }
            tzRes.setData(data.toString());
            area.put(locationName, tzRes);
        }

        return result;
    }
}
