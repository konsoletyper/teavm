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

import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.*;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;
import org.teavm.platform.metadata.builders.StringResourceBuilder;

public class TimeZoneLocalizationGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        CLDRReader cldr = context.getService(CLDRReader.class);
        var localizations = new ResourceMapBuilder<TimeZoneLocalizationBuilder>();
        for (var locale : cldr.getKnownLocales().entrySet()) {
            var localization = new TimeZoneLocalizationBuilder();
            localizations.values.put(locale.getKey(), localization);

            for (CLDRTimeZone tz : locale.getValue().getTimeZones()) {
                var area = localization.timeZones.values.get(tz.getArea());
                if (area == null) {
                    area = new ResourceMapBuilder<>();
                    localization.timeZones.values.put(tz.getArea(), area);
                }
                var name = new StringResourceBuilder();
                name.value = tz.getName();
                area.values.put(tz.getLocation(), name);
            }
        }
        return localizations;
    }
}
