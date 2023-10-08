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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util.regex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@SuppressWarnings("nls")
@RunWith(TeaVMTestRunner.class)
public class MatcherTest {
    String[] testPatterns = {
            "(a|b)*abb",
            "(1*2*3*4*)*567",
            "(a|b|c|d)*aab",
            "(1|2|3|4|5|6|7|8|9|0)(1|2|3|4|5|6|7|8|9|0)*",
            "(abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ)*",
            "(a|b)*(a|b)*A(a|b)*lice.*",
            "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)(a|b|c|d|e|f|g|h|"
                    + "i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)*(1|2|3|4|5|6|7|8|9|0)*|while|for|struct|if|do" };

    String[] groupPatterns = { "(a|b)*aabb", "((a)|b)*aabb", "((a|b)*)a(abb)",
            "(((a)|(b))*)aabb", "(((a)|(b))*)aa(b)b", "(((a)|(b))*)a(a(b)b)" };

    @Test
    public void testRegionsIntInt() {
        Pattern p = Pattern.compile("x*");
        Matcher m = p.matcher("axxxxxa");
        assertFalse(m.matches());

        m.region(1, 6);
        assertEquals(1, m.regionStart());
        assertEquals(6, m.regionEnd());
        assertTrue(m.matches());

        try {
            m.region(1, 0);
            fail("expected an IOOBE");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.region(-1, 2);
            fail("expected an IOOBE");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.region(10, 11);
            fail("expected an IOOBE");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        try {
            m.region(1, 10);
            fail("expected an IOOBE");
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
    }

    @Test
    public void testAppendReplacement() {
        Pattern pat = Pattern.compile("XX");
        Matcher m = pat.matcher("Today is XX-XX-XX ...");
        StringBuffer sb = new StringBuffer();

        for (int i = 0; m.find(); i++) {
            m.appendReplacement(sb, Integer.toString(i * 10 + i));
        }
        m.appendTail(sb);
        assertEquals("Today is 0-11-22 ...", sb.toString());
    }

    @Test
    public void testAppendReplacementRef() {
        Pattern p = Pattern.compile("xx (rur|\\$)");
        Matcher m = p.matcher("xx $ equals to xx rur.");
        StringBuffer sb = new StringBuffer();
        for (int i = 1; m.find(); i *= 30) {
            String rep = i + " $1";
            m.appendReplacement(sb, rep);
        }
        m.appendTail(sb);
        assertEquals("1 $ equals to 30 rur.", sb.toString());
    }

    @Test
    public void testReplaceAll() {
        String input = "aabfooaabfooabfoob";
        String pattern = "a*b";
        Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(input);

        assertEquals("-foo-foo-foo-", mat.replaceAll("-"));
    }

    @Test
    public void testResetCharSequence() {
        Pattern p = Pattern.compile("abcd");
        Matcher m = p.matcher("abcd");
        assertTrue(m.matches());
        m.reset("efgh");
        assertFalse(m.matches());

        try {
            m.reset(null);
            fail("expected a NPE");
        } catch (NullPointerException e) {
            // ok
        }
    }

    @Test
    public void testAppendSlashes() {
        Pattern p = Pattern.compile("\\\\");
        Matcher m = p.matcher("one\\cat\\two\\cats\\in\\the\\yard");
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "\\\\");
        }
        m.appendTail(sb);
        assertEquals("one\\cat\\two\\cats\\in\\the\\yard", sb.toString());
    }

    @Test
    public void testReplaceFirst() {
        String input = "zzzdogzzzdogzzz";
        String pattern = "dog";
        Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(input);

        assertEquals("zzzcatzzzdogzzz", mat.replaceFirst("cat"));
    }

    @Test
    public void testPattern() {
        for (String element : testPatterns) {
            Pattern test = Pattern.compile(element);
            assertEquals(test, test.matcher("aaa").pattern());
        }

        for (String element : testPatterns) {
            assertEquals(element, Pattern.compile(element).matcher("aaa")
                    .pattern().toString());
        }
    }

