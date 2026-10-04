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
package org.teavm.classlib.java.util.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * Patterns passed as literals to {@link Pattern#compile(String)} are compiled in build time.
 * These tests check that such patterns behave exactly like patterns compiled in run time.
 */
@TeaVMTest
public class PatternLiteralTest {
    private static final String[] INPUTS = {
        "", "abc", "ABC", "a1b22c333", "foo bar  baz\tqux", "2026-10-04, 1999-01-31", "x+y=z; (a|b)",
        "line1\nline2\r\nline3", "aaa bbb aaa", "Hello, World!", "été фоо",
        "😀 smile 😁", "foo.bar@example.com"
    };

    @Test
    public void sameAsRuntimeCompiled() {
        check(Pattern.compile("a"), "a");
        check(Pattern.compile("abc"), "abc");
        check(Pattern.compile("\\d+"), "\\d+");
        check(Pattern.compile("\\D+"), "\\D+");
        check(Pattern.compile("\\w+"), "\\w+");
        check(Pattern.compile("\\W"), "\\W");
        check(Pattern.compile("\\s+"), "\\s+");
        check(Pattern.compile("\\S+"), "\\S+");
        check(Pattern.compile("[a-z]+"), "[a-z]+");
        check(Pattern.compile("[^a-z ]+"), "[^a-z ]+");
        check(Pattern.compile("[a-cx-z0-2]"), "[a-cx-z0-2]");
        check(Pattern.compile("[\\w.]+@[\\w.]+"), "[\\w.]+@[\\w.]+");
        check(Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})"), "(\\d{4})-(\\d{2})-(\\d{2})");
        check(Pattern.compile("(?<year>\\d{4})-(?<month>\\d\\d)"), "(?<year>\\d{4})-(?<month>\\d\\d)");
        check(Pattern.compile("a*"), "a*");
        check(Pattern.compile("a+?"), "a+?");
        check(Pattern.compile("a*+"), "a*+");
        check(Pattern.compile("a?b"), "a?b");
        check(Pattern.compile("a{2,3}"), "a{2,3}");
        check(Pattern.compile("a{2,}?"), "a{2,}?");
        check(Pattern.compile("(ab)+"), "(ab)+");
        check(Pattern.compile("(?:ab|c)*"), "(?:ab|c)*");
        check(Pattern.compile("(a|b)*?c"), "(a|b)*?c");
        check(Pattern.compile("(?:a|bc){1,2}"), "(?:a|bc){1,2}");
        check(Pattern.compile("(?>a|ab)c"), "(?>a|ab)c");
        check(Pattern.compile("foo|bar|baz"), "foo|bar|baz");
        check(Pattern.compile("a(?=b)"), "a(?=b)");
        check(Pattern.compile("a(?!b)"), "a(?!b)");
        check(Pattern.compile("(?<=a)b"), "(?<=a)b");
        check(Pattern.compile("(?<!a)b"), "(?<!a)b");
        check(Pattern.compile("^\\w+"), "^\\w+");
        check(Pattern.compile("\\w+$"), "\\w+$");
        check(Pattern.compile("(?m)^\\w+$"), "(?m)^\\w+$");
        check(Pattern.compile("\\bba"), "\\bba");
        check(Pattern.compile("a\\B"), "a\\B");
        check(Pattern.compile(".+"), ".+");
        check(Pattern.compile("(?s).+"), "(?s).+");
        check(Pattern.compile("\\Aa|c\\z"), "\\Aa|c\\z");
        check(Pattern.compile("\\Ga"), "\\Ga");
        check(Pattern.compile("[\\p{L}]+"), "[\\p{L}]+");
        check(Pattern.compile("\\p{Lu}"), "\\p{Lu}");
        check(Pattern.compile("\\P{L}+"), "\\P{L}+");
        check(Pattern.compile("\\p{Punct}"), "\\p{Punct}");
        check(Pattern.compile("\\p{InGreek}"), "\\p{InGreek}");
        check(Pattern.compile("😀"), "😀");
        check(Pattern.compile("[😀-😁]"), "[😀-😁]");
        check(Pattern.compile("\\Q(a|b)\\E"), "\\Q(a|b)\\E");
        check(Pattern.compile("(a)|(b)"), "(a)|(b)");
        check(Pattern.compile("(\\w)\\1"), "(\\w)\\1");
        check(Pattern.compile("\\p{javaLowerCase}+"), "\\p{javaLowerCase}+");
    }

    @Test
    public void sameAsRuntimeCompiledWithFlags() {
        check(Pattern.compile("abc", Pattern.CASE_INSENSITIVE), "abc", Pattern.CASE_INSENSITIVE);
        check(Pattern.compile("[a-c]+", Pattern.CASE_INSENSITIVE), "[a-c]+", Pattern.CASE_INSENSITIVE);
        check(Pattern.compile("(?i)hello"), "(?i)hello", 0);
        check(Pattern.compile("^line\\d$", Pattern.MULTILINE), "^line\\d$", Pattern.MULTILINE);
        check(Pattern.compile("^line\\d$", Pattern.MULTILINE | Pattern.UNIX_LINES), "^line\\d$",
                Pattern.MULTILINE | Pattern.UNIX_LINES);
        check(Pattern.compile("e.l", Pattern.DOTALL), "e.l", Pattern.DOTALL);
        check(Pattern.compile("a b # comment", Pattern.COMMENTS), "a b # comment", Pattern.COMMENTS);
        check(Pattern.compile("(a|b)", Pattern.LITERAL), "(a|b)", Pattern.LITERAL);
        check(Pattern.compile("ÉTÉ", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), "ÉTÉ",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Test
    public void properties() {
        var pattern = Pattern.compile("(?<word>\\w+)-(\\d+)", Pattern.CASE_INSENSITIVE);
        assertEquals("(?<word>\\w+)-(\\d+)", pattern.pattern());
        assertEquals("(?<word>\\w+)-(\\d+)", pattern.toString());
        assertEquals(Pattern.CASE_INSENSITIVE, pattern.flags());
        assertEquals(Map.of("word", 1), pattern.namedGroups());

        var matcher = pattern.matcher("abc-123");
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertEquals("abc", matcher.group("word"));
        assertEquals("123", matcher.group(2));
    }

    @Test
    public void syntaxErrorReportedInRuntime() {
        assertThrows(PatternSyntaxException.class, () -> Pattern.compile("(abc"));
        assertThrows(PatternSyntaxException.class, () -> Pattern.compile("[a-"));
        assertThrows(IllegalArgumentException.class, () -> Pattern.compile("abc", 0xFFFFFF));
    }

    @Test
    public void newInstanceEachTime() {
        var patterns = new ArrayList<Pattern>();
        for (var i = 0; i < 2; ++i) {
            patterns.add(Pattern.compile("a+b"));
        }
        assertFalse(patterns.get(0) == patterns.get(1));
        assertEquals(List.of("", "c"), List.of(patterns.get(0).split("aabc")));
        assertEquals(List.of("", "c"), List.of(patterns.get(1).split("aabc")));
    }

    private static void check(Pattern precompiled, String source) {
        check(precompiled, source, 0);
    }

    private static void check(Pattern precompiled, String source, int flags) {
        var expected = Pattern.compile(dynamic(source), flags);
        assertEquals(expected.pattern(), precompiled.pattern());
        assertEquals(expected.flags(), precompiled.flags());
        assertEquals(expected.namedGroups(), precompiled.namedGroups());
        for (var input : INPUTS) {
            assertEquals(describeMatches(expected, input), describeMatches(precompiled, input),
                    "Pattern " + source + " on input " + input);
            assertEquals(List.of(expected.split(input)), List.of(precompiled.split(input)),
                    "Pattern " + source + " splitting input " + input);
        }
    }

    private static String describeMatches(Pattern pattern, String input) {
        var sb = new StringBuilder();
        var matcher = pattern.matcher(input);
        sb.append(matcher.matches()).append(';').append(matcher.lookingAt()).append(';');
        matcher.reset();
        while (matcher.find()) {
            sb.append('[').append(matcher.start()).append(',').append(matcher.end());
            for (var i = 1; i <= matcher.groupCount(); ++i) {
                sb.append(',').append(matcher.group(i));
            }
            sb.append(']');
        }
        sb.append(';').append(matcher.hitEnd());
        return sb.toString();
    }

    // Prevents pattern from being compiled in build time
    private static String dynamic(String value) {
        return new StringBuilder(value).toString();
    }
}
