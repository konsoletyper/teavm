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
package org.teavm.dependency;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.model.MethodReference;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

public class ClassValueTest {
    @Test
    public void simple() {
        ValueDependencyInfo info = runTestWithConsume("simpleSnippet").getClassValueNode();
        assertTrue("Long must be consumed", info.hasType("java.lang.Long"));
        assertTrue("String must be consumed", info.hasType("java.lang.String"));
        assertTrue("Nothing except Long and String expected", info.getTypes().length == 2);
    }

    @SuppressWarnings("unused")
    public static void simpleSnippet() {
        consume(Long.class);
        consume(String.class);
    }

    @Test
    public void fromGetClass() {
        ValueDependencyInfo info = runTestWithConsume("fromGetClassSnippet").getClassValueNode();
        assertTrue("Long must be consumed", info.hasType("java.lang.Long"));
        assertTrue("String must be consumed", info.hasType("java.lang.String"));
        assertTrue("Nothing except Long and String expected", info.getTypes().length == 2);
    }

    @SuppressWarnings("unused")
    public static void fromGetClassSnippet() {
        consumeClass(23L);
        consumeClass("foo");
    }

    private static void consumeClass(Object value) {
        consume(value.getClass());
    }

    protected static void consume(@SuppressWarnings("unused") Object value) {
        // do nothing
    }

    private ValueDependencyInfo runTestWithConsume(String methodName) {
        DependencyInfo info = runTest(methodName);
        MethodDependencyInfo methodInfo = info.getMethod(new MethodReference(ClassValueTest.class, "consume",
                Object.class, void.class));
        if (methodInfo == null) {
            fail("consume method never reached");
        }
        return methodInfo.getVariable(1);
    }

    private DependencyInfo runTest(String methodName) {
        JavaScriptTarget target = new JavaScriptTarget();
        target.setStrict(true);
        TeaVM vm = new TeaVMBuilder(target).build();
        vm.add(new DependencyTestPatcher(getClass().getName(), methodName));
        vm.installPlugins();
        vm.entryPoint(getClass().getName());
        vm.build(fileName -> new ByteArrayOutputStream(), "tmp");
        if (!vm.getProblemProvider().getSevereProblems().isEmpty()) {
            fail("Code compiled with errors:\n" + describeProblems(vm));
        }
        return vm.getDependencyInfo();
    }

    private String describeProblems(TeaVM vm) {
        Log log = new Log();
        TeaVMProblemRenderer.describeProblems(vm, log);
        return log.sb.toString();
    }


    static class Log implements TeaVMToolLog {
        StringBuilder sb = new StringBuilder();

        @Override
        public void info(String text) {
            appendLine(text);
        }

        @Override
        public void debug(String text) {
            appendLine(text);
        }

        @Override
        public void warning(String text) {
            appendLine(text);
        }

        @Override
        public void error(String text) {
            appendLine(text);
        }

        @Override
        public void info(String text, Throwable e) {
            appendLine(text);
        }

        @Override
        public void debug(String text, Throwable e) {
            appendLine(text);
        }

        @Override
        public void warning(String text, Throwable e) {
            appendLine(text);
        }

        @Override
        public void error(String text, Throwable e) {
            appendLine(text);
        }

        private void appendLine(String text) {
            sb.append(text).append('\n');
        }
    }
}
