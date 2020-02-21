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

    function run(path, options) {
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

        xhr.onload = function() {
            let response = xhr.response;
            if (!response) {
                return;
            }

            WebAssembly.instantiate(response, importObj).then(function(resultObject) {
                importObj.teavm.logString.memory = resultObject.instance.exports.memory;
                resultObject.instance.exports.main();
                callback(resultObject);
            }).catch(function(error) {
                console.log("Error loading WebAssembly %o", error);
                errorCallback(error);
            });
        };
        xhr.send();
    }

    return { importDefaults: importDefaults, run: run };
}();
