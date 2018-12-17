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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
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

    private String mainClass;
    private String[] classPath;
    private String fileName = "classes.js";
    private String pathToFile = "/";
    private List<String> sourcePath = new ArrayList<>();
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private boolean indicator;
    private boolean automaticallyReloaded;
    private int port;
    private int debugPort;

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

    private final Set<CodeWsEndpoint> wsEndpoints = new LinkedHashSet<>();
    private final Object statusLock = new Object();
    private volatile boolean cancelRequested;
    private boolean compiling;
    private double progress;
    private boolean waiting;
    private Thread buildThread;
    private List<DevServerListener> listeners = new ArrayList<>();

    public CodeServlet(String mainClass, String[] classPath) {
        this.mainClass = mainClass;
        this.classPath = classPath.clone();
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPathToFile(String pathToFile) {
        if (!pathToFile.endsWith("/")) {
            pathToFile += "/";
        }
        if (!pathToFile.startsWith("/")) {
            pathToFile = "/" + pathToFile;
        }
        this.pathToFile = pathToFile;
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

    public void setPort(int port) {
        this.port = port;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public void setAutomaticallyReloaded(boolean automaticallyReloaded) {
        this.automaticallyReloaded = automaticallyReloaded;
    }

    public void addWsEndpoint(CodeWsEndpoint endpoint) {
        synchronized (wsEndpoints) {
            wsEndpoints.add(endpoint);
        }

        double progress;
        synchronized (statusLock) {
            if (!compiling) {
                return;
            }
            progress = this.progress;
        }

        endpoint.progress(progress);
    }

    public void removeWsEndpoint(CodeWsEndpoint endpoint) {
        synchronized (wsEndpoints) {
            wsEndpoints.remove(endpoint);
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path != null) {
            log.debug("Serving " + path);
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.startsWith(pathToFile) && path.length() > pathToFile.length()) {
                String fileName = path.substring(pathToFile.length());
                if (fileName.startsWith("src/")) {
                    if (serveSourceFile(fileName.substring("src/".length()), resp)) {
                        log.debug("File " + path + " served as source file");
                        return;
                    }
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
        }

        log.debug("File " + path + " not found");
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    public void destroy() {
        super.destroy();
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
            script = script.replace("FILE_NAME", "http://localhost:" + port + pathToFile + fileName);
            script = script.replace("INDICATOR_FLAG", Boolean.toString(indicator));
            script = script.replace("DEBUG_PORT", Integer.toString(debugPort));
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

        CodeWsEndpoint[] endpoints;
        synchronized (wsEndpoints) {
            endpoints = wsEndpoints.toArray(new CodeWsEndpoint[0]);
        }

        for (CodeWsEndpoint endpoint : endpoints) {
            endpoint.progress(progress);
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

        CodeWsEndpoint[] endpoints;
        synchronized (wsEndpoints) {
            endpoints = wsEndpoints.toArray(new CodeWsEndpoint[0]);
        }

        for (CodeWsEndpoint endpoint : endpoints) {
            endpoint.complete(success);
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
                    end = 400;
                    break;
                case LINKING:
                    start = 400;
                    end = 500;
                    break;
                case OPTIMIZATION:
                    start = 500;
                    end = 750;
                    break;
                case RENDERING:
                    start = 750;
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
}
