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
package org.teavm.classlib.java.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.classlib.impl.currency.CurrencyHelper;
import org.teavm.classlib.impl.currency.CurrencyResource;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.impl.unicode.CurrencyLocalization;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.metadata.StringResource;

public final class TCurrency implements TSerializable {
    private static Map<String, TCurrency> currencies;
    private CurrencyResource resource;

    private TCurrency(CurrencyResource resource) {
        this.resource = resource;
    }

    private static void initCurrencies() {
        if (currencies != null) {
            return;
        }
        currencies = new HashMap<>();
        ResourceArray<CurrencyResource> resources = CurrencyHelper.getCurrencies();
        for (int i = 0; i < resources.size(); ++i) {
            CurrencyResource resource = resources.get(i);
            currencies.put(resource.getCode(), new TCurrency(resource));
        }
    }

    public static TCurrency getInstance(String currencyCode) {
        if (currencyCode == null) {
            throw new NullPointerException();
        }
        initCurrencies();
        TCurrency currency = currencies.get(currencyCode);
        if (currency == null) {
            throw new IllegalArgumentException("Currency not found: " + currencyCode);
        }
        return currency;
    }

    public static TCurrency getInstance(TLocale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        String coutry = CLDRHelper.resolveCountry(locale.getLanguage(), locale.getCountry());
        ResourceMap<StringResource> countryMap = CurrencyHelper.getCountryToCurrencyMap();
        if (!countryMap.has(coutry)) {
            return null;
        }
        return getInstance(countryMap.get(coutry).getValue());
    }

    public static Set<TCurrency> getAvailableCurrencies() {
        initCurrencies();
        return new HashSet<>(currencies.values());
    }

    public String getCurrencyCode() {
        return resource.getCode();
    }

    public String getSymbol() {
        return getSymbol(TLocale.getDefault());
    }

    public String getSymbol(TLocale locale) {
        CurrencyLocalization localization = CLDRHelper.resolveCurrency(locale.getLanguage(), locale.getCountry(),
                getCurrencyCode());
        return localization != null && !localization.getSymbol().isEmpty()
                ? localization.getSymbol() : getCurrencyCode();
    }

    public int getDefaultFractionDigits() {
        return resource.getFractionDigits();
    }

    public int getNumericCode() {
        return resource.getNumericCode();
    }

    public String getDisplayName() {
        return getDisplayName(TLocale.getDefault());
    }

    public String getDisplayName(TLocale locale) {
        CurrencyLocalization localization = CLDRHelper.resolveCurrency(locale.getLanguage(), locale.getCountry(),
                getCurrencyCode());
        return localization != null ? localization.getName() : getCurrencyCode();
    }

    @Override
    public String toString() {
        return resource.getCode();
    }
}
