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
package org.teavm.classlib.java.lang;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
public class ClassLoaderTest {
    @Test
    public void loadsResources() {
        assertEquals("q", loadResource("1"));
        assertEquals("qw", loadResource("2"));
        assertEquals("qwe", loadResource("3"));
        assertEquals("qwer", loadResource("4"));
        assertEquals("qwert", loadResource("5"));
        assertEquals("qwerty", loadResource("6"));
        assertEquals("qwertyu", loadResource("7"));
        assertEquals("qwertyui", loadResource("8"));
        assertEquals("qwertyuiopasdfghjklzxcvbnm", loadResource("9"));
    }

    @Test
    public void binaryResourceWithNullByte() throws IOException {
        var classLoader = ClassLoader.getSystemClassLoader();
        var bytes = classLoader.getResourceAsStream("resources-for-test/binary-null").readAllBytes();
        assertArrayEquals(new byte[] { 'a', 0, 'b' }, bytes);
    }

    @Test
    public void binaryResourceAllBytes() throws IOException {
        var classLoader = ClassLoader.getSystemClassLoader();
        var bytes = classLoader.getResourceAsStream("resources-for-test/binary-all-bytes").readAllBytes();
        assertEquals(256, bytes.length);
        for (int i = 0; i < 256; i++) {
            assertEquals((byte) i, bytes[i]);
        }
    }

    @Test
    public void returnsNullForNonExistentResource() {
        var input = ClassLoader.getSystemClassLoader().getResourceAsStream("non-existent-resource.txt");
        assertNull(input);
    }

    private static String loadResource(String name) {
        var classLoader = ClassLoader.getSystemClassLoader();
        try (var reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream(
                "resources-for-test/" + name), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return "";
        }
    }

    // ClassLoader(): "Creates a new class loader using the ClassLoader returned by the method
    // getSystemClassLoader() as the parent class loader."
    @Test
    public void defaultParentIsSystemLoader() {
        assertSame(ClassLoader.getSystemClassLoader(), new ClassLoader() { }.getParent());
        assertNull(new ClassLoader(null) { }.getParent());
        var parent = new ClassLoader() { };
        assertSame(parent, new ClassLoader(parent) { }.getParent());
    }

    @Test
    public void loaderDelegatesToParentFirst() throws ClassNotFoundException {
        var loader = new ClassLoader() { };
        assertSame(String.class, loader.loadClass("java.lang.String"));
        assertSame(ClassLoaderTest.class, loader.loadClass(ClassLoaderTest.class.getName()));
        assertSame(ArrayList.class, loader.loadClass("java.util.ArrayList"));
        try {
            loader.loadClass("org.teavm.classlib.java.lang.NoSuchClassForLoaderTest");
            fail();
        } catch (ClassNotFoundException e) {
            // ok
        }
    }

    @Test
    public void systemLoaderFindsKeptClasses() throws ClassNotFoundException {
        var system = ClassLoader.getSystemClassLoader();
        assertSame(String.class, system.loadClass("java.lang.String"));
        assertSame(ClassLoaderTest.class, system.loadClass(ClassLoaderTest.class.getName()));
        assertSame(ClassLoaderTest.class, ClassLoaderTest.class.getClassLoader().loadClass(
                ClassLoaderTest.class.getName()));
        try {
            system.loadClass("org.teavm.classlib.java.lang.NoSuchClassForLoaderTest");
            fail();
        } catch (ClassNotFoundException e) {
            // ok
        }
    }

