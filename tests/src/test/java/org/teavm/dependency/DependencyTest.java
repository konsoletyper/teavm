/*
 *  Copyright 2017 Alexey Andreev.
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

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.common.DisjointSet;
import org.teavm.diagnostics.Problem;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassInference;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

public class DependencyTest {
    @Rule
    public final TestName testName = new TestName();

    private static ClassHolderSource classSource;

    @BeforeClass
    public static void prepare() {
        classSource = new ClasspathClassHolderSource(DependencyTest.class.getClassLoader());
    }

    @AfterClass
    public static void cleanup() {
        classSource = null;
    }

    @Test
    public void virtualCall() {
        doTest();
    }

    @Test
    public void instanceOf() {
        doTest();
    }

    @Test
    public void catchException() {
        doTest();
    }

    @Test
    public void propagateException() {
        doTest();
    }

    @Test
    public void arrays() {
        doTest();
    }

    @Test
    public void arraysPassed() {
        doTest();
    }

    @Test
    public void arraysRetrieved() {
        doTest();
    }

    private void doTest() {
        TeaVM vm = new TeaVMBuilder(new JavaScriptTarget())
                .setClassLoader(DependencyTest.class.getClassLoader())
                .setClassSource(classSource)
                .build();
        vm.setProgressListener(new TeaVMProgressListener() {
            @Override
            public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
                return phase == TeaVMPhase.DEPENDENCY_ANALYSIS
                        ? TeaVMProgressFeedback.CONTINUE
                        : TeaVMProgressFeedback.CANCEL;
            }

            @Override
            public TeaVMProgressFeedback progressReached(int progress) {
                return TeaVMProgressFeedback.CONTINUE;
            }
        });
        vm.add(new DependencyTestPatcher(DependencyTestData.class.getName(), testName.getMethodName()));
        vm.installPlugins();

        MethodReference testMethod = new MethodReference(DependencyTestData.class,
                testName.getMethodName(), void.class);
        vm.entryPoint(DependencyTestData.class.getName());
        vm.build(fileName -> new ByteArrayOutputStream(), "out");

        List<Problem> problems = vm.getProblemProvider().getSevereProblems();
        if (!problems.isEmpty()) {
            Problem problem = problems.get(0);
            Assert.fail("Error at " + problem.getLocation().getSourceLocation() + ": " + problem.getText());
        }

        MethodHolder method = classSource.get(testMethod.getClassName()).getMethod(testMethod.getDescriptor());
        List<Assertion> assertions = collectAssertions(method);
        processAssertions(assertions, vm.getDependencyInfo().getMethod(testMethod), vm.getDependencyInfo(),
                method.getProgram());
    }

    private void processAssertions(List<Assertion> assertions, MethodDependencyInfo methodDep,
            DependencyInfo dependencyInfo, Program program) {
        ClassInference classInference = new ClassInference(dependencyInfo, new ClassHierarchy(
                dependencyInfo.getClassSource()));
        classInference.infer(program, methodDep.getReference());

        for (Assertion assertion : assertions) {
            ValueDependencyInfo valueDep = methodDep.getVariable(assertion.value);
            String[] actualTypes = valueDep.getTypes();
            String[] expectedTypes = assertion.expectedTypes.clone();
            Arrays.sort(actualTypes);
            Arrays.sort(expectedTypes);
            Assert.assertArrayEquals("Assertion at " + assertion.location, expectedTypes, actualTypes);

            actualTypes = classInference.classesOf(assertion.value);
            Arrays.sort(actualTypes);
            Assert.assertArrayEquals("Assertion at " + assertion.location + " (class inference)",
                    expectedTypes, actualTypes);
        }
    }

    private List<Assertion> collectAssertions(MethodHolder method) {
        Program program = method.getProgram();

        AliasCollector aliasCollector = new AliasCollector(program.variableCount());
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                instruction.acceptVisitor(aliasCollector);
            }
        }
        int[] aliases = aliasCollector.disjointSet.pack(program.variableCount());

        AssertionCollector assertionCollector = new AssertionCollector(aliases);
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                instruction.acceptVisitor(assertionCollector);
            }
        }
        assertionCollector.postProcess();

        return assertionCollector.assertions;
    }

    static class AliasCollector extends AbstractInstructionVisitor {
        DisjointSet disjointSet = new DisjointSet();

        AliasCollector(int variableCount) {
            for (int i = 0; i < variableCount; ++i) {
                disjointSet.create();
            }
        }

        @Override
        public void visit(AssignInstruction insn) {
            disjointSet.union(insn.getReceiver().getIndex(), insn.getAssignee().getIndex());
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            disjointSet.union(insn.getReceiver().getIndex(), insn.getArray().getIndex());
        }
    }

    static class AssertionCollector extends AbstractInstructionVisitor {
        static final MethodReference assertionMethod = new MethodReference(MetaAssertions.class,
                "assertTypes", Object.class, Class[].class, void.class);
        int[] aliases;
        List<Assertion> assertions = new ArrayList<>();
        IntSet[] arrayContent;
        ValueType[] classConstants;

        AssertionCollector(int[] aliases) {
            this.aliases = aliases;
            classConstants = new ValueType[aliases.length];
            arrayContent = new IntSet[aliases.length];
        }

        void postProcess() {
            int[] aliasInstances = new int[aliases.length];
            Arrays.fill(aliasInstances, -1);
            for (int i = 0; i < aliases.length; ++i) {
                int alias = aliases[i];
                if (aliasInstances[alias] < 0) {
                    aliasInstances[alias] = i;
                }
            }

            for (Assertion assertion : assertions) {
                IntSet items = arrayContent[assertion.array];
                if (items != null) {
                    Set<String> expectedClasses = new HashSet<>();
                    for (int item : items.toArray()) {
                        ValueType constant = classConstants[item];
                        if (constant != null) {
                            String expectedClass;
                            if (constant instanceof ValueType.Object) {
                                expectedClass = ((ValueType.Object) constant).getClassName();
                            } else {
                                expectedClass = constant.toString();
                            }
                            expectedClasses.add(expectedClass);
                        }
                    }
                    assertion.expectedTypes = expectedClasses.toArray(new String[0]);
                } else {
                    assertion.expectedTypes = new String[0];
                }

                assertion.value = aliasInstances[assertion.value];
            }
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getMethod().equals(assertionMethod)) {
                Assertion assertion = new Assertion();
                assertion.value = aliases[insn.getArguments().get(0).getIndex()];
                assertion.array = aliases[insn.getArguments().get(1).getIndex()];
                assertion.location = insn.getLocation();
                assertions.add(assertion);
            }
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            classConstants[aliases[insn.getReceiver().getIndex()]] = insn.getConstant();
        }

        @Override
        public void visit(PutElementInstruction insn) {
            int array = aliases[insn.getArray().getIndex()];
            int value = aliases[insn.getValue().getIndex()];
            IntSet items = arrayContent[array];
            if (items == null) {
                items = new IntHashSet();
                arrayContent[array] = items;
            }
            items.add(value);
        }
    }

    private static class Assertion {
        int value;
        int array;
        TextLocation location;
        String[] expectedTypes;
    }
}
