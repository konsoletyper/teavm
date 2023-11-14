/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.WASI)
public class DateTest {
    @SuppressWarnings("deprecation")
    @Test
    @SkipPlatform(TestPlatform.WEBASSEMBLY)
    public void setsDateAndMonth() {
        Date date = new Date();
        date.setMonth(0);
        date.setDate(4);
        date.setYear(115);
        assertEquals(0, date.getMonth());
        assertEquals(4, date.getDate());
        assertEquals(115, date.getYear());
    }

    @SuppressWarnings("deprecation")
    @Test
    @SkipPlatform(TestPlatform.WEBASSEMBLY)
    public void setsUTC() {
        long epochTime = Date.UTC(2023, 1, 20, 10, 0, 0);
        Date date = new Date(epochTime);
        assertEquals(2023, date.getYear());
        assertEquals(1, date.getMonth());
        assertEquals(20, date.getDate());
    }
}
