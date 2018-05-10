/*
 *  Copyright 2017 Alexey Andreev.
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

window.addEventListener("message", event => {
    let request = event.data;
    switch (request.type) {
        case "js":
            appendFiles(request.files, 0, () => {
                launchTest(response => {
                    event.source.postMessage(response, "*");
                });
            }, error => {
                event.source.postMessage({ status: "failed", errorMessage: error }, "*");
            });
            break;
        case "wasm":
            appendFiles(request.files.filter(f => f.endsWith(".js")), 0, () => {
                launchWasmTest(request.files.filter(f => f.endsWith(".wasm"))[0], response => {
                    event.source.postMessage(response, "*");
                });
            }, error => {
                event.source.postMessage({ status: "failed", errorMessage: error }, "*");
            });
            break;
    }
});

function appendFiles(files, index, callback, errorCallback) {
    if (index === files.length) {
        callback();
    } else {
        let fileName = files[index];
        let script = document.createElement("script");
        script.onload = () => {
            appendFiles(files, index + 1, callback, errorCallback);
        };
        script.onerror = () => {
            errorCallback("failed to load script " + fileName);
        };
        script.src = fileName;
        document.body.appendChild(script);
    }
}

function launchTest(callback) {
    $rt_startThread(() => {
        let thread = $rt_nativeThread();
        let instance;
        let message;
        if (thread.isResuming()) {
            instance = thread.pop();
        }
        try {
            runTest();
        } catch (e) {
            message = buildErrorMessage(e);
            callback({
                status: "failed",
                errorMessage: buildErrorMessage(e)
            });
            return;
        }
        if (thread.isSuspending()) {
            thread.push(instance);
        } else {
            callback({ status: "OK" });
        }
    });

    function buildErrorMessage(e) {
        let stack = e.stack;
        if (e.$javaException && e.$javaException.constructor.$meta) {
            stack = e.$javaException.constructor.$meta.name + ": ";
            let exceptionMessage = extractException(e.$javaException);
            stack += exceptionMessage ? $rt_ustr(exceptionMessage) : "";
        }
        stack += "\n" + stack;
        return stack;
    }
}

function launchWasmTest(path, callback) {
    var output = [];
    var outputBuffer = "";

    function putwchar(charCode) {
        if (charCode === 10) {
            switch (outputBuffer) {
                case "SUCCESS":
                    callback({status: "OK"});
                    break;
                case "FAILURE":
                    callback({
                        status: "failed",
                        errorMessage: output.join("\n")
                    });
                    break;
                default:
                    output.push(outputBuffer);
                    outputBuffer = "";
            }
        } else {
            outputBuffer += String.fromCharCode(charCode);
        }
    }

    TeaVM.wasm.run(path, {
        installImports: function(o) {
            o.teavm.putwchar = putwchar;
        },
        errorCallback: function(err) {
            callback({
                status: "failed",
                errorMessage: err.message + '\n' + err.stack
            });
        }
    });
}

function start() {
    window.parent.postMessage("ready", "*");
}