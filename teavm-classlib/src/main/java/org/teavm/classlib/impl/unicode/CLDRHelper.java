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

    public static String[] resolveEras(String localeCode) {
        ResourceMap<ResourceArray<StringResource>> map = getErasMap();
        ResourceArray<StringResource> arrayRes = map.has(localeCode) ? map.get(localeCode) : map.get("root");
        return new String[] { arrayRes.get(0).getValue(), arrayRes.get(1).getValue() };
    }

    private static native ResourceMap<ResourceArray<StringResource>> getErasMap();

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
}
