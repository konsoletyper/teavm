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
package org.teavm.classlib.java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.Renderer;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 *
 * @author Alexey Andreev
 */
public class LocaleSettingsNativeGenerator implements Generator {
    private ClassLoader classLoader;
    private Properties properties;
    private Map<String, LocaleInfo> knownLocales = new LinkedHashMap<>();
    private Map<String, Integer> minDaysMap = new LinkedHashMap<>();
    private Map<String, Integer> firstDayMap = new LinkedHashMap<>();
    private Map<String, String> likelySubtags = new LinkedHashMap<>();
    private Set<String> availableLocales = new LinkedHashSet<>();
    private Set<String> availableLanguages = new LinkedHashSet<>();
    private Set<String> availableCountries = new LinkedHashSet<>();
    private boolean initialized;

    public LocaleSettingsNativeGenerator(ClassLoader classLoader, Properties properties) {
        this.classLoader = classLoader;
        this.properties = properties;
    }

    private synchronized void init() {
        if (!initialized) {
            initialized = true;
            findAvailableLocales();
            readCLDR();
        }
    }

    private void findAvailableLocales() {
        String availableLocalesString = properties.getProperty("java.util.Locale.available", "en_EN").trim();
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

    private void readCLDR() {
        try (ZipInputStream input = new ZipInputStream(classLoader.getResourceAsStream(
                "org/teavm/classlib/impl/unicode/cldr-json.zip"))) {
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
                String objectName = entry.getName().substring(objectIndex + 1);
                String localeName = entry.getName().substring(0, objectIndex);
                if (localeName.startsWith("/")) {
                    localeName = localeName.substring(1);
                }
                if (!availableLocales.contains(localeName)) {
                    continue;
                }
                LocaleInfo localeInfo = knownLocales.get(localeName);
                if (localeInfo == null) {
                    localeInfo = new LocaleInfo();
                    knownLocales.put(localeName, localeInfo);
                }
                switch (objectName) {
                    case "languages.json":
                        readLanguages(localeName, localeInfo, input);
                        break;
                    case "territories.json":
                        readCountries(localeName, localeInfo, input);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading CLDR file", e);
        }
    }

    private void readLanguages(String localeCode, LocaleInfo locale, InputStream input) {
        JsonObject root = (JsonObject)new JsonParser().parse(new InputStreamReader(input));
        JsonObject languagesJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("localeDisplayNames").getAsJsonObject().get("languages").getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : languagesJson.entrySet()) {
            String language = property.getKey();
            if (availableLanguages.contains(language)) {
                locale.languages.put(language, property.getValue().getAsString());
            }
        }
    }

    private void readCountries(String localeCode, LocaleInfo locale, InputStream input) {
        JsonObject root = (JsonObject)new JsonParser().parse(new InputStreamReader(input));
        JsonObject countriesJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("localeDisplayNames").getAsJsonObject().get("territories").getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : countriesJson.entrySet()) {
            String country = property.getKey();
            if (availableCountries.contains(country)) {
                locale.territories.put(country, property.getValue().getAsString());
            }
        }
    }

    private void readWeekData(InputStream input) {
        JsonObject root = (JsonObject)new JsonParser().parse(new InputStreamReader(input));
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
        JsonObject root = (JsonObject)new JsonParser().parse(new InputStreamReader(input));
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

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        init();
        switch (methodRef.getName()) {
            case "readLanguagesFromCLDR":
                generateReadLanguagesFromCLDR(writer);
                break;
            case "readCountriesFromCLDR":
                generateReadCountriesFromCLDR(writer);
                break;
            case "readWeeksFromCDLR":
                generateReadWeeksFromCDLR(writer);
                break;
            case "readLikelySubtagsFromCLDR":
                generateReadLikelySubtagsFromCLDR(writer);
                break;
            case "readAvailableLocales":
                generateReadAvailableLocales(writer);
                break;
            case "getDefaultLocale":
                generateGetDefaultLocale(writer);
                break;
        }
    }

    private void generateDefender(SourceWriter writer, String property) throws IOException {
        writer.append("if (").appendClass("java.util.Locale").append(".$CLDR.").append(property)
            .append(")").ws().append("{").indent().softNewLine();
        writer.append("return;").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateReadAvailableLocales(SourceWriter writer) throws IOException {
        generateDefender(writer, "availableLocales");
        writer.appendClass("java.util.Locale").append(".$CLDR.availableLocales = [");
        boolean first = true;
        for (String locale : availableLocales) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            writer.append('"').append(Renderer.escapeString(locale)).append('"');
        }
        writer.append("];").softNewLine();
    }

