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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.websocket.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.teavm.chromerpd.data.CallFrameDTO;
import org.teavm.chromerpd.data.LocationDTO;
import org.teavm.chromerpd.data.Message;
import org.teavm.chromerpd.data.Response;
import org.teavm.chromerpd.messages.*;
import org.teavm.debugging.*;

/**
 *
 * @author Alexey Andreev
 */
@ClientEndpoint
public class ChromeRDPDebuggerEndpoint implements JavaScriptDebugger {
    private List<JavaScriptDebuggerListener> listeners = new ArrayList<>();
    private Session session;
    private RDPCallFrame[] callStack = new RDPCallFrame[0];
    private Map<String, String> scripts = new HashMap<>();
    private Map<String, String> scriptIds = new HashMap<>();
    private boolean suspended;
    private ObjectMapper mapper = new ObjectMapper();
    private Map<Integer, Deferred> deferredResponses = new HashMap<>();
    private int messageIdGenerator;
    boolean closed;

    @OnOpen
    public void open(Session session) {
        this.session = session;
        Object container = session.getUserProperties().get("container");
        if (container instanceof ChromeRDPContainer) {
            ((ChromeRDPContainer)container).setDebugger(this);
        }
    }

    @OnClose
    public void close() {
        closed = true;
    }

    @OnMessage
    public void receive(String messageText) throws IOException {
        JsonNode jsonMessage = mapper.readTree(messageText);
        if (jsonMessage.has("result")) {
            Response response = mapper.reader(Response.class).readValue(jsonMessage);
            deferredResponses.remove(response.getId()).set(response.getResult());
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

    private void firePaused(SuspendedNotification params) {
        suspended = true;
        CallFrameDTO[] callFrameDTOs = params.getCallFrames();
        RDPCallFrame[] callFrames = new RDPCallFrame[callFrameDTOs.length];
        for (int i = 0; i < callFrames.length; ++i) {
            callFrames[i] = map(callFrameDTOs[i]);
        }
        for (JavaScriptDebuggerListener listener : listeners) {
            listener.paused();
        }
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

    private void fireResumed() {
        suspended = false;
        for (JavaScriptDebuggerListener listener : listeners) {
            listener.resumed();
        }
    }

    private void scriptParsed(ScriptParsedNotification params) {
        if (scripts.containsKey(params.getScriptId())) {
            return;
        }
        scripts.put(params.getScriptId(), params.getUrl());
        scriptIds.put(params.getUrl(), params.getScriptId());
        for (JavaScriptDebuggerListener listener : listeners) {
            listener.scriptAdded(params.getUrl());
        }
    }

    @Override
    public void addListener(JavaScriptDebuggerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(JavaScriptDebuggerListener listener) {
        listeners.remove(listener);
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

    @Override
    public void suspend() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.pause");
        sendMessage(message);
    }

    @Override
    public void resume() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.resume");
        sendMessage(message);
    }

    @Override
    public void stepInto() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepInto");
        sendMessage(message);
    }

    @Override
    public void stepOut() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepOut");
        sendMessage(message);
    }

    @Override
    public void stepOver() {
        if (closed) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepOver");
        sendMessage(message);
    }

    @Override
    public void continueToLocation(JavaScriptLocation location) {
        Message message = new Message();
        message.setMethod("Debugger.continueToLocation");
        ContinueToLocationCommand params = new ContinueToLocationCommand();
        params.setLocation(unmap(location));
        message.setParams(mapper.valueToTree(params));
        sendMessage(message);
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public JavaScriptCallFrame[] getCallStack() {
        return callStack;
    }

    @Override
    public JavaScriptBreakpoint getCurrentBreakpoint() {
        return null;
    }

    @Override
    public JavaScriptBreakpoint createBreakpoint(JavaScriptLocation location) {
        Message message = new Message();
        message.setId(messageIdGenerator++);
        message.setMethod("Debugger.setBreakpoint");
        SetBreakpointCommand params = new SetBreakpointCommand();
        params.setLocation(unmap(location));
        message.setParams(mapper.valueToTree(params));
        Deferred deferred = new Deferred();
        deferredResponses.put(message.getId(), deferred);
        sendMessage(message);
        try {
            SetBreakpointResponse response = mapper.reader(SetBreakpointResponse.class)
                    .readValue((JsonNode)deferred.get());
            return new RDPBreakpoint(response.getBreakpointId(), this, map(response.getActualLocation()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void destroyBreakpoint(RDPBreakpoint breakpoint) {
        Message message = new Message();
        message.setMethod("Debugger.removeBreakpoint");
        RemoveBreakpointCommand params = new RemoveBreakpointCommand();
        params.setBreakpointId(breakpoint.getChromeId());
        message.setParams(mapper.valueToTree(params));
        sendMessage(message);
    }
}
