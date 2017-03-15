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
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
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

public class RemoteBuildStrategy implements TeaVMBuildStrategy {
    private final CompileContext context;
    private TeaVMRemoteBuildRequest request;
    private TeaVMRemoteBuildService buildService;

    public RemoteBuildStrategy(CompileContext context, TeaVMRemoteBuildService buildService) {
        this.context = context;
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
    public TeaVMBuildResult build() {
        TeaVMRemoteBuildResponse response;
        try {
            response = buildService.build(request, new CallbackImpl(context));
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
        };
    }

    static class CallbackImpl extends UnicastRemoteObject implements TeaVMRemoteBuildCallback {
        private CompileContext context;
        int expectedCount;
        TeaVMPhase currentPhase;

        public CallbackImpl(CompileContext context) throws RemoteException {
            super();
            this.context = context;
        }

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) throws RemoteException {
            expectedCount = count;
            context.processMessage(new ProgressMessage(phaseName(phase), 0));
            currentPhase = phase;
            return context.getCancelStatus().isCanceled() ? TeaVMProgressFeedback.CANCEL
                    : TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(int progress) throws RemoteException {
            context.processMessage(new ProgressMessage(phaseName(currentPhase), (float) progress / expectedCount));
            return context.getCancelStatus().isCanceled() ? TeaVMProgressFeedback.CANCEL
                    : TeaVMProgressFeedback.CONTINUE;
        }
    }

    private static String phaseName(TeaVMPhase phase) {
        switch (phase) {
            case DEPENDENCY_CHECKING:
                return "Discovering classes to compile";
            case LINKING:
                return "Resolving method invocations";
            case DECOMPILATION:
                return "Compiling classes";
            case OPTIMIZATION:
                return "Optimizing code";
            case RENDERING:
                return "Building JS file";
            default:
                throw new AssertionError();
        }
    }
}
