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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class PatternTest {
    String[] testPatterns = {
            "(a|b)*abb",
            "(1*2*3*4*)*567",
            "(a|b|c|d)*aab",
            "(1|2|3|4|5|6|7|8|9|0)(1|2|3|4|5|6|7|8|9|0)*",
            "(abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ)*",
            "(a|b)*(a|b)*A(a|b)*lice.*",
            "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)(a|b|c|d|e|f|g|h|"
                    + "i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)*(1|2|3|4|5|6|7|8|9|0)*|while|for|struct|if|do",
            "x(?c)y", "x(?cc)y", "x(?:c)y"
    };

    @Test
    public void testCommentsInPattern() {
        Pattern p = Pattern.compile("ab# this is a comment\ncd", Pattern.COMMENTS);
        assertTrue(p.matcher("abcd").matches());
    }

    @Test
    public void testSplitCharSequenceint() {
        // splitting CharSequence which ends with pattern
        // bug6193
        assertEquals(",,".split(",", 3).length, 3);
        assertEquals(",,".split(",", 4).length, 3);
        // bug6193
        // bug5391
        assertEquals(Pattern.compile("o").split("boo:and:foo", 5).length, 5);
        assertEquals(Pattern.compile("b").split("ab", -1).length, 2);
        // bug5391
        String[] s;
        Pattern pat = Pattern.compile("x");
        s = pat.split("zxx:zzz:zxx", 10);
        assertEquals(s.length, 5);
        s = pat.split("zxx:zzz:zxx", 3);
        assertEquals(s.length, 3);
        s = pat.split("zxx:zzz:zxx", -1);
        assertEquals(s.length, 5);
        s = pat.split("zxx:zzz:zxx", 0);
        assertEquals(s.length, 3);
        // other splitting
        // negative limit
        pat = Pattern.compile("b");
        s = pat.split("abccbadfebb", -1);
        assertEquals(s.length, 5);
        s = pat.split("", -1);
        assertEquals(s.length, 1);
        pat = Pattern.compile("");
        s = pat.split("", -1);
        assertEquals(s.length, 1);
        s = pat.split("abccbadfe", -1);
        //assertEquals(s.length, 11);
        // zero limit
        pat = Pattern.compile("b");
        s = pat.split("abccbadfebb", 0);
        assertEquals(s.length, 3);
        s = pat.split("", 0);
        assertEquals(s.length, 1);
        pat = Pattern.compile("");
        s = pat.split("", 0);
        assertEquals(s.length, 1);
        //s = pat.split("abccbadfe", 0);
        //assertEquals(s.length, 10);
        // positive limit
        pat = Pattern.compile("b");
        s = pat.split("abccbadfebb", 12);
        assertEquals(s.length, 5);
        s = pat.split("", 6);
        assertEquals(s.length, 1);
        pat = Pattern.compile("");
        s = pat.split("", 11);
        assertEquals(s.length, 1);
        //s = pat.split("abccbadfe", 15);
        //assertEquals(s.length, 11);

        pat = Pattern.compile("b");
        s = pat.split("abccbadfebb", 5);
        assertEquals(s.length, 5);
        s = pat.split("", 1);
        assertEquals(s.length, 1);
        pat = Pattern.compile("");
        s = pat.split("", 1);
        assertEquals(s.length, 1);
        //s = pat.split("abccbadfe", 11);
        //assertEquals(s.length, 11);

        pat = Pattern.compile("b");
        s = pat.split("abccbadfebb", 3);
        assertEquals(s.length, 3);
        pat = Pattern.compile("");
        s = pat.split("abccbadfe", 5);
        assertEquals(s.length, 5);
    }

    @Test
    public void testSplitCharSequence() {
        String[] s;
        Pattern pat = Pattern.compile("b");
        s = pat.split("abccbadfebb");
        assertEquals(s.length, 3);
        s = pat.split("");
        assertEquals(s.length, 1);
        pat = Pattern.compile("");
        s = pat.split("");
        assertEquals(s.length, 1);
        s = pat.split("abccbadfe");
        //assertEquals(s.length, 10);
        // bug6544
        String s1 = "";
        String[] arr = s1.split(":");
        assertEquals(arr.length, 1);
        // bug6544
    }

    public void testPattern() {
    }

    @Test
    public void testFlags() {
        String baseString;
        String testString;
        Pattern pat;
        Matcher mat;

        baseString = "((?i)|b)a";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        baseString = "(?i)a|b";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)a|b";
        testString = "B";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "c|(?i)a|b";
        testString = "B";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)a|(?s)b";
        testString = "B";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)a|(?-i)b";
        testString = "B";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        baseString = "(?i)a|(?-i)c|b";
        testString = "B";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        baseString = "(?i)a|(?-i)c|(?i)b";
        testString = "B";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)a|(?-i)b";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "((?i))a";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        baseString = "|(?i)|a";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)((?s)a.)";
        testString = "A\n";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)((?-i)a)";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        baseString = "(?i)(?s:a.)";
        testString = "A\n";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)fgh(?s:aa)";
        testString = "fghAA";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?i)((?-i))a";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "abc(?i)d";
        testString = "ABCD";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        testString = "abcD";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "a(?i)a(?-i)a(?i)a(?-i)a";
        testString = "aAaAa";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "aAAAa";
        mat = pat.matcher(testString);
        assertFalse(mat.matches());
    }

    @Test
    public void testFlagsMethod() {
        String baseString;
        Pattern pat;

        /*
         * These tests are for compatibility with RI only. Logically we have to
         * return only flags specified during the compilation. For example
         * pat.flags() == 0 when we compile Pattern pat =
         * Pattern.compile("(?i)abc(?-i)"); but the whole expression is compiled
         * in a case insensitive manner. So there is little sense to do calls to
         * flags() now.
         */
        baseString = "(?-i)";
        pat = Pattern.compile(baseString);

        baseString = "(?idmsux)abc(?-i)vg(?-dmu)";
        pat = Pattern.compile(baseString);
        assertEquals(pat.flags(), Pattern.DOTALL | Pattern.COMMENTS);

        baseString = "(?idmsux)abc|(?-i)vg|(?-dmu)";
        pat = Pattern.compile(baseString);
        assertEquals(pat.flags(), Pattern.DOTALL | Pattern.COMMENTS);

        baseString = "(?is)a((?x)b.)";
        pat = Pattern.compile(baseString);
        assertEquals(pat.flags(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        baseString = "(?i)a((?-i))";
        pat = Pattern.compile(baseString);
        assertEquals(pat.flags(), Pattern.CASE_INSENSITIVE);

        baseString = "((?i)a)";
        pat = Pattern.compile(baseString);
        assertEquals(pat.flags(), 0);

        pat = Pattern.compile("(?is)abc");
        assertEquals(pat.flags(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    @Test
    public void testCompileStringint() {
        /*
         * this tests are needed to verify that appropriate exceptions are
         * thrown
         */
        String pattern = "b)a";
        try {
            Pattern.compile(pattern);
            fail("Expected a PatternSyntaxException when compiling pattern: "
                    + pattern);
        } catch (PatternSyntaxException e) {
            // pass
        }
        pattern = "bcde)a";
        try {
            Pattern.compile(pattern);
            fail("Expected a PatternSyntaxException when compiling pattern: "
                    + pattern);
        } catch (PatternSyntaxException e) {
            // pass
        }
        pattern = "bbg())a";
        try {
            Pattern.compile(pattern);
            fail("Expected a PatternSyntaxException when compiling pattern: "
                    + pattern);
        } catch (PatternSyntaxException e) {
            // pass
        }

        pattern = "cdb(?i))a";
        try {
            Pattern.compile(pattern);
            fail("Expected a PatternSyntaxException when compiling pattern: "
                    + pattern);
        } catch (PatternSyntaxException e) {
            // pass
        }

        /*
         * This pattern should compile - HARMONY-2127
         */
        pattern = "x(?c)y";
        Pattern.compile(pattern);

        /*
         * this pattern doesn't match any string, but should be compiled anyway
         */
        pattern = "(b\\1)a";
        Pattern.compile(pattern);
    }

    @Test
    public void testQuantCompileNeg() {
        String[] patterns = { "5{,2}", "{5asd", "{hgdhg", "{5,hjkh", "{,5hdsh",
                "{5,3shdfkjh}" };
        for (String element : patterns) {
            try {
                Pattern.compile(element);
                fail("PatternSyntaxException was expected, but compilation succeeds");
            } catch (PatternSyntaxException pse) {
                continue;
            }
        }
        // Regression for HARMONY-1365
        String pattern =
                "(?![^\\<C\\f\\0146\\0270\\}&&[|\\02-\\x3E\\}|X-\\|]]{7,}+)[|\\\\\\x98\\<\\?\\u4FCFr\\,"
                        + "\\0025\\}\\004|\\0025-\\052\061]|(?<![|\\01-\\u829E])|(?<!\\p{Alpha})|^|"
                        + "(?-s:[^\\x15\\\\\\x24F\\a\\,\\a\\u97D8[\\x38\\a[\\0224-\\0306[^\\0020-\\u6A57]]]]??)"
                        + "(?uxix:[^|\\{\\[\\0367\\t\\e\\x8C\\{\\[\\074c\\]V[|b\\fu"
                        + "\\r\\0175\\<\\07f\\066s[^D-\\x5D]]])(?xx:^{5,}+)(?uuu)(?=^\\D)|(?!\\G)(?>\\G*?)"
                        + "(?![^|\\]\\070\\ne\\{\\t\\[\\053\\?\\\\\\x51\\a\\075\\0023-\\[&&[|\\022-\\xEA\\00-\\u41C2"
                        + "&&[^|a-\\xCC&&[^\\037\\uECB3\\u3D9A\\x31\\|\\<b\\0206\\uF2EC\\01m\\,"
                        + "\\ak\\a\\03&&\\p{Punct}]]]])"
                        + "(?-dxs:[|\\06-\\07|\\e-\\x63&&[|Tp\\u18A3\\00\\|\\xE4\\05\\061\\015\\0116C|"
                        + "\\r\\{\\}\\006\\xEA\\0367\\xC4\\01\\0042\\0267\\xBB\\01T\\}\\0100\\?[|\\[-\\u459B|"
                        + "\\x23\\x91\\rF\\0376[|\\?-\\x94\\0113-\\\\\\s]]]]{6}?)"
                        + "(?<=[^\\t-\\x42H\\04\\f\\03\\0172\\?i\\u97B6\\e\\f\\uDAC2])(?=\\B*+)(?>[^\\016"
                        + "\\r\\{\\,\\uA29D\\034\\02[\\02-\\[|\\t\\056\\uF599\\x62\\e\\<\\032\\uF0AC\\0026"
                        + "\\0205Q\\|\\\\\\06\\0164[|\\057-\\u7A98&&[\\061-g|\\|\\0276\\n\\042\\011\\e"
                        + "\\xE8\\x64B\\04\\u6D0EDW^\\p{Lower}]]]]?)(?<=[^\\n\\\\\\t\\u8E13\\,"
                        + "\\0114\\u656E\\xA5\\]&&[\\03-\\026|\\uF39D\\01\\{i\\u3BC2\\u14FE]])"
                        + "(?<=[^|\\uAE62\\054H\\|\\}&&^\\p{Space}])(?sxx)(?<=[\\f\\006\\a\\r\\xB4]*+)|"
                        + "(?x-xd:^{5}+)()";
        assertNotNull(Pattern.compile(pattern));
    }

    @Test
    public void testQuantCompilePos() {
        String[] patterns = { "abc{2,}", "abc{5}" };
        for (String element : patterns) {
            Pattern.compile(element);
        }
    }

    @Test
    public void testQuantComposition() {
        String pattern = "(a{1,3})aab";
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher mat = pat.matcher("aaab");
        mat.matches();
        mat.start(1);
        mat.group(1);
    }

    @Test
    public void testMatches() {
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
                { "a123", "bnxnvgds156", "for", "while", "if", "struct" },
                { "xy" }, { "xy" }, { "xcy" }
        };

        for (int i = 0; i < testPatterns.length; i++) {
            for (int j = 0; j < posSeq[i].length; j++) {
                assertTrue("Incorrect match: " + testPatterns[i] + " vs "
                        + posSeq[i][j], Pattern.matches(testPatterns[i],
                        posSeq[i][j]));
            }
        }
    }

    @Test
    public void testTimeZoneIssue() {
        Pattern p = Pattern.compile("GMT(\\+|\\-)(\\d+)(:(\\d+))?");
        Matcher m = p.matcher("GMT-9:45");
        assertTrue(m.matches());
        assertEquals("-", m.group(1));
        assertEquals("9", m.group(2));
        assertEquals(":45", m.group(3));
        assertEquals("45", m.group(4));
    }

    @Test
    public void testCompileRanges() {
        String[] correctTestPatterns = { "[^]*abb]*",
                "[a-d\\d]*abb", "[a-z&&[^m-p]]*abb", "[a-d[m-p]]*abb",
                "[a-zA-Z]*abb", "[+*?]*abb", "[^+*?]*abb" };

        String[] inputSecuence = { "kkkk", "abcabcd124654abb",
                "acbvfgtyabb", "adbcacdbmopabcoabb",
                "jhfkjhaSDFGHJkdfhHNJMjkhfabb", "+*??+*abb", "sdfghjkabb" };

        for (int i = 0; i < correctTestPatterns.length; i++) {
            assertTrue("pattern: " + correctTestPatterns[i] + " input: "
                    + inputSecuence[i], Pattern.matches(correctTestPatterns[i],
                    inputSecuence[i]));
        }

        String[] wrongInputSecuence = { "]", "abcabcd124k654abb",
                "acbotyabb", "adbcaecdbmopabcoabb",
                "jhfkjhaSDFGHJk;dfhHNJMjkhfabb", "+*?a?+*abb", "sdf+ghjkabb" };

        for (int i = 0; i < correctTestPatterns.length; i++) {
            assertFalse("pattern: " + correctTestPatterns[i] + " input: "
                    + wrongInputSecuence[i], Pattern.matches(
                    correctTestPatterns[i], wrongInputSecuence[i]));
        }
    }

    @Test
    public void testRangesSpecialCases() {
        String[] negPatterns = { "[a-&&[b-c]]", "[a-\\w]", "[b-a]", "[]" };

        for (String element : negPatterns) {
            try {
                Pattern.compile(element);
                fail("PatternSyntaxException was expected: " + element);
            } catch (PatternSyntaxException pse) {
                // ok
            }
        }

        String[] posPatterns = { "[-]+", "----", "[a-]+", "a-a-a-a-aa--",
                "[\\w-a]+", "123-2312--aaa-213", "[a-]]+", "-]]]]]]]]]]]]]]]" };

        for (int i = 0; i < posPatterns.length; i++) {
            String pat = posPatterns[i++];
            String inp = posPatterns[i];
            assertTrue("pattern: " + pat + " input: " + inp, Pattern.matches(
                    pat, inp));
        }
    }

    @Test
    public void testZeroSymbols() {
        assertTrue(Pattern.matches("[\0]*abb", "\0\0\0\0\0\0abb"));
    }

    @Test
    public void testEscapes() {
        Pattern pat = Pattern.compile("\\Q{]()*?");
        Matcher mat = pat.matcher("{]()*?");

        assertTrue(mat.matches());
    }

    @Test
    public void testRegressions() {
        // Bug 181
        Pattern.compile("[\\t-\\r]");

        // HARMONY-4472
        Pattern.compile("a*.+");

        // Bug187
        Pattern
                .compile("|(?idmsux-idmsux)|(?idmsux-idmsux)|[^|\\[-\\0274|\\,"
                        + "-\\\\[^|W\\}\\nq\\x65\\002\\xFE\\05\\06\\00\\x66\\x47i\\,"
                        + "\\xF2\\=\\06\\u0EA4\\x9B\\x3C\\f\\|\\{\\xE5\\05\\r\\u944A\\xCA\\e|\\x19\\04\\x07\\04\\u607B"
                        + "\\023\\0073\\x91Tr\\0150\\x83]]?(?idmsux-idmsux:\\p{Alpha}{7}?)||"
                        + "(?<=[^\\uEC47\\01\\02\\u3421\\a\\f\\a\\013q\\035w\\e])(?<=\\p{Punct}{0,}?)(?=^\\p{Lower})"
                        + "(?!\\b{8,14})(?<![|\\00-\\0146[^|\\04\\01\\04\\060\\f\\u224DO\\x1A\\xC4\\00\\02\\0315\\0351"
                        + "\\u84A8\\xCBt\\xCC\\06|\\0141\\00\\=\\e\\f\\x6B\\0026Tb\\040\\x76xJ&&[\\\\-\\]\\05\\07\\02"
                        + "\\u2DAF\\t\\x9C\\e\\0023\\02\\,X\\e|\\u6058flY\\u954C]]]{5}?)(?<=\\p{Sc}{8}+)"
                        + "[^|\\026-\\u89BA|o\\u6277\\t\\07\\x50&&\\p{Punct}]{8,14}+((?<=^\\p{Punct})|"
                        + "(?idmsux-idmsux)||(?>[\\x3E-\\]])|(?idmsux-idmsux:\\p{Punct})|"
                        + "(?<![\\0111\\0371\\xDF\\u6A49\\07\\u2A4D\\00\\0212\\02Xd-\\xED[^\\a-\\0061|\\0257\\04\\f"
                        + "\\[\\0266\\043\\03\\x2D\\042&&[^\\f-\\]&&\\s]]])|"
                        + "(?>[|\\n\\042\\uB09F\\06\\u0F2B\\uC96D\\x89\\uC166\\xAA|\\04-\\][^|\\a\\|\\rx\\04\\uA770\\n"
                        + "\\02\\t\\052\\056\\0274\\|\\=\\07\\e|\\00-\\x1D&&[^\\005\\uB15B\\uCDAC\\n\\x74\\0103\\0147"
                        + "\\uD91B\\n\\062G\\u9B4B\\077\\}\\0324&&[^\\0302\\,"
                        + "\\0221\\04\\u6D16\\04xy\\uD193\\[\\061\\06\\045\\x0F|\\e\\xBB\\f\\u1B52\\023\\u3AD2\\033"
                        + "\\007\\022\\}\\x66\\uA63FJ-\\0304]]]]{0,0})||"
                        + "(?<![^|\\0154U\\u0877\\03\\fy\\n\\|\\0147\\07-\\=[|q\\u69BE\\0243\\rp\\053\\02\\x33I\\u5E39"
                        + "\\u9C40\\052-\\xBC[|\\0064-\\?|\\uFC0C\\x30\\0060\\x45\\\\\\02\\?p\\xD8\\0155\\07\\0367\\04"
                        + "\\uF07B\\000J[^|\\0051-\\{|\\u9E4E\\u7328\\]\\u6AB8\\06\\x71\\a\\]\\e\\|KN\\u06AA\\0000\\063"
                        + "\\u2523&&[\\005\\0277\\x41U\\034\\}R\\u14C7\\u4767\\x09\\n\\054Ev\\0144\\<\\f\\,"
                        + "Q-\\xE4]]]]]{3}+)|(?>^+)|"
                        + "(?![^|\\|\\nJ\\t\\<\\04E\\\\\\t\\01\\\\\\02\\|\\=\\}\\xF3\\uBEC2\\032K\\014\\uCC5F\\072q"
                        + "\\|\\0153\\xD9\\0322\\uC6C8[^\\t\\0342\\x34\\x91\\06\\{\\xF1\\a\\u1710\\?\\xE7\\uC106\\02pF"
                        + "\\<&&[^|\\]\\064\\u381D\\u50CF\\eO&&[^|\\06\\x2F\\04\\045\\032\\u8536W\\0377\\0017|\\x06"
                        + "\\uE5FA\\05\\xD4\\020\\04c\\xFC\\02H\\x0A\\r]]]]+?)(?idmsux-idmsux)|(?<![|\\r-\\,"
                        + "&&[I\\t\\r\\0201\\xDB\\e&&[^|\\02\\06\\00\\<\\a\\u7952\\064\\051\\073\\x41\\?n\\040\\"
                        + "0053\\031&&[\\x15-\\|]]]]{8,11}?)"
                        + "(?![^|\\<-\\uA74B\\xFA\\u7CD2\\024\\07n\\<\\x6A\\0042\\uE4FF\\r\\u896B\\[\\=\\042Y"
                        + "&&^\\p{ASCII}]++)|(?<![R-\\|&&[\\a\\0120A\\u6145\\<\\050-d[|\\e-\\uA07C|\\016-\\u80D9]]]"
                        + "{1,}+)|(?idmsux-idmsux)|(?idmsux-idmsux)|(?idmsux-idmsux:\\B{6,}?)|(?<=\\D{5,8}?)|"
                        + "(?>[\\{-\\0207|\\06-\\0276\\p{XDigit}])"
                        + "(?idmsux-idmsux:[^|\\x52\\0012\\]u\\xAD\\0051f\\0142\\\\l\\|\\050\\05\\f\\t\\u7B91"
                        + "\\r\\u7763\\{|h\\0104\\a\\f\\0234\\u2D4F&&^\\P{InGreek}]))");
        // HARMONY-5858
        Pattern.compile("\\u6211", Pattern.LITERAL);
    }

    @Test
    public void testOrphanQuantifiers() {
        try {
            Pattern.compile("+++++");
            fail("PatternSyntaxException expected");
        } catch (PatternSyntaxException pse) {
            // ok
        }
    }

    @Test
    public void testOrphanQuantifiers2() {
        try {
            Pattern.compile("\\d+*");
            fail("PatternSyntaxException expected");
        } catch (PatternSyntaxException pse) {
            // ok
        }
    }

    @Test
    public void testBug197() {
        Object[] vals = { ":", new Integer(2),
                new String[] { "boo", "and:foo" }, ":", new Integer(5),
                new String[] { "boo", "and", "foo" }, ":", new Integer(-2),
                new String[] { "boo", "and", "foo" }, ":", new Integer(3),
                new String[] { "boo", "and", "foo" }, ":", new Integer(1),
                new String[] { "boo:and:foo" }, "o", new Integer(5),
                new String[] { "b", "", ":and:f", "", "" }, "o",
                new Integer(4), new String[] { "b", "", ":and:f", "o" }, "o",
                new Integer(-2), new String[] { "b", "", ":and:f", "", "" },
                "o", new Integer(0), new String[] { "b", "", ":and:f" } };

        for (int i = 0; i < vals.length / 3;) {
            String[] res = Pattern.compile(vals[i++].toString()).split("boo:and:foo", (Integer) vals[i++]);
            String[] expectedRes = (String[]) vals[i++];

            assertEquals(expectedRes.length, res.length);

            for (int j = 0; j < expectedRes.length; j++) {
                assertEquals(expectedRes[j], res[j]);
            }
        }
    }

    @Test
    public void testURIPatterns() {
        String uriRegexpStr = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
        String schemeRegexpStr = "^[a-zA-Z]{1}[\\w+-.]+$";
        String relUriRegexpStr = "^(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
        String ipv6RegexpStr = "^[0-9a-fA-F\\:\\.]+(\\%\\w+)?$";
        String ipv6RegexpStr2 = "^\\[[0-9a-fA-F\\:\\.]+(\\%\\w+)?\\]$";
        String ipv4RegexpStr = "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$";
        String hostnameRegexpStr = "\\w+[\\w\\-\\.]*";

        Pattern.compile(uriRegexpStr);
        Pattern.compile(relUriRegexpStr);
        Pattern.compile(schemeRegexpStr);
        Pattern.compile(ipv4RegexpStr);
        Pattern.compile(ipv6RegexpStr);
        Pattern.compile(ipv6RegexpStr2);
        Pattern.compile(hostnameRegexpStr);
    }

    @Test
    public void testFindBoundaryCases1() {
        Pattern pat = Pattern.compile(".*\n");
        Matcher mat = pat.matcher("a\n");

        mat.find();
        assertEquals("a\n", mat.group());
    }

    @Test
    public void testFindBoundaryCases2() {
        Pattern pat = Pattern.compile(".*A");
        Matcher mat = pat.matcher("aAa");

        mat.find();
        assertEquals("aA", mat.group());
    }

    @Test
    public void testFindBoundaryCases3() {
        Pattern pat = Pattern.compile(".*A");
        Matcher mat = pat.matcher("a\naA\n");

        mat.find();
        assertEquals("aA", mat.group());
    }

    @Test
    public void testFindBoundaryCases4() {
        Pattern pat = Pattern.compile("A.*");
        Matcher mat = pat.matcher("A\n");

        mat.find();
        assertEquals("A", mat.group());
    }

    @Test
    public void testFindBoundaryCases5() {
        Pattern pat = Pattern.compile(".*A.*");
        Matcher mat = pat.matcher("\nA\naaa\nA\naaAaa\naaaA\n");
        // Matcher mat = pat.matcher("\nA\n");
        String[] res = { "A", "A", "aaAaa", "aaaA" };
        int k = 0;
        for (; mat.find(); k++) {
            assertEquals(res[k], mat.group());
        }
    }

    @Test
    public void testFindBoundaryCases6() {
        String[] res = { "", "a", "", "" };
        Pattern pat = Pattern.compile(".*");
        Matcher mat = pat.matcher("\na\n");
        int k = 0;

        for (; mat.find(); k++) {
            assertEquals(res[k], mat.group());
        }
    }

    @Test
    public void testBackReferences() {
        Pattern pat = Pattern.compile("(\\((\\w*):(.*):(\\2)\\))");
        Matcher mat = pat
                .matcher("(start1: word :start1)(start2: word :start2)");
        int k = 1;
        for (; mat.find(); k++) {
            assertEquals("start" + k, mat.group(2));
            assertEquals(" word ", mat.group(3));
            assertEquals("start" + k, mat.group(4));
        }

        assertEquals(3, k);
        pat = Pattern.compile(".*(.)\\1");
        mat = pat.matcher("saa");
        assertTrue(mat.matches());
    }

    @Test
    public void testNewLine() {
        Pattern pat = Pattern.compile("(^$)*\n", Pattern.MULTILINE);
        Matcher mat = pat.matcher("\r\n\n");
        int counter = 0;
        while (mat.find()) {
            counter++;
        }
        assertEquals(2, counter);
    }

    @Test
    public void testFindGreedy() {
        Pattern pat = Pattern.compile(".*aaa", Pattern.DOTALL);
        Matcher mat = pat.matcher("aaaa\naaa\naaaaaa");
        mat.matches();
        assertEquals(15, mat.end());
    }

    @Test
    public void testSOLQuant() {
        Pattern pat = Pattern.compile("$*", Pattern.MULTILINE);
        Matcher mat = pat.matcher("\n\n");
        int counter = 0;
        while (mat.find()) {
            counter++;
        }

        assertEquals(3, counter);
    }

    @Test
    public void testIllegalEscape() {
        try {
            Pattern.compile("\\y");
            fail("PatternSyntaxException expected");
        } catch (PatternSyntaxException pse) {
            // ok
        }
    }

    @Test
    public void testEmptyFamily() {
        Pattern.compile("\\p{Lower}");
    }

    @Test
    public void testNonCaptConstr() {
        // Flags
        Pattern pat = Pattern.compile("(?i)b*(?-i)a*");
        assertTrue(pat.matcher("bBbBaaaa").matches());
        assertFalse(pat.matcher("bBbBAaAa").matches());

        // Non-capturing groups
        pat = Pattern.compile("(?i:b*)a*");
        assertTrue(pat.matcher("bBbBaaaa").matches());
        assertFalse(pat.matcher("bBbBAaAa").matches());

        pat = Pattern
                // 1 2 3 4 5 6 7 8 9 10 11
                .compile("(?:-|(-?\\d+\\d\\d\\d))?(?:-|-(\\d\\d))?(?:-|-(\\d\\d))?(T)?(?:(\\d\\d):(\\d\\d)"
                        + ":(\\d\\d)(\\.\\d+)?)?(?:(?:((?:\\+|\\-)\\d\\d):(\\d\\d))|(Z))?");
        Matcher mat = pat.matcher("-1234-21-31T41:51:61.789+71:81");
        assertTrue(mat.matches());
        assertEquals("-1234", mat.group(1));
        assertEquals("21", mat.group(2));
        assertEquals("31", mat.group(3));
        assertEquals("T", mat.group(4));
        assertEquals("41", mat.group(5));
        assertEquals("51", mat.group(6));
        assertEquals("61", mat.group(7));
        assertEquals(".789", mat.group(8));
        assertEquals("+71", mat.group(9));
        assertEquals("81", mat.group(10));

        // positive lookahead
        pat = Pattern.compile(".*\\.(?=log$).*$");
        assertTrue(pat.matcher("a.b.c.log").matches());
        assertFalse(pat.matcher("a.b.c.log.").matches());

        // negative lookahead
        pat = Pattern.compile(".*\\.(?!log$).*$");
        assertFalse(pat.matcher("abc.log").matches());
        assertTrue(pat.matcher("abc.logg").matches());

        // positive lookbehind
        pat = Pattern.compile(".*(?<=abc)\\.log$");
        assertFalse(pat.matcher("cde.log").matches());
        assertTrue(pat.matcher("abc.log").matches());

        // negative lookbehind
        pat = Pattern.compile(".*(?<!abc)\\.log$");
        assertTrue(pat.matcher("cde.log").matches());
        assertFalse(pat.matcher("abc.log").matches());

        // atomic group
        pat = Pattern.compile("(?>a*)abb");
        assertFalse(pat.matcher("aaabb").matches());
        pat = Pattern.compile("(?>a*)bb");
        assertTrue(pat.matcher("aaabb").matches());

        pat = Pattern.compile("(?>a|aa)aabb");
        assertTrue(pat.matcher("aaabb").matches());
        pat = Pattern.compile("(?>aa|a)aabb");
        assertFalse(pat.matcher("aaabb").matches());

        // quantifiers over look ahead
        pat = Pattern.compile(".*(?<=abc)*\\.log$");
        assertTrue(pat.matcher("cde.log").matches());
        pat = Pattern.compile(".*(?<=abc)+\\.log$");
        assertFalse(pat.matcher("cde.log").matches());
    }

    @Test
    public void testCompilePatternWithTerminatorMark() {
        Pattern pat = Pattern.compile("a\u0000\u0000cd");
        Matcher mat = pat.matcher("a\u0000\u0000cd");
        assertTrue(mat.matches());
    }

    @Test
    public void testAlternations() {
        String baseString = "|a|bc";
        Pattern pat = Pattern.compile(baseString);
        Matcher mat = pat.matcher("");

        assertTrue(mat.matches());

        baseString = "a||bc";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("");
        assertTrue(mat.matches());

        baseString = "a|bc|";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("");
        assertTrue(mat.matches());

        baseString = "a|b|";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("");
        assertTrue(mat.matches());

        baseString = "a(|b|cd)e";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("ae");
        assertTrue(mat.matches());

        baseString = "a(b||cd)e";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("ae");
        assertTrue(mat.matches());

        baseString = "a(b|cd|)e";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("ae");
        assertTrue(mat.matches());

        baseString = "a(b|c|)e";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("ae");
        assertTrue(mat.matches());

        baseString = "a(|)e";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("ae");
        assertTrue(mat.matches());

        baseString = "|";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("");
        assertTrue(mat.matches());

        baseString = "a(?:|)e";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("ae");
        assertTrue(mat.matches());

        baseString = "a||||bc";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("");
        assertTrue(mat.matches());

        baseString = "(?i-is)|a";
        pat = Pattern.compile(baseString);
        mat = pat.matcher("a");
        assertTrue(mat.matches());
    }

    @Test
    public void testMatchWithGroups() {
        String baseString = "jwkerhjwehrkwjehrkwjhrwkjehrjwkehrjkwhrkwehrkwhrkwrhwkhrwkjehr";
        String pattern = ".*(..).*\\1.*";
        assertTrue(Pattern.compile(pattern).matcher(baseString).matches());

        baseString = "saa";
        pattern = ".*(.)\\1";
        assertTrue(Pattern.compile(pattern).matcher(baseString).matches());
        assertTrue(Pattern.compile(pattern).matcher(baseString).find());
    }

    @Test
    public void testSplitEmptyCharSequence() {
        String s1 = "";
        String[] arr = s1.split(":");
        assertEquals(arr.length, 1);
    }

    @Test
    public void testSplitEndsWithPattern() {
        assertEquals(",,".split(",", 3).length, 3);
        assertEquals(",,".split(",", 4).length, 3);

        assertEquals(Pattern.compile("o").split("boo:and:foo", 5).length, 5);
        assertEquals(Pattern.compile("b").split("ab", -1).length, 2);
    }

    @Test
    public void testCaseInsensitiveFlag() {
        assertTrue(Pattern.matches("(?i-:AbC)", "ABC"));
    }

    @Test
    public void testEmptyGroups() {
        Pattern pat = Pattern.compile("ab(?>)cda");
        Matcher mat = pat.matcher("abcda");
        assertTrue(mat.matches());

        pat = Pattern.compile("ab()");
        mat = pat.matcher("ab");
        assertTrue(mat.matches());

        pat = Pattern.compile("abc(?:)(..)");
        mat = pat.matcher("abcgf");
        assertTrue(mat.matches());
    }

    @Test
    public void testCompileNonCaptGroup() {
        boolean isCompiled = false;

        try {
            Pattern.compile("(?:)", Pattern.CANON_EQ);
            Pattern.compile("(?:)", Pattern.CANON_EQ | Pattern.DOTALL);
            Pattern
                    .compile("(?:)", Pattern.CANON_EQ
                            | Pattern.CASE_INSENSITIVE);
            Pattern.compile("(?:)", Pattern.CANON_EQ | Pattern.COMMENTS
                    | Pattern.UNIX_LINES);
            isCompiled = true;
        } catch (PatternSyntaxException e) {
            System.out.println(e);
        }
        assertTrue(isCompiled);
    }

    @Test
    public void testEmbeddedFlags() {
        String baseString = "(?i)((?s)a)";
        String testString = "A";
        Pattern pat = Pattern.compile(baseString);
        Matcher mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?x)(?i)(?s)(?d)a";
        testString = "A";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "(?x)(?i)(?s)(?d)a.";
        testString = "a\n";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "abc(?x:(?i)(?s)(?d)a.)";
        testString = "abcA\n";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        baseString = "abc((?x)d)(?i)(?s)a";
        testString = "abcdA";
        pat = Pattern.compile(baseString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());
    }

    @Test
    public void testAltWithFlags() {
        Pattern.compile("|(?i-xi)|()");
    }

    @Test
    public void testRestoreFlagsAfterGroup() {
        String baseString = "abc((?x)d)   a";
        String testString = "abcd   a";
        Pattern pat = Pattern.compile(baseString);
        Matcher mat = pat.matcher(testString);

        assertTrue(mat.matches());
    }

    @Test
    public void testCompileCharacterClass() {
        // Regression for HARMONY-606, 696
        Pattern pattern = Pattern.compile("\\p{javaLowerCase}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaUpperCase}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaWhitespace}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaMirrored}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaDefined}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaDigit}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaIdentifierIgnorable}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaISOControl}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaJavaIdentifierPart}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaJavaIdentifierStart}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaLetter}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaLetterOrDigit}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaSpaceChar}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaTitleCase}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaUnicodeIdentifierPart}");
        assertNotNull(pattern);

        pattern = Pattern.compile("\\p{javaUnicodeIdentifierStart}");
        assertNotNull(pattern);
    }

    @Test
    public void testRangesWithSurrogatesSupplementary() {
        String patString = "[abc\uD8D2]";
        String testString = "\uD8D2";
        Pattern pat = Pattern.compile(patString);
        Matcher mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "a";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "ef\uD8D2\uDD71gh";
        mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "ef\uD8D2gh";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        patString = "[abc\uD8D3&&[c\uD8D3]]";
        testString = "c";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "a";
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        testString = "ef\uD8D3\uDD71gh";
        mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "ef\uD8D3gh";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        patString = "[abc\uD8D3\uDBEE\uDF0C&&[c\uD8D3\uDBEE\uDF0C]]";
        testString = "c";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "\uDBEE\uDF0C";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "ef\uD8D3\uDD71gh";
        mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "ef\uD8D3gh";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        patString = "[abc\uDBFC]\uDDC2cd";
        testString = "\uDBFC\uDDC2cd";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        testString = "a\uDDC2cd";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());
    }

    @Test
    public void testSequencesWithSurrogatesSupplementary() {
        String patString = "abcd\uD8D3";
        String testString = "abcd\uD8D3\uDFFC";
        Pattern pat = Pattern.compile(patString);
        Matcher mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "abcd\uD8D3abc";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        patString = "ab\uDBEFcd";
        testString = "ab\uDBEFcd";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        patString = "\uDFFCabcd";
        testString = "\uD8D3\uDFFCabcd";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "abc\uDFFCabcdecd";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        patString = "\uD8D3\uDFFCabcd";
        testString = "abc\uD8D3\uD8D3\uDFFCabcd";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.find());
    }

    @Test
    public void testPredefinedClassesWithSurrogatesSupplementary() {
        String patString = "[123\\D]";
        String testString = "a";
        Pattern pat = Pattern.compile(patString);
        Matcher mat = pat.matcher(testString);
        assertTrue(mat.find());

        testString = "5";
        mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "3";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        // low surrogate
        testString = "\uDFC4";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        // high surrogate
        testString = "\uDADA";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        testString = "\uDADA\uDFC4";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        patString = "[123[^\\p{javaDigit}]]";
        testString = "a";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        testString = "5";
        mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "3";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        // low surrogate
        testString = "\uDFC4";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        // high surrogate
        testString = "\uDADA";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        testString = "\uDADA\uDFC4";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        // surrogate characters
        patString = "\\p{Cs}";
        testString = "\uD916\uDE27";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);

        // swap low and high surrogates
        testString = "\uDE27\uD916";
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        patString = "[\uD916\uDE271\uD91623&&[^\\p{Cs}]]";
        testString = "1";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        testString = "\uD916";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertFalse(mat.find());

        testString = "\uD916\uDE27";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.find());

        // \uD9A0\uDE8E=\u7828E
        // \u78281=\uD9A0\uDE81
        patString = "[a-\uD9A0\uDE8E]";
        testString = "\uD9A0\uDE81";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());
    }

    @Test
    public void testDotConstructionWithSurrogatesSupplementary() {
        String patString = ".";
        String testString = "\uD9A0\uDE81";
        Pattern pat = Pattern.compile(patString);
        Matcher mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "\uDE81";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "\uD9A0";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "\n";
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        patString = ".*\uDE81";
        testString = "\uD9A0\uDE81\uD9A0\uDE81\uD9A0\uDE81";
        pat = Pattern.compile(patString);
        mat = pat.matcher(testString);
        assertFalse(mat.matches());

        testString = "\uD9A0\uDE81\uD9A0\uDE81\uDE81";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        patString = ".*";
        testString = "\uD9A0\uDE81\n\uD9A0\uDE81\uD9A0\n\uDE81";
        pat = Pattern.compile(patString, Pattern.DOTALL);
        mat = pat.matcher(testString);
        assertTrue(mat.matches());
    }

    @Test
    public void testQuantifiersWithSurrogatesSupplementary() {
        String patString = "\uD9A0\uDE81*abc";
        String testString = "\uD9A0\uDE81\uD9A0\uDE81abc";
        Pattern pat = Pattern.compile(patString);
        Matcher mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "abc";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());
    }

    @Test
    public void testAlternationsWithSurrogatesSupplementary() {
        String patString = "\uDE81|\uD9A0\uDE81|\uD9A0";
        String testString = "\uD9A0";
        Pattern pat = Pattern.compile(patString);
        Matcher mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "\uDE81";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "\uD9A0\uDE81";
        mat = pat.matcher(testString);
        assertTrue(mat.matches());

        testString = "\uDE81\uD9A0";
        mat = pat.matcher(testString);
        assertFalse(mat.matches());
    }

    @Test
    public void testGroupsWithSurrogatesSupplementary() {

        // this pattern matches nothing
        String patString = "(\uD9A0)\uDE81";
        String testString = "\uD9A0\uDE81";
        Pattern pat = Pattern.compile(patString);
        Matcher mat = pat.matcher(testString);
        assertFalse(mat.matches());

        patString = "(\uD9A0)";
        testString = "\uD9A0\uDE81";
        pat = Pattern.compile(patString, Pattern.DOTALL);
        mat = pat.matcher(testString);
        assertFalse(mat.find());
    }

    @Test
    public void testUnicodeCategoryWithSurrogatesSupplementary() {
        Pattern p = Pattern.compile("\\p{javaLowerCase}");
        Matcher matcher = p.matcher("\uD801\uDC28");
        assertTrue(matcher.find());
    }
}
