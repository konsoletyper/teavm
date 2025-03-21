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

Error.stackTraceLimit = 250;

let lastLauncher = null;

window.addEventListener("message", event => {
    let request = event.data;
    switch (request.type) {
        case "JAVASCRIPT":
            processRequest(event, async () => {
                const files = request.additionalFiles ? [...request.additionalFiles, request.file] : [request.file];
                await appendFiles(files);
                let launcher = prepareJsLauncher();
                lastLauncher = launcher;
                await launcher(request.argument);
            });
            break;
        case "WASM":
            processRequest(event, async () => {
                const runtimeFile = request.file.path + "-runtime.js";
                await appendFiles([{ path: runtimeFile, type: "regular" }]);
                let launcher = await prepareWasmLauncher(request.file);
                lastLauncher = launcher;
                await launcher(request.argument);
            });
            break;

        case "WASM_GC":
            processRequest(event, async () => {
                const runtimeFile = request.file.path + "-runtime.js";
                const runtimeFileObj = { path: runtimeFile, type: "regular" };
                const files = request.additionalFiles ? [...request.additionalFiles, runtimeFileObj] : [runtimeFileObj];
                await appendFiles(files);
                let launcher = await prepareWasmGCLauncher(request.file);
                lastLauncher = launcher;
                await launcher(request.argument);
            });
            break;

        case "REPEAT":
            processRequest(event, async () => {
                await lastLauncher(request.argument);
            });
            break;
    }
});

function processRequest(event, processor) {
    (async function() {
        try {
            await processor();
            event.source.postMessage(wrapResponse({ status: "OK" }), "*");
        } catch (e) {
            event.source.postMessage(
                wrapResponse({ status: "failed", errorMessage: e.message + "\n" + e.stack }),
                "*"
            );
        }
    })();
}

async function appendFiles(files) {
    for (const file of files) {
        if (file.type === "module") {
            const module = await import("./" + file.path);
            window.main = module.main;
        } else {
            let script = document.createElement("script");
            let promise = new Promise((resolve, reject) => {
                script.onload = () => {
                    resolve();
                };
                script.onerror = () => {
                    reject(new Error("failed to load script " + file.path));
                };
            })
            script.src = file.path;
            document.body.appendChild(script);
            await promise;
        }
    }
}

function prepareJsLauncher() {
    return async argument => {
        await new Promise((resolve, reject) => {
            let m = typeof main === "undefined" ? window.main : main;
            m(argument ? [argument] : [], result => {
                if (result instanceof Error) {
                    reject(new Error(buildErrorMessage(result)));
                } else {
                    resolve();
                }
            });
        });
    }
}

function buildErrorMessage(e) {
    if (typeof $rt_decodeStack === "function" && typeof teavmException == "string") {
        return teavmException;
    }
    let stack = "";
    let je = main.javaException ? main.javaException(e) : void 0;
    if (je && je.constructor.$meta) {
        stack = je.constructor.$meta.name + ": ";
        stack += je.getMessage();
        stack += "\n";
    }
    stack += e.stack;
    return stack;
}

async function prepareWasmLauncher(file) {
    let output = [];
    let outputBuffer = "";
    let outputBufferStderr = "";
    let resolveCallback = null;
    let rejectCallback = null;

    function putwchar(charCode) {
        if (charCode === 10) {
            switch (outputBuffer) {
                case "SUCCESS":
                    resolveCallback();
                    break;
                case "FAILURE":
                    rejectCallback(new Error(output.join("\n")));
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

    let loader = await TeaVM.wasm.loader(file.path);
    return async argument => {
        output = [];
        outputBuffer = "";
        outputBufferStderr = "";

        let teavm = await loader({
            installImports: function(o) {
                o.teavm.putwcharsOut = (chars, count) => putwchars(instance, chars, count);
                o.teavm.putwcharsErr = (chars, count) => putwcharsStderr(instance, chars, count);
                o.teavm.putwchar = putwchar;
            },
            errorCallback: function(err) {
                rejectCallback(err);
            }
        });
        instance = teavm.instance;

        let promise = new Promise((resolve, reject) => {
            resolveCallback = resolve;
            rejectCallback = reject;
        })
        teavm.main(argument ? [argument] : []);
        await promise;
    }
}

async function prepareWasmGCLauncher(file) {
    let outputBuffer = "";
    let outputBufferStderr = "";
    let resolveCallback = null;
    let rejectCallback = null;

    function putchar(charCode) {
        if (charCode === 10) {
            log.push({ message: outputBuffer, type: "stdout" });
            outputBuffer = "";
        } else {
            outputBuffer += String.fromCharCode(charCode);
        }
    }

    function putcharStderr(charCode) {
        if (charCode === 10) {
            log.push({ message: outputBufferStderr, type: "stderr" });
            outputBufferStderr = "";
        } else {
            outputBufferStderr += String.fromCharCode(charCode);
        }
    }

    let teavm = await TeaVM.wasmGC.load(file.path, {
        stackDeobfuscator: {
            enabled: true
        },
        installImports: function(o) {
            o.teavmConsole.putcharStdout = putchar;
            o.teavmConsole.putcharStderr = putcharStderr;
            o.teavmTest = {
                success() {
                    resolveCallback();
                },
                failure(message) {
                    rejectCallback(new Error(message));
                }
            };
        }
    });
    return async argument => {
        outputBuffer = "";
        outputBufferStderr = "";
        let promise = new Promise((resolve, reject) => {
            resolveCallback = resolve;
            rejectCallback = reject;
        });
        teavm.exports.main(argument ? [argument] : []);
        await promise;
    };
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