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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.internal.logging.progress.ProgressLogger;
import org.teavm.devserver.client.DefaultDevServerEventQueue;
import org.teavm.devserver.client.DevServerClient;
import org.teavm.devserver.client.DevServerListener;
import org.teavm.devserver.client.DevServerTarget;
import org.teavm.gradle.api.JSModuleType;

public class ProjectDevServerManager {
    private Set<File> runningServerClasspath = new HashSet<>();
    private Set<File> runningClasspath = new HashSet<>();
    private DevServerTarget runningTarget;
    private String runningTargetFileName;
    private String runningTargetFilePath;
    private Map<String, String> runningProperties = new HashMap<>();
    private Set<String> runningPreservedClasses = new HashSet<>();
    private org.teavm.backend.javascript.JSModuleType runningJsModuleType;
    private boolean runningWasmSharedBuffer;
    private boolean runningWasmModularRuntime;
    private String runningMainClass;
    private boolean runningStackDeobfuscated;
    private boolean runningIndicator;
    private int runningPort;
    private Set<File> runningSources = new HashSet<>();
    private boolean runningAutoReload;
    private String runningProxyUrl;
    private String runningProxyPath;
    private List<String> runningStaticDirs = new ArrayList<>();
    private String runningStaticServePath;
    private List<String> runningResourcePaths = new ArrayList<>();
    private String runningResourceServePath;
    private int runningProcessMemory;
    private int runningDebugPort;

    private final DefaultDevServerEventQueue edt = new DefaultDevServerEventQueue();
    private final DevServerClient client = new DevServerClient(edt);

    public ProjectDevServerManager() {
        client.addListener(new DevServerListener() {
            @Override
            public void onCancelled() {
                edt.stopEventQueue();
            }

            @Override
            public void onComplete(List<DevServerClient.Problem> problems) {
                edt.stopEventQueue();
            }

            @Override
            public void onUnexpectedStop() {
                edt.stopEventQueue();
            }
        });
        client.setNoWatch(true);
    }

    public void setServerClasspath(Set<File> serverClasspath) {
        client.setServerClasspath(serverClasspath);
    }

    public void setClasspath(Set<File> classpath) {
        client.setClasspath(classpath);
    }

    public void setTarget(DevServerTarget target) {
        client.setTarget(target);
    }

    public void setProperties(Map<String, String> properties) {
        client.setProperties(properties);
    }

    public void setPreservedClasses(Collection<String> preservedClasses) {
        client.setPreservedClasses(preservedClasses);
    }

    public void setJsModuleType(JSModuleType jsModuleType) {
        client.setJsModuleType(jsModuleType != null ? TaskUtils.mapJsModuleType(jsModuleType) : null);
    }

    public void setWasmSharedBuffer(boolean wasmSharedBuffer) {
        client.setWasmSharedBuffer(wasmSharedBuffer);
    }

    public void setWasmModularRuntime(boolean wasmModularRuntime) {
        client.setWasmModularRuntime(wasmModularRuntime);
    }

    public void setTargetFileName(String targetFileName) {
        client.setTargetFileName(targetFileName);
    }

    public void setTargetFilePath(String targetFilePath) {
        client.setTargetFilePath(targetFilePath);
    }

    public void setMainClass(String mainClass) {
        client.setMainClass(mainClass);
    }

    public void setStackDeobfuscated(boolean stackDeobfuscated) {
        client.setStackDeobfuscated(stackDeobfuscated);
    }

    public void setIndicator(boolean indicator) {
        client.setIndicator(indicator);
    }

    public void setPort(int port) {
        client.setPort(port);
    }

    public void setSources(Set<File> sources) {
        client.setSources(sources);
    }

    public void setAutoReload(boolean autoReload) {
        client.setAutoReload(autoReload);
    }

    public void setProxyUrl(String proxyUrl) {
        client.setProxyUrl(proxyUrl);
    }

    public void setProxyPath(String proxyPath) {
        client.setProxyPath(proxyPath);
    }

    public void setStaticDirs(List<String> staticDirs) {
        client.setStaticDirs(staticDirs);
    }

    public void setStaticServePath(String staticServePath) {
        client.setStaticServePath(staticServePath);
    }

    public void setResourcePaths(List<String> resourcePaths) {
        client.setResourcePaths(resourcePaths);
    }

    public void setResourceServePath(String resourceServePath) {
        client.setResourceServePath(resourceServePath);
    }

    public void setProcessMemory(int processMemory) {
        client.setProcessMemory(processMemory);
    }

    public void setDebugPort(int debugPort) {
        client.setDebugPort(debugPort);
    }

    public void runBuild(Logger logger, ProgressLogger progressLogger) throws IOException {
        restartIfNecessary(logger);
        var listener = new GradleBuildListener(logger, progressLogger);
        client.addListener(listener);
        client.build();
        edt.runEventQueue();
        client.removeListener(listener);
    }

    public void stop(Logger logger) {
        if (client.isStarted()) {
            logger.info("Stopping TeaVM development server, PID = {}", client.getPid());
            if (!client.isRunning()) {
                logger.info("Process was dead");
            }
            client.stop();
        } else {
            logger.info("No development server running, doing nothing");
        }
    }

