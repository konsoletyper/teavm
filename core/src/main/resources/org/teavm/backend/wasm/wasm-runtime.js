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

var TeaVM = TeaVM || {};
TeaVM.wasm = function() {
    class JavaError extends Error {
        constructor(message) {
            super(message)
        }
    }

    let lineBuffer = "";
    function putwchar(charCode) {
        if (charCode === 10) {
            console.log(lineBuffer);
            lineBuffer = "";
        } else {
            lineBuffer += String.fromCharCode(charCode);
        }
    }
    function towlower(code) {
        return String.fromCharCode(code).toLowerCase().charCodeAt(0);
    }
    function towupper(code) {
        return String.fromCharCode(code).toUpperCase().charCodeAt(0);
    }
    function currentTimeMillis() {
        return new Date().getTime();
    }
    function getNativeOffset(instant) {
        return new Date(instant).getTimezoneOffset();
    }
    function logString(string) {
        var memory = new DataView(logString.memory.buffer);
        var arrayPtr = memory.getUint32(string + 8, true);
        var length = memory.getUint32(arrayPtr + 8, true);
        for (var i = 0; i < length; ++i) {
            putwchar(memory.getUint16(i * 2 + arrayPtr + 12, true));
        }
    }
    function logInt(i) {
        lineBuffer += i.toString();
    }

    function importDefaults(obj) {
        obj.teavm = {
            currentTimeMillis: currentTimeMillis,
            nanoTime: function() { return performance.now(); },
            isnan: isNaN,
            teavm_getNaN: function() { return NaN; },
            isinf: function(n) { return !isFinite(n) },
            isfinite: isFinite,
            putwchar: putwchar,
            towlower: towlower,
            towupper: towupper,
            getNativeOffset: getNativeOffset,
            logString: logString,
            logInt: logInt,
            logOutOfMemory: function() { console.log("Out of memory") }
        };

        obj.teavmMath = Math;

        obj.teavmHeapTrace = {
            allocate: function(address, size) {},
            free: function(address, size) {},
            assertFree: function(address, size) {},
            markStarted: function() {},
            mark: function(address) {},
            reportDirtyRegion: function(address) {},
            markCompleted: function() {},
            move: function(from, to, size) {},
            gcStarted: function(full) {},
            sweepStarted: function() {},
            sweepCompleted: function() {},
            defragStarted: function() {},
            defragCompleted: function() {},
            gcCompleted: function() {},
            init: function(maxHeap) {}
        };
    }

    function createTeaVM(instance) {
        let teavm = {
            memory: instance.exports.memory,
            instance,
            catchException: instance.exports.teavm_catchException
        }

        for (const name of ["allocateString", "stringData", "allocateObjectArray", "allocateStringArray",
            "allocateByteArray", "allocateShortArray", "allocateCharArray", "allocateIntArray",
            "allocateLongArray", "allocateFloatArray", "allocateDoubleArray",
            "objectArrayData", "byteArrayData", "shortArrayData", "charArrayData", "intArrayData",
            "longArrayData", "floatArrayData", "doubleArrayData", "arrayLength"]) {
            teavm[name] = wrapExport(instance.exports["teavm_" + name], instance);
        }

        teavm.main = createMain(teavm, instance.exports.main);

        return teavm;
    }

    function wrapExport(fn, instance) {
        return function() {
            let result = fn.apply(this, arguments);
            let ex = instance.exports.teavm_catchException();
            if (ex !== 0) {
                throw new JavaError("Uncaught exception occurred in java");
            }
            return result;
        }
    }

    function load(path, options) {
        if (!options) {
            options = {};
        }

        let callback = typeof options.callback !== "undefined" ? options.callback : function() {};
        let errorCallback = typeof options.errorCallback !== "undefined" ? options.errorCallback : function() {};

        let importObj = {};
        importDefaults(importObj);
        if (typeof options.installImports !== "undefined") {
            options.installImports(importObj);
        }

        let xhr = new XMLHttpRequest();
        xhr.responseType = "arraybuffer";
        xhr.open("GET", path);

        return new Promise((resolve, reject) => {
            xhr.onload = () => {
                let response = xhr.response;
                if (!response) {
                    return;
                }

                WebAssembly.instantiate(response, importObj).then(resultObject => {
                    importObj.teavm.logString.memory = resultObject.instance.exports.memory;
                    let teavm = createTeaVM(resultObject.instance);
                    teavm.main = createMain(teavm, wrapExport, resultObject.instance.exports.main);
                    resolve(teavm);
                }).catch(error => {
                    reject(error);
                });
            };
            xhr.send();
        });
    }

    function createMain(teavm, mainFunction) {
        return function(args) {
            if (typeof args === "undefined") {
                args = [];
            }
            return new Promise(resolve => {
                let javaArgs = teavm.allocateStringArray(mainArgs.length);
                let javaArgsData = new Uint32Array(teavm.memory, teavm.objectArrayData(javaArgs), args.length);
                for (let i = 0; i < mainArgs.length; ++i) {
                    let arg = args[i];
                    let javaArg = teavm.allocateString(arg.length);
                    let javaArgAddress = teavm.objectArrayData(teavm.stringData(javaArg));
                    let javaArgData = new Uint16Array(teavm.memory, javaArgAddress, arg.length);
                    for (let j = 0; j < arg.length; ++j) {
                        javaArgData[j] = arg.charCodeAt(j);
                    }
                    javaArgsData[i] = javaArg;
                }

                resolve(wrapExport(mainFunction, teavm.instance)(javaArgs));
            });
        }
    }

    return { JavaError, importDefaults, load, wrapExport, createTeaVM, createMain };
}();
