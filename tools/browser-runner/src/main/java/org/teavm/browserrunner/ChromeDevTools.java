/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.browserrunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Minimal client of Chrome DevTools protocol. Connects to the page that runs tests and collects CPU profiles
 * that code running in the page records with {@code console.profile(title)} and {@code console.profileEnd(title)}.
 * Not intended to be used directly, see {@link BrowserRunner#enableDevTools()}.
 */
public class ChromeDevTools {
    private static final long MAX_MESSAGE_SIZE = 1024L * 1024 * 1024;
    private static final int SAMPLING_INTERVAL_MICROS = 100;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger idGenerator = new AtomicInteger();
    private final ConcurrentMap<Integer, CompletableFuture<JsonNode>> pendingCommands = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<String>> profiles = new ConcurrentHashMap<>();
    private WebSocketClient client;
    private Session session;

    ChromeDevTools() {
    }

    void connect(int port, String pageUrl, long timeoutMillis) {
        var deadline = System.currentTimeMillis() + timeoutMillis;
        String wsUrl = null;
        while (wsUrl == null) {
            try {
                wsUrl = findPage(port, pageUrl);
            } catch (IOException e) {
                // browser is not ready yet
            }
            if (wsUrl == null) {
                if (System.currentTimeMillis() > deadline) {
                    throw new RuntimeException("Could not connect to Chrome DevTools on port " + port
                            + ". Note that DevTools are only supported in Chrome");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }

        client = new WebSocketClient();
        client.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
        client.setIdleTimeout(Duration.ZERO);
        try {
            client.start();
            session = client.connect(new Listener(), URI.create(wsUrl)).get(timeoutMillis, TimeUnit.MILLISECONDS);
            send("Profiler.enable");
            send("Profiler.setSamplingInterval", "{\"interval\":" + SAMPLING_INTERVAL_MICROS + "}");
        } catch (Exception e) {
            close();
            throw new RuntimeException("Could not connect to Chrome DevTools", e);
        }
    }

    private String findPage(int port, String pageUrl) throws IOException {
        JsonNode targets;
        try (InputStream input = URI.create("http://127.0.0.1:" + port + "/json/list").toURL().openStream()) {
            targets = objectMapper.readTree(input);
        }
        for (var target : targets) {
            if ("page".equals(target.path("type").asText()) && target.path("url").asText().startsWith(pageUrl)) {
                return target.path("webSocketDebuggerUrl").asText();
            }
        }
        return null;
    }

    private void send(String method) throws Exception {
        send(method, "{}");
    }

    private void send(String method, String params) throws Exception {
        var id = idGenerator.incrementAndGet();
        var future = new CompletableFuture<JsonNode>();
        pendingCommands.put(id, future);
        session.sendText("{\"id\":" + id + ",\"method\":\"" + method + "\",\"params\":" + params + "}",
                Callback.NOOP);
        future.get(30, TimeUnit.SECONDS);
    }

    String takeConsoleProfile(String title, long timeout, TimeUnit unit) throws TimeoutException {
        var future = profiles.computeIfAbsent(title, t -> new CompletableFuture<>());
        try {
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } finally {
            profiles.remove(title);
        }
    }

    void close() {
        if (session != null) {
            session.close();
            session = null;
        }
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                // ignore
            }
            client = null;
        }
    }

    // Must be public, since Jetty calls its methods via method handles
    public class Listener implements Session.Listener.AutoDemanding {
        @Override
        public void onWebSocketText(String message) {
            var node = objectMapper.readTree(message);
            var id = node.get("id");
            if (id != null) {
                var future = pendingCommands.remove(id.asInt());
                if (future != null) {
                    var error = node.get("error");
                    if (error != null) {
                        future.completeExceptionally(new RuntimeException(error.toString()));
                    } else {
                        future.complete(node.get("result"));
                    }
                }
                return;
            }
            if ("Profiler.consoleProfileFinished".equals(node.path("method").asText())) {
                var params = node.get("params");
                var title = params.path("title").asText();
                profiles.computeIfAbsent(title, t -> new CompletableFuture<>())
                        .complete(params.get("profile").toString());
            }
        }
    }
}
