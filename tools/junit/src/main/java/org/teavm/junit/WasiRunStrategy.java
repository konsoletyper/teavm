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

class WasiRunStrategy implements TestRunStrategy {
    private String runCommand;

    WasiRunStrategy(String runCommand) {
        this.runCommand = runCommand;
    }

    @Override
    public void runTest(TestRun run) throws IOException {
        try {
            List<String> commandLine = new ArrayList<>();
            commandLine.add(this.runCommand);
            commandLine.add(new File(run.getBaseDirectory(), run.getFileName()).getAbsolutePath());
            if (run.getArgument() != null) {
                commandLine.add(run.getArgument());
            }
            List<String> runtimeOutput = new ArrayList<>();
            List<String> stdout = new ArrayList<>();
            synchronized (this) {
                runProcess(new ProcessBuilder(commandLine.toArray(new String[0])).start(), runtimeOutput, stdout);
            }
            if (!stdout.isEmpty() && stdout.get(stdout.size() - 1).equals("SUCCESS")) {
                writeLines(runtimeOutput);
            } else {
                throw new RuntimeException("Test failed:\n" + mergeLines(runtimeOutput));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
