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
        this.instance = null;
        this.resultTableBody = document.getElementById("result-table-body");
    }
    Benchmark.prototype.load = function() {
        TeaVM.wasm.load("teavm-wasm/classes.wasm", {
            installImports: installImports.bind(this),
        }).then(teavm => {
            this.instance = teavm;
            teavm.main();
        })
    };

    function installImports(o) {
        var canvas = this.canvas;
        o.benchmark = {
            performanceTime: function() { return window.performance.now() || 0; },
            reportPerformance: function(second, timeSpentComputing) {
                var row = document.createElement("tr");
                this.resultTableBody.appendChild(row);
                var secondCell = document.createElement("td");
                row.appendChild(secondCell);
                secondCell.appendChild(document.createTextNode(second.toString()));
                var timeCell = document.createElement("td");
                row.appendChild(timeCell);
                timeCell.appendChild(document.createTextNode(timeSpentComputing.toString()));
            }.bind(this),
            repeatAfter: function(time) {
                setTimeout(tick.bind(this), time);
            }.bind(this),
            setupCanvas: function() {
                canvas.fillStyle = "white";
                canvas.strokeStyle = "grey";
                canvas.fillRect(0, 0, 600, 600);
                canvas.translate(0, 600);
                canvas.scale(1, -1);
                canvas.scale(100, 100);
                canvas.lineWidth = 0.01;
            }
        };
        o.canvas = {
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
        };
    }

    function tick() {
        var exports = this.instance.exports;
        exports.tick();
        var exception = exports.teavm_catchException();
        if (exception !== 0) {
            console.log("Exception: " + exception);
        }
    }

    return Benchmark;
}();