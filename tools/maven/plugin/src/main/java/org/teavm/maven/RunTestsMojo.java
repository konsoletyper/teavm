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
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
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

    @Parameter(defaultValue = "${project.build.directory}/teavm-test-report.json")
    private File reportFile;

    @Parameter
    private String seleniumURL;

    @Parameter
    private int numThreads = 1;

    @Parameter
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Tests run skipped as specified by skip property");
            return;
        }

        if (System.getProperty("maven.test.skip", "false").equals("true")
                || System.getProperty("skipTests") != null) {
            getLog().info("Tests run skipped as specified by system property");
            return;
        }

        TestRunner runner = new TestRunner(pickStrategy());
        runner.setLog(getLog());
        runner.setNumThreads(numThreads);

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

    private TestRunStrategy pickStrategy() throws MojoFailureException {
        if (seleniumURL != null) {
            try {
                return new SeleniumRunStrategy(new URL(seleniumURL), testDirectory);
            } catch (MalformedURLException e) {
                throw new MojoFailureException("Can't parse URL: " + seleniumURL, e);
            }
        } else {
            return new HtmlUnitRunStrategy(testDirectory);
        }
    }

    private void processReport(TestReport report) throws MojoExecutionException, MojoFailureException {
        if (report.getResults().isEmpty()) {
            getLog().info("No tests ran");
            return;
        }

        int failedTests = 0;
        for (TestResult result : report.getResults()) {
            if (result.getStatus() != TestStatus.PASSED) {
                failedTests++;
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8")) {
            mapper.writeValue(writer, report);
        } catch (IOException e) {
            throw new MojoFailureException("Error writing test report", e);
        }

        if (failedTests > 0) {
            throw new MojoExecutionException(failedTests + " of " + report.getResults().size() + " test(s) failed");
        } else {
            getLog().info("All of " + report.getResults().size() + " tests successfully passed");
        }
    }
}
