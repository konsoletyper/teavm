debuggerAgentMap = {};

chrome.browserAction.onClicked.addListener(function(tab) {
    new DebuggerAgent(tab).attach();
});
function DebuggerAgent(tab) {
    this.pendingMessages = [];
    this.connection = null;
    this.tab = null;
    this.debuggee = { tabId : tab.id };
    this.attachedToDebugger = false;
    this.messageBuffer = "";
    debuggerAgentMap[tab.id] = this;
}
DebuggerAgent.MAX_MESSAGE_SIZE = 65534;
DebuggerAgent.prototype.attach = function() {
    chrome.debugger.attach(this.debuggee, "1.0", function(callback) {
        this.attachedToDebugger = true;
        chrome.debugger.sendCommand(this.debuggee, "Debugger.enable", {}, callback);
    }.bind(this, this.connectToServer.bind(this)));
};
DebuggerAgent.prototype.connectToServer = function() {
    this.connection = new WebSocket("ws://localhost:2357/");
    this.connection.onmessage = function(event) {
        var str = event.data;
        var ctl = str.substring(0, 1);
        this.messageBuffer += str.substring(1);
        if (ctl == '.') {
            this.receiveMessage(JSON.parse(this.messageBuffer));
            this.messageBuffer = "";
        }
    }.bind(this);
    this.connection.onclose = function(event) {
        if (this.connection != null) {
            this.connection = null;
            this.disconnect();
        }
    }.bind(this);
    this.connection.onopen = function() {
        for (var i = 0; i < this.pendingMessages.length; ++i) {
            this.sendMessage(this.pendingMessages[i]);
        }
        this.pendingMessages = null;
    }.bind(this);
};
DebuggerAgent.prototype.receiveMessage = function(message) {
    chrome.debugger.sendCommand(this.debuggee, message.method, message.params, function(response) {
        if (message.id) {
            var responseToServer = { id : message.id, result : response,
                    error : response ? undefined : chrome.runtime.lastError };
            this.sendMessage(responseToServer);
        }
    }.bind(this));
};
DebuggerAgent.prototype.sendMessage = function(message) {
    var str = JSON.stringify(message);
    while (str.length > DebuggerAgent.MAX_MESSAGE_SIZE) {
        var part = "," + str.substring(0, DebuggerAgent.MAX_MESSAGE_SIZE);
        this.connection.send(part);
        str = str.substring(DebuggerAgent.MAX_MESSAGE_SIZE);
    }
    this.connection.send("." + str);
};
DebuggerAgent.prototype.disconnect = function() {
    if (this.connection) {
        var conn = this.connection;
        this.connection = null;
        conn.close();
    }
    if (this.attachedToDebugger) {
        chrome.debugger.detach(this.debuggee);
        this.attachedToDebugger = false;
    }
    if (this.debuggee) {
        delete debuggerAgentMap[this.debuggee.tabId];
        this.debuggee = null;
    }
};

chrome.debugger.onEvent.addListener(function(source, method, params) {
    var agent = debuggerAgentMap[source.tabId];
    if (!agent) {
        return;
    }
    var message = { method : method, params : params };
    if (agent.pendingMessages) {
        agent.pendingMessages.push(message);
    } else if (agent.connection) {
        agent.sendMessage(message);
    }
});
chrome.debugger.onDetach.addListener(function(source) {
    var agent = debuggerAgentMap[source.tabId];
    if (agent) {
        agent.attachedToDebugger = false;
        agent.disconnect();
    }
});