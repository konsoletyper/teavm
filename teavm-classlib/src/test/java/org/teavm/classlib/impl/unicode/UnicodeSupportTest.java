package org.teavm.classlib.impl.unicode;

import static org.junit.Assert.assertFalse;

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
