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
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.teavm.chromerpd.data.CallFrameDTO;
import org.teavm.chromerpd.data.LocationDTO;
import org.teavm.chromerpd.data.Message;
import org.teavm.chromerpd.messages.ContinueToLocationCommand;
import org.teavm.chromerpd.messages.ScriptParsedNotification;
import org.teavm.chromerpd.messages.SuspendedNotification;
import org.teavm.debugging.*;

/**
 *
 * @author Alexey Andreev
 */
@ServerEndpoint("/")
public class ChromeRDPDebugger implements JavaScriptDebugger {
    private List<JavaScriptDebuggerListener> listeners = new ArrayList<>();
    private Session session;
    private RDPCallFrame[] callStack = new RDPCallFrame[0];
    private Map<String, String> scripts = new HashMap<>();
    private Map<String, String> scriptIds = new HashMap<>();
    private boolean suspended;
    private ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void open(Session session) {
        this.session = session;
    }

    @OnMessage
    public void receive(String messageText) throws IOException {
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
        try {
            String messageText = mapper.writer().writeValueAsString(message);
            session.getAsyncRemote().sendText(messageText);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void suspend() {
        Message message = new Message();
        message.setMethod("Debugger.pause");
        sendMessage(message);
    }

    @Override
    public void resume() {
        Message message = new Message();
        message.setMethod("Debugger.resume");
        sendMessage(message);
    }

    @Override
    public void stepInto() {
        Message message = new Message();
        message.setMethod("Debugger.stepInto");
        sendMessage(message);
    }

    @Override
    public void stepOut() {
        Message message = new Message();
        message.setMethod("Debugger.stepOut");
        sendMessage(message);
    }

    @Override
    public void stepOver() {
        Message message = new Message();
        message.setMethod("Debugger.stepOver");
        sendMessage(message);
    }

    @Override
    public void continueToLocation(JavaScriptLocation location) {
        Message message = new Message();
        message.setMethod("Debugger.continueToLocation");
        ContinueToLocationCommand params = new ContinueToLocationCommand();
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setScriptId(scriptIds.get(location.getScript()));
        locationDTO.setLineNumber(location.getLine());
        locationDTO.setColumnNumber(location.getColumn());
        params.setLocation(locationDTO);
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
        return null;
    }
}
