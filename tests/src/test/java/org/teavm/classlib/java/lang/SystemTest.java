/*
 *  Copyright 2013 Alexey Andreev.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
public class SystemTest {
    @Test
    public void copiesArray() {
        Object a = new Object();
        Object b = new Object();
        Object[] src = { a, b, a };
        Object[] dest = new Object[3];
        System.arraycopy(src, 0, dest, 0, 3);
        assertSame(a, dest[0]);
        assertSame(b, dest[1]);
        assertSame(a, dest[2]);
    }

    @Test
    public void copiesArrayPart() {
        var a = new Object();
        var b = new Object();
        var src = new Object[] { a, b, a };
        var dest = new Object[5];
        System.arraycopy(src, 0, dest, 1, 3);
        assertNull(dest[0]);
        assertSame(a, dest[1]);
        assertSame(b, dest[2]);
        assertSame(a, dest[3]);
        assertNull(dest[4]);

        dest = new Object[5];
        System.arraycopy(src, 1, dest, 0, 2);
        assertSame(b, dest[0]);
        assertSame(a, dest[1]);
        assertNull(dest[2]);
        assertNull(dest[3]);
        assertNull(dest[4]);
    }

    @Test
    public void copiesPrimitiveArray() {
        int[] src = { 23, 24, 25 };
        int[] dest = new int[3];
        System.arraycopy(src, 0, dest, 0, 3);
        assertEquals(23, dest[0]);
        assertEquals(24, dest[1]);
        assertEquals(25, dest[2]);
    }

    @Test
    public void copiesPrimitiveArrayPart() {
        var src = new int[] { 23, 24, 25 };
        var dest = new int[5];
        System.arraycopy(src, 0, dest, 1, 3);
        assertArrayEquals(new int[] { 0, 23, 24, 25, 0 }, dest);

        dest = new int[5];
        System.arraycopy(src, 1, dest, 0, 2);
        assertArrayEquals(new int[] { 24, 25, 0, 0, 0 }, dest);
    }

    @Test
    public void copiesToSubclassArray() {
        String[] src = { "foo", "bar", "baz" };
        Object[] dest = new Object[3];
        System.arraycopy(src, 0, dest, 0, 3);
        assertEquals("foo", dest[0]);
        assertEquals("bar", dest[1]);
        assertEquals("baz", dest[2]);
    }

    @Test
    public void copiesToSuperclassArrayWhenItemsMatch() {
        Object[] src = { "foo", "bar", "baz" };
        String[] dest = new String[3];
        System.arraycopy(src, 0, dest, 0, 3);
        assertEquals("foo", dest[0]);
        assertEquals("bar", dest[1]);
        assertEquals("baz", dest[2]);
    }

    @Test
    public void failsToConverItemsCopyingArray() {
        Object[] src = { "foo", 23, "baz" };
        String[] dest = new String[3];
        try {
            System.arraycopy(src, 0, dest, 0, 3);
            fail("Exception expected");
        } catch (ArrayStoreException e) {
            assertEquals("foo", dest[0]);
            assertNull(dest[1]);
            assertNull(dest[2]);
        }
    }

    @Test
    public void failsToCopyToUnrelatedReferenceArray() {
        String[] src = { "foo", "bar", "baz" };
        Integer[] dest = new Integer[3];
        assertThrows(ArrayStoreException.class, () -> System.arraycopy(src, 0, dest, 0, 3));
    }

    @Test
    public void failsToCopyArraysWithInvalidIndexes() {
        assertThrows(IndexOutOfBoundsException.class, () -> System.arraycopy(new Object[0], 0, new Object[0], 0, 1));
    }

    @Test
    public void failsToCopyArraysWithIncompatibleElements() {
        assertThrows(ArrayStoreException.class, () -> System.arraycopy(new Object[1], 0, new int[1], 0, 1));
    }

    @Test
    public void failsToCopyFromNullSource() {
        assertThrows(NullPointerException.class, () -> System.arraycopy(null, 0, new int[1], 0, 1));
    }

    @Test
    public void failsToCopyToNullTarget() {
        assertThrows(NullPointerException.class, () -> System.arraycopy(new Object[1], 0, null, 0, 1));
    }

    @Test
    public void overridesStdErrAndOut() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        System.setOut(new PrintStream(out));

        System.out.println("out overridden");
        System.out.flush();
        System.err.println("err overridden");
        System.err.flush();

        assertEquals("err overridden" + System.lineSeparator(), new String(err.toByteArray()));
        assertEquals("out overridden" + System.lineSeparator(), new String(out.toByteArray()));
    }

    @Test
    @SkipJVM
    public void propertiesWork() {
        Properties properties = System.getProperties();
        assertNotNull(properties);
        assertNotNull(properties.getProperty("java.version"));

        System.setProperty("myprop", "foo");
        assertNull(properties.getProperty("foo"));
        assertEquals("foo", System.getProperty("myprop"));
        properties = System.getProperties();
        assertEquals("foo", properties.getProperty("myprop"));

        Properties newProps = new Properties();
        newProps.setProperty("myprop2", "bar");
        System.setProperties(newProps);
        assertNotNull(System.getProperty("java.version"));
        assertNull(System.getProperty("myprop"));
        assertEquals("bar", System.getProperty("myprop2"));
    }

    @Test
    public void identityHashCode() {
        assertEquals(0, System.identityHashCode(null));

        Object o = new Object();
        int hc = System.identityHashCode(o);
        int hc1 = System.identityHashCode(o);
        assertEquals(hc, hc1);
    }

    @Test
    public void standardPropertiesAreSet() {
        for (var name : List.of("java.vendor", "java.vm.vendor", "os.arch", "java.runtime.name",
                "java.specification.version", "user.dir", "file.encoding")) {
            assertNotNull(System.getProperty(name), name);
        }
    }

    // System.getenv(): "Returns an unmodifiable string map view of the current system environment."
    // System.getenv(String): "the string value of the variable, or null if the variable is not defined".
    @Test
    public void getenvMapAgreesWithGetenv() {
        var env = System.getenv();
        assertNotNull(env);
        for (var entry : env.entrySet()) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            assertEquals(entry.getValue(), System.getenv(entry.getKey()));
            assertTrue(env.containsKey(entry.getKey()));
            assertTrue(env.containsValue(entry.getValue()));
        }
        for (var name : List.of("PATH", "HOME", "TEAVM_NO_SUCH_VARIABLE", "")) {
            assertEquals(env.get(name), System.getenv(name), name);
            assertEquals(env.containsKey(name), System.getenv(name) != null, name);
        }
    }

    // TeaVM programs have no environment. getenv(): "If the system does not support environment variables,
    // an empty map is returned."
    @Test
    @SkipJVM
    public void environmentIsEmpty() {
        assertTrue(System.getenv().isEmpty());
        assertEquals(0, System.getenv().size());
        assertNull(System.getenv("PATH"));
    }

    // getenv(String): "NullPointerException - if name is null"
    @Test
    public void getenvRejectsNullName() {
        assertThrows(NullPointerException.class, () -> System.getenv(null));
    }

    // getenv(): "Attempting to query the presence of a null key or value will throw a NullPointerException.
    // Attempting to query the presence of a key or value which is not of type String will throw a
    // ClassCastException."
    @Test
    @SuppressWarnings({ "SuspiciousMethodCalls", "ResultOfMethodCallIgnored" })
    public void getenvMapRejectsNullAndNonStringQueries() {
        var env = System.getenv();
        assertThrows(NullPointerException.class, () -> env.get(null));
        assertThrows(NullPointerException.class, () -> env.containsKey(null));
        assertThrows(NullPointerException.class, () -> env.containsValue(null));
        assertThrows(ClassCastException.class, () -> env.get(1));
        assertThrows(ClassCastException.class, () -> env.containsKey(1));
        assertThrows(ClassCastException.class, () -> env.containsValue(1));
    }

    // getenv(): "Returns an unmodifiable string map view". Map: "unmodifiable maps ... throw
    // UnsupportedOperationException" for mutators that would change the map. Calls that would have no
    // effect "may, but are not required to" throw, so for them only check that the map is unchanged.
    @Test
    public void getenvMapIsUnmodifiable() {
        var env = System.getenv();
        var snapshot = new HashMap<>(env);
        var key = "TEAVM_NO_SUCH_VARIABLE";
        var other = new HashMap<String, String>();
        other.put(key, "x");

        List<Runnable> mutators = List.of(
                () -> env.put(key, "x"),
                () -> env.putAll(other),
                () -> env.putIfAbsent(key, "x"),
                () -> env.computeIfAbsent(key, k -> "x"),
                () -> env.compute(key, (k, v) -> "x"),
                () -> env.merge(key, "x", (a, b) -> a + b),
                () -> env.keySet().add(key),
                () -> env.values().add("x"),
                () -> env.entrySet().add(new AbstractMap.SimpleEntry<>(key, "x"))
        );
        for (var i = 0; i < mutators.size(); ++i) {
            var mutator = mutators.get(i);
            assertThrows(UnsupportedOperationException.class, mutator::run, "mutator " + i);
            assertEquals(snapshot, env);
        }

        List<Runnable> noEffect = List.of(
                () -> env.remove(key),
                () -> env.remove(key, "x"),
                () -> env.replace(key, "x"),
                () -> env.replace(key, "x", "y"),
                () -> env.computeIfPresent(key, (k, v) -> "x"),
                () -> env.putAll(new HashMap<>()),
                () -> env.keySet().remove(key),
                () -> env.values().remove("TEAVM_NO_SUCH_VALUE")
        );
        for (var mutator : noEffect) {
            try {
                mutator.run();
            } catch (UnsupportedOperationException e) {
                // allowed
            }
            assertEquals(snapshot, env);
        }

        if (!env.isEmpty()) {
            assertThrows(UnsupportedOperationException.class, env::clear);
            assertThrows(UnsupportedOperationException.class, () -> env.replaceAll((k, v) -> "x"));
            var some = env.keySet().iterator().next();
            assertThrows(UnsupportedOperationException.class, () -> env.remove(some));
            assertThrows(UnsupportedOperationException.class, () -> env.put(some, "x"));
        } else {
            List<Runnable> noEffectWhenEmpty = List.of(env::clear, () -> env.replaceAll((k, v) -> "x"));
            for (var mutator : noEffectWhenEmpty) {
                try {
                    mutator.run();
                } catch (UnsupportedOperationException e) {
                    // allowed: no effect on an empty map
                }
            }
        }
        assertEquals(snapshot, env);
    }

    // System.loadLibrary: "UnsatisfiedLinkError - if either the libname argument contains a file path, the native
    // library is not statically linked with the VM, or the library cannot be mapped to a native library image by
    // the host system." System.load: "UnsatisfiedLinkError - if either the filename is not an absolute path name,
    // the native library is not statically linked with the VM, or the library cannot be mapped to a native library
    // image by the host system." TeaVM programs cannot load native libraries.
    // "The call System.loadLibrary(name) is effectively equivalent to the call Runtime.getRuntime().loadLibrary(name)",
    // and likewise for load.
    @Test
    public void nativeLibrariesAreNotFound() {
        var runtime = Runtime.getRuntime();
        for (var name : List.of("teavm-test-no-such-library", "", "teavm-test/no-such-library",
                "/teavm-test/no-such-library.so")) {
            assertThrows(UnsatisfiedLinkError.class, () -> System.loadLibrary(name), name);
            assertThrows(UnsatisfiedLinkError.class, () -> runtime.loadLibrary(name), name);
        }
        for (var name : List.of("/teavm-test/no-such-library.so", "/teavm-test/libno-such-library.dylib",
                "C:\\teavm-test\\no-such-library.dll", "no-such-library.so", "")) {
            assertThrows(UnsatisfiedLinkError.class, () -> System.load(name), name);
            assertThrows(UnsatisfiedLinkError.class, () -> runtime.load(name), name);
        }
    }

    // System.load / System.loadLibrary / Runtime.load / Runtime.loadLibrary:
    // "NullPointerException - if filename is null" ("if libname is null" for loadLibrary)
    @Test
    public void nativeLibraryNameMustNotBeNull() {
        var runtime = Runtime.getRuntime();
        assertThrows(NullPointerException.class, () -> System.load(null));
        assertThrows(NullPointerException.class, () -> System.loadLibrary(null));
        assertThrows(NullPointerException.class, () -> runtime.load(null));
        assertThrows(NullPointerException.class, () -> runtime.loadLibrary(null));
    }
}
