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

async function run() {
    const wsConn = await CDP.connect("http://localhost:9222");
    const cdp = new CDP(wsConn);
    const waitForLog = new Promise(resolve => {
        let timeout = setTimeout(resolve, 500);
        cdp.on("Runtime.consoleAPICalled", event => {
            const value = event.args.filter(arg => arg.type === "string").map(arg => arg.value).join("");
            if (value !== "") {
                console.log("[LOG] " + new Date(event.timestamp) + ": " + value);
            }
            clearTimeout(timeout);
            timeout = setTimeout(resolve, 500);
        });
    });

    cdp.start();

    try {
        await cdp.call("Runtime.enable");
        await waitForLog;
    } finally {
        wsConn.close();
    }
}

run().catch(e => {
    console.error("Error", e);
});
