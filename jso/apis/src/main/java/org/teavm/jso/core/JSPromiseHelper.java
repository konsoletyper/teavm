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
package org.teavm.jso.core;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSExceptions;

class JSPromiseHelper {
    private JSPromiseHelper() {
    }

    @Async
    static native <T> T await(JSPromise<T> promise);

    static <T> void await(JSPromise<T> promise, AsyncCallback<T> callback) {
        promise.then(
                result -> {
                    callback.complete(result);
                    return null;
                },
                err -> {
                    if (err instanceof Throwable) {
                        callback.error((Throwable) err);
                    } else if (err instanceof JSError) {
                        var javaException = JSExceptions.getJavaException((JSError) err);
                        if (javaException != null) {
                            callback.error(javaException);
                        } else {
                            callback.error(new RuntimeException());
                        }
                    } else {
                        callback.error(new RuntimeException());
                    }
                    return null;
                }
        );
    }
}
