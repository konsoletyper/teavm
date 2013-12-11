JUnitServer = function(container) {
    this.container = container;
    this.timeSpent = 0;
    this.totalTimeSpent = 0;
    this.methodCount = 0;
    this.statusCell = null;
    this.exceptionCell = null;
    this.timeCell = null;
    this.startTime = 0;
    this.expectedExceptions = [];
    this.table = null;
    this.tableBody = null;
    this.frame = null;
}
JUnitServer.prototype = new Object();
JUnitServer.prototype.handleEvent = function(message, callback) {
    endTime = new Date().getTime();
    if (message.status === "ok") {
        if (this.expectedExceptions.length > 0) {
            this.statusCell.appendChild(document.createTextNode("expected exception not thrown"));
            this.statusCell.style.color = 'yellow';
        } else {
            this.statusCell.appendChild(document.createTextNode("ok"));
            this.statusCell.style.color = 'green';
        }
    } else if (message.status === "exception") {
        if (message.exception && this.isExpectedException(message.exception)) {
            this.statusCell.appendChild(document.createTextNode("ok"));
            this.statusCell.style.color = 'green';
        } else {
            this.statusCell.appendChild(document.createTextNode("unexpected exception"));
            var exceptionText = document.createElement("pre");
            exceptionText.appendChild(document.createTextNode(message.stack));
            this.exceptionCell.appendChild(exceptionText);
            this.statusCell.style.color = 'red';
        }
    }
    ++this.methodCount;
    var timeSpent = (endTime - this.startTime) / 1000;
    this.timeSpent += timeSpent;
    this.timeCell.appendChild(document.createTextNode(timeSpent.toFixed(3)));
    document.body.removeChild(this.frame);
    self.frame = null;
    callback();
}
JUnitServer.prototype.isExpectedException = function(ex) {
    for (var i = 0; i < this.expectedExceptions.length; ++i) {
        if (this.expectedExceptions[i] === ex) {
            return true;
        }
    }
    return false;
}
JUnitServer.prototype.runTestCase = function(methodName, path, expectedExceptions, callback) {
    this.createRow(methodName);
    this.startTime = new Date().getTime();
    this.expectedExceptions = expectedExceptions;
    var self = this;
    this.loadCode(path, function() {
        messageHandler = function(event) {
            window.removeEventListener("message", messageHandler);
            self.handleEvent(JSON.parse(event.data), callback);
        };
        window.addEventListener("message", messageHandler);
        self.frame.contentWindow.postMessage("runTest", "*");
    });
}
JUnitServer.prototype.createRow = function(methodName) {
    var row = document.createElement("tr");
    this.tableBody.appendChild(row);
    var nameCell = document.createElement("td");
    row.appendChild(nameCell);
    nameCell.appendChild(document.createTextNode(methodName));
    this.statusCell = document.createElement("td");
    row.appendChild(this.statusCell);
    this.exceptionCell = document.createElement("td");
    row.appendChild(this.exceptionCell);
    this.timeCell = document.createElement("td");
    row.appendChild(this.timeCell);
}
JUnitServer.prototype.loadCode = function(path, callback) {
    this.frame = document.createElement("iframe");
    document.body.appendChild(this.frame);
    var frameDoc = this.frame.contentWindow.document;
    var self = this;
    this.loadScript("junit-support.js", function() {
        self.loadScript("runtime.js", function() {
            self.loadScript(path, callback);
        });
    });
}
JUnitServer.prototype.loadScript = function(name, callback) {
    var doc = this.frame.contentWindow.document;
    var script = doc.createElement("script");
    script.src = name;
    doc.body.appendChild(script);
    script.onload = function() {
        callback();
    }
}
JUnitServer.prototype.runTest = function(test, callback) {
    this.timeSpent = 0;
    this.methodCount = 0;
    this.createTable(test.name);
    var self = this;
    this.runMethodFromList(test.methods, 0, function() {
        self.createFooter();
        callback();
    });
}
JUnitServer.prototype.runAllTests = function(tests, callback) {
    this.runTestFromList(tests, 0, callback);
}
JUnitServer.prototype.runTestFromList = function(tests, index, callback) {
    if (index < tests.length) {
        var test = tests[index];
        var self = this;
        this.runTest(test, function() {
            self.runTestFromList(tests, index + 1, callback);
        });
    } else {
        callback();
    }
}
JUnitServer.prototype.runMethodFromList = function(methods, index, callback) {
    if (index < methods.length) {
        var method = methods[index];
        var self = this;
        this.runTestCase(method.name, method.script, method.expected, function() {
            self.runMethodFromList(methods, index + 1, callback);
        });
    } else {
        callback();
    }
}
JUnitServer.prototype.createTable = function(name) {
    this.table = document.createElement("table");
    this.container.appendChild(this.table);
    var caption = document.createElement("caption");
    this.table.appendChild(caption);
    this.createHeader();
    caption.appendChild(document.createTextNode(name));
    this.tableBody = document.createElement("tbody");
    this.table.appendChild(this.tableBody);
}
JUnitServer.prototype.createHeader = function() {
    var head = document.createElement("thead");
    this.table.appendChild(head);
    var headRow = document.createElement("tr");
    head.appendChild(headRow);
    var headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Method"));
    headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Result"));
    headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Exception"));
    headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Time spent, s"));
    this.table.appendChild(head);
}
JUnitServer.prototype.createFooter = function() {
    var foot = document.createElement("tfoot");
    this.table.appendChild(foot);
    var footRow = document.createElement("tr");
    foot.appendChild(footRow);
    var footName = document.createElement("td");
    footRow.appendChild(footName);
    footName.appendChild(document.createTextNode("---"));
    var footMethods = document.createElement("td");
    footRow.appendChild(footMethods);
    footMethods.appendChild(document.createTextNode(this.methodCount));
    var footSpace = document.createElement("td");
    footRow.appendChild(footSpace);
    footSpace.appendChild(document.createTextNode("---"));
    var footTime = document.createElement("td");
    footRow.appendChild(footTime);
    footTime.appendChild(document.createTextNode(this.timeSpent.toFixed(3)));
}

JUnitClient = {};
JUnitClient.run = function(runner) {
    var handler = window.addEventListener("message", function() {
        window.removeEventListener("message", handler);
        var message = {};
        try {
            runner();
            message.status = "ok";
        } catch (e) {
            message.status = "exception";
            if (e.$javaException && e.$javaException.$class && e.$javaException.$class.$meta) {
                message.exception = e.$javaException.$class.$meta.name;
            }
            message.stack = e.stack;
        }
        window.parent.postMessage(JSON.stringify(message), "*");
    });
}