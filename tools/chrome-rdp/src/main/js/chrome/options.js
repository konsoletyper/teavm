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

function saveOptions() {
    const port = document.getElementById("port").value;
    chrome.storage.sync.set({
        port: port !== "" ? parseInt(port) : 2357,
    });
}

function loadOptions() {
    chrome.storage.sync.get({
        port: 2357,
    }, (items) => {
        document.getElementById("port").value = items.port;
    });
}
document.addEventListener("DOMContentLoaded", loadOptions);
document.getElementById("save").addEventListener('click', saveOptions);