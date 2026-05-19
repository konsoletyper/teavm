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
package org.teavm.extension.spi.reflection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GlobMatchTest {
    @Test
    public void exact() {
        assertTrue(SimpleReflectionPolicy.globMatch("foo", "foo"));
    }

    @Test
    public void exactNonMatch() {
        assertFalse(SimpleReflectionPolicy.globMatch("foo", "bar"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo", "foobar"));
        assertFalse(SimpleReflectionPolicy.globMatch("foobar", "foo"));
    }

    @Test
    public void twoStarsEnd() {
        assertTrue(SimpleReflectionPolicy.globMatch("foo**", "foo123"));
        assertTrue(SimpleReflectionPolicy.globMatch("foo**", "foo"));
        assertTrue(SimpleReflectionPolicy.globMatch("foo**", "foofoo"));
        assertTrue(SimpleReflectionPolicy.globMatch("foo**", "foo**"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo**", "bar"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo**", "fo"));
    }

    @Test
    public void singleStarAtEnd() {
        assertTrue(SimpleReflectionPolicy.globMatch("foo.*", "foo.bar"));
        assertTrue(SimpleReflectionPolicy.globMatch("foo.*", "foo."));
        assertFalse(SimpleReflectionPolicy.globMatch("foo.*", "foo.bar.baz"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo.*", "bar.baz"));
    }

    @Test
    public void singleStarInMiddle() {
        assertTrue(SimpleReflectionPolicy.globMatch("foo.*.bar", "foo.baz.bar"));
        assertTrue(SimpleReflectionPolicy.globMatch("f*o.bar", "fXo.bar"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo.*.bar", "foo.baz.qux.bar"));
        assertFalse(SimpleReflectionPolicy.globMatch("f*o.bar", "f.o.bar"));
    }

    @Test
    public void singleStarAlone() {
        assertTrue(SimpleReflectionPolicy.globMatch("*", "foo"));
        assertFalse(SimpleReflectionPolicy.globMatch("*", "foo.bar"));
    }

    @Test
    public void singleStarWithNoDotInName() {
        assertTrue(SimpleReflectionPolicy.globMatch("foo*bar", "fooXXbar"));
        assertTrue(SimpleReflectionPolicy.globMatch("foo*bar", "foobar"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo*bar", "foo.bar"));
    }

    @Test
    public void doubleStarAlone() {
        assertTrue(SimpleReflectionPolicy.globMatch("**", "foo"));
        assertTrue(SimpleReflectionPolicy.globMatch("**", "foo.bar"));
        assertTrue(SimpleReflectionPolicy.globMatch("**", ""));
    }

    @Test
    public void doubleStarInMiddle() {
        assertTrue(SimpleReflectionPolicy.globMatch("foo.**.bar", "foo.baz.bar"));
        assertTrue(SimpleReflectionPolicy.globMatch("foo.**.bar", "foo.baz.qux.bar"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo.**.bar", "foo.baz"));
        assertFalse(SimpleReflectionPolicy.globMatch("foo.**.bar", "bar.baz.bar"));
    }

    // * must skip early matches of the character that follows it in the pattern
    @Test
    public void singleStarBacktracking() {
        // first and second 'c' leave wrong suffix length; third 'c' works
        assertTrue(SimpleReflectionPolicy.globMatch("a*cb", "acccb"));
        // first 'b' leaves too-short suffix; second 'b' aligns "bbc" correctly
        assertTrue(SimpleReflectionPolicy.globMatch("a*bbc", "abbbc"));
        // all 'c' positions leave a trailing char that can't be consumed
        assertFalse(SimpleReflectionPolicy.globMatch("a*cb", "acXb"));
    }

    // second * must backtrack when first match leaves wrong suffix length
    @Test
    public void twoSingleStarBacktracking() {
        // second * tries first 'c' (leftover "Xc" too long), then second 'c'
        assertTrue(SimpleReflectionPolicy.globMatch("a*b*c", "aXbXcXc"));
        // first * matches empty; second * tries first 'c' (1 char short), then second 'c'
        assertTrue(SimpleReflectionPolicy.globMatch("aa*bb*cc", "aabbbbcc"));
        // no 'c' exists in second segment at all
        assertFalse(SimpleReflectionPolicy.globMatch("a*b*c", "aXbXd"));
    }

    // ** must try later dots when earlier dot leaves the wrong suffix
    @Test
    public void doubleStarBacktracking() {
        // first '.' gives suffix "bar.bar" (7 chars) != "bar" (3) → tries second '.'
        assertTrue(SimpleReflectionPolicy.globMatch("**.bar", "foo.bar.bar"));
        // literal prefix consumed; ** tries first inner dot, then second
        assertTrue(SimpleReflectionPolicy.globMatch("foo.**.bar", "foo.bar.bar.bar"));
        // no dot position leaves a ".bar" suffix
        assertFalse(SimpleReflectionPolicy.globMatch("**.bar", "foo.baz.qux"));
    }
}
