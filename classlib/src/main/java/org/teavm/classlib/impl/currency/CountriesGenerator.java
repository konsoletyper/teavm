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
package org.teavm.classlib.impl.currency;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;
import org.teavm.platform.metadata.builders.StringResourceBuilder;

public class CountriesGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        try (InputStream input = new BufferedInputStream(context.getResourceProvider().getResource(
                "org/teavm/classlib/impl/currency/iso3166.csv").open())) {
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return readIso3166(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading ISO 3166 table", e);
        }
    }

    private ResourceMapBuilder<StringResourceBuilder> readIso3166(BufferedReader reader) throws IOException {
        var result = new ResourceMapBuilder<StringResourceBuilder>();
        int index = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (index++ == 0 || line.trim().isEmpty()) {
                continue;
            }
            String[] cells = readCsvRow(index - 1, line);
            var currency = new StringResourceBuilder();
            currency.value = cells[7];
            result.values.put(cells[10], currency);
        }
        return result;
    }

    private String[] readCsvRow(int rowIndex, String row) {
        List<String> values = new ArrayList<>();
        int index = 0;
        while (index < row.length()) {
            char c = row.charAt(index);
            int next = index;
            if (c == '"') {
                ++index;
                StringBuilder sb = new StringBuilder();
                while (index < row.length()) {
                    next = row.indexOf('"', index);
                    if (next == -1) {
                        throw new IllegalStateException("Syntax error at row " + rowIndex
                                + ": closing quote not found");
                    }
                    if (next + 1 == row.length() || row.charAt(next + 1) != '"') {
                        sb.append(row, index, next);
                        index = next + 1;
                        break;
                    }
                    index = next + 2;
                }
                if (index < row.length() && row.charAt(index) != ',') {
                    throw new IllegalStateException("Syntax error at row " + rowIndex + ": closing quote must be "
                            + "followed by either line separator or comma");
                }
                values.add(sb.toString());
            } else {
                next = row.indexOf(',', index);
                if (next == -1) {
                    values.add(row.substring(index));
                    index = row.length();
                } else {
                    values.add(row.substring(index, next));
                    ++next;
                    index = next;
                }
            }
        }
        return values.toArray(new String[0]);
    }
}
