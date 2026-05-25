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
package org.teavm.extension.spi;

public class GlobMatch {
    private GlobMatch() {
    }

    public static boolean match(String pattern, String name) {
        return match(pattern, name, 0, 0);
    }

    private static boolean match(String pattern, String name, int patternIndex, int nameIndex) {
        if (patternIndex == pattern.length()) {
            return nameIndex == name.length();
        }
        var starIndex = pattern.indexOf('*', patternIndex);
        if (starIndex < 0) {
            var remainingLen = pattern.length() - patternIndex;
            return nameIndex + remainingLen == name.length()
                    && pattern.regionMatches(patternIndex, name, nameIndex, remainingLen);
        }
        if (!pattern.regionMatches(patternIndex, name, nameIndex, starIndex - patternIndex)) {
            return false;
        }
        nameIndex += starIndex - patternIndex;
        if (starIndex == pattern.length() - 1) {
            return name.indexOf('.', nameIndex) < 0;
        } else if (pattern.charAt(starIndex + 1) == '*') {
            if (starIndex + 2 == pattern.length()) {
                return true;
            }
            var nextChar = pattern.charAt(starIndex + 2);
            var index = nameIndex;
            while (true) {
                var next = name.indexOf(nextChar, index);
                if (next < 0) {
                    return false;
                }
                if (match(pattern, name, starIndex + 3, next + 1)) {
                    return true;
                }
                index = next + 1;
            }
        } else {
            var nextChar = pattern.charAt(starIndex + 1);
            var dotPos = name.indexOf('.', nameIndex);
            var index = nameIndex;
            while (true) {
                var next = name.indexOf(nextChar, index);
                if (next < 0 || (dotPos >= 0 && dotPos < next)) {
                    return false;
                }
                if (match(pattern, name, starIndex + 2, next + 1)) {
                    return true;
                }
                index = next + 1;
            }
        }
    }
}
