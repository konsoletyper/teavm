/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.devserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.client.io.UpgradeListener;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.cache.InMemoryMethodNodeCache;
import org.teavm.cache.InMemoryProgramCache;
import org.teavm.cache.MemoryCachedClassReaderSource;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.dependency.FastDependencyAnalyzer;
import org.teavm.model.ClassReader;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.util.FileSystemWatcher;
import org.teavm.vm.MemoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public class CodeServlet extends HttpServlet {
    private static final Supplier<InputStream> EMPTY_CONTENT = () -> null;
    private WebSocketServletFactory wsFactory;

    private String mainClass;
    private String[] classPath;
    private String fileName = "classes.js";
    private String pathToFile = "/";
    private String indicatorWsPath;
    private String deobfuscatorPath;
    private List<String> sourcePath = new ArrayList<>();
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private boolean indicator;
    private boolean deobfuscateStack;
    private boolean automaticallyReloaded;
    private int port;
    private int debugPort;
    private String proxyUrl;
    private String proxyPath = "/";
    private String proxyHost;
    private String proxyProtocol;
    private int proxyPort;
    private String proxyBaseUrl;

    private Map<String, Supplier<InputStream>> sourceFileCache = new HashMap<>();

    private volatile boolean stopped;
    private FileSystemWatcher watcher;
    private MemoryCachedClassReaderSource classSource;
    private InMemoryProgramCache programCache;
    private InMemoryMethodNodeCache astCache;
    private int lastReachedClasses;
    private boolean firstTime = true;

    private final Object contentLock = new Object();
    private final Map<String, byte[]> content = new HashMap<>();
    private MemoryBuildTarget buildTarget = new MemoryBuildTarget();

    private final Set<ProgressHandler> progressHandlers = new LinkedHashSet<>();
    private final Object statusLock = new Object();
    private volatile boolean cancelRequested;
    private boolean compiling;
    private double progress;
    private boolean waiting;
    private Thread buildThread;
    private List<DevServerListener> listeners = new ArrayList<>();
    private HttpClient httpClient;
    private WebSocketClient wsClient = new WebSocketClient();

    public CodeServlet(String mainClass, String[] classPath) {
        this.mainClass = mainClass;
        this.classPath = classPath.clone();

        httpClient = new HttpClient();
        httpClient.setFollowRedirects(false);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPathToFile(String pathToFile) {
        this.pathToFile = normalizePath(pathToFile);
    }

    public List<String> getSourcePath() {
        return sourcePath;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    public void setDeobfuscateStack(boolean deobfuscateStack) {
        this.deobfuscateStack = deobfuscateStack;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public void setAutomaticallyReloaded(boolean automaticallyReloaded) {
        this.automaticallyReloaded = automaticallyReloaded;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public void setProxyPath(String proxyPath) {
        this.proxyPath = normalizePath(proxyPath);
    }

    public void addProgressHandler(ProgressHandler handler) {
        synchronized (progressHandlers) {
            progressHandlers.add(handler);
        }

        double progress;
        synchronized (statusLock) {
            if (!compiling) {
                return;
            }
            progress = this.progress;
        }

        handler.progress(progress);
    }

    public void removeProgressHandler(ProgressHandler handler) {
        synchronized (progressHandlers) {
            progressHandlers.remove(handler);
        }
    }

    public void addListener(DevServerListener listener) {
        listeners.add(listener);
    }

    public void invalidateCache() {
        synchronized (statusLock) {
            if (compiling) {
                return;
            }
            astCache.invalidate();
            programCache.invalidate();
            classSource.invalidate();
        }
    }

    public void buildProject() {
        synchronized (statusLock) {
            if (waiting) {
                buildThread.interrupt();
            }
        }
    }

    public void cancelBuild() {
        synchronized (statusLock) {
            if (compiling) {
                cancelRequested = true;
            }
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (proxyUrl != null) {
            try {
                httpClient.start();
                wsClient.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                URL url = new URL(proxyUrl);
                proxyPort = url.getPort();
                proxyHost = proxyPort >= 0 ? url.getHost() + ":" + proxyPort : url.getHost();
                proxyProtocol = url.getProtocol();

                StringBuilder sb = new StringBuilder();
                sb.append(proxyProtocol).append("://").append(proxyHost);
                proxyBaseUrl = sb.toString();
            } catch (MalformedURLException e) {
                log.warning("Could not extract host from URL: " + proxyUrl, e);
            }
        }

        indicatorWsPath = pathToFile + fileName + ".ws";
        deobfuscatorPath = pathToFile + fileName + ".deobfuscator.js";
        WebSocketPolicy wsPolicy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        wsFactory = WebSocketServletFactory.Loader.load(config.getServletContext(), wsPolicy);
        wsFactory.setCreator((req, resp) -> {
            ProxyWsClient proxyClient = (ProxyWsClient) req.getHttpServletRequest().getAttribute("teavm.ws.client");
            if (proxyClient == null) {
                return new CodeWsEndpoint(this);
            } else {
                ProxyWsClient proxy = new ProxyWsClient();
                proxy.setTarget(proxyClient);
                proxyClient.setTarget(proxy);
                return proxy;
            }
        });
        try {
            wsFactory.start();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path != null) {
            log.debug("Serving " + path);
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (req.getMethod().equals("GET") && path.startsWith(pathToFile) && path.length() > pathToFile.length()) {
                String fileName = path.substring(pathToFile.length());
                if (fileName.startsWith("src/")) {
                    if (serveSourceFile(fileName.substring("src/".length()), resp)) {
                        log.debug("File " + path + " served as source file");
                        return;
                    }
                } else if (path.equals(indicatorWsPath)) {
                    if (wsFactory.isUpgradeRequest(req, resp)) {
                        if (wsFactory.acceptWebSocket(req, resp) || resp.isCommitted()) {
                            return;
                        }
                    }
                } else if (path.equals(deobfuscatorPath)) {
                    serveDeobfuscator(resp);
                    return;
                } else {
                    byte[] fileContent;
                    boolean firstTime;
                    synchronized (contentLock) {
                        fileContent = content.get(fileName);
                        firstTime = this.firstTime;
                    }
                    if (fileContent != null) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setCharacterEncoding("UTF-8");
                        resp.setContentType("text/plain");
                        resp.getOutputStream().write(fileContent);
                        resp.getOutputStream().flush();
                        log.debug("File " + path + " served as generated file");
                        return;
                    } else if (fileName.equals(this.fileName) && indicator && firstTime) {
                        serveBootFile(resp);
                        return;
                    }
                }
            }

            if (proxyUrl != null && path.startsWith(proxyPath)) {
                if (wsFactory.isUpgradeRequest(req, resp)) {
                    proxyWebSocket(req, resp, path);
                } else {
                    proxy(req, resp, path);
                }
                return;
            }
        }

        log.debug("File " + path + " not found");
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private void serveDeobfuscator(HttpServletResponse resp) throws IOException {
        ClassLoader loader = CodeServlet.class.getClassLoader();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain");
        try (InputStream input = loader.getResourceAsStream("teavm/devserver/deobfuscator.js")) {
            IOUtils.copy(input, resp.getOutputStream());
        }
        resp.getOutputStream().flush();
    }

    private void proxy(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        AsyncContext async = req.startAsync();

        String relPath = path.substring(proxyPath.length());
        StringBuilder sb = new StringBuilder(proxyUrl);
        if (!relPath.isEmpty() && !proxyUrl.endsWith("/")) {
            sb.append("/");
        }
        sb.append(relPath);

        if (req.getQueryString() != null) {
            sb.append("?").append(req.getQueryString());
        }
        log.debug("Trying to serve '" + relPath + "' from '" + sb + "'");

        Request proxyReq = httpClient.newRequest(sb.toString());
        proxyReq.method(req.getMethod());
        copyRequestHeaders(req, proxyReq::header);

        proxyReq.content(new InputStreamContentProvider(req.getInputStream()));
        HeaderSender headerSender = new HeaderSender(resp);

        proxyReq.onResponseContent((response, responseContent) -> {
            headerSender.send(response);
            try {
                WritableByteChannel channel = Channels.newChannel(resp.getOutputStream());
                while (responseContent.remaining() > 0) {
                    channel.write(responseContent);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        proxyReq.send(result -> {
            headerSender.send(result.getResponse());
            async.complete();
        });
    }

    class HeaderSender {
        final HttpServletResponse resp;
        boolean sent;

        HeaderSender(HttpServletResponse resp) {
            this.resp = resp;
        }

        void send(Response response) {
            if (sent) {
                return;
            }

            sent = true;
            resp.setStatus(response.getStatus());

            for (HttpField field : response.getHeaders()) {
                if (field.getName().toLowerCase().equals("location")) {
                    String value = field.getValue();
                    if (value.startsWith(proxyUrl)) {
                        String relLocation = value.substring(proxyUrl.length());
                        resp.addHeader(field.getName(), "http://localhost:" + port + proxyPath + relLocation);
                        continue;
                    }
                }
                resp.addHeader(field.getName(), field.getValue());
            }
        }
    }

    private void proxyWebSocket(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        AsyncContext async = req.startAsync();

        String relPath = path.substring(proxyPath.length());
        StringBuilder sb = new StringBuilder(proxyProtocol.equals("http") ? "ws" : "wss").append("://");
        sb.append(proxyHost);
        if (!relPath.isEmpty()) {
            sb.append("/");
        }
        sb.append(relPath);
        if (req.getQueryString() != null) {
            sb.append("?").append(req.getQueryString());
        }
        URI uri;
        try {
            uri = new URI(sb.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        ProxyWsClient client = new ProxyWsClient();
        req.setAttribute("teavm.ws.client", client);
        ClientUpgradeRequest proxyReq = new ClientUpgradeRequest();
        proxyReq.setMethod(req.getMethod());
        Map<String, List<String>> headers = new LinkedHashMap<>();
        copyRequestHeaders(req, (key, value) -> headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value));
        proxyReq.setHeaders(headers);

        wsClient.connect(client, uri, proxyReq, new UpgradeListener() {
            @Override
            public void onHandshakeRequest(UpgradeRequest request) {
            }

            @Override
            public void onHandshakeResponse(UpgradeResponse response) {
                resp.setStatus(response.getStatusCode());
                for (String header : response.getHeaderNames()) {
                    switch (header.toLowerCase()) {
                        case "connection":
                        case "date":
                        case "sec-websocket-accept":
                        case "upgrade":
                            continue;
                    }
                    for (String value : response.getHeaders(header)) {
                        resp.addHeader(header, value);
                    }
                }

                try {
                    wsFactory.acceptWebSocket(req, resp);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                async.complete();
            }
        });
    }

    private void copyRequestHeaders(HttpServletRequest req, HeaderConsumer proxyReq) {
        Enumeration<String> headers = req.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            String headerLower = header.toLowerCase();
            switch (headerLower) {
                case "host":
                    if (proxyHost != null) {
                        proxyReq.header(header, proxyHost);
                        continue;
                    }
                    break;
                case "origin":
                    if (proxyBaseUrl != null) {
                        String origin = req.getHeader(header);
                        if (origin.equals("http://localhost:" + port)) {
                            proxyReq.header(header, proxyBaseUrl);
                            continue;
                        }
                    }
                    break;
                case "referer": {
                    String referer = req.getHeader(header);
                    String localUrl = "http://localhost:" + port + "/";
                    if (referer.startsWith(localUrl)) {
                        String relReferer = referer.substring(localUrl.length());
                        proxyReq.header(header, proxyUrl + relReferer);
                        continue;
                    }
                    break;
                }
                case "connection":
                case "upgrade":
                case "user-agent":
                case "sec-websocket-key":
                case "sec-websocket-version":
                case "sec-websocket-extensions":
                case "accept-encoding":
                    continue;
            }
            Enumeration<String> values = req.getHeaders(header);
            while (values.hasMoreElements()) {
                proxyReq.header(header, values.nextElement());
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            wsFactory.stop();
        } catch (Exception e) {
            log.warning("Error stopping WebSocket server", e);
        }
        if (proxyUrl != null) {
            try {
                httpClient.stop();
            } catch (Exception e) {
                log.warning("Error stopping HTTP client", e);
            }
            try {
                wsClient.stop();
            } catch (Exception e) {
                log.warning("Error stopping WebSocket client", e);
            }
        }
        stopped = true;
        synchronized (statusLock) {
            if (waiting) {
                buildThread.interrupt();
            }
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Thread thread = new Thread(this::runTeaVM);
        thread.setName("TeaVM compiler");
        thread.start();
        buildThread = thread;
    }

    private boolean serveSourceFile(String fileName, HttpServletResponse resp) throws IOException {
        try (InputStream stream = sourceFileCache.computeIfAbsent(fileName, this::findSourceFile).get()) {
            if (stream == null) {
                return false;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain");
            IOUtils.copy(stream, resp.getOutputStream());
            resp.getOutputStream().flush();
            return true;
        }
    }

    private Supplier<InputStream> findSourceFile(String fileName) {
        for (String element : sourcePath) {
            File sourceFile = new File(element);
            if (sourceFile.isFile()) {
                Supplier<InputStream> result = findSourceFileInZip(sourceFile, fileName);
                if (result != null) {
                    return result;
                }
            } else if (sourceFile.isDirectory()) {
                File result = new File(sourceFile, fileName);
                if (result.exists()) {
                    return () -> {
                        try {
                            return new FileInputStream(result);
                        } catch (FileNotFoundException e) {
                            return null;
                        }
                    };
                }
            }
        }

        return EMPTY_CONTENT;
    }

    private Supplier<InputStream> findSourceFileInZip(File zipFile, String fileName) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry(fileName);
            if (entry == null) {
                return null;
            }
            return () -> {
                try {
                    ZipInputStream input = new ZipInputStream(new FileInputStream(zipFile));
                    while (true) {
                        ZipEntry e = input.getNextEntry();
                        if (e == null) {
                            return null;
                        }
                        if (e.getName().equals(fileName)) {
                            return input;
                        }
                    }
                } catch (IOException e) {
                    return null;
                }
            };
        } catch (IOException e) {
            return null;
        }
    }

    private void serveBootFile(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain");
        resp.getWriter().write("function main() { }\n");
        resp.getWriter().write(getIndicatorScript(true));
        resp.getWriter().flush();
        log.debug("Served boot file");
    }

    private void runTeaVM() {
        try {
            initBuilder();

            while (!stopped) {
                buildOnce();

                if (stopped) {
                    break;
                }

                try {
                    synchronized (statusLock) {
                        waiting = true;
                    }
                    watcher.waitForChange(750);
                    synchronized (statusLock) {
                        waiting = false;
                    }
                    log.info("Changes detected. Recompiling.");
                } catch (InterruptedException e) {
                    if (stopped) {
                        break;
                    }
                    log.info("Build triggered by user");
                }

                List<String> staleClasses = getChangedClasses(watcher.grabChangedFiles());
                if (staleClasses.size() > 15) {
                    List<String> displayedStaleClasses = staleClasses.subList(0, 10);
                    log.debug("Following classes changed (" + staleClasses.size() + "): "
                            + String.join(", ", displayedStaleClasses) + " and more...");
                } else {
                    log.debug("Following classes changed (" + staleClasses.size() + "): "
                            + String.join(", ", staleClasses));
                }

                classSource.evict(staleClasses);
            }
            log.info("Build process stopped");
        } catch (Throwable e) {
            log.error("Compile server crashed", e);
        } finally {
            shutdownBuilder();
        }
    }

    private void initBuilder() throws IOException {
        watcher = new FileSystemWatcher(classPath);

        classSource = new MemoryCachedClassReaderSource();
        astCache = new InMemoryMethodNodeCache();
        programCache = new InMemoryProgramCache();
    }

    private void shutdownBuilder() {
        try {
            watcher.dispose();
        } catch (IOException e) {
            log.debug("Exception caught", e);
        }
        classSource = null;
        watcher = null;
        astCache = null;
        programCache = null;
        synchronized (content) {
            content.clear();
        }
        buildTarget.clear();

        log.info("Build thread complete");
    }

    private void buildOnce() {
        fireBuildStarted();
        reportProgress(0);

        DebugInformationBuilder debugInformationBuilder = new DebugInformationBuilder();
        ClassLoader classLoader = initClassLoader();
        classSource.setUnderlyingSource(new PreOptimizingClassHolderSource(
                new ClasspathClassHolderSource(classLoader)));

        long startTime = System.currentTimeMillis();
        JavaScriptTarget jsTarget = new JavaScriptTarget();

        TeaVM vm = new TeaVMBuilder(jsTarget)
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .setDependencyAnalyzerFactory(FastDependencyAnalyzer::new)
                .build();

        jsTarget.setStackTraceIncluded(true);
        jsTarget.setMinifying(false);
        jsTarget.setAstCache(astCache);
        jsTarget.setDebugEmitter(debugInformationBuilder);
        jsTarget.setClassScoped(true);
        vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
        vm.setCacheStatus(classSource);
        vm.addVirtualMethods(m -> true);
        vm.setProgressListener(progressListener);
        vm.setProgramCache(programCache);
        vm.installPlugins();

        vm.setLastKnownClasses(lastReachedClasses);
        vm.entryPoint(mainClass);

        log.info("Starting build");
        progressListener.last = 0;
        progressListener.lastTime = System.currentTimeMillis();
        vm.build(buildTarget, fileName);
        addIndicator();
        generateDebug(debugInformationBuilder);

        postBuild(vm, startTime);
    }

    private void addIndicator() {
        String script = getIndicatorScript(false);
        try (Writer writer = new OutputStreamWriter(buildTarget.appendToResource(fileName), StandardCharsets.UTF_8)) {
            writer.append("\n");
            writer.append(script);
        } catch (IOException e) {
            throw new RuntimeException("IO error occurred writing debug information", e);
        }
    }

    private String getIndicatorScript(boolean boot) {
        try (Reader reader = new InputStreamReader(CodeServlet.class.getResourceAsStream("indicator.js"),
                StandardCharsets.UTF_8)) {
            String script = IOUtils.toString(reader);
            script = script.substring(script.indexOf("*/") + 2);
            script = script.replace("WS_PATH", "localhost:" + port + pathToFile + fileName + ".ws");
            script = script.replace("BOOT_FLAG", Boolean.toString(boot));
            script = script.replace("RELOAD_FLAG", Boolean.toString(automaticallyReloaded));
            script = script.replace("INDICATOR_FLAG", Boolean.toString(indicator));
            script = script.replace("DEBUG_PORT", Integer.toString(debugPort));
            script = script.replace("FILE_NAME", "\"" + fileName + "\"");
            script = script.replace("PATH_TO_FILE", "\"http://localhost:" + port + pathToFile + "\"");
            script = script.replace("DEOBFUSCATE_FLAG", String.valueOf(deobfuscateStack));
            return script;
        } catch (IOException e) {
            throw new RuntimeException("IO error occurred writing debug information", e);
        }
    }

    private void generateDebug(DebugInformationBuilder debugInformationBuilder) {
        try {
            DebugInformation debugInformation = debugInformationBuilder.getDebugInformation();
            String sourceMapName = fileName + ".map";

            try (Writer writer = new OutputStreamWriter(buildTarget.appendToResource(fileName),
                    StandardCharsets.UTF_8)) {
                writer.append("\n//# sourceMappingURL=" + sourceMapName);
            }

            try (Writer writer = new OutputStreamWriter(buildTarget.createResource(sourceMapName),
                    StandardCharsets.UTF_8)) {
                debugInformation.writeAsSourceMaps(writer, "src", fileName);
            }
            debugInformation.write(buildTarget.createResource(fileName + ".teavmdbg"));
        } catch (IOException e) {
            throw new RuntimeException("IO error occurred writing debug information", e);
        }
    }

    private void postBuild(TeaVM vm, long startTime) {
        if (!vm.wasCancelled()) {
            log.info("Recompiled stale methods: " + programCache.getPendingItemsCount());
            fireBuildComplete(vm);
            if (vm.getProblemProvider().getSevereProblems().isEmpty()) {
                log.info("Build complete successfully");
                saveNewResult();
                lastReachedClasses = vm.getDependencyInfo().getReachableClasses().size();
                classSource.commit();
                programCache.commit();
                astCache.commit();
                reportCompilationComplete(true);
            } else {
                log.info("Build complete with errors");
                reportCompilationComplete(false);
            }
            printStats(vm, startTime);
            TeaVMProblemRenderer.describeProblems(vm, log);
        } else {
            log.info("Build cancelled");
            fireBuildCancelled();
        }

        astCache.discard();
        programCache.discard();
        buildTarget.clear();
        cancelRequested = false;
    }

    private void printStats(TeaVM vm, long startTime) {
        if (vm.getWrittenClasses() != null) {
            int classCount = vm.getWrittenClasses().getClassNames().size();
            int methodCount = 0;
            for (String className : vm.getWrittenClasses().getClassNames()) {
                ClassReader cls = vm.getWrittenClasses().get(className);
                methodCount += cls.getMethods().size();
            }

            log.info("Classes compiled: " + classCount);
            log.info("Methods compiled: " + methodCount);
        }

        log.info("Compilation took " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private void saveNewResult() {
        synchronized (contentLock) {
            firstTime = false;
            content.clear();
            for (String name : buildTarget.getNames()) {
                content.put(name, buildTarget.getContent(name));
            }
        }
    }

    private List<String> getChangedClasses(Collection<File> changedFiles) {
        List<String> result = new ArrayList<>();

        for (File file : changedFiles) {
            String path = file.getPath();
            if (!path.endsWith(".class")) {
                continue;
            }

            String prefix = Arrays.stream(classPath)
                    .filter(path::startsWith)
                    .findFirst()
                    .orElse("");
            int start = prefix.length();
            if (start < path.length() && path.charAt(start) == '/') {
                ++start;
            }

            path = path.substring(start, path.length() - ".class".length()).replace('/', '.');
            result.add(path);
        }

        return result;
    }

    private ClassLoader initClassLoader() {
        URL[] urls = new URL[classPath.length];
        try {
            for (int i = 0; i < classPath.length; i++) {
                urls[i] = new File(classPath[i]).toURI().toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return new URLClassLoader(urls, CodeServlet.class.getClassLoader());
    }

    private void reportProgress(double progress) {
        synchronized (statusLock) {
            if (compiling && this.progress == progress) {
                return;
            }
            compiling = true;
            this.progress = progress;
        }

        ProgressHandler[] handlers;
        synchronized (progressHandlers) {
            handlers = progressHandlers.toArray(new ProgressHandler[0]);
        }

        for (ProgressHandler handler : handlers) {
            handler.progress(progress);
        }

        for (DevServerListener listener : listeners) {
            listener.compilationProgress(progress);
        }
    }

    private void reportCompilationComplete(boolean success) {
        synchronized (statusLock) {
            if (!compiling) {
                return;
            }
            compiling = false;
        }

        ProgressHandler[] handlers;
        synchronized (progressHandlers) {
            handlers = progressHandlers.toArray(new ProgressHandler[0]);
        }

        for (ProgressHandler handler : handlers) {
            handler.complete(success);
        }
    }

    private void fireBuildStarted() {
        for (DevServerListener listener : listeners) {
            listener.compilationStarted();
        }
    }

    private void fireBuildCancelled() {
        for (DevServerListener listener : listeners) {
            listener.compilationCancelled();
        }
    }

    private void fireBuildComplete(TeaVM vm) {
        CodeServletBuildResult result = new CodeServletBuildResult(vm, new ArrayList<>(buildTarget.getNames()));
        for (DevServerListener listener : listeners) {
            listener.compilationComplete(result);
        }
    }

    private final ProgressListenerImpl progressListener = new ProgressListenerImpl();

    class ProgressListenerImpl implements TeaVMProgressListener {
        private int start;
        private int end;
        private int phaseLimit;
        private int last;
        private long lastTime;

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
            switch (phase) {
                case DEPENDENCY_ANALYSIS:
                    start = 0;
                    end = 500;
                    break;
                case COMPILING:
                    start = 500;
                    end = 1000;
                    break;
            }
            phaseLimit = count;
            return progressReached(0);
        }

        @Override
        public TeaVMProgressFeedback progressReached(int progress) {
            if (indicator) {
                int current = start + Math.min(progress, phaseLimit) * (end - start) / phaseLimit;
                if (current != last) {
                    if (current - last > 10 || System.currentTimeMillis() - lastTime > 100) {
                        lastTime = System.currentTimeMillis();
                        last = current;
                        reportProgress(current / 10.0);
                    }
                }
            }
            return getResult();
        }

        private TeaVMProgressFeedback getResult() {
            if (cancelRequested) {
                log.info("Trying to cancel compilation due to user request");
                return TeaVMProgressFeedback.CANCEL;
            }

            if (stopped) {
                log.info("Trying to cancel compilation due to server stopping");
                return TeaVMProgressFeedback.CANCEL;
            }

            try {
                if (watcher.hasChanges()) {
                    log.info("Changes detected, cancelling build");
                    return TeaVMProgressFeedback.CANCEL;
                }
            } catch (IOException e) {
                log.info("IO error occurred", e);
                return TeaVMProgressFeedback.CANCEL;
            }

            return TeaVMProgressFeedback.CONTINUE;
        }
    }

    static String normalizePath(String path) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }
}
