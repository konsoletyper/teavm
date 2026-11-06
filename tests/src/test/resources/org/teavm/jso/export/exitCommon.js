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

// Runs the given scenario, which must lead to System.exit with expectedCode,
// then checks that no Java code runs afterwards, neither by pending coroutines nor by calls from JS
export async function checkExit(name, scenario, expectedCode, expectedLog) {
    const provider = await import(`/tests/${name}/provider.js`);
    const module = await provider.default;
    const { init, createCallback, createObject, sleepThenLog, waitForJsThenLog, throwCustomException, C } = module;
    const { ExitError } = provider;

    assertEquals(null, provider.killedWithCode());

    let log = [];
    let lateErrors = [];
    init(log, lateErrors);
    let callback = createCallback("callback");
    let obj = createObject();
    callback();
    obj.foo();
    obj.bar;
    C.baz();
    new C();
    assertEquals(["constructor", "callback", "foo", "bar", "baz", "constructor"], log);

    let javaError = null;
    try {
        throwCustomException();
    } catch (e) {
        javaError = e;
    }
    assertEquals("custom message", javaError.message);
    log.length = 0;

    // coroutines that are suspended at the moment of exit
    sleepThenLog(200, "sleep resumed");
    waitForJsThenLog(200, "JS wait resumed");
    // let threads start and suspend
    await new Promise(resolve => setTimeout(resolve, 20));

    await scenario(module, ExitError);
    await new Promise(resolve => setTimeout(resolve, 400));

    assertEquals([expectedCode], provider.exitCodes);
    assertEquals(expectedCode, provider.killedWithCode());
    assertEquals(expectedLog, log);
    // coroutine waiting for JS is resumed by calling Java functor, which should throw
    assertEquals(1, lateErrors.length);
    checkExitError(lateErrors[0], ExitError, expectedCode);

    // user's getMessage() must not be called after exit
    javaError.message;
    assertEquals(expectedLog, log);

    let calls = {
        "exported function": () => sleepThenLog(0, "should not happen"),
        "functor": () => callback(),
        "method": () => obj.foo(),
        "property": () => obj.bar,
        "static method": () => C.baz(),
        "constructor": () => new C()
    };
    for (let [what, call] of Object.entries(calls)) {
        let error = null;
        try {
            call();
        } catch (e) {
            error = e;
        }
        if (error === null) {
            throw new Error(`Calling ${what} after exit did not fail`);
        }
        checkExitError(error, ExitError, expectedCode);
    }
    await new Promise(resolve => setTimeout(resolve, 50));
    assertEquals(expectedLog, log);
    assertEquals([expectedCode], provider.exitCodes);
}

export function checkExitError(error, ExitError, expectedCode) {
    if (!(error instanceof ExitError)) {
        throw new Error("Expected ExitError, got " + error);
    }
    if (!(error instanceof Error)) {
        throw new Error("ExitError is not an instance of Error");
    }
    assertEquals("TeaVMExitError", error.name);
    assertEquals(expectedCode, error.exitCode);
    assertEquals(`TeaVM module was terminated with exit code ${expectedCode}`, error.message);
}
