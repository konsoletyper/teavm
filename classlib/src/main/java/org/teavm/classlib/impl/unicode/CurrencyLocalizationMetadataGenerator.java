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
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceMap;

public class CurrencyLocalizationMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        CLDRReader reader = context.getService(CLDRReader.class);
        ResourceMap<ResourceMap<CurrencyLocalization>> map = context.createResourceMap();
        for (Map.Entry<String, CLDRLocale> localeEntry : reader.getKnownLocales().entrySet()) {
            CLDRLocale locale = localeEntry.getValue();
            ResourceMap<CurrencyLocalization> currencies = context.createResourceMap();
            map.put(localeEntry.getKey(), currencies);
            for (Map.Entry<String, CLDRCurrency> currencyEntry : locale.getCurrencies().entrySet()) {
                CLDRCurrency currency = currencyEntry.getValue();
                CurrencyLocalization localization = context.createResource(CurrencyLocalization.class);
                localization.setName(currency.getName());
                localization.setSymbol(currency.getSymbol() != null ? currency.getSymbol() : "");
                currencies.put(currencyEntry.getKey(), localization);
            }
        }
        return map;
    }
}
