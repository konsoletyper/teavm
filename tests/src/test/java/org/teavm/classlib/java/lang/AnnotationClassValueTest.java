/*
 *  Copyright 2026 konsoletyper.
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
package org.teavm.classlib.java.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * Classes below are named only inside annotation values, never as class literals in code, so the
 * only way their metadata can reach the program is through the annotation. Expected values are
 * therefore string literals.
 */
@TeaVMTest
public class AnnotationClassValueTest {
    private static final String PREFIX = "org.teavm.classlib.java.lang.AnnotationClassValueTest$";

    // Class.getName: "Returns the name of the entity ... represented by this Class object". A class that
    // only an annotation member names must still know its name.
    @Test
    public void names() {
        var a = Annotated.class.getAnnotation(ClassValues.class);
        assertEquals(PREFIX + "ScalarTarget", a.scalar().getName());
        assertEquals(PREFIX + "DefaultTarget", a.byDefault().getName());
        assertEquals(PREFIX + "ElementTarget1", a.list()[0].getName());
        assertEquals(PREFIX + "ElementTarget2", a.list()[1].getName());
        assertEquals("[L" + PREFIX + "ArrayTarget;", a.arrayClass().getName());
        assertEquals("int", a.primitive().getName());
        assertEquals(PREFIX + "NestedTarget", a.nested().value().getName());
    }

    @Test
    public void simpleNames() {
        var a = Annotated.class.getAnnotation(ClassValues.class);
        assertEquals("ScalarTarget", a.scalar().getSimpleName());
        assertEquals("ArrayTarget[]", a.arrayClass().getSimpleName());
    }

    // Class.getSuperclass / isAssignableFrom must also work on classes that only annotation members name.
    @Test
    public void hierarchy() {
        var a = Annotated.class.getAnnotation(ClassValues.class);
        assertEquals(Object.class, a.scalar().getSuperclass());
        assertTrue(a.scalar().isAssignableFrom(a.scalar()));
        assertFalse(a.scalar().isAssignableFrom(String.class));
    }

    // Class.getName: "If this Class object represents a class of arrays, then the internal form of the
    // name consists of the name of the element type preceded by one or more '[' characters". This holds
    // even when the element class's own name is never asked for.
    @Test
    public void arrayNamesWhenOnlyArrayIsAsked() {
        assertEquals("[L" + PREFIX + "LiteralOnlyTarget;", LiteralOnlyTarget[].class.getName());
        assertEquals("[[L" + PREFIX + "LiteralOnlyTarget2;", LiteralOnlyTarget2[][].class.getName());
        assertEquals("LiteralOnlyTarget3[][]", LiteralOnlyTarget3[][].class.getSimpleName());
    }

    static class LiteralOnlyTarget {
    }

    static class LiteralOnlyTarget2 {
    }

    static class LiteralOnlyTarget3 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Nested {
        Class<?> value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ClassValues {
        Class<?> scalar();

        Class<?> byDefault() default DefaultTarget.class;

        Class<?>[] list();

        Class<?> arrayClass();

        Class<?> primitive();

        Nested nested();
    }

    @ClassValues(scalar = ScalarTarget.class, list = { ElementTarget1.class, ElementTarget2.class },
            arrayClass = ArrayTarget[].class, primitive = int.class, nested = @Nested(NestedTarget.class))
    static class Annotated {
    }

    static class ScalarTarget {
    }

    static class DefaultTarget {
    }

    static class ElementTarget1 {
    }

    static class ElementTarget2 {
    }

    static class ArrayTarget {
    }

    static class NestedTarget {
    }
}
