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
package org.teavm.backend.lowlevel.transform.test;

import java.util.Set;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.backend.lowlevel.transform.CoroutineTransformation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.MethodReference;
import org.teavm.model.MutableClassHolderSource;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.text.ListingBuilder;

public class CoroutineTransformationTest {
    private static final String PREFIX = "backend/lowlevel/coroutine-transformation/";
    private static final MethodReference ASYNC_METHOD = new MethodReference("Foo", "suspend", ValueType.VOID);
    private static final MethodReference INT_TO_FLOAT = method(ValueType.INTEGER, ValueType.FLOAT);
    private static final MethodReference INT_TO_VOID = method(ValueType.INTEGER, ValueType.VOID);
    private static final MethodReference NO_ARGS = method(ValueType.VOID);
    @Rule
    public TestName name = new TestName();

    @Test
    public void arrayRestoredFromFiber() {
        doTest(INT_TO_FLOAT);
    }

    @Test
    public void exactTypes() {
        doTest(INT_TO_VOID);
    }

    @Test
    public void classMerges() {
        doTest(INT_TO_VOID);
    }

    @Test
    public void arrayMerges() {
        doTest(INT_TO_VOID);
    }

    @Test
    public void missingMetadata() {
        doTest(NO_ARGS);
    }

    @Test
    public void nullValues() {
        doTest(method(ValueType.FLOAT));
    }

    @Test
    public void parametersAndThis() {
        doTest(method(ValueType.object("Itf"), ValueType.VOID));
    }

    @Test
    public void exceptions() {
        doTest(NO_ARGS);
    }

    @Test
    public void propagation() {
        doTest(NO_ARGS);
    }

    @Test
    public void hoistedUnwrap() {
        doTest(INT_TO_FLOAT);
    }

    @Test
    public void twoSuspensions() {
        doTest(NO_ARGS);
    }

    private static MethodReference method(ValueType... signature) {
        return new MethodReference("TestClass", "testMethod", signature);
    }

    private void doTest(MethodReference method) {
        String originalPath = PREFIX + name.getMethodName() + ".original.txt";
        String expectedPath = PREFIX + name.getMethodName() + ".expected.txt";
        Program original = ListingParseUtils.parseFromResource(originalPath);
        Program expected = ListingParseUtils.parseFromResource(expectedPath);

        new CoroutineTransformation(createClassSource(), Set.of(ASYNC_METHOD), false).apply(original, method);

        String originalText = new ListingBuilder().buildListing(original, "");
        String expectedText = new ListingBuilder().buildListing(expected, "");
        Assert.assertEquals(expectedText, originalText);
    }

    private static MutableClassHolderSource createClassSource() {
        var source = new MutableClassHolderSource();
        addClass(source, "java.lang.Object", null);
        addClass(source, "java.lang.String", "java.lang.Object");
        addClass(source, "java.lang.Integer", "java.lang.Object");
        addClass(source, "TestClass", "java.lang.Object");
        addClass(source, "Base", "java.lang.Object");
        addClass(source, "Sub", "Base");
        addClass(source, "Left", "Base");
        addClass(source, "Right", "Base");
        addClass(source, "OtherA", "java.lang.Object");
        addClass(source, "OtherB", "java.lang.Object");
        addClass(source, "Itf", "java.lang.Object").getModifiers().add(ElementModifier.INTERFACE);
        addClass(source, "Impl", "java.lang.Object").getInterfaces().add("Itf");
        addClass(source, "MissingChild", "MissingParent");
        return source;
    }

    private static ClassHolder addClass(MutableClassHolderSource source, String name, String parent) {
        var cls = new ClassHolder(name);
        cls.setParent(parent);
        source.putClassHolder(cls);
        return cls;
    }
}
