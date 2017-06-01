/*
 *  Copyright 2014 Alexey Andreev.
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

public class DateSymbolsMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "getErasMap":
                return generateSymbols(context, locale -> locale.getEras());
            case "getAmPmMap":
                return generateSymbols(context, locale -> locale.getDayPeriods());
            case "getMonthMap":
                return generateSymbols(context, locale -> locale.getMonths());
            case "getShortMonthMap":
                return generateSymbols(context, locale -> locale.getShortMonths());
            case "getWeekdayMap":
                return generateSymbols(context, locale -> locale.getWeekdays());
            case "getShortWeekdayMap":
                return generateSymbols(context, locale -> locale.getShortWeekdays());
            default:
                throw new AssertionError("Unsupported method: " + method);
        }
    }

    private Resource generateSymbols(MetadataGeneratorContext context, ResourceExtractor extractor) {
        CLDRReader reader = context.getService(CLDRReader.class);
        ResourceMap<ResourceArray<StringResource>> result = context.createResourceMap();
        for (Map.Entry<String, CLDRLocale> localeEntry : reader.getKnownLocales().entrySet()) {
            ResourceArray<StringResource> symbolsRes = context.createResourceArray();
            result.put(localeEntry.getKey(), symbolsRes);
            for (String symbol : extractor.extract(localeEntry.getValue())) {
                StringResource symbolRes = context.createResource(StringResource.class);
                symbolRes.setValue(symbol);
                symbolsRes.add(symbolRes);
            }
        }
        return result;
    }

    private interface ResourceExtractor {
        String[] extract(CLDRLocale locale);
    }
}
