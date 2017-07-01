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
package org.teavm.classlib.java.text;

import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.java.text.TDecimalFormat.FormatField;

class TDecimalFormatParser {
    private FormatField[] positivePrefix;
    private FormatField[] positiveSuffix;
    private FormatField[] negativePrefix;
    private FormatField[] negativeSuffix;
    private int groupSize;
    private int minimumIntLength;
    private int intLength;
    private int minimumFracLength;
    private int fracLength;
    private int exponentLength;
    private boolean decimalSeparatorRequired;
    private String string;
    private int index;
    private int multiplier;

    public void parse(String string) {
        groupSize = 0;
        minimumFracLength = 0;
        fracLength = 0;
        exponentLength = 0;
        decimalSeparatorRequired = false;
        multiplier = 1;
        this.string = string;
        index = 0;
        positivePrefix = parseText(false, false);
        if (index == string.length()) {
            throw new IllegalArgumentException("Positive number pattern not found in " + string);
        }
        parseNumber(true);
        negativePrefix = null;
        negativeSuffix = null;
        if (index < string.length() && string.charAt(index) != ';') {
            positiveSuffix = parseText(true, false);
        }
        if (index < string.length()) {
            if (string.charAt(index++) != ';') {
                throw new IllegalArgumentException("Expected ';' at " + index + " in " + string);
            }
            negativePrefix = parseText(false, true);
            parseNumber(false);
            negativeSuffix = parseText(true, true);
        }
    }

    public void apply(TDecimalFormat format) {
        format.positivePrefix = positivePrefix;
        format.positiveSuffix = positiveSuffix;
        if (negativePrefix != null) {
            format.negativePrefix = negativePrefix;
        } else {
            format.negativePrefix = new FormatField[positivePrefix.length + 1];
            System.arraycopy(positivePrefix, 0, format.negativePrefix, 1, positivePrefix.length);
            format.negativePrefix[0] = new TDecimalFormat.MinusField();
        }
        format.negativeSuffix = negativeSuffix != null ? negativeSuffix : positiveSuffix;
        format.setGroupingSize(groupSize);
        format.setGroupingUsed(groupSize > 0);
        format.setMinimumIntegerDigits(!decimalSeparatorRequired ? minimumIntLength
                : Math.max(1, minimumIntLength));
        format.setMaximumIntegerDigits(intLength);
        format.setMinimumFractionDigits(minimumFracLength);
        format.setMaximumFractionDigits(fracLength);
        format.setDecimalSeparatorAlwaysShown(decimalSeparatorRequired);
        format.exponentDigits = exponentLength;
        format.setMultiplier(multiplier);
    }

    FormatField[] parseText(boolean suffix, boolean end) {
        List<FormatField> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        loop: while (index < string.length()) {
            char c = string.charAt(index);
            switch (c) {
                case '#':
                case '0':
                    if (suffix) {
                        throw new IllegalArgumentException("Prefix contains special character at " + index + " in "
                                + string);
                    }
                    break loop;
                case ';':
                    if (end) {
                        throw new IllegalArgumentException("Prefix contains special character at " + index + " in "
                                + string);
                    }
                    break loop;
                case '.':
                case 'E':
                    throw new IllegalArgumentException("Prefix contains special character at " + index + " in "
                            + string);
                case '\'': {
                    ++index;
                    int next = string.indexOf('\'', index);
                    if (next < 0) {
                        throw new IllegalArgumentException("Quote opened at " + index + " was not closed in "
                                + string);
                    }
                    if (next == index) {
                        sb.append('\'');
                    } else {
                        sb.append(string.substring(index, next));
                    }
                    index = next + 1;
                    break;
                }
                // Currency symbol ¤
                case '\u00A4':
                    if (sb.length() > 0) {
                        fields.add(new TDecimalFormat.TextField(sb.toString()));
                        sb.setLength(0);
                    }
                    fields.add(new TDecimalFormat.CurrencyField());
                    ++index;
                    break;
                case '%':
                    if (sb.length() > 0) {
                        fields.add(new TDecimalFormat.TextField(sb.toString()));
                        sb.setLength(0);
                    }
                    fields.add(new TDecimalFormat.PercentField());
                    ++index;
                    multiplier = 100;
                    break;
                // Per mill symbol
                case '\u2030':
                    if (sb.length() > 0) {
                        fields.add(new TDecimalFormat.TextField(sb.toString()));
                        sb.setLength(0);
                    }
                    fields.add(new TDecimalFormat.PerMillField());
                    ++index;
                    multiplier = 1000;
                    break;
                case '-':
                    if (sb.length() > 0) {
                        fields.add(new TDecimalFormat.TextField(sb.toString()));
                        sb.setLength(0);
                    }
                    fields.add(new TDecimalFormat.MinusField());
                    ++index;
                    break;
                default:
                    sb.append(c);
                    ++index;
                    break;
            }
        }
        if (sb.length() > 0) {
            fields.add(new TDecimalFormat.TextField(sb.toString()));
        }
        return fields.toArray(new FormatField[fields.size()]);
    }

