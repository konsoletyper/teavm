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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
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
import org.teavm.common.CompletablePromise;
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptBreakpoint;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;
import org.teavm.debugging.javascript.JavaScriptDebuggerListener;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.debugging.javascript.JavaScriptVariable;

public class ChromeRDPDebugger implements JavaScriptDebugger, ChromeRDPExchangeConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ChromeRDPDebugger.class);
    private static final Promise<Map<String, ? extends JavaScriptVariable>> EMPTY_SCOPE =
            Promise.of(Collections.emptyMap());
    private volatile ChromeRDPExchange exchange;
    private Set<JavaScriptDebuggerListener> listeners = new LinkedHashSet<>();
    private Map<JavaScriptLocation, RDPNativeBreakpoint> breakpointLocationMap = new HashMap<>();
    private Set<RDPBreakpoint> breakpoints = new LinkedHashSet<>();
    private Map<String, RDPNativeBreakpoint> breakpointsByChromeId = new HashMap<>();
    private volatile RDPCallFrame[] callStack = new RDPCallFrame[0];
    private Map<String, String> scripts = new HashMap<>();
    private Map<String, String> scriptIds = new HashMap<>();
    private volatile boolean suspended;
    private ObjectMapper mapper = new ObjectMapper();
    private ConcurrentMap<Integer, ResponseHandler<Object>> responseHandlers = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, CompletablePromise<Object>> promises = new ConcurrentHashMap<>();
    private AtomicInteger messageIdGenerator = new AtomicInteger();
    private Promise<Void> runtimeEnabledPromise;

    private List<JavaScriptDebuggerListener> getListeners() {
        return new ArrayList<>(listeners);
    }

    private Executor executor;

    public ChromeRDPDebugger(Executor executor) {
        this.executor = executor;
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
            for (RDPBreakpoint breakpoint : breakpoints.toArray(new RDPBreakpoint[0])) {
                updateBreakpoint(breakpoint.nativeBreakpoint);
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

    private Promise<Void> injectFunctions(int contextId) {
        return enableRuntime()
                .thenAsync(v -> {
                    CompileScriptCommand compileParams = new CompileScriptCommand();
                    compileParams.expression = "$dbg_class = function(obj) { return typeof obj === 'object' "
                            + "&& obj !== null && '__teavm_class__' in obj ? obj.__teavm_class__() : null; };\n"
                            + "$dbg_repr = function(obj) { return typeof obj === 'object' "
                            + "&& obj !== null && 'toString' in obj ? obj.toString() : null; }\n";
                    compileParams.sourceURL = "file://fake";
                    compileParams.persistScript = true;
                    compileParams.executionContextId = contextId;
                    return callMethodAsync("Runtime.compileScript", CompileScriptResponse.class, compileParams);
                })
                .thenAsync(response -> {
                    RunScriptCommand runParams = new RunScriptCommand();
                    runParams.scriptId = response.scriptId;
                    return callMethodAsync("Runtime.runScript", void.class, runParams);
                });
    }

    private Promise<Void> enableRuntime() {
        if (runtimeEnabledPromise == null) {
            runtimeEnabledPromise = callMethodAsync("Runtime.enable", void.class, null);
        }
        return runtimeEnabledPromise;
    }

    private ChromeRDPExchangeListener exchangeListener = messageText -> {
        callInExecutor(() -> receiveMessage(messageText)
            .catchError(e -> {
                logger.error("Error handling message", e);
                return null;
            }));
    };

    private Promise<Void> receiveMessage(String messageText) {
        try {
            JsonNode jsonMessage = mapper.readTree(messageText);
            if (jsonMessage.has("id")) {
                Response response = mapper.readerFor(Response.class).readValue(jsonMessage);
                if (response.getError() != null) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Error message #{} received from browser: {}", jsonMessage.get("id"),
                                response.getError().toString());
                    }
                }
                CompletablePromise<Object> promise = promises.remove(response.getId());
                try {
                    responseHandlers.remove(response.getId()).received(response.getResult(), promise);
                } catch (RuntimeException e) {
                    logger.warn("Error processing message ${}", response.getId(), e);
                    promise.completeWithError(e);
                }
                return Promise.VOID;
            } else {
                Message message = mapper.readerFor(Message.class).readValue(messageText);
                if (message.getMethod() == null) {
                    return Promise.VOID;
                }
                switch (message.getMethod()) {
                    case "Debugger.paused":
                        return firePaused(parseJson(SuspendedNotification.class, message.getParams()));
                    case "Debugger.resumed":
                        return fireResumed();
                    case "Debugger.scriptParsed":
                        return scriptParsed(parseJson(ScriptParsedNotification.class, message.getParams()));
                }
                return Promise.VOID;
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error receiving message from Google Chrome", e);
            }
            return Promise.VOID;
        }
    }

    private Promise<Void> firePaused(SuspendedNotification params) {
        suspended = true;
        CallFrameDTO[] callFrameDTOs = params.getCallFrames();
        RDPCallFrame[] callStack = new RDPCallFrame[callFrameDTOs.length];
        for (int i = 0; i < callStack.length; ++i) {
            callStack[i] = map(callFrameDTOs[i]);
        }
        this.callStack = callStack;

        RDPNativeBreakpoint nativeBreakpoint = null;
        if (params.getHitBreakpoints() != null && !params.getHitBreakpoints().isEmpty()) {
            nativeBreakpoint = breakpointsByChromeId.get(params.getHitBreakpoints().get(0));
        }
        RDPBreakpoint breakpoint = !nativeBreakpoint.breakpoints.isEmpty()
                ? nativeBreakpoint.breakpoints.iterator().next()
                : null;

        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.paused(breakpoint);
        }

        return Promise.VOID;
    }

    private Promise<Void> fireResumed() {
        suspended = false;
        callStack = null;
        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.resumed();
        }

        return Promise.VOID;
    }

    private Promise<Void> scriptParsed(ScriptParsedNotification params) {
        if (params.getUrl() == null) {
            return Promise.VOID;
        }
        if (scripts.putIfAbsent(params.getScriptId(), params.getUrl()) != null) {
            return Promise.VOID;
        }
        if (params.getUrl().equals("file://fake")) {
            return Promise.VOID;
        }
        scriptIds.put(params.getUrl(), params.getScriptId());
        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.scriptAdded(params.getUrl());
        }
        return injectFunctions(params.getExecutionContextId());
    }

    @Override
    public void addListener(JavaScriptDebuggerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(JavaScriptDebuggerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Promise<Void> suspend() {
        return callMethodAsync("Debugger.pause", void.class, null);
    }

    @Override
    public Promise<Void> resume() {
        return callMethodAsync("Debugger.resume", void.class, null);
    }

    @Override
    public Promise<Void> stepInto() {
        return callMethodAsync("Debugger.stepInto", void.class, null);
    }

    @Override
    public Promise<Void> stepOut() {
        return callMethodAsync("Debugger.stepOut", void.class, null);
    }

    @Override
    public Promise<Void> stepOver() {
        return callMethodAsync("Debugger.stepOver", void.class, null);
    }

    @Override
    public Promise<Void> continueToLocation(JavaScriptLocation location) {
        ContinueToLocationCommand params = new ContinueToLocationCommand();
        params.setLocation(unmap(location));
        return callMethodAsync("Debugger.continueToLocation", void.class, params);
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
    public Promise<JavaScriptBreakpoint> createBreakpoint(JavaScriptLocation location) {
        RDPBreakpoint breakpoint = new RDPBreakpoint(this);
        breakpoint.nativeBreakpoint = lockNativeBreakpoint(location, breakpoint);
        CompletablePromise<JavaScriptBreakpoint> result = new CompletablePromise<>();
        breakpoints.add(breakpoint);
        breakpoint.nativeBreakpoint.initPromise.thenVoid(v -> result.complete(breakpoint));
        return result;
    }

    Promise<Void> destroyBreakpoint(RDPBreakpoint breakpoint) {
        if (breakpoint.nativeBreakpoint == null) {
            return Promise.VOID;
        }
        RDPNativeBreakpoint nativeBreakpoint = breakpoint.nativeBreakpoint;
        breakpoint.nativeBreakpoint = null;
        nativeBreakpoint.breakpoints.remove(breakpoint);
        return releaseNativeBreakpoint(nativeBreakpoint, breakpoint);
    }

    private RDPNativeBreakpoint lockNativeBreakpoint(JavaScriptLocation location, RDPBreakpoint bp) {
        RDPNativeBreakpoint breakpoint;

        breakpoint = breakpointLocationMap.get(location);
        if (breakpoint != null) {
            breakpoint.breakpoints.add(bp);
            return breakpoint;
        }

        breakpoint = new RDPNativeBreakpoint(this, location);
        breakpoint.breakpoints.add(bp);
        breakpointLocationMap.put(location, breakpoint);
        RDPNativeBreakpoint finalBreakpoint = breakpoint;
        breakpoint.initPromise = updateBreakpoint(breakpoint).then(v -> {
            checkBreakpoint(finalBreakpoint);
            return null;
        });
        return breakpoint;
    }

    private Promise<Void> releaseNativeBreakpoint(RDPNativeBreakpoint breakpoint, RDPBreakpoint bp) {
        breakpoint.breakpoints.remove(bp);
        return checkBreakpoint(breakpoint);
    }

    private Promise<Void> checkBreakpoint(RDPNativeBreakpoint breakpoint) {
        if (!breakpoint.breakpoints.isEmpty()) {
            return Promise.VOID;
        }
        if (breakpointLocationMap.get(breakpoint.getLocation()) == breakpoint) {
            breakpointLocationMap.remove(breakpoint.getLocation());
        }
        if (breakpoint.destroyPromise == null) {
            breakpoint.destroyPromise = breakpoint.initPromise.thenAsync(v -> {
                breakpointsByChromeId.remove(breakpoint.chromeId);
                if (logger.isInfoEnabled()) {
                    logger.info("Removing breakpoint at {}", breakpoint.getLocation());
                }
                RemoveBreakpointCommand params = new RemoveBreakpointCommand();
                params.setBreakpointId(breakpoint.chromeId);
                return callMethodAsync("Debugger.removeBreakpoint", void.class, params);
            });
            breakpoint.debugger = null;
        }
        return breakpoint.destroyPromise;
    }

    private Promise<Void> updateBreakpoint(RDPNativeBreakpoint breakpoint) {
        if (breakpoint.chromeId != null) {
            return Promise.VOID;
        }
        SetBreakpointCommand params = new SetBreakpointCommand();
        params.setLocation(unmap(breakpoint.getLocation()));

        if (logger.isInfoEnabled()) {
            logger.info("Setting breakpoint at {}", breakpoint.getLocation());
        }

        return callMethodAsync("Debugger.setBreakpoint", SetBreakpointResponse.class, params)
            .thenVoid(response -> {
                if (response != null) {
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

                for (RDPBreakpoint bp : breakpoint.breakpoints) {
                    for (JavaScriptDebuggerListener listener : getListeners()) {
                        listener.breakpointChanged(bp);
                    }
                }
            });
    }

    Promise<List<RDPLocalVariable>> getScope(String scopeId) {
        GetPropertiesCommand params = new GetPropertiesCommand();
        params.setObjectId(scopeId);
        params.setOwnProperties(true);

        return callMethodAsync("Runtime.getProperties", GetPropertiesResponse.class, params)
                .thenAsync(response -> {
                    if (response == null) {
                        return Promise.of(Collections.emptyList());
                    }

                    PropertyDescriptorDTO proto = Arrays.asList(response.getResult()).stream()
                            .filter(p -> p.getName().equals("__proto__"))
                            .findAny()
                            .orElse(null);
                    if (proto == null || proto.getValue() == null || proto.getValue().getObjectId() == null) {
                        return Promise.of(parseProperties(scopeId, response.getResult(), null));
                    }

                    GetPropertiesCommand protoParams = new GetPropertiesCommand();
                    protoParams.setObjectId(proto.getValue().getObjectId());
                    protoParams.setOwnProperties(false);

                    return callMethodAsync("Runtime.getProperties", GetPropertiesResponse.class, protoParams)
                            .then(protoProperties -> {
                                PropertyDescriptorDTO[] getters = Arrays.asList(protoProperties.getResult()).stream()
                                        .filter(p -> p.getGetter() != null && p.getValue() == null
                                                && !p.getName().equals("__proto__"))
                                        .toArray(PropertyDescriptorDTO[]::new);
                                return parseProperties(scopeId, response.getResult(), getters);
                            });
                });
    }

    Promise<String> getClassName(String objectId) {
        CallFunctionCommand params = new CallFunctionCommand();
        CallArgumentDTO arg = new CallArgumentDTO();
        arg.setObjectId(objectId);
        params.setObjectId(objectId);
        params.setArguments(new CallArgumentDTO[] { arg });
        params.setFunctionDeclaration("$dbg_class");

        return callMethodAsync("Runtime.callFunctionOn", CallFunctionResponse.class, params)
                .then(response -> {
                    RemoteObjectDTO result = response != null ? response.getResult() : null;
                    return result.getValue() != null ? result.getValue().textValue() : null;
                });
    }

    Promise<String> getRepresentation(String objectId) {
        CallFunctionCommand params = new CallFunctionCommand();
        CallArgumentDTO arg = new CallArgumentDTO();
        arg.setObjectId(objectId);
        params.setObjectId(objectId);
        params.setArguments(new CallArgumentDTO[] { arg });
        params.setFunctionDeclaration("$dbg_repr");

        return callMethodAsync("Runtime.callFunctionOn", CallFunctionResponse.class, params)
                .then(response -> {
                    RemoteObjectDTO result = response != null ? response.getResult() : null;
                    return result.getValue() != null ? result.getValue().textValue() : null;
                });
    }

    private List<RDPLocalVariable> parseProperties(String scopeId, PropertyDescriptorDTO[] properties,
            PropertyDescriptorDTO[] getters) {
        List<RDPLocalVariable> variables = new ArrayList<>();
        if (properties != null) {
            for (PropertyDescriptorDTO property : properties) {
                RemoteObjectDTO remoteValue = property.getValue();
                RemoteObjectDTO getter = property.getGetter();
                RDPValue value;
                if (remoteValue != null && remoteValue.getType() != null) {
                    value = mapValue(remoteValue);
                } else if (getter != null && getter.getObjectId() != null) {
                    value = mapValue(getter);
                } else {
                    value = new RDPValue(this, "null", "null", null, false);
                }

                RDPLocalVariable var = new RDPLocalVariable(property.getName(), value);
                variables.add(var);
            }
        }
        if (getters != null) {
            for (PropertyDescriptorDTO property : getters) {
                RDPValue value = new RDPValue(this, "<get>", "@Function", scopeId, true);
                value.getter = property.getGetter();
                RDPLocalVariable var = new RDPLocalVariable(property.getName(), value);
                variables.add(var);
            }
        }
        return variables;
    }

    Promise<RDPValue> invokeGetter(String functionId, String objectId) {
        CallFunctionCommand params = new CallFunctionCommand();
        params.setObjectId(functionId);

        CallArgumentDTO functionArg = new CallArgumentDTO();
        functionArg.setObjectId(functionId);
        CallArgumentDTO arg = new CallArgumentDTO();
        arg.setObjectId(objectId);

        params.setArguments(new CallArgumentDTO[] { arg });
        params.setFunctionDeclaration("Function.prototype.call");

        return callMethodAsync("Runtime.callFunctionOn", CallFunctionResponse.class, params)
                .then(response -> {
                    RemoteObjectDTO result = response != null ? response.getResult() : null;
                    return result.getValue() != null ? mapValue(result) : null;
                });
    }

    RDPValue mapValue(RemoteObjectDTO remoteValue) {
        switch (remoteValue.getType()) {
            case "undefined":
                return new RDPValue(this, "undefined", "undefined", null, false);
            case "object":
            case "function":
                if (remoteValue.getValue() instanceof NullNode) {
                    return new RDPValue(this, "null", "null", null, false);
                } else {
                    return new RDPValue(this, null, remoteValue.getType(), remoteValue.getObjectId(),
                            true);
                }
            default:
                return new RDPValue(this, remoteValue.getValue().asText(), remoteValue.getType(),
                        remoteValue.getObjectId(), false);
        }
    }

    private <T> T parseJson(Class<T> type, JsonNode node) throws IOException {
        return mapper.readerFor(type).readValue(node);
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
        return new RDPCallFrame(this, dto.getCallFrameId(), map(dto.getLocation()), scopeId,
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

    private <R> Promise<R> callMethodAsync(String method, Class<R> returnType, Object params) {
        if (exchange == null) {
            return Promise.of(null);
        }
        Message message = new Message();
        message.setId(messageIdGenerator.incrementAndGet());
        message.setMethod(method);
        if (params != null) {
            message.setParams(mapper.valueToTree(params));
        }

        sendMessage(message);
        return setResponseHandler(message.getId(), (JsonNode node, CompletablePromise<R> out) -> {
            if (node == null) {
                out.complete(null);
            } else {
                R response = returnType != void.class ? mapper.readerFor(returnType).readValue(node) : null;
                out.complete(response);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> Promise<T> setResponseHandler(int messageId, ResponseHandler<T> handler) {
        CompletablePromise<T> promise = new CompletablePromise<>();
        promises.put(messageId, (CompletablePromise<Object>) promise);
        responseHandlers.put(messageId, (ResponseHandler<Object>) handler);
        return promise;
    }

    interface ResponseHandler<T> {
        void received(JsonNode node, CompletablePromise<T> out) throws IOException;
    }

    Promise<Map<String, ? extends JavaScriptVariable>> createScope(String id) {
        if (id == null) {
            return EMPTY_SCOPE;
        }
        return getScope(id).then(scope -> {
            Map<String, RDPLocalVariable> newBackingMap = new HashMap<>();
            for (RDPLocalVariable variable : scope) {
                newBackingMap.put(variable.getName(), variable);
            }
            return Collections.unmodifiableMap(newBackingMap);
        });
    }

    private <T> Promise<T> callInExecutor(Supplier<Promise<T>> f) {
        CompletablePromise<T> result = new CompletablePromise<>();
        executor.execute(() -> {
            f.get().thenVoid(result::complete).catchVoid(result::completeWithError);
        });
        return result;
    }
}
