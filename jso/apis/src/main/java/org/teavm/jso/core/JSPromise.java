/*
 *  Copyright 2023 Bernd Busse.
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
package org.teavm.jso.core;

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

public abstract class JSPromise implements JSObject {
    private JSPromise() {
    }

    @JSFunctor
    public interface Executor extends JSObject {
        void onExecute(JSFunction resolveFunc, JSFunction rejectFunc);
    }

    @JSBody(params = "executor", script = "return new Promise(executor);")
    @NoSideEffects
    public static native JSPromise create(Executor executor);

    @JSBody(params = "promises", script = "return Promise.any(promises);")
    @NoSideEffects
    public static native JSPromise any(@JSByRef JSArray promises);

    @JSBody(params = "promises", script = "return Promise.all(promises);")
    @NoSideEffects
    public static native JSPromise all(@JSByRef JSArray promises);

    @JSBody(params = "promises", script = "return Promise.allSettled(promises);")
    @NoSideEffects
    public static native JSPromise allSettled(@JSByRef JSArray promises);

    @JSBody(params = "promises", script = "return Promise.race(promises);")
    @NoSideEffects
    public static native JSPromise race(@JSByRef JSArray promises);

    @JSBody(params = "value", script = "return Promise.resolve(value);")
    @NoSideEffects
    public static native JSPromise resolve(JSObject value);

    @JSBody(params = "reason", script = "return Promise.reject(reason);")
    @NoSideEffects
    public static native JSPromise reject(JSObject reason);

    public abstract JSPromise then(JSFunction onFulfilled);

    public abstract JSPromise then(JSFunction onFulfilled, JSFunction onRejected);

    @JSMethod("catch")
    public abstract JSPromise catch0(JSFunction onRejected);

    @JSMethod("finally")
    public abstract JSPromise finally0(JSFunction onFinally);
}
