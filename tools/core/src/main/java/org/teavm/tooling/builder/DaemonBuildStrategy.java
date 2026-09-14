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
package org.teavm.tooling.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.backend.wasm.WasmDebugInfoLocation;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.common.json.JsonArrayValue;
import org.teavm.common.json.JsonObjectValue;
import org.teavm.common.json.JsonParser;
import org.teavm.diagnostics.ProblemSeverity;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.daemon.BuildDaemon;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressListener;

/**
 * Runs a build in a separate, one-shot process (see {@link BuildDaemon}), passing build parameters as
 * command line switches and reading progress, log messages and the result back as newline-delimited JSON
 * on the process's stdout.
 */
public class DaemonBuildStrategy implements BuildStrategy {
    private final String javaCommand;
    private final String[] daemonClassPath;
    private final int processMemory;
    private final int debugPort;
    private final List<String> args = new ArrayList<>();
    private TeaVMProgressListener progressListener;
    private TeaVMToolLog log = new EmptyTeaVMToolLog();

    public DaemonBuildStrategy(String javaCommand, int processMemory, String... daemonClassPath) {
        this(javaCommand, processMemory, 0, daemonClassPath);
    }

    public DaemonBuildStrategy(String javaCommand, int processMemory, int debugPort, String... daemonClassPath) {
        this.javaCommand = javaCommand == null || javaCommand.isEmpty()
                ? System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
                : javaCommand;
        this.processMemory = processMemory;
        this.debugPort = debugPort;
        this.daemonClassPath = daemonClassPath.clone();
    }

    @Override
    public void init() {
    }

    @Override
    public void addSourcesDirectory(String directory) {
        args.add("--source-directory");
        args.add(directory);
    }

    @Override
    public void addSourcesJar(String jarFile) {
        args.add("--source-jar");
        args.add(jarFile);
    }

    @Override
    public void setClassPathEntries(List<String> entries) {
        args.add("--classpath");
        args.addAll(entries);
    }

    @Override
    public void setTargetType(TeaVMTargetType targetType) {
        args.add("--target-type");
        args.add(targetType.name());
    }

    @Override
    public void setMainClass(String mainClass) {
        args.add("--main-class");
        args.add(mainClass);
    }

    @Override
    public void setEntryPointName(String entryPointName) {
        args.add("--entry-point-name");
        args.add(entryPointName);
    }

    @Override
    public void setTargetDirectory(String targetDirectory) {
        args.add("--target-dir");
        args.add(targetDirectory);
    }

