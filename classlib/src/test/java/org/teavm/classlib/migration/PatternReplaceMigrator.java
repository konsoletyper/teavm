/*
 *  Copyright 2020 Joerg Hohwiller.
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
package org.teavm.classlib.migration;

import java.util.regex.Pattern;

/**
 * {@link LineMigrator} that replaces a fixed {@link String} with a replacement {@link String}.
 */
public class PatternReplaceMigrator implements LineMigrator {

    private final Pattern match;

    private final String replacement;

    /**
     * The constructor.
     *
     * @param match the fixed {@link Pattern} to search.
     * @param replacement the {@link String} used to replace the given {@code match} if found.
     */
    public PatternReplaceMigrator(Pattern match, String replacement) {

        super();
        this.match = match;
        this.replacement = replacement;
    }

    @Override
    public String migrate(String line) {

        return this.match.matcher(line).replaceAll(this.replacement);
    }

}
