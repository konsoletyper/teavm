/*
 *  Copyright 2016 Alexey Andreev.
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
import static org.teavm.metaprogramming.Metaprogramming.exit;
import org.junit.Test;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;

@CompileTime
public class MetaprogrammingTest {
    @Test
    public void works() {
        assertEquals("java.lang.Object".length() + 2, classNameLength(Object.class, 2));
        assertEquals("java.lang.Integer".length() + 3, classNameLength(Integer.valueOf(5).getClass(), 3));
    }

    @Meta
    static native int classNameLength(Class<?> cls, int add);
    static void classNameLength(ReflectClass<Object> cls, Value<Integer> add) {
        int length = cls.getName().length();
        exit(() -> length + add.get());
    }
}
