/*
 *  Copyright 2026 TeaVM contributors.
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

import static org.junit.Assert.assertArrayEquals;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class UnicodeHelperTest {
    @Test
    public void longRunsRoundTrip() {
        byte[] data = new byte[50000];
        Arrays.fill(data, 50, 40000, (byte) Character.OTHER_LETTER);
        Arrays.fill(data, 45000, 50000, (byte) Character.PRIVATE_USE);

        byte[] decoded = new byte[data.length];
        for (var range : UnicodeHelper.extractRle(UnicodeHelper.compressRle(data))) {
            System.arraycopy(range.data, 0, decoded, range.start, range.data.length);
        }

        assertArrayEquals(data, decoded);
    }
}
