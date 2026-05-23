/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.devserver.client;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.common.json.JsonArrayValue;
import org.teavm.common.json.JsonObjectValue;
import org.teavm.common.json.JsonParser;
import org.teavm.common.json.JsonValue;
import org.teavm.diagnostics.ProblemSeverity;

public class DevServerClient {
    private Set<File> serverClasspath = new LinkedHashSet<>();
    private Set<File> classpath = new LinkedHashSet<>();
    private String targetFileName;
    private String targetFilePath;
    private Map<String, String> properties = new LinkedHashMap<>();
    private Set<String> preservedClasses = new LinkedHashSet<>();
    private JSModuleType jsModuleType;
    private String mainClass;
    private boolean stackDeobfuscated;
    private boolean indicator;
    private int port;
    private Set<File> sources = new LinkedHashSet<>();
    private boolean autoReload;
    private String proxyUrl;
    private String proxyPath;
    private List<String> staticDirs = new ArrayList<>();
    private String staticServePath = "";
    private List<String> resourcePaths = new ArrayList<>();
    private String resourceServePath = "";
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
    private BuildListener currentListener;

    public DevServerClient() {
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

    public void setJsModuleType(JSModuleType jsModuleType) {
        this.jsModuleType = jsModuleType;
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

    public void setStaticDirs(List<String> staticDirs) {
        this.staticDirs.clear();
        this.staticDirs.addAll(staticDirs);
    }

    public void setStaticServePath(String staticServePath) {
        this.staticServePath = staticServePath;
    }

    public void setResourcePaths(List<String> resourcePaths) {
        this.resourcePaths.clear();
        this.resourcePaths.addAll(resourcePaths);
    }

    public void setResourceServePath(String resourceServePath) {
        this.resourceServePath = resourceServePath;
    }

    public void setProcessMemory(int processMemory) {
        this.processMemory = processMemory;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public Set<File> getServerClasspath() {
        return serverClasspath;
    }

    public Set<File> getClasspath() {
        return classpath;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public String getTargetFilePath() {
        return targetFilePath;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Set<String> getPreservedClasses() {
        return preservedClasses;
    }

    public JSModuleType getJsModuleType() {
        return jsModuleType;
    }

    public String getMainClass() {
        return mainClass;
    }

    public boolean isStackDeobfuscated() {
        return stackDeobfuscated;
    }

    public boolean isIndicator() {
        return indicator;
    }

    public int getPort() {
        return port;
    }

    public Set<File> getSources() {
        return sources;
    }

    public boolean isAutoReload() {
        return autoReload;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getProxyPath() {
        return proxyPath;
    }

    public List<String> getStaticDirs() {
        return staticDirs;
    }

    public String getStaticServePath() {
        return staticServePath;
    }

    public List<String> getResourcePaths() {
        return resourcePaths;
    }

    public String getResourceServePath() {
        return resourceServePath;
    }

    public int getProcessMemory() {
        return processMemory;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public boolean isStarted() {
        return process != null;
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public long getPid() {
        return process != null ? process.pid() : -1;
    }

    public void start() throws IOException {
        var pb = new ProcessBuilder();
        pb.command(buildCommand().toArray(new String[0]));

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
    }

    public void stop() {
        if (process != null) {
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
            }
            process = null;
            Runtime.getRuntime().removeShutdownHook(processKillHook);
            processKillHook = null;
            commandInputThread.interrupt();
            commandInputThread = null;
            stderrThread.interrupt();
            stderrThread = null;
            commandOutput = null;
        }
    }

    public void build(BuildListener listener) {
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
        runEventQueue(listener);
    }

    private void runEventQueue(BuildListener listener) {
        eventQueueDone = false;
        currentListener = listener;
        var stoppedUnexpectedly = new boolean[1];
        var processMonitorThread = new Thread(() -> {
            try {
                process.waitFor();
                schedule(() -> stoppedUnexpectedly[0] = true);
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
                listener.onUnexpectedStop();
            }
        } finally {
            currentListener = null;
            processMonitorThread.interrupt();
        }
    }

    private void readCommandsFromProcess() {
        try (var input = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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
            } catch (InterruptedException e2) {
                // do nothing
            }
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private void readStderrFromProcess() {
        try (var input = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            while (!Thread.currentThread().isInterrupted()) {
                var line = input.readLine();
                if (line == null) {
                    break;
                }
                var listener = currentListener;
                schedule(() -> {
                    if (listener != null) {
                        listener.onStderr(line);
                    }
                });
            }
        } catch (IOException | InterruptedException e) {
            // do nothing
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
                default:
                    break;
            }
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private void logCommand(JsonObjectValue command) {
        var level = command.get("level").asString();
        var message = command.get("message").asString();
        var throwable = command.get("throwable");
        if (throwable != null) {
            message += "\n" + throwable.asString();
        }
        if (currentListener != null) {
            currentListener.onLog(level, message);
        }
    }

    private void progressCommand(JsonObjectValue command) {
        var progress = command.get("progress").asNumber();
        if (currentListener != null) {
            currentListener.onProgress(progress);
        }
    }

    private void completeCommand(JsonObjectValue command) throws InterruptedException {
        var problemsJson = command.get("problems");
        var problems = new ArrayList<Problem>();
        if (problemsJson != null) {
            problems.addAll(parseProblems((JsonArrayValue) problemsJson));
        }
        if (currentListener != null) {
            currentListener.onComplete(problems);
        }
        stopEventQueue();
    }

    private List<Problem> parseProblems(JsonArrayValue json) {
        var problems = new ArrayList<Problem>();
        for (var i = 0; i < json.size(); ++i) {
            var problem = json.get(i).asObject();
            var severityStr = problem.get("severity").asString();
            ProblemSeverity severity;
            switch (severityStr) {
                case "error":
                    severity = ProblemSeverity.ERROR;
                    break;
                case "warning":
                    severity = ProblemSeverity.WARNING;
                    break;
                default:
                    continue;
            }
            var sb = new StringBuilder();
            sb.append(problem.get("message").asString());
            sb.append(problem.get("location").asString());
            problems.add(new Problem(severity, sb.toString()));
        }
        return problems;
    }

    private List<String> buildCommand() {
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

        if (processMemory != 0) {
            command.add("-Xmx" + processMemory + "m");
        }

        if (debugPort != 0) {
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=*:" + debugPort);
        }

        command.add("org.teavm.cli.devserver.TeaVMDevServerRunner");
        command.add("--json-interface");
        command.add("--no-watch");

        if (targetFileName != null) {
            command.add("--targetfile");
            command.add(targetFileName);
        }

        if (targetFilePath != null) {
            command.add("--targetdir");
            command.add(targetFilePath);
        }

        if (!classpath.isEmpty()) {
            command.add("--classpath");
            command.addAll(classpath.stream().map(File::getAbsolutePath).toList());
        }

        if (!sources.isEmpty()) {
            command.add("--sourcepath");
            command.addAll(sources.stream().map(File::getAbsolutePath).toList());
        }

        if (port != 0) {
            command.add("--port");
            command.add(String.valueOf(port));
        }

        if (indicator) {
            command.add("--indicator");
        }

        if (stackDeobfuscated) {
            command.add("--deobfuscate-stack");
        }

        if (autoReload) {
            command.add("--auto-reload");
        }

        if (proxyUrl != null) {
            command.add("--proxy-url");
            command.add(proxyUrl);
        }

        if (proxyPath != null) {
            command.add("--proxy-path");
            command.add(proxyPath);
        }

        if (!staticDirs.isEmpty()) {
            command.add("--static-dirs");
            command.addAll(staticDirs);
        }
        if (!staticServePath.isEmpty()) {
            command.add("--static-serve-path");
            command.add(staticServePath);
        }

        if (!resourcePaths.isEmpty()) {
            command.add("--resources");
            command.addAll(resourcePaths);
        }
        if (!resourceServePath.isEmpty()) {
            command.add("--resource-serve-path");
            command.add(resourceServePath);
        }

        for (var entry : properties.entrySet()) {
            command.add("--property");
            command.add(entry.getKey() + "=" + entry.getValue());
        }

        if (!preservedClasses.isEmpty()) {
            command.add("--preserved-classes");
            command.addAll(preservedClasses);
        }

        if (jsModuleType != null) {
            command.add("--js-module-type");
            command.add(jsModuleType.name().toLowerCase().replace('_', '-'));
        }

        command.add("--");
        command.add(mainClass);

        return command;
    }

    public interface BuildListener {
        default void onLog(String level, String message) {
        }

        default void onProgress(double progress) {
        }

        default void onComplete(List<Problem> problems) {
        }

        default void onStderr(String line) {
        }

        default void onUnexpectedStop() {
        }
    }

    public static final class Problem {
        private final ProblemSeverity severity;
        private final String message;

        Problem(ProblemSeverity severity, String message) {
            this.severity = severity;
            this.message = message;
        }

        public ProblemSeverity getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }
    }
}
