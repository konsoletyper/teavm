/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.gradle.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.internal.logging.progress.ProgressLogger;
import org.teavm.common.json.JsonArrayValue;
import org.teavm.common.json.JsonObjectValue;
import org.teavm.common.json.JsonParser;
import org.teavm.common.json.JsonValue;

public class ProjectDevServerManager {
    private Set<File> serverClasspath = new LinkedHashSet<>();
    private Set<File> classpath = new LinkedHashSet<>();
    private String targetFileName;
    private String targetFilePath;
    private Map<String, String> properties = new LinkedHashMap<>();
    private Set<String> preservedClasses = new LinkedHashSet<>();
    private String mainClass;
    private boolean stackDeobfuscated;
    private boolean indicator;
    private int port;
    private Set<File> sources = new HashSet<>();
    private boolean autoReload;
    private String proxyUrl;
    private String proxyPath;
    private int processMemory;
    private int debugPort;

    private Process process;
    private Thread processKillHook;
    private Thread commandInputThread;
    private Thread stderrThread;
    private BufferedWriter commandOutput;
    private JsonParser jsonParser;
    private BlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<>();
    private boolean eventQueueDone;
    private Logger logger;
    private ProgressLogger progressLogger;

    private Set<File> runningServerClasspath = new HashSet<>();
    private Set<File> runningClasspath = new HashSet<>();
    private String runningTargetFileName;
    private String runningTargetFilePath;
    private Map<String, String> runningProperties = new HashMap<>();
    private Set<String> runningPreservedClasses = new HashSet<>();
    private String runningMainClass;
    private boolean runningStackDeobfuscated;
    private boolean runningIndicator;
    private int runningPort;
    private Set<File> runningSources = new HashSet<>();
    private boolean runningAutoReload;
    private String runningProxyUrl;
    private String runningProxyPath;
    private int runningProcessMemory;
    private int runningDebugPort;

    ProjectDevServerManager() {
        jsonParser = JsonParser.ofValue(this::parseCommand);
    }

    public void setServerClasspath(Set<File> serverClasspath) {
        this.serverClasspath.clear();
        this.serverClasspath.addAll(serverClasspath);
    }

    public void setClasspath(Set<File> classpath) {
        this.classpath.clear();
        this.classpath.addAll(classpath);
    }

