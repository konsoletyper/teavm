/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.buildutil;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

/**
 * Gives access to project properties, that can be overridden in {@value #FILE_NAME} file in project
 * directory. This file is not supposed to be committed, so every developer can keep their own settings there.
 *
 * <p>Property value is looked up in the following order:
 * <ol>
 *   <li>{@code -P} options passed in command line;</li>
 *   <li>{@value #FILE_NAME} in project directory;</li>
 *   <li>other sources of Gradle properties: {@code gradle.properties} in Gradle user home and in project
 *     directory, {@code ORG_GRADLE_PROJECT_} environment variables and so on.</li>
 * </ol>
 *
 * <p>Blank values are treated as absent, so that writing {@code name=} in {@value #FILE_NAME} is the same as
 * not mentioning property at all.
 */
public class LocalPropertiesExtension {
    public static final String FILE_NAME = "gradle-local.properties";

    private final ProviderFactory providers;
    private final Map<String, String> commandLineProperties;
    private final Provider<Properties> localProperties;

    LocalPropertiesExtension(ProviderFactory providers, Map<String, String> commandLineProperties,
            RegularFile file) {
        this.providers = providers;
        this.commandLineProperties = commandLineProperties;
        localProperties = providers.fileContents(file).getAsText().map(LocalPropertiesExtension::parse);
    }

    /**
     * Returns property as a lazy provider. In Kotlin, can be used as {@code localProperties["name"]}.
     */
    public Provider<String> get(String name) {
        return providers.provider(() -> nonBlank(commandLineProperties.get(name)))
                .orElse(localProperties.map(properties -> nonBlank(properties.getProperty(name))))
                .orElse(providers.gradleProperty(name).map(LocalPropertiesExtension::nonBlank));
    }

    public String string(String name) {
        return get(name).getOrNull();
    }

    public String string(String name, String defaultValue) {
        return get(name).getOrElse(defaultValue);
    }

    public boolean flag(String name) {
        return Boolean.parseBoolean(get(name).getOrElse("false"));
    }

    /**
     * Returns value split by given separator, with blank items removed and remaining items trimmed.
     */
    public List<String> list(String name, String separator) {
        return split(string(name), separator);
    }

    /**
     * Returns comma-separated list.
     */
    public List<String> list(String name) {
        return list(name, ",");
    }

    static List<String> split(String value, String separatorRegex) {
        var result = new ArrayList<String>();
        if (value != null) {
            for (var item : value.split(separatorRegex)) {
                item = item.trim();
                if (!item.isEmpty()) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private static String nonBlank(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static Properties parse(String text) {
        var properties = new Properties();
        try {
            properties.load(new StringReader(text));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
