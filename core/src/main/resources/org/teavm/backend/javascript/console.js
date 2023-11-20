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
"use strict";

let $rt_createOutputFunction = outputFunction => {
    let buffer = "";
    return msg => {
        let index = 0;
        while (true) {
            let next = msg.indexOf('\n', index);
            if (next < 0) {
                break;
            }
            outputFunction(buffer + msg.substring(index, next));
            buffer = "";
            index = next + 1;
        }
        buffer += msg.substring(index);
    }
}

let $rt_putStdout = typeof teavm_globals.$rt_putStdoutCustom === "function"
    ? teavm_globals.$rt_putStdoutCustom
    : typeof teavm_globals.console === "object"
        ? $rt_createOutputFunction(msg => teavm_globals.console.info(msg)) : () => {};
let $rt_putStderr = typeof teavm_globals.$rt_putStderrCustom === "function"
    ? teavm_globals.$rt_putStderrCustom
    : typeof teavm_globals.console === "object"
        ? $rt_createOutputFunction(msg => teavm_globals.console.error(msg)) : () => {};
