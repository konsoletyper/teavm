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
package org.teavm.classlib.impl.regex;

public final class SplitFastPath {
    private SplitFastPath() {
    }

    /**
     * Checks whether splitting by the given regular expression is equivalent to splitting by a single
     * character, i.e. whether regex is either a single character without special meaning,
     * or a backslash followed by a character which is neither ASCII letter nor digit.
     * Used both by {@code String.split} at run time and by {@link PatternCompileTransformer} at compile time.
     *
     * @return the character to split by, or -1 if fast path is not applicable.
     */
    public static int singleChar(String regex) {
        char ch;
        if (regex.length() == 1) {
            ch = regex.charAt(0);
            if (".$|()[{^?*+\\".indexOf(ch) >= 0) {
                return -1;
            }
        } else if (regex.length() == 2 && regex.charAt(0) == '\\') {
            ch = regex.charAt(1);
            if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                return -1;
            }
        } else {
            return -1;
        }
        return Character.isSurrogate(ch) ? -1 : ch;
    }
}
