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
package org.teavm.chromerdp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.chromerdp.data.*;
import org.teavm.chromerdp.messages.*;
import org.teavm.debugging.javascript.*;

/**
 *
 * @author Alexey Andreev
 */
public class ChromeRDPDebugger implements JavaScriptDebugger, ChromeRDPExchangeConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ChromeRDPDebugger.class);
    private static final Object dummy = new Object();
    private ChromeRDPExchange exchange;
    private ConcurrentMap<JavaScriptDebuggerListener, Object> listeners = new ConcurrentHashMap<>();
    private ConcurrentMap<JavaScriptLocation, RDPBreakpoint> breakpointLocationMap = new ConcurrentHashMap<>();
    private ConcurrentMap<RDPBreakpoint, Object> breakpoints = new ConcurrentHashMap<>();
    private volatile RDPCallFrame[] callStack = new RDPCallFrame[0];
    private ConcurrentMap<String, String> scripts = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> scriptIds = new ConcurrentHashMap<>();
    private boolean suspended;
    private ObjectMapper mapper = new ObjectMapper();
    private ConcurrentMap<Integer, ResponseHandler> responseHandlers = new ConcurrentHashMap<>();
    private AtomicInteger messageIdGenerator = new AtomicInteger();
    private Lock breakpointLock = new ReentrantLock();

    private List<JavaScriptDebuggerListener> getListeners() {
        return new ArrayList<>(listeners.keySet());
    }

    @Override
    public void setExchange(ChromeRDPExchange exchange) {
        if (this.exchange == exchange) {
            return;
        }
        if (this.exchange != null) {
            this.exchange.removeListener(exchangeListener);
        }
        this.exchange = exchange;
        if (exchange != null) {
            for (RDPBreakpoint breakpoint : breakpoints.keySet().toArray(new RDPBreakpoint[0])) {
                updateBreakpoint(breakpoint);
            }
            for (JavaScriptDebuggerListener listener : getListeners()) {
                listener.attached();
            }
        } else {
            suspended = false;
            callStack = null;
            for (JavaScriptDebuggerListener listener : getListeners()) {
                listener.detached();
            }
        }
        if (this.exchange != null) {
            this.exchange.addListener(exchangeListener);
        }
    }

    private ChromeRDPExchangeListener exchangeListener = new ChromeRDPExchangeListener() {
        @Override public void received(String messageText) throws IOException {
            receiveMessage(messageText);
        }
    };

    private void receiveMessage(final String messageText) {
        new Thread() {
            @Override public void run() {
                try {
                    JsonNode jsonMessage = mapper.readTree(messageText);
                    if (jsonMessage.has("id")) {
                        Response response = mapper.reader(Response.class).readValue(jsonMessage);
                        if (response.getError() != null) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Error message #{} received from browser: {}", jsonMessage.get("id"),
                                        response.getError().toString());
                            }
                        }
                        responseHandlers.remove(response.getId()).received(response.getResult());
                    } else {
                        Message message = mapper.reader(Message.class).readValue(messageText);
                        if (message.getMethod() == null) {
                            return;
                        }
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
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Error receiving message from Google Chrome", e);
                    }
                }
            }
        }.start();
    }

    private synchronized void firePaused(SuspendedNotification params) {
        suspended = true;
        CallFrameDTO[] callFrameDTOs = params.getCallFrames();
        RDPCallFrame[] callStack = new RDPCallFrame[callFrameDTOs.length];
        for (int i = 0; i < callStack.length; ++i) {
            callStack[i] = map(callFrameDTOs[i]);
        }
        this.callStack = callStack;
        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.paused();
        }
    }

    private synchronized void fireResumed() {
        suspended = false;
        callStack = null;
        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.resumed();
        }
    }

    private synchronized void scriptParsed(ScriptParsedNotification params) {
        if (scripts.putIfAbsent(params.getScriptId(), params.getUrl()) != null) {
            return;
        }
        scriptIds.put(params.getUrl(), params.getScriptId());
        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.scriptAdded(params.getUrl());
        }
    }


    @Override
    public void addListener(JavaScriptDebuggerListener listener) {
        listeners.put(listener, dummy);
    }

    @Override
    public void removeListener(JavaScriptDebuggerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void suspend() {
        if (exchange == null) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.pause");
        sendMessage(message);
    }

    @Override
    public void resume() {
        if (exchange == null) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.resume");
        sendMessage(message);
    }

    @Override
    public void stepInto() {
        if (exchange == null) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepInto");
        sendMessage(message);
    }

    @Override
    public void stepOut() {
        if (exchange == null) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepOut");
        sendMessage(message);
    }

    @Override
    public void stepOver() {
        if (exchange == null) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.stepOver");
        sendMessage(message);
    }

    @Override
    public void continueToLocation(JavaScriptLocation location) {
        if (exchange == null) {
            return;
        }
        Message message = new Message();
        message.setMethod("Debugger.continueToLocation");
        ContinueToLocationCommand params = new ContinueToLocationCommand();
        params.setLocation(unmap(location));
        message.setParams(mapper.valueToTree(params));
        sendMessage(message);
    }

    @Override
    public boolean isSuspended() {
        return exchange != null && suspended;
    }

    @Override
    public boolean isAttached() {
        return exchange != null;
    }

    @Override
    public void detach() {
        if (exchange != null) {
            exchange.disconnect();
        }
    }

    @Override
    public JavaScriptCallFrame[] getCallStack() {
        if (exchange == null) {
            return null;
        }
        JavaScriptCallFrame[] callStack = this.callStack;
        return callStack != null ? callStack.clone() : null;
    }

    @Override
    public JavaScriptBreakpoint createBreakpoint(JavaScriptLocation location) {
        RDPBreakpoint breakpoint;

        breakpointLock.lock();
        try {
            breakpoint = breakpointLocationMap.get(location);
            if (breakpoint == null) {
                breakpoint = new RDPBreakpoint(this, location);
                breakpointLocationMap.put(location, breakpoint);
                updateBreakpoint(breakpoint);
            }
            breakpoint.referenceCount.incrementAndGet();
            breakpoints.put(breakpoint, dummy);
        } finally {
            breakpointLock.unlock();
        }

        return breakpoint;
    }

    void destroyBreakpoint(RDPBreakpoint breakpoint) {
        if (breakpoint.referenceCount.decrementAndGet() > 0) {
            return;
        }
        breakpointLock.lock();
        try {
            if (breakpoint.referenceCount.get() > 0) {
                return;
            }
            breakpointLocationMap.remove(breakpoint.getLocation());
            breakpoints.remove(breakpoint);
            if (breakpoint.chromeId != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Removing breakpoint at {}", breakpoint.getLocation());
                }
                Message message = new Message();
                message.setMethod("Debugger.removeBreakpoint");
                RemoveBreakpointCommand params = new RemoveBreakpointCommand();
                params.setBreakpointId(breakpoint.chromeId);
                message.setParams(mapper.valueToTree(params));
                sendMessage(message);
            }
            breakpoint.debugger = null;
            breakpoint.chromeId = null;
        } finally {
            breakpointLock.unlock();
        }
    }

    void fireScriptAdded(String script) {
        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.scriptAdded(script);
        }
    }

    void updateBreakpoint(final RDPBreakpoint breakpoint) {
        if (exchange == null || breakpoint.chromeId != null) {
            return;
        }
        final Message message = new Message();
        message.setId(messageIdGenerator.incrementAndGet());
        message.setMethod("Debugger.setBreakpoint");
        SetBreakpointCommand params = new SetBreakpointCommand();
        params.setLocation(unmap(breakpoint.getLocation()));
        message.setParams(mapper.valueToTree(params));
        if (logger.isInfoEnabled()) {
            logger.info("Setting breakpoint at {}, message id is ", breakpoint.getLocation(), message.getId());
        }
        ResponseHandler handler = new ResponseHandler() {
            @Override public void received(JsonNode node) throws IOException {
                if (node != null) {
                    SetBreakpointResponse response = mapper.reader(SetBreakpointResponse.class).readValue(node);
                    breakpoint.chromeId = response.getBreakpointId();
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Error setting breakpoint at {}, message id is {}",
                                breakpoint.getLocation(), message.getId());
                    }
                    breakpoint.chromeId = null;
                }
                for (JavaScriptDebuggerListener listener : getListeners()) {
                    listener.breakpointChanged(breakpoint);
                }
            }
        };
        responseHandlers.put(message.getId(), handler);
        sendMessage(message);
    }

    List<RDPLocalVariable> getScope(String scopeId) {
        if (exchange == null) {
            return Collections.emptyList();
        }
        Message message = new Message();
        message.setId(messageIdGenerator.incrementAndGet());
        message.setMethod("Runtime.getProperties");
        GetPropertiesCommand params = new GetPropertiesCommand();
        params.setObjectId(scopeId);
        params.setOwnProperties(true);
        message.setParams(mapper.valueToTree(params));
        final BlockingQueue<List<RDPLocalVariable>> sync = new LinkedTransferQueue<>();
        responseHandlers.put(message.getId(), new ResponseHandler() {
            @Override public void received(JsonNode node) throws IOException {
                GetPropertiesResponse response = mapper.reader(GetPropertiesResponse.class).readValue(node);
                sync.add(parseProperties(response.getResult()));
            }
        });
        sendMessage(message);
        try {
            return sync.take();
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
    }

    String getClassName(String objectId) {
        if (exchange == null) {
            return null;
        }
        Message message = new Message();
        message.setId(messageIdGenerator.incrementAndGet());
        message.setMethod("Runtime.callFunctionOn");
        CallFunctionCommand params = new CallFunctionCommand();
        CallArgumentDTO arg = new CallArgumentDTO();
        arg.setObjectId(objectId);
        params.setObjectId(objectId);
        params.setArguments(new CallArgumentDTO[] { arg });
        params.setFunctionDeclaration("$dbg_class");
        message.setParams(mapper.valueToTree(params));
        final BlockingQueue<String> sync = new LinkedTransferQueue<>();
        responseHandlers.put(message.getId(), new ResponseHandler() {
            @Override public void received(JsonNode node) throws IOException {
                if (node == null) {
                    sync.add("");
                } else {
                    CallFunctionResponse response = mapper.reader(CallFunctionResponse.class).readValue(node);
                    RemoteObjectDTO result = response.getResult();
                    sync.add(result.getValue() != null ? result.getValue().getTextValue() : "");
                }
            }
        });
        sendMessage(message);
        try {
            String result = sync.take();
            return result.isEmpty() ? null : result;
        } catch (InterruptedException e) {
            return null;
        }
    }

    String getRepresentation(String objectId) {
        if (exchange == null) {
            return null;
        }
        Message message = new Message();
        message.setId(messageIdGenerator.incrementAndGet());
        message.setMethod("Runtime.callFunctionOn");
        CallFunctionCommand params = new CallFunctionCommand();
        CallArgumentDTO arg = new CallArgumentDTO();
        arg.setObjectId(objectId);
        params.setObjectId(objectId);
        params.setArguments(new CallArgumentDTO[] { arg });
        params.setFunctionDeclaration("$dbg_repr");
        message.setParams(mapper.valueToTree(params));
        final BlockingQueue<RepresentationWrapper> sync = new LinkedTransferQueue<>();
        responseHandlers.put(message.getId(), new ResponseHandler() {
            @Override public void received(JsonNode node) throws IOException {
                if (node == null) {
                    sync.add(new RepresentationWrapper(null));
                } else {
                    CallFunctionResponse response = mapper.reader(CallFunctionResponse.class).readValue(node);
                    RemoteObjectDTO result = response.getResult();
                    sync.add(new RepresentationWrapper(result.getValue() != null
                            ? result.getValue().getTextValue() : null));
                }
            }
        });
        sendMessage(message);
        try {
            RepresentationWrapper result = sync.take();
            return result.repr;
        } catch (InterruptedException e) {
            return null;
        }
    }

    private static class RepresentationWrapper {
        String repr;

        public RepresentationWrapper(String repr) {
            super();
            this.repr = repr;
        }
    }

    private List<RDPLocalVariable> parseProperties(PropertyDescriptorDTO[] properties) {
        List<RDPLocalVariable> variables = new ArrayList<>();
        if (properties != null) {
            for (PropertyDescriptorDTO property : properties) {
                RemoteObjectDTO remoteValue = property.getValue();
                RDPValue value;
                if (remoteValue != null && remoteValue.getType() != null) {
                    switch (remoteValue.getType()) {
                        case "undefined":
                            value = new RDPValue(this, "undefined", "undefined", null, false);
                            break;
                        case "object":
                        case "function":
                            value = new RDPValue(this, null, remoteValue.getType(), remoteValue.getObjectId(), true);
                            break;
                        default:
                            value = new RDPValue(this, remoteValue.getValue().asText(), remoteValue.getType(),
                                    remoteValue.getObjectId(), false);
                            break;
                    }
                } else {
                    value = new RDPValue(this, "null", "null", "null", false);
                }

                RDPLocalVariable var = new RDPLocalVariable(property.getName(), value);
                variables.add(var);
            }
        }
        return variables;
    }

    private <T> T parseJson(Class<T> type, JsonNode node) throws IOException {
        return mapper.reader(type).readValue(node);
    }

    private void sendMessage(Message message) {
        if (exchange == null) {
            return;
        }
        try {
            exchange.send(mapper.writer().writeValueAsString(message));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    RDPCallFrame map(CallFrameDTO dto) {
        String scopeId = null;
        RDPValue thisObject = null;
        RDPValue closure = null;
        for (ScopeDTO scope : dto.getScopeChain()) {
            if (scope.getType().equals("local")) {
                scopeId = scope.getObject().getObjectId();
            } else if (scope.getType().equals("closure")) {
                closure = new RDPValue(this, scope.getObject().getDescription(), scope.getObject().getType(),
                        scope.getObject().getObjectId(), true);
            } else if (scope.getType().equals("global")) {
                thisObject = new RDPValue(this, scope.getObject().getDescription(), scope.getObject().getType(),
                        scope.getObject().getObjectId(), true);
            }
        }
        return new RDPCallFrame(this, dto.getCallFrameId(), map(dto.getLocation()), new RDPScope(this, scopeId),
                thisObject, closure);
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

    interface ResponseHandler {
        void received(JsonNode node) throws IOException;
    }
}
