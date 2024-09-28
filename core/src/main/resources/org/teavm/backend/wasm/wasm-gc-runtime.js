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
    let getGlobalName = function(name) {
        return eval(name);
    }
    function defaults(imports) {
        let stderr = "";
        let stdout = "";
        let finalizationRegistry = new FinalizationRegistry(heldValue => {
            if (typeof exports.reportGarbageCollectedValue === "function") {
                exports.reportGarbageCollectedValue(heldValue)
            }
        });
        let stringFinalizationRegistry = new FinalizationRegistry(heldValue => {
            exports.reportGarbageCollectedString(heldValue);
        });
        imports.teavmDate = {
            currentTimeMillis() {
                return new Date().getTime();
            },
            dateToString(timestamp) {
                return stringToJava(new Date(timestamp).toString());
            },
            getYear(timestamp) {
                return new Date(timestamp).getFullYear();
            },
            setYear(timestamp, year) {
                let date = new Date(timestamp);
                date.setFullYear(year);
                return date.getTime();
            },
            getMonth(timestamp) {
                return new Date(timestamp).getMonth();
            },
            setMonth(timestamp, month) {
                let date = new Date(timestamp);
                date.setMonth(month);
                return date.getTime();
            },
            getDate(timestamp) {
                return new Date(timestamp).getDate();
            },
            setDate(timestamp, value) {
                let date = new Date(timestamp);
                date.setDate(value);
                return date.getTime();
            },
            create(year, month, date, hrs, min, sec) {
                return new Date(year, month, date, hrs, min, sec).getTime();
            },
            createFromUTC(year, month, date, hrs, min, sec) {
                return Date.UTC(year, month, date, hrs, min, sec);
            }
        };
        imports.teavmConsole = {
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
        };
        imports.teavm = {
            createWeakRef(value, heldValue) {
                let weakRef = new WeakRef(value);
                if (heldValue !== null) {
                    finalizationRegistry.register(value, heldValue)
                }
                return weakRef;
            },
            deref: weakRef => weakRef.deref(),
            createStringWeakRef(value, heldValue) {
                let weakRef = new WeakRef(value);
                stringFinalizationRegistry.register(value, heldValue)
                return weakRef;
            },
            stringDeref: weakRef => weakRef.deref()
        };
        function identity(value) {
            return value;
        }
        imports.teavmJso = {
            emptyString: () => "",
            stringFromCharCode: code => String.fromCharCode(code),
            concatStrings: (a, b) => a + b,
            stringLength: s => s.length,
            charAt: (s, index) => s.charCodeAt(index),
            emptyArray: () => [],
            appendToArray: (array, e) => array.push(e),
            unwrapBoolean: value => value ? 1 : 0,
            wrapBoolean: value => !!value,
            getProperty: (obj, prop) => obj[prop],
            getPropertyPure: (obj, prop) => obj[prop],
            setProperty: (obj, prop, value) => obj[prop] = value,
            setPropertyPure: (obj, prop) => obj[prop] = value,
            global: getGlobalName
        };
        for (let name of ["wrapByte", "wrapShort", "wrapChar", "wrapInt", "wrapFloat", "wrapDouble", "unwrapByte",
                "unwrapShort", "unwrapChar", "unwrapInt", "unwrapFloat", "unwrapDouble"]) {
            imports.teavmJso[name] = identity;
        }
        for (let i = 0; i < 32; ++i) {
            imports.teavmJso["createFunction" + i] = function() {
                return new Function(...arguments);
            };
            imports.teavmJso["callFunction" + i] = function(fn, ...args) {
                return fn(...args);
            };
            imports.teavmJso["callMethod" + i] = function(instance, method, ...args) {
                return instance[method](...args);
            };
            imports.teavmJso["construct" + i] = function(constructor, ...args) {
                return new constructor(...args);
            };
        }
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