    private void restartIfNecessary(Logger logger) throws IOException {
        if (client.isStarted() && !checkProcess()) {
            logger.info("Changes detected in TeaVM development server config, restarting server");
            stop(logger);
        }
        if (!client.isStarted() || !client.isRunning()) {
            start(logger);
        }
    }

    private void start(Logger logger) throws IOException {
        logger.info("Starting TeaVM development server");
        client.start();
        snapshotRunningConfig();
        logger.info("Development server started");
    }

    private void snapshotRunningConfig() {
        runningServerClasspath.clear();
        runningServerClasspath.addAll(client.getServerClasspath());
        runningTarget = client.getTarget();
        runningClasspath.clear();
        runningClasspath.addAll(client.getClasspath());
        runningTargetFileName = client.getTargetFileName();
        runningTargetFilePath = client.getTargetFilePath();
        runningProperties.clear();
        runningProperties.putAll(client.getProperties());
        runningPreservedClasses.clear();
        runningPreservedClasses.addAll(client.getPreservedClasses());
        runningJsModuleType = client.getJsModuleType();
        runningWasmSharedBuffer = client.isWasmSharedBuffer();
        runningWasmModularRuntime = client.isWasmModularRuntime();
        runningMainClass = client.getMainClass();
        runningStackDeobfuscated = client.isStackDeobfuscated();
        runningIndicator = client.isIndicator();
        runningPort = client.getPort();
        runningSources.clear();
        runningSources.addAll(client.getSources());
        runningAutoReload = client.isAutoReload();
        runningProxyUrl = client.getProxyUrl();
        runningProxyPath = client.getProxyPath();
        runningStaticDirs.clear();
        runningStaticDirs.addAll(client.getStaticDirs());
        runningStaticServePath = client.getStaticServePath();
        runningResourcePaths.clear();
        runningResourcePaths.addAll(client.getResourcePaths());
        runningResourceServePath = client.getResourceServePath();
        runningProcessMemory = client.getProcessMemory();
        runningDebugPort = client.getDebugPort();
    }

    private boolean checkProcess() {
        return Objects.equals(client.getServerClasspath(), runningServerClasspath)
                && Objects.equals(client.getClasspath(), runningClasspath)
                && client.getTarget() == runningTarget
                && Objects.equals(client.getTargetFileName(), runningTargetFileName)
                && Objects.equals(client.getTargetFilePath(), runningTargetFilePath)
                && Objects.equals(client.getProperties(), runningProperties)
                && Objects.equals(client.getPreservedClasses(), runningPreservedClasses)
                && client.getJsModuleType() == runningJsModuleType
                && client.isWasmSharedBuffer() == runningWasmSharedBuffer
                && client.isWasmModularRuntime() == runningWasmModularRuntime
                && Objects.equals(client.getMainClass(), runningMainClass)
                && client.isStackDeobfuscated() == runningStackDeobfuscated
                && client.isIndicator() == runningIndicator
                && client.getPort() == runningPort
                && Objects.equals(client.getSources(), runningSources)
                && client.isAutoReload() == runningAutoReload
                && Objects.equals(client.getProxyUrl(), runningProxyUrl)
                && Objects.equals(client.getProxyPath(), runningProxyPath)
                && Objects.equals(client.getStaticDirs(), runningStaticDirs)
                && Objects.equals(client.getStaticServePath(), runningStaticServePath)
                && Objects.equals(client.getResourcePaths(), runningResourcePaths)
                && Objects.equals(client.getResourceServePath(), runningResourceServePath)
                && client.getProcessMemory() == runningProcessMemory
                && client.getDebugPort() == runningDebugPort;
    }

    private static final class GradleBuildListener implements DevServerListener {
        private final Logger logger;
        private final ProgressLogger progressLogger;
        private boolean hasSevere;

        GradleBuildListener(Logger logger, ProgressLogger progressLogger) {
            this.logger = logger;
            this.progressLogger = progressLogger;
        }

        @Override
        public void onLog(DevServerClient.LogLevel level, String message) {
            switch (level) {
                case DEBUG:
                    logger.debug(message);
                    break;
                case INFO:
                    logger.info(message);
                    break;
                case WARNING:
                    logger.warn(message);
                    break;
                case ERROR:
                    logger.error(message);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onProgress(double progress) {
            if (progressLogger == null) {
                return;
            }
            var roundedResult = (int) (progress * 1000 + 5) / 10;
            var result = Math.min(100, roundedResult / 10.0);
            progressLogger.progress(result + " %");
        }

        @Override
        public void onComplete(List<DevServerClient.Problem> problems) {
            for (var problem : problems) {
                switch (problem.getSeverity()) {
                    case ERROR:
                        hasSevere = true;
                        logger.error(problem.getMessage());
                        break;
                    case WARNING:
                        logger.warn(problem.getMessage());
                        break;
                }
            }
            if (hasSevere) {
                throw new GradleException("Errors occurred during TeaVM build");
            }
        }

        @Override
        public void onStderr(String line) {
            logger.warn("server stderr: {}", line);
        }

        @Override
        public void onUnexpectedStop() {
            logger.error("Dev server process stopped unexpectedly");
            throw new GradleException();
        }
    }
}
