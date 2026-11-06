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
package org.teavm.benchmarks.classlib;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Quantifiers over single characters, character classes and dot. Every benchmark processes about 1K chars,
 * {@link #itemLength} is the length of a single item (field, word, tag) matched by a quantifier.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class RegexQuantifierBenchmark {
    private static final int TEXT_LENGTH = 1024;

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern FIELD = Pattern.compile("[^,]+");
    private static final Pattern WORD = Pattern.compile("[a-z]+");
    private static final Pattern BOUNDED_WORD = Pattern.compile("[a-z]{1,16}");
    private static final Pattern UNICODE_WORD = Pattern.compile("\\p{L}+");
    private static final Pattern TAG = Pattern.compile("<(.*?)>");
    private static final Pattern GREEDY_DOT = Pattern.compile("(.*),");
    private static final Pattern GROUP_LOOP = Pattern.compile("(?:ab)+");

    @Param({ "8", "128" })
    public int itemLength;

    private String quoted;
    private String unterminatedQuote;
    private String csv;
    private String words;
    private String unicodeWords;
    private String tags;
    private String abs;

    @Setup
    public void setup() {
        var count = Math.max(1, TEXT_LENGTH / (itemLength + 1));
        var csvBuilder = new StringBuilder();
        var wordsBuilder = new StringBuilder();
        var unicodeBuilder = new StringBuilder();
        var tagsBuilder = new StringBuilder();
        var quotedBuilder = new StringBuilder("\"");
        for (var i = 0; i < count; ++i) {
            var item = item(i);
            if (i > 0) {
                csvBuilder.append(',');
                wordsBuilder.append(' ');
                unicodeBuilder.append(i % 4 == 0 ? " 😀 " : " ");
            }
            csvBuilder.append(item);
            wordsBuilder.append(item);
            unicodeBuilder.append(i % 2 == 0 ? item : cyrillic(item));
            tagsBuilder.append('<').append(item, 0, item.length() / 2).append("> ")
                    .append(item, item.length() / 2, item.length());
            quotedBuilder.append(item).append(' ');
        }
        quoted = quotedBuilder.append('"').toString();
        unterminatedQuote = quoted.substring(0, quoted.length() - 1);
        csv = csvBuilder.toString();
        words = wordsBuilder.toString();
        unicodeWords = unicodeBuilder.toString();
        tags = tagsBuilder.toString();
        abs = "ab".repeat(TEXT_LENGTH / 2);
    }

    private String item(int index) {
        var chars = new char[itemLength];
        for (var i = 0; i < itemLength; ++i) {
            chars[i] = (char) ('a' + (index * 7 + i * 3) % 26);
        }
        return new String(chars);
    }

    private static String cyrillic(String s) {
        var chars = s.toCharArray();
        for (var i = 0; i < chars.length; ++i) {
            chars[i] = (char) (chars[i] - 'a' + 'а');
        }
        return new String(chars);
    }

    private static int count(Matcher matcher) {
        var result = 0;
        while (matcher.find()) {
            result += matcher.end() - matcher.start();
        }
        return result;
    }

    /**
     * Greedy negated class inside a group, whole input matches.
     */
    @Benchmark
    public String quotedString() {
        var matcher = QUOTED.matcher(quoted);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * Greedy negated class that has to backtrack over the whole input before failing.
     */
    @Benchmark
    public boolean unterminatedQuote() {
        return QUOTED.matcher(unterminatedQuote).find();
    }

    /**
     * Find loop with greedy negated class.
     */
    @Benchmark
    public int findFields() {
        return count(FIELD.matcher(csv));
    }

    /**
     * Find loop with greedy positive class.
     */
    @Benchmark
    public int findWords() {
        return count(WORD.matcher(words));
    }

    /**
     * Find loop with bounded repetition of positive class.
     */
    @Benchmark
    public int findBoundedWords() {
        return count(BOUNDED_WORD.matcher(words));
    }

    /**
     * Find loop with Unicode category, input contains non-Latin letters and supplementary chars.
     */
    @Benchmark
    public int findUnicodeWords() {
        return count(UNICODE_WORD.matcher(unicodeWords));
    }

    /**
     * Find loop with reluctant dot.
     */
    @Benchmark
    public int findTags() {
        return count(TAG.matcher(tags));
    }

    /**
     * Greedy dot, which was not changed (control).
     */
    @Benchmark
    public int greedyDot() {
        var matcher = GREEDY_DOT.matcher(csv);
        return matcher.lookingAt() ? matcher.end(1) : -1;
    }

    /**
     * Quantified group, which was not changed (control).
     */
    @Benchmark
    public boolean groupLoop() {
        return GROUP_LOOP.matcher(abs).matches();
    }
}
