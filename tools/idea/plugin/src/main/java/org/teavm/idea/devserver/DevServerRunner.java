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
package org.teavm.idea.devserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import org.teavm.devserver.DevServer;
import org.teavm.devserver.DevServerListener;
import org.teavm.idea.DevServerRunnerListener;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.builder.BuildResult;

public class DevServerRunner extends UnicastRemoteObject implements DevServerManager {
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 1 << 16;
    private static final String PORT_MESSAGE_PREFIX = "Build server port: ";
    private static final String DEBUG_PORT_PROPERTY = "teavm.server.debug.port";
    private int port;
    private Registry registry;
    private DevServer server;
    private final List<DevServerManagerListener> listeners = new ArrayList<>();

    private DevServerRunner(DevServer server) throws RemoteException {
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
                registry.bind(ID, this);
            } catch (RemoteException | AlreadyBoundException e) {
                throw new IllegalStateException("Could not bind remote service", e);
            }

            this.server = server;
            server.addListener(devServerListener);

            return;
        }
        throw new IllegalStateException("Could not create RMI registry");
    }

    @Override
    public void stop() {
        server.stop();
    }

    @Override
    public void invalidateCache() {
        server.invalidateCache();
    }

    @Override
    public void buildProject() {
        server.buildProject();
    }

    @Override
    public void cancelBuild() {
        server.cancelBuild();
    }

    @Override
    public void addListener(DevServerManagerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(DevServerManagerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public static void main(String[] args) throws Exception {
        DevServer server = new DevServer();
        server.setLog(new ConsoleTeaVMToolLog(false));
        server.setMainClass(args[0]);
        List<String> classPath = new ArrayList<>();
        for (int i = 1; i < args.length; ++i) {
            switch (args[i]) {
                case "-c":
                    classPath.add(args[++i]);
                    break;
                case "-s":
                    server.getSourcePath().add(args[++i]);
                    break;
                case "-i":
                    server.setIndicator(true);
                    break;
                case "-deobf":
                    server.setDeobfuscateStack(true);
                    break;
                case "-a":
                    server.setReloadedAutomatically(true);
                    break;
                case "-p":
                    server.setPort(Integer.parseInt(args[++i]));
                    break;
                case "-d":
                    server.setPathToFile(args[++i]);
                    break;
                case "-f":
                    server.setFileName(args[++i]);
                    break;
                case "-P":
                    server.setDebugPort(Integer.parseInt(args[++i]));
                    break;
                case "-proxy-url":
                    server.setProxyUrl(args[++i]);
                    break;
                case "-proxy-path":
                    server.setProxyPath(args[++i]);
                    break;
            }
        }
        server.setClassPath(classPath.toArray(new String[0]));

        DevServerRunner daemon = new DevServerRunner(server);
        System.out.println(PORT_MESSAGE_PREFIX + daemon.port);
        server.start();

        try {
            daemon.registry.unbind(ID);
            UnicastRemoteObject.unexportObject(daemon, true);
        } catch (NoSuchObjectException e) {
            throw new IllegalStateException("Could not shutdown RMI registry", e);
        }
    }

    public static DevServerInfo start(String[] classPathEntries, DevServerConfiguration options,
            DevServerRunnerListener listener) throws IOException {
        String javaCommand = options.javaHome + "/bin/java";
        String classPath = String.join(File.pathSeparator, classPathEntries);
        List<String> arguments = new ArrayList<>();

        arguments.addAll(Arrays.asList(javaCommand, "-cp", classPath, "-Xmx" + options.maxHeap + "m"));

        String debugPort = System.getProperty(DEBUG_PORT_PROPERTY);
        if (debugPort != null) {
            arguments.add("-agentlib:jdwp=transport=dt_socket,quiet=y,server=y,address=" + debugPort + ",suspend=y");
        }

        arguments.add(DevServerRunner.class.getName());
        arguments.add(options.mainClass);

        if (options.indicator) {
            arguments.add("-i");
        }
        if (options.deobfuscateStack) {
            arguments.add("-deobf");
        }
        if (options.autoReload) {
            arguments.add("-a");
        }
        arguments.add("-d");
        arguments.add(options.pathToFile);
        arguments.add("-f");
        arguments.add(options.fileName);
        arguments.add("-p");
        arguments.add(Integer.toString(options.port));

        for (String entry : options.classPath) {
            arguments.add("-c");
            arguments.add(entry);
        }
        for (String entry : options.sourcePath) {
            arguments.add("-s");
            arguments.add(entry);
        }

        if (options.debugPort > 0) {
            arguments.add("-P");
            arguments.add(Integer.toString(options.debugPort));
        }

        if (options.proxyUrl != null && !options.proxyUrl.isEmpty()) {
            arguments.add("-proxy-url");
            arguments.add(options.proxyUrl);
        }
        if (options.proxyPath != null && !options.proxyPath.isEmpty()) {
            arguments.add("-proxy-path");
            arguments.add(options.proxyPath);
        }

        ProcessBuilder builder = new ProcessBuilder(arguments.toArray(new String[0]));
        Process process = builder.start();
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8));
        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                StandardCharsets.UTF_8));
        String line = stdoutReader.readLine();
        if (line == null || !line.startsWith(PORT_MESSAGE_PREFIX)) {
            StringBuilder sb = new StringBuilder();
            while (true) {
                line = stderrReader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append('\n');
            }
            stderrReader.close();
            stdoutReader.close();
            process.destroy();
            throw new IllegalStateException("Could not start daemon. Stderr: " + sb);
        }
        int port = Integer.parseInt(line.substring(PORT_MESSAGE_PREFIX.length()));

        daemonThread("TeaVM devserver stdout", new ProcessOutputWatcher(stdoutReader, listener::info)).start();
        daemonThread("TeaVM devserver stderr", new ProcessOutputWatcher(stderrReader, listener::error)).start();
        daemonThread("TeaVM devserver monitor", () -> {
            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                return;
            }
            listener.stopped(exitCode);
        }).start();

        DevServerManager service;
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            service = (DevServerManager) registry.lookup(ID);
        } catch (RemoteException | NotBoundException e) {
            throw new RuntimeException("Error connecting TeaVM process", e);
        }

        return new DevServerInfo(port, service, process);
    }

    private static Thread daemonThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(name);
        thread.setDaemon(true);
        return thread;
    }

    static class ProcessOutputWatcher implements Runnable {
        private BufferedReader reader;
        private Consumer<String> consumer;

        ProcessOutputWatcher(BufferedReader reader, Consumer<String> consumer) {
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
                e.printStackTrace();
            }
        }
    }

    private List<DevServerManagerListener> getListeners() {
        synchronized (listeners) {
            return new ArrayList<>(listeners);
        }
    }

    final DevServerListener devServerListener = new DevServerListener() {
        @Override
        public void compilationStarted() {
            for (DevServerManagerListener listener : getListeners()) {
                try {
                    listener.compilationStarted();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void compilationProgress(double v) {
            for (DevServerManagerListener listener : getListeners()) {
                try {
                    listener.compilationProgress(v);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void compilationComplete(BuildResult buildResult) {
            DevServerBuildResult result = new DevServerBuildResult();
            result.problems.addAll(buildResult.getProblems().getProblems());
            for (DevServerManagerListener listener : getListeners()) {
                try {
                    listener.compilationComplete(result);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void compilationCancelled() {
            for (DevServerManagerListener listener : getListeners()) {
                try {
                    listener.compilationCancelled();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
