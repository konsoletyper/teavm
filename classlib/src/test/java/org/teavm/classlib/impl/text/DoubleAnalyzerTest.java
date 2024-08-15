/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.classlib.impl.text;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DoubleAnalyzerTest {
    @Test
    public void decimalExponent() {
        var numbers = new double[] { 1e-200, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1, 1, 1e1, 1e2, 1e3, 1e4, 1e5, 1e200 };
        var result = new DoubleAnalyzer.Result();
        for (var number : numbers) {
            var e = number / 100;
            assertEquals((int) Math.log10(number), DoubleAnalyzer.fastIntLog10(number));
            assertEquals((int) Math.log10(number + e), DoubleAnalyzer.fastIntLog10(number + e));
            assertEquals((int) Math.log10(number - e), DoubleAnalyzer.fastIntLog10(number - e));
        }
    }
}
