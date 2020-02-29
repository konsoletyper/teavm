/*
 *  Copyright 2016 Alexey Andreev.
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.jps.builders.BuildOutputConsumer;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryRoot;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsLibraryDependency;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleDependency;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.common.IntegerArray;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;
import org.teavm.idea.jps.model.TeaVMProperty;
import org.teavm.idea.jps.remote.TeaVMBuilderAssistant;
import org.teavm.idea.jps.remote.TeaVMElementLocation;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.tooling.builder.BuildException;
import org.teavm.tooling.builder.BuildResult;
import org.teavm.tooling.builder.BuildStrategy;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

class TeaVMBuild {
    private static final String[] BLACKLISTED_CLASSES = {
            "org/teavm/jso/impl/JSOPlugin.class",
            "org/teavm/metaprogramming/impl/MetaprogrammingImpl.class",
            "org/teavm/classlib/impl/JCLPlugin.class"
    };

    private final CompileContext context;
    private final List<String> classPathEntries = new ArrayList<>();
    private List<String> directoryClassPathEntries;
    private TeaVMStorage storage;
    private final TeaVMBuilderAssistant assistant;
    private final Map<String, File> sourceFileCache = new HashMap<>();
    private final Map<File, int[]> fileLineCache = new HashMap<>();
    private BuildStrategy buildStrategy;
    private BuildOutputConsumer outputConsumer;
    private boolean incrementaSupported;

    TeaVMBuild(CompileContext context, TeaVMBuilderAssistant assistant, BuildStrategy buildStrategy,
            BuildOutputConsumer outputConsumer, boolean incrementalSupported) {
        this.context = context;
        this.assistant = assistant;
        this.buildStrategy = buildStrategy;
        this.outputConsumer = outputConsumer;
        this.incrementaSupported = incrementalSupported;
    }

    boolean perform(JpsModule module, TeaVMBuildTarget target) throws IOException, BuildException {
        TeaVMStorageProvider storageProvider = new TeaVMStorageProvider(
                target.getConfiguration().getTargetType().name());
        storage = context.getProjectDescriptor().dataManager.getStorage(target, storageProvider);

        TeaVMJpsConfiguration config = target.getConfiguration();
        if (config.isSkipped()) {
            context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.INFO,
                    "TeaVM skipped due to facet configuration"));
            return false;
        }

        classPathEntries.clear();
        buildStrategy.init();
        buildClassPath(module);
        directoryClassPathEntries = classPathEntries.stream().filter(name -> new File(name).isDirectory())
                .collect(toList());

        if (!hasChanges(target)) {
            return false;
        }

        buildStrategy.setClassPathEntries(classPathEntries);
        buildStrategy.setDebugInformationGenerated(config.isSourceMapsFileGenerated());
        buildStrategy.setSourceMapsFileGenerated(config.isSourceMapsFileGenerated());
        buildStrategy.setSourceFilesCopied(config.isSourceFilesCopied());
        buildStrategy.setMainClass(config.getMainClass());
        buildStrategy.setTargetType(config.getTargetType());
        buildStrategy.setTargetDirectory(config.getTargetDirectory());
        buildStrategy.setProgressListener(createProgressListener(context));
        buildStrategy.setIncremental(incrementaSupported && !isRebuild(target));

        Properties properties = new Properties();
        for (TeaVMProperty property : config.getProperties()) {
            properties.put(property.getKey(), property.getValue());
        }
        buildStrategy.setProperties(properties);

        BuildResult buildResult = buildStrategy.build();

        if (!buildResult.getProblems().getSevereProblems().isEmpty()) {
            updateStorage(buildResult);
        }

        reportProblems(buildResult.getProblems(), buildResult.getCallGraph());

        for (String fileName : buildResult.getGeneratedFiles()) {
            outputConsumer.registerOutputFile(new File(fileName), Collections.emptyList());
        }

        return true;
    }

    private TeaVMProgressListener createProgressListener(CompileContext context) {
        return new TeaVMProgressListener() {
            private TeaVMPhase currentPhase;
            int expectedCount;

            @Override
            public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
                expectedCount = count;
                context.processMessage(new ProgressMessage(phaseName(phase), 0));
                currentPhase = phase;
                return context.getCancelStatus().isCanceled() ? TeaVMProgressFeedback.CANCEL
                        : TeaVMProgressFeedback.CONTINUE;
            }

            @Override
            public TeaVMProgressFeedback progressReached(int progress) {
                context.processMessage(new ProgressMessage(phaseName(currentPhase), (float) progress / expectedCount));
                return context.getCancelStatus().isCanceled() ? TeaVMProgressFeedback.CANCEL
                        : TeaVMProgressFeedback.CONTINUE;
            }
        };
    }

    private static String phaseName(TeaVMPhase phase) {
        switch (phase) {
            case DEPENDENCY_ANALYSIS:
                return "Discovering classes to compile";
            case COMPILING:
                return "Compiling";
            default:
                throw new AssertionError();
        }
    }

    private void reportProblems(ProblemProvider problemProvider, CallGraph callGraph) {
        for (Problem problem : problemProvider.getProblems()) {
            BuildMessage.Kind kind;
            switch (problem.getSeverity()) {
                case ERROR:
                    kind = BuildMessage.Kind.ERROR;
                    break;
                case WARNING:
                    kind = BuildMessage.Kind.WARNING;
                    break;
                default:
                    continue;
            }

            DefaultProblemTextConsumer textConsumer = new DefaultProblemTextConsumer();
            problem.render(textConsumer);
            String baseText = textConsumer.getText();

            List<ProblemToReport> problemsToReport = resolveProblemLocation(problem, callGraph);

            if (problemsToReport.isEmpty()) {
                context.processMessage(new CompilerMessage("TeaVM", kind, problem.getText(), null,
                        -1, -1, -1, -1, -1));
            }
            for (ProblemToReport problemToReport : problemsToReport) {
                String text = baseText + buildCallStack(problemToReport.locations);
                context.processMessage(new CompilerMessage("TeaVM", kind, text, problemToReport.path,
                        problemToReport.startOffset, problemToReport.endOffset, problemToReport.startOffset,
                        problemToReport.line, 0));
            }
        }
    }

    static class ProblemToReport {
        int line;
        int startOffset;
        int endOffset;
        String path;
        CallLocationList locations;
    }

    static class CallLocationList {
        final CallLocation value;
        final CallLocationList next;

        private CallLocationList(CallLocation value, CallLocationList next) {
            this.value = value;
            this.next = next;
        }
    }

    private List<ProblemToReport> resolveProblemLocation(Problem problem, CallGraph callGraph) {
        class Step {
            private final CallLocationList locationList;
            private final CallLocation location;

            private Step(CallLocationList locationList, CallLocation location) {
                this.locationList = locationList;
                this.location = location;
            }
        }

        List<ProblemToReport> problemsToReport = new ArrayList<>();
        Set<MethodReference> visited = new HashSet<>();
        Queue<Step> workList = new ArrayDeque<>();
        workList.add(new Step(null, problem.getLocation()));

        while (!workList.isEmpty()) {
            Step step = workList.remove();
            if (step.location == null || !visited.add(step.location.getMethod())) {
                continue;
            }
            ProblemToReport result = getProblemLocation(step.location);

            CallGraphNode node = callGraph.getNode(step.location.getMethod());
            if (node.getCallerCallSites().isEmpty() || isValid(result)) {
                result.locations = step.locationList;
                problemsToReport.add(result);
            } else {
                for (CallSite callSite : node.getCallerCallSites()) {
                    for (CallGraphNode caller : callSite.getCallers()) {
                        CallLocation nextLocation = new CallLocation(caller.getMethod(),
                                getLocation(callSite.getLocations(caller)));
                        workList.add(new Step(new CallLocationList(step.location, step.locationList), nextLocation));
                    }
                }
            }
        }

        return problemsToReport;
    }

    private TextLocation getLocation(Collection<? extends TextLocation> location) {
        return location.isEmpty() ? null : location.iterator().next();
    }

    private boolean isValid(ProblemToReport problemToReport) {
        return problemToReport.path != null && (problemToReport.line >= 0 || problemToReport.startOffset >= 0);
    }

    private ProblemToReport getProblemLocation(CallLocation callLocation) {
        String path = null;
        File file = null;
        int line = -1;
        int startOffset = -1;
        int endOffset = -1;

        if (callLocation != null) {
            TextLocation insnLocation = callLocation.getSourceLocation();
            if (insnLocation != null) {
                path = insnLocation.getFileName();
                line = insnLocation.getLine();
            }

            if (line <= 0 && assistant != null && callLocation.getMethod() != null) {
                MethodReference method = callLocation.getMethod();
                try {
                    TeaVMElementLocation location = assistant.getMethodLocation(method.getClassName(),
                            method.getName(), ValueType.methodTypeToString(method.getSignature()));
                    line = location.getLine();
                    startOffset = location.getStartOffset();
                    endOffset = location.getEndOffset();
                    file = new File(location.getPath());

                    if (line <= 0) {
                        int[] lines = getLineOffsets(file);
                        if (lines != null) {
                            line = Arrays.binarySearch(lines, startOffset + 1);
                            if (line < 0) {
                                line = -line - 1;
                            }
                        }
                    }
                } catch (Exception e) {
                    // just don't fill location fields
                }
            }

            if (file == null) {
                if (path != null) {
                    file = lookupSource(path);
                    path = file != null ? file.getPath() : null;
                }
            } else {
                path = file.getPath();
            }

            if (startOffset < 0 && file != null && line > 0) {
                int[] lines = getLineOffsets(file);
                if (lines != null && line < lines.length) {
                    startOffset = lines[line - 1];
                    endOffset = lines[line] - 1;
                }
            }
        }

        ProblemToReport result = new ProblemToReport();
        result.line = line;
        result.startOffset = startOffset;
        result.endOffset = endOffset;
        result.path = path;
        return result;
    }

    private String buildCallStack(CallLocationList callLocationList) {
        List<CallLocation> locations = new ArrayList<>();
        while (callLocationList != null) {
            locations.add(callLocationList.value);
            callLocationList = callLocationList.next;
        }
        Collections.reverse(locations);

        StringBuilder sb = new StringBuilder();
        for (CallLocation location : locations) {
            sb.append("\n  at ").append(location.getMethod());
            if (location.getSourceLocation() != null) {
                sb.append("(").append(location.getSourceLocation()).append(")");
            }
        }
        return sb.toString();
    }

    private File lookupSource(String relativePath) {
        return sourceFileCache.computeIfAbsent(relativePath, this::lookupSourceCacheMiss);
    }

    private File lookupSourceCacheMiss(String relativePath) {
        JpsProject project = context.getProjectDescriptor().getModel().getProject();
        for (JpsModule module : project.getModules()) {
            for (JpsModuleSourceRoot sourceRoot : module.getSourceRoots()) {
                File fullPath = new File(sourceRoot.getFile(), relativePath);
                if (fullPath.exists()) {
                    return fullPath;
                }
            }
        }
        return null;
    }

    private int[] getLineOffsets(File file) {
        return fileLineCache.computeIfAbsent(file, this::getLineOffsetsCacheMiss);
    }

    private int[] getLineOffsetsCacheMiss(File file) {
        IntegerArray lines = new IntegerArray(50);
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            int offset = 0;
            lines.add(0);

            boolean expectingLf = false;
            while (true) {
                int c = reader.read();
                if (c == -1) {
                    break;
                }
                if (c == '\n') {
                    expectingLf = false;
                    lines.add(offset + 1);
                } else {
                    if (expectingLf) {
                        expectingLf = false;
                        lines.add(offset);
                    }
                    if (c == '\r') {
                        lines.add(offset + 1);
                        expectingLf = true;
                    }
                }
                ++offset;
            }

            if (expectingLf) {
                lines.add(offset);
            }
            lines.add(offset + 1);
        } catch (IOException e) {
            return null;
        }
        return lines.getAll();
    }

    private boolean isRebuild(TeaVMBuildTarget target) {
        return !context.getScope().isBuildIncrementally(target.getTargetType())
                || context.getScope().isBuildForced(target);
    }

    private boolean hasChanges(TeaVMBuildTarget target) {
        if (isRebuild(target)) {
            return true;
        }

        List<TeaVMStorage.Entry> filesToWatch = storage.getParticipatingFiles();
        if (filesToWatch == null) {
            return true;
        }

        for (TeaVMStorage.Entry fileToWatch : filesToWatch) {
            Long actualTimestamp = getTimestamp(fileToWatch.path);
            if (actualTimestamp == null || actualTimestamp > fileToWatch.timestamp) {
                return true;
            }
        }
        return false;
    }

    private void updateStorage(BuildResult buildResult) {
        Set<String> resources = Stream.concat(buildResult.getClasses().stream()
                .map(cls -> cls.replace('.', '/') + ".class"), buildResult.getUsedResources().stream())
                .collect(toSet());
        List<TeaVMStorage.Entry> participatingFiles = resources.stream()
                .map(path -> {
                    Long timestamp = getTimestamp(path);
                    return timestamp != null ? new TeaVMStorage.Entry(path, timestamp) : null;
                })
                .filter(Objects::nonNull)
                .collect(toList());

        storage.setParticipatingFiles(participatingFiles);
    }

    private Long getTimestamp(String path) {
        for (String classPathEntry : directoryClassPathEntries) {
            File file = new File(classPathEntry, path);
            if (file.exists()) {
                return file.lastModified();
            }
        }
        return null;
    }

    private void buildClassPath(JpsModule module) {
        File output = JpsJavaExtensionService.getInstance().getOutputDirectory(module, false);
        if (output != null) {
            classPathEntries.add(output.getPath());
        }

        for (JpsModuleSourceRoot sourceRoot : module.getSourceRoots()) {
            buildStrategy.addSourcesDirectory(sourceRoot.getFile().getAbsolutePath());
        }

        for (JpsDependencyElement dependency : module.getDependenciesList().getDependencies()) {
            if (dependency instanceof JpsModuleDependency) {
                JpsModuleDependency moduleDependency = (JpsModuleDependency) dependency;
                File dependencyOutput = JpsJavaExtensionService.getInstance().getOutputDirectory(
                        moduleDependency.getModule(), false);
                if (dependencyOutput != null && !isBlacklistedDependency(dependencyOutput)) {
                    classPathEntries.add(dependencyOutput.getPath());
                }
                for (JpsModuleSourceRoot sourceRoot : moduleDependency.getModule().getSourceRoots()) {
                    buildStrategy.addSourcesDirectory(sourceRoot.getFile().getAbsolutePath());
                }
            } else if (dependency instanceof JpsLibraryDependency) {
                JpsLibrary library = ((JpsLibraryDependency) dependency).getLibrary();
                if (library == null) {
                    continue;
                }
                classPathEntries.addAll(library.getFiles(JpsOrderRootType.COMPILED).stream().map(File::getPath)
                        .collect(toList()));

                for (JpsLibraryRoot libraryRoot : library.getRoots(JpsOrderRootType.SOURCES)) {
                    File file = getFileFromUrl(libraryRoot.getUrl());
                    if (file != null) {
                        if (file.isDirectory()) {
                            buildStrategy.addSourcesDirectory(file.getAbsolutePath());
                        } else {
                            buildStrategy.addSourcesJar(file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    private static boolean isBlacklistedDependency(File dependency) {
        for (String entry : BLACKLISTED_CLASSES) {
            if (new File(dependency, entry).exists()) {
                return true;
            }
        }
        return false;
    }

    private File getFileFromUrl(String url) {
        if (url.startsWith("file://")) {
            return new File(url.substring("file://".length()));
        } else if (url.startsWith("jar://")) {
            int index = url.indexOf('!');
            return new File(url.substring("file://".length(), index));
        } else if (url.startsWith("jar:file://")) {
            int index = url.indexOf('!');
            return new File(url.substring("jar:file://".length(), index));
        } else {
            return null;
        }
    }
}
