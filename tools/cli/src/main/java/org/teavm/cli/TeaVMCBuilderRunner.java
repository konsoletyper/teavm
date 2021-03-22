/*
 *  Copyright 2019 Alexey Andreev.
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.tooling.c.incremental.IncrementalCBuilder;

public class TeaVMCBuilderRunner {
    private static Options options = new Options();
    private IncrementalCBuilder builder;
    private CommandLine commandLine;

    static {
        setupOptions();
    }

    private static void setupOptions() {
        options.addOption(Option.builder("d")
                .argName("directory")
                .hasArg()
                .desc("a directory in which generated C files will be placed")
                .longOpt("targetdir")
                .build());
        options.addOption(Option.builder("p")
                .argName("classpath")
                .hasArgs()
                .desc("classpath element (either directory or jar file)")
                .longOpt("classpath")
                .build());
        options.addOption(Option.builder("v")
                .desc("display more messages on server log")
                .longOpt("verbose")
                .build());
        options.addOption(Option.builder("g")
                .desc("generate debugger-friendly code")
                .longOpt("debug")
                .build());
        options.addOption(Option.builder()
                .longOpt("min-heap")
                .argName("size")
                .hasArg()
                .desc("Minimum heap size in megabytes")
                .build());
        options.addOption(Option.builder()
                .longOpt("max-heap")
                .argName("size")
                .hasArg()
                .desc("Minimum heap size in megabytes")
                .build());
        options.addOption(Option.builder()
                .longOpt("no-longjmp")
                .desc("Don't use setjmp/longjmp functions to emulate exception handling")
                .build());
        options.addOption(Option.builder("e")
                .longOpt("entry-point")
                .argName("name")
                .hasArg()
                .desc("Name of entry point function (main by default)")
                .build());
        options.addOption(Option.builder()
                .longOpt("external-tool")
                .argName("path")
                .hasArg()
                .desc("Process to run after successful build")
                .build());
        options.addOption(Option.builder()
                .longOpt("external-tool-workdir")
                .argName("path")
                .hasArg()
                .desc("Working directory of process")
                .build());
    }

    private TeaVMCBuilderRunner(CommandLine commandLine) {
        this.commandLine = commandLine;
        builder = new IncrementalCBuilder();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage();
            return;
        }

        TeaVMCBuilderRunner runner = new TeaVMCBuilderRunner(commandLine);
        runner.parseArguments();
        runner.runAll();
    }

    private void parseArguments() {
        parseClassPathOptions();
        parseOutputOptions();
        parseHeap();
        parseExternalTool();

        builder.setLog(new ConsoleTeaVMToolLog(commandLine.hasOption('v')));
        builder.setLineNumbersGenerated(commandLine.hasOption('g'));
        if (commandLine.hasOption('e')) {
            builder.setMainFunctionName(commandLine.getOptionValue('e'));
        }
        if (commandLine.hasOption("no-longjmp")) {
            builder.setLongjmpSupported(false);
        }

        String[] args = commandLine.getArgs();
        if (args.length != 1) {
            System.err.println("Unexpected arguments");
            printUsage();
        } else {
            builder.setMainClass(args[0]);
        }
    }

    private void parseExternalTool() {
        boolean hasExternalTool = false;
        if (commandLine.hasOption("external-tool")) {
            builder.setExternalTool(commandLine.getOptionValue("external-tool"));
            hasExternalTool = true;
        }

        if (commandLine.hasOption("external-tool-workdir")) {
            if (!hasExternalTool) {
                System.err.println("Redundant 'external-tool-workdir' option: no external tool specified");
            } else {
                builder.setExternalToolWorkingDir(commandLine.getOptionValue("external-tool-workdir"));
            }
        }
    }

    private void parseOutputOptions() {
        if (commandLine.hasOption("d")) {
            builder.setTargetPath(commandLine.getOptionValue("d"));
        }
    }

    private void parseClassPathOptions() {
        if (commandLine.hasOption('p')) {
            builder.setClassPath(commandLine.getOptionValues('p'));
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
            builder.setMinHeapSize(size);
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
            builder.setMaxHeapSize(size);
        }
    }

    private void runAll() {
        builder.start();
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + TeaVMCBuilderRunner.class.getName() + " [OPTIONS] [qualified.main.Class]",
                options);
        System.exit(-1);
    }
}
