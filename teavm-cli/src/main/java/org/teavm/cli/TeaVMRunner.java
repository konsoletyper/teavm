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
import org.apache.commons.cli.*;
import org.teavm.tooling.RuntimeCopyOperation;
import org.teavm.tooling.TeaVMTool;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public final class TeaVMRunner {
    private TeaVMRunner() {
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        Options options = new Options();
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
                .withArgName("separate|merge|none")
                .hasArg()
                .withDescription("how to attach runtime. Possible values are: separate|merge|none")
                .withLongOpt("runtime")
                .create("r"));
        options.addOption(OptionBuilder
                .withDescription("causes TeaVM to include default main page")
                .withLongOpt("mainpage")
                .create());
        options.addOption(OptionBuilder
                .withDescription("causes TeaVM to log bytecode")
                .withLongOpt("logbytecode")
                .create());
        options.addOption(OptionBuilder
                .withDescription("Generate debug information")
                .withLongOpt("debug")
                .create('D'));
        options.addOption(OptionBuilder
                .withDescription("Generate source maps")
                .withLongOpt("sourcemaps")
                .create());
        options.addOption(OptionBuilder
                .withArgName("number")
                .hasArg()
                .withDescription("how many threads should TeaVM run")
                .withLongOpt("threads")
                .create("t"));

        if (args.length == 0) {
            printUsage(options);
            return;
        }
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage(options);
            return;
        }

        TeaVMTool tool = new TeaVMTool();
        tool.setBytecodeLogging(commandLine.hasOption("logbytecode"));
        if (commandLine.hasOption("d")) {
            tool.setTargetDirectory(new File(commandLine.getOptionValue("d")));
        }
        if (commandLine.hasOption("f")) {
            tool.setTargetFileName(commandLine.getOptionValue("f"));
        }
        if (commandLine.hasOption("m")) {
            tool.setMinifying(true);
        } else {
            tool.setMinifying(false);
        }
        if (commandLine.hasOption("r")) {
            switch (commandLine.getOptionValue("r")) {
                case "separate":
                    tool.setRuntime(RuntimeCopyOperation.SEPARATE);
                    break;
                case "merge":
                    tool.setRuntime(RuntimeCopyOperation.MERGED);
                    break;
                case "none":
                    tool.setRuntime(RuntimeCopyOperation.NONE);
                    break;
                default:
                    System.err.println("Wrong parameter for -r option specified");
                    printUsage(options);
                    return;
            }
        }
        if (commandLine.hasOption("mainpage")) {
            tool.setMainPageIncluded(true);
        }
        if (commandLine.hasOption("t")) {
            try {
                tool.setNumThreads(Integer.parseInt(commandLine.getOptionValue("t")));
            } catch (NumberFormatException e) {
                System.err.println("Wrong parameter for -t option specified");
                printUsage(options);
                return;
            }
        }
        if (commandLine.hasOption('D')) {
            tool.setDebugInformation(new File(tool.getTargetDirectory(), tool.getTargetFileName() + ".teavmdbg"));
            if (commandLine.hasOption("sourcemaps")) {
                tool.setSourceMapsFileGenerated(true);
            }
        }
        args = commandLine.getArgs();
        if (args.length > 1) {
            System.err.println("Unexpected arguments");
            printUsage(options);
            return;
        } else if (args.length == 1) {
            tool.setMainClass(args[0]);
        }
        tool.setLog(new ConsoleTeaVMToolLog());
        tool.getProperties().putAll(System.getProperties());

        try {
            tool.generate();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-2);
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + TeaVMRunner.class.getName() + " [OPTIONS] [qualified.main.Class]", options);
        System.exit(-1);
    }
}
