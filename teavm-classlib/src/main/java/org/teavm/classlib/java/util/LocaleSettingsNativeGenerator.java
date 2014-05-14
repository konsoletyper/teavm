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
import com.google.gson.*;

/**
 *
 * @author Alexey Andreev
 */
public class LocaleSettingsNativeGenerator implements Generator {
    private ClassLoader classLoader;
    private Properties properties;
    private Map<String, LocaleInfo> knownLocales = new LinkedHashMap<>();
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
        for (String locale : Arrays.asList(availableLocalesString.split(" *, +"))) {
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
        JsonObject languagesJson = root.get("main").getAsJsonObject().get(localeCode).getAsJsonObject()
                .get("localeDisplayNames").getAsJsonObject().get("territories").getAsJsonObject();
        for (Map.Entry<String, JsonElement> property : languagesJson.entrySet()) {
            String language = property.getKey();
            if (availableCountries.contains(language)) {
                locale.territories.put(language, property.getValue().getAsString());
            }
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        init();
        switch (methodRef.getName()) {
            case "readCLDR":
                generateReadCLDR(writer);
                break;
            case "getDefaultLocale":
                generateGetDefaultLocale(writer);
                break;
        }
    }

    private void generateReadCLDR(SourceWriter writer) throws IOException {
        writer.appendClass("java.util.Locale").append(".$CLDR = {").indent().softNewLine();
        boolean firstLocale = true;
        for (Map.Entry<String, LocaleInfo> entry : knownLocales.entrySet()) {
            if (!firstLocale) {
                writer.append(",").softNewLine();
            }
            firstLocale = false;
            writer.append('"').append(Renderer.escapeString(entry.getKey())).append('"').ws().append(":").ws()
                    .append('{').indent().softNewLine();

            writer.append("\"languages\"").ws().append(':').ws().append('{').indent().softNewLine();
            boolean first = true;
            for (Map.Entry<String, String> langEntry : entry.getValue().languages.entrySet()) {
                if (!first) {
                    writer.append(',').softNewLine();
                }
                first = false;
                writer.append('"').append(Renderer.escapeString(langEntry.getKey())).append('"').ws().append(':')
                        .ws().append('"').append(Renderer.escapeString(langEntry.getValue())).append('"');
            }
            writer.outdent().append("},").softNewLine();

            writer.append("\"territories\"").ws().append(':').ws().append('{').indent().softNewLine();
            first = true;
            for (Map.Entry<String, String> langEntry : entry.getValue().territories.entrySet()) {
                if (!first) {
                    writer.append(',').softNewLine();
                }
                first = false;
                writer.append('"').append(Renderer.escapeString(langEntry.getKey())).append('"').ws().append(':')
                        .ws().append('"').append(Renderer.escapeString(langEntry.getValue())).append('"');
            }
            writer.outdent().append('}');

            writer.outdent().append('}');
        }
        writer.outdent().append("}").softNewLine();
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
