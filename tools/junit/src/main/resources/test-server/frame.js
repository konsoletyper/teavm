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
            const files = request.additionalFiles ? [...request.additionalFiles, request.file] : [request.file];
            appendFiles(files, 0, () => {
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
    let outputBufferStderr = "";

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
                    log.push({ message: outputBuffer, type: "stdout" });
                    outputBuffer = "";
            }
        } else {
            outputBuffer += String.fromCharCode(charCode);
        }
    }

    function putwchars(controller, buffer, count) {
        let memory = new Int8Array(instance.exports.memory.buffer);
        for (let i = 0; i < count; ++i) {
            // TODO: support UTF-8
            putwchar(memory[buffer++]);
        }
    }

    function putwcharStderr(charCode) {
        if (charCode === 10) {
            log.push({ message: outputBufferStderr, type: "stderr" });
            outputBufferStderr = "";
        } else {
            outputBufferStderr += String.fromCharCode(charCode);
        }
    }

    function putwcharsStderr(controller, buffer, count) {
        let memory = new Int8Array(instance.exports.memory.buffer);
        for (let i = 0; i < count; ++i) {
            // TODO: support UTF-8
            putwcharStderr(memory[buffer++]);
        }
    }

    let instance = null;

    TeaVM.wasm.load(path, {
        installImports: function(o) {
            o.teavm.putwcharsOut = (chars, count) => putwchars(instance, chars, count);
            o.teavm.putwcharsErr = (chars, count) => putwcharsStderr(instance, chars, count);
            o.teavm.putwchar = putwchar;
        },
        errorCallback: function(err) {
            callback(wrapResponse({
                status: "failed",
                errorMessage: err.message + '\n' + err.stack
            }));
        }
    }).then(teavm => {
        instance = teavm.instance;
        return teavm.main(argument ? [argument] : []);
    }).catch(err => {
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
    return msg => {
        let index = 0;
        while (true) {
            let next = msg.indexOf('\n', index);
            if (next < 0) {
                break;
            }
            printFunction(buffer + msg.substring(index, next));
            buffer = "";
            index = next + 1;
        }
        buffer += msg.substring(index);
    }
}