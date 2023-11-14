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
"use strict";

let $rt_throw = ex => {
    throw $rt_exception(ex)
};
let $rt_javaExceptionProp = teavm_globals.Symbol("javaException")
let $rt_exception = ex => {
    let err = ex.$jsException;
    if (!err) {
        let javaCause = $rt_throwableCause(ex);
        let jsCause = javaCause !== null ? javaCause.$jsException : void 0;
        let cause = typeof jsCause === "object" ? { cause : jsCause } : void 0;
        err = new JavaError("Java exception thrown", cause);
        if (typeof teavm_globals.Error.captureStackTrace === "function") {
            teavm_globals.Error.captureStackTrace(err);
        }
        err[$rt_javaExceptionProp] = ex;
        ex.$jsException = err;
        $rt_fillStack(err, ex);
    }
    return err;
}
let $rt_fillStack = (err, ex) => {
    if (typeof $rt_decodeStack === "function" && err.stack) {
        let stack = $rt_decodeStack(err.stack);
        let javaStack = $rt_createArray($rt_stecls(), stack.length);
        let elem;
        let noStack = false;
        for (let i = 0; i < stack.length; ++i) {
            let element = stack[i];
            elem = $rt_createStackElement($rt_str(element.className),
                $rt_str(element.methodName), $rt_str(element.fileName), element.lineNumber);
            if (elem == null) {
                noStack = true;
                break;
            }
            javaStack.data[i] = elem;
        }
        if (!noStack) {
            $rt_setStack(ex, javaStack);
        }
    }
}

let JavaError;
if (typeof Reflect === 'object') {
    let defaultMessage = teavm_globals.Symbol("defaultMessage");
    JavaError = function JavaError(message, cause) {
        let self = teavm_globals.Reflect.construct(teavm_globals.Error, [void 0, cause], JavaError);
        teavm_globals.Object.setPrototypeOf(self, JavaError.prototype);
        self[defaultMessage] = message;
        return self;
    }
    JavaError.prototype = teavm_globals.Object.create(teavm_globals.Error.prototype, {
        constructor: {
            configurable: true,
            writable: true,
            value: JavaError
        },
        message: {
            get() {
                try {
                    let javaException = this[$rt_javaExceptionProp];
                    if (typeof javaException === 'object') {
                        let javaMessage = $rt_throwableMessage(javaException);
                        if (typeof javaMessage === "object") {
                            return javaMessage !== null ? javaMessage.toString() : null;
                        }
                    }
                    return this[defaultMessage];
                } catch (e) {
                    return "Exception occurred trying to extract Java exception message: " + e
                }
            }
        }
    });
} else {
    JavaError = teavm_globals.Error;
}

let $rt_javaException = e => e instanceof teavm_globals.Error && typeof e[$rt_javaExceptionProp] === 'object'
    ? e[$rt_javaExceptionProp]
    : null;

let $rt_jsException = e => typeof e.$jsException === 'object' ? e.$jsException : null;
let $rt_wrapException = err => {
    let ex = err[$rt_javaExceptionProp];
    if (!ex) {
        ex = $rt_createException($rt_str("(JavaScript) " + err.toString()));
        err[$rt_javaExceptionProp] = ex;
        ex.$jsException = err;
        $rt_fillStack(err, ex);
    }
    return ex;
}

let $rt_createException = message => teavm_javaConstructor("java.lang.RuntimeException",
    "(Ljava/lang/String;)V")(message);
let $rt_throwableMessage = t => teavm_javaMethod("java.lang.Throwable", "getMessage()Ljava/lang/String;")(t);
let $rt_throwableCause = t => teavm_javaMethod("java.lang.Throwable", "getCause()Ljava/lang/Throwable;")(t);
let $rt_stecls = () => teavm_javaClassExists("java.lang.StackTraceElement")
    ? teavm_javaClass("java.lang.StackTraceElement")
    : $rt_objcls();

let $rt_throwAIOOBE = () => teavm_javaConstructorExists("java.lang.ArrayIndexOutOfBoundsException", "()V")
    ? $rt_throw(teavm_javaConstructor("java.lang.ArrayIndexOutOfBoundsException", "()V")())
    : $rt_throw($rt_createException($rt_str("")));

let $rt_throwCCE = () => teavm_javaConstructorExists("java.lang.ClassCastException", "()V")
    ? $rt_throw(teavm_javaConstructor("java.lang.ClassCastException", "()V")())
    : $rt_throw($rt_createException($rt_str("")));

let $rt_createStackElement = (className, methodName, fileName, lineNumber) => {
    if (teavm_javaConstructorExists("java.lang.StackTraceElement",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")) {
        return teavm_javaConstructor("java.lang.StackTraceElement",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V")(className, methodName, fileName, lineNumber);
    } else {
        return null;
    }
}
let $rt_setStack = (e, stack) => {
    if (teavm_javaMethodExists("java.lang.Throwable", "setStackTrace([Ljava/lang/StackTraceElement;)V")) {
        teavm_javaMethod("java.lang.Throwable", "setStackTrace([Ljava/lang/StackTraceElement;)V")(e, stack);
    }
}