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
(function (window) {
    var boot = BOOT_FLAG;
    var reload = RELOAD_FLAG;
    var indicatorVisible = INDICATOR_FLAG;
    var debugPort = DEBUG_PORT;
    var deobfuscate = DEOBFUSCATE_FLAG;
    var fileName = FILE_NAME;
    var pathToFile = PATH_TO_FILE;

    function createWebSocket() {
        return new WebSocket("ws://WS_PATH");
    }

    function createIndicator() {
        function createMainElement() {
            var element = document.createElement("div");
            element.style.position = "fixed";
            element.style.left = "0";
            element.style.bottom = "0";
            element.style.backgroundColor = "black";
            element.style.color = "white";
            element.style.opacity = 0.4;
            element.style.padding = "5px";
            element.style.fontSize = "18px";
            element.style.fontWeight = "bold";
            element.style.pointerEvents = "none";
            element.style.zIndex = 1000;
            element.style.display = "none";
            return element;
        }

        function createLabelElement() {
            return document.createElement("span");
        }

        function createProgressElements() {
            var element = document.createElement("span");
            element.style.display = "none";
            element.style.marginLeft = "10px";
            element.style.width = "150px";
            element.style.height = "15px";
            element.style.borderWidth = "1px";
            element.style.borderColor = "rgb(170,170,170)";
            element.style.borderStyle = "solid";
            element.style.borderRadius = "2px";
            element.style.backgroundColor = "white";
            element.style.position = "relative";

            var progress = document.createElement("span");
            progress.style.display = "block";
            progress.style.position = "absolute";
            progress.style.left = "0";
            progress.style.top = "0";
            progress.style.bottom = "0";
            progress.style.backgroundColor = "rgb(210,210,210)";
            progress.style.borderWidth = "1px";
            progress.style.borderColor = "rgb(170,170,170)";
            progress.style.borderRightStyle = "solid";

            element.appendChild(progress);

            return {
                container: element,
                indicator: progress
            };
        }

        var container = createMainElement();
        var label = createLabelElement();
        var progress = createProgressElements();
        container.appendChild(label);
        container.appendChild(progress.container);

        return {
            container: container,
            label: label,
            progress: progress,
            timer: void 0,

            show: function(text, timeout) {
                this.container.style.display = "block";
                this.label.innerText = text;
                if (this.timer) {
                    clearTimeout(this.timer);
                    this.timer = void 0;
                }
                if (timeout) {
                    setTimeout(function() {
                        this.timer = void 0;
                        this.container.style.display = "none";
                    }.bind(this), timeout * 1000);
                }
            },

            showProgress(value) {
                this.progress.container.style.display = "inline-block";
                this.progress.indicator.style.width = value.toFixed(2) + "%";
            },

            hideProgress() {
                this.progress.container.style.display = "none";
            },
        };
    }

    var indicator = createIndicator();
    function onLoad() {
        document.body.appendChild(indicator.container);
    }

    if (indicatorVisible) {
        if (document.body) {
            onLoad();
        } else {
            window.addEventListener("load", onLoad);
        }
    }

    function installDeobfuscator() {
        if (!deobfuscate) {
            return;
        }

        if (typeof main === 'function') {
            var oldMain = main;
            main = function() {
                var args = arguments;
                window.$teavm_deobfuscator_callback = function() {
                    oldMain.apply(window, args);
                };
                var elem = document.createElement("script");
                elem.src = pathToFile + fileName + ".deobfuscator.js";
                elem.onload = function() {
                    $teavm_deobfuscator([pathToFile + fileName + ".teavmdbg", pathToFile + fileName]);
                };
                document.head.append(elem);
            };
        }
    }

    if (!boot) {
        installDeobfuscator();
    }

    function startMain() {
        ws.close();
        window.removeEventListener("load", onLoad);
        document.body.removeChild(indicator.container);
        installDeobfuscator();
        main();
    }

    var ws = createWebSocket();
    ws.onmessage = function(event) {
        var message = JSON.parse(event.data);
        switch (message.command) {
            case "compiling":
                indicator.show("Compiling...");
                indicator.showProgress(message.progress || 0);
                break;
            case "complete":
                if (message.success) {
                    indicator.show("Compilation complete", 10);
                    if (reload) {
                        window.location.reload(true);
                    } else if (boot) {
                        var scriptElem = document.createElement("script");
                        scriptElem.src = pathToFile + fileName;
                        scriptElem.onload = startMain;
                        document.head.appendChild(scriptElem);
                    }
                } else {
                    indicator.show("Compilation errors occurred")
                }
                indicator.hideProgress();
                break;
        }
    };

    if (boot) {
        indicator.show("File is not ready, about to compile");
    }

    if (debugPort > 0) {
        var connected = false;
        function connectDebugAgent(event) {
            if (event.source !== window) {
                return;
            }
            var data = event.data;
            if (typeof data.teavmDebuggerRequest !== "undefined" && !connected) {
                connected = true;
                window.postMessage({teavmDebugger: {port: debugPort}}, "*");
            }
        }
        window.addEventListener("message", connectDebugAgent);
        window.postMessage({teavmDebugger: {port: debugPort}}, "*");
    }
})(this);