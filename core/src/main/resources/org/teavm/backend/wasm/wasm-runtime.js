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

    function importDefaults(obj) {
        obj.teavm = {
            currentTimeMillis: currentTimeMillis,
            isnan: isNaN,
            TeaVM_getNaN: function() { return NaN; },
            isinf: function(n) { return !isFinite(n) },
            isfinite: isFinite,
            putwchar: putwchar,
            towlower: towlower,
            towupper: towupper,
            getNativeOffset: getNativeOffset
        };

        obj.teavmMath = Math;
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
