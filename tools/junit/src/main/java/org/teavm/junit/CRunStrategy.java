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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class CRunStrategy implements TestRunStrategy {
    private String compilerCommand;

    CRunStrategy(String compilerCommand) {
        this.compilerCommand = compilerCommand;
    }

    @Override
    public void beforeThread() {
    }

    @Override
    public void afterThread() {
    }

    @Override
    public void runTest(TestRun run) throws IOException {
        try {
            File inputFile = new File(run.getBaseDirectory(), run.getFileName());
            String exeName = run.getFileName();
            if (exeName.endsWith(".c")) {
                exeName = exeName.substring(0, exeName.length() - 2);
            }
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                exeName += ".exe";
            } else {
                exeName += ".out";
            }

            File outputFile = new File(run.getBaseDirectory(), exeName);
            List<String> compilerOutput = new ArrayList<>();
            boolean compilerSuccess = runCompiler(inputFile, outputFile, compilerOutput);
            if (!compilerSuccess) {
                run.getCallback().error(new RuntimeException("C compiler error:\n" + mergeLines(compilerOutput)));
                return;
            }
            writeLines(compilerOutput);

            List<String> runtimeOutput = new ArrayList<>();
            outputFile.setExecutable(true);
            runProcess(new ProcessBuilder(outputFile.getPath()).start(), runtimeOutput);
            if (!runtimeOutput.isEmpty() && runtimeOutput.get(runtimeOutput.size() - 1).equals("SUCCESS")) {
                writeLines(runtimeOutput.subList(0, runtimeOutput.size() - 1));
                run.getCallback().complete();
            } else {
                run.getCallback().error(new RuntimeException("Test failed:\n" + mergeLines(runtimeOutput)));
            }
        } catch (InterruptedException e) {
            run.getCallback().complete();
        }
    }

    private String mergeLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void writeLines(List<String> lines) {
        for (String line : lines) {
            System.out.println(line);
        }
    }

    private boolean runCompiler(File inputFile, File outputFile, List<String> output)
            throws IOException, InterruptedException {
        String[] parts = compilerCommand.split(" +");
        for (int i = 0; i < parts.length; ++i) {
            parts[i] = parts[i].replace("@IN", inputFile.getPath()).replace("@OUT", outputFile.getPath());
        }
        return runProcess(new ProcessBuilder(parts).start(), output);
    }

    private boolean runProcess(Process process, List<String> output) throws IOException, InterruptedException {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        while (true) {
            String line = stderr.readLine();
            if (line == null) {
                break;
            }
            output.add(line);
        }
        while (true) {
            String line = stdin.readLine();
            if (line == null) {
                break;
            }
            output.add(line);
        }

        return process.waitFor() == 0;
    }
}
