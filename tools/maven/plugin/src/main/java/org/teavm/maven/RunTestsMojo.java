/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.teavm.tooling.testing.TestPlan;

/**
 *
 * @author Alexey Andreev
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class RunTestsMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.directory}/javascript-test")
    private File testDirectory;

    @Parameter
    private URL seleniumURL;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        SeleniumTestRunner runner = new SeleniumTestRunner();
        runner.setLog(getLog());
        runner.setDirectory(testDirectory);
        runner.setUrl(seleniumURL);

        TestPlan plan;
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(testDirectory, "plan.json");
        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            plan = mapper.readValue(reader, TestPlan.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading test plan", e);
        }

        runner.run(plan);
        processReport(runner.getReport());
    }

    private void processReport(List<TestResult> report) throws MojoExecutionException {
        if (report.isEmpty()) {
            getLog().info("No tests ran");
            return;
        }

        int failedTests = 0;
        for (TestResult result : report) {
            if (result.getStatus() != TestStatus.PASSED) {
                failedTests++;
            }
        }

        if (failedTests > 0) {
            throw new MojoExecutionException(failedTests + " of " + report.size() + " test(s) failed");
        } else {
            getLog().info("All of " + report.size() + " tests successfully passed");
        }
    }
}
