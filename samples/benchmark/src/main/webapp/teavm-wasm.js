/*
 *  Copyright 2016 Alexey Andreev.
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

var Benchmark = function() {
    function Benchmark(canvas) {
        this.canvas = canvas;
        this.module = null;
        this.line = "";
    }
    Benchmark.prototype.runAll = function() {
        load(this, function() { this.module.exports.main(); }.bind(this));
    }

    function tick(benchmark) {
        var exports = benchmark.module.exports;
        exports.tick();
        console.log("tick");
        var exception = exports.sys$catchException();
        if (exception != null) {
            console.log("Exception: " + exception);
        }
    }

    function currentTimeMillis() {
        return new Date().getTime();
    }

    function putchar(benchmark, charCode) {
        if (charCode == 10) {
            console.log(benchmark.line);
            benchmark.line = "";
        } else {
            benchmark.line += String.fromCharCode(charCode);
        }
    }

    function load(benchmark, callback) {
        var xhr = new XMLHttpRequest();
        xhr.open("GET", "teavm-wasm/classes.wasm");
        xhr.onreadystatechange = function() {
            var response = xhr.response;
            if (!response) {
                return;
            }
            var importObj = {
                runtime: {
                    currentTimeMillis: currentTimeMillis,
                    isNaN: isNaN,
                    isFinite: isFinite,
                    getNaN: function() { return NaN; },
                    putchar: function() { putchar(benchmark); }
                },
                benchmark: {
                    performanceTime: function() { return window.performance.now() || 0; },
                    reportPerformance: function(second, timeSpentComputing) {
                        console.log("Second: " + second + ", time: " + timeSpentComputing);
                    },
                    repeatAfter: function(time) {
                        console.log("repeatAfter");
                        setTimeout(tick.bind(benchmark), time);
                    },
                    setupCanvas: function() {
                        var canvas = benchmark.canvas;
                        canvas.setFillStyle("white");
                        context.setStrokeStyle("grey");
                        canvas.fillRect(0, 0, 600, 600);
                        canvas.translate(0, 600);
                        canvas.scale(1, -1);
                        canvas.scale(100, 100);
                        canvas.setLineWidth(0.01);
                    }
                },
                canvas: benchmark.canvas,
                math: Math,
                debug: {
                    traceMemoryAccess: function(callSite, address) {
                        if (address >= 63 * 65536) {
                            console.log("Memory access #" + callSite + " at " + address);
                        }
                        return address;
                    }
                }
            };
            benchmark.module = Wasm.instantiateModule(new Uint8Array(response), importObj)
            callback();
        };
        xhr.send();
    }

    return Benchmark;
}();
