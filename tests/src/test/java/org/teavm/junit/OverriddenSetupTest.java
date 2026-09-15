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
package org.teavm.junit;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TeaVMTestRunner.class)
public class OverriddenSetupTest extends InheritedSetupBase {
    private int overriddenSetupCount;

    @Before
    @Override
    public void countSetup() {
        overriddenSetupCount++;
    }

    @Test
    public void anOverriddenSetupRunsOncePerTest() {
        assertEquals(1, overriddenSetupCount);
        assertEquals(0, setupCount);
    }
}
