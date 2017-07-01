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
package org.teavm.classlib.impl;

import static org.junit.Assert.*;
import org.junit.Test;

public class Base46Test {
    @Test
    public void encode() {
        StringBuilder sb = new StringBuilder();
        for (int i = -65536; i <= 65536; ++i) {
            sb.setLength(0);
            Base46.encode(sb, i);
            CharFlow flow = new CharFlow(sb.toString().toCharArray());
            int num = Base46.decode(flow);
            assertEquals(i, num);
        }
    }
}
