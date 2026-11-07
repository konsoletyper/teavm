/*
 *  Copyright 2026 Simon Allegraud.
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
package org.teavm.classlib.java.util.stream.impl;

import java.util.function.Predicate;
import org.teavm.classlib.java.lang.TString;

public class TStringLinesStream extends TSimpleStreamImpl<TString> {
    private final TString string;
    private int index;

    public TStringLinesStream(TString string) {
        this.string = string;
    }

    @Override
    public boolean next(Predicate<? super TString> consumer) {
        var length = string.length();
        while (index < length) {
            var start = index;
            var end = start;
            while (end < length) {
                var c = string.charAt(end);
                if (c == '\n' || c == '\r') {
                    break;
                }
                ++end;
            }
            index = end;
            if (index < length && string.charAt(index++) == '\r' && index < length
                    && string.charAt(index) == '\n') {
                ++index;
            }
            if (!consumer.test(string.substring(start, end))) {
                break;
            }
        }
        return index < length;
    }
}
