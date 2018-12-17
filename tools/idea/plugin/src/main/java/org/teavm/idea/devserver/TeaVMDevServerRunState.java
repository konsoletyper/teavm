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

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.SearchScopeProvider;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.DaemonUtil;
import org.teavm.idea.DevServerRunnerListener;
import org.teavm.idea.devserver.ui.TeaVMDevServerConsole;

public class TeaVMDevServerRunState implements RunProfileState {
    private final TeaVMDevServerConfiguration configuration;
    private final TextConsoleBuilder consoleBuilder;

    public TeaVMDevServerRunState(@NotNull ExecutionEnvironment environment,
            @NotNull TeaVMDevServerConfiguration configuration) {
        this.configuration = configuration;

        Project project = environment.getProject();
        GlobalSearchScope searchScope = SearchScopeProvider.createSearchScope(project, environment.getRunProfile());
        consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope);
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
        DevServerConfiguration config = new DevServerConfiguration();
        Module module = configuration.getConfigurationModule().getModule();

        Sdk moduleSdk = JavaParametersUtil.createModuleJdk(module, true, configuration.getJdkPath());
        config.javaHome = moduleSdk.getHomePath();
        OrderEnumerator enumerator = OrderEnumerator.orderEntries(module).withoutSdk().recursively();
        config.classPath = Arrays.stream(enumerator.getClassesRoots())
                .map(this::path)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        config.sourcePath = Arrays.stream(enumerator.getSourceRoots())
                .map(this::path)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        config.pathToFile = configuration.getPathToFile();
        config.fileName = configuration.getFileName();
        config.port = configuration.getPort();
        config.indicator = configuration.isIndicator();
        config.autoReload = configuration.isAutomaticallyReloaded();
        config.mainClass = configuration.getMainClass();
        config.maxHeap = configuration.getMaxHeap();

        try {
            TeaVMDevServerConsole console = new TeaVMDevServerConsole(consoleBuilder.getConsole());
            ProcessHandlerImpl processHandler = new ProcessHandlerImpl(config, console);
            console.getUnderlyingConsole().attachToProcess(processHandler);
            processHandler.start();
            return new DefaultExecutionResult(console, processHandler);
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }

    private String path(VirtualFile file) {
        while (file.getFileSystem() instanceof JarFileSystem) {
            file = ((JarFileSystem) file.getFileSystem()).getLocalByEntry(file);
            if (file == null) {
                return null;
            }
        }
        return file.getCanonicalPath();
    }

    class ProcessHandlerImpl extends ProcessHandler implements DevServerRunnerListener {
        private DevServerConfiguration config;
        private TeaVMDevServerConsole console;
        private DevServerInfo info;

        ProcessHandlerImpl(DevServerConfiguration config, TeaVMDevServerConsole console) {
            this.config = config;
            this.console = console;
        }

        void start() throws IOException {
            info = DevServerRunner.start(DaemonUtil.detectClassPath().toArray(new String[0]), config, this);
            console.setServerManager(info.server);
            startNotify();
        }

        @Override
        protected void destroyProcessImpl() {
            info.process.destroy();
        }

        @Override
        protected void detachProcessImpl() {
            try {
                info.server.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean detachIsDefault() {
            return true;
        }

        @Nullable
        @Override
        public OutputStream getProcessInput() {
            return null;
        }


        @Override
        public void error(String text) {
            console.getUnderlyingConsole().print(text + System.lineSeparator(), ConsoleViewContentType.ERROR_OUTPUT);
        }

        @Override
        public void info(String text) {
            console.getUnderlyingConsole().print(text + System.lineSeparator(), ConsoleViewContentType.NORMAL_OUTPUT);
        }

        @Override
        public void stopped(int code) {
            console.stop();
            notifyProcessTerminated(code);
        }
    }
}