    private void parseNumber(boolean apply) {
        parseIntegerPart(apply);
        if (index < string.length() && string.charAt(index) == '.') {
            ++index;
            parseFractionalPart(apply);
        }
        if (index < string.length() && string.charAt(index) == 'E') {
            ++index;
            parseExponent(apply);
        }
    }

    private void parseIntegerPart(boolean apply) {
        int start = index;
        int lastGroup = index;
        boolean optionalDigits = true;
        int length = 0;
        int minimumLength = 0;
        loop: while (index < string.length()) {
            switch (string.charAt(index)) {
                case '#':
                    if (!optionalDigits) {
                        throw new IllegalArgumentException("Unexpected '#' at non-optional digit part at " + index
                                + " in " + string);
                    }
                    ++length;
                    break;
                case ',':
                    if (lastGroup == index) {
                        throw new IllegalArgumentException("Two group separators at " + index + " in " + string);
                    }
                    if (apply) {
                        groupSize = index - lastGroup;
                    }
                    lastGroup = index + 1;
                    break;
                case '0':
                    optionalDigits = false;
                    ++length;
                    ++minimumLength;
                    break;
                default:
                    break loop;
            }
            ++index;
        }
        if (length == 0) {
            throw new IllegalArgumentException("Pattern does not specify integer digits at " + index
                    + " in " + string);
        }
        if (lastGroup == index) {
            throw new IllegalArgumentException("Group separator at the end of number at " + index + " in " + string);
        }
        if (apply && lastGroup > start) {
            groupSize = index - lastGroup;
        }
        if (apply) {
            intLength = length;
            minimumIntLength = minimumLength;
        }
    }

    private void parseFractionalPart(boolean apply) {
        boolean optionalDigits = false;
        int length = 0;
        int minimumLength = 0;
        loop: while (index < string.length()) {
            switch (string.charAt(index)) {
                case '#':
                    ++length;
                    optionalDigits = true;
                    break;
                case ',':
                    throw new IllegalArgumentException("Group separator found at fractional part at " + index
                            + " in " + string);
                case '0':
                    if (optionalDigits) {
                        throw new IllegalArgumentException("Unexpected '0' at optional digit part at " + index
                                + " in " + string);
                    }
                    ++length;
                    ++minimumLength;
                    break;
                case '.':
                    throw new IllegalArgumentException("Unexpected second decimal separator at " + index
                            + " in " + string);
                default:
                    break loop;
            }
            ++index;
        }
        if (apply) {
            fracLength = length;
            minimumFracLength = minimumLength;
            decimalSeparatorRequired = length == 0;
        }
    }

    private void parseExponent(boolean apply) {
        int length = 0;
        loop: while (index < string.length()) {
            switch (string.charAt(index)) {
                case '#':
                case ',':
                case '.':
                case 'E':
                    throw new IllegalArgumentException("Unexpected char at exponent at " + index + " in " + string);
                case '0':
                    ++length;
                    break;
                default:
                    break loop;
            }
            ++index;
        }
        if (length == 0) {
            throw new IllegalArgumentException("Pattern does not specify exponent digits at " + index
                    + " in " + string);
        }
        if (apply) {
            exponentLength = length;
        }
    }
}
