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

import * as http from "http";
import { client as WebSocketClient } from "websocket";

function httpGetJson(url) {
    return new Promise((resolve, reject) => {
        http.request(url, resp => {
            if (resp.statusCode !== 200) {
                reject(new Error(`HTTP status: ${resp.statusCode}`));
                return;
            }
            let data = "";
            resp.on('data', (chunk) => {
                data += chunk;
            });
            resp.on('end', () => {
                resolve(JSON.parse(data));
            });
        }).on("error", err => {
            reject(new Error(`HTTP error: ${err.message}`));
        }).end();
    });
}

function connectWs(url) {
    return new Promise((resolve, reject) => {
        const client = new WebSocketClient();
        client.on("connectFailed", error => reject(new Error('Connect Error: ' + error)));
        client.on("connect", resolve);
        client.connect(url);
    });
}

class CDP {
    constructor(conn) {
        this.idGenerator = 1;
        this.pendingCalls = Object.create(null);
        this.eventHandlers = Object.create(null);
        this.conn = conn;
    }

    start() {
        this.conn.on("message", message => {
            if (message.type === 'utf8') {
                const messageObj = JSON.parse(message.utf8Data);
                if (messageObj.id !== void 0) {
                    const pendingCall = this.pendingCalls[messageObj.id];
                    delete this.pendingCalls[messageObj.id];
                    if (messageObj.error) {
                        pendingCall.reject(new Error(`Error calling CDP method ${messageObj.error}`));
                    } else {
                        pendingCall.resolve(messageObj.result);
                    }
                } else {
                    const handlers = this.eventHandlers[messageObj.method];
                    if (handlers) {
                        for (const handler of handlers) {
                            handler(messageObj.params);
                        }
                    }
                }
            }
        });
        this.conn.on("close", () => {
            for (const key in Object.getOwnPropertyNames(this.pendingCalls)) {
                this.pendingCalls[key].reject(new Error("Connection closed before result received"));
            }
        });
        this.conn.on("error", err => {
            console.error("WS error: %j", err);
        });
    }

    call(method, params = undefined) {
        return new Promise((resolve, reject) => {
            const id = this.idGenerator++;
            this.pendingCalls[id] = { resolve, reject };
            this.conn.send(JSON.stringify({ id, method, params }));
        });
    }

    async on(eventName, handler) {
        let handlers = this.eventHandlers[eventName];
        if (handlers === void 0) {
            handlers = [];
            this.eventHandlers[eventName] = handlers;
        }
        handlers.push(handler);
    }
}

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
    const targets = await httpGetJson("http://localhost:9222/json/list");
    const wsUrl = targets.find(target => target.type === "page").webSocketDebuggerUrl;
    console.log("Connected to Chrome");
    const wsConn = await connectWs(wsUrl);
    console.log(`Connected to WS endpoint: ${wsUrl}`);
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
