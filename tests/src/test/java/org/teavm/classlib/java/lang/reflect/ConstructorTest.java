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
package org.teavm.classlib.java.lang.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Reflectable;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
public class ConstructorTest {
    @Test
    public void constructorsEnumerated() {
        callConstructors();

        assertEquals(""
                        + "ConstructorTest$ReflectableType(int,java.lang.Object);"
                        + "ConstructorTest$ReflectableType(java.lang.String);"
                        + "protected ConstructorTest$ReflectableType();"
                        + "public ConstructorTest$ReflectableType(int);",
                collectConstructors(ReflectableType.class.getDeclaredConstructors()));
    }

    @Test
    public void publicConstructorsEnumerated() {
        callConstructors();

        assertEquals("public ConstructorTest$ReflectableType(int);",
                collectConstructors(ReflectableType.class.getConstructors()));
    }

    private void callConstructors() {
        new ReflectableType();
        new ReflectableType(1);
        new ReflectableType("2");
        new ReflectableType(1, "2");
    }

    private String collectConstructors(Constructor<?>[] constructors) {
        List<String> lines = new ArrayList<>();
        for (Constructor<?> constructor : constructors) {
            lines.add(constructor + ";");
        }
        StringBuilder sb = new StringBuilder();
        Collections.sort(lines);
        for (String line : lines) {
            sb.append(line);
        }
        return sb.toString().replace("org.teavm.classlib.java.lang.reflect.", "");
    }

    @Test
    public void newInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        Constructor<ReflectableType> constructor = ReflectableType.class.getDeclaredConstructor();
        ReflectableType instance = constructor.newInstance();
        assertEquals(0, instance.getA());
        assertNull(instance.getB());
    }

    @Test
    public void newInstance2() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        Constructor<ReflectableType> constructor = ReflectableType.class
                .getDeclaredConstructor(int.class, Object.class);
        ReflectableType instance = constructor.newInstance(23, "42");
        assertEquals(23, instance.getA());
        assertEquals("42", instance.getB());
    }

    static class ReflectableType {
        public int a;
        public Object b;

        @Reflectable protected ReflectableType() {
        }

        @Reflectable public ReflectableType(int a) {
            this.a = a;
        }

        @Reflectable ReflectableType(String b) {
            this.b = b;
        }

        @Reflectable ReflectableType(int a, Object b) {
            this.a = a;
            this.b = b;
        }

        public int getA() {
            return a;
        }

        public Object getB() {
            return b;
        }
    }
}
