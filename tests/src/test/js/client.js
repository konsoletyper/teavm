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

function tryConnect() {
    let ws = new WebSocket("ws://localhost:9090");

    ws.onopen = () => {
        console.log("Connection established");
        listen(ws);
    };

    ws.onclose = () => {
        ws.close();
        setTimeout(() => {
            tryConnect();
        }, 500);
    };

    ws.onerror = err => {
        console.log("Could not connect WebSocket", err);
    }
}

function listen(ws) {
    ws.onmessage = (event) => {
        let request = JSON.parse(event.data);
        console.log("Request #" + request.id + " received");
        runTests(ws, request.id, request.tests, 0);
    }
}

function runTests(ws, suiteId, tests, index) {
    if (index === tests.length) {
        return;
    }
    let test = tests[index];
    runSingleTest(test, result => {
        console.log("Sending response #" + suiteId);
        ws.send(JSON.stringify({
            id: suiteId,
            index: index,
            result: result
        }));
        runTests(ws, suiteId, tests, index + 1);
    });
}

function runSingleTest(test, callback) {
    console.log("Running test " + test.name + " consisting of " + test.files);
    let iframe = document.createElement("iframe");
    document.body.appendChild(iframe);
    let handshakeListener = () => {
        window.removeEventListener("message", handshakeListener);

        let listener = event => {
            window.removeEventListener("message", listener);
            document.body.removeChild(iframe);
            callback(event.data);
        };
        window.addEventListener("message", listener);

        iframe.contentWindow.postMessage(test, "*");
    };
    window.addEventListener("message", handshakeListener);
    iframe.src = "about:blank";
    iframe.src = "frame.html";
}

tryConnect();