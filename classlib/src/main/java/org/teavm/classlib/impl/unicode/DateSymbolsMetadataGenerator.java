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

import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.ResourceArrayBuilder;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;
import org.teavm.platform.metadata.builders.StringResourceBuilder;

public class DateSymbolsMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
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

    private ResourceBuilder generateSymbols(MetadataGeneratorContext context, ResourceExtractor extractor) {
        CLDRReader reader = context.getService(CLDRReader.class);
        var result = new ResourceMapBuilder<ResourceArrayBuilder<StringResourceBuilder>>();
        for (var localeEntry : reader.getKnownLocales().entrySet()) {
            var symbolsRes = new ResourceArrayBuilder<StringResourceBuilder>();
            result.values.put(localeEntry.getKey(), symbolsRes);
            for (String symbol : extractor.extract(localeEntry.getValue())) {
                var symbolRes = new StringResourceBuilder();
                symbolRes.value = symbol;
                symbolsRes.values.add(symbolRes);
            }
        }
        return result;
    }

    private interface ResourceExtractor {
        String[] extract(CLDRLocale locale);
    }
}
