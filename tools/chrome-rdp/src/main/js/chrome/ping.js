/*
 *  Copyright 2022 Alexey Andreev.
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

function sendPing() {
    chrome.runtime.sendMessage({ command: "ping" });
}
function listener(message) {
    if (message.command === "pong") {
        setTimeout(sendPing, 2000);
    } else if (message.command === "quitPingPong") {
        chrome.runtime.onMessage.removeListener(listener);
    }
}
chrome.runtime.onMessage.addListener(listener);
sendPing();