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
package org.teavm.idea.devserver;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.Processor;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.devserver.client.DevServerClient;
import org.teavm.idea.BuildConfig;
import org.teavm.idea.devserver.ui.TeaVMDevServerConsole;

public class TeaVMDevServerRunState implements RunProfileState {
    private static final String MARKER_ARTIFACT = "org.teavm:teavm-classlib:";
    private final TeaVMDevServerConfiguration configuration;
    private final TextConsoleBuilder consoleBuilder;
    private final Project project;

    public TeaVMDevServerRunState(@NotNull ExecutionEnvironment environment,
            @NotNull TeaVMDevServerConfiguration configuration) {
        this.configuration = configuration;

        project = environment.getProject();
        var searchScope = GlobalSearchScopes.projectProductionScope(project);
        consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope);
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
        Module module = configuration.getConfigurationModule().getModule();

        var teavmVersion = detectTeaVMVersion(module);
        var app = ApplicationManager.getApplication();
        var client = new DevServerClient(app::invokeLater);
        var moduleSdk = JavaParametersUtil.createModuleJdk(module, true, configuration.getJdkPath());
        client.setJavaHome(moduleSdk.getHomePath());
        var enumerator = OrderEnumerator.orderEntries(module).withoutSdk().recursively();

        client.setClasspath(Arrays.stream(enumerator.getClassesRoots())
                .map(this::path)
                .filter(Objects::nonNull)
                .map(File::new)
                .collect(Collectors.toSet()));
        client.setSources(Arrays.stream(enumerator.getSourceRoots())
                .map(this::path)
                .filter(Objects::nonNull)
                .map(File::new)
                .collect(Collectors.toSet()));
        client.setTargetFilePath(configuration.getPathToFile());
        client.setTargetFileName(configuration.getFileName());
        client.setPort(configuration.getPort());
        client.setIndicator(configuration.isIndicator());
        client.setStackDeobfuscated(configuration.isDeobfuscateStack());
        client.setAutoReload(configuration.isAutomaticallyReloaded());
        client.setMainClass(configuration.getMainClass());
        client.setProcessMemory(configuration.getMaxHeap());
        client.setProxyUrl(configuration.getProxyUrl());
        client.setProxyPath(configuration.getProxyPath());

        if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            client.setJsDebugPort(choosePort());
        }

        TeaVMProcessHandler processHandler;
        ExecutionResult executionResult;
        var console = new TeaVMDevServerConsole(consoleBuilder.getConsole());
        var downloader = new TeaVMDownloader(teavmVersion, console, app);
        processHandler = new TeaVMProcessHandler(client.getJsDebugPort(), console, downloader, client);
        console.setClient(client);
        console.getUnderlyingConsole().attachToProcess(processHandler);
        executionResult = new DefaultExecutionResult(console, processHandler);
        downloader.downloadAndStart(success -> {
            if (success) {
                var classpath = Arrays.stream(downloader.localPath().listFiles())
                        .filter(File::isFile)
                        .filter(file -> file.getName().endsWith(".jar"))
                        .collect(Collectors.toSet());
                client.setServerClasspath(classpath);
                try {
                    client.start();
                } catch (IOException e) {
                    console.getUnderlyingConsole().print("Failed to start TeaVM dev server: " + e.getMessage(),
                            ConsoleViewContentType.ERROR_OUTPUT);
                }
                client.build();
            } else {
                processHandler.notifyProcessTerminated(1);
            }
        });

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

    private String detectTeaVMVersion(Module module) {
        var enumerator = OrderEnumerator.orderEntries(module).withoutSdk().recursively();
        var libProcessor = new Processor<Library>() {
            String version;

            @Override
            public boolean process(Library lib) {
                if (lib.getName() == null) {
                    return true;
                }
                var index = lib.getName().indexOf(MARKER_ARTIFACT);
                if (index < 0) {
                    return true;
                }
                version = lib.getName().substring(index + MARKER_ARTIFACT.length()).trim();
                return false;
            }
        };
        enumerator.forEachLibrary(libProcessor);
        return libProcessor.version != null ? libProcessor.version : BuildConfig.VERSION;
    }
}
