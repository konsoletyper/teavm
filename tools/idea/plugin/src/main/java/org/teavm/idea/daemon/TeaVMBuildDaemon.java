/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.idea.daemon;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildCallback;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildRequest;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildResponse;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildService;
import org.teavm.idea.jps.util.ExceptionUtil;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.tooling.sources.JarSourceFileProvider;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public class TeaVMBuildDaemon extends UnicastRemoteObject implements TeaVMRemoteBuildService {
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 1 << 16;
    private static final Set<String> KOTLIN_FILES = new HashSet<>(Arrays.asList("teavm-jps-common.jar",
            "teavm-plugin.jar", "teavm.jar"));
    private static final String DAEMON_CLASS = TeaVMBuildDaemon.class.getName().replace('.', '/') + ".class";
    private static final int DAEMON_CLASS_DEPTH;
    private static final String DAEMON_MESSAGE_PREFIX = "TeaVM daemon port: ";
    private static final String INCREMENTAL_PROPERTY = "teavm.daemon.incremental";
    private boolean incremental;
    private int port;
    private Registry registry;
    private File incrementalCache;
    private ClassLoader lastJarClassLoader;
    private List<String> lastJarClassPath;

    static {
        int depth = 0;
        for (int i = 0; i < DAEMON_CLASS.length(); ++i) {
            if (DAEMON_CLASS.charAt(i) == '/') {
                depth++;
            }
        }
        DAEMON_CLASS_DEPTH = depth;
    }

    TeaVMBuildDaemon(boolean incremental) throws RemoteException {
        super();
        this.incremental = incremental;
        Random random = new Random();
        for (int i = 0; i < 20; ++i) {
            port = random.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;
            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                continue;
            }
            try {
                registry.bind(TeaVMRemoteBuildService.ID, this);
            } catch (RemoteException | AlreadyBoundException e) {
                throw new IllegalStateException("Could not bind remote build assistant service", e);
            }

            setupIncrementalCache();

            return;
        }
        throw new IllegalStateException("Could not create RMI registry");
    }

    private void setupIncrementalCache() {
        if (!incremental) {
            return;
        }

        Thread mainThread = Thread.currentThread();
        try {
            incrementalCache = Files.createTempDirectory("teavm-cache").toFile();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (incrementalCache != null) {
                        FileUtils.deleteDirectory(incrementalCache);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            System.err.println("Could not setup incremental cache");
            e.printStackTrace(System.err);
            incremental = false;
        }
    }

    public static void main(String[] args) throws RemoteException {
        boolean incremental = Boolean.parseBoolean(System.getProperty(INCREMENTAL_PROPERTY, "false"));
        TeaVMBuildDaemon daemon = new TeaVMBuildDaemon(incremental);
        System.out.println(DAEMON_MESSAGE_PREFIX + daemon.port);
        if (daemon.incrementalCache != null) {
            System.out.println("Incremental cache set up in " + daemon.incrementalCache);
        }
    }

    @Override
    public TeaVMRemoteBuildResponse build(TeaVMRemoteBuildRequest request, TeaVMRemoteBuildCallback callback)
            throws RemoteException {
        System.out.println("Build started");

        if (!request.incremental && incremental) {
            try {
                System.out.println("Dropping incremental cache");
                FileUtils.cleanDirectory(incrementalCache);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        TeaVMTool tool = new TeaVMTool();
        tool.setIncremental(incremental && request.incremental);
        if (tool.isIncremental()) {
            tool.setCacheDirectory(incrementalCache);
        }
        tool.setProgressListener(createProgressListener(callback));
        tool.setLog(new EmptyTeaVMToolLog());
        tool.setTargetType(request.targetType);
        tool.setMainClass(request.mainClass);
        tool.setTargetDirectory(new File(request.targetDirectory));
        tool.setClassLoader(buildClassLoader(request.classPath, incremental && request.incremental));

        tool.setSourceMapsFileGenerated(request.sourceMapsFileGenerated);
        tool.setDebugInformationGenerated(request.debugInformationGenerated);
        tool.setSourceFilesCopied(request.sourceFilesCopied);
        tool.getProperties().putAll(request.properties);

        tool.setMinifying(false);

        for (String sourceDirectory : request.sourceDirectories) {
            tool.addSourceFileProvider(new DirectorySourceFileProvider(new File(sourceDirectory)));
        }
        for (String sourceJar : request.sourceJarFiles) {
            tool.addSourceFileProvider(new JarSourceFileProvider(new File(sourceJar)));
        }

        boolean errorOccurred = false;
        String stackTrace = null;
        try {
            tool.generate();
            System.out.println("Build complete");
        } catch (TeaVMToolException | RuntimeException | Error e) {
            stackTrace = ExceptionUtil.exceptionToString(e);
            errorOccurred = true;
        }

        TeaVMRemoteBuildResponse response = new TeaVMRemoteBuildResponse();
        response.errorOccurred = errorOccurred;
        response.stackTrace = stackTrace;
        response.callGraph = tool.getDependencyInfo().getCallGraph();
        response.problems.addAll(tool.getProblemProvider().getProblems());
        response.severeProblems.addAll(tool.getProblemProvider().getSevereProblems());
        response.classes.addAll(tool.getClasses());
        response.usedResources.addAll(tool.getUsedResources());
        response.generatedFiles.addAll(tool.getGeneratedFiles().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet()));

        return response;
    }

    private ClassLoader buildClassLoader(List<String> classPathEntries, boolean incremental) {
        System.out.println("Classpath: " + classPathEntries);
        Function<String, URL> mapper = entry -> {
            try {
                return new File(entry).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(entry);
            }
        };
        List<String> jarEntries = classPathEntries.stream()
                .filter(entry -> entry.endsWith(".jar"))
                .collect(Collectors.toList());

        ClassLoader jarClassLoader = null;

        if (incremental) {
            if (jarEntries.equals(lastJarClassPath) && lastJarClassLoader != null) {
                jarClassLoader = lastJarClassLoader;
                System.out.println("Reusing previous class path");
            }
        } else {
            lastJarClassLoader = null;
            lastJarClassPath = null;
        }
        if (jarClassLoader == null) {
            URL[] jarUrls = jarEntries.stream()
                    .map(mapper)
                    .toArray(URL[]::new);
            jarClassLoader = new URLClassLoader(jarUrls);
        }
        if (incremental) {
            lastJarClassPath = jarEntries;
            lastJarClassLoader = jarClassLoader;
        }

        URL[] urls = classPathEntries.stream()
                .filter(entry -> !entry.endsWith(".jar"))
                .map(mapper)
                .toArray(URL[]::new);

        return new URLClassLoader(urls, jarClassLoader);
    }

    private TeaVMProgressListener createProgressListener(TeaVMRemoteBuildCallback callback) {
        return new TeaVMProgressListener() {
            private long lastReportedTime;

            @Override
            public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
                try {
                    return callback.phaseStarted(phase, count);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TeaVMProgressFeedback progressReached(int progress) {
                if ((System.currentTimeMillis() - lastReportedTime) > 100) {
                    lastReportedTime = System.currentTimeMillis();
                    try {
                        return callback.progressReached(progress);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return TeaVMProgressFeedback.CONTINUE;
                }
            }
        };
    }

    public static TeaVMDaemonInfo start(boolean incremental, int daemonMemory) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaCommand = javaHome + "/bin/java";
        String classPath = String.join(File.pathSeparator, detectClassPath());
        ProcessBuilder builder = new ProcessBuilder(javaCommand, "-cp", classPath,
                "-D" + INCREMENTAL_PROPERTY + "=" + incremental,
                "-Xmx" + daemonMemory + "m",
                TeaVMBuildDaemon.class.getName());
        Process process = builder.start();

        Log log = LogFactory.getLog(TeaVMBuildDaemon.class);

        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8));
        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                StandardCharsets.UTF_8));
        String line = stdoutReader.readLine();
        if (line == null || !line.startsWith(DAEMON_MESSAGE_PREFIX)) {
            StringBuilder sb = new StringBuilder();
            while (true) {
                line = stderrReader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append('\n');
            }
            IOUtils.closeQuietly(stderrReader);
            IOUtils.closeQuietly(stdoutReader);
            throw new IllegalStateException("Could not start daemon. Stderr: " + sb);
        }
        int port = Integer.parseInt(line.substring(DAEMON_MESSAGE_PREFIX.length()));

        daemonThread(new DaemonProcessOutputWatcher(log, stdoutReader, "stdout", false)).start();
        daemonThread(new DaemonProcessOutputWatcher(log, stderrReader, "stderr", true)).start();

        return new TeaVMDaemonInfo(port, process);
    }

    private static Thread daemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    }

    static class DaemonProcessOutputWatcher implements Runnable {
        private Log log;
        private BufferedReader reader;
        private String name;
        private boolean isError;

        public DaemonProcessOutputWatcher(Log log, BufferedReader reader, String name, boolean isError) {
            this.log = log;
            this.reader = reader;
            this.name = name;
            this.isError = isError;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (isError) {
                        log.error("Build daemon [" + name + "]: " + line);
                    } else {
                        log.info("Build daemon [" + name + "]: " + line);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading build daemon output", e);
            }
        }
    }

    private static List<String> detectClassPath() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("org.teavm.idea"));
        Set<File> visited = new HashSet<>();
        List<String> classPath = new ArrayList<>();
        findInHierarchy(plugin.getPath(), classPath, visited);
        return classPath;
    }

    private static void findInHierarchy(File file, List<String> targetFiles, Set<File> visited) {
        if (!visited.add(file)) {
            return;
        }
        if (file.isFile() && KOTLIN_FILES.contains(file.getName())) {
            targetFiles.add(file.getAbsolutePath());
        } else if (file.getPath().endsWith(DAEMON_CLASS)) {
            for (int i = 0; i <= DAEMON_CLASS_DEPTH; ++i) {
                file = file.getParentFile();
            }
            targetFiles.add(file.getAbsolutePath());
        } else if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                findInHierarchy(childFile, targetFiles, visited);
            }
        }
    }
}
