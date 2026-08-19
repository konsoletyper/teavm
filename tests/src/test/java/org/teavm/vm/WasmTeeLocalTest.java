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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.interop.Intrinsified;
import org.teavm.interop.NativeAsync;
import org.teavm.jso.browser.Window;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@OnlyPlatform(TestPlatform.WEBASSEMBLY_GC)
@SkipJVM
public class WasmTeeLocalTest {
    /**
     * local.tee leaves a value typed as the local's declared type, not as the type of the value
     * that was stored. When a String is teed into an Object local and a suspension point follows
     * while the value is still on the stack, recording the value's own type gives the restore
     * block a signature narrower than what the running code leaves there, so the branch over the
     * restore path fails to validate: "type error in branch[0] (expected (ref null String),
     * got (ref null Object))".
     */
    @Test
    public void suspendOverTeeToWiderLocal() {
        assertEquals(2, teeThenSuspend(0));
    }

    @Async
    @NativeAsync
    @Intrinsified
    private static native int teeThenSuspend(int n);

    static String newString() {
        return "s";
    }

    static int tag(Object o) {
        return o instanceof String ? 1 : 2;
    }

    @Async
    private static native int sum(int a, int b);

    private static void sum(int a, int b, AsyncCallback<Integer> callback) {
        Window.setTimeout(() -> callback.complete(a + b), 0);
    }
}
