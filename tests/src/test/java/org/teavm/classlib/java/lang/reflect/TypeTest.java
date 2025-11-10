/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.lang.reflect;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@OnlyPlatform({TestPlatform.WEBASSEMBLY_GC, TestPlatform.JAVASCRIPT})
public class TypeTest {
    @Test
    public void classTypeParameters() {
        var s = Arrays.stream(A.class.getTypeParameters())
                .map(TypeVariable::getName)
                .collect(Collectors.joining(", "));
        assertEquals("T", s);
        
        s = Arrays.stream(B.class.getTypeParameters())
                .map(TypeVariable::getName)
                .collect(Collectors.joining(", "));
        assertEquals("Q, W", s);

        s = Arrays.stream(C.class.getTypeParameters())
                .map(TypeVariable::getName)
                .collect(Collectors.joining(", "));
        assertEquals("", s);
    }

    interface A<T> {
    }

    static class B<Q, W> {
    }
    
    static class C {
    }
}
