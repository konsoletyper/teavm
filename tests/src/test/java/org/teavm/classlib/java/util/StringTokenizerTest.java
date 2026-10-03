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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@SuppressWarnings("RedundantCast")
@TeaVMTest
public class StringTokenizerTest {
    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_String() {
        StringTokenizer st = new StringTokenizer("This:is:a:test:String", ":");
        assertTrue(st.countTokens() == 5 && (st.nextElement().equals("This")), "Created incorrect tokenizer");
    }

    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_StringZ() {
        StringTokenizer st = new StringTokenizer("This:is:a:test:String", ":", true);
        st.nextElement();
        assertTrue(st.countTokens() == 8 && (st.nextElement().equals(":")), "Created incorrect tokenizer");
    }

    @Test
    public void test_countTokens() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        assertEquals(5, st.countTokens(), "Incorrect token count returned");
    }

    @Test
    public void test_hasMoreElements() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        st.nextElement();
        assertTrue(st.hasMoreElements(), "hasMoreElements returned incorrect value");
        st.nextElement();
        st.nextElement();
        st.nextElement();
        st.nextElement();
        assertTrue(!st.hasMoreElements(), "hasMoreElements returned incorrect value");
    }

    @Test
    public void test_hasMoreTokens() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        for (int counter = 0; counter < 5; counter++) {
            assertTrue(st.hasMoreTokens(), "StringTokenizer incorrectly reports it has no more tokens");
            st.nextToken();
        }
        assertTrue(!st.hasMoreTokens(), "StringTokenizer incorrectly reports it has more tokens");
    }

    @Test
    public void test_nextElement() {
        StringTokenizer st = new StringTokenizer("This is a test String");
        assertEquals("This", st.nextElement(), "nextElement returned incorrect value");
        assertEquals("is", st.nextElement(), "nextElement returned incorrect value");
        assertEquals("a", st.nextElement(), "nextElement returned incorrect value");
        assertEquals("test", st.nextElement(), "nextElement returned incorrect value");
        assertEquals("String", st.nextElement(), "nextElement returned incorrect value");
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
        assertEquals("This", st.nextToken(), "nextToken returned incorrect value");
        assertEquals("is", st.nextToken(), "nextToken returned incorrect value");
        assertEquals("a", st.nextToken(), "nextToken returned incorrect value");
        assertEquals("test", st.nextToken(), "nextToken returned incorrect value");
        assertEquals("String", st.nextToken(), "nextToken returned incorrect value");
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
        assertEquals("This", st.nextToken(" "), "nextToken(String) returned incorrect value with normal token String");
        assertEquals(" is a ", st.nextToken("tr"),
                "nextToken(String) returned incorrect value with custom token String");
        assertEquals("es", st.nextToken(), "calling nextToken() did not use the new default delimiter list");
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
