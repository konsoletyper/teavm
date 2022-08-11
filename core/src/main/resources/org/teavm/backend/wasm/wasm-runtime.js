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

const fdImport = import("./node_modules/@bjorn3/browser_wasi_shim/src/fd.js");
const fsFdImport = import("./node_modules/@bjorn3/browser_wasi_shim/src/fs_fd.js");
const wasiImport = import("./node_modules/@bjorn3/browser_wasi_shim/src/wasi.js");

var TeaVM = TeaVM || {};
TeaVM.wasm = function() {
    class JavaError extends Error {
        constructor(message) {
            super(message);
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
    function logString(string, controller) {
        let instance = controller.instance;
        let memory = instance.exports.memory.buffer;
        let arrayPtr = instance.exports.teavm_stringData(string);
        let length = instance.exports.teavm_arrayLength(arrayPtr);
        let arrayData = new DataView(memory, instance.exports.teavm_charArrayData(arrayPtr), length * 2);
        for (let i = 0; i < length; ++i) {
            putwchar(arrayData.memory.getUint16(i * 2, true));
        }
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
            controller.timer = setTimeout(() => process(controller), result)
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
            putwchar: putwchar,
            towlower: towlower,
            towupper: towupper,
            getNativeOffset: getNativeOffset,
            teavm_interrupt: () => interrupt(controller)
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
        let xhr = new XMLHttpRequest();
        xhr.responseType = "arraybuffer";
        xhr.open("GET", path);

        return new Promise((resolve, reject) => {
            xhr.onload = () => {
                let response = xhr.response;
                if (!response) {
                    reject("Error loading Wasm data")
                    return;
                }

                resolve(response);
            };
            xhr.send();
        }).then(data => create(data, options));
    }

    async function create(data, options) {
        if (!options) {
            options = {};
        }

        const importObj = {};
        const controller = defaults(importObj);
        if (typeof options.installImports !== "undefined") {
            options.installImports(importObj);
        }

        let args = options.args || [];
        // WASI expects the first argument to be the executable name, so we prepend a dummy arg here:
        args = args.slice();
        args.unshift("");

        const env = [];

        const { Fd } = await fdImport;
        const { PreopenDirectory } = await fsFdImport;
        const { default: WASI } = await wasiImport;

        class LogFd extends Fd {
            constructor(putwchar) {
                super();
                this.putwchar = putwchar;
            }

            fd_write(view8, iovs) {
                let nwritten = 0;
                let decoder = new TextDecoder("utf-8");
                for (let iovec of iovs) {
                    let string = decoder.decode(view8.slice(iovec.buf, iovec.buf + iovec.buf_len));
                    for (let i = 0; i < string.length; ++i) {
                        this.putwchar(string.charCodeAt(i));
                    }
                    nwritten += iovec.buf_len;
                }
                return { ret: 0, nwritten };
            }
        }

        const fds = [
            new Fd(),
            new LogFd(importObj.teavm.putwchar),
            new LogFd(importObj.teavm.putwchar),
            new PreopenDirectory("/", {})
        ];

        const wasi = new WASI(args, env, fds);

        const imports = {
            ...{ "wasi_snapshot_preview1": wasi.wasiImport },
            ...importObj
        };

        const { instance } = await WebAssembly.instantiate(data, imports);

        controller.instance = instance;
        let teavm = createTeaVM(instance);
        teavm.main = createMain(teavm, controller, wasi);
        return teavm;
    }

    function createMain(teavm, controller, wasi) {
        return function() {
            return new Promise((resolve, reject) => {
                controller.resolve = resolve;
                controller.reject = reject;
                wrapExport(() => wasi.start(teavm.instance), teavm.instance)();
                process(controller);
            });
        }
    }

    return { JavaError, load, create };
}();
