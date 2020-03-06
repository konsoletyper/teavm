/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.maven;

import java.io.File;
import java.net.URLClassLoader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.builder.BuildException;
import org.teavm.tooling.builder.BuildResult;
import org.teavm.tooling.builder.BuildStrategy;
import org.teavm.tooling.builder.InProcessBuildStrategy;
import org.teavm.tooling.builder.RemoteBuildStrategy;
import org.teavm.tooling.daemon.BuildDaemon;
import org.teavm.tooling.daemon.DaemonInfo;
import org.teavm.tooling.daemon.DaemonLog;
import org.teavm.tooling.daemon.RemoteBuildService;
import org.teavm.vm.TeaVMOptimizationLevel;

@Mojo(name = "compile", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class TeaVMCompileMojo extends AbstractMojo {
    @Component
    private MavenProject project;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(required = true, readonly = true, defaultValue = "${localRepository}")
    private MavenArtifactRepository localRepository;

    @Parameter(required = true, readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    private List<MavenArtifactRepository> remoteRepositories;

    @Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
    private List<Artifact> pluginArtifacts;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classFiles;

    @Parameter
    private List<String> compileScopes;

    @Parameter(property = "teavm.minifying", defaultValue = "true")
    private boolean minifying = true;

    @Parameter(property = "teavm.strict", defaultValue = "false")
    private boolean strict;

    @Parameter(property = "teavm.maxTopLevelNames", defaultValue = "10000")
    private int maxTopLevelNames = 10000;

    @Parameter
    private Properties properties;

    @Parameter(property = "teavm.debugInformationGenerated", defaultValue = "false")
    private boolean debugInformationGenerated;

    @Parameter(property = "teavm.sourceMapsGenerated", defaultValue = "false")
    private boolean sourceMapsGenerated;

    @Parameter(property = "teavm.sourceFilesCopied", defaultValue = "false")
    private boolean sourceFilesCopied;

    @Parameter(property = "teavm.incremental", defaultValue = "false")
    private boolean incremental;

    @Parameter
    private String[] transformers;

    @Parameter(defaultValue = "${project.build.directory}/javascript")
    private File targetDirectory;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDirectory;

    @Parameter(property = "teavm.targetFileName", defaultValue = "")
    private String targetFileName = "";

    @Parameter(property = "teavm.mainClass")
    private String mainClass;

    @Parameter(property = "teavm.entryPointName", defaultValue = "main")
    private String entryPointName;

    @Parameter
    private String[] classesToPreserve;

    @Parameter(property = "teavm.stopOnErrors", defaultValue = "true")
    private boolean stopOnErrors = true;

    @Parameter(property = "teavm.optimizationLevel", defaultValue = "SIMPLE")
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.SIMPLE;

    @Parameter(property = "teavm.fastGlobalAnalysis", defaultValue = "false")
    private boolean fastGlobalAnalysis;

    @Parameter(property = "teavm.targetType", defaultValue = "JAVASCRIPT")
    private TeaVMTargetType targetType = TeaVMTargetType.JAVASCRIPT;

    @Parameter(defaultValue = "${project.build.directory}/teavm-cache")
    private File cacheDirectory;

    @Parameter(property = "teavm.wasmVersion", defaultValue = "V_0x1")
    private WasmBinaryVersion wasmVersion = WasmBinaryVersion.V_0x1;

    @Parameter(property = "teavm.minHeapSize", defaultValue = "4")
    private int minHeapSize;

    @Parameter(property = "teavm.maxHeapSize", defaultValue = "128")
    private int maxHeapSize;

    @Parameter(property = "teavm.outOfProcess", defaultValue = "false")
    private boolean outOfProcess;

    @Parameter(property = "teavm.processMemory", defaultValue = "512")
    private int processMemory;

    @Parameter(property = "teavm.longjmpSupported", defaultValue = "true")
    private boolean longjmpSupported;

    @Parameter(property = "teavm.heapDump", defaultValue = "false")
    private boolean heapDump;

    private void setupBuilder(BuildStrategy builder) throws MojoExecutionException {
        builder.setLog(new MavenTeaVMToolLog(getLog()));
        try {
            builder.setClassPathEntries(prepareClassPath());
            builder.setObfuscated(minifying);
            builder.setStrict(strict);
            builder.setMaxTopLevelNames(maxTopLevelNames);
            builder.setTargetDirectory(targetDirectory.getAbsolutePath());
            if (transformers != null) {
                builder.setTransformers(transformers);
            }
            if (sourceFilesCopied) {
                getSourceFileProviders(builder);
                builder.addSourcesDirectory(sourceDirectory.getAbsolutePath());
            }
            if (properties != null) {
                builder.setProperties(properties);
            }
            builder.setIncremental(incremental);
            builder.setDebugInformationGenerated(debugInformationGenerated);
            builder.setSourceMapsFileGenerated(sourceMapsGenerated);
            builder.setSourceFilesCopied(sourceFilesCopied);
            builder.setMinHeapSize(minHeapSize * 1024 * 1024);
            builder.setMaxHeapSize(maxHeapSize * 1024 * 1024);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Unexpected error occurred", e);
        }
    }

    private List<String> prepareClassPath() {
        Log log = getLog();
        log.info("Preparing classpath for TeaVM");
        List<String> paths = new ArrayList<>();
        StringBuilder classpath = new StringBuilder();
        for (Artifact artifact : project.getArtifacts()) {
            if (!filterByScope(artifact)) {
                continue;
            }
            File file = artifact.getFile();
            if (classpath.length() > 0) {
                classpath.append(':');
            }
            classpath.append(file.getPath());
            paths.add(file.getAbsolutePath());
        }
        if (classpath.length() > 0) {
            classpath.append(':');
        }
        classpath.append(classFiles.getPath());
        paths.add(classFiles.getAbsolutePath());
        log.info("Using the following classpath for TeaVM: " + classpath);
        return paths;
    }

    private boolean filterByScope(Artifact artifact) {
        return compileScopes == null ? isSupportedScope(artifact.getScope())
                : compileScopes.contains(artifact.getScope());
    }

    protected boolean isSupportedScope(String scope) {
        switch (scope) {
            case Artifact.SCOPE_COMPILE:
            case Artifact.SCOPE_PROVIDED:
            case Artifact.SCOPE_SYSTEM:
                return true;
            default:
                return false;
        }
    }

    private void getSourceFileProviders(BuildStrategy builder) {
        MavenSourceFileProviderLookup lookup = new MavenSourceFileProviderLookup();
        lookup.setMavenProject(project);
        lookup.setRepositorySystem(repositorySystem);
        lookup.setLocalRepository(localRepository);
        lookup.setRemoteRepositories(remoteRepositories);
        lookup.setPluginDependencies(pluginArtifacts);
        lookup.resolve(builder);
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (outOfProcess) {
            executeInSeparateProcess();
        } else {
            executeWithBuilder(new InProcessBuildStrategy(URLClassLoader::new));
        }
    }

    private void executeInSeparateProcess() throws MojoExecutionException {
        DaemonInfo daemon;
        try {
            daemon = BuildDaemon.start(false, processMemory, new DaemonLogImpl(), createDaemonClassPath());
        } catch (Throwable e) {
            throw new MojoExecutionException("Error starting TeaVM process", e);
        }

        try {
            RemoteBuildService buildService;
            try {
                Registry registry = LocateRegistry.getRegistry(daemon.getPort());
                buildService = (RemoteBuildService) registry.lookup(RemoteBuildService.ID);
            } catch (RemoteException | NotBoundException e) {
                throw new MojoExecutionException("Error connecting TeaVM process", e);
            }

            RemoteBuildStrategy builder = new RemoteBuildStrategy(buildService);
            executeWithBuilder(builder);
        } finally {
            daemon.getProcess().destroy();
        }
    }

    private void executeWithBuilder(BuildStrategy builder) throws MojoExecutionException {
        builder.init();
        Log log = getLog();
        setupBuilder(builder);
        MavenTeaVMToolLog toolLog = new MavenTeaVMToolLog(log);
        builder.setLog(toolLog);
        try {
            builder.setMainClass(mainClass);
            builder.setEntryPointName(entryPointName);
            if (!targetFileName.isEmpty()) {
                builder.setTargetFileName(targetFileName);
            }
            builder.setOptimizationLevel(optimizationLevel);
            builder.setFastDependencyAnalysis(fastGlobalAnalysis);
            if (classesToPreserve != null) {
                builder.setClassesToPreserve(classesToPreserve);
            }
            builder.setCacheDirectory(cacheDirectory.getAbsolutePath());
            builder.setTargetType(targetType);
            builder.setWasmVersion(wasmVersion);
            builder.setLongjmpSupported(longjmpSupported);
            builder.setHeapDump(heapDump);
            BuildResult result;
            result = builder.build();
            TeaVMProblemRenderer.describeProblems(result.getCallGraph(), result.getProblems(), toolLog);
            if (stopOnErrors && !result.getProblems().getSevereProblems().isEmpty()) {
                throw new MojoExecutionException("Build error");
            }
        } catch (BuildException e) {
            throw new MojoExecutionException("Unexpected error occurred", e.getCause());
        } catch (Exception e) {
            throw new MojoExecutionException("Unexpected error occurred", e);
        }
    }

    private String[] createDaemonClassPath() {
        Artifact toolArtifact = pluginArtifacts.stream()
                .filter(artifact -> artifact.getGroupId().equals("org.teavm")
                        && artifact.getArtifactId().equals("teavm-tooling"))
                .findFirst()
                .orElse(null);
        if (toolArtifact == null) {
            return new String[0];
        }

        ArtifactResolutionResult resolutionResult = repositorySystem.resolve(new ArtifactResolutionRequest()
                .setLocalRepository(localRepository)
                .setRemoteRepositories(new ArrayList<>(remoteRepositories))
                .setResolveTransitively(true)
                .setResolveRoot(true)
                .setArtifact(toolArtifact));

        if (!resolutionResult.isSuccess()) {
            return new String[0];
        }

        return resolutionResult.getArtifacts().stream()
                .map(artifact -> artifact.getFile().getAbsolutePath())
                .toArray(String[]::new);
    }

    class DaemonLogImpl implements DaemonLog {
        @Override
        public void error(String message) {
            getLog().error(message);
        }

        @Override
        public void error(String message, Throwable e) {
            getLog().error(message, e);
        }

        @Override
        public void info(String message) {
            getLog().info(message);
        }
    }
}
