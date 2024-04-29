/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.classlib.java.util.stream.intimpl;

import java.util.function.IntPredicate;
import org.teavm.classlib.java.lang.TCharacter;
import org.teavm.classlib.java.lang.TString;

public class TStringCodePointsStream extends TSimpleIntStreamImpl {
    private final TString string;
    private int index;

    public TStringCodePointsStream(TString string) {
        this.string = string;
    }

    @Override
    public boolean next(IntPredicate consumer) {
        while (index < string.length()) {
            var hi = string.charAt(index++);
            if (TCharacter.isHighSurrogate(hi) && index < string.length()) {
                var lo = string.charAt(index);
                if (TCharacter.isLowSurrogate(lo)) {
                    ++index;
                    if (!consumer.test(TCharacter.toCodePoint(hi, lo))) {
                        break;
                    }
                    continue;
                }
            }
            if (!consumer.test(hi)) {
                break;
            }
        }
        return index < string.length();
    }

    @Override
    protected int estimateSize() {
        return string.length();
    }

    @Override
    public long count() {
        return string.length();
    }
}
