/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.js.lexer;

public class CharSequenceCodePointReader implements CodePointReader {
    private CharSequence charSequence;
    private int pos;

    public CharSequenceCodePointReader(CharSequence charSequence) {
        this.charSequence = charSequence;
    }

    @Override
    public int read() {
        if (pos >= charSequence.length()) {
            return -1;
        }
        var high = charSequence.charAt(pos++);
        if (pos < charSequence.length() && Character.isHighSurrogate(high)) {
            var low = charSequence.charAt(pos + 1);
            if (Character.isLowSurrogate(low)) {
                ++pos;
                return Character.toCodePoint(high, low);
            }
        }
        return high;
    }
}
