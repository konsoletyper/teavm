/*
 *  Copyright 2024 Alexey Andreev.
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

var TeaVM = TeaVM || {};
TeaVM.wasm = function() {
    let exports;
    function defaults(imports) {
        let stderr = "";
        let stdout = "";
        let finalizationRegistry = new FinalizationRegistry(heldValue => {
            if (typeof exports.reportGarbageCollectedValue === "function") {
                exports.reportGarbageCollectedValue(heldValue)
            }
        });
        imports.teavm = {
            putcharStderr(c) {
                if (c === 10) {
                    console.error(stderr);
                    stderr = "";
                } else {
                    stderr += String.fromCharCode(c);
                }
            },
            putcharStdout(c) {
                if (c === 10) {
                    console.log(stdout);
                    stdout = "";
                } else {
                    stdout += String.fromCharCode(c);
                }
            },
            currentTimeMillis() {
                return new Date().getTime();
            },
            dateToString(timestamp) {
                return stringToJava(new Date(timestamp).toString());
            },
            createWeakRef(value, heldValue) {
                let weakRef = new WeakRef(value);
                if (heldValue !== null) {
                    finalizationRegistry.register(value, heldValue)
                }
                return weakRef;
            },
            deref(weakRef) {
                return weakRef.deref();
            }
        };
        imports.teavmMath = Math;
    }

    function load(path, options) {
        if (!options) {
            options = {};
        }

        const importObj = {};
        defaults(importObj);
        if (typeof options.installImports !== "undefined") {
            options.installImports(importObj);
        }

        return WebAssembly.instantiateStreaming(fetch(path), importObj).then((obj => {
            let teavm = {};
            teavm.main = createMain(obj.instance);
            teavm.instance = obj.instance;
            return teavm;
        }));
    }

    function stringToJava(str) {
        let sb = exports.createStringBuilder();
        for (let i = 0; i < str.length; ++i) {
            exports.appendChar(sb, str.charCodeAt(i));
        }
        return exports.buildString(sb);
    }

    function createMain(instance) {
        return args => {
            if (typeof args === "undefined") {
                args = [];
            }
            return new Promise((resolve, reject) => {
                exports = instance.exports;
                let javaArgs = exports.createStringArray(args.length);
                for (let i = 0; i < args.length; ++i) {
                    exports.setToStringArray(javaArgs, i, stringToJava(args[i]));
                }
                try {
                    exports.main(javaArgs);
                } catch (e) {
                    reject(e);
                }
            });
        }
    }

    return { load };
}();
