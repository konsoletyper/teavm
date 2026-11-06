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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.lang.TString;
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
    private transient String scriptCode;
    private transient String variantCode;
    private transient String extensionsCode;

    public TLocale(String language) {
        this(language, "", "");
    }

    public TLocale(String language, String country) {
        this(language, country, "");
    }

    public TLocale(String language, String country, String variant) {
        this(language, "", country, variant, "");
    }

    private TLocale(String language, String script, String country, String variant, String extensions) {
        Objects.requireNonNull(language);
        Objects.requireNonNull(script);
        Objects.requireNonNull(country);
        Objects.requireNonNull(variant);
        Objects.requireNonNull(extensions);
        languageCode = legacyLanguage(lowerAscii(language));
        scriptCode = titleAscii(script);
        countryCode = upperAscii(country);
        variantCode = variant;
        extensionsCode = extensions;
    }

    public static TLocale forLanguageTag(String languageTag) {
        Objects.requireNonNull(languageTag);
        List<String> tokens = splitTag(languageTag);
        if (tokens.isEmpty() || tokens.get(0).isEmpty()) {
            return ROOT;
        }

        String grandfathered = grandfatheredLanguage(languageTag);
        if (grandfathered != null) {
            return forLanguageTag(grandfathered);
        }

        String first = tokens.get(0);
        if ("x".equalsIgnoreCase(first)) {
            String privateUse = parsePrivateUse(tokens, 1);
            return privateUse.isEmpty() ? ROOT : new TLocale("", "", "", "", "x-" + privateUse);
        }
        if (!isAlpha(first) || first.length() < 2 || first.length() > 8) {
            return ROOT;
        }

        int index = 1;
        String language = "und".equalsIgnoreCase(first) ? "" : legacyLanguage(lowerAscii(first));
        if (first.length() <= 3) {
            int extlangCount = 0;
            while (index < tokens.size() && extlangCount < 3 && tokens.get(index).length() == 3
                    && isAlpha(tokens.get(index))) {
                if (extlangCount == 0) {
                    language = lowerAscii(tokens.get(index));
                }
                ++index;
                ++extlangCount;
            }
        }

        String script = "";
        if (index < tokens.size() && tokens.get(index).length() == 4 && isAlpha(tokens.get(index))) {
            script = tokens.get(index++);
        }

        String country = "";
        if (index < tokens.size() && isRegion(tokens.get(index))) {
            country = tokens.get(index++);
        }

        List<String> variants = new ArrayList<>();
        while (index < tokens.size() && isVariant(tokens.get(index))) {
            variants.add(tokens.get(index++));
        }
        String variant = join(variants, "_");

        String[] extensionParts = new String[36];
        boolean[] seen = new boolean[36];
        while (index < tokens.size() && isSingleton(tokens.get(index))) {
            String singleton = lowerAscii(tokens.get(index));
            int key = singletonIndex(singleton.charAt(0));
            if (seen[key]) {
                break;
            }
            int start = index++;
            List<String> values = new ArrayList<>();
            int minimumLength = "x".equals(singleton) ? 1 : 2;
            while (index < tokens.size() && tokens.get(index).length() >= minimumLength
                    && tokens.get(index).length() <= 8 && isAlphaNumeric(tokens.get(index))) {
                values.add(lowerAscii(tokens.get(index++)));
            }
            if (values.isEmpty()) {
                index = start;
                break;
            }
            extensionParts[key] = "u".equals(singleton)
                    ? canonicalizeUnicodeExtension(values)
                    : join(values, "-");
            seen[key] = true;
            if ("x".equals(singleton)) {
                break;
            }
        }

        int privateUseKey = singletonIndex('x');
        if (extensionParts[privateUseKey] != null) {
            List<String> privateParts = splitTag(extensionParts[privateUseKey]);
            if (!privateParts.isEmpty() && "lvariant".equals(privateParts.get(0))) {
                variants.addAll(privateParts.subList(1, privateParts.size()));
                variant = join(variants, "_");
                extensionParts[privateUseKey] = null;
            }
        }

        StringBuilder extensions = new StringBuilder();
        for (int key = 0; key < extensionParts.length; ++key) {
            if (extensionParts[key] != null) {
                if (extensions.length() > 0) {
                    extensions.append('-');
                }
                extensions.append(indexSingleton(key)).append('-').append(extensionParts[key]);
            }
        }
        return new TLocale(language, script, country, variant, extensions.toString());
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
            return languageCode.equals(o.languageCode) && scriptCode.equals(o.scriptCode)
                    && countryCode.equals(o.countryCode) && variantCode.equals(o.variantCode)
                    && extensionsCode.equals(o.extensionsCode);
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
        if (!languageCode.isEmpty()) {
            buffer.append(getDisplayLanguage(locale));
            count++;
        }
        if (!countryCode.isEmpty()) {
            if (count == 1) {
                buffer.append(" (");
            }
            buffer.append(getDisplayCountry(locale));
            count++;
        }
        if (!variantCode.isEmpty()) {
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

    public String getScript() {
        return scriptCode;
    }

    public String getVariant() {
        return variantCode;
    }

    public String getExtension(char key) {
        int index = singletonIndex(Character.toLowerCase(key));
        if (index < 0) {
            throw new IllegalArgumentException("Invalid extension key: " + key);
        }
        List<String> tokens = splitTag(extensionsCode);
        for (int i = 0; i < tokens.size(); ++i) {
            String token = tokens.get(i);
            if (token.length() == 1 && singletonIndex(token.charAt(0)) == index) {
                List<String> values = new ArrayList<>();
                for (++i; i < tokens.size() && (index == singletonIndex('x') || tokens.get(i).length() != 1); ++i) {
                    values.add(tokens.get(i));
                }
                return join(values, "-");
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return countryCode.hashCode() + languageCode.hashCode() + scriptCode.hashCode()
                + variantCode.hashCode() + extensionsCode.hashCode();
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
        if (!countryCode.isEmpty()) {
            result.append('_');
            result.append(countryCode);
        }
        if (!variantCode.isEmpty() && result.length() > 0) {
            if (countryCode.isEmpty()) {
                result.append("__");
            } else {
                result.append('_');
            }
            result.append(variantCode);
        }
        if (!scriptCode.isEmpty() || !extensionsCode.isEmpty()) {
            if (result.length() > 0) {
                result.append("_#");
            } else {
                result.append('#');
            }
            result.append(scriptCode);
            if (!scriptCode.isEmpty() && !extensionsCode.isEmpty()) {
                result.append('_');
            }
            result.append(extensionsCode);
        }
        return result.toString();
    }

    public TString toLanguageTag() {
        StringBuilder result = new StringBuilder();
        List<String> privateVariants = new ArrayList<>();
        boolean privateUseOnly = languageCode.isEmpty() && scriptCode.isEmpty() && countryCode.isEmpty()
                && variantCode.isEmpty() && extensionsCode.startsWith("x-");
        if (!privateUseOnly) {
            result.append(languageCode.isEmpty() ? "und" : languageCode);
        }
        if (!scriptCode.isEmpty()) {
            result.append('-').append(scriptCode);
        }
        if (!countryCode.isEmpty()) {
            result.append('-');
            result.append(countryCode);
        }
        if (!variantCode.isEmpty()) {
            String[] variants = variantCode.split("_");
            boolean validVariants = true;
            for (String variant : variants) {
                if (validVariants && isVariant(variant)) {
                    result.append('-').append(variant);
                } else {
                    validVariants = false;
                    privateVariants.add(variant);
                }
            }
        }
        String extensions = extensionsCode;
        if (!privateVariants.isEmpty()) {
            int privateUseStart = extensions.indexOf("x-");
            String variantExtension = "lvariant-" + join(privateVariants, "-");
            if (privateUseStart >= 0) {
                extensions += "-" + variantExtension;
            } else {
                extensions += (extensions.isEmpty() ? "" : "-") + "x-" + variantExtension;
            }
        }
        if (!extensions.isEmpty()) {
            if (result.length() > 0) {
                result.append('-');
            }
            result.append(extensions);
        }
        return new TString(result.toString().toCharArray());
    }

    private static List<String> splitTag(String tag) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= tag.length(); ++i) {
            if (i == tag.length() || tag.charAt(i) == '-') {
                result.add(tag.substring(start, i));
                start = i + 1;
            }
        }
        return result;
    }

    private static String parsePrivateUse(List<String> tokens, int index) {
        List<String> values = new ArrayList<>();
        while (index < tokens.size() && tokens.get(index).length() >= 1 && tokens.get(index).length() <= 8
                && isAlphaNumeric(tokens.get(index))) {
            values.add(lowerAscii(tokens.get(index++)));
        }
        return join(values, "-");
    }

    private static boolean isAlpha(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAlphaNumeric(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRegion(String text) {
        if (text.length() == 2) {
            return isAlpha(text);
        }
        if (text.length() != 3) {
            return false;
        }
        for (int i = 0; i < 3; ++i) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isVariant(String text) {
        if (text.length() >= 5 && text.length() <= 8) {
            return isAlphaNumeric(text);
        }
        return text.length() == 4 && text.charAt(0) >= '0' && text.charAt(0) <= '9'
                && isAlphaNumeric(text);
    }

    private static boolean isSingleton(String text) {
        return text.length() == 1 && isAlphaNumeric(text);
    }

    private static int singletonIndex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        c = Character.toLowerCase(c);
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 10;
        }
        return -1;
    }

    private static char indexSingleton(int index) {
        return index < 10 ? (char) ('0' + index) : (char) ('a' + index - 10);
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(delimiter);
            }
            result.append(value);
        }
        return result.toString();
    }

    private static String canonicalizeUnicodeExtension(List<String> values) {
        List<String> attributes = new ArrayList<>();
        String[] types = new String[36 * 36];
        int index = 0;
        while (index < values.size() && values.get(index).length() != 2) {
            attributes.add(values.get(index++));
        }
        attributes.sort(String::compareTo);
        while (index < values.size()) {
            String key = values.get(index++);
            int keyIndex = singletonIndex(key.charAt(0)) * 36 + singletonIndex(key.charAt(1));
            List<String> typeParts = new ArrayList<>();
            while (index < values.size() && values.get(index).length() != 2) {
                typeParts.add(values.get(index++));
            }
            if (types[keyIndex] == null) {
                types[keyIndex] = join(typeParts, "-");
            }
        }

        List<String> canonical = new ArrayList<>(attributes);
        for (int keyIndex = 0; keyIndex < types.length; ++keyIndex) {
            if (types[keyIndex] != null) {
                int first = keyIndex / 36;
                int second = keyIndex % 36;
                canonical.add(String.valueOf(indexSingleton(first)) + indexSingleton(second));
                if (!types[keyIndex].isEmpty()) {
                    canonical.addAll(splitTag(types[keyIndex]));
                }
            }
        }
        return join(canonical, "-");
    }

    private static String lowerAscii(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            result.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return result.toString();
    }

    private static String upperAscii(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            result.append(c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c);
        }
        return result.toString();
    }

    private static String titleAscii(String value) {
        String lower = lowerAscii(value);
        return lower.isEmpty() ? lower : upperAscii(lower.substring(0, 1)) + lower.substring(1);
    }

    private static String legacyLanguage(String language) {
        switch (language) {
            case "iw":
                return "he";
            case "ji":
                return "yi";
            case "in":
                return "id";
            default:
                return language;
        }
    }

    private static String grandfatheredLanguage(String tag) {
        String lower = lowerAscii(tag);
        switch (lower) {
            case "art-lojban":
                return "jbo";
            case "cel-gaulish":
                return "xtg-x-cel-gaulish";
            case "en-gb-oed":
                return "en-GB-oxendict";
            case "i-ami":
                return "ami";
            case "i-default":
                return "en-x-i-default";
            case "i-enochian":
                return "x-i-enochian";
            case "i-klingon":
                return "tlh";
            case "i-bnn":
                return "bnn";
            case "i-hak":
                return "hak";
            case "i-lux":
                return "lb";
            case "i-mingo":
                return "see-x-i-mingo";
            case "i-navajo":
                return "nv";
            case "i-pwn":
                return "pwn";
            case "i-tao":
                return "tao";
            case "i-tay":
                return "tay";
            case "i-tsu":
                return "tsu";
            case "sgn-be-fr":
                return "sfb";
            case "sgn-be-nl":
                return "vgt";
            case "sgn-ch-de":
                return "sgg";
            case "no-bok":
                return "nb";
            case "no-nyn":
                return "nn";
            case "zh-guoyu":
                return "cmn";
            case "zh-hakka":
                return "hak";
            case "zh-min":
                return "nan-x-zh-min";
            case "zh-min-nan":
                return "nan";
            case "zh-xiang":
                return "hsn";
            default:
                return null;
        }
    }

    public static TLocale of(String language, String country, String variant) {
        return new TLocale(lowerAscii(language), upperAscii(country), variant);
    }

    public static TLocale of(String language, String country) {
        return new TLocale(lowerAscii(language), upperAscii(country), "");
    }

    public static TLocale of(String language) {
        return new TLocale(lowerAscii(language), "", "");
    }
}
