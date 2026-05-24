/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.devserver.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.common.json.JsonParser;
import org.teavm.devserver.DevServer;
import org.teavm.tooling.ConsoleTeaVMToolLog;

public final class TeaVMDevServerRunner {
    private static Options options = new Options();
    private DevServer devServer;
    private CommandLine commandLine;
    private JsonCommandWriter jsonWriter;

    static {
        setupOptions();
    }

    private static void setupOptions() {
        options.addOption(Option.builder("d")
                .argName("directory")
                .hasArg()
                .desc("a directory, relative to server's root, which serves generated files")
                .longOpt("targetdir")
                .get());
        options.addOption(Option.builder("f")
                .argName("file")
                .hasArg()
                .desc("a file where to put decompiled classes (classes.js by default)")
                .longOpt("targetfile")
                .get());
        options.addOption(Option.builder("p")
                .argName("classpath")
                .hasArgs()
                .desc("classpath element (either directory or jar file)")
                .longOpt("classpath")
                .get());
        options.addOption(Option.builder("s")
                .argName("sourcepath")
                .hasArgs()
                .desc("source path (either directory or jar file which contains source code)")
                .longOpt("sourcepath")
                .get());
        options.addOption(Option.builder()
                .argName("classnames")
                .hasArgs()
                .desc("list of classes that should be preserved during the build (e.g. to use with reflection)")
                .longOpt("preserved-classes")
                .get());
        options.addOption(Option.builder()
                .valueSeparator()
                .hasArgs()
                .longOpt("property")
                .get());
        options.addOption(Option.builder()
                .argName("module_type")
                .hasArg()
                .longOpt("js-module-type")
                .desc("JS module type (umd, common-js, es2015 or none)")
                .get());
        options.addOption(Option.builder()
                .argName("number")
                .hasArg()
                .desc("port (default is 9090)")
                .longOpt("port")
                .get());
        options.addOption(Option.builder("i")
                .desc("display indicator on web page")
                .longOpt("indicator")
                .get());
        options.addOption(Option.builder()
                .desc("deobfuscate stack traces")
                .longOpt("deobfuscate-stack")
                .get());
        options.addOption(Option.builder()
                .desc("automatically reload page when compilation completes")
                .longOpt("auto-reload")
                .get());
        options.addOption(Option.builder("v")
                .desc("display more messages on server log")
                .longOpt("verbose")
                .get());
        options.addOption(Option.builder()
                .argName("URL")
                .hasArg()
                .desc("delegate requests to URL")
                .longOpt("proxy-url")
                .get());
        options.addOption(Option.builder()
                .argName("path")
                .hasArg()
                .desc("delegate requests from path")
                .longOpt("proxy-path")
                .get());
        options.addOption(Option.builder()
                .argName("paths")
                .hasArgs()
                .desc("paths to directories from which static content will be served")
                .longOpt("static-dirs")
                .get());
        options.addOption(Option.builder()
                .argName("path")
                .hasArg()
                .desc("path on the server from which static content will be served")
                .longOpt("static-serve-path")
                .get());
        options.addOption(Option.builder()
                .argName("paths")
                .hasArgs()
                .desc("path to resources which which will be served")
                .longOpt("resources")
                .get());
        options.addOption(Option.builder()
                .hasArg()
                .desc("path on the server from which resources will be served")
                .longOpt("resources-serve-path")
                .get());
        options.addOption(Option.builder()
                .desc("don't watch file system changes")
                .longOpt("no-watch")
                .get());
        options.addOption(Option.builder()
                .desc("JSON interface over stdout")
                .longOpt("json-interface")
                .get());
        options.addOption(Option.builder()
                .hasArg()
                .argName("port")
                .desc("Debug port")
                .longOpt("debug-port")
                .get());
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
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage();
            return;
        }

