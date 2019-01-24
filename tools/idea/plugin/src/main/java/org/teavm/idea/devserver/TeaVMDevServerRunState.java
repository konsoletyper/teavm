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
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.devserver.ui.TeaVMDevServerConsole;

public class TeaVMDevServerRunState implements RunProfileState {
    private final TeaVMDevServerConfiguration configuration;
    private final TextConsoleBuilder consoleBuilder;
    private final Project project;

    public TeaVMDevServerRunState(@NotNull ExecutionEnvironment environment,
            @NotNull TeaVMDevServerConfiguration configuration) {
        this.configuration = configuration;

        project = environment.getProject();
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
        config.deobfuscateStack = configuration.isDeobfuscateStack();
        config.autoReload = configuration.isAutomaticallyReloaded();
        config.mainClass = configuration.getMainClass();
        config.maxHeap = configuration.getMaxHeap();
        config.proxyUrl = configuration.getProxyUrl();
        config.proxyPath = configuration.getProxyPath();

        if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            config.debugPort = choosePort();
        }

        TeaVMProcessHandler processHandler;
        ExecutionResult executionResult;
        try {
            TeaVMDevServerConsole console = new TeaVMDevServerConsole(consoleBuilder.getConsole());
            processHandler = new TeaVMProcessHandler(config, console);
            console.getUnderlyingConsole().attachToProcess(processHandler);
            processHandler.start();
            executionResult = new DefaultExecutionResult(console, processHandler);
        } catch (IOException e) {
            throw new ExecutionException(e);
        }

        return executionResult;
    }

    private int choosePort() {
        Random random = new Random();
        int minPort = 10000;
        int maxPort = 1 << 16;
        for (int i = 0; i < 20; ++i) {
            int port = minPort + random.nextInt(maxPort - minPort);
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new RuntimeException("Could not find available port");
    }

    private boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
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
}
