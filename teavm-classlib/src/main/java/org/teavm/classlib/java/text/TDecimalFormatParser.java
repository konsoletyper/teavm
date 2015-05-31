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

/**
 *
 * @author Alexey Andreev
 */
class TDecimalFormatParser {
    private String positivePrefix;
    private String positiveSuffix;
    private String negativePrefix;
    private String negativeSuffix;
    private int groupSize;
    private int minimumIntLength;
    private int intLength;
    private int minimumFracLength;
    private int fracLength;
    private int exponentLength;
    private boolean decimalSeparatorRequired;
    private String string;
    private int index;

    public void parse(String string) {
        groupSize = 0;
        minimumFracLength = 0;
        fracLength = 0;
        exponentLength = 0;
        decimalSeparatorRequired = false;
        this.string = string;
        index = 0;
        positivePrefix = parseText(false, false);
        if (index == string.length()) {
            throw new IllegalArgumentException("Positive number pattern not found in " + string);
        }
        parseNumber(true);
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
        format.setPositivePrefix(positivePrefix);
        format.setPositiveSuffix(positiveSuffix);
        format.setNegativePrefix(negativePrefix != null ? negativePrefix :
                format.symbols.getMinusSign() + positivePrefix);
        format.setNegativeSuffix(negativeSuffix != null ? negativeSuffix : positiveSuffix);
        format.setGroupingSize(groupSize);
        format.setGroupingUsed(groupSize > 0);
        format.setMinimumIntegerDigits(minimumIntLength);
        format.setMaximumIntegerDigits(intLength);
        format.setMinimumFractionDigits(minimumFracLength);
        format.setMaximumFractionDigits(fracLength);
        format.setDecimalSeparatorAlwaysShown(decimalSeparatorRequired);
        format.exponentDigits = exponentLength;
    }

    private String parseText(boolean suffix, boolean end) {
        StringBuilder sb = new StringBuilder();
        loop: while (index < string.length()) {
            char c = string.charAt(index);
            switch (c) {
                case '#':
                case '0':
                    if (suffix) {
                        throw new IllegalArgumentException("Prefix contains special character at " + index + " in " +
                                string);
                    }
                    break loop;
                case ';':
                    if (end) {
                        throw new IllegalArgumentException("Prefix contains special character at " + index + " in " +
                                string);
                    }
                    break loop;
                case '.':
                case 'E':
                    throw new IllegalArgumentException("Prefix contains special character at " + index + " in " +
                            string);
                case '\'': {
                    ++index;
                    int next = string.indexOf('\'', index);
                    if (next < 0) {
                        throw new IllegalArgumentException("Quote opened at " + index + " was not closed in " +
                                string);
                    }
                    if (next == index) {
                        sb.append('\'');
                    } else {
                        sb.append(string.substring(index, next));
                    }
                    index = next + 1;
                    break;
                }
                default:
                    sb.append(c);
                    ++index;
                    break;
            }
        }
        return sb.toString();
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
        int lastGroup = index;
        boolean optionalDigits = true;
        int length = 0;
        int minimumLength = 0;
        loop: while (index < string.length()) {
            switch (string.charAt(index)) {
                case '#':
                    if (!optionalDigits) {
                        throw new IllegalArgumentException("Unexpected '#' at non-optional digit part at " + index +
                                " in " + string);
                    }
                    ++length;
                    break;
                case ',':
                    if (lastGroup + 1 == index) {
                        throw new IllegalArgumentException("Two commas at " + index + " in " + string);
                    }
                    if (apply) {
                        groupSize = index - lastGroup;
                    }
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
            throw new IllegalArgumentException("Pattern does not specify integer digits at " + index +
                    " in " + string);
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
                    throw new IllegalArgumentException("Group separator found at fractional part at " + index +
                            " in " + string);
                case '0':
                    if (!optionalDigits) {
                        throw new IllegalArgumentException("Unexpected '0' at optional digit part at " + index +
                                " in " + string);
                    }
                    ++length;
                    ++minimumLength;
                    break;
                case '.':
                    throw new IllegalArgumentException("Unexpected second decimal separator at " + index +
                            " in " + string);
                default:
                    break loop;
            }
            ++index;
        }
        if (apply) {
            fracLength = length;
            minimumFracLength = minimumLength;
            decimalSeparatorRequired = true;
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
                    throw new IllegalArgumentException("Unexpected char at exponent at " + index +
                            " in " + string);
                case '0':
                    ++length;
                    break;
                default:
                    break loop;
            }
            ++index;
        }
        if (length == 0) {
            throw new IllegalArgumentException("Pattern does not specify exponent digits at " + index +
                    " in " + string);
        }
        if (apply) {
            exponentLength = length;
        }
    }
}
