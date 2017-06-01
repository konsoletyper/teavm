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
package org.teavm.classlib.impl.unicode;

import java.util.Map;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.*;

public class TimeZoneLocalizationGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        CLDRReader cldr = context.getService(CLDRReader.class);
        ResourceMap<TimeZoneLocalization> localizations = context.createResourceMap();
        for (Map.Entry<String, CLDRLocale> locale : cldr.getKnownLocales().entrySet()) {
            TimeZoneLocalization localization = context.createResource(TimeZoneLocalization.class);
            ResourceMap<ResourceMap<StringResource>> map = context.createResourceMap();
            localization.setTimeZones(map);
            localizations.put(locale.getKey(), localization);

            for (CLDRTimeZone tz : locale.getValue().getTimeZones()) {
                ResourceMap<StringResource> area;
                if (!map.has(tz.getArea())) {
                    area = context.createResourceMap();
                    map.put(tz.getArea(), area);
                }
                area = map.get(tz.getArea());
                StringResource name = context.createResource(StringResource.class);
                name.setValue(tz.getName());
                area.put(tz.getLocation(), name);
            }
        }
        return localizations;
    }
}
