/*
 *  Copyright 2019 Alexey Andreev.
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

import { CDP } from "./cdp.js";

function waitForTest(cdp) {
    return new Promise(resolve => {
        let stage = 'initial';
        let requestId = null;
        cdp.on("Runtime.consoleAPICalled", event => {
            const value = event.args.filter(arg => arg.type === "string").map(arg => arg.value).join("");
            switch (stage) {
                case 'initial':
                    if (value === 'Connection established') {
                        stage = 'connected';
                    }
                    break;
                case 'connected': {
                    const result = /Request #([0-9]+) received/.exec(value);
                    if (result) {
                        requestId = result[1];
                        stage = 'sent';
                    }
                    break;
                }
                case 'sent':
                    if (value === "Running test only simple consisting of http://localhost:9090//only/test.js") {
                        stage = 'ran';
                    }
                    break;
                case 'ran':
                    if (value === "Sending response #" + requestId) {
                        stage = 'received';
                        resolve();
                    }
                    break;
            }
        });
    });
}

function timeout(time) {
    let timer;
    return {
        promise: new Promise((resolve, reject) => {
            timer = setTimeout(() => { reject(new Error("Timeout expired")); }, time);
        }),
        cancel: () => clearTimeout(timer)
    }
}

async function run() {
    const wsConn = await CDP.connect("http://localhost:9222");
    const cdp = new CDP(wsConn);
    cdp.on("Runtime.consoleAPICalled", event => {
        const value = event.args.filter(arg => arg.type === "string").map(arg => arg.value).join("");
        if (value !== "") {
            console.log("[LOG] " + new Date(event.timestamp) + ": " + value);
        }
    });

    const wait = waitForTest(cdp);
    let timer;
    cdp.start();

    try {
        await cdp.call("Runtime.enable");
        timer = timeout(10000);
        await Promise.race([wait, timer.promise]);
    } finally {
        if (timer) {
            timer.cancel();
        }
        wsConn.close();
    }
}

run().catch(e => {
    console.error("Error", e);
});
