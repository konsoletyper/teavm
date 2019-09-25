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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.debug.TeaVMDebugProcess;

public class TeaVMDevServerRunner extends GenericProgramRunner<RunnerSettings> {
    @NotNull
    @Override
    public String getRunnerId() {
        return "TeaVMDevServerRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return profile instanceof TeaVMDevServerConfiguration;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state,
            @NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionResult executionResult = state.execute(environment.getExecutor(), environment.getRunner());
        if (executionResult == null) {
            return null;
        }

        RunContentDescriptor runContent = new RunContentBuilder(executionResult, environment).showRunContent(null);
        int debugPort = ((TeaVMProcessHandler) executionResult.getProcessHandler()).config.debugPort;
        ExecutionConsole console = runContent.getExecutionConsole();
        ProcessHandler processHandler = runContent.getProcessHandler();
        if (debugPort > 0) {
            XDebuggerManager debuggerManager = XDebuggerManager.getInstance(environment.getProject());
            XDebugSession debugSession = debuggerManager.startSession(environment, new XDebugProcessStarter() {
                @NotNull
                @Override
                public XDebugProcess start(@NotNull XDebugSession session) {
                    TeaVMDebugProcess debugProcess = new TeaVMDebugProcess(session, debugPort);
                    debugProcess.console = console;
                    return debugProcess;
                }
            });
            runContent = debugSession.getRunContentDescriptor();

            runContent.getProcessHandler().addProcessListener(new ProcessAdapter() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                    processHandler.startNotify();
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    processHandler.detachProcess();
                }
            });
        }

        return runContent;
    }
}
