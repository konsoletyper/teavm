package org.teavm.chromerdp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicInteger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.teavm.chromerdp.data.*;
import org.teavm.chromerdp.messages.*;
import org.teavm.debugging.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ChromeRDPDebugger implements JavaScriptDebugger, ChromeRDPExchangeConsumer {
    private static final Object dummy = new Object();
    private ChromeRDPExchange exchange;
    private ConcurrentMap<JavaScriptDebuggerListener, Object> listeners = new ConcurrentHashMap<>();
    private ConcurrentMap<RDPBreakpoint, Object> breakpoints = new ConcurrentHashMap<>();
    private volatile RDPCallFrame[] callStack = new RDPCallFrame[0];
    private ConcurrentMap<String, String> scripts = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> scriptIds = new ConcurrentHashMap<>();
    private boolean suspended = false;
    private ObjectMapper mapper = new ObjectMapper();
    private ConcurrentMap<Integer, ResponseHandler> responseHandlers = new ConcurrentHashMap<>();
    private AtomicInteger messageIdGenerator = new AtomicInteger();

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
    };


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
        RDPBreakpoint breakpoint = new RDPBreakpoint(this, location);
        breakpoints.put(breakpoint, dummy);
        updateBreakpoint(breakpoint);
        return breakpoint;
    }

    void destroyBreakpoint(RDPBreakpoint breakpoint) {
        breakpoints.remove(breakpoint);
        if (breakpoint.chromeId != null) {
            Message message = new Message();
            message.setMethod("Debugger.removeBreakpoint");
            RemoveBreakpointCommand params = new RemoveBreakpointCommand();
            params.setBreakpointId(breakpoint.chromeId);
            message.setParams(mapper.valueToTree(params));
            sendMessage(message);
        }
    }

    void fireScriptAdded(String script) {
        for (JavaScriptDebuggerListener listener : getListeners()) {
            listener.scriptAdded(script);
        }
    }

    void updateBreakpoint(final RDPBreakpoint breakpoint) {
        if (exchange == null) {
            return;
        }
        Message message = new Message();
        message.setId(messageIdGenerator.incrementAndGet());
        message.setMethod("Debugger.setBreakpoint");
        SetBreakpointCommand params = new SetBreakpointCommand();
        params.setLocation(unmap(breakpoint.getLocation()));
        message.setParams(mapper.valueToTree(params));
        ResponseHandler handler = new ResponseHandler() {
            @Override public void received(JsonNode node) throws IOException {
                SetBreakpointResponse response = mapper.reader(SetBreakpointResponse.class).readValue(node);
                breakpoint.chromeId = response.getBreakpointId();
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
        final Exchanger<List<RDPLocalVariable>> exchanger = new Exchanger<>();
        responseHandlers.put(message.getId(), new ResponseHandler() {
            @Override public void received(JsonNode node) throws IOException {
                GetPropertiesResponse response = mapper.reader(GetPropertiesResponse.class).readValue(node);
                // TODO: parse response
                try {
                    exchanger.exchange(new ArrayList<RDPLocalVariable>());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        sendMessage(message);
        try {
            return exchanger.exchange(null);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
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
        for (ScopeDTO scope : dto.getScopeChain()) {
            if (scope.getType().equals("local")) {
                scopeId = scope.getObject();
                break;
            }
        }
        return new RDPCallFrame(dto.getCallFrameId(), map(dto.getLocation()), new RDPScope(this, scopeId));
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
