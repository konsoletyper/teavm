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

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.teavm.chromerdp.data.CallArgumentDTO;
import org.teavm.chromerdp.data.CallFrameDTO;
import org.teavm.chromerdp.data.LocationDTO;
import org.teavm.chromerdp.data.Message;
import org.teavm.chromerdp.data.PropertyDescriptorDTO;
import org.teavm.chromerdp.data.RemoteObjectDTO;
import org.teavm.chromerdp.data.ScopeDTO;
import org.teavm.chromerdp.messages.CallFunctionCommand;
import org.teavm.chromerdp.messages.CallFunctionResponse;
import org.teavm.chromerdp.messages.CompileScriptCommand;
import org.teavm.chromerdp.messages.CompileScriptResponse;
import org.teavm.chromerdp.messages.GetPropertiesCommand;
import org.teavm.chromerdp.messages.GetPropertiesResponse;
import org.teavm.chromerdp.messages.GetScriptSourceCommand;
import org.teavm.chromerdp.messages.RemoveBreakpointCommand;
import org.teavm.chromerdp.messages.RunScriptCommand;
import org.teavm.chromerdp.messages.ScriptParsedNotification;
import org.teavm.chromerdp.messages.ScriptSource;
import org.teavm.chromerdp.messages.SetBreakpointCommand;
import org.teavm.chromerdp.messages.SetBreakpointResponse;
import org.teavm.chromerdp.messages.SuspendedNotification;
import org.teavm.common.CompletablePromise;
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptBreakpoint;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;
import org.teavm.debugging.javascript.JavaScriptDebuggerListener;
import org.teavm.debugging.javascript.JavaScriptLanguage;
import org.teavm.debugging.javascript.JavaScriptLocation;
import org.teavm.debugging.javascript.JavaScriptScript;
import org.teavm.debugging.javascript.JavaScriptVariable;

public class ChromeRDPDebugger extends BaseChromeRDPDebugger implements JavaScriptDebugger {
    private static final Promise<Map<String, ? extends JavaScriptVariable>> EMPTY_SCOPE =
            Promise.of(Collections.emptyMap());
    private Map<JavaScriptLocation, RDPNativeBreakpoint> breakpointLocationMap = new HashMap<>();
    private Set<RDPBreakpoint> breakpoints = new LinkedHashSet<>();
    private Map<String, RDPNativeBreakpoint> breakpointsByChromeId = new HashMap<>();
    private volatile RDPCallFrame[] callStack = new RDPCallFrame[0];
    private Map<String, ChromeRDPScript> scripts = new LinkedHashMap<>();
    private Map<String, JavaScriptScript> readonlyScripts = Collections.unmodifiableMap(scripts);
    private volatile boolean suspended;
    private Promise<Void> runtimeEnabledPromise;

    public ChromeRDPDebugger(Executor executor) {
        super(executor);
    }

    @Override
    protected void onAttach() {
        for (RDPBreakpoint breakpoint : breakpoints.toArray(new RDPBreakpoint[0])) {
            updateBreakpoint(breakpoint.nativeBreakpoint);
        }
    }

