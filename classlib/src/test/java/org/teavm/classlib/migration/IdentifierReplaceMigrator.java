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

/**
 * {@link LineMigrator} that replaces a fixed {@link String} with a replacement {@link String}.
 */
public class IdentifierReplaceMigrator implements LineMigrator {

    private final String match;

    private final int matchLength;

    private final String replacement;

    /**
     * The constructor.
     *
     * @param match the fixed identifier to search.
     * @param replacement the identifier used to replace the given {@code match} if found.
     */
    public IdentifierReplaceMigrator(String match, String replacement) {

        super();
        this.match = match;
        this.matchLength = match.length();
        this.replacement = replacement;
    }

    @Override
    public String migrate(String line) {

        int start = 0;
        int length = line.length();
        StringBuilder sb = null;
        do {
            int index = line.indexOf(this.match, start);
            int end = index + this.matchLength;
            boolean matches = (index >= 0);
            if (matches) {
                char c = 0;
                if (index > 0) {
                    c = line.charAt(index - 1);
                }
                matches = (!Character.isJavaIdentifierStart(c));
                if (matches && (end < length)) {
                    c = line.charAt(end);
                    matches = (!Character.isJavaIdentifierPart(c));
                }
            }
            if (matches) {
                if (sb == null) {
                    sb = new StringBuilder(length);
                }
                sb.append(line.substring(start, index));
                sb.append(this.replacement);
                start = end;
            } else {
                if ((start < length) && (sb != null)) {
                    sb.append(line.substring(start));
                }
                start = -1;
            }
        } while (start > 0);
        if (sb == null) {
            return line;
        }
        return sb.toString();
    }

}
