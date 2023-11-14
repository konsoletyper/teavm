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

import static org.teavm.junit.PropertyNames.JS_DECODE_STACK;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
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

class BrowserRunStrategy implements TestRunStrategy {
    private boolean decodeStack = Boolean.parseBoolean(System.getProperty(JS_DECODE_STACK, "true"));
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

    BrowserRunStrategy(File baseDir, String type, Function<String, Process> browserRunner) {
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

    static class CallbackWrapper implements TestRunCallback {
        private final CountDownLatch latch;
        volatile Throwable error;
        volatile boolean shouldRepeat;

        CallbackWrapper(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void complete() {
            latch.countDown();
        }

        @Override
        public void error(Throwable e) {
            error = e;
            latch.countDown();
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
            Thread.currentThread().interrupt();
            return true;
        }

        int id = idGenerator.incrementAndGet();
        var latch = new CountDownLatch(1);

        CallbackWrapper callbackWrapper = new CallbackWrapper(latch);
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

        var additionalJs = additionalJs(run);
        if (additionalJs.length > 0) {
            var additionalJsJson = nf.arrayNode();
            for (var additionalFile : additionalJs) {
                additionalJsJson.add("resources/" + additionalFile);
            }
            testNode.set("additionalFiles", additionalJsJson);
        }

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

        if (callbackWrapper.error != null) {
            var err = callbackWrapper.error;
            if (err instanceof RuntimeException) {
                throw (RuntimeException) err;
            } else {
                throw new RuntimeException(err);
            }
        }

        return !callbackWrapper.shouldRepeat;
    }

    private String[] additionalJs(TestRun run) {
        var result = new LinkedHashSet<String>();

        var method = run.getMethod();
        var attachAnnot = method.getAnnotation(AttachJavaScript.class);
        if (attachAnnot != null) {
            result.addAll(List.of(attachAnnot.value()));
        }

        var cls = method.getDeclaringClass();
        while (cls != null) {
            var classAttachAnnot = cls.getAnnotation(AttachJavaScript.class);
            if (classAttachAnnot != null) {
                result.addAll(List.of(attachAnnot.value()));
            }
            cls = cls.getSuperclass();
        }

        return result.toArray(new String[0]);
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
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
                                input.transferTo(resp.getOutputStream());
                            }
                            resp.getOutputStream().flush();
                        }
                    }
                    if (path.startsWith("/resources/")) {
                        var relPath = path.substring("/resources/".length());
                        var classLoader = BrowserRunStrategy.class.getClassLoader();
                        try (var input = classLoader.getResourceAsStream(relPath)) {
                            if (input != null) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                input.transferTo(resp.getOutputStream());
                            } else {
                                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
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

    static Process customBrowser(String url) {
        System.out.println("Open link to run tests: " + url + "?logging=true");
        return null;
    }

    static Process chromeBrowser(String url) {
        return browserTemplate("chrome", url, (profile, params) -> {
            addChromeCommand(params);
            params.addAll(Arrays.asList(
                    "--headless",
                    "--disable-gpu",
                    "--remote-debugging-port=9222",
                    "--no-first-run",
                    "--user-data-dir=" + profile
            ));
        });
    }

    static Process firefoxBrowser(String url) {
        return browserTemplate("firefox", url, (profile, params) -> {
            addFirefoxCommand(params);
            params.addAll(Arrays.asList(
                    "--headless",
                    "--profile",
                    profile
            ));
        });
    }

    private static void addChromeCommand(List<String> params) {
        if (isMacos()) {
            params.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        } else if (isWindows()) {
            params.add("cmd.exe");
            params.add("start");
            params.add("/C");
            params.add("chrome");
        } else {
            params.add("google-chrome-stable");
        }
    }

    private static void addFirefoxCommand(List<String> params) {
        if (isMacos()) {
            params.add("/Applications/Firefox.app/Contents/MacOS/firefox");
            return;
        }
        if (isWindows()) {
            params.add("cmd.exe");
            params.add("/C");
            params.add("start");
        }
        params.add("firefox");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    private static boolean isMacos() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    private static Process browserTemplate(String name, String url, BiConsumer<String, List<String>> paramsBuilder) {
        File temp;
        try {
            temp = File.createTempFile("teavm", "teavm");
            temp.delete();
            temp.mkdirs();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteDir(temp)));
            System.out.println("Running " + name + " with user data dir: " + temp.getAbsolutePath());
            List<String> params = new ArrayList<>();
            paramsBuilder.accept(temp.getAbsolutePath(), params);
            params.add(url);
            ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[0]));
            Process process = pb.start();
            logStream(process.getInputStream(), name + " stdout");
            logStream(process.getErrorStream(), name + " stderr");
            new Thread(() -> {
                try {
                    System.out.println(name + " process terminated with code: " + process.waitFor());
                } catch (InterruptedException e) {
                    // ignore
                }
            });
            return process;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void logStream(InputStream stream, String name) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(name + ": " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }
}
