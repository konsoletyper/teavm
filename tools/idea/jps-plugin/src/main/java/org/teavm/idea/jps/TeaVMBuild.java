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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsLibraryDependency;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleDependency;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public class TeaVMBuild {
    private CompileContext context;
    private TeaVMStorageProvider storageProvider = new TeaVMStorageProvider();
    private List<String> classPathEntries = new ArrayList<>();
    private List<String> directoryClassPathEntries;
    private TeaVMStorage storage;

    public TeaVMBuild(CompileContext context) {
        this.context = context;
    }

    public boolean perform(JpsModule module, ModuleBuildTarget target) throws IOException {
        storage = context.getProjectDescriptor().dataManager.getStorage(target, storageProvider);

        TeaVMJpsConfiguration config = TeaVMJpsConfiguration.get(module);
        if (config == null || !config.isEnabled()) {
            return false;
        }

        classPathEntries.clear();
        buildClassPath(module, new HashSet<>());
        directoryClassPathEntries = classPathEntries.stream().filter(name -> new File(name).isDirectory())
                .collect(toList());

        if (!hasChanges(target)) {
            return false;
        }

        TeaVMTool tool = new TeaVMTool();
        tool.setProgressListener(createProgressListener(context));
        tool.setLog(createLog(context));
        tool.setMainClass(config.getMainClass());
        tool.setSourceMapsFileGenerated(true);
        tool.setTargetDirectory(new File(config.getTargetDirectory()));
        tool.setClassLoader(buildClassLoader());
        tool.setMinifying(false);

        boolean errorOccurred = false;
        try {
            tool.generate();
        } catch (TeaVMToolException | RuntimeException | Error e) {
            e.printStackTrace(System.err);
            context.processMessage(new CompilerMessage("TeaVM", e));
            errorOccurred = true;
        }

        if (!errorOccurred && tool.getProblemProvider().getSevereProblems().isEmpty()) {
            updateStorage(tool);
        }

        return true;
    }

    private boolean hasChanges(ModuleBuildTarget target) {
        if (!context.getScope().isBuildIncrementally(target.getTargetType())
                || context.getScope().isBuildForced(target)) {
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

    private void updateStorage(TeaVMTool tool) {
        Set<String> resources = Stream.concat(tool.getClasses().stream().map(cls -> cls.replace('.', '/') + ".class"),
                tool.getUsedResources().stream())
                .sorted()
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

    private TeaVMToolLog createLog(CompileContext context) {
        return new TeaVMToolLog() {
            @Override
            public void info(String text) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.INFO, text));
            }

            @Override
            public void debug(String text) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.INFO, text));
            }

            @Override
            public void warning(String text) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.WARNING, text));
            }

            @Override
            public void error(String text) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.ERROR, text));
            }

            @Override
            public void info(String text, Throwable e) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.INFO, text + "\n"
                        + CompilerMessage.getTextFromThrowable(e)));
            }

            @Override
            public void debug(String text, Throwable e) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.INFO, text + "\n"
                        + CompilerMessage.getTextFromThrowable(e)));
            }

            @Override
            public void warning(String text, Throwable e) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.WARNING, text + "\n"
                        + CompilerMessage.getTextFromThrowable(e)));
            }

            @Override
            public void error(String text, Throwable e) {
                context.processMessage(new CompilerMessage("TeaVM", BuildMessage.Kind.ERROR, text + "\n"
                        + CompilerMessage.getTextFromThrowable(e)));
            }
        };
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
            case DEPENDENCY_CHECKING:
                return "Discovering classes to compile";
            case LINKING:
                return "Resolving method invocations";
            case DEVIRTUALIZATION:
                return "Eliminating virtual calls";
            case DECOMPILATION:
                return "Compiling classes";
            case RENDERING:
                return "Building JS file";
            default:
                throw new AssertionError();
        }
    }

    private ClassLoader buildClassLoader() {
        URL[] urls = classPathEntries.stream().map(entry -> {
            try {
                return new File(entry).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(entry);
            }
        }).toArray(URL[]::new);

        return new URLClassLoader(urls, TeaVMBuilder.class.getClassLoader());
    }

    private void buildClassPath(JpsModule module, Set<JpsModule> visited) {
        if (!visited.add(module)) {
            return;
        }
        File output = JpsJavaExtensionService.getInstance().getOutputDirectory(module, false);
        if (output != null) {
            classPathEntries.add(output.getPath());
        }
        for (JpsDependencyElement dependency : module.getDependenciesList().getDependencies()) {
            if (dependency instanceof JpsModuleDependency) {
                buildClassPath(((JpsModuleDependency) dependency).getModule(), visited);
            } else if (dependency instanceof JpsLibraryDependency) {
                JpsLibrary library = ((JpsLibraryDependency) dependency).getLibrary();
                if (library == null) {
                    continue;
                }
                classPathEntries.addAll(library.getFiles(JpsOrderRootType.COMPILED).stream().map(File::getPath)
                        .collect(toList()));
            }
        }
    }
}
