/*
 *  Copyright 2021 Alexey Andreev.
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
        case "JAVASCRIPT":
            appendFiles([request.file], 0, () => {
                launchTest(request.argument, response => {
                    event.source.postMessage(response, "*");
                });
            }, error => {
                event.source.postMessage(wrapResponse({ status: "failed", errorMessage: error }), "*");
            });
            break;

        case "WASM":
            const runtimeFile = request.file + "-runtime.js";
            appendFiles([runtimeFile], 0, () => {
                launchWasmTest(request.file, request.argument, response => {
                    event.source.postMessage(response, "*");
                });
            }, error => {
                event.source.postMessage(wrapResponse({ status: "failed", errorMessage: error }), "*");
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

function launchTest(argument, callback) {
    main(argument ? [argument] : [], result => {
        if (result instanceof Error) {
            callback(wrapResponse({
                status: "failed",
                errorMessage: buildErrorMessage(result)
            }));
        } else {
            callback({ status: "OK" });
        }
    });

    function buildErrorMessage(e) {
        if (typeof $rt_decodeStack === "function" && typeof teavmException == "string") {
            return teavmException;
        }
        let stack = "";
        let je = main.javaException(e);
        if (je && je.constructor.$meta) {
            stack = je.constructor.$meta.name + ": ";
            stack += je.getMessage();
            stack += "\n";
        }
        stack += e.stack;
        return stack;
    }
}

function launchWasmTest(path, argument, callback) {
    let output = [];
    let outputBuffer = "";

    function putwchar(charCode) {
        if (charCode === 10) {
            switch (outputBuffer) {
                case "SUCCESS":
                    callback(wrapResponse({ status: "OK" }));
                    break;
                case "FAILURE":
                    callback(wrapResponse({
                        status: "failed",
                        errorMessage: output.join("\n")
                    }));
                    break;
                default:
                    output.push(outputBuffer);
                    outputBuffer = "";
            }
        } else {
            outputBuffer += String.fromCharCode(charCode);
        }
    }

    TeaVM.wasm.load(path, {
        installImports: function(o) {
            o.teavm.putwchar = putwchar;
        },
        errorCallback: function(err) {
            callback(wrapResponse({
                status: "failed",
                errorMessage: err.message + '\n' + err.stack
            }));
        }
    }).then(teavm => {
        teavm.main(argument ? [argument] : []);
    })
    .then(() => {
        callback(wrapResponse({ status: "OK" }));
    })
    .catch(err => {
        callback(wrapResponse({
            status: "failed",
            errorMessage: err.message + '\n' + err.stack
        }));
    })
}

function start() {
    window.parent.postMessage("ready", "*");
}

let log = [];

function wrapResponse(response) {
    if (log.length > 0) {
        response.log = log;
        log = [];
    }
    return response;
}

let $rt_putStdoutCustom = createOutputFunction(msg => {
    log.push({ type: "stdout", message: msg });
});
let $rt_putStderrCustom = createOutputFunction(msg => {
    log.push({ type: "stderr", message: msg });
});

function createOutputFunction(printFunction) {
    let buffer = "";
    let utf8Buffer = 0;
    let utf8Remaining = 0;

    function putCodePoint(ch) {
        if (ch === 0xA) {
            printFunction(buffer);
            buffer = "";
        } else if (ch < 0x10000) {
            buffer += String.fromCharCode(ch);
        } else {
            ch = (ch - 0x10000) | 0;
            var hi = (ch >> 10) + 0xD800;
            var lo = (ch & 0x3FF) + 0xDC00;
            buffer += String.fromCharCode(hi, lo);
        }
    }

    return ch => {
        if ((ch & 0x80) === 0) {
            putCodePoint(ch);
        } else if ((ch & 0xC0) === 0x80) {
            if (utf8Buffer > 0) {
                utf8Remaining <<= 6;
                utf8Remaining |= ch & 0x3F;
                if (--utf8Buffer === 0) {
                    putCodePoint(utf8Remaining);
                }
            }
        } else if ((ch & 0xE0) === 0xC0) {
            utf8Remaining = ch & 0x1F;
            utf8Buffer = 1;
        } else if ((ch & 0xF0) === 0xE0) {
            utf8Remaining = ch & 0x0F;
            utf8Buffer = 2;
        } else if ((ch & 0xF8) === 0xF0) {
            utf8Remaining = ch & 0x07;
            utf8Buffer = 3;
        }
    };
}