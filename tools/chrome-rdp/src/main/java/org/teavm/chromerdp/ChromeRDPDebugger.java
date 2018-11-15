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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.chromerdp.data.CallArgumentDTO;
import org.teavm.chromerdp.data.CallFrameDTO;
import org.teavm.chromerdp.data.LocationDTO;
import org.teavm.chromerdp.data.Message;
import org.teavm.chromerdp.data.PropertyDescriptorDTO;
import org.teavm.chromerdp.data.RemoteObjectDTO;
import org.teavm.chromerdp.data.Response;
import org.teavm.chromerdp.data.ScopeDTO;
import org.teavm.chromerdp.messages.CallFunctionCommand;
import org.teavm.chromerdp.messages.CallFunctionResponse;
import org.teavm.chromerdp.messages.CompileScriptCommand;
import org.teavm.chromerdp.messages.CompileScriptResponse;
import org.teavm.chromerdp.messages.ContinueToLocationCommand;
import org.teavm.chromerdp.messages.GetPropertiesCommand;
import org.teavm.chromerdp.messages.GetPropertiesResponse;
import org.teavm.chromerdp.messages.RemoveBreakpointCommand;
import org.teavm.chromerdp.messages.RunScriptCommand;
import org.teavm.chromerdp.messages.ScriptParsedNotification;
import org.teavm.chromerdp.messages.SetBreakpointCommand;
import org.teavm.chromerdp.messages.SetBreakpointResponse;
import org.teavm.chromerdp.messages.SuspendedNotification;
import org.teavm.debugging.javascript.JavaScriptBreakpoint;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;
import org.teavm.debugging.javascript.JavaScriptDebuggerListener;
import org.teavm.debugging.javascript.JavaScriptLocation;

