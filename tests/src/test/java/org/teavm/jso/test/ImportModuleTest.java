/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.jso.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSBodyImport;
import org.teavm.junit.AttachJavaScript;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
@EachTestCompiledSeparately
public class ImportModuleTest {
    @Test
    @AttachJavaScript({
            "org/teavm/jso/test/amd.js",
            "org/teavm/jso/test/amdModule.js"
    })
    public void amd() {
        assertEquals(23, runTestFunction());
    }

    @Test
    @AttachJavaScript("org/teavm/jso/test/commonjs.js")
    public void commonjs() {
        assertEquals(23, runTestFunction());
    }

    @JSBody(
            script = "return testModule.foo();",
            imports = @JSBodyImport(alias = "testModule", fromModule = "testModule.js")
    )
    private static native int runTestFunction();
}
