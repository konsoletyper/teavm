/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.cli;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.util.FileSystemWatcher;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public final class TeaVMRunner {
    private static Options options = new Options();
    private TeaVMTool tool = new TeaVMTool();
    private AccumulatingTeaVMToolLog log = new AccumulatingTeaVMToolLog(new ConsoleTeaVMToolLog(false));
    private CommandLine commandLine;
    private long startTime;
    private long phaseStartTime;
    private String[] classPath;
    private boolean interactive;

    static {
        setupOptions();
    }

    @SuppressWarnings("static-access")
    private static void setupOptions() {
        options.addOption(OptionBuilder
                .withArgName("target")
                .hasArg()
                .withDescription("target type (javascript/js, webassembly/wasm, C)")
                .create('t'));
        options.addOption(OptionBuilder
                .withArgName("directory")
                .hasArg()
                .withDescription("a directory where to put generated files (current directory by default)")
                .withLongOpt("targetdir")
                .create('d'));
        options.addOption(OptionBuilder
                .withArgName("file")
                .hasArg()
                .withDescription("a file where to put decompiled classes (classes.js by default)")
                .withLongOpt("targetfile")
                .create('f'));
        options.addOption(OptionBuilder
                .withDescription("causes TeaVM to generate minimized JavaScript file")
                .withLongOpt("minify")
                .create("m"));
        options.addOption(OptionBuilder
                .withDescription("causes TeaVM to produce code that is as close to Java semantics as possible "
                        + "(in cost of performance)")
                .create("strict"));
        options.addOption(OptionBuilder
                .withDescription("optimization level (1-3)")
                .hasArg()
                .withArgName("number")
                .create("O"));
        options.addOption(OptionBuilder
                .withDescription("Generate debug information")
                .withLongOpt("debug")
                .create('g'));
        options.addOption(OptionBuilder
                .withDescription("Generate source maps")
                .withLongOpt("sourcemaps")
                .create('G'));
        options.addOption(OptionBuilder
                .withDescription("Incremental build")
                .withLongOpt("incremental")
                .create('i'));
        options.addOption(OptionBuilder
                .withArgName("directory")
                .hasArg()
                .withDescription("Incremental build cache directory")
                .withLongOpt("cachedir")
                .create('c'));
        options.addOption(OptionBuilder
                .withDescription("Wait for command after compilation, in order to enable hot recompilation")
                .withLongOpt("wait")
                .create('w'));
        options.addOption(OptionBuilder
                .withArgName("classpath")
                .hasArgs()
                .withDescription("Additional classpath that will be reloaded by TeaVM each time in wait mode")
                .withLongOpt("classpath")
                .create('p'));
        options.addOption(OptionBuilder
                .withArgName("class name")
                .hasArgs()
                .withDescription("Tell optimizer to not remove class, so that it can be found by Class.forName")
                .withLongOpt("preserve-class")
                .create());
        options.addOption(OptionBuilder
                .withLongOpt("wasm-version")
                .withArgName("version")
                .hasArg()
                .withDescription("WebAssembly binary version (currently, only 1 is supported)")
                .create());
        options.addOption(OptionBuilder
                .withLongOpt("entry-point")
                .withArgName("name")
                .hasArg()
                .withDescription("Entry point name in target language (main by default)")
                .create("e"));
        options.addOption(OptionBuilder
                .withLongOpt("min-heap")
                .withArgName("size")
                .hasArg()
                .withDescription("Minimum heap size in megabytes (for C and WebAssembly)")
                .create());
        options.addOption(OptionBuilder
                .withLongOpt("max-heap")
                .withArgName("size")
                .hasArg()
                .withDescription("Maximum heap size in megabytes (for C and WebAssembly)")
                .create());
        options.addOption(OptionBuilder
                .withLongOpt("max-toplevel-names")
                .withArgName("number")
                .hasArg()
                .withDescription("Maximum number of names kept in top-level scope ("
                        + "other will be put in a separate object. 10000 by default.")
                .create());
        options.addOption(OptionBuilder
                .withLongOpt("no-longjmp")
                .withDescription("Don't use setjmp/longjmp functions to emulate exceptions (C target)")
                .create());
    }

    private TeaVMRunner(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage();
            return;
        }

        TeaVMRunner runner = new TeaVMRunner(commandLine);
        runner.parseArguments();
        runner.setUp();
        runner.runAll();
    }

    private void parseArguments() {
        parseClassPathOptions();
        parseTargetOption();
        parseOutputOptions();
        parseDebugOptions();
        parsePreserveClassOptions();
        parseOptimizationOption();
        parseIncrementalOptions();
        parseGenerationOptions();
        parseWasmOptions();
        parseCOptions();
        parseHeap();

        if (commandLine.hasOption("e")) {
            tool.setEntryPointName(commandLine.getOptionValue("e"));
        }

        interactive = commandLine.hasOption('w');

        String[] args = commandLine.getArgs();
        if (args.length > 1) {
            System.err.println("Unexpected arguments");
            printUsage();
        } else if (args.length == 1) {
            tool.setMainClass(args[0]);
        }
    }

    private void parseTargetOption() {
        if (commandLine.hasOption("t")) {
            switch (commandLine.getOptionValue('t').toLowerCase()) {
                case "javascript":
                case "js":
                    tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
                    break;
                case "webassembly":
                case "wasm":
                    tool.setTargetType(TeaVMTargetType.WEBASSEMBLY);
                    break;
                case "c":
                    tool.setTargetType(TeaVMTargetType.C);
                    break;
            }
        }
    }

    private void parseOutputOptions() {
        if (commandLine.hasOption("d")) {
            tool.setTargetDirectory(new File(commandLine.getOptionValue("d")));
        }
        if (commandLine.hasOption("f")) {
            tool.setTargetFileName(commandLine.getOptionValue("f"));
        }
    }

    private void parseGenerationOptions() {
        tool.setObfuscated(commandLine.hasOption("m"));
        tool.setStrict(commandLine.hasOption("strict"));

        if (commandLine.hasOption("max-toplevel-names")) {
            try {
                tool.setMaxTopLevelNames(Integer.parseInt(commandLine.getOptionValue("max-toplevel-names")));
            } catch (NumberFormatException e) {
                System.err.println("'--max-toplevel-names' must be integer number");
                printUsage();
            }
        }
    }

    private void parseDebugOptions() {
        if (commandLine.hasOption('g')) {
            tool.setDebugInformationGenerated(true);
        }
        if (commandLine.hasOption('G')) {
            tool.setSourceMapsFileGenerated(true);
        }
    }

    private void parsePreserveClassOptions() {
        if (commandLine.hasOption("preserve-class")) {
            tool.getClassesToPreserve().addAll(Arrays.asList(commandLine.getOptionValues("preserve-class")));
        }
    }

    private void parseOptimizationOption() {
        if (commandLine.hasOption("O")) {
            int level;
            try {
                level = Integer.parseInt(commandLine.getOptionValue("O"));
            } catch (NumberFormatException e) {
                System.err.print("Wrong optimization level");
                printUsage();
                return;
            }
            switch (level) {
                case 1:
                    tool.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
                    break;
                case 2:
                    tool.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
                    break;
                case 3:
                    tool.setOptimizationLevel(TeaVMOptimizationLevel.FULL);
                    break;
                default:
                    System.err.print("Wrong optimization level");
                    printUsage();
            }
        }
    }

    private void parseIncrementalOptions() {
        if (commandLine.hasOption('i')) {
            tool.setIncremental(true);
        }
        if (commandLine.hasOption('c')) {
            tool.setCacheDirectory(new File(commandLine.getOptionValue('c')));
        } else {
            tool.setCacheDirectory(new File(tool.getTargetDirectory(), "teavm-cache"));
        }
    }

    private void parseClassPathOptions() {
        if (commandLine.hasOption('p')) {
            classPath = commandLine.getOptionValues('p');
        }
    }

    private void parseWasmOptions() {
        if (commandLine.hasOption("wasm-version")) {
            String value = commandLine.getOptionValue("wasm-version");
            try {
                int version = Integer.parseInt(value);
                switch (version) {
                    case 1:
                        tool.setWasmVersion(WasmBinaryVersion.V_0x1);
                        break;
                    default:
                        System.err.print("Wrong version value");
                        printUsage();
                }
            } catch (NumberFormatException e) {
                System.err.print("Wrong version value");
                printUsage();
            }
        }
    }

    private void parseCOptions() {
        if (commandLine.hasOption("no-longjmp")) {
            tool.setLongjmpSupported(false);
        }
        if (commandLine.hasOption("heap-dump")) {
            tool.setHeapDump(true);
        }
    }

    private void parseHeap() {
        if (commandLine.hasOption("min-heap")) {
            int size;
            try {
                size = Integer.parseInt(commandLine.getOptionValue("min-heap"));
            } catch (NumberFormatException e) {
                System.err.print("Wrong heap size");
                printUsage();
                return;
            }
            tool.setMinHeapSize(size * 1024 * 1024);
        }
        if (commandLine.hasOption("max-heap")) {
            int size;
            try {
                size = Integer.parseInt(commandLine.getOptionValue("max-heap"));
            } catch (NumberFormatException e) {
                System.err.print("Wrong heap size");
                printUsage();
                return;
            }
            tool.setMaxHeapSize(size * 1024 * 1024);
        }
    }

    private void setUp() {
        tool.setLog(log);
        tool.getProperties().putAll(System.getProperties());
    }

    private void runAll() {
        if (interactive) {
            buildInteractive();
        } else {
            buildNonInteractive();
        }
    }

    private void buildInteractive() {
        FileSystemWatcher watcher;
        try {
            watcher = new FileSystemWatcher(classPath);
        } catch (IOException e) {
            System.err.println("Error listening file system events");
            e.printStackTrace();
            System.exit(2);
            return;
        }

        while (true) {
            ProgressListenerImpl progressListener = new ProgressListenerImpl(watcher);
            try {
                build(progressListener);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            try {
                System.out.println("Waiting for changes...");
                watcher.waitForChange(750);
                watcher.grabChangedFiles();
                System.out.println();
                System.out.println("Changes detected. Recompiling...");
            } catch (InterruptedException | IOException e) {
                break;
            }
        }
    }

    private void buildNonInteractive() {
        try {
            build(new ProgressListenerImpl(null));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-2);
        }
        if (!tool.getProblemProvider().getSevereProblems().isEmpty()) {
            System.exit(-2);
        }
    }

    private void build(ProgressListenerImpl progressListener) throws TeaVMToolException {
        tool.setProgressListener(progressListener);
        resetClassLoader();
        startTime = System.currentTimeMillis();
        phaseStartTime = System.currentTimeMillis();
        tool.generate();
        reportPhaseComplete();
        TeaVMProblemRenderer.describeProblems(tool.getDependencyInfo().getCallGraph(), tool.getProblemProvider(), log);
        log.flush();
        System.out.println("Build complete for " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
    }

    private void resetClassLoader() {
        if (classPath == null || classPath.length == 0) {
            return;
        }
        URL[] urls = new URL[classPath.length];
        for (int i = 0; i < classPath.length; ++i) {
            try {
                urls[i] = new File(classPath[i]).toURI().toURL();
            } catch (MalformedURLException e) {
                System.err.println("Illegal classpath entry: " + classPath[i]);
                System.exit(-1);
                return;
            }
        }

        tool.setClassLoader(new URLClassLoader(urls, TeaVMRunner.class.getClassLoader()));
    }

    class ProgressListenerImpl implements TeaVMProgressListener {
        private TeaVMPhase currentPhase;
        private FileSystemWatcher fileSystemWatcher;

        ProgressListenerImpl(FileSystemWatcher fileSystemWatcher) {
            this.fileSystemWatcher = fileSystemWatcher;
        }

        @Override
        public TeaVMProgressFeedback progressReached(int progress) {
            return getStatus();
        }

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
            log.flush();
            if (currentPhase != phase) {
                if (currentPhase != null) {
                    reportPhaseComplete();
                }
                phaseStartTime = System.currentTimeMillis();
                switch (phase) {
                    case DEPENDENCY_ANALYSIS:
                        System.out.print("Analyzing classes...");
                        break;
                    case COMPILING:
                        System.out.print("Compiling...");
                        break;
                }
                currentPhase = phase;
            }
            return getStatus();
        }

        private TeaVMProgressFeedback getStatus() {
            try {
                if (fileSystemWatcher != null && fileSystemWatcher.hasChanges()) {
                    System.out.println("Classes changed during compilation. Canceling.");
                    return TeaVMProgressFeedback.CANCEL;
                }
                return TeaVMProgressFeedback.CONTINUE;
            } catch (IOException e) {
                throw new RuntimeException("IO error occurred");
            }
        }
    }

    private void reportPhaseComplete() {
        System.out.println(" complete for " + ((System.currentTimeMillis() - phaseStartTime) / 1000.0) + " seconds");
        log.flush();
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + TeaVMRunner.class.getName() + " [OPTIONS] [qualified.main.Class]", options);
        System.exit(-1);
    }
}
