/*
 *  Copyright 2026 Ethan Resnick.
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

/**
 * Lazy stream behind {@code String.lines()}: finds each line only when the consumer asks for it.
 */
public class TStringLinesStream extends TSimpleStreamImpl<TString> {
    private final TString string;
    private int index;

    public TStringLinesStream(TString string) {
        this.string = string;
    }

    @Override
    public boolean next(Predicate<? super TString> consumer) {
        int length = string.length();
        while (index < length) {
            int start = index;
            int end = lineEnd(string, start);
            index = nextLineStart(string, end);
            if (!consumer.test(string.substring(start, end))) {
                break;
            }
        }
        return index < length;
    }

    /** Returns the index of the line terminator ({@code \n} or {@code \r}) at or after start, or the length. */
    public static int lineEnd(TString string, int start) {
        int length = string.length();
        int i = start;
        while (i < length) {
            char c = string.charAt(i);
            if (c == '\n' || c == '\r') {
                break;
            }
            i++;
        }
        return i;
    }

    /** Given the result of {@link #lineEnd}, returns where the next line starts, treating \r\n as one terminator. */
    public static int nextLineStart(TString string, int end) {
        int length = string.length();
        if (end >= length) {
            return length;
        }
        if (string.charAt(end) == '\r' && end + 1 < length && string.charAt(end + 1) == '\n') {
            return end + 2;
        }
        return end + 1;
    }
}
