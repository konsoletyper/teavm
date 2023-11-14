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
    function putwchars(controller, buffer, count) {
        let instance = controller.instance;
        let memory = new Int8Array(instance.exports.memory.buffer);
        for (let i = 0; i < count; ++i) {
            // TODO: support UTF-8
            putwchar(memory[buffer++]);
        }
    }
    function currentTimeMillis() {
        return new Date().getTime();
    }
    function getNativeOffset(instant) {
        return new Date(instant).getTimezoneOffset();
    }
    function logString(string, controller) {
        let instance = controller.instance;
        let memory = instance.exports.memory.buffer;
        let arrayPtr = instance.exports.teavm_stringData(string);
        let length = instance.exports.teavm_arrayLength(arrayPtr);
        let arrayData = new Uint16Array(memory, instance.exports.teavm_charArrayData(arrayPtr), length * 2);
        for (let i = 0; i < length; ++i) {
            putwchar(arrayData[i]);
        }
    }
    function dateToString(timestamp, controller) {
        const s = new Date(timestamp).toString();
        let instance = controller.instance;
        let result = instance.exports.teavm_allocateString(s.length);
        if (result === 0) {
            return 0;
        }
        let resultAddress = instance.exports.teavm_objectArrayData(instance.exports.teavm_stringData(result));
        let resultView = new Uint16Array(instance.exports.memory.buffer, resultAddress, s.length);
        for (let i = 0; i < s.length; ++i) {
            resultView[i] = s.charCodeAt(i);
        }
        return result;
    }
    function logInt(i) {
        lineBuffer += i.toString();
    }
    function interrupt(controller) {
        if (controller.timer !== null) {
            clearTimeout(controller.timer);
            controller.timer = null;
        }
        controller.timer = setTimeout(() => process(controller), 0);
    }
    function process(controller) {
        let result = controller.instance.exports.teavm_processQueue();
        if (!controller.complete) {
            if (controller.instance.exports.teavm_stopped()) {
                controller.complete = true;
                controller.resolve();
            }
        }
        if (result >= 0) {
            controller.timer = setTimeout(() => process(controller), Number(result))
        }
    }

    function defaults(obj) {
        let controller = {};
        controller.instance = null;
        controller.timer = null;
        controller.resolve = null;
        controller.reject = null;
        controller.complete = false;
        obj.teavm = {
            currentTimeMillis: currentTimeMillis,
            nanoTime: () => performance.now(),
            putwcharsOut: (chars, count) => putwchars(controller, chars, count),
            putwcharsErr: (chars, count) => putwchars(controller, chars, count),
            getNativeOffset: getNativeOffset,
            logString: string => logString(string, controller),
            logInt: logInt,
            logOutOfMemory: () => console.log("Out of memory"),
            teavm_interrupt: () => interrupt(controller),
            dateToString: (timestamp) => dateToString(timestamp, controller)
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

        return controller;
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
            let ex = catchException(instance);
            if (ex !== null) {
                throw ex;
            }
            return result;
        }
    }

    function catchException(instance) {
        let ex = instance.exports.teavm_catchException();
        if (ex !== 0) {
            return new JavaError("Uncaught exception occurred in Java");
        }
        return null;
    }

    function load(path, options) {
        if (!options) {
            options = {};
        }

        const importObj = {};
        const controller = defaults(importObj);
        if (typeof options.installImports !== "undefined") {
            options.installImports(importObj, controller);
        }

        return WebAssembly.instantiateStreaming(fetch(path), importObj).then((obj => {
            controller.instance = obj.instance;
            let teavm = createTeaVM(obj.instance);
            teavm.main = createMain(teavm, controller);
            return teavm;
        }));
    }

    function createMain(teavm, controller) {
        return function(args) {
            if (typeof args === "undefined") {
                args = [];
            }
            return new Promise((resolve, reject) => {
                let javaArgs = teavm.allocateStringArray(args.length);
                let javaArgsData = new Int32Array(teavm.memory.buffer, teavm.objectArrayData(javaArgs), args.length);
                for (let i = 0; i < args.length; ++i) {
                    let arg = args[i];
                    let javaArg = teavm.allocateString(arg.length);
                    let javaArgAddress = teavm.objectArrayData(teavm.stringData(javaArg));
                    let javaArgData = new Uint16Array(teavm.memory.buffer, javaArgAddress, arg.length);
                    for (let j = 0; j < arg.length; ++j) {
                        javaArgData[j] = arg.charCodeAt(j);
                    }
                    javaArgsData[i] = javaArg;
                }

                controller.resolve = resolve;
                controller.reject = reject;
                try {
                    wrapExport(teavm.instance.exports.start, teavm.instance)(javaArgs);
                } catch (e) {
                    reject(e);
                    return;
                }
                process(controller);
            });
        }
    }

    return { JavaError, load };
}();
