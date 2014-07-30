/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.chromerpd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.teavm.chromerpd.data.CallFrameDTO;
import org.teavm.chromerpd.data.LocationDTO;
import org.teavm.chromerpd.data.Message;
import org.teavm.chromerpd.data.Response;
import org.teavm.chromerpd.messages.*;
import org.teavm.debugging.JavaScriptBreakpoint;
import org.teavm.debugging.JavaScriptCallFrame;
import org.teavm.debugging.JavaScriptLocation;

/**
 *
 * @author Alexey Andreev
 */
@ServerEndpoint("/")
public class ChromeRDPDebuggerEndpoint {
    private Session session;
    private RDPCallFrame[] callStack = new RDPCallFrame[0];
    private Map<String, String> scripts = new HashMap<>();
    private Map<String, String> scriptIds = new HashMap<>();
    private boolean suspended = false;
    private ObjectMapper mapper = new ObjectMapper();
    private Map<Integer, ResponseHandler> responseHandlers = new HashMap<>();
    private int messageIdGenerator;
    boolean closed;
    private ChromeRDPDebugger debugger;

    @OnOpen
    public void open(Session session) {
        this.session = session;
        Object debugger = session.getUserProperties().get("chrome.rdp");
        if (debugger instanceof ChromeRDPDebugger) {
            this.debugger = (ChromeRDPDebugger)debugger;
            this.debugger.setEndpoint(this);
        }
    }

    @OnClose
    public void close() {
        closed = true;
        if (this.debugger != null) {
            this.debugger.setEndpoint(null);
            this.debugger = null;
        }
    }

    @OnMessage
    public void receive(String messageText) throws IOException {
        JsonNode jsonMessage = mapper.readTree(messageText);
        if (jsonMessage.has("result")) {
            Response response = mapper.reader(Response.class).readValue(jsonMessage);
            responseHandlers.remove(response.getId()).received(response.getResult());
        } else {
            Message message = mapper.reader(Message.class).readValue(messageText);
            switch (message.getMethod()) {
                case "Debugger.paused":
                    firePaused(parseJson(SuspendedNotification.class, message.getParams()));
                    break;
                case "Debugger.resumed":
                    fireResumed();
                    break;
                case "Debugger.scriptParsed":
                    scriptParsed(parseJson(ScriptParsedNotification.class, message.getParams()));
                    break;
            }
        }
    }

    private <T> T parseJson(Class<T> type, JsonNode node) throws IOException {
        return mapper.reader(type).readValue(node);
    }

    private synchronized void firePaused(SuspendedNotification params) {
        suspended = true;
        CallFrameDTO[] callFrameDTOs = params.getCallFrames();
        RDPCallFrame[] callStack = new RDPCallFrame[callFrameDTOs.length];
        for (int i = 0; i < callStack.length; ++i) {
            callStack[i] = map(callFrameDTOs[i]);
        }
        this.callStack = callStack;
        debugger.firePaused();
    }

    RDPCallFrame map(CallFrameDTO dto) {
        return new RDPCallFrame(dto.getCallFrameId(), map(dto.getLocation()));
    }

    JavaScriptLocation map(LocationDTO dto) {
        return new JavaScriptLocation(scripts.get(dto.getScriptId()), dto.getLineNumber(), dto.getColumnNumber());
    }

    LocationDTO unmap(JavaScriptLocation location) {
        LocationDTO dto = new LocationDTO();
        dto.setScriptId(scriptIds.get(location.getScript()));
        dto.setLineNumber(location.getLine());
        dto.setColumnNumber(location.getColumn());
        return dto;
    }

    private synchronized void fireResumed() {
        suspended = false;
        callStack = null;
        debugger.fireResumed();
    }

    private synchronized void scriptParsed(ScriptParsedNotification params) {
        if (scripts.containsKey(params.getScriptId())) {
            return;
        }
        scripts.put(params.getScriptId(), params.getUrl());
        scriptIds.put(params.getUrl(), params.getScriptId());
        debugger.fireScriptAdded(params.getUrl());
    }

    private void sendMessage(Message message) {
        if (closed) {
            return;
        }
        try {
            String messageText = mapper.writer().writeValueAsString(message);
            session.getAsyncRemote().sendText(messageText);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void suspend() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.pause");
        sendMessage(message);
    }

    public void resume() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.resume");
        sendMessage(message);
    }

    public void stepInto() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepInto");
        sendMessage(message);
    }

    public void stepOut() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepOut");
        sendMessage(message);
    }

    public void stepOver() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepOver");
        sendMessage(message);
    }

    public synchronized void continueToLocation(JavaScriptLocation location) {
        Message message = new Message();
        message.setMethod("Debugger.continueToLocation");
        ContinueToLocationCommand params = new ContinueToLocationCommand();
        params.setLocation(unmap(location));
        message.setParams(mapper.valueToTree(params));
        sendMessage(message);
    }

    public synchronized boolean isSuspended() {
        return suspended;
    }

    public synchronized JavaScriptCallFrame[] getCallStack() {
        return callStack;
    }

    public synchronized JavaScriptBreakpoint getCurrentBreakpoint() {
        return null;
    }

    public synchronized void updateBreakpoint(final RDPBreakpoint breakpoint) {
        Message message = new Message();
        message.setId(++messageIdGenerator);
        message.setMethod("Debugger.setBreakpoint");
        SetBreakpointCommand params = new SetBreakpointCommand();
        params.setLocation(unmap(breakpoint.getLocation()));
        message.setParams(mapper.valueToTree(params));
        ResponseHandler handler = new ResponseHandler() {
            @Override public void received(JsonNode node) throws IOException {
                SetBreakpointResponse response = mapper.reader(SetBreakpointResponse.class).readValue(node);
                breakpoint.chromeId = response.getBreakpointId();
                debugger.fireBreakpointStatusChanged(breakpoint);
            }
        };
        responseHandlers.put(message.getId(), handler);
        sendMessage(message);
    }

    void destroyBreakpoint(RDPBreakpoint breakpoint) {
        if (breakpoint.chromeId != null) {
            Message message = new Message();
            message.setMethod("Debugger.removeBreakpoint");
            RemoveBreakpointCommand params = new RemoveBreakpointCommand();
            params.setBreakpointId(breakpoint.chromeId);
            message.setParams(mapper.valueToTree(params));
            sendMessage(message);
        }
    }

    interface ResponseHandler {
        void received(JsonNode node) throws IOException;
    }
}
