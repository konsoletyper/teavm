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
package org.teavm.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses {@code @CsvSource} records at compile time, following the rules documented for
 * {@code @CsvSource}: unquoted empty values are {@code null}, quoted empty values are replaced
 * with {@code emptyValue}, a doubled quote character inside a quoted value stands for the quote
 * character itself, values that match one of {@code nullValues} become {@code null}, and in text
 * blocks every line is a record, while blank lines and lines starting with {@code #} are skipped.
 */
final class JupiterCsvParser {
    private final String delimiter;
    private final char quote;
    private final String emptyValue;
    private final Set<String> nullValues;
    private final boolean trim;

    private String text;
    private int pos;

    JupiterCsvParser(String delimiter, char quote, String emptyValue, Set<String> nullValues, boolean trim) {
        this.delimiter = delimiter;
        this.quote = quote;
        this.emptyValue = emptyValue;
        this.nullValues = nullValues;
        this.trim = trim;
    }

    List<String> parseRecord(String record) {
        text = record;
        pos = 0;
        var result = parseRecord(false);
        if (pos < text.length()) {
            throw new IllegalArgumentException("unexpected line break in record: " + record);
        }
        return result;
    }

    List<List<String>> parseTextBlock(String textBlock) {
        text = textBlock;
        pos = 0;
        var result = new ArrayList<List<String>>();
        while (pos < text.length()) {
            var lineStart = pos;
            skipWhitespace();
            if (atLineEnd()) {
                skipLineEnd();
                continue;
            }
            if (text.charAt(pos) == '#') {
                while (!atLineEnd()) {
                    pos++;
                }
                skipLineEnd();
                continue;
            }
            pos = lineStart;
            result.add(parseRecord(true));
            skipLineEnd();
        }
        return result;
    }

    private List<String> parseRecord(boolean multiline) {
        var values = new ArrayList<String>();
        while (true) {
            values.add(parseValue(multiline));
            if (text.startsWith(delimiter, pos)) {
                pos += delimiter.length();
            } else {
                break;
            }
        }
        return values;
    }

    private String parseValue(boolean multiline) {
        var start = pos;
        skipWhitespace();
        if (pos < text.length() && text.charAt(pos) == quote) {
            return parseQuotedValue();
        }
        if (!trim) {
            pos = start;
        }

        var sb = new StringBuilder();
        while (pos < text.length() && !text.startsWith(delimiter, pos) && !(multiline && atLineEnd())) {
            sb.append(text.charAt(pos++));
        }
        var value = sb.toString();
        if (trim) {
            value = value.strip();
        }
        if (value.isEmpty()) {
            return null;
        }
        return nullValues.contains(value) ? null : value;
    }

    private String parseQuotedValue() {
        pos++;
        var sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw new IllegalArgumentException("unterminated quoted value");
            }
            var c = text.charAt(pos++);
            if (c == quote) {
                if (pos < text.length() && text.charAt(pos) == quote) {
                    sb.append(quote);
                    pos++;
                } else {
                    break;
                }
            } else {
                sb.append(c);
            }
        }
        skipWhitespace();
        if (pos < text.length() && !text.startsWith(delimiter, pos) && !atLineEnd()) {
            throw new IllegalArgumentException("unexpected character after quoted value: " + text.charAt(pos));
        }

        var value = sb.toString();
        if (value.isEmpty()) {
            return emptyValue;
        }
        return nullValues.contains(value) ? null : value;
    }

    private void skipWhitespace() {
        while (pos < text.length() && (text.charAt(pos) == ' ' || text.charAt(pos) == '\t')
                && !text.startsWith(delimiter, pos)) {
            pos++;
        }
    }

    private boolean atLineEnd() {
        return pos >= text.length() || text.charAt(pos) == '\n' || text.charAt(pos) == '\r';
    }

    private void skipLineEnd() {
        if (pos < text.length() && text.charAt(pos) == '\r') {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '\n') {
            pos++;
        }
    }
}
