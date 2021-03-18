/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.junit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class BrowserRunStrategy implements TestRunStrategy {
    private boolean decodeStack = Boolean.parseBoolean(System.getProperty(TeaVMTestRunner.JS_DECODE_STACK, "true"));
    private final File baseDir;
    private final String type;
    private final Function<String, Process> browserRunner;
    private Process browserProcess;
    private Server server;
    private int port;
    private AtomicInteger idGenerator = new AtomicInteger(0);
    private BlockingQueue<Session> wsSessionQueue = new LinkedBlockingQueue<>();
    private ConcurrentMap<Integer, CallbackWrapper> awaitingRuns = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public BrowserRunStrategy(File baseDir, String type, Function<String, Process> browserRunner) {
        this.baseDir = baseDir;
        this.type = type;
        this.browserRunner = browserRunner;
    }

    @Override
    public void beforeAll() {
        runServer();
        browserProcess = browserRunner.apply("http://localhost:" + port + "/index.html");
    }

    private void runServer() {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        TestCodeServlet servlet = new TestCodeServlet();

        ServletHolder servletHolder = new ServletHolder(servlet);
        servletHolder.setAsyncSupported(true);
        context.addServlet(servletHolder, "/*");

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        port = connector.getLocalPort();
    }

    @Override
    public void afterAll() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (browserProcess != null) {
            browserProcess.destroy();
        }
    }

    @Override
    public void beforeThread() {
    }

    @Override
    public void afterThread() {
    }

    static class CallbackWrapper implements TestRunCallback {
        private final CountDownLatch latch;
        private final TestRun run;
        volatile boolean shouldRepeat;

        CallbackWrapper(CountDownLatch latch, TestRun run) {
            this.latch = latch;
            this.run = run;
        }

        @Override
        public void complete() {
            latch.countDown();
            run.getCallback().complete();
        }

        @Override
        public void error(Throwable e) {
            latch.countDown();
            run.getCallback().error(e);
        }

        void repeat() {
            latch.countDown();
            shouldRepeat = true;
        }
    }

    @Override
    public void runTest(TestRun run) throws IOException {
        while (!runTestOnce(run)) {
            // repeat
        }
    }

    private boolean runTestOnce(TestRun run) {
        Session ws;
        try {
            do {
                ws = wsSessionQueue.poll(1, TimeUnit.SECONDS);
            } while (ws == null || !ws.isOpen());
        } catch (InterruptedException e) {
            run.getCallback().error(e);
            return true;
        }

        int id = idGenerator.incrementAndGet();
        CountDownLatch latch = new CountDownLatch(1);

        CallbackWrapper callbackWrapper = new CallbackWrapper(latch, run);
        awaitingRuns.put(id, callbackWrapper);

        JsonNodeFactory nf = objectMapper.getNodeFactory();
        ObjectNode node = nf.objectNode();
        node.set("id", nf.numberNode(id));

        ArrayNode array = nf.arrayNode();
        node.set("tests", array);

        File file = new File(run.getBaseDirectory(), run.getFileName()).getAbsoluteFile();
        String relPath = baseDir.getAbsoluteFile().toPath().relativize(file.toPath()).toString();
        ObjectNode testNode = nf.objectNode();
        testNode.set("type", nf.textNode(type));
        testNode.set("name", nf.textNode(run.getFileName()));
        testNode.set("file", nf.textNode("tests/" + relPath));
        if (run.getArgument() != null) {
            testNode.set("argument", nf.textNode(run.getArgument()));
        }
        array.add(testNode);

        String message = node.toString();
        ws.getRemote().sendStringByFuture(message);

        try {
            latch.await();
        } catch (InterruptedException e) {
            // do nothing
        }

        if (ws.isOpen()) {
            wsSessionQueue.offer(ws);
        }

        return !callbackWrapper.shouldRepeat;
    }

    class TestCodeServlet extends HttpServlet {
        private WebSocketServletFactory wsFactory;
        private Map<String, String> contentCache = new ConcurrentHashMap<>();

        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            WebSocketPolicy wsPolicy = new WebSocketPolicy(WebSocketBehavior.SERVER);
            wsFactory = WebSocketServletFactory.Loader.load(config.getServletContext(), wsPolicy);
            wsFactory.setCreator((req, resp) -> new TestCodeSocket());
            try {
                wsFactory.start();
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String path = req.getRequestURI();
            if (path != null) {
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                if (req.getMethod().equals("GET")) {
                    switch (path) {
                        case "/index.html":
                        case "/frame.html": {
                            String content = getFromCache(path, "true".equals(req.getParameter("logging")));
                            if (content != null) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.setContentType("text/html");
                                resp.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
                                resp.getOutputStream().flush();
                                return;
                            }
                            break;
                        }
                        case "/client.js":
                        case "/frame.js":
                        case "/deobfuscator.js": {
                            String content = getFromCache(path, false);
                            if (content != null) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.setContentType("application/javascript");
                                resp.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
                                resp.getOutputStream().flush();
                                return;
                            }
                            break;
                        }
                    }
                    if (path.startsWith("/tests/")) {
                        String relPath = path.substring("/tests/".length());
                        File file = new File(baseDir, relPath);
                        if (file.isFile()) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            if (file.getName().endsWith(".js")) {
                                resp.setContentType("application/javascript");
                            } else if (file.getName().endsWith(".wasm")) {
                                resp.setContentType("application/wasm");
                            }
                            try (FileInputStream input = new FileInputStream(file)) {
                                copy(input, resp.getOutputStream());
                            }
                            resp.getOutputStream().flush();
                        }
                    }
                }
                if (path.equals("/ws") && wsFactory.isUpgradeRequest(req, resp)
                        && (wsFactory.acceptWebSocket(req, resp) || resp.isCommitted())) {
                    return;
                }
            }

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        private String getFromCache(String fileName, boolean logging) {
            return contentCache.computeIfAbsent(fileName, fn -> {
                ClassLoader loader = BrowserRunStrategy.class.getClassLoader();
                try (InputStream input = loader.getResourceAsStream("test-server" + fn);
                        Reader reader = new InputStreamReader(input)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[2048];
                    while (true) {
                        int charsRead = reader.read(buffer);
                        if (charsRead < 0) {
                            break;
                        }
                        sb.append(buffer, 0, charsRead);
                    }
                    return sb.toString()
                            .replace("{{PORT}}", String.valueOf(port))
                            .replace("\"{{LOGGING}}\"", String.valueOf(logging))
                            .replace("\"{{DEOBFUSCATION}}\"", String.valueOf(decodeStack));
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            });
        }

        private void copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[2048];
            while (true) {
                int bytes = input.read(buffer);
                if (bytes < 0) {
                    break;
                }
                output.write(buffer, 0, bytes);
            }
        }
    }

    class TestCodeSocket extends WebSocketAdapter {
        @Override
        public void onWebSocketConnect(Session sess) {
            wsSessionQueue.offer(sess);
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            for (CallbackWrapper run : awaitingRuns.values()) {
                run.repeat();
            }
        }

        @Override
        public void onWebSocketText(String message) {
            JsonNode node;
            try {
                node = objectMapper.readTree(new StringReader(message));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            int id = node.get("id").asInt();
            TestRunCallback run = awaitingRuns.remove(id);
            if (run == null) {
                System.err.println("Unexpected run id: " + id);
                return;
            }

            JsonNode resultNode = node.get("result");

            JsonNode log = resultNode.get("log");
            if (log != null) {
                for (JsonNode logEntry : log) {
                    String str = logEntry.get("message").asText();
                    switch (logEntry.get("type").asText()) {
                        case "stdout":
                            System.out.println(str);
                            break;
                        case "stderr":
                            System.err.println(str);
                            break;
                    }
                }
            }

            String status = resultNode.get("status").asText();
            if (status.equals("OK")) {
                run.complete();
            } else {
                run.error(new RuntimeException(resultNode.get("errorMessage").asText()));
            }
        }
    }
}
