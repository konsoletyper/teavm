debuggerAgentMap = Object.create(null);

class DebuggerAgent {
    constructor(tab, port) {
        this.pendingMessages = [];
        this.connection = null;
        this.attachedToDebugger = false;
        this.messageBuffer = "";
        this.port = 0;
        this.disconnectCallbacks = null;

        this.debuggee = { tabId : tab.id };
        this.port = port;
        debuggerAgentMap[tab.id] = this;
    }

    attach() {
        chrome.debugger.attach(this.debuggee, "1.2", () => {
            this.attachedToDebugger = true;
            chrome.debugger.sendCommand(this.debuggee, "Debugger.enable", {}, () => this.connectToServer());
        });
    }

    connectToServer() {
        this.connection = new WebSocket("ws://localhost:" + this.port + "/");
        this.connection.onmessage = event => {
            const str = event.data;
            const ctl = str.substring(0, 1);
            this.messageBuffer += str.substring(1);
            if (ctl === '.') {
                this.receiveMessage(JSON.parse(this.messageBuffer));
                this.messageBuffer = "";
            }
        };
        this.connection.onclose = () => {
            if (this.connection != null) {
                this.connection = null;
                this.disconnect();
            }
        };
        this.connection.onopen = () => {
            for (const pendingMessage of this.pendingMessages) {
                this.sendMessage(pendingMessage);
            }
            this.pendingMessages = null;
        };
    }

    receiveMessage(message) {
        chrome.debugger.sendCommand(this.debuggee, message.method, message.params, response => {
            if (message.id) {
                const responseToServer = {
                    id : message.id,
                    result : response,
                    error : response ? void 0 : chrome.runtime.lastError
                };
                this.sendMessage(responseToServer);
            }
        });
    }

    sendMessage(message) {
        let str = JSON.stringify(message);
        while (str.length > DebuggerAgent.MAX_MESSAGE_SIZE) {
            const part = "," + str.substring(0, DebuggerAgent.MAX_MESSAGE_SIZE);
            this.connection.send(part);
            str = str.substring(DebuggerAgent.MAX_MESSAGE_SIZE);
        }
        this.connection.send("." + str);
    }

    disconnect(callback) {
        if (this.connection) {
            const conn = this.connection;
            this.connection = null;
            conn.close();
        }
        if (this.attachedToDebugger) {
            chrome.debugger.detach(this.debuggee, () => {
                if (this.debuggee) {
                    delete debuggerAgentMap[this.debuggee.tabId];
                    this.debuggee = null;
                }
                for (const callback of this.disconnectCallbacks) {
                    callback();
                }
                this.disconnectCallbacks = null;
            });
            this.attachedToDebugger = false;
            this.disconnectCallbacks = [];
        }

        if (callback) {
            if (this.disconnectCallbacks != null) {
                this.disconnectCallbacks.push(callback);
            } else {
                callback();
            }
        }
    }
}

DebuggerAgent.MAX_MESSAGE_SIZE = 65534;

chrome.browserAction.onClicked.addListener(function(tab) {
    chrome.storage.sync.get({ port: 2357 }, items => attachDebugger(tab, items.port));
});

chrome.debugger.onEvent.addListener((source, method, params) => {
    const agent = debuggerAgentMap[source.tabId];
    if (!agent) {
        return;
    }
    const message = { method : method, params : params };
    if (agent.pendingMessages) {
        agent.pendingMessages.push(message);
    } else if (agent.connection) {
        agent.sendMessage(message);
    }
});
chrome.debugger.onDetach.addListener((source) => {
    const agent = debuggerAgentMap[source.tabId];
    if (agent) {
        agent.attachedToDebugger = false;
        agent.disconnect();
    }
});

chrome.runtime.onMessage.addListener((message, sender) => {
    if (message.command === "debug") {
        attachDebugger(sender.tab, message.port);
    }
});

function attachDebugger(tab, port) {
    const existingAgent = debuggerAgentMap[tab.id];
    if (!existingAgent) {
        new DebuggerAgent(tab, port).attach();
    } else if (existingAgent.port !== port) {
        existingAgent.disconnect(() => {
            new DebuggerAgent(tab, port).attach();
        });
    }
}