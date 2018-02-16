/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.classlib.java.util.concurrent;

import static org.junit.Assert.assertEquals;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class TimeUnitTest {
    @Test
    public void convert() {
        assertEquals(60, TimeUnit.MINUTES.toSeconds(1));
        assertEquals(30000, TimeUnit.SECONDS.toMillis(30));
        assertEquals(2, TimeUnit.DAYS.convert(48, TimeUnit.HOURS));
        assertEquals(180, TimeUnit.MINUTES.convert(3, TimeUnit.HOURS));
    }
}