    public void setProperties(Map<String, String> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    public void setPreservedClasses(Collection<String> preservedClasses) {
        this.preservedClasses.clear();
        this.preservedClasses.addAll(preservedClasses);
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public void setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setStackDeobfuscated(boolean stackDeobfuscated) {
        this.stackDeobfuscated = stackDeobfuscated;
    }

    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setSources(Set<File> sources) {
        this.sources.clear();
        this.sources.addAll(sources);
    }

    public void setAutoReload(boolean autoReload) {
        this.autoReload = autoReload;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public void setProxyPath(String proxyPath) {
        this.proxyPath = proxyPath;
    }

    public void setProcessMemory(int processMemory) {
        this.processMemory = processMemory;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public void runBuild(Logger logger, ProgressLogger progressLogger) throws IOException {
        restartIfNecessary(logger);
        try {
            schedule(() -> {
                try {
                    commandOutput.write("{\"type\":\"build\"}\n");
                    commandOutput.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException e) {
            return;
        }
        processQueue(logger, progressLogger);
    }

    private void processQueue(Logger logger, ProgressLogger progressLogger) {
        eventQueueDone = false;
        this.logger = logger;
        this.progressLogger = progressLogger;
        var stoppedUnexpectedly = new boolean[1];
        var processMonitorThread = new Thread(() -> {
            try {
                process.waitFor();
                schedule(() -> {
                    stoppedUnexpectedly[0] = true;
                });
                stopEventQueue();
            } catch (InterruptedException e) {
                // do nothing
            }
        });
        processMonitorThread.setDaemon(true);
        processMonitorThread.setName("Dev server process crash monitor");
        processMonitorThread.start();
        try {
            while (!eventQueueDone || !eventQueue.isEmpty()) {
                Runnable command;
                try {
                    command = eventQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                command.run();
            }
            if (stoppedUnexpectedly[0]) {
                logger.error("Dev server process stopped unexpectedly");
                throw new GradleException();
            }
        } finally {
            this.logger = null;
            this.progressLogger = null;
            processMonitorThread.interrupt();
        }
    }

    private void restartIfNecessary(Logger logger) throws IOException {
        if (process != null && !checkProcess()) {
            logger.info("Changes detected in TeaVM development server config, restarting server");
            stop(logger);
        }
        if (process == null || !process.isAlive()) {
            start(logger);
        }
    }

    public void stop(Logger logger) {
        if (process != null) {
            logger.info("Stopping TeaVM development server, PID = {}", process.pid());
            if (process.isAlive()) {
                try {
                    commandOutput.write("{\"type\":\"stop\"}\n");
                    commandOutput.flush();
                } catch (IOException e) {
                    process.destroy();
                }
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    // do nothing
                }
            } else {
                logger.info("Process was dead");
            }
            process = null;
            Runtime.getRuntime().removeShutdownHook(processKillHook);
            processKillHook = null;
            commandInputThread.interrupt();
            commandInputThread = null;
            stderrThread.interrupt();
            stderrThread = null;
            commandOutput = null;
        } else {
            logger.info("No development server running, doing nothing");
        }
    }

    private void start(Logger logger) throws IOException {
        logger.info("Starting TeaVM development server");

        var pb = new ProcessBuilder();
        pb.command(getBuilderCommand().toArray(new String[0]));

        process = pb.start();
        processKillHook = new Thread(() -> process.destroy());
        Runtime.getRuntime().addShutdownHook(processKillHook);
        commandOutput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        commandInputThread = new Thread(this::readCommandsFromProcess);
        commandInputThread.setName("TeaVM development server command reader");
        commandInputThread.setDaemon(true);
        commandInputThread.start();

        stderrThread = new Thread(this::readStderrFromProcess);
        stderrThread.setName("TeaVM development server stderr reader");
        stderrThread.setDaemon(true);
        stderrThread.start();

        logger.info("Development server started");
    }

    private void readCommandsFromProcess() {
        try (var input = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8))) {
            while (!Thread.currentThread().isInterrupted()) {
                var command = input.readLine();
                if (command == null) {
                    break;
                }
                schedule(() -> readCommand(command));
            }
        } catch (IOException e) {
            try {
                stopEventQueue();
                if (logger != null) {
                    logger.error("IO error occurred reading stdout of development server process", e);
                }
            } catch (InterruptedException e2) {
                if (logger != null) {
                    logger.info("Development server process input thread interrupted");
                }
            }
        } catch (InterruptedException e) {
            if (logger != null) {
                logger.info("Development server process input thread interrupted");
            }
        }
    }

    private void readStderrFromProcess() {
        try (var input = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                StandardCharsets.UTF_8))) {
            while (!Thread.currentThread().isInterrupted()) {
                var line = input.readLine();
                if (line == null) {
                    break;
                }
                schedule(() -> logger.warn("server stderr: {}", line));
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.error("IO error occurred reading stderr of development server process", e);
            }
        } catch (InterruptedException e) {
            if (logger != null) {
                logger.info("Development server process input thread interrupted");
            }
        }
    }

    private void stopEventQueue() throws InterruptedException {
        schedule(() -> eventQueueDone = true);
    }

    private void schedule(Runnable command) throws InterruptedException {
        eventQueue.put(command);
    }

    private void readCommand(String command) {
        try {
            jsonParser.parse(new StringReader(command));
        } catch (IOException e) {
            // This should not happen
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error reading command: " + command, e);
        }
    }

    private void parseCommand(JsonValue command) {
        var obj = (JsonObjectValue) command;
        var type = obj.get("type").asString();
        try {
            switch (type) {
                case "log":
                    logCommand(obj);
                    break;
                case "compilation-started":
                    // do nothing
                    break;
                case "compilation-progress":
                    progressCommand(obj);
                    break;
                case "compilation-complete":
                    completeCommand(obj);
                    break;
                case "compilation-cancelled":
                    stopEventQueue();
                    break;
            }
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private void logCommand(JsonObjectValue command) throws InterruptedException {
        if (logger == null) {
            return;
        }

        var level = command.get("level").asString();
        var message = command.get("message").asString();
        var throwable = command.get("throwable");
        if (throwable != null) {
            message += "\n" + throwable.asString();
        }
        var messageToReport = message;
        switch (level) {
            case "debug":
                schedule(() -> logger.debug(messageToReport));
                break;
            case "info":
                schedule(() -> logger.info(messageToReport));
                break;
            case "warning":
                schedule(() -> logger.warn(messageToReport));
                break;
            case "error":
                schedule(() -> logger.error(messageToReport));
                break;
        }
    }

    private void progressCommand(JsonObjectValue command) throws InterruptedException {
        if (progressLogger == null) {
            return;
        }
        var progress = command.get("progress").asNumber();
        var roundedResult = (int) (progress * 1000 + 5) / 10;
        var result = Math.min(100, roundedResult / 10.0);
        schedule(() -> progressLogger.progress(result + " %"));
    }

    private void completeCommand(JsonObjectValue command) throws InterruptedException {
        var problemsJson = command.get("problems");
        if (problemsJson != null && logger != null) {
            reportProblems((JsonArrayValue) problemsJson);
        }
        stopEventQueue();
    }

    private void reportProblems(JsonArrayValue json) throws InterruptedException {
        var hasSevere = false;
        for (var i = 0; i < json.size(); ++i) {
            var problem = json.get(i).asObject();
            var severity = problem.get("severity").asString();
            var sb = new StringBuilder();
            sb.append(problem.get("message").asString());
            sb.append(problem.get("location").asString());
            var message = sb.toString();
            switch (severity) {
                case "error":
                    hasSevere = true;
                    schedule(() -> logger.error(message));
                    break;
                case "warning":
                    schedule(() -> logger.warn(message));
                    break;
            }
        }
        if (hasSevere) {
            schedule(() -> {
                throw new GradleException("Errors occurred during TeaVM build");
            });
        }
    }

    private List<String> getBuilderCommand() {
        var command = new ArrayList<String>();

        var javaHome = System.getProperty("java.home");
        var javaExec = javaHome + "/bin/java";
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            javaExec += ".exe";
        }
        command.add(javaExec);

        if (!serverClasspath.isEmpty()) {
            command.add("-cp");
            command.add(serverClasspath.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator)));
        }
        runningServerClasspath.clear();
        runningServerClasspath.addAll(serverClasspath);

        if (processMemory != 0) {
            command.add("-Xmx" + processMemory + "m");
        }
        runningProcessMemory = processMemory;

        if (debugPort != 0) {
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=*:" + debugPort);
        }
        runningDebugPort = debugPort;

        command.add("org.teavm.cli.devserver.TeaVMDevServerRunner");
        command.add("--json-interface");
        command.add("--no-watch");

        if (targetFileName != null) {
            command.add("--targetfile");
            command.add(targetFileName);
        }
        runningTargetFileName = targetFileName;

        if (targetFilePath != null) {
            command.add("--targetdir");
            command.add(targetFilePath);
        }
        runningTargetFilePath = targetFilePath;

        if (!classpath.isEmpty()) {
            command.add("--classpath");
            command.addAll(classpath.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList()));
        }
        runningClasspath.clear();
        runningClasspath.addAll(classpath);

        if (!sources.isEmpty()) {
            command.add("--sourcepath");
            command.addAll(sources.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList()));
        }
        runningSources.clear();
        runningSources.addAll(sources);

        if (port != 0) {
            command.add("--port");
            command.add(String.valueOf(port));
        }
        runningPort = port;

        if (indicator) {
            command.add("--indicator");
        }
        runningIndicator = indicator;

        if (stackDeobfuscated) {
            command.add("--deobfuscate-stack");
        }
        runningStackDeobfuscated = stackDeobfuscated;

        if (autoReload) {
            command.add("--auto-reload");
        }
        runningAutoReload = autoReload;

        if (proxyUrl != null) {
            command.add("--proxy-url");
            command.add(proxyUrl);
        }
        runningProxyUrl = proxyUrl;

        if (proxyPath != null) {
            command.add("--proxy-path");
            command.add(proxyPath);
        }
        runningProxyPath = proxyPath;

        for (var entry : properties.entrySet()) {
            command.add("--property");
            command.add(entry.getKey() + "=" + entry.getValue());
        }
        runningProperties.clear();
        runningProperties.putAll(properties);

        if (!preservedClasses.isEmpty()) {
            command.add("--preserved-classes");
            command.addAll(preservedClasses);
        }
        runningPreservedClasses.clear();
        runningPreservedClasses.addAll(preservedClasses);

        command.add("--");
        command.add(mainClass);
        runningMainClass = mainClass;

        return command;
    }

    private boolean checkProcess() {
        return Objects.equals(serverClasspath, runningServerClasspath)
                && Objects.equals(classpath, runningClasspath)
                && Objects.equals(targetFileName, runningTargetFileName)
                && Objects.equals(targetFilePath, runningTargetFilePath)
                && Objects.equals(properties, runningProperties)
                && Objects.equals(preservedClasses, runningPreservedClasses)
                && Objects.equals(mainClass, runningMainClass)
                && stackDeobfuscated == runningStackDeobfuscated
                && indicator == runningIndicator
                && port == runningPort
                && Objects.equals(sources, runningSources)
                && autoReload == runningAutoReload
                && Objects.equals(proxyUrl, runningProxyUrl)
                && Objects.equals(proxyPath, runningProxyPath)
                && processMemory == runningProcessMemory
                && debugPort == runningDebugPort;
    }
}
