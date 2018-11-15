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

main([], function(result) {
    var message = {};
    if (result instanceof Error) {
        makeErrorMessage(message, result);
    } else {
        message.status = "ok";
    }
    window.parent.postMessage(JSON.stringify(message), "*");
});

function makeErrorMessage(message, e) {
    message.status = "exception";
    var stack = "";
    if (e.$javaException && e.$javaException.constructor.$meta) {
        stack = e.$javaException.constructor.$meta.name + ": ";
        stack += e.$javaException.getMessage() || "";
        stack += "\n";
    }
    message.stack = stack + e.stack;
}