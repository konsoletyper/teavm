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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.teavm.common.json.JsonObjectValue;
import org.teavm.common.json.JsonParser;
import org.teavm.common.json.JsonValue;
import org.teavm.common.json.JsonValueParserVisitor;
import org.teavm.common.json.JsonValueProvider;
import org.teavm.common.json.JsonVisitingConsumer;
import org.teavm.common.json.JsonVisitor;

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
                        var root = (JsonObjectValue) parse(input);
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
        var root = parse(input).asObject();
        var languagesJson = root.get("main").asObject().get(localeCode).asObject()
                .get("localeDisplayNames").asObject().get("languages").asObject();
        for (var property : languagesJson.entrySet()) {
            String language = property.getKey();
            if (availableLanguages.contains(language)) {
                locale.languages.put(language, property.getValue().asString());
            }
        }
    }

    private void readCountries(String localeCode, CLDRLocale locale, InputStream input) {
        var root = parse(input).asObject();
        var countriesJson = root.get("main").asObject().get(localeCode).asObject()
                .get("localeDisplayNames").asObject().get("territories").asObject();
        for (var property : countriesJson.entrySet()) {
            String country = property.getKey();
            if (availableCountries.contains(country)) {
                locale.territories.put(country, property.getValue().asString());
            }
        }
    }

    private void readTimeZones(String localeCode, CLDRLocale locale, InputStream input) {
        var root = parse(input).asObject();
        var zonesJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("timeZoneNames").asObject().get("zone")
                .asObject();
        var timeZones = new ArrayList<CLDRTimeZone>();
        for (var area : zonesJson.entrySet()) {
            var areaName = area.getKey();
            for (var location : area.getValue().asObject().entrySet()) {
                var locationName = location.getKey();
                var city = location.getValue().asObject().get("exemplarCity");
                if (city != null) {
                    var tz = new CLDRTimeZone(areaName, locationName, city.asString());
                    timeZones.add(tz);
                } else {
                    for (var sublocation : location.getValue().asObject().entrySet()) {
                        city = location.getValue().asObject().get("exemplarCity");
                        if (city != null) {
                            var tz = new CLDRTimeZone(areaName, locationName + "/" + sublocation.getKey(),
                                    city.asString());
                            timeZones.add(tz);
                        }
                    }
                }
            }
        }
        locale.timeZones = timeZones.toArray(new CLDRTimeZone[timeZones.size()]);
    }

    private void readCurrencies(String localeCode, CLDRLocale locale, InputStream input) {
        var root = parse(input).asObject();
        var currenciesJson = root.get("main").asObject().get(localeCode).asObject()
                .get("numbers").asObject().get("currencies").asObject();
        for (var currencyEntry : currenciesJson.entrySet()) {
            var currencyCode = currencyEntry.getKey();
            var currencyJson = currencyEntry.getValue().asObject();
            var currency = new CLDRCurrency();
            currency.name = currencyJson.get("displayName").asString();
            if (currencyJson.has("symbol")) {
                currency.symbol = currencyJson.get("symbol").asString();
            }
            locale.currencies.put(currencyCode, currency);
        }
    }

    private void readNumbers(String localeCode, CLDRLocale locale, InputStream input) {
        var root = parse(input).asObject();
        var numbersJson = root.get("main").asObject().get(localeCode).asObject()
                .get("numbers").asObject();
        String numbering = numbersJson.get("defaultNumberingSystem").asString();
        var symbolsJson = numbersJson.get("symbols-numberSystem-" + numbering).asObject();
        locale.decimalData.decimalSeparator = symbolsJson.get("decimal").asString().charAt(0);
        locale.decimalData.groupingSeparator = symbolsJson.get("group").asString().charAt(0);
        locale.decimalData.listSeparator = symbolsJson.get("list").asString().charAt(0);
        locale.decimalData.percent = symbolsJson.get("percentSign").asString().charAt(0);
        locale.decimalData.minusSign = symbolsJson.get("minusSign").asString().charAt(0);
        locale.decimalData.exponentSeparator = symbolsJson.get("exponential").asString();
        locale.decimalData.perMille = symbolsJson.get("perMille").asString().charAt(0);
        locale.decimalData.infinity = symbolsJson.get("infinity").asString();
        locale.decimalData.nan = symbolsJson.get("nan").asString();

        var numberJson = numbersJson.get("decimalFormats-numberSystem-" + numbering).asObject();
        locale.numberFormat = numberJson.get("standard").asString();

        var percentJson = numbersJson.get("percentFormats-numberSystem-" + numbering).asObject();
        locale.percentFormat = percentJson.get("standard").asString();

        var currencyJson = numbersJson.get("currencyFormats-numberSystem-" + numbering).asObject();
        locale.currencyFormat = currencyJson.get("standard").asString();
    }

    private void readEras(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var erasJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("eras").asObject().get("eraAbbr").asObject();
        var bc = erasJson.get("0").asString();
        var ac = erasJson.get("1").asString();
        locale.eras = new String[] { bc, ac };
    }

    private void readAmPms(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var ampmJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("dayPeriods").asObject()
                .get("format").asObject().get("abbreviated").asObject();
        String am = ampmJson.get("am").asString();
        String pm = ampmJson.get("pm").asString();
        locale.dayPeriods = new String[] { am, pm };
    }

    private void readMonths(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var monthsJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("months").asObject()
                .get("format").asObject().get("wide").asObject();
        locale.months = new String[12];
        for (int i = 0; i < 12; ++i) {
            locale.months[i] = monthsJson.get(String.valueOf(i + 1)).asString();
        }
    }

    private void readShortMonths(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var monthsJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("months").asObject()
                .get("format").asObject().get("abbreviated").asObject();
        locale.shortMonths = new String[12];
        for (int i = 0; i < 12; ++i) {
            locale.shortMonths[i] = monthsJson.get(String.valueOf(i + 1)).asString();
        }
    }

    private void readWeekdays(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var weekdaysJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("days").asObject()
                .get("format").asObject().get("wide").asObject();
        locale.weekdays = new String[7];
        for (int i = 0; i < 7; ++i) {
            locale.weekdays[i] = weekdaysJson.get(weekdayKeys[i]).asString();
        }
    }

    private void readShortWeekdays(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var weekdaysJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("days").asObject()
                .get("format").asObject().get("abbreviated").asObject();
        locale.shortWeekdays = new String[7];
        for (int i = 0; i < 7; ++i) {
            locale.shortWeekdays[i] = weekdaysJson.get(weekdayKeys[i]).asString();
        }
    }

    private void readDateFormats(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var formatsJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("dateFormats").asObject();
        locale.dateFormats = new CLDRDateFormats(formatsJson.get("short").asString(),
                formatsJson.get("medium").asString(), formatsJson.get("long").asString(),
                formatsJson.get("full").asString());
    }

    private void readTimeFormats(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var formatsJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("timeFormats").asObject();
        locale.timeFormats = new CLDRDateFormats(formatsJson.get("short").asString(),
                formatsJson.get("medium").asString(), formatsJson.get("long").asString(),
                formatsJson.get("full").asString());
    }

    private void readDateTimeFormats(String localeCode, CLDRLocale locale, JsonObjectValue root) {
        var formatsJson = root.get("main").asObject().get(localeCode).asObject()
                .get("dates").asObject().get("calendars").asObject()
                .get("gregorian").asObject().get("dateTimeFormats").asObject();
        locale.dateTimeFormats = new CLDRDateFormats(formatsJson.get("short").asString(),
                formatsJson.get("medium").asString(), formatsJson.get("long").asString(),
                formatsJson.get("full").asString());
    }

    private void readWeekData(InputStream input) {
        var root = parse(input).asObject();
        var weekJson = root.get("supplemental").asObject().get("weekData").asObject();
        var minDaysJson = weekJson.get("minDays").asObject();
        for (var property : minDaysJson.entrySet()) {
            minDaysMap.put(property.getKey(), Integer.parseInt(property.getValue().asString()));
        }
        var firstDayJson = weekJson.get("firstDay").asObject();
        for (var property : firstDayJson.entrySet()) {
            firstDayMap.put(property.getKey(), getNumericDay(property.getValue().asString()));
        }
    }

    private void readLikelySubtags(InputStream input) {
        var root = parse(input).asObject();
        var likelySubtagsJson = root.get("supplemental").asObject().get("likelySubtags").asObject();
        for (var property : likelySubtagsJson.entrySet()) {
            likelySubtags.put(property.getKey(), property.getValue().asString());
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

    private JsonValue parse(InputStream input) {
        var provider = new JsonValueProvider();
        var visitor = JsonValueParserVisitor.create(provider);
        parse(visitor, input);
        return provider.getValue();
    }

    private void parse(JsonVisitor visitor, InputStream input) {
        var consumer = new JsonVisitingConsumer(visitor);
        var parser = new JsonParser(consumer);
        try {
            parser.parse(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
