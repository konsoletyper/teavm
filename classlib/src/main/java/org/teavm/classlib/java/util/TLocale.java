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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util;

import java.util.Arrays;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.metadata.StringResource;

public final class TLocale implements TCloneable, TSerializable {
    private static TLocale defaultLocale;
    public static final TLocale CANADA = new TLocale("en", "CA");
    public static final TLocale CANADA_FRENCH = new TLocale("fr", "CA");
    public static final TLocale CHINA = new TLocale("zh", "CN");
    public static final TLocale CHINESE = new TLocale("zh", "");
    public static final TLocale ENGLISH = new TLocale("en", "");
    public static final TLocale FRANCE = new TLocale("fr", "FR");
    public static final TLocale FRENCH = new TLocale("fr", "");
    public static final TLocale GERMAN = new TLocale("de", "");
    public static final TLocale GERMANY = new TLocale("de", "DE");
    public static final TLocale ITALIAN = new TLocale("it", "");
    public static final TLocale ITALY = new TLocale("it", "IT");
    public static final TLocale JAPAN = new TLocale("ja", "JP");
    public static final TLocale JAPANESE = new TLocale("ja", "");
    public static final TLocale KOREA = new TLocale("ko", "KR");
    public static final TLocale KOREAN = new TLocale("ko", "");
    public static final TLocale PRC = new TLocale("zh", "CN");
    public static final TLocale SIMPLIFIED_CHINESE = new TLocale("zh", "CN");
    public static final TLocale TAIWAN = new TLocale("zh", "TW");
    public static final TLocale TRADITIONAL_CHINESE = new TLocale("zh", "TW");
    public static final TLocale UK = new TLocale("en", "GB");
    public static final TLocale US = new TLocale("en", "US");
    public static final TLocale ROOT = new TLocale("", "");
    private static TLocale[] availableLocales;

    static {
        String localeName = CLDRHelper.getDefaultLocale().getValue();
        int countryIndex = localeName.indexOf('_');
        defaultLocale = new TLocale(localeName.substring(0, countryIndex), localeName.substring(countryIndex + 1), "");
    }

    private transient String countryCode;
    private transient String languageCode;
    private transient String variantCode;

    public TLocale(String language) {
        this(language, "", "");
    }

    public TLocale(String language, String country) {
        this(language, country, "");
    }

    public TLocale(String language, String country, String variant) {
        if (language == null || country == null || variant == null) {
            throw new NullPointerException();
        }
        if (language.length() == 0 && country.length() == 0) {
            languageCode = "";
            countryCode = "";
            variantCode = variant;
            return;
        }
        languageCode = language;
        countryCode = country;
        variantCode = variant;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof TLocale) {
            TLocale o = (TLocale) object;
            return languageCode.equals(o.languageCode) && countryCode.equals(o.countryCode)
                    && variantCode.equals(o.variantCode);
        }
        return false;
    }

    public static TLocale[] getAvailableLocales() {
        if (availableLocales == null) {
            ResourceArray<StringResource> strings = CLDRHelper.getAvailableLocales();
            availableLocales = new TLocale[strings.size()];
            for (int i = 0; i < strings.size(); ++i) {
                String string = strings.get(i).getValue();
                int countryIndex = string.indexOf('-');
                if (countryIndex > 0) {
                    availableLocales[i] = new TLocale(string.substring(0, countryIndex),
                            string.substring(countryIndex + 1));
                } else {
                    availableLocales[i] = new TLocale(string);
                }
            }
        }
        return Arrays.copyOf(availableLocales, availableLocales.length);
    }

    public String getCountry() {
        return countryCode;
    }

    public static TLocale getDefault() {
        return defaultLocale;
    }

    public String getDisplayCountry() {
        return getDisplayCountry(getDefault());
    }

    public String getDisplayCountry(TLocale locale) {
        String result = getDisplayCountry(locale.getLanguage() + "-" + locale.getCountry(), countryCode);
        if (result == null) {
            result = getDisplayCountry(locale.getLanguage(), countryCode);
        }
        return result != null ? result : countryCode;
    }

    private static String getDisplayCountry(String localeName, String country) {
        if (!CLDRHelper.getCountriesMap().has(localeName)) {
            return null;
        }
        ResourceMap<StringResource> countries = CLDRHelper.getCountriesMap().get(localeName);
        if (!countries.has(country)) {
            return null;
        }
        return countries.get(country).getValue();
    }

    public String getDisplayLanguage() {
        return getDisplayLanguage(getDefault());
    }

    public String getDisplayLanguage(TLocale locale) {
        String result = getDisplayLanguage(locale.getLanguage() + "-" + locale.getCountry(), languageCode);
        if (result == null) {
            result = getDisplayLanguage(locale.getLanguage(), languageCode);
        }
        return result != null ? result : languageCode;
    }

    private static String getDisplayLanguage(String localeName, String language) {
        if (!CLDRHelper.getLanguagesMap().has(localeName)) {
            return null;
        }
        ResourceMap<StringResource> languages = CLDRHelper.getLanguagesMap().get(localeName);
        if (!languages.has(language)) {
            return null;
        }
        return languages.get(language).getValue();
    }

    public String getDisplayName() {
        return getDisplayName(getDefault());
    }

    public String getDisplayName(TLocale locale) {
        int count = 0;
        StringBuilder buffer = new StringBuilder();
        if (languageCode.length() > 0) {
            buffer.append(getDisplayLanguage(locale));
            count++;
        }
        if (countryCode.length() > 0) {
            if (count == 1) {
                buffer.append(" (");
            }
            buffer.append(getDisplayCountry(locale));
            count++;
        }
        if (variantCode.length() > 0) {
            if (count == 1) {
                buffer.append(" (");
            } else if (count == 2) {
                buffer.append(",");
            }
            buffer.append(getDisplayVariant(locale));
            count++;
        }
        if (count > 1) {
            buffer.append(")");
        }
        return buffer.toString();
    }

    public String getDisplayVariant() {
        return getDisplayVariant(getDefault());
    }

    public String getDisplayVariant(TLocale locale) {
        // TODO: use CLDR
        return locale.getVariant();
    }

    public String getLanguage() {
        return languageCode;
    }

    public String getVariant() {
        return variantCode;
    }

    @Override
    public int hashCode() {
        return countryCode.hashCode() + languageCode.hashCode() + variantCode.hashCode();
    }

    public static void setDefault(TLocale locale) {
        if (locale != null) {
            defaultLocale = locale;
        } else {
            throw new NullPointerException();
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(languageCode);
        if (countryCode.length() > 0) {
            result.append('_');
            result.append(countryCode);
        }
        if (variantCode.length() > 0 && result.length() > 0) {
            if (0 == countryCode.length()) {
                result.append("__");
            } else {
                result.append('_');
            }
            result.append(variantCode);
        }
        return result.toString();
    }
}
