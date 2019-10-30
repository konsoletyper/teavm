/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.model.optimization.test;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MutableClassHolderSource;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.optimization.MethodOptimizationContext;
import org.teavm.model.optimization.RepeatedFieldReadElimination;
import org.teavm.model.text.ListingBuilder;
import org.teavm.model.util.ProgramUtils;

public class RepeatedFieldReadEliminationTest {
    private static final String PREFIX = "model/optimization/repeated-field-read-elimination/";
    @Rule
    public TestName name = new TestName();

    @Test
    public void simple() {
        doTest();
    }

    @Test
    public void volatileField() {
        doTest();
    }

    @Test
    public void fieldStoreInvalidates() {
        doTest();
    }

    @Test
    public void fieldStoreInDifferentObjects() {
        doTest();
    }

    @Test
    public void invalidateInOneBranch() {
        doTest();
    }

    @Test
    public void invocationInvalidates() {
        doTest();
    }

    @Test
    public void alwaysInvalidateExternalObject() {
        doTest();
    }

    @Test
    public void updatingExternalObjectInvalidatesAll() {
        doTest();
    }

    @Test
    public void mergeInAliasAnalysis() {
        doTest();
    }

    private void doTest() {
        String originalPath = PREFIX + name.getMethodName() + ".original.txt";
        String expectedPath = PREFIX + name.getMethodName() + ".expected.txt";
        Program original = ListingParseUtils.parseFromResource(originalPath);
        Program expected = ListingParseUtils.parseFromResource(expectedPath);

        performOptimization(original);

        String originalText = new ListingBuilder().buildListing(original, "");
        String expectedText = new ListingBuilder().buildListing(expected, "");
        Assert.assertEquals(expectedText, originalText);
    }

    private void performOptimization(Program program) {
        MutableClassHolderSource classSource = new MutableClassHolderSource();

        ClassHolder testClass = new ClassHolder("TestClass");
        MethodHolder testMethod = new MethodHolder("testMethod", ValueType.VOID);
        testMethod.setProgram(ProgramUtils.copy(program));
        testClass.addMethod(testMethod);

        classSource.putClassHolder(testClass);

        ClassHolder foo = new ClassHolder("Foo");

        FieldHolder intField = new FieldHolder("intField");
        intField.setLevel(AccessLevel.PUBLIC);
        intField.setType(ValueType.INTEGER);
        foo.addField(intField);

        FieldHolder volatileField = new FieldHolder("volatileField");
        volatileField.setLevel(AccessLevel.PUBLIC);
        volatileField.setType(ValueType.INTEGER);
        volatileField.getModifiers().add(ElementModifier.VOLATILE);
        foo.addField(volatileField);

        MethodHolder getFoo = new MethodHolder("getFoo", ValueType.object("Foo"));
        getFoo.getModifiers().add(ElementModifier.STATIC);
        getFoo.getModifiers().add(ElementModifier.NATIVE);
        foo.addMethod(getFoo);

        classSource.putClassHolder(foo);

        MethodOptimizationContext context = new MethodOptimizationContext() {
            @Override
            public MethodReader getMethod() {
                return testMethod;
            }

            @Override
            public DependencyInfo getDependencyInfo() {
                return null;
            }

            @Override
            public ClassReaderSource getClassSource() {
                return classSource;
            }
        };

        new RepeatedFieldReadElimination().optimize(context, program);
    }
}
