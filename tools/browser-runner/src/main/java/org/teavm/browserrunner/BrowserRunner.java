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
package org.teavm.browserrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

public class BrowserRunner {
    private static final boolean logBrowserOutput = System.getenv().getOrDefault("TEAVM_TEST_BROWSER_LOG", "0")
            .equals("1");
    private boolean decodeStack;
    private final File baseDir;
    private final String type;
    private final Function<BrowserRunParams, Process> browserRunner;
    private Process browserProcess;
    private Server server;
    private int port;
    private AtomicInteger idGenerator = new AtomicInteger(0);
    private BlockingQueue<Session> wsSessionQueue = new LinkedBlockingQueue<>();
    private ConcurrentMap<Integer, CallbackWrapper> awaitingRuns = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public BrowserRunner(File baseDir, String type, Function<BrowserRunParams, Process> browserRunner,
            boolean decodeStack) {
        this.baseDir = baseDir;
        this.type = type;
        this.browserRunner = browserRunner;
        this.decodeStack = decodeStack;
    }

    public static Function<BrowserRunParams, Process> pickBrowser(String name) {
        switch (name) {
            case "browser":
                return BrowserRunner::customBrowser;
            case "browser-chrome":
                return BrowserRunner::chromeBrowser;
            case "browser-firefox":
                return BrowserRunner::firefoxBrowser;
            case "none":
                return null;
            default:
                throw new RuntimeException("Unknown run strategy: " + name);
        }
    }

    public void start() {
        runServer();
        var pid = ProcessHandle.current().pid();
        browserProcess = browserRunner.apply(
                new BrowserRunParams() {
                    @Override
                    public String url() {
                        return "http://localhost:" + port + "/index.html";
                    }

                    @Override
                    public File stderrFile() {
                        if (logBrowserOutput) {
                            return new File(baseDir, "browser-stderr-" + pid + ".txt");
                        }
                        return null;
                    }

                    @Override
                    public File stdoutFile() {
                        if (logBrowserOutput) {
                            return new File(baseDir, "browser-stdout-" + pid + ".txt");
                        }
                        return null;
                    }
                }
        );
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (browserProcess != null) {
            browserProcess.destroy();
        }
    }

    private void runServer() {
        server = new Server();
        var connector = new ServerConnector(server);
        connector.setIdleTimeout(0);
        server.addConnector(connector);

        var context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        var servlet = new TestCodeServlet();

        var servletHolder = new ServletHolder(servlet);
        servletHolder.setAsyncSupported(true);
        context.addServlet(servletHolder, "/*");

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        port = connector.getLocalPort();
    }

    static class CallbackWrapper  {
        private final CountDownLatch latch;
        volatile Throwable error;
        volatile boolean shouldRepeat;

        CallbackWrapper(CountDownLatch latch) {
            this.latch = latch;
        }

        void complete() {
            latch.countDown();
        }

        void error(Throwable e) {
            error = e;
            latch.countDown();
        }

        void repeat() {
            latch.countDown();
            shouldRepeat = true;
        }
    }

    public void runTest(BrowserRunDescriptor run) throws IOException {
        while (!runTestOnce(run)) {
            // repeat
        }
    }

