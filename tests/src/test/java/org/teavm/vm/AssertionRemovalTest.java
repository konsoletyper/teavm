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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.util.ProgramUtils;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;

public class AssertionRemovalTest {
    private static final MethodReference DESIRED_ASSERTION_STATUS = new MethodReference(Class.class,
            "desiredAssertionStatus", boolean.class);

    @Test
    public void removedSimple() {
        var stats = compile(TeaVMOptimizationLevel.SIMPLE, true);
        assertEquals(0, stats.fieldReads, stats.toString());
        assertEquals(0, stats.statusCalls, stats.toString());
    }

    @Test
    public void removedOptimized() {
        var stats = compile(TeaVMOptimizationLevel.FULL, true);
        assertEquals(0, stats.fieldReads, stats.toString());
        assertEquals(0, stats.statusCalls, stats.toString());
        assertEquals(0, stats.errorConstructions, stats.toString());
    }

    @Test
    public void keptSimple() {
        var stats = compile(TeaVMOptimizationLevel.SIMPLE, false);
        assertTrue(stats.fieldReads > 0, stats.toString());
        assertTrue(stats.errorConstructions > 0, stats.toString());
    }

    @Test
    public void keptOptimized() {
        var stats = compile(TeaVMOptimizationLevel.FULL, false);
        assertTrue(stats.fieldReads > 0, stats.toString());
        assertTrue(stats.errorConstructions > 0, stats.toString());
    }

    private Stats compile(TeaVMOptimizationLevel level, boolean removeAssertions) {
        var classLoader = AssertionRemovalTest.class.getClassLoader();
        var vm = new TeaVMBuilder(new JavaScriptTarget())
                .setClassLoader(classLoader)
                .setClassSource(new ClasspathClassHolderSource(new ReferenceCache()))
                .setResourceProvider(new ClasspathResourceProvider(classLoader))
                .build();
        vm.setOptimizationLevel(level);
        vm.setAssertionsRemoved(removeAssertions);
        vm.installPlugins();
        vm.setEntryPoint(Data.class.getName());
        vm.build(fileName -> new ByteArrayOutputStream(), "out");
        if (!vm.getProblemProvider().getSevereProblems().isEmpty()) {
            fail(vm.getProblemProvider().getSevereProblems().get(0).getText());
        }

        var stats = new Stats();
        var classes = vm.getWrittenClasses();
        for (var className : new String[] { Data.class.getName(), Data.Nested.class.getName() }) {
            var cls = classes.get(className);
            for (var method : cls.getMethods()) {
                if (method.getProgram() == null) {
                    continue;
                }
                var program = ProgramUtils.copy(method.getProgram());
                for (var i = 0; i < program.basicBlockCount(); ++i) {
                    for (var insn : program.basicBlockAt(i)) {
                        if (insn instanceof GetFieldInstruction) {
                            if (((GetFieldInstruction) insn).getField().getFieldName().equals("$assertionsDisabled")) {
                                stats.fieldReads++;
                            }
                        } else if (insn instanceof InvokeInstruction) {
                            if (((InvokeInstruction) insn).getMethod().equals(DESIRED_ASSERTION_STATUS)) {
                                stats.statusCalls++;
                            }
                        } else if (insn instanceof ConstructInstruction) {
                            if (((ConstructInstruction) insn).getType().equals(AssertionError.class.getName())) {
                                stats.errorConstructions++;
                            }
                        }
                    }
                }
            }
        }
        return stats;
    }

    static class Stats {
        int fieldReads;
        int statusCalls;
        int errorConstructions;

        @Override
        public String toString() {
            return "fieldReads=" + fieldReads + ", statusCalls=" + statusCalls
                    + ", errorConstructions=" + errorConstructions;
        }
    }

    public static class Data {
        private static int counter;

        public static void main(String[] args) {
            System.out.println(check(args.length));
            System.out.println(Nested.check(args.length));
            assert increment() > 100 : "message";
            System.out.println(counter);
        }

        private static int check(int value) {
            assert value < 0;
            return value + 1;
        }

        private static int increment() {
            return ++counter;
        }

        static class Nested {
            static int check(int value) {
                assert value > 10 : "nested";
                return value * 2;
            }
        }
    }
}
