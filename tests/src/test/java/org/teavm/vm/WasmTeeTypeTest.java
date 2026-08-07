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
public class WasmTeeTypeTest {
    /**
     * local.tee yields the type of the local it writes, not the type of the value handed to it.
     * Here a subclass value is tee'd into a local declared at the superclass and is still on the
     * stack across a suspension point, so the coroutine transformation turns the recorded stack
     * type into a block signature. Recording the value's type rather than the local's produced a
     * label typed at the subclass with a superclass value branching to it.
     */
    @Test
    public void suspendWithValueTeedIntoWiderLocal() {
        // sum(20, 25) plus the length of "generated"
        assertEquals(54, teeThenSuspend());
    }

    @Async
    @NativeAsync
    @Intrinsified
    private static native int teeThenSuspend();

    static IllegalStateException newError() {
        return new IllegalStateException("generated");
    }

    static int lengthOf(Throwable t) {
        return t.getMessage().length();
    }

    @Async
    private static native int sum(int a, int b);

    private static void sum(int a, int b, AsyncCallback<Integer> callback) {
        Window.setTimeout(() -> callback.complete(a + b), 0);
    }
}
