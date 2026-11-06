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
package org.teavm.tooling.daemon;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.backend.wasm.WasmDebugInfoLocation;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.tooling.sources.JarSourceFileProvider;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public final class BuildDaemon {
    private static final Options OPTIONS = createOptions();

    private BuildDaemon() {
    }

    public static void main(String[] args) {
        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(OPTIONS, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
            return;
        }

        var writer = new DaemonJsonWriter();
        var tool = new TeaVMTool();
        tool.setLog(writer);
        tool.setProgressListener(createProgressListener(writer));

        URLClassLoader classLoader;
        try {
            classLoader = configureTool(tool, commandLine);
        } catch (RuntimeException e) {
            writer.error(e);
            System.exit(-1);
            return;
        }

        try {
            tool.generate();
        } catch (TeaVMToolException | RuntimeException | Error e) {
            writer.error(e);
            System.exit(-1);
            return;
        } finally {
            try {
                classLoader.close();
            } catch (Exception e) {
                // do nothing
            }
        }

        var problems = TeaVMProblemRenderer.render(tool.getDependencyInfo().getCallGraph(),
                tool.getProblemProvider());
        writer.complete(problems);
    }

    private static TeaVMProgressListener createProgressListener(DaemonJsonWriter writer) {
        return new TeaVMProgressListener() {
            @Override
            public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
                writer.phaseStarted(phase, count);
                return TeaVMProgressFeedback.CONTINUE;
            }

            @Override
            public TeaVMProgressFeedback progressReached(int progress) {
                writer.progressReached(progress);
                return TeaVMProgressFeedback.CONTINUE;
            }
        };
    }

    private static URLClassLoader configureTool(TeaVMTool tool, CommandLine commandLine) {
        var classPathEntries = getValues(commandLine, "classpath");
        var classLoader = buildClassLoader(classPathEntries);
        tool.setClassLoader(classLoader);
        tool.setClassPath(classPathEntries.stream().map(File::new).collect(Collectors.toList()));

        if (commandLine.hasOption("target-type")) {
            tool.setTargetType(TeaVMTargetType.valueOf(commandLine.getOptionValue("target-type")));
        }
        tool.setTargetDirectory(new File(commandLine.getOptionValue("target-dir")));
        if (commandLine.hasOption("target-file")) {
            tool.setTargetFileName(commandLine.getOptionValue("target-file"));
        }
        tool.setMainClass(commandLine.getOptionValue("main-class"));
        if (commandLine.hasOption("entry-point-name")) {
            tool.setEntryPointName(commandLine.getOptionValue("entry-point-name"));
        }

        for (var directory : getValues(commandLine, "source-directory")) {
            tool.addSourceFileProvider(new DirectorySourceFileProvider(new File(directory)));
        }
        for (var jarFile : getValues(commandLine, "source-jar")) {
            tool.addSourceFileProvider(new JarSourceFileProvider(new File(jarFile)));
        }

        tool.setSourceMapsFileGenerated(commandLine.hasOption("source-maps"));
        tool.setDebugInformationGenerated(commandLine.hasOption("debug-info"));
        if (commandLine.hasOption("source-file-policy")) {
            tool.setSourceFilePolicy(TeaVMSourceFilePolicy.valueOf(commandLine.getOptionValue("source-file-policy")));
        }

        tool.setObfuscated(commandLine.hasOption("obfuscated"));
        tool.setStrict(commandLine.hasOption("strict"));
        if (commandLine.hasOption("js-module-type")) {
            tool.setJsModuleType(JSModuleType.valueOf(commandLine.getOptionValue("js-module-type")));
        }
        if (commandLine.hasOption("max-top-level-names")) {
            tool.setMaxTopLevelNames(Integer.parseInt(commandLine.getOptionValue("max-top-level-names")));
        }

        var properties = commandLine.getOptionProperties("property");
        for (var name : properties.stringPropertyNames()) {
            tool.getProperties().setProperty(name, properties.getProperty(name));
        }

        tool.getTransformers().addAll(getValues(commandLine, "transformer"));
        tool.getClassesToPreserve().addAll(getValues(commandLine, "classes-to-preserve"));

        if (commandLine.hasOption("optimization-level")) {
            tool.setOptimizationLevel(TeaVMOptimizationLevel.valueOf(commandLine.getOptionValue("optimization-level")));
        }
        tool.setFastDependencyAnalysis(commandLine.hasOption("fast-dependency-analysis"));

        if (commandLine.hasOption("wasm-version")) {
            tool.setWasmVersion(WasmBinaryVersion.valueOf(commandLine.getOptionValue("wasm-version")));
        }
        if (commandLine.hasOption("wasm-debug-info-level")) {
            tool.setWasmDebugInfoLevel(
                    WasmDebugInfoLevel.valueOf(commandLine.getOptionValue("wasm-debug-info-level")));
        }
        if (commandLine.hasOption("wasm-debug-info-location")) {
            tool.setWasmDebugInfoLocation(
                    WasmDebugInfoLocation.valueOf(commandLine.getOptionValue("wasm-debug-info-location")));
        }

        if (commandLine.hasOption("min-heap-size")) {
            tool.setMinHeapSize(Integer.parseInt(commandLine.getOptionValue("min-heap-size")));
        }
        if (commandLine.hasOption("max-heap-size")) {
            tool.setMaxHeapSize(Integer.parseInt(commandLine.getOptionValue("max-heap-size")));
        }
        if (commandLine.hasOption("min-direct-buffers-size")) {
            tool.setMinDirectBuffersSize(Integer.parseInt(commandLine.getOptionValue("min-direct-buffers-size")));
        }
        tool.setSharedBuffer(commandLine.hasOption("shared-buffer"));
        tool.setHeapDump(commandLine.hasOption("heap-dump"));
        tool.setShortFileNames(commandLine.hasOption("short-file-names"));
        tool.setAssertionsRemoved(commandLine.hasOption("assertions-removed"));

        return classLoader;
    }

    private static List<String> getValues(CommandLine commandLine, String option) {
        var values = commandLine.getOptionValues(option);
        return values != null ? Arrays.asList(values) : List.of();
    }

    private static URLClassLoader buildClassLoader(List<String> classPathEntries) {
        var urls = classPathEntries.stream().map(entry -> {
            try {
                return new File(entry).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(entry, e);
            }
        }).toArray(URL[]::new);

        return new URLClassLoader(urls, BuildDaemon.class.getClassLoader());
    }

    private static Options createOptions() {
        var options = new Options();
        options.addOption(Option.builder().longOpt("target-type").hasArg().get());
        options.addOption(Option.builder().longOpt("target-dir").hasArg().required().get());
        options.addOption(Option.builder().longOpt("target-file").hasArg().get());
        options.addOption(Option.builder().longOpt("main-class").hasArg().required().get());
        options.addOption(Option.builder().longOpt("entry-point-name").hasArg().get());
        options.addOption(Option.builder().longOpt("classpath").hasArgs().get());
        options.addOption(Option.builder().longOpt("source-directory").hasArgs().get());
        options.addOption(Option.builder().longOpt("source-jar").hasArgs().get());
        options.addOption(Option.builder().longOpt("source-maps").get());
        options.addOption(Option.builder().longOpt("debug-info").get());
        options.addOption(Option.builder().longOpt("source-file-policy").hasArg().get());
        options.addOption(Option.builder().longOpt("obfuscated").get());
        options.addOption(Option.builder().longOpt("strict").get());
        options.addOption(Option.builder().longOpt("js-module-type").hasArg().get());
        options.addOption(Option.builder().longOpt("max-top-level-names").hasArg().get());
        options.addOption(Option.builder().valueSeparator().hasArgs().longOpt("property").get());
        options.addOption(Option.builder().longOpt("transformer").hasArgs().get());
        options.addOption(Option.builder().longOpt("optimization-level").hasArg().get());
        options.addOption(Option.builder().longOpt("fast-dependency-analysis").get());
        options.addOption(Option.builder().longOpt("classes-to-preserve").hasArgs().get());
        options.addOption(Option.builder().longOpt("wasm-version").hasArg().get());
        options.addOption(Option.builder().longOpt("wasm-debug-info-level").hasArg().get());
        options.addOption(Option.builder().longOpt("wasm-debug-info-location").hasArg().get());
        options.addOption(Option.builder().longOpt("min-heap-size").hasArg().get());
        options.addOption(Option.builder().longOpt("max-heap-size").hasArg().get());
        options.addOption(Option.builder().longOpt("min-direct-buffers-size").hasArg().get());
        options.addOption(Option.builder().longOpt("shared-buffer").get());
        options.addOption(Option.builder().longOpt("heap-dump").get());
        options.addOption(Option.builder().longOpt("short-file-names").get());
        options.addOption(Option.builder().longOpt("assertions-removed").get());
        return options;
    }
}
