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
package org.teavm.backend.c.generate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class StringPoolGeneratorTest {
    @Test
    public void plainStringsUseLiteral() {
        assertFalse(StringPoolGenerator.hasBadCharacters("hello   é €"));
    }

    @Test
    public void c1ControlCharactersUseNumericCodes() {
        // C11 disallows universal character names for code points below U+00A0
        for (char c = 0x7f; c < 0xa0; ++c) {
            assertTrue("U+" + Integer.toHexString(c), StringPoolGenerator.hasBadCharacters("a" + c + "b"));
        }
        assertFalse(StringPoolGenerator.hasBadCharacters("~ "));
    }

    @Test
    public void nulAndSurrogatesUseNumericCodes() {
        assertTrue(StringPoolGenerator.hasBadCharacters("\0"));
        assertTrue(StringPoolGenerator.hasBadCharacters("😀"));
    }
}
