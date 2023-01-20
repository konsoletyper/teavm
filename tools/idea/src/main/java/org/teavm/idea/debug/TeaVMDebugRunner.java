/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.idea.debug;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeaVMDebugRunner extends GenericProgramRunner<RunnerSettings> {
    @NotNull
    @Override
    public String getRunnerId() {
        return "TeaVMDebugRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof TeaVMDebugConfiguration;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state,
            @NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionResult executionResult = state.execute(environment.getExecutor(), environment.getRunner());
        if (executionResult == null) {
            return null;
        }

        return null;
    }
}