    private boolean runTestOnce(BrowserRunDescriptor run) {
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

        var callbackWrapper = new CallbackWrapper(latch);
        awaitingRuns.put(id, callbackWrapper);

        var nf = objectMapper.getNodeFactory();
        var node = nf.objectNode();
        node.set("command", nf.textNode("run"));
        node.set("id", nf.numberNode(id));

        var array = nf.arrayNode();
        node.set("tests", array);

        var testNode = nf.objectNode();
        testNode.set("type", nf.textNode(type));
        testNode.set("name", nf.textNode(run.getName()));

        var fileNode = nf.objectNode();
        fileNode.set("path", nf.textNode(run.getTestPath()));
        fileNode.set("type", nf.textNode(run.isModule() ? "module" : "regular"));
        testNode.set("file", fileNode);

        testNode.set("cached", nf.booleanNode(run.isCached()));

        if (!run.getAdditionalFiles().isEmpty()) {
            var additionalJsJson = nf.arrayNode();
            for (var additionalFile : run.getAdditionalFiles()) {
                var additionFileObj = nf.objectNode();
                additionFileObj.set("path", nf.textNode(additionalFile));
                additionFileObj.set("type", nf.textNode("regular"));
                additionalJsJson.add(additionFileObj);
            }
            testNode.set("additionalFiles", additionalJsJson);
        }

        if (run.getArgument() != null) {
            testNode.set("argument", nf.textNode(run.getArgument()));
        }
        array.add(testNode);

        var message = node.toString();
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

    public void cleanup() {
        var ws = wsSessionQueue.peek();
        if (ws == null) {
            return;
        }
        var nf = objectMapper.getNodeFactory();
        var node = nf.objectNode();
        node.set("command", nf.textNode("cleanup"));

        var message = node.toString();
        ws.getRemote().sendStringByFuture(message);
    }

    class TestCodeServlet extends HttpServlet {
        private WebSocketServletFactory wsFactory;
        private Map<String, String> contentCache = new ConcurrentHashMap<>();

        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            var wsPolicy = new WebSocketPolicy(WebSocketBehavior.SERVER);
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
            var path = req.getRequestURI();
            if (path != null) {
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                if (req.getMethod().equals("GET")) {
                    switch (path) {
                        case "/index.html":
                        case "/frame.html": {
                            var content = getFromCache(path, "true".equals(req.getParameter("logging")));
                            if (content != null) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.setContentType("text/html");
                                resp.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                                resp.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                                resp.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
                                resp.getOutputStream().flush();
                                return;
                            }
                            break;
                        }
                        case "/client.js":
                        case "/frame.js":
                        case "/deobfuscator.js": {
                            var content = getFromCache(path, false);
                            if (content != null) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.setContentType("application/javascript");
                                resp.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                                resp.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                                resp.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
                                resp.getOutputStream().flush();
                                return;
                            }
                            break;
                        }
                    }
                    if (path.startsWith("/tests/")) {
                        var relPath = path.substring("/tests/".length());
                        var file = new File(baseDir, relPath);
                        if (file.isFile()) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            if (file.getName().endsWith(".js")) {
                                resp.setContentType("application/javascript");
                            } else if (file.getName().endsWith(".wasm")) {
                                resp.setContentType("application/wasm");
                            }
                            resp.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                            resp.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                            try (var input = new FileInputStream(file)) {
                                input.transferTo(resp.getOutputStream());
                            }
                            resp.getOutputStream().flush();
                        }
                    }
                    if (path.startsWith("/resources/")) {
                        var relPath = path.substring("/resources/".length());
                        var classLoader = BrowserRunner.class.getClassLoader();
                        try (var input = classLoader.getResourceAsStream(relPath)) {
                            if (input != null) {
                                if (relPath.endsWith(".js")) {
                                    resp.setContentType("application/javascript");
                                }
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                                resp.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
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
                var loader = BrowserRunner.class.getClassLoader();
                try (var input = loader.getResourceAsStream("test-server" + fn);
                        var reader = new InputStreamReader(input)) {
                    var sb = new StringBuilder();
                    var buffer = new char[2048];
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
            var run = awaitingRuns.remove(id);
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

    public static Process customBrowser(BrowserRunParams runParams) {
        System.out.println("Open link to run tests: " + runParams.url() + "?logging=true");
        return null;
    }

    public static Process chromeBrowser(BrowserRunParams runParams) {
        return browserTemplate("chrome", runParams, (profile, params) -> {
            addChromeCommand(params);
            if (logBrowserOutput) {
                params.addAll(List.of(
                        "--enable-logging=stderr",
                        "--log-level=0"
                ));
            }
            params.addAll(Arrays.asList(
                    "--headless",
                    "--disable-gpu",
                    "--no-first-run",
                    "--js-flags=--expose-gc",
                    "--user-data-dir=" + profile
            ));
        });
    }

    public static Process firefoxBrowser(BrowserRunParams runParams) {
        return browserTemplate("firefox", runParams, (profile, params) -> {
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

    private static Process browserTemplate(String name, BrowserRunParams runParams,
            BiConsumer<String, List<String>> paramsBuilder) {
        File temp;
        try {
            temp = File.createTempFile("teavm", "teavm");
            temp.delete();
            temp.mkdirs();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteDir(temp)));
            System.out.println("Running " + name + " with user data dir: " + temp.getAbsolutePath());
            List<String> params = new ArrayList<>();
            paramsBuilder.accept(temp.getAbsolutePath(), params);
            params.add(runParams.url());
            ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[0]));
            Process process = pb.start();
            logStream(process.getInputStream(), runParams.stdoutFile(), "browser stdout");
            logStream(process.getErrorStream(), runParams.stderrFile(), "browser stderr");
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

    private static void logStream(InputStream stream, File file, String prefix) {
        new Thread(() -> {
            if (file == null) {
                try (var reader = new BufferedReader(new InputStreamReader(stream))) {
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        System.out.println(prefix + ": " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                file.getParentFile().mkdirs();
                try (var reader = new BufferedReader(new InputStreamReader(stream));
                        var writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        writer.append(line);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
