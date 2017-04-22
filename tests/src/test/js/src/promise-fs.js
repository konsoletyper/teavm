/*
 *  Copyright 2017 Alexey Andreev.
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

import * as fs from "fs";

function wrapFsFunction(fsFunction) {
    return function() {
        const self = this;
        const args = Array.prototype.slice.call(arguments);
        return new Promise((resolve, reject) => {
            args.push((error, result) => {
                if (!error) {
                    resolve(result);
                } else {
                    reject(error);
                }
            });
            fsFunction.apply(self, args);
        })
    }
}

export let
    readdir = wrapFsFunction(fs.readdir),
    readFile = wrapFsFunction(fs.readFile),
    stat = wrapFsFunction(fs.stat),
    open = wrapFsFunction(fs.open);