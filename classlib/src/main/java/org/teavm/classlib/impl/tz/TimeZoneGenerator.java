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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.ResourceMap;

public class TimeZoneGenerator implements MetadataGenerator {
    public static final String TIMEZONE_DB_VERSION = "2021a";
    public static final String TIMEZONE_DB_PATH = "org/teavm/classlib/impl/tz/tzdata" + TIMEZONE_DB_VERSION + ".zip";

    public static void compile(ZoneInfoCompiler compiler, ClassLoader classLoader) {
        try (InputStream input = classLoader.getResourceAsStream(TIMEZONE_DB_PATH)) {
            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input))) {
                while (true) {
                    ZipEntry entry = zip.getNextEntry();
                    if (entry == null) {
                        break;
                    }
                    switch (entry.getName()) {
                        case "africa":
                        case "antarctica":
                        case "asia":
                        case "australasia":
                        case "etcetera":
                        case "europe":
                        case "northamerica":
                        case "pacificnew":
                        case "southamerica":
                            compiler.parseDataFile(new BufferedReader(
                                    new InputStreamReader(zip, StandardCharsets.UTF_8)), false);
                            break;
                        case "backward":
                        case "backzone":
                            compiler.parseDataFile(new BufferedReader(
                                    new InputStreamReader(zip, StandardCharsets.UTF_8)), true);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generating time zones", e);
        }
    }

    @Override
    public ResourceMap<ResourceMap<TimeZoneResource>> generateMetadata(
            MetadataGeneratorContext context, MethodReference method) {
        ResourceMap<ResourceMap<TimeZoneResource>> result = context.createResourceMap();
        Collection<StorableDateTimeZone> zones;
        try (InputStream input = context.getClassLoader().getResourceAsStream("org/teavm/classlib/impl/tz/cache")) {
            if (input != null) {
                TimeZoneCache cache = new TimeZoneCache();
                zones = cache.read(new BufferedInputStream(input)).values();
            } else {
                ZoneInfoCompiler compiler = new ZoneInfoCompiler();
                compile(compiler, context.getClassLoader());
                zones = compiler.compile().values();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generating time zones", e);
        }
        for (StorableDateTimeZone tz : zones) {
            String id = tz.getID();
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

            TimeZoneResource tzRes = context.createResource(TimeZoneResource.class);
            StringBuilder data = new StringBuilder();
            tz.write(data);
            tzRes.setData(data.toString());
            area.put(locationName, tzRes);
        }

        return result;
    }
}
