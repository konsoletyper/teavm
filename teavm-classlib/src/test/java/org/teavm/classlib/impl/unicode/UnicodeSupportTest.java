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
package org.teavm.classlib.impl.unicode;

import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class UnicodeSupportTest {
    private static boolean pairsEqual(final int[] pairs, final int index1, final int index2) {
        return pairs[index1] == pairs[index2] && pairs[index1 + 1] == pairs[index2 + 1];
    }

    @Test
    public void test_getDigitValues() {
        final int[] digitValues = UnicodeSupport.getDigitValues();
        if (digitValues.length >= 4) {
            // there are no duplicates, so the last two pairs should not be identical
            assertFalse(pairsEqual(digitValues, digitValues.length - 4, digitValues.length - 2));
        }
    }
}
