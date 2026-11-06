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
package org.teavm.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.dependency.DependencyTestPatcher;
import org.teavm.diagnostics.Problem;
import org.teavm.jso.JSBody;
import org.teavm.model.MethodReference;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

public class JSOTest {

    @Test
    public void reportsAboutWrongNonStaticJSBody() {
        Problem foundProblem = build("callWrongNonStaticJSBody").stream().filter(problem -> {
            return problem.getLocation().getMethod().getName().equals("callWrongNonStaticJSBody")
                    && problem.getText().equals("Method {{m0}} is not a proper native JavaScript method "
                        + "declaration. It is non-static and declared on a non-overlay class {{c1}}");
        }).findAny().orElse(null);

        assertNotNull(foundProblem);
        Object[] params = foundProblem.getParams();
        assertEquals(new MethodReference(JSOTest.class, "wrongNonStaticJSBody", void.class), params[0]);
        assertEquals(JSOTest.class.getName(), params[1]);
    }

    private static void callWrongNonStaticJSBody() {
        new JSOTest().wrongNonStaticJSBody();
    }

    @JSBody(script = "alert(this.toString());")
    private native void wrongNonStaticJSBody();

    private List<Problem> build(String methodName) {
        TeaVM vm = new TeaVMBuilder(new JavaScriptTarget()).build();
        vm.add(new DependencyTestPatcher(JSOTest.class.getName(), methodName));
        vm.installPlugins();
        vm.setEntryPoint(JSOTest.class.getName());
        vm.build(name -> new ByteArrayOutputStream(), "tmp");
        return vm.getProblemProvider().getSevereProblems();
    }

    public static class A {
    }
}
