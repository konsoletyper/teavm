/*
 *  Copyright 2013 Alexey Andreev.
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
"use strict";
function JUnitServer() {
    this.tree = new Tree(document.getElementById("test-tree"));
    this.totalTimeSpent = 0;
    this.expectedExceptions = [];
    this.frame = null;
    this.tests = [];
    this.currentTestNode = null;
    this.testCaseCount = 0;
    this.progressElem = document.getElementById("progress-bar-content");
    this.runCount = 0;
    this.failCount = 0;
    this.traceElem = document.getElementById("test-trace");
    this.failedElem = document.getElementById("failed-test-count");
    this.totalTimeElem = document.getElementById("total-time");
    var self = this;
    this.tree.addSelectionListener(function(node) {
        while (self.traceElem.firstChild) {
            self.traceElem.removeChild(self.traceElem.firstChild);
        }
        if (node.error) {
            var pre = document.createElement("pre");
            pre.appendChild(document.createTextNode(node.error));
            self.traceElem.appendChild(pre);
        }
    });
}
JUnitServer.prototype = {};
JUnitServer.prototype.handleEvent = function(message, callback) {
    if (message.status === "ok") {
        if (this.expectedExceptions.length > 0) {
            this.currentTestNode.success = false;
            this.currentTestNode.error = "Expected exception not thrown";
            this.failCount++;
        } else {
            this.currentTestNode.success = true;
        }
    } else if (message.status === "exception") {
        if (message.exception && this.isExpectedException(message.exception)) {
            this.currentTestNode.success = true;
        } else {
            this.currentTestNode.success = false;
            this.currentTestNode.error = message.stack;
            this.failCount++;
        }
    }
    this.currentTestNode.indicator.className = "complete-indicator " +
            (this.currentTestNode.success ? "successfull" : "failed");
    this.runCount++;
    this.progressElem.style.width = (100 * this.runCount / this.testCaseCount).toFixed(2) + "%";
    document.body.removeChild(this.frame);
    self.frame = null;
    callback();
};
JUnitServer.prototype.isExpectedException = function(ex) {
    for (var i = 0; i < this.expectedExceptions.length; ++i) {
        if (this.expectedExceptions[i] === ex) {
            return true;
        }
    }
    return false;
};
JUnitServer.prototype.loadCode = function(path, additionalScripts, callback) {
    this.frame = document.createElement("iframe");
    this.frame.src = "junit-client.html";
    document.body.appendChild(this.frame);
    var sequence = [];
    sequence.push(path);
    for (var i = 0; i < additionalScripts.length; ++i) {
        sequence.push(additionalScripts[i]);
    }
    var self = this;
    var handler = function() {
        window.removeEventListener("message", handler);
        self.loadScripts(sequence, callback);
    };
    window.addEventListener("message", handler);
};
JUnitServer.prototype.loadScripts = function(scripts, callback) {
    for (var i = 0; i < scripts.length; ++i) {
        this.frame.contentWindow.postMessage({ command : "loadScript", "script" : scripts[i] }, "*");
    }
    var handler = function() {
        window.removeEventListener("message", handler);
        callback();
    };
    window.addEventListener("message", handler);
};
JUnitServer.prototype.runTest = function(node, callback) {
    node.indicator.className = "complete-indicator in-progress";
    var startTime = new Date().getTime();
    if (node.testCase) {
        this.expectedExceptions = node.testCase.expected;
        this.currentTestNode = node;
        var self = this;
        this.loadCode(node.testCase.script, node.testCase.additionalScripts, function() {
            function messageHandler(event) {
                window.removeEventListener("message", messageHandler);
                var timeSpent = new Date().getTime() - startTime;
                node.timeIndicator.appendChild(document.createTextNode("(" + (timeSpent / 1000).toFixed(3) + ")"));
                self.handleEvent(event.data, callback);
            }
            window.addEventListener("message", messageHandler);
            self.frame.contentWindow.postMessage({ command : "runTest" }, "*");
        });
    } else {
        var self = this;
        var nodes = node.getNodes();
        this.runTestFromList(nodes, 0, function() {
            node.success = true;
            for (var i = 0; i < nodes.length; ++i) {
                if (!nodes[i].success) {
                    node.success = false;
                    break;
                }
            }
            node.indicator.className = "complete-indicator " + (node.success ? "successfull" : "failed");
            if (!node.success) {
                node.open();
            }
            var timeSpent = new Date().getTime() - startTime;
            node.timeIndicator.appendChild(document.createTextNode(
                    "(" + (timeSpent / 1000).toFixed(3) + ")"));
            callback();
        });
    }
};
JUnitServer.prototype.readTests = function(tests) {
    var groups = this.groupTests(tests);
    var groupNames = [];
    for (var groupName in groups) {
        groupNames.push(groupName);
    }
    groupNames.sort();
    this.tests = [];
    for (var i = 0; i < groupNames.length; ++i) {
        var groupName = groupNames[i];
        var group = groups[groupName];
        var pkgNode = this.createNode(this.tree, "package", groupName);
        group.sort();
        for (var j = 0; j < group.length; ++j) {
            var test = group[j];
            var simpleName = test.name.substring(groupName.length + 1);
            var testNode = this.createNode(pkgNode, "test", simpleName);
            var methods = test.methods.slice();
            methods.sort();
            for (var k = 0; k < methods.length; ++k) {
                var method = methods[k];
                var caseNode = this.createNode(testNode, "case", method.name);
                caseNode.testCase = method;
                ++this.testCaseCount;
            }
        }
    }
    document.getElementById("test-count").appendChild(document.createTextNode(this.testCaseCount));
    return this;
};
JUnitServer.prototype.createNode = function(parent, className, name) {
    var elem = document.createElement("div");
    elem.className = className;
    elem.appendChild(document.createTextNode(name));
    var node = parent.add(elem);
    node.indicator = document.createElement("div");
    node.indicator.className = "complete-indicator";
    elem.appendChild(node.indicator);
    node.timeIndicator = document.createElement("span");
    node.timeIndicator.className = "time-indicator";
    elem.appendChild(node.timeIndicator);
    return node;
};
JUnitServer.prototype.groupTests = function(tests) {
    var groups = {};
    for (var i = 0; i < tests.length; ++i) {
        var test = tests[i];
        var pkg = test.name.substring(0, test.name.lastIndexOf('.'));
        var group = groups[pkg];
        if (!group) {
            group = [];
            groups[pkg] = group;
        }
        group.push(test);
    }
    return groups;
};
JUnitServer.prototype.runAllTests = function(callback) {
    this.cleanupTests();
    var self = this;
    var startTime = new Date().getTime();
    this.runTestFromList(this.tree.getNodes(), 0, function() {
        self.failedElem.appendChild(document.createTextNode(self.failCount));
        var totalTime = new Date().getTime() - startTime;
        self.totalTimeElem.appendChild(document.createTextNode("(" +
                (totalTime / 1000).toFixed(3) + ")"));
        callback();
    });
};
JUnitServer.prototype.runTestFromList = function(nodes, index, callback) {
    if (index < nodes.length) {
        var node = nodes[index];
        var self = this;
        this.runTest(node, function() {
            self.runTestFromList(nodes, index + 1, callback);
        });
    } else {
        callback();
    }
};
JUnitServer.prototype.cleanupTests = function() {
    if (this.failedElem.firstChild) {
        this.failedElem.removeChild(this.failedElem.firstChild);
    }
    if (this.totalTimeElem.firstChild) {
        this.totalTimeElem.removeChild(this.totalTimeElem.firstChild);
    }
    this.runCount = 0;
    this.failCount = 0;
    this.progressElem.style.width = "0%";
    var nodes = this.tree.getNodes();
    for (var i = 0; i < nodes.length; ++i) {
        this.cleanupNode(nodes[i]);
    }
};
JUnitServer.prototype.cleanupNode = function(node) {
    delete node.error;
    node.indicator.className = "complete-indicator";
    if (node.timeIndicator.firstChild) {
        node.timeIndicator.removeChild(node.timeIndicator.firstChild);
    }
    var nodes = node.getNodes();
    for (var i = 0; i < nodes.length; ++i) {
        this.cleanupNode(nodes[i]);
    }
};

function Tree(container) {
    this.container = container;
    this.nodes = [];
    this.selectedNode = null;
    this.selectionListeners = [];
}
Tree.prototype.createNode = function(content) {
    var elem = document.createElement("div");
    elem.className = "tree-node";
    var contentElem = document.createElement("div");
    contentElem.className = "tree-node-content";
    elem.appendChild(contentElem);
    contentElem.appendChild(content);
    var buttonElem = document.createElement("div");
    buttonElem.className = "tree-node-button closed";
    buttonElem.style.display = "none";
    elem.appendChild(buttonElem);
    var childrenElem = document.createElement("div");
    childrenElem.className = "tree-node-children closed";
    childrenElem.style.display = "none";
    elem.appendChild(childrenElem);
    return new TreeNode(elem, contentElem, buttonElem, childrenElem, this);
};
Tree.prototype.add = function(content) {
    var node = this.createNode(content);
    this.container.appendChild(node.elem);
    this.nodes.push(node);
    return node;
};
Tree.prototype.getNodes = function() {
    return this.nodes;
};
Tree.prototype.addSelectionListener = function(listener) {
    this.selectionListeners.push(listener);
};
function TreeNode(elem, content, button, children, tree) {
    this.elem = elem;
    this.content = content;
    this.button = button;
    this.children = children;
    this.opened = false;
    this.parent = null;
    this.nodes = [];
    this.tree = tree;
    var self = this;
    this.button.onclick = function() {
        self.toggle();
    };
    this.content.onclick = function() {
        self.select();
    }
}
TreeNode.prototype.add = function(content) {
    var node = this.tree.createNode(content);
    this.children.appendChild(node.elem);
    this.button.style.display = "";
    this.children.style.display = "";
    this.content.className = "tree-node-content";
    node.parent = this;
    this.nodes.push(node);
    return node;
};
TreeNode.prototype.isOpened = function() {
    return this.opened;
};
TreeNode.prototype.open = function() {
    if (this.isOpened()) {
        return;
    }
    this.opened = true;
    this.children.className = "tree-node-children opened";
    this.button.className = "tree-node-button opened";
};
TreeNode.prototype.close = function() {
    if (!this.isOpened()) {
        return;
    }
    this.opened = false;
    this.children.className = "tree-node-children closed";
    this.button.className = "tree-node-button closed";
};
TreeNode.prototype.toggle = function() {
    if (this.isOpened()) {
        this.close();
    } else {
        this.open();
    }
};
TreeNode.prototype.getNodes = function() {
    return this.nodes;
};
TreeNode.prototype.getParent = function() {
    return this.parent;
};
TreeNode.prototype.getTree = function() {
    return this.tree;
};
TreeNode.prototype.isSelected = function() {
    return this.tree.selectedNode === this;
};
TreeNode.prototype.select = function() {
    if (this.isSelected()) {
        return;
    }
    if (this.tree.selectedNode != null) {
        this.tree.selectedNode.content.className = "tree-node-content";
    }
    this.content.className = "tree-node-content selected";
    this.tree.selectedNode = this;
    for (var i = 0; i < this.tree.selectionListeners.length; ++i) {
        this.tree.selectionListeners[i](this);
    }
};