    // loadClass(String, boolean): "The default implementation of this method searches for classes in the
    // following order: [findLoadedClass, then] Invoke the loadClass method on the parent class loader. [...]
    // Invoke the findClass(String) method to find the class."
    @Test
    public void parentIsAskedBeforeFindClass() throws ClassNotFoundException {
        var log = new ArrayList<String>();
        var parent = new RecordingLoader("parent", ClassLoader.getSystemClassLoader(), log,
                Map.of("test.Shared", Integer.class, "test.ParentOnly", Long.class));
        var child = new RecordingLoader("child", parent, log,
                Map.of("test.Shared", Short.class, "test.ChildOnly", Byte.class));

        // The parent has it, so the child's findClass is never called.
        assertSame(Integer.class, child.loadClass("test.Shared"));
        assertEquals(List.of("child.loadClass(test.Shared,false)", "parent.loadClass(test.Shared,false)",
                "parent.findClass(test.Shared)"), log);

        log.clear();
        assertSame(Long.class, child.loadClass("test.ParentOnly"));
        assertEquals(List.of("child.loadClass(test.ParentOnly,false)", "parent.loadClass(test.ParentOnly,false)",
                "parent.findClass(test.ParentOnly)"), log);

        // The parent throws ClassNotFoundException, so the child's findClass tries next.
        log.clear();
        assertSame(Byte.class, child.loadClass("test.ChildOnly"));
        assertEquals(List.of("child.loadClass(test.ChildOnly,false)", "parent.loadClass(test.ChildOnly,false)",
                "parent.findClass(test.ChildOnly)", "child.findClass(test.ChildOnly)"), log);

        // The system loader (the parent's parent) has it, so neither findClass is called.
        log.clear();
        assertSame(String.class, child.loadClass("java.lang.String"));
        assertEquals(List.of("child.loadClass(java.lang.String,false)",
                "parent.loadClass(java.lang.String,false)"), log);
        log.clear();
        var testName = ClassLoaderTest.class.getName();
        assertSame(ClassLoaderTest.class, child.loadClass(testName));
        assertEquals(List.of("child.loadClass(" + testName + ",false)", "parent.loadClass(" + testName + ",false)"),
                log);
    }

    // loadClass(String, boolean): "If the parent is null the class loader built into the virtual machine is
    // used, instead." The built-in loader holds the platform classes, not the application's.
    @Test
    public void nullParentUsesBuiltInLoader() throws ClassNotFoundException {
        var log = new ArrayList<String>();
        var testName = ClassLoaderTest.class.getName();
        var loader = new RecordingLoader("loader", null, log, Map.of("test.Mine", Byte.class, testName, Short.class));

        assertSame(String.class, loader.loadClass("java.lang.String"));
        assertSame(ArrayList.class, loader.loadClass("java.util.ArrayList"));
        assertEquals(List.of("loader.loadClass(java.lang.String,false)",
                "loader.loadClass(java.util.ArrayList,false)"), log);

        log.clear();
        assertSame(Byte.class, loader.loadClass("test.Mine"));
        assertEquals(List.of("loader.loadClass(test.Mine,false)", "loader.findClass(test.Mine)"), log);

        // An application class is not a platform class, so this loader's findClass decides.
        log.clear();
        assertSame(Short.class, loader.loadClass(testName));
        assertEquals(List.of("loader.loadClass(" + testName + ",false)", "loader.findClass(" + testName + ")"), log);

        // A platform name that the program does not have also goes to findClass, which throws.
        log.clear();
        try {
            loader.loadClass("java.lang.NoSuchClassForLoaderTest");
            fail();
        } catch (ClassNotFoundException e) {
            // ok
        }
        assertEquals(List.of("loader.loadClass(java.lang.NoSuchClassForLoaderTest,false)",
                "loader.findClass(java.lang.NoSuchClassForLoaderTest)"), log);

        assertSame(String.class, new ClassLoader(null) { }.loadClass("java.lang.String"));
        try {
            new ClassLoader(null) { }.loadClass(testName);
            fail();
        } catch (ClassNotFoundException e) {
            // ok
        }
    }

