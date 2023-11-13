/*
 *  Copyright 2023 Alexey Andreev.
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

importScripts("wasm/software3d.wasm-runtime.js");

let instance = null;
let pendingInstanceFunctions = [];

addEventListener("message", e => {
    let data = e.data
    switch (data.type) {
        case "init":
            pendingInstanceFunctions.push(() => {
                instance.exports.initWorker(data.width, data.height, data.step, data.offset)
            });
            runPendingFunctions();
            break;
        case "frame":
            pendingInstanceFunctions.push(() => {
                instance.exports.renderFrame(data.time);
            });
            runPendingFunctions();
            break;
        case "stop":
            self.close();
            break;
    }
});

TeaVM.wasm.load("wasm/software3d.wasm", {
    installImports(o, controller) {
        o.renderer = {
            result(data, size, time) {
                let buffer = controller.instance.exports.memory.buffer.slice(data, data + size);
                self.postMessage({ data: buffer, time: time });
            }
        }
    },
}).then(teavm => {
    teavm.main([]);
    instance = teavm.instance;
    runPendingFunctions();
});


function runPendingFunctions() {
    if (instance === null) {
        return;
    }
    for (let f of pendingInstanceFunctions) {
        f();
    }
    pendingInstanceFunctions = [];
}