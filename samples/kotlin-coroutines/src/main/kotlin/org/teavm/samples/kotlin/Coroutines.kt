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

package org.teavm.samples.kotlin

import org.teavm.jso.browser.Window
import org.teavm.jso.core.JSPromise
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.intrinsics.*

fun main() {
    async {
        for (i in 1..20) {
            println("task 1: $i")
            delay(500).await()
        }
    }
    async {
        for (i in 1..10) {
            println("task 2: $i")
            delay(750).await()
        }
    }
}

fun <T> async(block: suspend () -> T): JSPromise<T> {
    return JSPromise { resolve, reject ->
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result ->
            result.onSuccess(resolve::accept).onFailure(reject::accept)
        })
    }
}

suspend fun <T> JSPromise<T>.await(): T = suspendCoroutineUninterceptedOrReturn { cont ->
    then { cont.resumeWith(Result.success(it)) }
        .catchError { cont.resumeWith(Result.failure(it.asThrowable())) }
    COROUTINE_SUSPENDED
}

fun delay(ms: Long): JSPromise<Unit> = JSPromise { resolve, _ ->
    Window.setTimeout({ resolve.accept(Unit) }, ms.toDouble())
}

private fun Any.asThrowable(): Throwable {
    return if (this is Throwable) {
        this
    } else {
        RuntimeException(this.toString())
    }
}