    // loadClass(String): "Invoking this method is equivalent to invoking loadClass(name, false)."
    // loadClass(String, boolean): "If the class was found using the above steps, and the resolve flag is true,
    // this method will then invoke the resolveClass(Class) method on the resulting Class object."
    // resolveClass(Class): "Throws: NullPointerException - If c is null"
    @Test
    public void resolveFlag() throws ClassNotFoundException {
        var log = new ArrayList<String>();
        var loader = new RecordingLoader("loader", null, log, Map.of("test.Mine", Byte.class));
        assertSame(Byte.class, loader.loadClass("test.Mine", true));
        assertSame(String.class, loader.loadClass("java.lang.String", true));
        assertEquals(List.of("loader.loadClass(test.Mine,true)", "loader.findClass(test.Mine)",
                "loader.loadClass(java.lang.String,true)"), log);
        loader.resolve(String.class);
        try {
            loader.resolve(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        // A findClass that returns null breaks its contract; the null reaches resolveClass only if resolve is set.
        var nullLoader = new ClassLoader(null) {
            @Override
            protected Class<?> findClass(String name) {
                return null;
            }

            Class<?> load(String name, boolean resolve) throws ClassNotFoundException {
                return loadClass(name, resolve);
            }
        };
        assertNull(nullLoader.load("test.Null", false));
        try {
            nullLoader.load("test.Null", true);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
    }

    // findLoadedClass: "Returns: The Class object, or null if the class has not been loaded". A loader can not
    // define classes in TeaVM, so no class is ever recorded as loaded by it.
    @Test
    public void findLoadedClassFindsNothing() throws ClassNotFoundException {
        var loader = new RecordingLoader("loader", null, new ArrayList<>(), Map.of("test.Mine", Byte.class));
        loader.loadClass("test.Mine");
        loader.loadClass("java.lang.String");
        assertNull(loader.loaded("test.Mine"));
        assertNull(loader.loaded("java.lang.String"));
    }

    // findClass: "The default implementation throws ClassNotFoundException."
    // loadClass catches only the parent's ClassNotFoundException; any other exception reaches the caller, and
    // this loader's findClass is not called.
    @Test
    public void exceptionsPropagate() throws ClassNotFoundException {
        var log = new ArrayList<String>();
        var child = new RecordingLoader("child", new ClassLoader(null) { }, log, Map.of());
        try {
            child.loadClass("test.Missing");
            fail();
        } catch (ClassNotFoundException e) {
            // ok
        }
        assertEquals(List.of("child.loadClass(test.Missing,false)", "child.findClass(test.Missing)"), log);

        var failure = new IllegalStateException();
        var throwingParent = new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) {
                throw failure;
            }
        };
        log.clear();
        var child2 = new RecordingLoader("child", throwingParent, log, Map.of("test.Mine", Byte.class));
        try {
            child2.loadClass("test.Mine");
            fail();
        } catch (IllegalStateException e) {
            assertSame(failure, e);
        }
        assertEquals(List.of("child.loadClass(test.Mine,false)"), log);

        // The ClassNotFoundException from this loader's own findClass reaches the caller as is.
        var notFound = new ClassNotFoundException("custom");
        var custom = new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                throw notFound;
            }
        };
        try {
            custom.loadClass("test.Missing");
            fail();
        } catch (ClassNotFoundException e) {
            assertSame(notFound, e);
        }
    }

    static class RecordingLoader extends ClassLoader {
        private final String id;
        private final List<String> log;
        private final Map<String, Class<?>> classes;

        RecordingLoader(String id, ClassLoader parent, List<String> log, Map<String, Class<?>> classes) {
            super(parent);
            this.id = id;
            this.log = log;
            this.classes = classes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            log.add(id + ".loadClass(" + name + "," + resolve + ")");
            return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            log.add(id + ".findClass(" + name + ")");
            var cls = classes.get(name);
            if (cls == null) {
                throw new ClassNotFoundException(name);
            }
            return cls;
        }

        void resolve(Class<?> c) {
            resolveClass(c);
        }

        Class<?> loaded(String name) {
            return findLoadedClass(name);
        }
    }
}
