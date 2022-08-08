/*
 *  Copyright 2022 TeaVM Contributors.
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

import static org.teavm.junit.CRunStrategy.mergeLines;
import static org.teavm.junit.CRunStrategy.runProcess;
import static org.teavm.junit.CRunStrategy.writeLines;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class WasiRunStrategy implements TestRunStrategy {
    private final String runtime;

    WasiRunStrategy(String runtime) {
        this.runtime = runtime;
    }

    @Override
    public void beforeAll() {
    }

    @Override
    public void afterAll() {
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
            List<String> runtimeOutput = new ArrayList<>();
            List<String> stdout = new ArrayList<>();
            List<String> runCommand = new ArrayList<>();
            runCommand.add(runtime);
            runCommand.add(new File(run.getBaseDirectory(), run.getFileName()).getAbsoluteFile().toString());
            if (run.getArgument() != null) {
                runCommand.add(run.getArgument());
            }

            runProcess(new ProcessBuilder(runCommand.toArray(new String[0])).start(), runtimeOutput, stdout);

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
}
