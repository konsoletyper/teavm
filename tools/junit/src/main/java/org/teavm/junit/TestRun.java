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

import java.lang.reflect.Method;

class TestRun {
    TestRunGroup group;
    private String name;
    private Method method;
    private String argument;
    private TestPlatform testPlatform;
    private TeaVMTestConfiguration<?> configuration;

    public TestRun(String name, Method method, String argument, TestPlatform testPlatform,
            TeaVMTestConfiguration<?> configuration) {
        this.name = name;
        this.method = method;
        this.argument = argument;
        this.testPlatform = testPlatform;
        this.configuration = configuration;
    }

    TestRunGroup getGroup() {
        return group;
    }

    String getName() {
        return name;
    }

    public Method getMethod() {
        return method;
    }

    String getArgument() {
        return argument;
    }

    TestPlatform getPlatform() {
        return testPlatform;
    }

    TeaVMTestConfiguration<?> getConfiguration() {
        return configuration;
    }
}
