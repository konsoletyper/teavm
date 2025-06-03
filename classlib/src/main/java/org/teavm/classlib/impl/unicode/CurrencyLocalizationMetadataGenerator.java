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
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;

public class CurrencyLocalizationMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        CLDRReader reader = context.getService(CLDRReader.class);
        var map = new ResourceMapBuilder<ResourceMapBuilder<CurrencyLocalizationBuilder>>();
        for (var localeEntry : reader.getKnownLocales().entrySet()) {
            CLDRLocale locale = localeEntry.getValue();
            var currencies = new ResourceMapBuilder<CurrencyLocalizationBuilder>();
            map.values.put(localeEntry.getKey(), currencies);
            for (var currencyEntry : locale.getCurrencies().entrySet()) {
                CLDRCurrency currency = currencyEntry.getValue();
                var localization = new CurrencyLocalizationBuilder();
                localization.name = currency.getName();
                localization.symbol = currency.getSymbol() != null ? currency.getSymbol() : "";
                currencies.values.put(currencyEntry.getKey(), localization);
            }
        }
        return map;
    }
}
