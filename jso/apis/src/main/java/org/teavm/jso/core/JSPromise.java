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

import org.teavm.jso.util.function.JSConsumer;
import org.teavm.jso.util.function.JSFunction;
import org.teavm.jso.util.function.JSSupplier;

/**
 * Interface for interacting with JavaScript <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise">{@code Promise}s</a>.
 *
 * @param <T> The type this promise returns when resolving successfully.
 * @param <E> The type this promise returns when rejecting.
 */
public abstract class JSPromise<T, E> implements JSObject {
    private JSPromise() {
    }

    /** Interface for a function wrapped by a promise. */
    @FunctionalInterface
    @JSFunctor
    public interface Executor<T, E> extends JSObject {
        /**
         * @param resolveFunc Call this function to resolve with success value.
         * @param rejectFunc Call this function to reject with error value.
         */
        void onExecute(JSConsumer<T> resolveFunc, JSConsumer<E> rejectFunc);
    }

    @JSBody(params = "executor", script = "return new Promise(executor);")
    @NoSideEffects
    public static native <T, E> JSPromise<T, E> create(Executor<T, E> executor);

    @JSBody(params = "promises", script = "return Promise.any(promises);")
    @NoSideEffects
    public static native JSPromise<?, ? extends JSError> any(@JSByRef JSArray promises);

    @JSBody(params = "promises", script = "return Promise.all(promises);")
    @NoSideEffects
    public static native JSPromise<JSArray, ?> all(@JSByRef JSArray promises);

    @JSBody(params = "promises", script = "return Promise.allSettled(promises);")
    @NoSideEffects
    public static native JSPromise<JSArray, ?> allSettled(@JSByRef JSArray promises);

    @JSBody(params = "promises", script = "return Promise.race(promises);")
    @NoSideEffects
    public static native JSPromise<?, ?> race(@JSByRef JSArray promises);

    @JSBody(params = "value", script = "return Promise.resolve(value);")
    @NoSideEffects
    public static native <V extends JSObject> JSPromise<V, ?> resolve(V value);

    @JSBody(params = "reason", script = "return Promise.reject(reason);")
    @NoSideEffects
    public static native <V extends JSObject> JSPromise<?, V> reject(V reason);

    /** Call {@code onFulfilled} with the success value, resolving with its return value. */
    public abstract <V> JSPromise<V, ?> then(JSFunction<T, V> onFulfilled);

    /** Call {@code onFulfilled} with the success value or {@code onRejected} with the reject reason, resolving with its return value. */
    public abstract <V> JSPromise<V, ?> then(JSFunction<T, V> onFulfilled, JSFunction<E, V> onRejected);

    /** Call {@code onFulfilled} with the success value, returning a new promise. */
    @JSMethod("then")
    public abstract <V, W> JSPromise<V, W> flatThen(JSFunction<T, ? extends JSPromise<V, W>> onFulfilled);

    /** Call {@code onFulfilled} with the success value or {@code onRejected} with the reject reason, returning a new promise. */
    @JSMethod("then")
    public abstract <V, W> JSPromise<V, W> flatThen(JSFunction<T, ? extends JSPromise<V, W>> onFulfilled, JSFunction<E, ? extends JSPromise<V, W>> onRejected);

    /** Call {@code onRejected} with the reject reason, resolving with its return value. */
    @JSMethod("catch")
    public abstract <V> JSPromise<V, ?> catchError(JSFunction<E, V> onRejected);

    /** Call {@code onRejected} with the reject reason, returning a new promise. */
    @JSMethod("catch")
    public abstract <V, W> JSPromise<V, W> flatCatchError(JSFunction<E, ? extends JSPromise<V, W>> onRejected);

    /** Call {@code onFinally} after settling, ignoring the return value. */
    @JSMethod("finally")
    public abstract <V> JSPromise<T, E> onSettled(JSSupplier<V> onFinally);
}
