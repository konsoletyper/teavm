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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GlobMatchTest {
    @Test
    public void exact() {
        assertTrue(GlobMatch.match("foo", "foo"));
    }

    @Test
    public void exactNonMatch() {
        assertFalse(GlobMatch.match("foo", "bar"));
        assertFalse(GlobMatch.match("foo", "foobar"));
        assertFalse(GlobMatch.match("foobar", "foo"));
    }

    @Test
    public void twoStarsEnd() {
        assertTrue(GlobMatch.match("foo**", "foo123"));
        assertTrue(GlobMatch.match("foo**", "foo"));
        assertTrue(GlobMatch.match("foo**", "foofoo"));
        assertTrue(GlobMatch.match("foo**", "foo**"));
        assertFalse(GlobMatch.match("foo**", "bar"));
        assertFalse(GlobMatch.match("foo**", "fo"));
    }

    @Test
    public void singleStarAtEnd() {
        assertTrue(GlobMatch.match("foo.*", "foo.bar"));
        assertTrue(GlobMatch.match("foo.*", "foo."));
        assertFalse(GlobMatch.match("foo.*", "foo.bar.baz"));
        assertFalse(GlobMatch.match("foo.*", "bar.baz"));
    }

    @Test
    public void singleStarInMiddle() {
        assertTrue(GlobMatch.match("foo.*.bar", "foo.baz.bar"));
        assertTrue(GlobMatch.match("f*o.bar", "fXo.bar"));
        assertFalse(GlobMatch.match("foo.*.bar", "foo.baz.qux.bar"));
        assertFalse(GlobMatch.match("f*o.bar", "f.o.bar"));
    }

    @Test
    public void singleStarAlone() {
        assertTrue(GlobMatch.match("*", "foo"));
        assertFalse(GlobMatch.match("*", "foo.bar"));
    }

    @Test
    public void singleStarWithNoDotInName() {
        assertTrue(GlobMatch.match("foo*bar", "fooXXbar"));
        assertTrue(GlobMatch.match("foo*bar", "foobar"));
        assertFalse(GlobMatch.match("foo*bar", "foo.bar"));
    }

    @Test
    public void doubleStarAlone() {
        assertTrue(GlobMatch.match("**", "foo"));
        assertTrue(GlobMatch.match("**", "foo.bar"));
        assertTrue(GlobMatch.match("**", ""));
    }

    @Test
    public void doubleStarInMiddle() {
        assertTrue(GlobMatch.match("foo.**.bar", "foo.baz.bar"));
        assertTrue(GlobMatch.match("foo.**.bar", "foo.baz.qux.bar"));
        assertFalse(GlobMatch.match("foo.**.bar", "foo.baz"));
        assertFalse(GlobMatch.match("foo.**.bar", "bar.baz.bar"));
    }

    // * must skip early matches of the character that follows it in the pattern
    @Test
    public void singleStarBacktracking() {
        // first and second 'c' leave wrong suffix length; third 'c' works
        assertTrue(GlobMatch.match("a*cb", "acccb"));
        // first 'b' leaves too-short suffix; second 'b' aligns "bbc" correctly
        assertTrue(GlobMatch.match("a*bbc", "abbbc"));
        // all 'c' positions leave a trailing char that can't be consumed
        assertFalse(GlobMatch.match("a*cb", "acXb"));
    }

    // second * must backtrack when first match leaves wrong suffix length
    @Test
    public void twoSingleStarBacktracking() {
        // second * tries first 'c' (leftover "Xc" too long), then second 'c'
        assertTrue(GlobMatch.match("a*b*c", "aXbXcXc"));
        // first * matches empty; second * tries first 'c' (1 char short), then second 'c'
        assertTrue(GlobMatch.match("aa*bb*cc", "aabbbbcc"));
        // no 'c' exists in second segment at all
        assertFalse(GlobMatch.match("a*b*c", "aXbXd"));
    }

    // ** must try later dots when earlier dot leaves the wrong suffix
    @Test
    public void doubleStarBacktracking() {
        // first '.' gives suffix "bar.bar" (7 chars) != "bar" (3) → tries second '.'
        assertTrue(GlobMatch.match("**.bar", "foo.bar.bar"));
        // literal prefix consumed; ** tries first inner dot, then second
        assertTrue(GlobMatch.match("foo.**.bar", "foo.bar.bar.bar"));
        // no dot position leaves a ".bar" suffix
        assertFalse(GlobMatch.match("**.bar", "foo.baz.qux"));
    }
}
