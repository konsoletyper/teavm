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
import java.util.concurrent.ConcurrentLinkedQueue;

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
            String exeName = "run_test";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                exeName += ".exe";
            }

            File outputFile = new File(run.getBaseDirectory(), exeName);
            List<String> compilerOutput = new ArrayList<>();
            boolean compilerSuccess = runCompiler(run.getBaseDirectory(), compilerOutput);
            if (!compilerSuccess) {
                run.getCallback().error(new RuntimeException("C compiler error:\n" + mergeLines(compilerOutput)));
                return;
            }
            writeLines(compilerOutput);

            List<String> runtimeOutput = new ArrayList<>();
            List<String> stdout = new ArrayList<>();
            outputFile.setExecutable(true);
            runProcess(new ProcessBuilder(outputFile.getPath()).start(), runtimeOutput, stdout);
            if (!stdout.isEmpty() && stdout.get(stdout.size() - 1).equals("SUCCESS")) {
                writeLines(runtimeOutput);
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

    private boolean runCompiler(File inputDir, List<String> output)
            throws IOException, InterruptedException {
        String command = new File(compilerCommand).getAbsolutePath();
        return runProcess(new ProcessBuilder(command).directory(inputDir).start(), output, new ArrayList<>());
    }

    private boolean runProcess(Process process, List<String> output, List<String> stdout) throws InterruptedException {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        ConcurrentLinkedQueue<String> lines = new ConcurrentLinkedQueue<>();

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String line = stderr.readLine();
                    if (line == null) {
                        break;
                    }
                    lines.add(line);
                }
            } catch (IOException e) {
                // do nothing
            }
        });
        thread.setDaemon(true);
        thread.start();

        try {
            while (true) {
                String line = stdin.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
                stdout.add(line);
                if (lines.size() > 10000) {
                    output.addAll(lines);
                    process.destroy();
                    return false;
                }
            }
        } catch (IOException e) {
            // do nothing
        }

        boolean result = process.waitFor() == 0;
        output.addAll(lines);
        return result;
    }
}
