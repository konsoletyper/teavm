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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@EachTestCompiledSeparately
@SkipJVM
@OnlyPlatform(TestPlatform.WEBASSEMBLY_GC)
public class WasmTest {
    @Test
    public void returnWithTryCatch() {
        // don't assert anything, just make sure this code produces valid module
        tryCatchWithReturnInside();
    }
    
    private native int tryCatchWithReturnInside();

    private static int foo() {
        return 2;
    }

    @Test
    public void largeStartFunction() {
        // The start function fills the virtual tables of all generated classes. V8 does not compile
        // a function body larger than 7,654,321 bytes.
        assertEquals(WasmTestClassTransformer.VIRTUAL_METHOD_COUNT - 1, callManyVirtualMethods());
    }

    private static native int callManyVirtualMethods();

    @Test
    public void largeReflectionInitializer() {
        // teavm@initReflection creates the annotations of all generated classes.
        assertEquals(1, countAnnotationsOfGeneratedClass());
    }

    private static native int countAnnotationsOfGeneratedClass();

    @Retention(RetentionPolicy.RUNTIME)
    @interface LargeAnnotation {
        int[] value();
    }
}
