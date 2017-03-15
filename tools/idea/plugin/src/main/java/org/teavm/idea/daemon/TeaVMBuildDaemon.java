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
import java.util.stream.Collectors;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildCallback;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildRequest;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildResponse;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildService;
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
    private int port;
    private Registry registry;

    static {
        int depth = 0;
        for (int i = 0; i < DAEMON_CLASS.length(); ++i) {
            if (DAEMON_CLASS.charAt(i) == '/') {
                depth++;
            }
        }
        DAEMON_CLASS_DEPTH = depth;
    }

    TeaVMBuildDaemon() throws RemoteException {
        super();
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
            return;
        }
        throw new IllegalStateException("Could not create RMI registry");
    }

    public static void main(String[] args) throws RemoteException {
        TeaVMBuildDaemon daemon = new TeaVMBuildDaemon();
        System.out.println(DAEMON_MESSAGE_PREFIX + daemon.port);
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public TeaVMRemoteBuildResponse build(TeaVMRemoteBuildRequest request, TeaVMRemoteBuildCallback callback)
            throws RemoteException {
        TeaVMTool tool = new TeaVMTool();
        tool.setProgressListener(createProgressListener(callback));
        tool.setLog(new EmptyTeaVMToolLog());
        tool.setTargetType(request.targetType);
        tool.setMainClass(request.mainClass);
        tool.setTargetDirectory(new File(request.targetDirectory));
        tool.setClassLoader(buildClassLoader(request.classPath));

        tool.setSourceMapsFileGenerated(request.sourceMapsFileGenerated);
        tool.setDebugInformationGenerated(request.debugInformationGenerated);
        tool.setSourceFilesCopied(request.sourceFilesCopied);

        for (String sourceDirectory : request.sourceDirectories) {
            tool.addSourceFileProvider(new DirectorySourceFileProvider(new File(sourceDirectory)));
        }
        for (String sourceJar : request.sourceJarFiles) {
            tool.addSourceFileProvider(new JarSourceFileProvider(new File(sourceJar)));
        }

        boolean errorOccurred = false;
        try {
            tool.generate();
        } catch (TeaVMToolException | RuntimeException | Error e) {
            e.printStackTrace(System.err);
            errorOccurred = true;
        }

        TeaVMRemoteBuildResponse response = new TeaVMRemoteBuildResponse();
        response.errorOccurred = errorOccurred;
        response.callGraph = tool.getDependencyInfo().getCallGraph();
        response.problems.addAll(tool.getProblemProvider().getProblems());
        response.severeProblems.addAll(tool.getProblemProvider().getSevereProblems());
        response.classes.addAll(tool.getClasses());
        response.usedResources.addAll(tool.getUsedResources());

        return response;
    }

    private ClassLoader buildClassLoader(List<String> classPathEntries) {
        URL[] urls = classPathEntries.stream().map(entry -> {
            try {
                return new File(entry).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(entry);
            }
        }).toArray(URL[]::new);

        return new URLClassLoader(urls);
    }

    private TeaVMProgressListener createProgressListener(TeaVMRemoteBuildCallback callback) {
        return new TeaVMProgressListener() {
            private long lastReportedTime;

            @Override
            public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
                if ((System.currentTimeMillis() - lastReportedTime) > 100) {
                    lastReportedTime = System.currentTimeMillis();
                    try {
                        return callback.phaseStarted(phase, count);
                    } catch (RemoteException e) {
                        return TeaVMProgressFeedback.CANCEL;
                    }
                } else {
                    return TeaVMProgressFeedback.CONTINUE;
                }
            }

            @Override
            public TeaVMProgressFeedback progressReached(int progress) {
                if ((System.currentTimeMillis() - lastReportedTime) > 100) {
                    lastReportedTime = System.currentTimeMillis();
                    try {
                        return callback.progressReached(progress);
                    } catch (RemoteException e) {
                        return TeaVMProgressFeedback.CANCEL;
                    }
                } else {
                    return TeaVMProgressFeedback.CONTINUE;
                }
            }
        };
    }

    public static TeaVMDaemonInfo start() throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaCommand = javaHome + "/bin/java";
        String classPath = detectClassPath().stream().collect(Collectors.joining(File.pathSeparator));
        ProcessBuilder builder = new ProcessBuilder(javaCommand, "-cp", classPath, TeaVMBuildDaemon.class.getName());
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        String line = reader.readLine();
        if (line == null || !line.startsWith(DAEMON_MESSAGE_PREFIX)) {
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append('\n');
            }
            throw new IllegalStateException("Could not start daemon. Stderr: " + sb);
        }
        int port = Integer.parseInt(line.substring(DAEMON_MESSAGE_PREFIX.length()));
        return new TeaVMDaemonInfo(port, process);
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
