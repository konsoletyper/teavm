/*
 *  Copyright 2025 Alexey Andreev.
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

package org.teavm.buildutil;

import java.io.IOException;
import java.nio.file.Files;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

public class MacRemoteTestPlugin implements Plugin<Project> {
    private static final String CLASS_LIST_FILE_PROPERTY = "teavm.junit.c.classListFile";
    private static final String TESTS_C_OUTPUT = "teavm-tests/c";
    private static final String CLASS_LIST_FILENAME = "teavm-c-test-classes.txt";

    @Override
    public void apply(Project project) {
        var classListFile = project.getLayout().getBuildDirectory().file(CLASS_LIST_FILENAME);

        var prepareTask = project.getTasks().register("prepareCTestList", task -> {
            task.getOutputs().file(classListFile);
            task.doLast(t -> {
                var file = classListFile.get().getAsFile();
                file.getParentFile().mkdirs();
                try {
                    Files.write(file.toPath(), new byte[0]);
                } catch (IOException e) {
                    throw new GradleException("Failed to truncate C test class list file", e);
                }
            });
        });

        project.getTasks().named("test", Test.class, test -> {
            test.dependsOn(prepareTask);
            test.getOutputs().dir(project.getLayout().getBuildDirectory().dir(TESTS_C_OUTPUT));
            test.systemProperty(CLASS_LIST_FILE_PROPERTY,
                    classListFile.get().getAsFile().getAbsolutePath());
            var macTests = project.getProviders().gradleProperty("teavm.mac.tests");
            if (macTests.isPresent()) {
                test.filter(f -> f.includeTestsMatching(macTests.get()));
            }
        });

        project.getTasks().register("runMacRemoteTests", RunMacRemoteTestsTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Runs C backend tests on a remote Mac via SSH.");
            task.dependsOn(project.getTasks().named("test"));
            task.getMacHost().set(project.getProviders().gradleProperty("teavm.mac.host"));
            task.getMacUser().set(project.getProviders().gradleProperty("teavm.mac.login"));
            task.getMacPassword().set(project.getProviders().gradleProperty("teavm.mac.password"));
            task.getTestFilter().set(project.getProviders().gradleProperty("teavm.mac.tests"));
            task.getClassListFile().set(classListFile);
            task.getCTestOutputDir().set(
                    project.getLayout().getBuildDirectory().dir(TESTS_C_OUTPUT));
        });
    }
}
