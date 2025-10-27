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
package org.teavm.vm;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@SkipJVM
public class AsyncTest {
    @Test
    public void primitives() {
        assertEquals(23, getPrimitive());
    }

    @Async
    private native int getPrimitive();

    private void getPrimitive(AsyncCallback<Integer> callback) {
        Window.setTimeout(() -> callback.complete(23), 0);
    }

    @Test
    public void jsObjects() {
        var str = getJsString();
        assertEquals(3, str.getLength());
        assertEquals("foo", str.stringValue());
    }
    
    @Test
    public void switchAsyncArgument() {
        var sb = new StringBuilder();
        for (var i = 0; i < 4; ++i) {
            switch (returnSamePrimitive(i)) {
                case 0:
                    sb.append("zero;");
                    break;
                case 1:
                    sb.append("one;");
                    break;
                default:
                    sb.append("other;");
                    break;
            }
        }
        assertEquals("zero;one;other;other;", sb.toString());
    }
    
    @Test
    public void doublePrimitive() {
        assertEquals(2.5, returnSamePrimitive(2.5), 0.01);
    }
    
    @Test
    public void conditionalBranch() {
        var list = List.of(1, 2);
        var sb = new StringBuilder();
        for (var o : list) {
            sb.append(o.equals(1) ? getPrimitive() : 42).append(";");
        }
        assertEquals("23;42;", sb.toString());
    }
    
    @Test
    public void passStringConstant() {
        assertEquals("foo", returnSamePrimitive("foo"));
    }

    @Test
    public void collectionAddAll() {
        var list = new ArrayList<>() {
            @Override
            public boolean add(Object o) {
                returnSamePrimitive("ignore");
                return super.add(o);
            }
        };
        var toAdd = new ArrayList<String>() {
            @NotNull
            @Override
            public Iterator<String> iterator() {
                var underlying = super.iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        returnSamePrimitive("ignore");
                        return underlying.hasNext();
                    }

                    @Override
                    public String next() {
                        returnSamePrimitive("ignore");
                        return underlying.next();
                    }
                };
            }
        };
        toAdd.add("q");
        toAdd.add("w");
        list.addAll(toAdd);
        assertEquals(List.of("q", "w"), list);
    }
    
    @Test
    public void awaitPromise() {
        var promise = new JSPromise<String>((resolve, _) -> {
            Window.setTimeout(() -> resolve.accept("ok"), 1);
        });
        assertEquals("ok", promise.await());
    }

    @Async
    private native JSString getJsString();

    private void getJsString(AsyncCallback<JSString> callback) {
        Window.setTimeout(() -> callback.complete(JSString.valueOf("foo")), 0);
    }
    
    @Async
    private native int returnSamePrimitive(int value);
    
    private void returnSamePrimitive(int value, AsyncCallback<Integer> callback) {
        Window.setTimeout(() -> callback.complete(value), 0);
    }

    @Async
    private native double returnSamePrimitive(double value);

    private void returnSamePrimitive(double value, AsyncCallback<Double> callback) {
        Window.setTimeout(() -> callback.complete(value), 0);
    }
    
    @Async
    private native String returnSamePrimitive(String value);

    private void returnSamePrimitive(String value, AsyncCallback<String> callback) {
        Window.setTimeout(() -> callback.complete(value), 0);
    }
}
