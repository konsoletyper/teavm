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
package org.teavm.tooling.builder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.teavm.tooling.TeaVMTargetType;

public class DaemonBuildStrategyTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void buildsJavaScriptFile() throws Exception {
        var classPathEntries = Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator));

        var strategy = new DaemonBuildStrategy(null, 512, classPathEntries.toArray(new String[0]));
        strategy.init();
        strategy.setClassPathEntries(classPathEntries);
        strategy.setTargetType(TeaVMTargetType.JAVASCRIPT);
        strategy.setMainClass(DaemonBuildStrategyTestApplication.class.getName());
        strategy.setTargetDirectory(temporaryFolder.getRoot().getPath());

        BuildResult result = strategy.build();

        List<RenderedProblem> problems = result.getProblems();
        var problemTexts = problems.stream().map(RenderedProblem::getText).toList();
        assertTrue("Build should not report any problems: " + problemTexts, problems.isEmpty());

        var targetFile = new File(temporaryFolder.getRoot(), "classes.js");
        assertTrue("Expected JS file to be generated at " + targetFile, targetFile.isFile());
        assertTrue("Generated JS file should not be empty", Files.size(targetFile.toPath()) > 0);
    }

    @Test
    public void reportsAbnormalProcessTermination() {
        var classPathEntries = Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator));

        // a heap this small makes the daemon JVM fail to even start, so it exits
        // with a non-zero code without ever reporting a JSON result
        var strategy = new DaemonBuildStrategy(null, 1, classPathEntries.toArray(new String[0]));
        strategy.init();
        strategy.setClassPathEntries(classPathEntries);
        strategy.setTargetType(TeaVMTargetType.JAVASCRIPT);
        strategy.setMainClass(DaemonBuildStrategyTestApplication.class.getName());
        strategy.setTargetDirectory(temporaryFolder.getRoot().getPath());

        try {
            strategy.build();
            fail("Expected build to fail");
        } catch (BuildException e) {
            assertTrue("Expected failure message to mention exit code: " + e.getMessage(),
                    e.getMessage().contains("exit code"));
        }
    }
}