        TeaVMDevServerRunner runner = new TeaVMDevServerRunner(commandLine);
        runner.parseArguments();
        runner.devServer.start();
        if (runner.jsonWriter != null) {
            runner.readStdinCommands();
        } else {
            try {
                runner.devServer.awaitServer();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    private void parseArguments() {
        parseClassPathOptions();
        parseSourcePathOptions();
        parseOutputOptions();

        devServer.setIndicator(commandLine.hasOption("indicator"));
        devServer.setDeobfuscateStack(commandLine.hasOption("deobfuscate-stack"));
        devServer.setReloadedAutomatically(commandLine.hasOption("auto-reload"));
        if (commandLine.hasOption("port")) {
            try {
                devServer.setPort(Integer.parseInt(commandLine.getOptionValue("port")));
            } catch (NumberFormatException e) {
                System.err.println("port must be numeric");
                printUsage();
            }
        }

        var properties = commandLine.getOptionProperties("property");
        for (var property : properties.stringPropertyNames()) {
            devServer.getProperties().put(property, properties.getProperty(property));
        }

        if (commandLine.hasOption("preserved-classes")) {
            devServer.getPreservedClasses().addAll(List.of(commandLine.getOptionValues("preserved-classes")));
        }
        if (commandLine.hasOption("js-module-type")) {
            var moduleTypeValue = commandLine.getOptionValue("js-module-type");
            JSModuleType type;
            try {
                type = JSModuleType.valueOf(moduleTypeValue.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid value for --js-module-type: " + moduleTypeValue);
                printUsage();
                type = null;
            }
            devServer.setJsModuleType(type);
        }

        if (commandLine.hasOption("proxy-url")) {
            devServer.setProxyUrl(commandLine.getOptionValue("proxy-url"));
        }
        if (commandLine.hasOption("proxy-path")) {
            devServer.setProxyPath(commandLine.getOptionValue("proxy-path"));
        }
        if (commandLine.hasOption("no-watch")) {
            devServer.setFileSystemWatched(false);
        }
        if (commandLine.hasOption("static-dirs")) {
            for (var dir : commandLine.getOptionValues("static-dirs")) {
                devServer.addStaticFileRoot(dir);
            }
        }
        if (commandLine.hasOption("static-serve-path")) {
            devServer.setStaticFilesPath(commandLine.getOptionValue("static-serve-path"));
        }
        if (commandLine.hasOption("resources")) {
            for (var dir : commandLine.getOptionValues("resources")) {
                devServer.addResourcesRoot(dir);
            }
        }
        if (commandLine.hasOption("resources-serve-path")) {
            devServer.setResourcesPath(commandLine.getOptionValue("resources-serve-path"));
        }
        if (commandLine.hasOption("json-interface")) {
            setupJsonInterface(devServer);
        } else {
            devServer.setLog(new ConsoleTeaVMToolLog(commandLine.hasOption('v')));
        }
        if (commandLine.hasOption("debug-port")) {
            devServer.setDebugPort(Integer.parseInt(commandLine.getOptionValue("debug-port")));
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

    private void setupJsonInterface(DevServer devServer) {
        jsonWriter = new JsonCommandWriter();
        devServer.setLog(jsonWriter);
        devServer.addListener(jsonWriter);
        devServer.setCompileOnStartup(false);
        devServer.setLogBuildErrors(false);
        JsonLoggerFactory.setWriter(jsonWriter);
    }

    private void readStdinCommands() {
        var commandReader = new JsonCommandReader(devServer);
        var parser = JsonParser.ofValue(commandReader);
        try (var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                var command = reader.readLine();
                if (command == null) {
                    break;
                }
                parser.parse(new StringReader(command));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printUsage() {
        var formatter = HelpFormatter.builder().get();
        try {
            formatter.printHelp("java " + TeaVMDevServerRunner.class.getName(),
                    "", options, "", true);
        } catch (IOException e) {
            // Ignore IOException during help printing
        }
        System.exit(-1);
    }
}
