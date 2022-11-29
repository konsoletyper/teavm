/*
 *  Copyright 2022 Alexey Andreev.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.chromerdp.data.Message;
import org.teavm.chromerdp.data.Response;
import org.teavm.common.CompletablePromise;
import org.teavm.common.Promise;
import org.teavm.debugging.javascript.JavaScriptDebuggerListener;

public abstract class BaseChromeRDPDebugger implements ChromeRDPExchangeConsumer {
    protected static final Logger logger = LoggerFactory.getLogger(BaseChromeRDPDebugger.class);
    private ChromeRDPExchange exchange;
    protected Set<JavaScriptDebuggerListener> listeners = new LinkedHashSet<>();
    protected ObjectMapper mapper = new ObjectMapper();
    private ConcurrentMap<Integer, ChromeRDPDebugger.ResponseHandler<Object>>
            responseHandlers = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, CompletablePromise<Object>> promises = new ConcurrentHashMap<>();
    private int messageIdGenerator;

    protected List<JavaScriptDebuggerListener> getListeners() {
        return new ArrayList<>(listeners);
    }

    private Executor executor;

    public BaseChromeRDPDebugger(Executor executor) {
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
            onAttach();
            for (var listener : getListeners()) {
                listener.attached();
            }
        } else {
            onDetach();
            for (var listener : getListeners()) {
                listener.detached();
            }
        }
        if (this.exchange != null) {
            this.exchange.addListener(exchangeListener);
        }
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
            var jsonMessage = mapper.readTree(messageText);
            if (jsonMessage.has("id")) {
                var response = parseJson(Response.class, jsonMessage);
                if (response.getError() != null) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Error message #{} received from browser: {}", jsonMessage.get("id"),
                                response.getError().toString());
                    }
                }
                var promise = promises.remove(response.getId());
                try {
                    responseHandlers.remove(response.getId()).received(response.getResult(), promise);
                } catch (RuntimeException e) {
                    logger.warn("Error processing message ${}", response.getId(), e);
                    promise.completeWithError(e);
                }
                return Promise.VOID;
            } else {
                var message = parseJson(Message.class, jsonMessage);
                if (message.getMethod() == null) {
                    return Promise.VOID;
                }
                return handleMessage(message);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error receiving message from Google Chrome", e);
            }
            return Promise.VOID;
        }
    }

    public void detach() {
        if (exchange != null) {
            exchange.disconnect();
        }
    }

    public boolean isAttached() {
        return exchange != null;
    }

    protected abstract void onAttach();

    protected abstract void onDetach();

    protected abstract Promise<Void> handleMessage(Message message) throws IOException;

    private <T> Promise<T> callInExecutor(Supplier<Promise<T>> f) {
        var result = new CompletablePromise<T>();
        executor.execute(() -> {
            f.get().thenVoid(result::complete).catchVoid(result::completeWithError);
        });
        return result;
    }

    protected <T> T parseJson(Class<T> type, JsonNode node) throws IOException {
        return mapper.readerFor(type).readValue(node);
    }

    protected void sendMessage(Message message) {
        if (exchange == null) {
            return;
        }
        try {
            exchange.send(mapper.writer().writeValueAsString(message));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected <R> Promise<R> callMethodAsync(String method, Class<R> returnType, Object params) {
        if (!isAttached()) {
            return Promise.of(null);
        }
        var message = new Message();
        message.setId(++messageIdGenerator);
        message.setMethod(method);
        if (params != null) {
            message.setParams(mapper.valueToTree(params));
        } else {
            message.setParams(mapper.createObjectNode());
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
}
