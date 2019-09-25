/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.teavm.model.text.ListingParseException;
import org.teavm.model.text.ListingParser;

public final class ListingParseUtils {
    private ListingParseUtils() {
    }

    public static Program parseFromResource(String resourceName) {
        ClassLoader classLoader = ListingParseUtils.class.getClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(resourceName);
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return new ListingParser().parse(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ListingParseException e) {
            Location location;
            try (InputStream input = classLoader.getResourceAsStream(resourceName)) {
                location = offsetToLocation(e.getIndex(), input);
            } catch (IOException e2) {
                throw new RuntimeException(e2);
            }
            Assert.fail("Parse error at [" + (location.row + 1) + "; " + (location.column + 1)
                    + "]: " + e.getMessage());
            return null;
        }
    }

    private static Location offsetToLocation(int offset, InputStream input) throws IOException {
        int row = 0;
        int column = 0;
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            for (int i = 0; i < offset; ++i) {
                int c = reader.read();
                if (c == '\n') {
                    row++;
                    column = 0;
                } else {
                    column++;
                }
            }
        }
        return new Location(row, column);
    }

    static class Location {
        int row;
        int column;

        Location(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }
}