    /*
     * Class under test for Matcher reset()
     */
    public void testReset() {
    }

    @Test
    public void testGroupint() {
        String positiveTestString = "ababababbaaabb";

        // test IndexOutOfBoundsException
        // //
        for (int i = 0; i < groupPatterns.length; i++) {
            Pattern test = Pattern.compile(groupPatterns[i]);
            Matcher mat = test.matcher(positiveTestString);
            mat.matches();
            try {
                // groupPattern <index + 1> equals to number of groups
                // of the specified pattern
                // //
                mat.group(i + 2);
                fail("IndexOutBoundsException expected");
                mat.group(i + 100);
                fail("IndexOutBoundsException expected");
                mat.group(-1);
                fail("IndexOutBoundsException expected");
                mat.group(-100);
                fail("IndexOutBoundsException expected");
            } catch (IndexOutOfBoundsException iobe) {
                // ok
            }
        }

        String[][] groupResults = { { "a" }, { "a", "a" },
                { "ababababba", "a", "abb" }, { "ababababba", "a", "a", "b" },
                { "ababababba", "a", "a", "b", "b" },
                { "ababababba", "a", "a", "b", "abb", "b" }, };

        for (int i = 0; i < groupPatterns.length; i++) {
            Pattern test = Pattern.compile(groupPatterns[i]);
            Matcher mat = test.matcher(positiveTestString);
            mat.matches();
            for (int j = 0; j < groupResults[i].length; j++) {
                assertEquals("i: " + i + " j: " + j, groupResults[i][j], mat
                        .group(j + 1));
            }
        }
    }

    @Test
    public void testGroup() {
        String positiveTestString = "ababababbaaabb";
        String negativeTestString = "gjhfgdsjfhgcbv";
        for (String element : groupPatterns) {
            Pattern test = Pattern.compile(element);
            Matcher mat = test.matcher(positiveTestString);
            mat.matches();
            // test result
            assertEquals(positiveTestString, mat.group());

            // test equal to group(0) result
            assertEquals(mat.group(0), mat.group());
        }

        for (String element : groupPatterns) {
            Pattern test = Pattern.compile(element);
            Matcher mat = test.matcher(negativeTestString);
            mat.matches();
            try {
                mat.group();
                fail("IllegalStateException expected for <false> matches result");
            } catch (IllegalStateException ise) {
                // ok
            }
        }
    }

    @Test
    public void testGroupPossessive() {
        Pattern pat = Pattern.compile("((a)|(b))++c");
        Matcher mat = pat.matcher("aac");

        mat.matches();
        assertEquals("a", mat.group(1));
    }

    /*
     * Class under test for boolean find(int)
     */
    public void testFindint() {
    }

    /*
     * Class under test for int start(int)
     */
    public void testStartint() {
    }

    /*
     * Class under test for int end(int)
     */
    public void testEndint() {
    }

