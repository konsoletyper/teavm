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

let logging = false;
let deobfuscation = false;

function tryConnect() {
    let ws = new WebSocket("ws://localhost:{{PORT}}/ws");

    ws.onopen = () => {
        if (logging) {
            console.log("Connection established");
        }
        listen(ws);
    };

    ws.onclose = () => {
        ws.close();
        setTimeout(() => {
            tryConnect();
        }, 500);
    };

    ws.onerror = err => {
        if (logging) {
            console.log("Could not connect WebSocket", err);
        }
    }
}

function listen(ws) {
    ws.onmessage = (event) => {
        let request = JSON.parse(event.data);
        if (logging) {
            console.log("Request #" + request.id + " received");
        }
        runTests(ws, request.id, request.tests, 0);
    }
}

function runTests(ws, suiteId, tests, index) {
    if (index === tests.length) {
        return;
    }
    let test = tests[index];
    runSingleTest(test, result => {
        if (logging) {
            console.log("Sending response #" + suiteId);
        }
        ws.send(JSON.stringify({
            id: suiteId,
            index: index,
            result: result
        }));
        runTests(ws, suiteId, tests, index + 1);
    });
}

let lastDeobfuscator = null;
let lastDeobfuscatorFile = null;
let lastDeobfuscatorPromise = null;
function runSingleTest(test, callback) {
    if (logging) {
        console.log("Running test " + test.name);
    }
    if (deobfuscation) {
        const fileName = test.file.path + ".teavmdbg";
        if (lastDeobfuscatorFile === fileName) {
            if (lastDeobfuscatorPromise === null) {
                runSingleTestWithDeobfuscator(test, lastDeobfuscator, callback);
            } else {
                lastDeobfuscatorPromise.then(value => {
                    runSingleTestWithDeobfuscator(test, value, callback);
                })
            }
        } else {
            lastDeobfuscatorFile = fileName;
            lastDeobfuscator = null;
            const xhr = new XMLHttpRequest();
            xhr.responseType = "arraybuffer";
            lastDeobfuscatorPromise = new Promise(resolve => {
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === 4) {
                        const newDeobfuscator = xhr.status === 200
                            ? createDeobfuscator(xhr.response, "http://localhost:{{PORT}}/" + test.file.path)
                            : null;
                        if (lastDeobfuscatorFile === fileName) {
                            lastDeobfuscator = newDeobfuscator;
                            lastDeobfuscatorPromise = null;
                        }
                        resolve(newDeobfuscator);
                        runSingleTestWithDeobfuscator(test, newDeobfuscator, callback);
                    }
                }
                xhr.open("GET", fileName);
                xhr.send();
            });

        }
    } else {
        runSingleTestWithDeobfuscator(test, null, callback);
    }
}

function runSingleTestWithDeobfuscator(test, deobfuscator, callback) {
    let listener = event => {
        if (event.source !== lastIFrame.contentWindow) {
            return;
        }
        window.removeEventListener("message", listener);
        callback(event.data);
    };

    if (test.type === lastRequestType
            && filesEqual(test.file, lastFile)
            && arraysEqual(lastAdditionalFiles, test.additionalFiles, filesEqual)) {
        console.log("Reusing last launcher");
        window.addEventListener("message", listener);
        lastIFrame.contentWindow.postMessage({ type: "REPEAT", argument: test.argument }, "*");
        return;
    }

    if (lastIFrame !== null) {
        document.body.removeChild(lastIFrame);
    }
    console.log(test);
    lastRequestType = test.type;
    lastFile = test.file;
    lastAdditionalFiles = test.additionalFiles;

    let iframe = document.createElement("iframe");
    lastIFrame = iframe;
    document.body.appendChild(iframe);
    let handshakeListener = handshakeEvent => {
        if (handshakeEvent.source !== iframe.contentWindow || handshakeEvent.data !== "ready") {
            return;
        }
        window.removeEventListener("message", handshakeListener);

        window.addEventListener("message", listener);

        iframe.contentWindow.$rt_decodeStack = deobfuscator != null
            ? deobfuscator.deobfuscate.bind(deobfuscator)
            : null;
        iframe.contentWindow.postMessage(test, "*");
    };
    window.addEventListener("message", handshakeListener);
    iframe.src = "about:blank";
    iframe.src = "frame.html";
}

function arraysEqual(a, b) {
    if (!Array.isArray(a) || !Array.isArray(b)) {
        return a === b;
    }
    if (a.length !== b.length) {
        return false;
    }
    for (let i = 0; i < a.length; ++i) {
        if (a[i] !== b[i]) {
            return false;
        }
    }
    return true;
}

function filesEqual(a, b) {
    if (a === b) {
        return true;
    }
    if (typeof a !== "object" || typeof b !== "object") {
        return false;
    }
    return a.type === b.type && a.path === b.path;
}

let lastRequestType = null;
let lastAdditionalFiles = null;
let lastFiles = [];
let lastFile = null;
let lastIFrame = null;
let frameCallback = null;