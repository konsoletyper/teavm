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
public class WasmFlattenedConditionalTest {
    /**
     * A conditional declared to produce Object, whose arms push different subclasses, is flattened
     * away by the coroutine transformation because it contains a suspension point. What the block
     * leaves on the stack is an Object; the arm walked last pushed a String. A second suspension
     * point downstream turns whichever of the two was recorded into a block signature, so recording
     * the arm's type gave a label typed at String with an Object branching to it.
     */
    @Test
    public void suspendAfterFlattenedConditional() {
        assertEquals(9, conditionalThenSuspend(1));
        assertEquals(8, conditionalThenSuspend(0));
    }

    @Async
    @NativeAsync
    @Intrinsified
    private static native int conditionalThenSuspend(int n);

    static Object newObject() {
        return new Object();
    }

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
