/*
 *  Copyright 2023 Alexey Andreev.
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
import java.net.URLClassLoader;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Properties;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.teavm.gradle.api.OptimizationLevel;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.builder.BuildException;
import org.teavm.tooling.builder.BuildStrategy;
import org.teavm.tooling.builder.InProcessBuildStrategy;
import org.teavm.tooling.builder.RemoteBuildStrategy;
import org.teavm.tooling.daemon.BuildDaemon;
import org.teavm.tooling.daemon.DaemonLog;
import org.teavm.tooling.daemon.RemoteBuildService;
import org.teavm.vm.TeaVMOptimizationLevel;

public abstract class TeaVMTask extends DefaultTask {
    public TeaVMTask() {
        setGroup("TeaVM");
        getDebugInformation().convention(false);
        getTargetFileName().convention("bundle");
        getOptimization().convention(OptimizationLevel.BALANCED);
        getFastGlobalAnalysis().convention(false);
        getOutOfProcess().convention(false);
        getProcessMemory().convention(512);
    }

    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Input
    @Optional
    public abstract Property<Boolean> getDebugInformation();

    @Input
    @Optional
    public abstract Property<String> getTargetFileName();

    @Input
    @Optional
    public abstract Property<OptimizationLevel> getOptimization();

    @Input
    @Optional
    public abstract Property<Boolean> getFastGlobalAnalysis();

    @Input
    @Optional
    public abstract MapProperty<String, String> getProperties();

    @Input
    @Optional
    public abstract ListProperty<String> getPreservedClasses();

    @OutputDirectory
    public abstract Property<File> getOutputDir();

    @Input
    public abstract Property<String> getMainClass();

    @Input
    @Optional
    public abstract Property<Boolean> getOutOfProcess();

    @Input
    @Optional
    public abstract Property<Integer> getProcessMemory();

    @Classpath
    public abstract ConfigurableFileCollection getDaemonClasspath();

    @TaskAction
    public void execute() throws BuildException, IOException, NotBoundException {
        if (getOutOfProcess().get()) {
            executeInSeparateProcess();
        } else {
            executeWithBuilder(new InProcessBuildStrategy(URLClassLoader::new));
        }
    }

    private void executeInSeparateProcess() throws BuildException, IOException, NotBoundException {
        var daemon = BuildDaemon.start(false, getProcessMemory().get(), new DaemonLogImpl(),
                createDaemonClassPath());

        try {
            var registry = LocateRegistry.getRegistry("localhost", daemon.getPort());
            var buildService = (RemoteBuildService) registry.lookup(RemoteBuildService.ID);
            var builder = new RemoteBuildStrategy(buildService);
            executeWithBuilder(builder);
        } finally {
            daemon.getProcess().destroy();
        }
    }

    private void executeWithBuilder(BuildStrategy builder) throws BuildException {
        builder.init();
        var toolLog = new GradleTeaVMToolLog(getLogger());
        builder.setLog(toolLog);
        builder.setMainClass(getMainClass().get());
        builder.setDebugInformationGenerated(getDebugInformation().get());
        var classPathStrings = new ArrayList<String>();
        for (var file : getClasspath()) {
            classPathStrings.add(file.getAbsolutePath());
        }
        builder.setClassPathEntries(classPathStrings);
        builder.setTargetFileName(getTargetFileName().get());
        builder.setOptimizationLevel(map(getOptimization().get()));
        builder.setFastDependencyAnalysis(getFastGlobalAnalysis().get());
        builder.setTargetDirectory(getOutputDir().get().getAbsolutePath());
        builder.setClassesToPreserve(getPreservedClasses().get().toArray(new String[0]));
        if (getProperties().isPresent()) {
            var properties = new Properties();
            for (var entry : getProperties().get().entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
            builder.setProperties(properties);
        }
        setupBuilder(builder);
        var result = builder.build();
        TeaVMProblemRenderer.describeProblems(result.getCallGraph(), result.getProblems(), toolLog);
        if (!result.getProblems().getSevereProblems().isEmpty()) {
            throw new GradleException("Errors occurred during TeaVM build");
        }
    }

    private static TeaVMOptimizationLevel map(OptimizationLevel level) {
        switch (level) {
            case AGGRESSIVE:
                return TeaVMOptimizationLevel.FULL;
            case BALANCED:
                return TeaVMOptimizationLevel.ADVANCED;
            default:
                return TeaVMOptimizationLevel.SIMPLE;
        }
    }

    protected abstract void setupBuilder(BuildStrategy builder);

    private String[] createDaemonClassPath() {
        var result = new ArrayList<String>();
        for (var file : getDaemonClasspath()) {
            result.add(file.getAbsolutePath());
        }
        return result.toArray(new String[0]);
    }

    class DaemonLogImpl implements DaemonLog {
        @Override
        public void error(String message) {
            getLogger().error(message);
        }

        @Override
        public void error(String message, Throwable e) {
            getLogger().error(message, e);
        }

        @Override
        public void info(String message) {
            getLogger().info(message);
        }
    }
}
