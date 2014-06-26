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

import org.teavm.classlib.impl.FirstDayOfWeekMetadataGenerator;
import org.teavm.classlib.impl.MinimalDaysInFirstWeekMetadataGenerator;
import org.teavm.platform.metadata.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class CLDRHelper {
    public static String getCode(String language, String country) {
        return !country.isEmpty() ? language + "-" + country : language;
    }

    public static String getLikelySubtags(String localeCode) {
        ResourceMap<StringResource> map = getLikelySubtagsMap();
        return map.has(localeCode) ? map.get(localeCode).getValue() : localeCode;
    }

    @MetadataProvider(LikelySubtagsMetadataGenerator.class)
    private static native ResourceMap<StringResource> getLikelySubtagsMap();

    public static String[] resolveEras(String language, String country) {
        return resolveDateFormatSymbols(getErasMap(), language, country);
    }

    @MetadataProvider(DateSymbolsMetadataGenerator.class)
    private static native ResourceMap<ResourceArray<StringResource>> getErasMap();

    public static String[] resolveAmPm(String language, String country) {
        return resolveDateFormatSymbols(getAmPmMap(), language, country);
    }

    @MetadataProvider(DateSymbolsMetadataGenerator.class)
    private static native ResourceMap<ResourceArray<StringResource>> getAmPmMap();

    public static String[] resolveMonths(String language, String country) {
        return resolveDateFormatSymbols(getMonthMap(), language, country);
    }

    @MetadataProvider(DateSymbolsMetadataGenerator.class)
    private static native ResourceMap<ResourceArray<StringResource>> getMonthMap();

    public static String[] resolveShortMonths(String language, String country) {
        return resolveDateFormatSymbols(getShortMonthMap(), language, country);
    }

    @MetadataProvider(DateSymbolsMetadataGenerator.class)
    private static native ResourceMap<ResourceArray<StringResource>> getShortMonthMap();

    public static String[] resolveWeekdays(String language, String country) {
        return resolveDateFormatSymbols(getWeekdayMap(), language, country);
    }

    @MetadataProvider(DateSymbolsMetadataGenerator.class)
    private static native ResourceMap<ResourceArray<StringResource>> getWeekdayMap();

    public static String[] resolveShortWeekdays(String language, String country) {
        return resolveDateFormatSymbols(getShortWeekdayMap(), language, country);
    }

    @MetadataProvider(DateSymbolsMetadataGenerator.class)
    private static native ResourceMap<ResourceArray<StringResource>> getShortWeekdayMap();

    private static String[] resolveDateFormatSymbols(ResourceMap<ResourceArray<StringResource>> map, String language,
            String country) {
        String localeCode = getCode(language, country);
        ResourceArray<StringResource> arrayRes = map.has(localeCode) ? map.get(localeCode) :
                map.has(language) ? map.get(language) : map.get("root");
        String[] result = new String[arrayRes.size()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = arrayRes.get(i).getValue();
        }
        return result;
    }

    @MetadataProvider(LanguageMetadataGenerator.class)
    public static native ResourceMap<ResourceMap<StringResource>> getLanguagesMap();

    @MetadataProvider(CountryMetadataGenerator.class)
    public static native ResourceMap<ResourceMap<StringResource>> getCountriesMap();

    @MetadataProvider(DefaultLocaleMetadataGenerator.class)
    public static native StringResource getDefaultLocale();

    @MetadataProvider(AvailableLocalesMetadataGenerator.class)
    public static native ResourceArray<StringResource> getAvailableLocales();

    @MetadataProvider(MinimalDaysInFirstWeekMetadataGenerator.class)
    public static native ResourceMap<IntResource> getMinimalDaysInFirstWeek();

    @MetadataProvider(FirstDayOfWeekMetadataGenerator.class)
    public static native ResourceMap<IntResource> getFirstDayOfWeek();

    public static DateFormatCollection resolveDateFormats(String language, String country) {
        return resolveDateFormats(getDateFormatMap(), language, country);
    }

    @MetadataProvider(DateFormatMetadataGenerator.class)
    private static native ResourceMap<DateFormatCollection> getDateFormatMap();

    public static DateFormatCollection resolveTimeFormats(String language, String country) {
        return resolveDateFormats(getTimeFormatMap(), language, country);
    }

    @MetadataProvider(DateFormatMetadataGenerator.class)
    private static native ResourceMap<DateFormatCollection> getTimeFormatMap();

    public static DateFormatCollection resolveDateTimeFormats(String language, String country) {
        return resolveDateFormats(getDateTimeFormatMap(), language, country);
    }

    @MetadataProvider(DateFormatMetadataGenerator.class)
    private static native ResourceMap<DateFormatCollection> getDateTimeFormatMap();

    public static String resolveNumberFormat(String language, String country) {
        return resolveFormatSymbols(getNumberFormatMap(), language, country);
    }

    private static native ResourceMap<StringResource> getNumberFormatMap();

    public static String resolveDecimalFormat(String language, String country) {
        return resolveFormatSymbols(getDecimalFormatMap(), language, country);
    }

    private static native ResourceMap<StringResource> getDecimalFormatMap();

    public static String resolvePercentFormat(String language, String country) {
        return resolveFormatSymbols(getPercentFormatMap(), language, country);
    }

    private static native ResourceMap<StringResource> getPercentFormatMap();

    private static DateFormatCollection resolveDateFormats(ResourceMap<DateFormatCollection> map,
            String language, String country) {
        String localeCode = getCode(language, country);
        return map.has(localeCode) ? map.get(localeCode) : map.has(language) ? map.get(language) : map.get("root");
    }

    private static String resolveFormatSymbols(ResourceMap<StringResource> map, String language, String country) {
        String localeCode = getCode(language, country);
        StringResource res = map.has(localeCode) ? map.get(localeCode) : map.has(language) ? map.get(language) :
                map.get("root");
        return res.getValue();
    }

    public static CLDRDecimalData resolveDecimalData(String language, String country) {
        ResourceMap<CLDRDecimalData> map = getDecimalDataMap();
        String localeCode = getCode(language, country);
        return map.has(localeCode) ? map.get(localeCode) : map.has(language) ? map.get(language) :
                map.get("root");
    }

    private static native ResourceMap<CLDRDecimalData> getDecimalDataMap();
}
