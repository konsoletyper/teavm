/*
 *  Copyright 2016 Alexey Andreev.
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
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.teavm.tooling.testing.TeaVMTestTool;

public class TeaVMTestRunner extends Runner {
    private static final String PATH_PARAM = "teavm.junit.target";
    private Class<?> testClass;
    private Description description;

    public TeaVMTestRunner(Class<?> testClass) {
        this.testClass = testClass;
    }

    @Override
    public Description getDescription() {
        if (description == null) {
            description = Description.createSuiteDescription(testClass);
            for (Method method : testClass.getMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() == void.class
                        && method.isAnnotationPresent(Test.class)) {
                    Description testDescription = Description.createTestDescription(testClass, method.getName());
                    description.addChild(testDescription);
                }
            }
        }
        return description;
    }

    @Override
    public void run(RunNotifier notifier) {
        Description description = getDescription();

        notifier.fireTestStarted(description);
        String targetPath = System.getProperty(PATH_PARAM);
        if (targetPath == null) {
            for (Description testDescription : description.getChildren()) {
                notifier.fireTestIgnored(testDescription);
            }
            notifier.fireTestIgnored(description);
            notifier.fireTestFinished(description);
            return;
        }

        TeaVMTestTool tool = new TeaVMTestTool();
        tool.setTargetDirectory(new File(targetPath, testClass.getName()));
        tool.setMinifying(false);
        tool.getTestClasses().add(testClass.getName());
        boolean success = true;
        try {
            tool.generate();
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(description, e));
            success = false;
        }

        if (success) {
            for (Description testDescription : description.getChildren()) {
                notifier.fireTestStarted(testDescription);
                notifier.fireTestFinished(testDescription);
            }
        }

        notifier.fireTestFinished(description);
    }
}
