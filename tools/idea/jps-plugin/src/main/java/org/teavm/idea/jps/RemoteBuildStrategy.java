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
package org.teavm.idea.jps;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.teavm.callgraph.CallGraph;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.idea.jps.model.TeaVMBuildResult;
import org.teavm.idea.jps.model.TeaVMBuildStrategy;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildCallback;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildRequest;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildResponse;
import org.teavm.idea.jps.remote.TeaVMRemoteBuildService;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public class RemoteBuildStrategy implements TeaVMBuildStrategy {
    private TeaVMRemoteBuildRequest request;
    private TeaVMRemoteBuildService buildService;
    private TeaVMProgressListener progressListener;

    public RemoteBuildStrategy(TeaVMRemoteBuildService buildService) {
        this.buildService = buildService;
    }

    @Override
    public void init() {
        request = new TeaVMRemoteBuildRequest();
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
    public TeaVMBuildResult build() {
        TeaVMRemoteBuildResponse response;
        try {
            response = buildService.build(request, new CallbackImpl(progressListener));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return new TeaVMBuildResult() {
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
            public boolean isErrorOccurred() {
                return response.errorOccurred;
            }

            @Override
            public String getStackTrace() {
                return response.stackTrace;
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

    static class CallbackImpl extends UnicastRemoteObject implements TeaVMRemoteBuildCallback {
        private TeaVMProgressListener listener;

        public CallbackImpl(TeaVMProgressListener listener) throws RemoteException {
            super();
            this.listener = listener;
        }

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) throws RemoteException {
            return listener.phaseStarted(phase, count);
        }

        @Override
        public TeaVMProgressFeedback progressReached(int progress) throws RemoteException {
            return listener.progressReached(progress);
        }
    }
}
