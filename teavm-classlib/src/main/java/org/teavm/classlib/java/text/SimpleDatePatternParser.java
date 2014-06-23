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
package org.teavm.classlib.java.text;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
class SimpleDatePatternParser {
    private TDateFormatSymbols symbols;
    private List<DateFormatElement> elements = new ArrayList<>();
    private int index;
    private String pattern;

    public SimpleDatePatternParser(TDateFormatSymbols symbols) {
        this.symbols = symbols;
    }

    public void parsePattern(String pattern) {
        this.pattern = pattern;
        for (index = 0; index < pattern.length(); ++index) {
            char c = pattern.charAt(index);
            switch (c) {
                case '\'': {
                    ++index;
                    parseQuoted();
                    break;
                }
                case 'G':
                    parseRepetitions();
                    elements.add(new DateFormatElement.EraText(symbols));
                    break;
                case 'y':
                    break;
                default: {
                    StringBuilder sb = new StringBuilder();
                    while (index < pattern.length() && isControl(pattern.charAt(index))) {
                        sb.append(pattern.charAt(index++));
                    }
                    break;
                }
            }
        }
    }

    private boolean isControl(char c) {
        switch (c) {
            case '\'':
            case 'G':
            case 'y':
            case 'Y':
            case 'M':
            case 'w':
            case 'W':
            case 'D':
            case 'd':
            case 'F':
            case 'E':
            case 'u':
            case 'a':
            case 'H':
            case 'k':
            case 'K':
            case 'h':
            case 'm':
            case 's':
            case 'S':
            case 'z':
            case 'Z':
            case 'X':
                return true;
            default:
                return false;
        }
    }

    private void parseQuoted() {
        StringBuilder sb = new StringBuilder();
        while (index < pattern.length()) {
            char c = pattern.charAt(index++);
            if (c == '\'') {
                if (index < pattern.length() && pattern.charAt(index) == '\'') {
                    sb.append('\'');
                    ++index;
                } else {
                    break;
                }
            } else {
                sb.append(c);
            }
        }
        elements.add(new DateFormatElement.ConstantText(sb.toString()));
    }

    private int parseRepetitions() {
        int count = 1;
        char orig = pattern.charAt(index++);
        while (index < pattern.length() && pattern.charAt(index) == orig) {
            ++index;
            ++count;
        }
        return count;
    }
}