public class ChromeRDPDebugger implements JavaScriptDebugger, ChromeRDPExchangeConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ChromeRDPDebugger.class);
    private static final Object dummy = new Object();
    private ChromeRDPExchange exchange;
    private ConcurrentMap<JavaScriptDebuggerListener, Object> listeners = new ConcurrentHashMap<>();
    private ConcurrentMap<JavaScriptLocation, RDPBreakpoint> breakpointLocationMap = new ConcurrentHashMap<>();
    private ConcurrentMap<RDPBreakpoint, Object> breakpoints = new ConcurrentHashMap<>();
    private ConcurrentMap<String, RDPBreakpoint> breakpointsByChromeId = new ConcurrentHashMap<>();
    private volatile RDPCallFrame[] callStack = new RDPCallFrame[0];
    private ConcurrentMap<String, String> scripts = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> scriptIds = new ConcurrentHashMap<>();
    private boolean suspended;
    private ObjectMapper mapper = new ObjectMapper();
    private ConcurrentMap<Integer, ResponseHandler<Object>> responseHandlers = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, CompletableFuture<Object>> futures = new ConcurrentHashMap<>();
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

    private void injectFunctions(int contextId) {
        callMethod("Runtime.enable", void.class, null);

        CompileScriptCommand compileParams = new CompileScriptCommand();
        compileParams.expression = "$dbg_class = function(obj) { return typeof obj === 'object' && obj != null "
                + "? obj.__teavm_class__() : null };";
        compileParams.sourceURL = "file://fake";
        compileParams.persistScript = true;
        compileParams.executionContextId = contextId;
        CompileScriptResponse response = callMethod("Runtime.compileScript", CompileScriptResponse.class,
                compileParams);

        RunScriptCommand runParams = new RunScriptCommand();
        runParams.scriptId = response.scriptId;
        callMethod("Runtime.runScript", void.class, runParams);
    }

    private ChromeRDPExchangeListener exchangeListener = this::receiveMessage;

    private void receiveMessage(String messageText) {
        new Thread(() -> {
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
                    CompletableFuture<Object> future = futures.remove(response.getId());
                    try {
                        responseHandlers.remove(response.getId()).received(response.getResult(), future);
                    } catch (RuntimeException e) {
                        logger.warn("Error processing message ${}", response.getId(), e);
                        future.completeExceptionally(e);
                    }
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
        }).start();
    }

    private synchronized void firePaused(SuspendedNotification params) {
        suspended = true;
        CallFrameDTO[] callFrameDTOs = params.getCallFrames();
        RDPCallFrame[] callStack = new RDPCallFrame[callFrameDTOs.length];
        for (int i = 0; i < callStack.length; ++i) {
            callStack[i] = map(callFrameDTOs[i]);
        }
        this.callStack = callStack;

        RDPBreakpoint breakpoint = null;
        if (params.getHitBreakpoints() != null && !params.getHitBreakpoints().isEmpty()) {
            breakpoint = breakpointsByChromeId.get(params.getHitBreakpoints().get(0));
        }

        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.paused(breakpoint);
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
        injectFunctions(params.getExecutionContextId());
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
        callMethod("Debugger.pause", void.class, null);
    }

    @Override
    public void resume() {
        callMethod("Debugger.resume", void.class, null);
    }

    @Override
    public void stepInto() {
        callMethod("Debugger.stepInto", void.class, null);
    }

    @Override
    public void stepOut() {
        callMethod("Debugger.stepOut", void.class, null);
    }

    @Override
    public void stepOver() {
        callMethod("Debugger.stepOver", void.class, null);
    }

    @Override
    public void continueToLocation(JavaScriptLocation location) {
        ContinueToLocationCommand params = new ContinueToLocationCommand();
        params.setLocation(unmap(location));
        callMethod("Debugger.continueToLocation", void.class, params);
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

            if (breakpoint.chromeId == null) {
                synchronized (breakpoint.updateMonitor) {
                    while (breakpoint.updating.get()) {
                        try {
                            breakpoint.updateMonitor.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }

            if (breakpoint.chromeId != null) {
                breakpointsByChromeId.remove(breakpoint.chromeId);
                if (logger.isInfoEnabled()) {
                    logger.info("Removing breakpoint at {}", breakpoint.getLocation());
                }
                RemoveBreakpointCommand params = new RemoveBreakpointCommand();
                params.setBreakpointId(breakpoint.chromeId);
                callMethod("Debugger.removeBreakpoint", void.class, params);
            }
            breakpoint.debugger = null;
            breakpoint.chromeId = null;
        } finally {
            breakpointLock.unlock();
        }
    }

    private void updateBreakpoint(final RDPBreakpoint breakpoint) {
        if (breakpoint.chromeId != null) {
            return;
        }
        SetBreakpointCommand params = new SetBreakpointCommand();
        params.setLocation(unmap(breakpoint.getLocation()));

        if (logger.isInfoEnabled()) {
            logger.info("Setting breakpoint at {}", breakpoint.getLocation());
        }

        breakpoint.updating.set(true);
        try {
            SetBreakpointResponse response = callMethod("Debugger.setBreakpoint", SetBreakpointResponse.class, params);
            if (response == null) {
                breakpoint.chromeId = response.getBreakpointId();
                if (breakpoint.chromeId != null) {
                    breakpointsByChromeId.put(breakpoint.chromeId, breakpoint);
                }
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("Error setting breakpoint at {}", breakpoint.getLocation());
                }
                breakpoint.chromeId = null;
            }
        } finally {
            synchronized (breakpoint.updateMonitor) {
                breakpoint.updating.set(false);
                breakpoint.updateMonitor.notifyAll();
            }
        }

        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.breakpointChanged(breakpoint);
        }
    }

    List<RDPLocalVariable> getScope(String scopeId) {
        GetPropertiesCommand params = new GetPropertiesCommand();
        params.setObjectId(scopeId);
        params.setOwnProperties(true);

        GetPropertiesResponse response = callMethod("Runtime.getProperties", GetPropertiesResponse.class, params);
        if (response == null) {
            return Collections.emptyList();
        }

        return parseProperties(response.getResult());
    }

    String getClassName(String objectId) {
        CallFunctionCommand params = new CallFunctionCommand();
        CallArgumentDTO arg = new CallArgumentDTO();
        arg.setObjectId(objectId);
        params.setObjectId(objectId);
        params.setArguments(new CallArgumentDTO[] { arg });
        params.setFunctionDeclaration("$dbg_class");

        CallFunctionResponse response = callMethod("Runtime.callFunctionOn", CallFunctionResponse.class, params);
        RemoteObjectDTO result = response != null ? response.getResult() : null;
        return result.getValue() != null ? result.getValue().getTextValue() : null;
    }

    String getRepresentation(String objectId) {
        CallFunctionCommand params = new CallFunctionCommand();
        CallArgumentDTO arg = new CallArgumentDTO();
        arg.setObjectId(objectId);
        params.setObjectId(objectId);
        params.setArguments(new CallArgumentDTO[] { arg });
        params.setFunctionDeclaration("$dbg_repr");

        CallFunctionResponse response = callMethod("Runtime.callFunctionOn", CallFunctionResponse.class, params);
        RemoteObjectDTO result = response != null ? response.getResult() : null;
        return result.getValue() != null ? result.getValue().getTextValue() : null;
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

    private RDPCallFrame map(CallFrameDTO dto) {
        String scopeId = null;
        RDPValue thisObject = null;
        RDPValue closure = null;
        for (ScopeDTO scope : dto.getScopeChain()) {
            switch (scope.getType()) {
                case "local":
                    scopeId = scope.getObject().getObjectId();
                    break;
                case "closure":
                    closure = new RDPValue(this, scope.getObject().getDescription(), scope.getObject().getType(),
                            scope.getObject().getObjectId(), true);
                    break;
                case "global":
                    thisObject = new RDPValue(this, scope.getObject().getDescription(), scope.getObject().getType(),
                            scope.getObject().getObjectId(), true);
                    break;
            }
        }
        return new RDPCallFrame(this, dto.getCallFrameId(), map(dto.getLocation()), new RDPScope(this, scopeId),
                thisObject, closure);
    }

    private JavaScriptLocation map(LocationDTO dto) {
        return new JavaScriptLocation(scripts.get(dto.getScriptId()), dto.getLineNumber(), dto.getColumnNumber());
    }

    private LocationDTO unmap(JavaScriptLocation location) {
        LocationDTO dto = new LocationDTO();
        dto.setScriptId(scriptIds.get(location.getScript()));
        dto.setLineNumber(location.getLine());
        dto.setColumnNumber(location.getColumn());
        return dto;
    }

    private <R> R callMethod(String method, Class<R> returnType, Object params) {
        if (exchange == null) {
            return null;
        }
        Message message = new Message();
        message.setId(messageIdGenerator.incrementAndGet());
        message.setMethod(method);
        if (params != null) {
            message.setParams(mapper.valueToTree(params));
        }

        CompletableFuture<R> sync = setResponseHandler(message.getId(), (node, out) -> {
            if (node == null) {
                out.complete(null);
            } else {
                R response = returnType != void.class ? mapper.reader(returnType).readValue(node) : null;
                out.complete(response);
            }
        });
        sendMessage(message);
        try {
            return read(sync);
        } catch (InterruptedException e) {
            return null;
        } catch (TimeoutException e) {
            logger.warn("Chrome debug protocol: timed out", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> setResponseHandler(int messageId, ResponseHandler<T> handler) {
        CompletableFuture<T> future = new CompletableFuture<>();
        futures.put(messageId, (CompletableFuture<Object>) future);
        responseHandlers.put(messageId, (ResponseHandler<Object>) handler);
        return future;
    }

    interface ResponseHandler<T> {
        void received(JsonNode node, CompletableFuture<T> out) throws IOException;
    }

    private static <T> T read(Future<T> future) throws InterruptedException, TimeoutException {
        try {
            return future.get(1500, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }
}
