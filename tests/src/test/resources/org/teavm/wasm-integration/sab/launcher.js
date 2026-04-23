/*
 *  Copyright 2026 Alexey Andreev.
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

console.log("main installed");
this.main = function(args, callback) {
    let worker = new Worker("/resources/org/teavm/wasm-integration/sab/worker.js", {
        type: "module"
    });
    let buffer;
    let result = "";
    worker.onmessage = async e => {
        switch (e.data.type) {
            case "init":
                buffer = e.data.buffer;
                break
            case "update":
                // memory fence
                Atomics.load(buffer, 0);
                result += buffer[1] + ";"
                console.log(result);
                break;
            case "done":
                if (result !== "23;24;25;26;27;") {
                    console.log("Reject: " + result);
                    callback(new Error("Expected '23;24;25;26;27;' but got '" + result + "'"));
                } else {
                    console.log("Resolve");
                    callback();
                }
                break;
        }
    };
}