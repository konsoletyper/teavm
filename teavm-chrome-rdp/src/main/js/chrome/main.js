debuggerAgentMap = {};

chrome.browserAction.onClicked.addListener(function(tab) {
    new DebuggerAgent(tab).attach();
});
function DebuggerAgent(tab) {
    this.connection = null;
    this.tab = null;
    this.debuggee = { tabId : tab.id };
    this.attachedToDebugger = false;
    debuggerAgentMap[tab.id] = this;
}
DebuggerAgent.prototype.attach = function() {
    chrome.debugger.attach(this.debuggee, "1.0", (function(callback) {
        this.attachedToDebugger = true;
        chrome.debugger.sendCommand(this.debuggee, "Debugger.enable", {}, callback);
    }).bind(this, connectToServer.bind(this)));
};
DebuggerAgent.prototype.connectToServer = function() {
    this.connection = new WebSocket("ws://localhost:2357/");
    this.connection.onmessage = function(event) {
        receiveMessage(this.debuggee, this.connection, JSON.parse(event.data));
    }.bind(this);
    this.connection.onclose = function(event) {
        if (this.connection != null) {
            this.connection = null;
            this.disconnect();
        }
    }.bind(this);
};
DebuggerAgent.prototype.receiveMessage = function(message) {
    chrome.debugger.sendCommand(this.debuggee, message.method, message.params, function(response) {
        if (message.id) {
            var responseToServer = { id : message.id, result : response };
            this.connection.send(JSON.stringify(responseToServer));
        }
    }.bind(this));
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
    this.connection.send(JSON.stringify(message));
});
chrome.debugger.onDetach.addListener(function(source) {
    var agent = debuggerAgentMap[source.tabId];
    if (agent) {
        agent.attachedToDebugger = false;
        agent.disconnect();
    }
});