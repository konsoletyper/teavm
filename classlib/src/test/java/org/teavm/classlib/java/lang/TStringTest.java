
/*
 *  Copyright 2020 by Joerg Hohwiller
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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test of Java11 specific methods that can not be accessed via Java8 in StringTest.
 */
public class TStringTest {
    @Test
    public void testIsBlank() {
        assertTrue(new TString(new char[0]).isBlank());
        assertTrue(new TString(new char[] { ' ', ' ' }).isBlank());
        assertFalse(new TString(new char[] { ' ', 'x', ' ' }).isBlank());
        assertFalse(new TString(new char[] { 'a', ' ' }).isBlank());
    }

}