    private void generateReadLanguagesFromCLDR(SourceWriter writer) throws IOException {
        generateDefender(writer, "languages");
        writer.appendClass("java.util.Locale").append(".$CLDR.languages = {").indent().softNewLine();
        boolean firstLocale = true;
        for (Map.Entry<String, LocaleInfo> entry : knownLocales.entrySet()) {
            if (!firstLocale) {
                writer.append(",").softNewLine();
            }
            firstLocale = false;
            writer.append('"').append(Renderer.escapeString(entry.getKey())).append('"').ws().append(":").ws()
                    .append('{').indent().softNewLine();

            boolean first = true;
            for (Map.Entry<String, String> langEntry : entry.getValue().languages.entrySet()) {
                if (!first) {
                    writer.append(',').softNewLine();
                }
                first = false;
                writer.append('"').append(Renderer.escapeString(langEntry.getKey())).append('"').ws().append(':')
                        .ws().append('"').append(Renderer.escapeString(langEntry.getValue())).append('"');
            }
            writer.outdent().append('}');
        }
        writer.outdent().append("};").softNewLine();
    }

    private void generateReadCountriesFromCLDR(SourceWriter writer) throws IOException {
        generateDefender(writer, "territories");
        writer.appendClass("java.util.Locale").append(".$CLDR.territories = {").indent().softNewLine();
        boolean firstLocale = true;
        for (Map.Entry<String, LocaleInfo> entry : knownLocales.entrySet()) {
            if (!firstLocale) {
                writer.append(",").softNewLine();
            }
            firstLocale = false;
            writer.append('"').append(Renderer.escapeString(entry.getKey())).append('"').ws().append(":").ws()
                    .append('{').indent().softNewLine();

            boolean first = true;
            for (Map.Entry<String, String> langEntry : entry.getValue().territories.entrySet()) {
                if (!first) {
                    writer.append(',').softNewLine();
                }
                first = false;
                writer.append('"').append(Renderer.escapeString(langEntry.getKey())).append('"').ws().append(':')
                        .ws().append('"').append(Renderer.escapeString(langEntry.getValue())).append('"');
            }

            writer.outdent().append('}');
        }
        writer.outdent().append("};").softNewLine();
    }

    private void generateReadWeeksFromCDLR(SourceWriter writer) throws IOException {
        generateDefender(writer, "minDays");
        writer.appendClass("java.util.Locale").append(".$CLDR.minDays = {").indent().softNewLine();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : minDaysMap.entrySet()) {
            if (!first) {
                writer.append(",").softNewLine();
            }
            first = false;
            writer.append('"').append(Renderer.escapeString(entry.getKey())).append('"').ws().append(':')
                    .ws().append(entry.getValue());
        }
        writer.outdent().append("};").softNewLine();

        writer.appendClass("java.util.Locale").append(".$CLDR.firstDay = {").indent().softNewLine();
        first = true;
        for (Map.Entry<String, Integer> entry : firstDayMap.entrySet()) {
            if (!first) {
                writer.append(",").softNewLine();
            }
            first = false;
            writer.append('"').append(Renderer.escapeString(entry.getKey())).append('"').ws().append(':')
                    .ws().append(entry.getValue());
        }
        writer.outdent().append("};").softNewLine();
    }

    private void generateReadLikelySubtagsFromCLDR(SourceWriter writer) throws IOException {
        generateDefender(writer, "likelySubtags");
        writer.appendClass("java.util.Locale").append(".$CLDR.likelySubtags = {").indent().softNewLine();
        boolean first = true;
        for (Map.Entry<String, String> entry : likelySubtags.entrySet()) {
            if (!first) {
                writer.append(",").softNewLine();
            }
            first = false;
            writer.append('"').append(Renderer.escapeString(entry.getKey())).append('"').ws().append(':')
                    .ws().append('"').append(Renderer.escapeString(entry.getValue())).append('"');
        }
        writer.outdent().append("};").softNewLine();
    }

    private void generateGetDefaultLocale(SourceWriter writer) throws IOException {
        String locale = properties.getProperty("java.util.Locale.default", "en_EN");
        writer.append("return $rt_str(\"").append(Renderer.escapeString(locale)).append("\");").softNewLine();
    }

    static class LocaleInfo {
        Map<String, String> languages = new LinkedHashMap<>();
        Map<String, String> territories = new LinkedHashMap<>();
    }
}
