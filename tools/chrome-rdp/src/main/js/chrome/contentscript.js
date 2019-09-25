/*
 *  Copyright 2018 Alexey Andreev.
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

let connected = false;
window.addEventListener("message", (event) => {
    if (event.source !== window) {
        return;
    }

    const data = event.data;
    if (typeof data.teavmDebugger !== "undefined" && !connected) {
        connected = true;
        chrome.runtime.sendMessage({ command: "debug", port: data.teavmDebugger.port });
    }
}, false);
window.postMessage({ teavmDebuggerRequest: true }, "*");