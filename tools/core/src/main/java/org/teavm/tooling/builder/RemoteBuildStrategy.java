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
package org.teavm.tooling.builder;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.callgraph.CallGraph;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.daemon.RemoteBuildCallback;
import org.teavm.tooling.daemon.RemoteBuildRequest;
import org.teavm.tooling.daemon.RemoteBuildResponse;
import org.teavm.tooling.daemon.RemoteBuildService;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public class RemoteBuildStrategy implements BuildStrategy {
    private RemoteBuildRequest request;
    private RemoteBuildService buildService;
    private TeaVMProgressListener progressListener;
    private TeaVMToolLog log = new EmptyTeaVMToolLog();

    public RemoteBuildStrategy(RemoteBuildService buildService) {
        this.buildService = buildService;
    }

    @Override
    public void init() {
        request = new RemoteBuildRequest();
        request.optimizationLevel = TeaVMOptimizationLevel.ADVANCED;
        request.wasmVersion = WasmBinaryVersion.V_0x1;
        request.longjmpSupported = true;
    }

    @Override
    public void addSourcesDirectory(String directory) {
        request.sourceDirectories.add(directory);
    }

    @Override
    public void addSourcesJar(String jarFile) {
        request.sourceJarFiles.add(jarFile);
    }

    @Override
    public void setClassPathEntries(List<String> entries) {
        request.classPath.addAll(entries);
    }

    @Override
    public void setTargetType(TeaVMTargetType targetType) {
        request.targetType = targetType;
    }

    @Override
    public void setMainClass(String mainClass) {
        request.mainClass = mainClass;
    }

    @Override
    public void setEntryPointName(String entryPointName) {
        request.entryPointName = entryPointName;
    }

    @Override
    public void setTargetDirectory(String targetDirectory) {
        request.targetDirectory = targetDirectory;
    }

    @Override
    public void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated) {
        request.sourceMapsFileGenerated = sourceMapsFileGenerated;
    }

    @Override
    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        request.debugInformationGenerated = debugInformationGenerated;
    }

    @Override
    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        request.sourceFilesCopied = sourceFilesCopied;
    }

    @Override
    public void setProgressListener(TeaVMProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public void setIncremental(boolean incremental) {
        request.incremental = incremental;
    }

    @Override
    public void setProperties(Properties properties) {
        request.properties = new Properties();
        request.properties.putAll(properties);
    }

    @Override
    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    @Override
    public void setObfuscated(boolean obfuscated) {
        request.obfuscated = obfuscated;
    }

    @Override
    public void setStrict(boolean strict) {
        request.strict = strict;
    }

    @Override
    public void setMaxTopLevelNames(int maxTopLevelNames) {
        request.maxTopLevelNames = maxTopLevelNames;
    }

    @Override
    public void setTransformers(String[] transformers) {
        request.transformers = transformers.clone();
    }

    @Override
    public void setOptimizationLevel(TeaVMOptimizationLevel level) {
        request.optimizationLevel = level;
    }

    @Override
    public void setFastDependencyAnalysis(boolean value) {
        request.fastDependencyAnalysis = value;
    }

    @Override
    public void setTargetFileName(String targetFileName) {
        request.tagetFileName = targetFileName;
    }

    @Override
    public void setClassesToPreserve(String[] classesToPreserve) {
        request.classesToPreserve = classesToPreserve.clone();
    }

    @Override
    public void setCacheDirectory(String cacheDirectory) {
        request.cacheDirectory = cacheDirectory;
    }

    @Override
    public void setWasmVersion(WasmBinaryVersion wasmVersion) {
        request.wasmVersion = wasmVersion;
    }

    @Override
    public void setMinHeapSize(int minHeapSize) {
        request.minHeapSize = minHeapSize;
    }

    @Override
    public void setMaxHeapSize(int maxHeapSize) {
        request.maxHeapSize = maxHeapSize;
    }

    @Override
    public void setLongjmpSupported(boolean value) {
        request.longjmpSupported = value;
    }

    @Override
    public void setHeapDump(boolean heapDump) {
        request.heapDump = heapDump;
    }

    @Override
    public BuildResult build() throws BuildException {
        RemoteBuildResponse response;
        try {
            response = buildService.build(request, new CallbackImpl(progressListener, log));
        } catch (Throwable e) {
            throw new BuildException(e);
        }
        if (response.exception != null) {
            throw new BuildException(response.exception);
        }
        return new BuildResult() {
            private ProblemProvider problems = new ProblemProvider() {
                @Override
                public List<Problem> getProblems() {
                    return response.problems;
                }

                @Override
                public List<Problem> getSevereProblems() {
                    return response.severeProblems;
                }
            };

            @Override
            public CallGraph getCallGraph() {
                return response.callGraph;
            }

            @Override
            public ProblemProvider getProblems() {
                return problems;
            }

            @Override
            public Collection<String> getUsedResources() {
                return response.usedResources;
            }

            @Override
            public Collection<String> getClasses() {
                return response.classes;
            }

            @Override
            public Collection<String> getGeneratedFiles() {
                return response.generatedFiles;
            }
        };
    }

    static class CallbackImpl extends UnicastRemoteObject implements RemoteBuildCallback {
        private TeaVMProgressListener listener;
        private TeaVMToolLog log;

        CallbackImpl(TeaVMProgressListener listener, TeaVMToolLog log) throws RemoteException {
            super();
            this.listener = listener;
            this.log = log;
        }

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
            return listener != null ? listener.phaseStarted(phase, count) : TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(int progress) {
            return listener != null ? listener.progressReached(progress) : TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public void errorReported(String message, Throwable e) {
            log.error(message, e);
        }

        @Override
        public void errorReported(String message) {
            log.error(message);
        }

        @Override
        public void warningReported(String message, Throwable e) {
            log.warning(message, e);
        }

        @Override
        public void warningReported(String message) {
            log.warning(message);
        }

        @Override
        public void infoReported(String message, Throwable e) {
            log.info(message, e);
        }

        @Override
        public void infoReported(String message) {
            log.info(message);
        }
    }
}