    @Test
    public void testMatchesMisc() {
        String[][] posSeq = {
                { "abb", "ababb", "abababbababb", "abababbababbabababbbbbabb" },
                { "213567", "12324567", "1234567", "213213567",
                        "21312312312567", "444444567" },
                { "abcdaab", "aab", "abaab", "cdaab", "acbdadcbaab" },
                { "213234567", "3458", "0987654", "7689546432", "0398576",
                        "98432", "5" },
                {
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" },
                { "ababbaAabababblice", "ababbaAliceababab", "ababbAabliceaaa",
                        "abbbAbbbliceaaa", "Alice" },
                { "a123", "bnxnvgds156", "for", "while", "if", "struct" }
        };

        for (int i = 0; i < testPatterns.length; i++) {
            Pattern pat = Pattern.compile(testPatterns[i]);
            for (int j = 0; j < posSeq[i].length; j++) {
                Matcher mat = pat.matcher(posSeq[i][j]);
                assertTrue("Incorrect match: " + testPatterns[i] + " vs "
                        + posSeq[i][j], mat.matches());
            }
        }
    }

    @Test
    public void testMatchesQuantifiers() {
        String[] testPatternsSingles = { "a{5}", "a{2,4}", "a{3,}" };
        String[] testPatternsMultiple = { "((a)|(b)){1,2}abb",
                "((a)|(b)){2,4}", "((a)|(b)){3,}" };

        String[][] stringSingles = { { "aaaaa", "aaa" },
                { "aa", "a", "aaa", "aaaaaa", "aaaa", "aaaaa" },
                { "aaa", "a", "aaaa", "aa" }, };

        String[][] stringMultiples = { { "ababb", "aba" },
                { "ab", "b", "bab", "ababa", "abba", "abababbb" },
                { "aba", "b", "abaa", "ba" }, };

        for (int i = 0; i < testPatternsSingles.length; i++) {
            Pattern pat = Pattern.compile(testPatternsSingles[i]);
            for (int j = 0; j < stringSingles.length / 2; j++) {
                assertTrue("Match expected, but failed: " + pat.pattern()
                        + " : " + stringSingles[i][j], pat.matcher(
                        stringSingles[i][j * 2]).matches());
                assertFalse("Match failure expected, but match succeed: "
                                + pat.pattern() + " : " + stringSingles[i][j * 2 + 1],
                        pat.matcher(stringSingles[i][j * 2 + 1]).matches());
            }
        }

        for (int i = 0; i < testPatternsMultiple.length; i++) {
            Pattern pat = Pattern.compile(testPatternsMultiple[i]);
            for (int j = 0; j < stringMultiples.length / 2; j++) {
                assertTrue("Match expected, but failed: " + pat.pattern()
                        + " : " + stringMultiples[i][j], pat.matcher(
                        stringMultiples[i][j * 2]).matches());
                assertFalse(
                        "Match failure expected, but match succeed: "
                                + pat.pattern() + " : "
                                + stringMultiples[i][j * 2 + 1], pat.matcher(
                                stringMultiples[i][j * 2 + 1]).matches());
            }
        }
    }

    @Test
    public void testQuantVsGroup() {
        String patternString = "(d{1,3})((a|c)*)(d{1,3})((a|c)*)(d{1,3})";
        String testString = "dacaacaacaaddaaacaacaaddd";

        Pattern pat = Pattern.compile(patternString);
        Matcher mat = pat.matcher(testString);

        mat.matches();
        assertEquals("dacaacaacaaddaaacaacaaddd", mat.group());
        assertEquals("d", mat.group(1));
        assertEquals("acaacaacaa", mat.group(2));
        assertEquals("dd", mat.group(4));
        assertEquals("aaacaacaa", mat.group(5));
        assertEquals("ddd", mat.group(7));
    }

    public void testLookingAt() {
    }

    /*
     * Class under test for boolean find()
     */
    public void testFind() {
        String testPattern = "(abb)";
        String testString = "cccabbabbabbabbabb";
        Pattern pat = Pattern.compile(testPattern);
        Matcher mat = pat.matcher(testString);
        int start = 3;
        int end = 6;
        while (mat.find()) {
            assertEquals(start, mat.start(1));
            assertEquals(end, mat.end(1));

            start = end;
            end += 3;
        }

        testPattern = "(\\d{1,3})";
        testString = "aaaa123456789045";

        Pattern pat2 = Pattern.compile(testPattern);
        Matcher mat2 = pat2.matcher(testString);
        start = 4;
        int length = 3;
        while (mat2.find()) {
            assertEquals(testString.substring(start, start + length), mat2
                    .group(1));
            start += length;
        }
    }

    @Test
    public void testSEOLsymbols() {
        Pattern pat = Pattern.compile("^a\\(bb\\[$");
        Matcher mat = pat.matcher("a(bb[");

        assertTrue(mat.matches());
    }

    /*
     * Class under test for int start()
     */
    public void testStart() {
    }

    @Test
    public void testGroupCount() {
        for (int i = 0; i < groupPatterns.length; i++) {
            Pattern test = Pattern.compile(groupPatterns[i]);
            Matcher mat = test.matcher("ababababbaaabb");
            mat.matches();
            assertEquals(i + 1, mat.groupCount());
        }
    }

    @Test
    public void testRelactantQuantifiers() {
        Pattern pat = Pattern.compile("(ab*)*b");
        Matcher mat = pat.matcher("abbbb");

        if (mat.matches()) {
            assertEquals("abbb", mat.group(1));
        } else {
            fail("Match expected: (ab*)*b vs abbbb");
        }
    }

    @Test
    public void testEnhancedFind() {
        String input = "foob";
        String pattern = "a*b";
        Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(input);

        mat.find();
        assertEquals("b", mat.group());
    }

    @Test
    public void testPosCompositeGroup() {
        String[] posExamples = { "aabbcc", "aacc", "bbaabbcc" };
        String[] negExamples = { "aabb", "bb", "bbaabb" };
        Pattern posPat = Pattern.compile("(aa|bb){1,3}+cc");
        Pattern negPat = Pattern.compile("(aa|bb){1,3}+bb");

        Matcher mat;
        for (String element : posExamples) {
            mat = posPat.matcher(element);
            assertTrue(mat.matches());
        }

        for (String element : negExamples) {
            mat = negPat.matcher(element);
            assertFalse(mat.matches());
        }

        assertTrue(Pattern.matches("(aa|bb){1,3}+bb", "aabbaabb"));
    }

    @Test
    public void testPosAltGroup() {
        String[] posExamples = { "aacc", "bbcc", "cc" };
        String[] negExamples = { "bb", "aa" };
        Pattern posPat = Pattern.compile("(aa|bb)?+cc");
        Pattern negPat = Pattern.compile("(aa|bb)?+bb");

        Matcher mat;
        for (String element : posExamples) {
            mat = posPat.matcher(element);
            assertTrue(posPat.toString() + " vs: " + element, mat.matches());
        }

        for (String element : negExamples) {
            mat = negPat.matcher(element);
            assertFalse(mat.matches());
        }

        assertTrue(Pattern.matches("(aa|bb)?+bb", "aabb"));
    }

    @Test
    public void testRelCompGroup() {

        Matcher mat;
        Pattern pat;
        String res = "";
        for (int i = 0; i < 4; i++) {
            pat = Pattern.compile("((aa|bb){" + i + ",3}?).*cc");
            mat = pat.matcher("aaaaaacc");
            assertTrue(pat.toString() + " vs: " + "aaaaaacc", mat.matches());
            assertEquals(res, mat.group(1));
            res += "aa";
        }
    }

    @Test
    public void testRelAltGroup() {

        Matcher mat;
        Pattern pat;

        pat = Pattern.compile("((aa|bb)??).*cc");
        mat = pat.matcher("aacc");
        assertTrue(pat.toString() + " vs: " + "aacc", mat.matches());
        assertEquals("", mat.group(1));

        pat = Pattern.compile("((aa|bb)??)cc");
        mat = pat.matcher("aacc");
        assertTrue(pat.toString() + " vs: " + "aacc", mat.matches());
        assertEquals("aa", mat.group(1));
    }

    @Test
    public void testIgnoreCase() {
        Pattern pat = Pattern.compile("(aa|bb)*", Pattern.CASE_INSENSITIVE);
        Matcher mat = pat.matcher("aAbb");

        assertTrue(mat.matches());

        pat = Pattern.compile("(a|b|c|d|e)*", Pattern.CASE_INSENSITIVE);
        mat = pat.matcher("aAebbAEaEdebbedEccEdebbedEaedaebEbdCCdbBDcdcdADa");
        assertTrue(mat.matches());

        pat = Pattern.compile("[a-e]*", Pattern.CASE_INSENSITIVE);
        mat = pat.matcher("aAebbAEaEdebbedEccEdebbedEaedaebEbdCCdbBDcdcdADa");
        assertTrue(mat.matches());
    }

    @Test
    public void testQuoteReplacement() {
        assertEquals("\\\\aaCC\\$1", Matcher.quoteReplacement("\\aaCC$1"));
    }

    @Test
    public void testOverFlow() {
        Pattern tp = Pattern.compile("(a*)*");
        Matcher tm = tp.matcher("aaa");
        assertTrue(tm.matches());
        assertEquals("", tm.group(1));

        assertTrue(Pattern.matches("(1+)\\1+", "11"));
        assertTrue(Pattern.matches("(1+)(2*)\\2+", "11"));

        Pattern pat = Pattern.compile("(1+)\\1*");
        Matcher mat = pat.matcher("11");

        assertTrue(mat.matches());
        assertEquals("11", mat.group(1));

        pat = Pattern.compile("((1+)|(2+))(\\2+)");
        mat = pat.matcher("11");

        assertTrue(mat.matches());
        assertEquals("1", mat.group(2));
        assertEquals("1", mat.group(1));
        assertEquals("1", mat.group(4));
        assertNull(mat.group(3));
    }

    @Test
    public void testUnicode() {

        assertTrue(Pattern.matches("\\x61a", "aa"));
        assertTrue(Pattern.matches("\\u0061a", "aa"));
        assertTrue(Pattern.matches("\\0141a", "aa"));
        assertTrue(Pattern.matches("\\0777", "?7"));
    }

    @Test
    public void testUnicodeCategory() {
        assertTrue(Pattern.matches("\\p{Ll}", "k")); // Unicode lower case
        assertTrue(Pattern.matches("\\P{Ll}", "K")); // Unicode non-lower
        // case
        assertTrue(Pattern.matches("\\p{Lu}", "K")); // Unicode upper case
        assertTrue(Pattern.matches("\\P{Lu}", "k")); // Unicode non-upper
        // case
        // combinations
        assertTrue(Pattern.matches("[\\p{L}&&[^\\p{Lu}]]", "k"));
        assertTrue(Pattern.matches("[\\p{L}&&[^\\p{Ll}]]", "K"));
        assertFalse(Pattern.matches("[\\p{L}&&[^\\p{Lu}]]", "K"));
        assertFalse(Pattern.matches("[\\p{L}&&[^\\p{Ll}]]", "k"));

        // category/character combinations
        assertFalse(Pattern.matches("[\\p{L}&&[^a-z]]", "k"));
        assertTrue(Pattern.matches("[\\p{L}&&[^a-z]]", "K"));

        assertTrue(Pattern.matches("[\\p{Lu}a-z]", "k"));
        assertTrue(Pattern.matches("[a-z\\p{Lu}]", "k"));

        assertFalse(Pattern.matches("[\\p{Lu}a-d]", "k"));
        assertTrue(Pattern.matches("[a-d\\p{Lu}]", "K"));

        // assertTrue(Pattern.matches("[\\p{L}&&[^\\p{Lu}&&[^K]]]", "K"));
        assertFalse(Pattern.matches("[\\p{L}&&[^\\p{Lu}&&[^G]]]", "K"));
    }

    @Test
    public void testSplitEmpty() {

        Pattern pat = Pattern.compile("");
        String[] s = pat.split("", -1);

        assertEquals(1, s.length);
        assertEquals("", s[0]);
    }

    @Test
    public void testFindDollar() {
        Matcher mat = Pattern.compile("a$").matcher("a\n");
        assertTrue(mat.find());
        assertEquals("a", mat.group());
    }

    @Test
    public void testMatchesRegionChanged() {
        // Regression for HARMONY-610
        String input = " word ";
        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher = pattern.matcher(input);
        matcher.region(1, 5);
        assertTrue(matcher.matches());
    }

    @Test
    public void testFindRegionChanged() {
        // Regression for HARMONY-625
        Pattern pattern = Pattern.compile("(?s).*");
        Matcher matcher = pattern.matcher("abcde");
        matcher.find();
        assertEquals("abcde", matcher.group());

        matcher = pattern.matcher("abcde");
        matcher.region(0, 2);
        matcher.find();
        assertEquals("ab", matcher.group());
    }

    @Test
    public void testFindRegionChanged2() {
        // Regression for HARMONY-713
        Pattern pattern = Pattern.compile("c");

        String inputStr = "aabb.c";
        Matcher matcher = pattern.matcher(inputStr);
        matcher.region(0, 3);

        assertFalse(matcher.find());
    }

    @Test
    public void testPatternMatcher() throws Exception {
        Pattern pattern = Pattern.compile("(?:\\d+)(?:pt)");
        assertTrue(pattern.matcher("14pt").matches());
    }

    @Test
    public void test3360() {
        String str = "!\"#%&'(),-./";
        Pattern p = Pattern.compile("\\s");
        Matcher m = p.matcher(str);

        assertFalse(m.find());
    }

    @Test
    public void testGeneralPunctuationCategory() {
        String[] s = { ",", "!", "\"", "#", "%", "&", "'", "(", ")", "-", ".", "/" };
        String regexp = "\\p{P}";

        for (int i = 0; i < s.length; i++) {
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(s[i]);
            assertTrue(matcher.find());
        }
    }

    @Test
    public void testHitEndAfterFind() {
        hitEndTest(true, "#01.0", "r((ege)|(geg))x", "regexx", false);
        hitEndTest(true, "#01.1", "r((ege)|(geg))x", "regex", false);
        hitEndTest(true, "#01.2", "r((ege)|(geg))x", "rege", true);
        hitEndTest(true, "#01.2", "r((ege)|(geg))x", "xregexx", false);

        hitEndTest(true, "#02.0", "regex", "rexreger", true);
        hitEndTest(true, "#02.1", "regex", "raxregexr", false);

        String floatRegex = getHexFloatRegex();
        hitEndTest(true, "#03.0", floatRegex, Double.toHexString(-1.234d), true);
        hitEndTest(true, "#03.1", floatRegex, "1 ABC" + Double.toHexString(Double.NaN) + "buhuhu", false);
        hitEndTest(true, "#03.2", floatRegex, Double.toHexString(-0.0) + "--", false);
        hitEndTest(true, "#03.3", floatRegex, "--" + Double.toHexString(Double.MIN_VALUE) + "--", false);

        hitEndTest(true, "#04.0", "(\\d+) fish (\\d+) fish (\\w+) fish (\\d+)",
                "1 fish 2 fish red fish 5", true);
        hitEndTest(true, "#04.1", "(\\d+) fish (\\d+) fish (\\w+) fish (\\d+)",
                "----1 fish 2 fish red fish 5----", false);
    }

    @Test
    public void testToString() {
        String result = Pattern.compile("(\\d{1,3})").matcher("aaaa123456789045").toString();
        assertTrue("The result doesn't contain pattern info", result.contains("(\\d{1,3})"));
    }

    @Test
    public void testSOLRegion() {
        Pattern pattern = Pattern.compile("^cd");
        String input = "abcde";
        Matcher matcher = pattern.matcher(input);
        matcher.region(2, input.length());
        assertTrue(matcher.lookingAt());
    }

    private void hitEndTest(boolean callFind, String testNo, String regex,
            String input, boolean hit) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (callFind) {
            matcher.find();
        } else {
            matcher.matches();
        }
        boolean h = matcher.hitEnd();

        assertTrue(testNo, h == hit);
    }

    private String getHexFloatRegex() {
        String hexDecimal = "(-|\\+)?0[xX][0-9a-fA-F]*\\.[0-9a-fA-F]+([pP](-|\\+)?[0-9]+)?";
        String notANumber = "((-|\\+)?Infinity)|([nN]a[nN])";
        return new StringBuilder("((").append(hexDecimal).append(")|(").append(
                notANumber).append("))").toString();
    }
}
