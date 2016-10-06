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
        this.resultTableBody = document.getElementById("result-table-body");
    }
    Benchmark.prototype.runAll = function() {
        load(this, function() { this.module.exports.main(); }.bind(this));
    };

    function tick(benchmark) {
        var exports = benchmark.module.exports;
        exports.tick();
        var exception = exports.sys$catchException();
        if (exception !== 0) {
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
        xhr.responseType = "arraybuffer";
        xhr.open("GET", "teavm-wasm/classes.wasm");
        xhr.onload = function() {
            var response = xhr.response;
            if (!response) {
                return;
            }
            var canvas = benchmark.canvas;
            var importObj = {
                runtime: {
                    currentTimeMillis: currentTimeMillis,
                    isNaN: isNaN,
                    isFinite: isFinite,
                    getNaN: function() { return NaN; },
                    putchar: function(code) { putchar(benchmark, code); }
                },
                benchmark: {
                    performanceTime: function() { return window.performance.now() || 0; },
                    reportPerformance: function(second, timeSpentComputing) {
                        var row = document.createElement("tr");
                        benchmark.resultTableBody.appendChild(row);
                        var secondCell = document.createElement("td");
                        row.appendChild(secondCell);
                        secondCell.appendChild(document.createTextNode(second.toString()));
                        var timeCell = document.createElement("td");
                        row.appendChild(timeCell);
                        timeCell.appendChild(document.createTextNode(timeSpentComputing.toString()));
                    },
                    repeatAfter: function(time) {
                        setTimeout(tick.bind(null, benchmark), time);
                    },
                    setupCanvas: function() {
                        canvas.fillStyle = "white";
                        canvas.strokeStyle = "grey";
                        canvas.fillRect(0, 0, 600, 600);
                        canvas.translate(0, 600);
                        canvas.scale(1, -1);
                        canvas.scale(100, 100);
                        canvas.lineWidth = 0.01;
                    }
                },
                canvas: {
                    save: function() {
                        canvas.save();
                    },
                    restore: function() {
                        canvas.restore();
                    },
                    beginPath: function() {
                        canvas.beginPath();
                    },
                    closePath: function() {
                        canvas.closePath();
                    },
                    stroke: function() {
                        canvas.stroke();
                    },
                    moveTo: function(x, y) {
                        canvas.moveTo(x, y);
                    },
                    lineTo: function(x, y) {
                        canvas.lineTo(x, y);
                    },
                    translate: function(x, y) {
                        canvas.translate(x, y);
                    },
                    rotate: function(angle) {
                        canvas.rotate(angle);
                    },
                    arc: function(cx, cy, radius, startAngle, endAngle, counterClockwise) {
                        canvas.arc(cx, cy, radius, startAngle, endAngle, counterClockwise);
                    }
                },
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
            WebAssembly.compile(response).then(function(module) {
                benchmark.module = new WebAssembly.Instance(module, importObj);
                callback();
            });
        };
        xhr.send();
    }

    return Benchmark;
}();