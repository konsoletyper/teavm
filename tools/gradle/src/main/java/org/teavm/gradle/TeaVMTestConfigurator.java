/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.gradle;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.teavm.gradle.api.TeaVMTests;
import org.teavm.gradle.api.TeaVMWebTestRunner;

class TeaVMTestConfigurator {
    private TeaVMTestConfigurator() {
    }

    static void configure(Project project, TeaVMTests tests) {
        project.getTasks().withType(Test.class).configureEach(test -> {
            test.getSystemProperties().putIfAbsent("teavm.junit.target",
                    new File(project.getBuildDir(), "tests/teavm"));
            test.getSystemProperties().putIfAbsent("teavm.junit.threads", "1");

            test.getSystemProperties().putIfAbsent("teavm.junit.js",
                    tests.getJs().getEnabled().get());
            test.getSystemProperties().putIfAbsent("teavm.junit.js.runner",
                    tests.getJs().getRunner().map(TeaVMTestConfigurator::runnerToString).get());
            test.getSystemProperties().putIfAbsent("teavm.junit.js.decodeStack",
                    tests.getJs().getDecodeStack().get());

            test.getSystemProperties().putIfAbsent("teavm.junit.wasm",
                    tests.getWasm().getEnabled().get());
            test.getSystemProperties().putIfAbsent("teavm.junit.wasm.runner",
                    tests.getWasm().getRunner().map(TeaVMTestConfigurator::runnerToString).get());
        });
    }

    private static String runnerToString(TeaVMWebTestRunner runner) {
        switch (runner) {
            case CHROME:
                return "browser-chrome";
            case FIREFOX:
                return "browser-firefox";
            case CUSTOM_BROWSER:
                return "browser";
            default:
                return "none";
        }
    }
}
