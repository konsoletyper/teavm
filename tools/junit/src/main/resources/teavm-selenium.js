/*
 *  Copyright 2016 Alexey Andreev.
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

var runtimeSource = arguments[0];
var testSource = arguments[1];
var adapterSource = arguments[2];
var seleniumCallback = arguments[arguments.length - 1];

var iframe = document.createElement("iframe");
document.body.appendChild(iframe);
var doc = iframe.contentDocument;

window.jsErrors = [];
window.onerror = reportError;
iframe.contentWindow.onerror = reportError;

loadScripts([ runtimeSource, testSource, adapterSource ]);
window.addEventListener("message", handleMessage);

function handleMessage(event) {
    window.removeEventListener("message", handleMessage);
    document.body.removeChild(iframe);
    seleniumCallback(event.data)
}

function loadScripts(scripts) {
    for (var i = 0; i < scripts.length; ++i) {
        var elem = doc.createElement("script");
        elem.type = "text/javascript";
        doc.head.appendChild(elem);
        elem.text = scripts[i];
    }
}
function reportError(error, url, line) {
    window.jsErrors.push(error + " at " + line)
}
function report(error) {
    window.jsErrors.push(error)
}
function globalEval(window, arg) {
    eval.apply(window, [arg])
}