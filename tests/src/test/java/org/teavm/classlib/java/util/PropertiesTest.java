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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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
}