    @Override
    protected void onDetach() {
        suspended = false;
        callStack = null;
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

    private Promise<Void> injectWasmFunctions(int contextId) {
        return enableRuntime()
                .thenAsync(v -> {
                    var compileParams = new CompileScriptCommand();
                    compileParams.expression = ""
                            + "$dbg_memory = function(buffer, offset, count) { return btoa("
                            + "String.fromCharCode.apply(null, new Uint8Array(buffer, offset, count))) };\n";
                    compileParams.sourceURL = "file://fake-wasm";
                    compileParams.persistScript = true;
                    compileParams.executionContextId = contextId;
                    return callMethodAsync("Runtime.compileScript", CompileScriptResponse.class, compileParams);
                })
                .thenAsync(response -> {
                    var runParams = new RunScriptCommand();
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

    @Override
    protected Promise<Void> handleMessage(Message message) throws IOException {
        switch (message.getMethod()) {
            case "TeaVM.ping":
                sendPong();
                return Promise.VOID;
            case "Debugger.paused":
                return firePaused(parseJson(SuspendedNotification.class, message.getParams()));
            case "Debugger.resumed":
                return fireResumed();
            case "Debugger.scriptParsed":
                return scriptParsed(parseJson(ScriptParsedNotification.class, message.getParams()));
        }
        return Promise.VOID;
    }

    private void sendPong() {
        var message = new Message();
        message.setMethod("TeaVM.pong");
        sendMessage(message);
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
        RDPBreakpoint breakpoint = nativeBreakpoint != null && !nativeBreakpoint.breakpoints.isEmpty()
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
        var language = JavaScriptLanguage.JS;
        if (params.getScriptLanguage() != null) {
            switch (params.getScriptLanguage()) {
                case "WebAssembly":
                    language = JavaScriptLanguage.WASM;
                    break;
                case "JavaScript":
                    language = JavaScriptLanguage.JS;
                    break;
                default:
                    language = JavaScriptLanguage.UNKNOWN;
                    break;
            }
        }
        var script = new ChromeRDPScript(this, params.getScriptId(), language, params.getUrl());
        scripts.put(script.getId(), script);
        if (params.getUrl().startsWith("file://fake")) {
            return Promise.VOID;
        }
        for (var listener : getListeners()) {
            listener.scriptAdded(script);
        }
        if (language == JavaScriptLanguage.JS) {
            return injectFunctions(params.getExecutionContextId());
        } else if (language == JavaScriptLanguage.WASM) {
            return injectWasmFunctions(params.getExecutionContextId());
        }
        return Promise.VOID;
    }

    @Override
    public Map<? extends String, ? extends JavaScriptScript> getScripts() {
        return readonlyScripts;
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
    public boolean isSuspended() {
        return isAttached() && suspended;
    }

    @Override
    public JavaScriptCallFrame[] getCallStack() {
        if (!isAttached()) {
            return null;
        }
        JavaScriptCallFrame[] callStack = this.callStack;
        return callStack != null ? callStack.clone() : null;
    }

    @Override
    public Promise<JavaScriptBreakpoint> createBreakpoint(JavaScriptLocation location) {
        RDPBreakpoint breakpoint = new RDPBreakpoint(this);
        breakpoint.nativeBreakpoint = lockNativeBreakpoint(location, breakpoint);
        var result = new CompletablePromise<JavaScriptBreakpoint>();
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
        breakpoints.remove(breakpoint);
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
        var params = new GetPropertiesCommand();
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

    Promise<List<RDPLocalVariable>> getSpecialScope(String scopeId) {
        var params = new GetPropertiesCommand();
        params.setObjectId(scopeId);
        params.setOwnProperties(false);

        return callMethodAsync("Runtime.getProperties", GetPropertiesResponse.class, params)
                .then(response -> {
                    if (response == null) {
                        return Collections.emptyList();
                    }
                    return parseProperties(scopeId, response.getResult(), null);
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

    Promise<byte[]> getMemory(String objectId, int start, int count) {
        var params = new CallFunctionCommand();
        params.setObjectId(objectId);
        params.setArguments(new CallArgumentDTO[] { objArg(objectId), intArg(start), intArg(count) });
        params.setFunctionDeclaration("$dbg_memory");

        return callMethodAsync("Runtime.callFunctionOn", CallFunctionResponse.class, params)
                .then(response -> {
                    var result = response != null ? response.getResult() : null;
                    if (result.getValue() == null) {
                        return null;
                    }
                    return Base64.getDecoder().decode(result.getValue().textValue());
                });
    }

    private CallArgumentDTO objArg(String objectId) {
        var arg = new CallArgumentDTO();
        arg.setObjectId(objectId);
        return arg;
    }

    private CallArgumentDTO intArg(int value) {
        var arg = new CallArgumentDTO();
        arg.setValue(new IntNode(value));
        return arg;
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
            default: {
                var valueAsText = remoteValue.getValue() != null ? remoteValue.getValue().asText() : "null";
                return new RDPValue(this, valueAsText, remoteValue.getType(), remoteValue.getObjectId(), false);
            }
        }
    }

    private RDPCallFrame map(CallFrameDTO dto) {
        String scopeId = null;
        RDPValue thisObject = null;
        RDPValue closure = null;
        RDPValue module = null;
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
                case "module":
                    module = new RDPValue(this, scope.getObject().getDescription(), scope.getObject().getType(),
                            scope.getObject().getObjectId(), true);
                    break;
            }
        }
        return new RDPCallFrame(this, dto.getCallFrameId(), map(dto.getLocation()), scopeId,
                thisObject, module, closure);
    }

    private JavaScriptLocation map(LocationDTO dto) {
        return new JavaScriptLocation(scripts.get(dto.getScriptId()), dto.getLineNumber(), dto.getColumnNumber());
    }

    private LocationDTO unmap(JavaScriptLocation location) {
        var dto = new LocationDTO();
        dto.setScriptId(location.getScript().getId());
        dto.setLineNumber(location.getLine());
        dto.setColumnNumber(location.getColumn());
        return dto;
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

    Promise<String> getScriptSource(String id) {
        var callArgs = new GetScriptSourceCommand();
        callArgs.scriptId = id;
        return callMethodAsync("Debugger.getScriptSource", ScriptSource.class, callArgs)
                .then(source -> source.bytecode);
    }
}
