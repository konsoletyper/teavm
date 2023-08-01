/*
 *  Copyright 2023 konsoletyper.
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

let main;
const define = (function() {
    const modules = new Map();
    function def() {
        let index = 0;
        const moduleName = typeof arguments[index] === 'string' ? arguments[index++] : null;
        const deps = arguments[index++];
        const module = arguments[index++];

        const exports = Object.create(null);
        const args = [];
        for (const dep of deps) {
            if (dep === 'exports') {
                args.push(exports);
            } else {
                args.push(modules[dep]);
            }
        }
        let result = module.apply(this, args);
        if (typeof result === 'undefined') {
            result = exports;
        }
        if (moduleName !== null) {
            modules[moduleName] = result;
        } else {
            main = result.main;
        }
    }
    def.amd = {};
    return def;
})();