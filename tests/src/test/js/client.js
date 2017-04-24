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
        console.log("Connected established");
        listen(ws);
    };

    ws.onclose = () => {
        ws.close();
        setTimeout(() => {
            tryConnect();
        }, 500);
    };
}

function listen(ws) {
    ws.onmessage = (event) => {
        let resultConsumer = [];
        let request = JSON.parse(event.data);
        console.log("Request #" + request.id + " received");
        runTests(request.tests, resultConsumer, 0, () => {
            console.log("Sending response #" + request.id);
            ws.send(JSON.stringify({
                id: request.id,
                result: resultConsumer
            }));
        });
    }
}

function runTests(tests, consumer, index, callback) {
    if (index === tests.length) {
        callback();
    } else {
        let test = tests[index];
        runSingleTest(test, result => {
            consumer.push(result);
            runTests(tests, consumer, index + 1, callback);
        });
    }
}

function runSingleTest(test, callback) {
    console.log("Running test " + test.name + " consisting of " + test.files);
    let iframe = document.getElementById("test");
    let handshakeListener = () => {
        window.removeEventListener("message", handshakeListener);

        let listener = event => {
            window.removeEventListener("message", listener);
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