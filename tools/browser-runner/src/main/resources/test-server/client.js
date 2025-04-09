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
        switch (request.command) {
            case "run":
                if (logging) {
                    console.log("Request #" + request.id + " received");
                }
                runTests(ws, request.id, request.tests, 0);
                break;
            case "cleanup":
                cleanup();
                break;
        }
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

function cleanup() {
    console.log("Cleanup: ", frames);
    for (let key of Object.keys(frames)) {
        let frame = frames[key];
        document.body.removeChild(frame);
    }
    frames = {}
    deobfuscators = {};
}

let deobfuscators = {};

function runSingleTest(test, callback) {
    if (logging) {
        console.log("Running test " + test.name);
    }
    if (deobfuscation) {
        const fileName = test.file.path + ".teavmdbg";
        if (test.cached && fileName in deobfuscators) {
            let deobfuscator = deobfuscators[fileName];
            if (typeof deobfuscator.value === "undefined") {
                runSingleTestWithDeobfuscator(test, deobfuscator.value, callback);
            } else {
                deobfuscator.promise.then(value => {
                    runSingleTestWithDeobfuscator(test, value, callback);
                });
            }
        } else {
            const xhr = new XMLHttpRequest();
            xhr.responseType = "arraybuffer";
            let deobfuscator = {};
            deobfuscator.promise = new Promise(resolve => {
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === 4) {
                        deobfuscator.value = xhr.status === 200
                            ? createDeobfuscator(xhr.response, "http://localhost:{{PORT}}/" + test.file.path)
                            : null;
                        resolve(deobfuscator.value);
                        runSingleTestWithDeobfuscator(test, deobfuscator.value, callback);
                    }
                }
                xhr.open("GET", fileName);
                xhr.send();
            });
            if (test.cached) {
                deobfuscators[fileName] = deobfuscator;
            }
        }
    } else {
        runSingleTestWithDeobfuscator(test, null, callback);
    }
}

let frames = {};

function runSingleTestWithDeobfuscator(test, deobfuscator, callback) {
    let key;
    let frame;
    let reused = true;
    if (test.cached) {
        key = JSON.stringify({
            type: test.type,
            file: test.file,
            additionalFiles: test.additionalFiles
        });
        frame = frames[key];
    }
    if (!frame) {
        reused = false;
        frame = document.createElement("iframe");
        document.body.appendChild(frame);
        if (test.cached) {
            frames[key] = frame;
        }
    }

    let listener = event => {
        if (event.source !== frame.contentWindow) {
            return;
        }
        window.removeEventListener("message", listener);
        if (!test.cached) {
            document.body.removeChild(frame);
        }
        callback(event.data);
    };

    if (reused) {
        window.addEventListener("message", listener);
        frame.contentWindow.postMessage({ type: "REPEAT", argument: test.argument }, "*");
        return;
    }

    let handshakeListener = handshakeEvent => {
        if (handshakeEvent.source !== frame.contentWindow || handshakeEvent.data !== "ready") {
            return;
        }
        window.removeEventListener("message", handshakeListener);

        window.addEventListener("message", listener);

        frame.contentWindow.$rt_decodeStack = deobfuscator != null
            ? deobfuscator.deobfuscate.bind(deobfuscator)
            : null;
        frame.contentWindow.postMessage(test, "*");
    };
    window.addEventListener("message", handshakeListener);
    frame.src = "about:blank";
    frame.src = "frame.html";
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