    @Override
    public void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated) {
        if (sourceMapsFileGenerated) {
            args.add("--source-maps");
        }
    }

    @Override
    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        if (debugInformationGenerated) {
            args.add("--debug-info");
        }
    }

    @Override
    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        setSourceFilePolicy(sourceFilesCopied ? TeaVMSourceFilePolicy.COPY : TeaVMSourceFilePolicy.DO_NOTHING);
    }

    @Override
    public void setSourceFilePolicy(TeaVMSourceFilePolicy sourceFilePolicy) {
        args.add("--source-file-policy");
        args.add(sourceFilePolicy.name());
    }

    @Override
    public void setProgressListener(TeaVMProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public void setIncremental(boolean incremental) {
        if (incremental) {
            log.warning("Incremental build is not supported for out-of-process builds; ignoring");
        }
    }

    @Override
    public void setObfuscated(boolean obfuscated) {
        if (obfuscated) {
            args.add("--obfuscated");
        }
    }

    @Override
    public void setStrict(boolean strict) {
        if (strict) {
            args.add("--strict");
        }
    }

    @Override
    public void setJsModuleType(JSModuleType jsModuleType) {
        args.add("--js-module-type");
        args.add(jsModuleType.name());
    }

    @Override
    public void setMaxTopLevelNames(int maxTopLevelNames) {
        args.add("--max-top-level-names");
        args.add(String.valueOf(maxTopLevelNames));
    }

    @Override
    public void setProperties(Properties properties) {
        for (var name : properties.stringPropertyNames()) {
            args.add("--property");
            args.add(name + "=" + properties.getProperty(name));
        }
    }

    @Override
    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    @Override
    public void setTransformers(String[] transformers) {
        if (transformers.length > 0) {
            args.add("--transformer");
            args.addAll(List.of(transformers));
        }
    }

    @Override
    public void setOptimizationLevel(TeaVMOptimizationLevel level) {
        args.add("--optimization-level");
        args.add(level.name());
    }

    @Override
    public void setFastDependencyAnalysis(boolean value) {
        if (value) {
            args.add("--fast-dependency-analysis");
        }
    }

    @Override
    public void setTargetFileName(String targetFileName) {
        args.add("--target-file");
        args.add(targetFileName);
    }

    @Override
    public void setClassesToPreserve(String[] classesToPreserve) {
        if (classesToPreserve.length > 0) {
            args.add("--classes-to-preserve");
            args.addAll(List.of(classesToPreserve));
        }
    }

    @Override
    public void setCacheDirectory(String cacheDirectory) {
        // out-of-process builds do not support caching between builds
    }

    @Override
    public void setWasmVersion(WasmBinaryVersion wasmVersion) {
        args.add("--wasm-version");
        args.add(wasmVersion.name());
    }

    @Override
    public void setWasmDebugInfoLevel(WasmDebugInfoLevel wasmDebugInfoLevel) {
        args.add("--wasm-debug-info-level");
        args.add(wasmDebugInfoLevel.name());
    }

    @Override
    public void setWasmDebugInfoLocation(WasmDebugInfoLocation wasmDebugInfoLocation) {
        args.add("--wasm-debug-info-location");
        args.add(wasmDebugInfoLocation.name());
    }

    @Override
    public void setMinHeapSize(int minHeapSize) {
        args.add("--min-heap-size");
        args.add(String.valueOf(minHeapSize));
    }

    @Override
    public void setMaxHeapSize(int maxHeapSize) {
        args.add("--max-heap-size");
        args.add(String.valueOf(maxHeapSize));
    }

    @Override
    public void setMinDirectBuffersSize(int minDirectBuffersSize) {
        args.add("--min-direct-buffers-size");
        args.add(String.valueOf(minDirectBuffersSize));
    }

    @Override
    public void setSharedBuffer(boolean sharedBuffer) {
        if (sharedBuffer) {
            args.add("--shared-buffer");
        }
    }

    @Override
    public void setHeapDump(boolean heapDump) {
        if (heapDump) {
            args.add("--heap-dump");
        }
    }

    @Override
    public void setShortFileNames(boolean shortFileNames) {
        if (shortFileNames) {
            args.add("--short-file-names");
        }
    }

    @Override
    public void setAssertionsRemoved(boolean assertionsRemoved) {
        if (assertionsRemoved) {
            args.add("--assertions-removed");
        }
    }

    @Override
    public BuildResult build() throws BuildException {
        var command = new ArrayList<String>();
        command.add(javaCommand);
        if (daemonClassPath.length > 0) {
            command.add("-cp");
            command.add(String.join(File.pathSeparator, daemonClassPath));
        }
        command.add("-Xmx" + processMemory + "m");
        if (debugPort != 0) {
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=*:" + debugPort);
        }
        command.add(BuildDaemon.class.getName());
        command.addAll(args);

        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new BuildException(e);
        }

        var result = new ResultHolder();
        var stderr = new StringBuilder();

        var stderrThread = new Thread(() -> readStderr(process, stderr));
        stderrThread.setDaemon(true);
        stderrThread.start();

        Exception readError = null;
        var jsonParser = JsonParser.ofValue(value -> handleMessage(value.asObject(), result));
        try (var reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonParser.parse(new StringReader(line));
            }
        } catch (IOException | RuntimeException e) {
            // an incomplete or malformed line usually means the process was killed
            // or crashed mid-write (e.g. OutOfMemoryError); the exit code, checked below,
            // gives the actual reason
            readError = e;
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
            stderrThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildException(e);
        }

        if (result.error != null) {
            throw new BuildException(result.error);
        }
        if (readError != null || exitCode != 0 || result.problems == null) {
            var message = new StringBuilder("Build daemon process failed");
            if (exitCode != 0) {
                message.append(" with exit code ").append(exitCode);
            }
            if (readError != null) {
                message.append("; error while reading its output: ").append(readError.getMessage());
            }
            if (!stderr.isEmpty()) {
                message.append(":\n").append(stderr);
            }
            throw new BuildException(new IOException(message.toString(), readError));
        }

        var problems = result.problems;
        return () -> problems;
    }

    private void readStderr(Process process, StringBuilder stderr) {
        try (var reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.error(line);
                stderr.append(line).append('\n');
            }
        } catch (IOException e) {
            // do nothing
        }
    }

    private void handleMessage(JsonObjectValue message, ResultHolder result) {
        var type = message.get("type").asString();
        switch (type) {
            case "log":
                handleLog(message);
                break;
            case "phase-started":
                if (progressListener != null) {
                    var phase = TeaVMPhase.valueOf(message.get("phase").asString());
                    var count = (int) message.get("count").asIntNumber();
                    progressListener.phaseStarted(phase, count);
                }
                break;
            case "progress":
                if (progressListener != null) {
                    progressListener.progressReached((int) message.get("progress").asIntNumber());
                }
                break;
            case "complete":
                handleComplete(message, result);
                break;
            case "error":
                handleError(message, result);
                break;
            default:
                break;
        }
    }

    private void handleLog(JsonObjectValue message) {
        var level = message.get("level").asString();
        var text = message.get("message").asString();
        var throwableValue = message.get("throwable");
        if (throwableValue != null) {
            text += "\n" + throwableValue.asString();
        }
        switch (level) {
            case "error":
                log.error(text);
                break;
            case "warning":
                log.warning(text);
                break;
            case "debug":
                log.debug(text);
                break;
            default:
                log.info(text);
                break;
        }
    }

    private void handleComplete(JsonObjectValue message, ResultHolder result) {
        var problemsJson = (JsonArrayValue) message.get("problems");
        var problems = new ArrayList<RenderedProblem>();
        for (var i = 0; i < problemsJson.size(); ++i) {
            var problemJson = problemsJson.get(i).asObject();
            var severity = "error".equals(problemJson.get("severity").asString())
                    ? ProblemSeverity.ERROR
                    : ProblemSeverity.WARNING;
            problems.add(new RenderedProblem(severity, problemJson.get("message").asString(),
                    problemJson.get("stackTrace").asString()));
        }
        result.problems = problems;
    }

    private void handleError(JsonObjectValue message, ResultHolder result) {
        result.error = new RuntimeException("Build daemon reported an error: "
                + message.get("message").asString());
    }

    private static final class ResultHolder {
        List<RenderedProblem> problems;
        Throwable error;
    }
}
