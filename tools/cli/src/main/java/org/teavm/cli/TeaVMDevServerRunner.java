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
package org.teavm.cli;

import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.teavm.devserver.DevServer;
import org.teavm.tooling.ConsoleTeaVMToolLog;

public final class TeaVMDevServerRunner {
    private static Options options = new Options();
    private DevServer devServer;
    private CommandLine commandLine;

    static {
        setupOptions();
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private static void setupOptions() {
        options.addOption(OptionBuilder
                .withArgName("directory")
                .hasArg()
                .withDescription("a directory, relative to server's root, which serves generated files")
                .withLongOpt("targetdir")
                .create('d'));
        options.addOption(OptionBuilder
                .withArgName("file")
                .hasArg()
                .withDescription("a file where to put decompiled classes (classes.js by default)")
                .withLongOpt("targetfile")
                .create('f'));
        options.addOption(OptionBuilder
                .withArgName("classpath")
                .hasArgs()
                .withDescription("classpath element (either directory or jar file)")
                .withLongOpt("classpath")
                .create('p'));
        options.addOption(OptionBuilder
                .withArgName("sourcepath")
                .hasArg()
                .withDescription("source path (either directory or jar file which contains source code)")
                .withLongOpt("sourcepath")
                .create('s'));
        options.addOption(OptionBuilder
                .withArgName("number")
                .hasArg()
                .withDescription("port (default is 9090)")
                .withLongOpt("port")
                .create());
        options.addOption(OptionBuilder
                .withDescription("display indicator on web page")
                .withLongOpt("indicator")
                .create('i'));
        options.addOption(OptionBuilder
                .withDescription("deobfuscate stack traces")
                .withLongOpt("deobfuscate-stack")
                .create());
        options.addOption(OptionBuilder
                .withDescription("automatically reload page when compilation completes")
                .withLongOpt("auto-reload")
                .create());
        options.addOption(OptionBuilder
                .withDescription("display more messages on server log")
                .withLongOpt("verbose")
                .create('v'));
        options.addOption(OptionBuilder
                .withArgName("URL")
                .hasArg()
                .withDescription("delegate requests to URL")
                .withLongOpt("proxy-url")
                .create());
        options.addOption(OptionBuilder
                .withArgName("path")
                .hasArg()
                .withDescription("delegate requests from path")
                .withLongOpt("proxy-path")
                .create());
    }

    private TeaVMDevServerRunner(CommandLine commandLine) {
        this.commandLine = commandLine;
        devServer = new DevServer();
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

        TeaVMDevServerRunner runner = new TeaVMDevServerRunner(commandLine);
        runner.parseArguments();
        runner.runAll();
    }

    private void parseArguments() {
        parseClassPathOptions();
        parseSourcePathOptions();
        parseOutputOptions();

        devServer.setIndicator(commandLine.hasOption("indicator"));
        devServer.setDeobfuscateStack(commandLine.hasOption("deobfuscate-stack"));
        devServer.setReloadedAutomatically(commandLine.hasOption("auto-reload"));
        devServer.setLog(new ConsoleTeaVMToolLog(commandLine.hasOption('v')));
        if (commandLine.hasOption("port")) {
            try {
                devServer.setPort(Integer.parseInt(commandLine.getOptionValue("port")));
            } catch (NumberFormatException e) {
                System.err.println("port must be numeric");
                printUsage();
            }
        }

        if (commandLine.hasOption("proxy-url")) {
            devServer.setProxyUrl(commandLine.getOptionValue("proxy-url"));
        }
        if (commandLine.hasOption("proxy-path")) {
            devServer.setProxyPath(commandLine.getOptionValue("proxy-path"));
        }

        String[] args = commandLine.getArgs();
        if (args.length != 1) {
            System.err.println("Unexpected arguments");
            printUsage();
        } else {
            devServer.setMainClass(args[0]);
        }
    }

    private void parseOutputOptions() {
        if (commandLine.hasOption("d")) {
            devServer.setPathToFile(commandLine.getOptionValue("d"));
        }
        if (commandLine.hasOption("f")) {
            devServer.setFileName(commandLine.getOptionValue("f"));
        }
    }

    private void parseClassPathOptions() {
        if (commandLine.hasOption('p')) {
            devServer.setClassPath(commandLine.getOptionValues('p'));
        }
    }

    private void parseSourcePathOptions() {
        if (commandLine.hasOption('s')) {
            devServer.getSourcePath().addAll(Arrays.asList(commandLine.getOptionValues('s')));
        }
    }

    private void runAll() {
        devServer.start();
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + TeaVMDevServerRunner.class.getName() + " [OPTIONS] [qualified.main.Class]",
                options);
        System.exit(-1);
    }
}
