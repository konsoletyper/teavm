/*
 *  Copyright 2025 Ashera Cordova.
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
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class PropertiesTest {

    @Test
    public void testSetAndGetProperty() {
        Properties props = new Properties();
        props.setProperty("username", "admin");
        props.setProperty("timeout", "30");

        assertEquals("admin", props.getProperty("username"));
        assertEquals("30", props.getProperty("timeout"));
        assertNull(props.getProperty("missingKey"));
        assertEquals("default", props.getProperty("missingKey", "default"));
    }
    
    @Test
    public void testLoadFromReader() throws IOException {
        String data = "# Sample config\n"
                + "host=localhost\n"
                + "port=8080\n";

        Properties props = new Properties();
        Reader reader = new StringReader(data);
        props.load(reader);
        reader.close();

        assertEquals("localhost", props.getProperty("host"));
        assertEquals("8080", props.getProperty("port"));

        data = "# Sample configuration\n"
                + "host=localhost\n"
                + "port=8080\n"
                + "description=This is a long description \\\n"
                + "that continues on the next line \\\n"
                + "and ends here.\n"
                + "path=C:\\\\Program Files\\\\MyApp\n";
        reader = new StringReader(data);
        props = new Properties();
        props.load(reader);
        reader.close();

        assertEquals("localhost", props.getProperty("host"));
        assertEquals("8080", props.getProperty("port"));
        assertEquals("This is a long description that continues on the next line and ends here.",
                     props.getProperty("description"));
        assertEquals("C:\\Program Files\\MyApp", props.getProperty("path"));
    }
    
    
    @Test
    public void testLoadFromInputStream() throws IOException {
        String data = "db.user=root\n"
                       + "db.pass=secret\n";

        Properties props = new Properties();
        InputStream in = new ByteArrayInputStream(data.getBytes("ISO-8859-1"));
        props.load(in);
        in.close();

        assertEquals("root", props.getProperty("db.user"));
        assertEquals("secret", props.getProperty("db.pass"));
    }

    /*
     * Properties Javadoc (Java SE 21), class description: "If the store or save method is called on a
     * "compromised" Properties object that contains a non-String key or value, the call will fail.
     * Similarly, the call to the propertyNames or list method will fail if it is called on a "compromised"
     * Properties object that contains a non-String key."
     *
     * stringPropertyNames(): "Returns an unmodifiable set of keys from this property list where the key and
     * its corresponding value are strings, including distinct keys in the default property list if a key of
     * the same name has not already been found from the main properties list. Properties whose key or value
     * is not of type String are omitted. The returned set is not backed by this Properties object."
     *
     * propertyNames(), list(PrintStream): "Throws: ClassCastException - if any key in this property list is
     * not a string."
     *
     * store(...): "Throws: ClassCastException - if this Properties object contains any keys or values that
     * are not Strings."
     */

    @Test
    public void stringPropertyNamesSkipsNonStringKeysAndValues() {
        Properties props = new Properties();
        props.setProperty("a", "1");
        props.put(1, "x");
        props.put("n", 5);
        props.put(2, 3);
        assertEquals(Set.of("a"), props.stringPropertyNames());

        // A non-String entry in the main list doesn't hide a String entry of the defaults (as in OpenJDK,
        // consistent with getProperty falling back to the defaults)
        Properties withDefaults = new Properties(props);
        withDefaults.put("a", 6);
        assertEquals(Set.of("a"), withDefaults.stringPropertyNames());

        // A String entry in the main list counts although the defaults' entry of that key is not a String
        withDefaults.setProperty("n", "7");
        withDefaults.setProperty("c", "8");
        assertEquals(Set.of("a", "n", "c"), withDefaults.stringPropertyNames());

        // Defaults of defaults; a non-String key anywhere never throws here
        Properties deeper = new Properties(withDefaults);
        deeper.put(new Object(), new Object());
        deeper.setProperty("d", "9");
        assertEquals(Set.of("a", "n", "c", "d"), deeper.stringPropertyNames());

        assertEquals(Set.of(), new Properties().stringPropertyNames());
    }

    @Test
    public void stringPropertyNamesIsUnmodifiableSnapshot() {
        Properties props = new Properties();
        props.setProperty("a", "1");
        Set<String> names = props.stringPropertyNames();
        try {
            names.add("b");
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        try {
            names.remove("a");
            fail();
        } catch (UnsupportedOperationException e) {
            // ok
        }
        props.setProperty("b", "2");
        props.remove("a");
        assertEquals(Set.of("a"), names);
    }

    @Test
    public void propertyNamesRejectsNonStringKey() {
        Properties props = new Properties();
        props.setProperty("a", "1");
        props.put(1, "x");
        assertThrows(ClassCastException.class, props::propertyNames);
        assertThrows(ClassCastException.class, () -> props.list(new PrintStream(new ByteArrayOutputStream())));

        // Through the defaults, and their defaults
        Properties withDefaults = new Properties(props);
        withDefaults.setProperty("b", "2");
        assertThrows(ClassCastException.class, withDefaults::propertyNames);
        assertThrows(ClassCastException.class,
                () -> withDefaults.list(new PrintStream(new ByteArrayOutputStream())));
        Properties deeper = new Properties(withDefaults);
        assertThrows(ClassCastException.class, deeper::propertyNames);

        // Any kind of non-String key
        for (Object key : new Object[] { 'c', 1L, new Object(), new StringBuilder("a") }) {
            Properties p = new Properties();
            p.put(key, "v");
            assertThrows(ClassCastException.class, p::propertyNames);
        }
    }

    @Test
    public void propertyNamesAcceptsNonStringValue() {
        // Only a non-String key makes propertyNames fail
        Properties props = new Properties();
        props.setProperty("a", "1");
        props.put("n", 5);
        assertEquals(List.of("a", "n"), sortedPropertyNames(props));

        Properties withDefaults = new Properties(props);
        withDefaults.setProperty("a", "2");
        withDefaults.setProperty("b", "3");
        assertEquals(List.of("a", "b", "n"), sortedPropertyNames(withDefaults));

        assertEquals(List.of(), sortedPropertyNames(new Properties()));
    }

    @Test
    public void storeRejectsNonStringKeyOrValue() {
        Properties badKey = new Properties();
        badKey.setProperty("a", "1");
        badKey.put(1, "x");
        assertThrows(ClassCastException.class, () -> badKey.store(new ByteArrayOutputStream(), null));
        assertThrows(ClassCastException.class, () -> badKey.save(new ByteArrayOutputStream(), null));

        Properties badValue = new Properties();
        badValue.setProperty("a", "1");
        badValue.put("n", 5);
        assertThrows(ClassCastException.class, () -> badValue.store(new ByteArrayOutputStream(), null));
        assertThrows(ClassCastException.class, () -> badValue.save(new ByteArrayOutputStream(), null));
    }

    @Test
    public void storeRoundTrips() throws IOException {
        Properties props = new Properties();
        props.setProperty("a", "1");
        props.setProperty("key with = and :", " value\twith\nspecials #!\\");
        props.setProperty("unicode", "é中😀");
        props.setProperty("", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, "comment\nsecond line");
        for (byte b : out.toByteArray()) {
            // store(OutputStream) writes ISO 8859-1 and escapes other characters as \\uXXXX
            assertTrue(b >= 0, "non-ASCII byte in output");
        }
        Properties loaded = new Properties();
        loaded.load(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(props, loaded);

        ByteArrayOutputStream saved = new ByteArrayOutputStream();
        props.save(saved, null);
        Properties loadedSaved = new Properties();
        loadedSaved.load(new ByteArrayInputStream(saved.toByteArray()));
        assertEquals(props, loadedSaved);
    }

    @Test
    public void getPropertyIgnoresNonStringValue() {
        Properties defaults = new Properties();
        defaults.setProperty("a", "d");
        Properties props = new Properties(defaults);
        props.put("a", 5);
        props.put("n", 6);
        // As in OpenJDK, a non-String value counts as absent, so the defaults are searched
        assertEquals("d", props.getProperty("a"));
        assertNull(props.getProperty("n"));
        assertEquals("x", props.getProperty("n", "x"));
    }

    private static List<String> sortedPropertyNames(Properties props) {
        List<String> names = new ArrayList<>();
        for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
            names.add((String) e.nextElement());
        }
        Collections.sort(names);
        return names;
    }
}
