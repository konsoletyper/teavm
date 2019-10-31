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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CLDRReader {
    private static final String[] weekdayKeys = { "sun", "mon", "tue", "wed", "thu", "fri", "sat" };
    private static CLDRReader lastInstance;
    private Map<String, CLDRLocale> knownLocales = new LinkedHashMap<>();
    private Map<String, Integer> minDaysMap = new LinkedHashMap<>();
    private Map<String, Integer> firstDayMap = new LinkedHashMap<>();
    private Map<String, String> likelySubtags = new LinkedHashMap<>();
    private Set<String> availableLocales = new LinkedHashSet<>();
    private Set<String> availableLanguages = new LinkedHashSet<>();
    private Set<String> availableCountries = new LinkedHashSet<>();
    private boolean initialized;
    private ClassLoader classLoader;
    private String availableLocalesString;

    private CLDRReader(ClassLoader classLoader, String availableLocalesString) {
        this.classLoader = classLoader;
        this.availableLocalesString = availableLocalesString;
    }

    public static CLDRReader getInstance(Properties properties, ClassLoader classLoader) {
        String availableLocalesString = properties.getProperty("java.util.Locale.available", "en_EN").trim();
        if (lastInstance == null || !lastInstance.availableLocalesString.equals(availableLocalesString)
                || lastInstance.classLoader != classLoader) {
            lastInstance = new CLDRReader(classLoader, availableLocalesString);
        }
        return lastInstance;
    }

    private synchronized void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            findAvailableLocales();
            readCLDR(classLoader);
        }
    }

    private void findAvailableLocales() {
        for (String locale : Arrays.asList(availableLocalesString.split(" *, *"))) {
            int countryIndex = locale.indexOf('_');
            if (countryIndex > 0) {
                String language = locale.substring(0, countryIndex);
                String country = locale.substring(countryIndex + 1);
                availableLocales.add(language + "-" + country);
                availableLocales.add(language);
                availableLanguages.add(language);
                availableCountries.add(country);
            } else {
                availableLocales.add(locale);
                availableLanguages.add(locale);
            }
        }
    }

    private void readCLDR(ClassLoader classLoader) {
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(classLoader.getResourceAsStream(
                "org/teavm/classlib/impl/unicode/cldr-json.zip")))) {
            while (true) {
                ZipEntry entry = input.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (!entry.getName().endsWith(".json")) {
                    continue;
                }
                if (entry.getName().equals("supplemental/weekData.json")) {
                    readWeekData(input);
                    continue;
                } else if (entry.getName().equals("supplemental/likelySubtags.json")) {
                    readLikelySubtags(input);
                }
                int objectIndex = entry.getName().lastIndexOf('/');
                if (objectIndex < 0) {
                    continue;
                }
                String objectName = entry.getName().substring(objectIndex + 1);
                String localeName = entry.getName().substring(0, objectIndex);
                if (localeName.startsWith("/")) {
                    localeName = localeName.substring(1);
                }
                if (!localeName.equals("root") && !availableLocales.contains(localeName)) {
                    continue;
                }
                CLDRLocale localeInfo = knownLocales.get(localeName);
                if (localeInfo == null) {
                    localeInfo = new CLDRLocale();
                    knownLocales.put(localeName, localeInfo);
                }
                switch (objectName) {
                    case "languages.json":
                        readLanguages(localeName, localeInfo, input);
                        break;
                    case "territories.json":
                        readCountries(localeName, localeInfo, input);
                        break;
                    case "timeZoneNames.json":
                        readTimeZones(localeName, localeInfo, input);
                        break;
                    case "ca-gregorian.json": {
                        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
                        readEras(localeName, localeInfo, root);
                        readAmPms(localeName, localeInfo, root);
                        readMonths(localeName, localeInfo, root);
                        readShortMonths(localeName, localeInfo, root);
                        readWeekdays(localeName, localeInfo, root);
                        readShortWeekdays(localeName, localeInfo, root);
                        readDateFormats(localeName, localeInfo, root);
                        readTimeFormats(localeName, localeInfo, root);
                        readDateTimeFormats(localeName, localeInfo, root);
                        break;
                    }
                    case "currencies.json":
                        readCurrencies(localeName, localeInfo, input);
                        break;
                    case "numbers.json":
                        readNumbers(localeName, localeInfo, input);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading CLDR file", e);
        }
    }

    private void readLanguages(String localeCode, CLDRLocale locale, InputStream input) {
        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
        JsonObject languagesJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("localeDisplayNames").getAsJsonObject().get("languages").getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : languagesJson.entrySet()) {
            String language = property.getKey();
            if (availableLanguages.contains(language)) {
                locale.languages.put(language, property.getValue().getAsString());
            }
        }
    }

    private void readCountries(String localeCode, CLDRLocale locale, InputStream input) {
        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
        JsonObject countriesJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("localeDisplayNames").getAsJsonObject().get("territories").getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : countriesJson.entrySet()) {
            String country = property.getKey();
            if (availableCountries.contains(country)) {
                locale.territories.put(country, property.getValue().getAsString());
            }
        }
    }

    private void readTimeZones(String localeCode, CLDRLocale locale, InputStream input) {
        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
        JsonObject zonesJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("timeZoneNames").getAsJsonObject().get("zone")
                .getAsJsonObject();
        List<CLDRTimeZone> timeZones = new ArrayList<>();
        for (Map.Entry<String, JsonElement> area : zonesJson.entrySet()) {
            String areaName = area.getKey();
            for (Map.Entry<String, JsonElement> location : area.getValue().getAsJsonObject().entrySet()) {
                String locationName = location.getKey();
                JsonElement city = location.getValue().getAsJsonObject().get("exemplarCity");
                if (city != null) {
                    CLDRTimeZone tz = new CLDRTimeZone(areaName, locationName, city.getAsString());
                    timeZones.add(tz);
                } else {
                    for (Map.Entry<String, JsonElement> sublocation : location.getValue()
                            .getAsJsonObject().entrySet()) {
                        city = location.getValue().getAsJsonObject().get("exemplarCity");
                        if (city != null) {
                            CLDRTimeZone tz = new CLDRTimeZone(areaName, locationName + "/" + sublocation.getKey(),
                                    city.getAsString());
                            timeZones.add(tz);
                        }
                    }
                }
            }
        }
        locale.timeZones = timeZones.toArray(new CLDRTimeZone[timeZones.size()]);
    }

    private void readCurrencies(String localeCode, CLDRLocale locale, InputStream input) {
        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
        JsonObject currenciesJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("numbers").getAsJsonObject().get("currencies").getAsJsonObject();
        for (Map.Entry<String, JsonElement> currencyEntry : currenciesJson.entrySet()) {
            String currencyCode = currencyEntry.getKey();
            JsonObject currencyJson = currencyEntry.getValue().getAsJsonObject();
            CLDRCurrency currency = new CLDRCurrency();
            currency.name = currencyJson.get("displayName").getAsString();
            if (currencyJson.has("symbol")) {
                currency.symbol = currencyJson.get("symbol").getAsString();
            }
            locale.currencies.put(currencyCode, currency);
        }
    }

    private void readNumbers(String localeCode, CLDRLocale locale, InputStream input) {
        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
        JsonObject numbersJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("numbers").getAsJsonObject();
        String numbering = numbersJson.get("defaultNumberingSystem").getAsString();
        JsonObject symbolsJson = numbersJson.get("symbols-numberSystem-" + numbering).getAsJsonObject();
        locale.decimalData.decimalSeparator = symbolsJson.get("decimal").getAsString().charAt(0);
        locale.decimalData.groupingSeparator = symbolsJson.get("group").getAsString().charAt(0);
        locale.decimalData.listSeparator = symbolsJson.get("list").getAsString().charAt(0);
        locale.decimalData.percent = symbolsJson.get("percentSign").getAsString().charAt(0);
        locale.decimalData.minusSign = symbolsJson.get("minusSign").getAsString().charAt(0);
        locale.decimalData.exponentSeparator = symbolsJson.get("exponential").getAsString();
        locale.decimalData.perMille = symbolsJson.get("perMille").getAsString().charAt(0);
        locale.decimalData.infinity = symbolsJson.get("infinity").getAsString();
        locale.decimalData.nan = symbolsJson.get("nan").getAsString();

        JsonObject numberJson = numbersJson.get("decimalFormats-numberSystem-" + numbering).getAsJsonObject();
        locale.numberFormat = numberJson.get("standard").getAsString();

        JsonObject percentJson = numbersJson.get("percentFormats-numberSystem-" + numbering).getAsJsonObject();
        locale.percentFormat = percentJson.get("standard").getAsString();

        JsonObject currencyJson = numbersJson.get("currencyFormats-numberSystem-" + numbering).getAsJsonObject();
        locale.currencyFormat = currencyJson.get("standard").getAsString();
    }

    private void readEras(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject erasJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("eras").getAsJsonObject().get("eraAbbr").getAsJsonObject();
        String bc = erasJson.get("0").getAsString();
        String ac = erasJson.get("1").getAsString();
        locale.eras = new String[] { bc, ac };
    }

    private void readAmPms(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject ampmJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("dayPeriods").getAsJsonObject()
                .get("format").getAsJsonObject().get("abbreviated").getAsJsonObject();
        String am = ampmJson.get("am").getAsString();
        String pm = ampmJson.get("pm").getAsString();
        locale.dayPeriods = new String[] { am, pm };
    }

    private void readMonths(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject monthsJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("months").getAsJsonObject()
                .get("format").getAsJsonObject().get("wide").getAsJsonObject();
        locale.months = new String[12];
        for (int i = 0; i < 12; ++i) {
            locale.months[i] = monthsJson.get(String.valueOf(i + 1)).getAsString();
        }
    }

    private void readShortMonths(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject monthsJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("months").getAsJsonObject()
                .get("format").getAsJsonObject().get("abbreviated").getAsJsonObject();
        locale.shortMonths = new String[12];
        for (int i = 0; i < 12; ++i) {
            locale.shortMonths[i] = monthsJson.get(String.valueOf(i + 1)).getAsString();
        }
    }

    private void readWeekdays(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject weekdaysJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("days").getAsJsonObject()
                .get("format").getAsJsonObject().get("wide").getAsJsonObject();
        locale.weekdays = new String[7];
        for (int i = 0; i < 7; ++i) {
            locale.weekdays[i] = weekdaysJson.get(weekdayKeys[i]).getAsString();
        }
    }

    private void readShortWeekdays(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject weekdaysJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("days").getAsJsonObject()
                .get("format").getAsJsonObject().get("abbreviated").getAsJsonObject();
        locale.shortWeekdays = new String[7];
        for (int i = 0; i < 7; ++i) {
            locale.shortWeekdays[i] = weekdaysJson.get(weekdayKeys[i]).getAsString();
        }
    }

    private void readDateFormats(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject formatsJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("dateFormats").getAsJsonObject();
        locale.dateFormats = new CLDRDateFormats(formatsJson.get("short").getAsString(),
                formatsJson.get("medium").getAsString(), formatsJson.get("long").getAsString(),
                formatsJson.get("full").getAsString());
    }

    private void readTimeFormats(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject formatsJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("timeFormats").getAsJsonObject();
        locale.timeFormats = new CLDRDateFormats(formatsJson.get("short").getAsString(),
                formatsJson.get("medium").getAsString(), formatsJson.get("long").getAsString(),
                formatsJson.get("full").getAsString());
    }

    private void readDateTimeFormats(String localeCode, CLDRLocale locale, JsonObject root) {
        JsonObject formatsJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("dates").getAsJsonObject().get("calendars").getAsJsonObject()
                .get("gregorian").getAsJsonObject().get("dateTimeFormats").getAsJsonObject();
        locale.dateTimeFormats = new CLDRDateFormats(formatsJson.get("short").getAsString(),
                formatsJson.get("medium").getAsString(), formatsJson.get("long").getAsString(),
                formatsJson.get("full").getAsString());
    }

    private void readWeekData(InputStream input) {
        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
        JsonObject weekJson = root.get("supplemental").getAsJsonObject().get("weekData").getAsJsonObject();
        JsonObject minDaysJson = weekJson.get("minDays").getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : minDaysJson.entrySet()) {
            minDaysMap.put(property.getKey(), property.getValue().getAsInt());
        }
        JsonObject firstDayJson = weekJson.get("firstDay").getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : firstDayJson.entrySet()) {
            firstDayMap.put(property.getKey(), getNumericDay(property.getValue().getAsString()));
        }
    }

    private void readLikelySubtags(InputStream input) {
        JsonObject root = (JsonObject) new JsonParser().parse(new InputStreamReader(input));
        JsonObject likelySubtagsJson = root.get("supplemental").getAsJsonObject().get("likelySubtags")
                .getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : likelySubtagsJson.entrySet()) {
            likelySubtags.put(property.getKey(), property.getValue().getAsString());
        }
    }

    private int getNumericDay(String day) {
        switch (day) {
            case "sun":
                return 1;
            case "mon":
                return 2;
            case "tue":
                return 3;
            case "wed":
                return 4;
            case "thu":
                return 5;
            case "fri":
                return 6;
            case "sat":
                return 7;
            default:
                throw new IllegalArgumentException("Can't recognize day name: " + day);
        }
    }

    public Map<String, CLDRLocale> getKnownLocales() {
        ensureInitialized();
        return Collections.unmodifiableMap(knownLocales);
    }

    public Set<String> getAvailableLocales() {
        ensureInitialized();
        return Collections.unmodifiableSet(availableLocales);
    }

    public Set<String> getAvailableLanguages() {
        ensureInitialized();
        return Collections.unmodifiableSet(availableLanguages);
    }

    public Set<String> getAvailableCountries() {
        ensureInitialized();
        return Collections.unmodifiableSet(availableCountries);
    }

    public Map<String, Integer> getMinDaysMap() {
        ensureInitialized();
        return Collections.unmodifiableMap(minDaysMap);
    }

    public Map<String, Integer> getFirstDayMap() {
        ensureInitialized();
        return Collections.unmodifiableMap(firstDayMap);
    }

    public Map<String, String> getLikelySubtags() {
        ensureInitialized();
        return Collections.unmodifiableMap(likelySubtags);
    }
}
