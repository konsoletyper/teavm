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

package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@SuppressWarnings({ "RedundantStringConstructorCall", "RedundantCast" })
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class StringTokenizerTest {
    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_String() {
        StringTokenizer st = new StringTokenizer("This:is:a:test:String", ":");
        assertTrue("Created incorrect tokenizer", st.countTokens() == 5 && (st.nextElement().equals("This")));
    }

    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_StringZ() {
        StringTokenizer st = new StringTokenizer("This:is:a:test:String", ":", true);
        st.nextElement();
        assertTrue("Created incorrect tokenizer", st.countTokens() == 8 && (st.nextElement().equals(":")));
    }

    @Test
    public void test_countTokens() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        assertEquals("Incorrect token count returned", 5, st.countTokens());
    }

    @Test
    public void test_hasMoreElements() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        st.nextElement();
        assertTrue("hasMoreElements returned incorrect value", st.hasMoreElements());
        st.nextElement();
        st.nextElement();
        st.nextElement();
        st.nextElement();
        assertTrue("hasMoreElements returned incorrect value", !st.hasMoreElements());
    }

    @Test
    public void test_hasMoreTokens() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        for (int counter = 0; counter < 5; counter++) {
            assertTrue("StringTokenizer incorrectly reports it has no more tokens", st.hasMoreTokens());
            st.nextToken();
        }
        assertTrue("StringTokenizer incorrectly reports it has more tokens", !st.hasMoreTokens());
    }

    @Test
    public void test_nextElement() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        assertEquals("nextElement returned incorrect value", "This", st.nextElement());
        assertEquals("nextElement returned incorrect value", "is", st.nextElement());
        assertEquals("nextElement returned incorrect value", "a", st.nextElement());
        assertEquals("nextElement returned incorrect value", "test", st.nextElement());
        assertEquals("nextElement returned incorrect value", "String", st.nextElement());
        try {
            st.nextElement();
            fail("nextElement failed to throw a NoSuchElementException when it should have been out of elements");
        } catch (NoSuchElementException e) {
            // do nothing
        }
    }

    @Test
    public void test_nextToken() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        assertEquals("nextToken returned incorrect value", "This", st.nextToken());
        assertEquals("nextToken returned incorrect value", "is", st.nextToken());
        assertEquals("nextToken returned incorrect value", "a", st.nextToken());
        assertEquals("nextToken returned incorrect value", "test", st.nextToken());
        assertEquals("nextToken returned incorrect value", "String", st.nextToken());
        try {
            st.nextToken();
            fail("nextToken failed to throw a NoSuchElementException when it should have been out of elements");
        } catch (NoSuchElementException e) {
            // do nothing
        }
    }

    @Test
    public void test_nextTokenLjava_lang_String() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        assertEquals("nextToken(String) returned incorrect value with normal token String", "This", st.nextToken(" "));
        assertEquals("nextToken(String) returned incorrect value with custom token String", " is a ",
                st.nextToken("tr"));
        assertEquals("calling nextToken() did not use the new default delimiter list", "es", st.nextToken());
    }

    @Test
    public void test_hasMoreElements_NPE() {
        StringTokenizer stringTokenizer = new StringTokenizer(new String(), (String) null, true);
        try {
            stringTokenizer.hasMoreElements();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }

        stringTokenizer = new StringTokenizer(new String(), (String) null);
        try {
            stringTokenizer.hasMoreElements();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void test_hasMoreTokens_NPE() {
        StringTokenizer stringTokenizer = new StringTokenizer(new String(), (String) null, true);
        try {
            stringTokenizer.hasMoreTokens();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }

        stringTokenizer = new StringTokenizer(new String(), (String) null);
        try {
            stringTokenizer.hasMoreTokens();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void test_nextElement_NPE() {
        StringTokenizer stringTokenizer = new StringTokenizer(new String(), (String) null, true);
        try {
            stringTokenizer.nextElement();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }

        stringTokenizer = new StringTokenizer(new String(), (String) null);
        try {
            stringTokenizer.nextElement();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void test_nextToken_NPE() {
        StringTokenizer stringTokenizer = new StringTokenizer(new String(), (String) null, true);
        try {
            stringTokenizer.nextToken();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }

        stringTokenizer = new StringTokenizer(new String(), (String) null);
        try {
            stringTokenizer.nextToken();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void test_nextTokenLjava_lang_String_NPE() {
        StringTokenizer stringTokenizer = new StringTokenizer(new String());
        try {
            stringTokenizer.nextToken(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }
}
