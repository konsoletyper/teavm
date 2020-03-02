/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.tooling.c.incremental;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.c.generate.CNameProvider;
import org.teavm.cache.InMemoryMethodNodeCache;
import org.teavm.cache.InMemoryProgramCache;
import org.teavm.cache.InMemorySymbolTable;
import org.teavm.cache.MemoryCachedClassReaderSource;
import org.teavm.dependency.FastDependencyAnalyzer;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathResourceMapper;
import org.teavm.parsing.resource.ClasspathResourceReader;
import org.teavm.parsing.resource.ResourceClassHolderMapper;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.builder.SimpleBuildResult;
import org.teavm.tooling.util.FileSystemWatcher;
import org.teavm.vm.IncrementalDirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public class IncrementalCBuilder {
    private String mainClass;
    private String[] classPath;
    private int minHeapSize = 4;
    private int maxHeapSize = 128;
    private boolean longjmpSupported = true;
    private boolean lineNumbersGenerated;
    private String targetPath;
    private String externalTool;
    private String externalToolWorkingDir;
    private String mainFunctionName;

    private IncrementalDirectoryBuildTarget buildTarget;
    private FileSystemWatcher watcher;
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private MemoryCachedClassReaderSource classSource;

    private ReferenceCache referenceCache = new ReferenceCache();
    private InMemorySymbolTable symbolTable = new InMemorySymbolTable();
    private InMemorySymbolTable fileSymbolTable = new InMemorySymbolTable();
    private InMemorySymbolTable variableSymbolTable = new InMemorySymbolTable();
    private InMemoryProgramCache programCache;
    private InMemoryMethodNodeCache astCache;

    private List<BuilderListener> listeners = new ArrayList<>();
    private final Set<ProgressHandler> progressHandlers = new LinkedHashSet<>();
    private final CNameProvider nameProvider = new CNameProvider();

    private int lastReachedClasses;
    private final Object statusLock = new Object();
    private volatile boolean cancelRequested;
    private volatile boolean stopped;
    private boolean compiling;
    private double progress;
    private boolean waiting;
    private Thread buildThread;

    private boolean needsExternalTool;

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setClassPath(String[] classPath) {
        this.classPath = classPath;
    }

    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    public void setMaxHeapSize(int maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    public void setLineNumbersGenerated(boolean lineNumbersGenerated) {
        this.lineNumbersGenerated = lineNumbersGenerated;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public void setExternalTool(String externalTool) {
        this.externalTool = externalTool;
    }

    public void setExternalToolWorkingDir(String externalToolWorkingDir) {
        this.externalToolWorkingDir = externalToolWorkingDir;
    }

    public void setMainFunctionName(String mainFunctionName) {
        this.mainFunctionName = mainFunctionName;
    }

    public void setLongjmpSupported(boolean longjmpSupported) {
        this.longjmpSupported = longjmpSupported;
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

    public void addListener(BuilderListener listener) {
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
            symbolTable.invalidate();
            fileSymbolTable.invalidate();
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

    public void start() {
        buildThread = new Thread(this::run);
        buildThread.start();
    }

    public void stop() {
        stopped = true;
        synchronized (statusLock) {
            if (waiting) {
                buildThread.interrupt();
            }
        }
    }

    private void run() {
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

    private List<String> getChangedClasses(Collection<File> changedFiles) {
        List<String> result = new ArrayList<>();
        String[] prefixes = Arrays.stream(classPath).map(s -> s.replace('\\', '/')).toArray(String[]::new);

        for (File file : changedFiles) {
            String path = file.getPath().replace('\\', '/');
            if (!path.endsWith(".class")) {
                continue;
            }

            String prefix = Arrays.stream(prefixes)
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

    private void initBuilder() throws IOException {
        buildTarget = new IncrementalDirectoryBuildTarget(new File(targetPath));
        watcher = new FileSystemWatcher(classPath);

        classSource = createCachedSource();
        astCache = new InMemoryMethodNodeCache(referenceCache, symbolTable, fileSymbolTable, variableSymbolTable);
        programCache = new InMemoryProgramCache(referenceCache, symbolTable, fileSymbolTable, variableSymbolTable);
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

        log.info("Build thread complete");
    }

    private MemoryCachedClassReaderSource createCachedSource() {
        return new MemoryCachedClassReaderSource(referenceCache, symbolTable, fileSymbolTable,
                variableSymbolTable);
    }

    private void buildOnce() {
        fireBuildStarted();
        reportProgress(0);

        ClassLoader classLoader = initClassLoader();
        ClasspathResourceReader reader = new ClasspathResourceReader(classLoader);
        ResourceClassHolderMapper rawMapper = new ResourceClassHolderMapper(reader, referenceCache);
        Function<String, ClassHolder> classPathMapper = new ClasspathResourceMapper(classLoader, referenceCache,
                rawMapper);
        classSource.setProvider(name -> PreOptimizingClassHolderSource.optimize(classPathMapper, name));

        long startTime = System.currentTimeMillis();
        CTarget cTarget = new CTarget(nameProvider);
        cTarget.setAstCache(astCache);

        TeaVM vm = new TeaVMBuilder(cTarget)
                .setReferenceCache(referenceCache)
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .setDependencyAnalyzerFactory(FastDependencyAnalyzer::new)
                .setClassSourcePacker(this::packClasses)
                .setStrict(true)
                .setObfuscated(false)
                .build();

        cTarget.setIncremental(true);
        cTarget.setMinHeapSize(minHeapSize * 1024 * 1024);
        cTarget.setMaxHeapSize(maxHeapSize * 1024 * 1024);
        cTarget.setLineNumbersGenerated(lineNumbersGenerated);
        cTarget.setLongjmpUsed(longjmpSupported);
        cTarget.setHeapDump(true);
        vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
        vm.setCacheStatus(classSource);
        vm.addVirtualMethods(m -> true);
        vm.setProgressListener(progressListener);
        vm.setProgramCache(programCache);
        vm.installPlugins();

        vm.setLastKnownClasses(lastReachedClasses);
        vm.entryPoint(mainClass, mainFunctionName != null ? mainFunctionName : "main");

        log.info("Starting build");
        progressListener.last = 0;
        progressListener.lastTime = System.currentTimeMillis();
        vm.build(buildTarget, "");

        postBuild(vm, startTime);

        runExternalTool();
    }

    private void postBuild(TeaVM vm, long startTime) {
        needsExternalTool = false;
        boolean hasErrors = false;
        if (!vm.wasCancelled()) {
            log.info("Recompiled stale methods: " + programCache.getPendingItemsCount());
            fireBuildComplete(vm);
            if (vm.getProblemProvider().getSevereProblems().isEmpty()) {
                log.info("Build complete successfully");
                lastReachedClasses = vm.getDependencyInfo().getReachableClasses().size();
                classSource.commit();
                programCache.commit();
                astCache.commit();
                reportCompilationComplete(true);
                needsExternalTool = true;
            } else {
                hasErrors = true;
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
        if (!vm.wasCancelled() && !hasErrors) {
            buildTarget.reset();
        }
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

    private void runExternalTool() {
        if (externalTool == null || !needsExternalTool) {
            return;
        }

        try {
            log.info("Running external tool");
            long start = System.currentTimeMillis();
            ProcessBuilder pb = new ProcessBuilder(externalTool);
            if (externalToolWorkingDir != null) {
                pb.directory(new File(externalToolWorkingDir));
            }
            Process process = pb.start();
            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(),
                    StandardCharsets.UTF_8));
            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                    StandardCharsets.UTF_8));
            daemon("external tool stdout watcher", new ExternalOutputWatcher(stdoutReader,
                    s -> log.info("[external tool] " + s)));
            daemon("external tool stderr watcher", new ExternalOutputWatcher(stderrReader,
                    s -> log.error("[external tool] " + s)));

            int code = process.waitFor();
            if (code != 0) {
                log.error("External tool returned non-zero code: " + code);
            } else {
                log.info("External tool took " + (System.currentTimeMillis() - start) + " ms");
            }
        } catch (IOException e) {
            log.error("Could not start external tool", e);
        } catch (InterruptedException e) {
            log.info("Interrupted while running external tool");
        }
    }

    private static Thread daemon(String name, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(name);
        thread.start();
        return thread;
    }

    private ClassReaderSource packClasses(ClassReaderSource source, Collection<? extends String> classNames) {
        MemoryCachedClassReaderSource packedSource = createCachedSource();
        packedSource.setProvider(source::get);
        for (String className : classNames) {
            packedSource.populate(className);
        }
        packedSource.setProvider(null);
        return packedSource;
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
        return new URLClassLoader(urls, IncrementalCBuilder.class.getClassLoader());
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

        for (BuilderListener listener : listeners) {
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
        for (BuilderListener listener : listeners) {
            listener.compilationStarted();
        }
    }

    private void fireBuildCancelled() {
        for (BuilderListener listener : listeners) {
            listener.compilationCancelled();
        }
    }

    private void fireBuildComplete(TeaVM vm) {
        SimpleBuildResult result = new SimpleBuildResult(vm, Collections.emptyList());
        for (BuilderListener listener : listeners) {
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
            int current = start + Math.min(progress, phaseLimit) * (end - start) / phaseLimit;
            if (current != last) {
                if (current - last > 10 || System.currentTimeMillis() - lastTime > 100) {
                    lastTime = System.currentTimeMillis();
                    last = current;
                    reportProgress(current / 10.0);
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

    class ExternalOutputWatcher implements Runnable {
        private BufferedReader reader;
        private Consumer<String> consumer;

        ExternalOutputWatcher(BufferedReader reader, Consumer<String> consumer) {
            this.reader = reader;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    consumer.accept(line);
                }
            } catch (IOException e) {
                log.error("Error reading build daemon output", e);
            }
        }
    }
}
