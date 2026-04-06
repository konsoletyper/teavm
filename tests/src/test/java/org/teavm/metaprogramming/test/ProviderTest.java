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
package org.teavm.metaprogramming.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@EachTestCompiledSeparately
public class ProviderTest {
    @Test
    public void simple() {
        assertEquals("int: 1\njava.lang.String: qwe\n", argWithType1(1, "qwe"));
        assertEquals("long: 23\n", argWithType2(23));
    }
    
    @ArgWithType
    private static native String argWithType1(int a, String b);

    @ArgWithType
    private static native String argWithType2(long c);
}
