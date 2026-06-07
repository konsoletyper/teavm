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
package org.teavm.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class CRunStrategy implements TestRunStrategy {
    private String compilerCommand;
    private String wrapperCommand;
    private ConcurrentMap<String, Compilation> compilationMap = new ConcurrentHashMap<>();

    CRunStrategy(String compilerCommand, String wrapperCommand) {
        this.compilerCommand = compilerCommand;
        this.wrapperCommand = wrapperCommand;
    }

    @Override
    public void runTest(TestRun run) throws IOException {
        try {
            String exeName = "run_test";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                exeName += ".exe";
            }

            var sourcesDir = new File(run.getGroup().getBaseDirectory(), run.getGroup().getFileName());
            var outputFile = new File(sourcesDir, exeName);
            var compilerSuccess = compile(sourcesDir);
            if (!compilerSuccess) {
                throw new RuntimeException("C compiler error");
            }

            outputFile.setExecutable(true);
            boolean passed;
            synchronized (this) {
                List<String> runCommand = new ArrayList<>();
                if (wrapperCommand != null) {
                    runCommand.addAll(List.of(wrapperCommand.split(" ")));
                }
                runCommand.add(outputFile.getPath());
                if (run.getArgument() != null) {
                    runCommand.add(run.getArgument());
                }
                passed = new ProcessBuilder(runCommand.toArray(new String[0]))
                        .inheritIO()
                        .start()
                        .waitFor() == 0;
            }
            if (!passed) {
                throw new RuntimeException("Test failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean compile(File inputDir) throws IOException, InterruptedException {
        Compilation compilation = compilationMap.computeIfAbsent(inputDir.getPath(), k -> new Compilation());
        synchronized (compilation) {
            if (!compilation.started) {
                compilation.started = true;
                compilation.success = doCompile(inputDir);
            }
        }
        return compilation.success;
    }

    private boolean doCompile(File inputDir) throws IOException, InterruptedException {
        String command = new File(compilerCommand).getAbsolutePath();
        var process = new ProcessBuilder(command)
                .directory(inputDir)
                .inheritIO()
                .start();
        return process.waitFor() == 0;
    }

    @Override
    public void cleanup() {
    }

    static class Compilation {
        volatile boolean started;
        volatile boolean success;
    }
}
