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
    if (!ex.$jsException) {
        $rt_fillNativeException(ex);
    }
    return ex.$jsException;
}
let $rt_fillNativeException = ex => {
    let javaCause = $rt_throwableCause(ex);
    let jsCause = javaCause !== null ? javaCause.$jsException : void 0;
    let cause = typeof jsCause === "object" ? { cause : jsCause } : void 0;
    let err = new JavaError("Java exception thrown", cause);
    if (typeof teavm_globals.Error.captureStackTrace === "function") {
        teavm_globals.Error.captureStackTrace(err);
    }
    err[$rt_javaExceptionProp] = ex;
    ex.$jsException = err;
}
let $rt_fillStack = (err, ex) => {
    if (err.stack) {
        let stack;
        if (typeof $rt_decodeStack === "function") {
            stack = $rt_decodeStack(err.stack);
        } else {
            stack = $rt_parseNativeStack(err.stack);
        }
        let javaStack = $rt_createArray($rt_stecls(), stack.length);
        let elem;
        let noStack = false;
        let i = 0;
        while (i < stack.length) {
            let element = stack[i];
            elem = $rt_createStackElement($rt_str(element.className),
                $rt_str(element.methodName), $rt_str(element.fileName), element.lineNumber);
            if (elem == null) {
                noStack = true;
                break;
            }
            javaStack.data[i] = elem;
            ++i;
        }
        if (!noStack) {
            $rt_setStack(ex, javaStack);
        }
    }
}
let $rt_parseNativeStack = stackText => {
    let result = [];
    let lines = stackText.split(/\r?\n/);
    let i = 0;
    while (i < lines.length) {
        let line = lines[i].trim();
        let functionName = null;
        let fileName = null;
        let lineNumber = -1;
        let recognized = false;
        if (line.indexOf("at ") === 0) {
            recognized = true;
            line = line.substring(3);
            let openParen = line.lastIndexOf("(");
            if (openParen >= 0 && line.charAt(line.length - 1) === ")") {
                functionName = line.substring(0, openParen).trim() || null;
                line = line.substring(openParen + 1, line.length - 1);
            }
        } else {
            let at = line.lastIndexOf("@");
            if (at >= 0) {
                recognized = true;
                functionName = line.substring(0, at) || null;
                line = line.substring(at + 1);
            }
        }
        let lastColon = line.lastIndexOf(":");
        if (recognized && lastColon >= 0) {
            let lastPartText = line.substring(lastColon + 1);
            if ($rt_isDigits(lastPartText)) {
                let lastPart = Number(lastPartText);
                let previousColon = line.lastIndexOf(":", lastColon - 1);
                if (previousColon >= 0) {
                    let previousPartText = line.substring(previousColon + 1, lastColon);
                    if ($rt_isDigits(previousPartText)) {
                        lineNumber = Number(previousPartText);
                        line = line.substring(0, previousColon);
                    } else {
                        lineNumber = lastPart;
                        line = line.substring(0, lastColon);
                    }
                } else {
                    lineNumber = lastPart;
                    line = line.substring(0, lastColon);
                }
                if (lineNumber > 0) {
                    fileName = line || null;
                }
            }
        }
        if (fileName !== null) {
            result.push({ className: "<JS>", methodName: functionName || "<unknown function>",
                fileName: fileName, lineNumber: lineNumber });
        }
        ++i;
    }
    return result;
}
let $rt_isDigits = text => {
    if (text.length === 0) {
        return false;
    }
    let i = 0;
    while (i < text.length) {
        let code = text.charCodeAt(i);
        if (code < 48 || code > 57) {
            return false;
        }
        ++i;
    }
    return true;
}
let $rt_refreshNativeStack = ex => {
    let err = ex.$jsException;
    if (err && typeof teavm_globals.Error.captureStackTrace === "function") {
        teavm_globals.Error.captureStackTrace(err);
    } else if (err) {
        err.stack = (new teavm_globals.Error()).stack;
    }
    if (err) {
        $rt_fillStack(err, ex);
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
                if ($rt_exitCode !== null) {
                    // Throwable.getMessage() may be overridden by user, don't run any Java code after System.exit
                    return this[defaultMessage];
                }
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
    if ($rt_exitCode !== null) {
        // System.exit was called, no Java catch or finally block should run anymore
        throw err;
    }
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

let $rt_throwCCEIfFalse = (value, o) => {
    if (!value) {
        $rt_throwCCE();
    }
    return o;
}

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
let $rt_exitCode = null;
let $rt_exitHandler = null;
let $rt_ExitError = (() => {
    let message = code => "TeaVM module was terminated with exit code " + code;
    let ExitError;
    if (typeof Reflect === 'object') {
        ExitError = function ExitError(exitCode) {
            let self = teavm_globals.Reflect.construct(teavm_globals.Error, [message(exitCode)], ExitError);
            teavm_globals.Object.setPrototypeOf(self, ExitError.prototype);
            self.exitCode = exitCode;
            return self;
        };
        ExitError.prototype = teavm_globals.Object.create(teavm_globals.Error.prototype, {
            constructor: {
                configurable: true,
                writable: true,
                value: ExitError
            },
            name: {
                configurable: true,
                writable: true,
                value: "TeaVMExitError"
            }
        });
    } else {
        ExitError = function ExitError(exitCode) {
            let self = new teavm_globals.Error(message(exitCode));
            self.name = "TeaVMExitError";
            self.exitCode = exitCode;
            return self;
        };
    }
    return ExitError;
})();
let $rt_exit = code => {
    if ($rt_exitCode === null) {
        $rt_exitCode = code;
        if (typeof teavm_globals.process === "object" && teavm_globals.process !== null
                && typeof teavm_globals.process.exit === "function") {
            teavm_globals.process.exit(code);
        } else if ($rt_exitHandler !== null) {
            $rt_exitHandler(code);
        }
    }
    throw new $rt_ExitError($rt_exitCode);
}
let $rt_exited = () => $rt_exitCode !== null;
let $rt_checkAlive = () => {
    if ($rt_exitCode !== null) {
        throw new $rt_ExitError($rt_exitCode);
    }
}
let $rt_runtimeApi = () => ({
    setExitHandler: handler => {
        $rt_exitHandler = typeof handler === "function" ? handler : null;
    },
    get killedWithCode() {
        return $rt_exitCode;
    },
    ExitError: $rt_ExitError
});
