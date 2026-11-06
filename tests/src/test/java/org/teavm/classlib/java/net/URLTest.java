/*
 *  Copyright 2018 Alexey Andreev.
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

package org.teavm.classlib.java.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class URLTest {
    URL u;
    URL u1;
    URL u2;
    URL u3;
    URL u4;
    URL u5;
    boolean caught;
    
    
    @Test
    public void test_ConstructorLjava_lang_String() throws IOException {
        // Tests for multiple URL instantiation basic parsing test
        u = new URL("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1");
        assertEquals("http", u.getProtocol(), "u returns a wrong protocol");
        assertEquals("www.yahoo1.com", u.getHost(), "u returns a wrong host");
        assertEquals(8080, u.getPort(), "u returns a wrong port");
        assertEquals("/dir1/dir2/test.cgi?point1.html", u.getFile(), "u returns a wrong file");
        assertEquals("anchor1", u.getRef(), "u returns a wrong anchor");

        // test for no file
        u1 = new URL("http://www.yahoo2.com:9999");
        assertEquals("http", u1.getProtocol(), "u1 returns a wrong protocol");
        assertEquals("www.yahoo2.com", u1.getHost(), "u1 returns a wrong host");
        assertEquals(9999, u1.getPort(), "u1 returns a wrong port");
        assertTrue(u1.getFile().equals(""), "u1 returns a wrong file");
        assertNull(u1.getRef(), "u1 returns a wrong anchor");

        // test for no port
        u2 = new URL("http://www.yahoo3.com/dir1/dir2/test.cgi?point1.html#anchor1");
        assertEquals("http", u2.getProtocol(), "u2 returns a wrong protocol");
        assertEquals("www.yahoo3.com", u2.getHost(), "u2 returns a wrong host");
        assertEquals(-1, u2.getPort(), "u2 returns a wrong port");
        assertEquals("/dir1/dir2/test.cgi?point1.html", u2.getFile(), "u2 returns a wrong file");
        assertEquals("anchor1", u2.getRef(), "u2 returns a wrong anchor");

        // test for no port
        URL u2a = new URL("file://www.yahoo3.com/dir1/dir2/test.cgi#anchor1");
        assertEquals("file", u2a.getProtocol(), "u2a returns a wrong protocol");
        assertEquals("www.yahoo3.com", u2a.getHost(), "u2a returns a wrong host");
        assertEquals(-1, u2a.getPort(), "u2a returns a wrong port");
        assertEquals("/dir1/dir2/test.cgi", u2a.getFile(), "u2a returns a wrong file");
        assertEquals("anchor1", u2a.getRef(), "u2a returns a wrong anchor");

        // test for no file, no port
        u3 = new URL("http://www.yahoo4.com/");
        assertEquals("http", u3.getProtocol(), "u3 returns a wrong protocol");
        assertEquals("www.yahoo4.com", u3.getHost(), "u3 returns a wrong host");
        assertEquals(-1, u3.getPort(), "u3 returns a wrong port");
        assertEquals("/", u3.getFile(), "u3 returns a wrong file");
        assertNull(u3.getRef(), "u3 returns a wrong anchor");

        // test for no file, no port
        URL u3a = new URL("file://www.yahoo4.com/");
        assertEquals("file", u3a.getProtocol(), "u3a returns a wrong protocol");
        assertEquals("www.yahoo4.com", u3a.getHost(), "u3a returns a wrong host");
        assertEquals(-1, u3a.getPort(), "u3a returns a wrong port");
        assertEquals("/", u3a.getFile(), "u3a returns a wrong file");
        assertNull(u3a.getRef(), "u3a returns a wrong anchor");

        // test for no file, no port
        URL u3b = new URL("file://www.yahoo4.com");
        assertEquals("file", u3b.getProtocol(), "u3b returns a wrong protocol");
        assertEquals("www.yahoo4.com", u3b.getHost(), "u3b returns a wrong host");
        assertEquals(-1, u3b.getPort(), "u3b returns a wrong port");
        assertTrue(u3b.getFile().equals(""), "u3b returns a wrong file");
        assertNull(u3b.getRef(), "u3b returns a wrong anchor");

        // test for non-port ":" and wierd characters occurrences
        u4 = new URL("http://www.yahoo5.com/di!@$%^&*()_+r1/di:::r2/test.cgi?point1.html#anchor1");
        assertEquals("http", u4.getProtocol(), "u4 returns a wrong protocol");
        assertEquals("www.yahoo5.com", u4.getHost(), "u4 returns a wrong host");
        assertEquals(-1, u4.getPort(), "u4 returns a wrong port");
        assertEquals("/di!@$%^&*()_+r1/di:::r2/test.cgi?point1.html", u4.getFile(), "u4 returns a wrong file");
        assertEquals("anchor1", u4.getRef(), "u4 returns a wrong anchor");

        u5 = new URL("file:/testing.tst");
        assertEquals("file", u5.getProtocol(), "u5 returns a wrong protocol");
        assertTrue(u5.getHost().equals(""), "u5 returns a wrong host");
        assertEquals(-1, u5.getPort(), "u5 returns a wrong port");
        assertEquals("/testing.tst", u5.getFile(), "u5 returns a wrong file");
        assertNull(u5.getRef(), "u5 returns a wrong anchor");

        URL u5a = new URL("file:testing.tst");
        assertEquals("file", u5a.getProtocol(), "u5a returns a wrong protocol");
        assertTrue(u5a.getHost().equals(""), "u5a returns a wrong host");
        assertEquals(-1, u5a.getPort(), "u5a returns a wrong port");
        assertEquals("testing.tst", u5a.getFile(), "u5a returns a wrong file");
        assertNull(u5a.getRef(), "u5a returns a wrong anchor");

        URL u6 = new URL("http://host:/file");
        assertEquals(-1, u6.getPort(), "u6 return a wrong port");

        URL u7 = new URL("file:../../file.txt");
        assertTrue(u7.getFile().equals("../../file.txt"), "u7 returns a wrong file: " + u7.getFile());

        URL u8 = new URL("http://[fec0::1:20d:60ff:fe24:7410]:35/file.txt");
        assertTrue(u8.getProtocol().equals("http"), "u8 returns a wrong protocol " + u8.getProtocol());
        assertTrue(u8.getHost().equals("[fec0::1:20d:60ff:fe24:7410]"), "u8 returns a wrong host " + u8.getHost());
        assertTrue(u8.getPort() == 35, "u8 returns a wrong port " + u8.getPort());
        assertTrue(u8.getFile().equals("/file.txt"), "u8 returns a wrong file " + u8.getFile());
        assertNull(u8.getRef(), "u8 returns a wrong anchor " + u8.getRef());

        URL u9 = new URL("file://[fec0::1:20d:60ff:fe24:7410]/file.txt#sogood");
        assertTrue(u9.getProtocol().equals("file"), "u9 returns a wrong protocol " + u9.getProtocol());
        assertTrue(u9.getHost().equals("[fec0::1:20d:60ff:fe24:7410]"), "u9 returns a wrong host " + u9.getHost());
        assertTrue(u9.getPort() == -1, "u9 returns a wrong port " + u9.getPort());
        assertTrue(u9.getFile().equals("/file.txt"), "u9 returns a wrong file " + u9.getFile());
        assertTrue(u9.getRef().equals("sogood"), "u9 returns a wrong anchor " + u9.getRef());

        URL u10 = new URL("file://[fec0::1:20d:60ff:fe24:7410]");
        assertTrue(u10.getProtocol().equals("file"), "u10 returns a wrong protocol " + u10.getProtocol());
        assertTrue(u10.getHost().equals("[fec0::1:20d:60ff:fe24:7410]"), "u10 returns a wrong host " + u10.getHost());
        assertTrue(u10.getPort() == -1, "u10 returns a wrong port " + u10.getPort());

        URL u11 = new URL("file:////file.txt");
        assertNull(u11.getAuthority(), "u11 returns a wrong authority " + u11.getAuthority());
        assertTrue(u11.getFile().equals("////file.txt"), "u11 returns a wrong file " + u11.getFile());

        URL u12 = new URL("file:///file.txt");
        assertTrue(u12.getAuthority().equals(""), "u12 returns a wrong authority");
        assertTrue(u12.getFile().equals("/file.txt"), "u12 returns a wrong file " + u12.getFile());


        // test for error catching

        // Bad HTTP format - no "//"
        u = new URL("http:www.yahoo5.com::22/dir1/di:::r2/test.cgi?point1.html#anchor1");

        caught = false;
        try {
            u = new URL("http://www.yahoo5.com::22/dir1/di:::r2/test.cgi?point1.html#anchor1");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue(caught, "Should have throw MalformedURLException");

        // unknown protocol
        try {
            u = new URL("myProtocol://www.yahoo.com:22");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue(caught, "3 Failed to throw MalformedURLException");

        caught = false;
        // no protocol
        try {
            u = new URL("www.yahoo.com");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue(caught, "4 Failed to throw MalformedURLException");

        caught = false;

        URL u1 = null;
        try {
            // No leading or trailing spaces.
            u1 = new URL("file:/some/path");
            assertEquals(10, u1.getFile().length(), "5 got wrong file length1");

            // Leading spaces.
            u1 = new URL("  file:/some/path");
            assertEquals(10, u1.getFile().length(), "5 got wrong file length2");

            // Trailing spaces.
            u1 = new URL("file:/some/path  ");
            assertEquals(10, u1.getFile().length(), "5 got wrong file length3");

            // Leading and trailing.
            u1 = new URL("  file:/some/path ");
            assertEquals(10, u1.getFile().length(), "5 got wrong file length4");

            // in-place spaces.
            u1 = new URL("  file:  /some/path ");
            assertEquals(12, u1.getFile().length(), "5 got wrong file length5");

        } catch (MalformedURLException e) {
            fail("5 Did not expect the exception " + e);
        }

        // testing jar protocol with relative path
        // to make sure it's not canonicalized
        try {
            String file = "file:/a!/b/../d";

            u = new URL("jar:" + file);
            assertEquals(file, u.getFile(), "Wrong file (jar protocol, relative path)");
        } catch (MalformedURLException e) {
            fail("Unexpected exception (jar protocol, relative path)" + e);
        }

        // no protocol
        caught = false;
        try {
            u = new URL(":");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue(caught, "7 Failed to throw MalformedURLException");
    }

    @Test
    public void test_ConstructorLjava_net_URLLjava_lang_String() throws Exception {
        // Test for method java.net.URL(java.net.URL, java.lang.String)
        u = new URL("http://www.yahoo.com");
        URL uf = new URL("file://www.yahoo.com");
        // basic ones
        u1 = new URL(u, "file.java");
        assertEquals("http", u1.getProtocol(), "1 returns a wrong protocol");
        assertEquals("www.yahoo.com", u1.getHost(), "1 returns a wrong host");
        assertEquals(-1, u1.getPort(), "1 returns a wrong port");
        assertEquals("/file.java", u1.getFile(), "1 returns a wrong file");
        assertNull(u1.getRef(), "1 returns a wrong anchor");

        URL u1f = new URL(uf, "file.java");
        assertEquals("file", u1f.getProtocol(), "1f returns a wrong protocol");
        assertEquals("www.yahoo.com", u1f.getHost(), "1f returns a wrong host");
        assertEquals(-1, u1f.getPort(), "1f returns a wrong port");
        assertEquals("/file.java", u1f.getFile(), "1f returns a wrong file");
        assertNull(u1f.getRef(), "1f returns a wrong anchor");

        u1 = new URL(u, "dir1/dir2/../file.java");
        assertEquals("http", u1.getProtocol(), "3 returns a wrong protocol");
        assertTrue(u1.getHost().equals("www.yahoo.com"), "3 returns a wrong host: " + u1.getHost());
        assertEquals(-1, u1.getPort(), "3 returns a wrong port");
        assertEquals("/dir1/dir2/../file.java", u1.getFile(), "3 returns a wrong file");
        assertNull(u1.getRef(), "3 returns a wrong anchor");

        u1 = new URL(u, "http:dir1/dir2/../file.java");
        assertEquals("http", u1.getProtocol(), "3a returns a wrong protocol");
        assertTrue(u1.getHost().equals(""), "3a returns a wrong host: " + u1.getHost());
        assertEquals(-1, u1.getPort(), "3a returns a wrong port");
        assertEquals("dir1/dir2/../file.java", u1.getFile(), "3a returns a wrong file");
        assertNull(u1.getRef(), "3a returns a wrong anchor");

        u = new URL("http://www.apache.org/testing/");
        u1 = new URL(u, "file.java");
        assertEquals("http", u1.getProtocol(), "4 returns a wrong protocol");
        assertEquals("www.apache.org", u1.getHost(), "4 returns a wrong host");
        assertEquals(-1, u1.getPort(), "4 returns a wrong port");
        assertEquals("/testing/file.java", u1.getFile(), "4 returns a wrong file");
        assertNull(u1.getRef(), "4 returns a wrong anchor");

        uf = new URL("file://www.apache.org/testing/");
        u1f = new URL(uf, "file.java");
        assertEquals("file", u1f.getProtocol(), "4f returns a wrong protocol");
        assertEquals("www.apache.org", u1f.getHost(), "4f returns a wrong host");
        assertEquals(-1, u1f.getPort(), "4f returns a wrong port");
        assertEquals("/testing/file.java", u1f.getFile(), "4f returns a wrong file");
        assertNull(u1f.getRef(), "4f returns a wrong anchor");

        uf = new URL("file:/testing/");
        u1f = new URL(uf, "file.java");
        assertEquals("file", u1f.getProtocol(), "4fa returns a wrong protocol");
        assertTrue(u1f.getHost().equals(""), "4fa returns a wrong host");
        assertEquals(-1, u1f.getPort(), "4fa returns a wrong port");
        assertEquals("/testing/file.java", u1f.getFile(), "4fa returns a wrong file");
        assertNull(u1f.getRef(), "4fa returns a wrong anchor");

        uf = new URL("file:testing/");
        u1f = new URL(uf, "file.java");
        assertEquals("file", u1f.getProtocol(), "4fb returns a wrong protocol");
        assertTrue(u1f.getHost().equals(""), "4fb returns a wrong host");
        assertEquals(-1, u1f.getPort(), "4fb returns a wrong port");
        assertEquals("testing/file.java", u1f.getFile(), "4fb returns a wrong file");
        assertNull(u1f.getRef(), "4fb returns a wrong anchor");

        u1f = new URL(uf, "file:file.java");
        assertEquals("file", u1f.getProtocol(), "4fc returns a wrong protocol");
        assertTrue(u1f.getHost().equals(""), "4fc returns a wrong host");
        assertEquals(-1, u1f.getPort(), "4fc returns a wrong port");
        assertEquals("file.java", u1f.getFile(), "4fc returns a wrong file");
        assertNull(u1f.getRef(), "4fc returns a wrong anchor");

        u1f = new URL(uf, "file:");
        assertEquals("file", u1f.getProtocol(), "4fd returns a wrong protocol");
        assertTrue(u1f.getHost().equals(""), "4fd returns a wrong host");
        assertEquals(-1, u1f.getPort(), "4fd returns a wrong port");
        assertTrue(u1f.getFile().equals(""), "4fd returns a wrong file");
        assertNull(u1f.getRef(), "4fd returns a wrong anchor");

        u = new URL("http://www.apache.org/testing");
        u1 = new URL(u, "file.java");
        assertEquals("http", u1.getProtocol(), "5 returns a wrong protocol");
        assertEquals("www.apache.org", u1.getHost(), "5 returns a wrong host");
        assertEquals(-1, u1.getPort(), "5 returns a wrong port");
        assertEquals("/file.java", u1.getFile(), "5 returns a wrong file");
        assertNull(u1.getRef(), "5 returns a wrong anchor");

        uf = new URL("file://www.apache.org/testing");
        u1f = new URL(uf, "file.java");
        assertEquals("file", u1f.getProtocol(), "5f returns a wrong protocol");
        assertEquals("www.apache.org", u1f.getHost(), "5f returns a wrong host");
        assertEquals(-1, u1f.getPort(), "5f returns a wrong port");
        assertEquals("/file.java", u1f.getFile(), "5f returns a wrong file");
        assertNull(u1f.getRef(), "5f returns a wrong anchor");

        uf = new URL("file:/testing");
        u1f = new URL(uf, "file.java");
        assertEquals("file", u1f.getProtocol(), "5fa returns a wrong protocol");
        assertTrue(u1f.getHost().equals(""), "5fa returns a wrong host");
        assertEquals(-1, u1f.getPort(), "5fa returns a wrong port");
        assertEquals("/file.java", u1f.getFile(), "5fa returns a wrong file");
        assertNull(u1f.getRef(), "5fa returns a wrong anchor");

        uf = new URL("file:testing");
        u1f = new URL(uf, "file.java");
        assertEquals("file", u1f.getProtocol(), "5fb returns a wrong protocol");
        assertTrue(u1f.getHost().equals(""), "5fb returns a wrong host");
        assertEquals(-1, u1f.getPort(), "5fb returns a wrong port");
        assertEquals("file.java", u1f.getFile(), "5fb returns a wrong file");
        assertNull(u1f.getRef(), "5fb returns a wrong anchor");

        u = new URL("http://www.apache.org/testing/foobaz");
        u1 = new URL(u, "/file.java");
        assertEquals("http", u1.getProtocol(), "6 returns a wrong protocol");
        assertEquals("www.apache.org", u1.getHost(), "6 returns a wrong host");
        assertEquals(-1, u1.getPort(), "6 returns a wrong port");
        assertEquals("/file.java", u1.getFile(), "6 returns a wrong file");
        assertNull(u1.getRef(), "6 returns a wrong anchor");

        uf = new URL("file://www.apache.org/testing/foobaz");
        u1f = new URL(uf, "/file.java");
        assertEquals("file", u1f.getProtocol(), "6f returns a wrong protocol");
        assertEquals("www.apache.org", u1f.getHost(), "6f returns a wrong host");
        assertEquals(-1, u1f.getPort(), "6f returns a wrong port");
        assertEquals("/file.java", u1f.getFile(), "6f returns a wrong file");
        assertNull(u1f.getRef(), "6f returns a wrong anchor");

        u = new URL("http://www.apache.org:8000/testing/foobaz");
        u1 = new URL(u, "/file.java");
        assertEquals("http", u1.getProtocol(), "7 returns a wrong protocol");
        assertEquals("www.apache.org", u1.getHost(), "7 returns a wrong host");
        assertEquals(8000, u1.getPort(), "7 returns a wrong port");
        assertEquals("/file.java", u1.getFile(), "7 returns a wrong file");
        assertNull(u1.getRef(), "7 returns a wrong anchor");

        u = new URL("http://www.apache.org/index.html");
        u1 = new URL(u, "#bar");
        assertEquals("www.apache.org", u1.getHost(), "8 returns a wrong host");
        assertEquals("/index.html", u1.getFile(), "8 returns a wrong file");
        assertEquals("bar", u1.getRef(), "8 returns a wrong anchor");

        u = new URL("http://www.apache.org/index.html#foo");
        u1 = new URL(u, "http:#bar");
        assertEquals("www.apache.org", u1.getHost(), "9 returns a wrong host");
        assertEquals("/index.html", u1.getFile(), "9 returns a wrong file");
        assertEquals("bar", u1.getRef(), "9 returns a wrong anchor");

        u = new URL("http://www.apache.org/index.html");
        u1 = new URL(u, "");
        assertEquals("www.apache.org", u1.getHost(), "10 returns a wrong host");
        assertEquals("/index.html", u1.getFile(), "10 returns a wrong file");
        assertNull(u1.getRef(), "10 returns a wrong anchor");

        uf = new URL("file://www.apache.org/index.html");
        u1f = new URL(uf, "");
        assertEquals("www.apache.org", u1.getHost(), "10f returns a wrong host");
        assertEquals("/index.html", u1.getFile(), "10f returns a wrong file");
        assertNull(u1.getRef(), "10f returns a wrong anchor");

        u = new URL("http://www.apache.org/index.html");
        u1 = new URL(u, "http://www.apache.org");
        assertEquals("www.apache.org", u1.getHost(), "11 returns a wrong host");
        assertTrue(u1.getFile().equals(""), "11 returns a wrong file");
        assertNull(u1.getRef(), "11 returns a wrong anchor");

        // test for question mark processing
        u = new URL("http://www.foo.com/d0/d1/d2/cgi-bin?foo=bar/baz");

        // test for relative file and out of bound "/../" processing
        u1 = new URL(u, "../dir1/./dir2/../file.java");
        assertTrue(u1.getFile().equals("/d0/d1/dir1/file.java"), "A) returns a wrong file: " + u1.getFile());

        // test for absolute and relative file processing
        u1 = new URL(u, "/../dir1/./dir2/../file.java");
        assertEquals("/../dir1/./dir2/../file.java", u1.getFile(),  "B) returns a wrong file");

        try {
            // u should raise a MalFormedURLException because u, the context is
            // null
            u = null;
            u1 = new URL(u, "file.java");
            fail("didn't throw the expected MalFormedURLException");
        } catch (MalformedURLException e) {
            // valid
        }
    }

    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_StringLjava_lang_String()
            throws MalformedURLException {

        u = new URL("http", "www.yahoo.com", "test.html#foo");
        assertEquals("http", u.getProtocol());
        assertEquals("www.yahoo.com", u.getHost());
        assertEquals(-1, u.getPort());
        assertEquals("test.html", u.getFile());
        assertEquals("foo", u.getRef());
    }

    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_StringILjava_lang_String() throws MalformedURLException {
        u = new URL("http", "www.yahoo.com", 8080, "test.html#foo");
        assertEquals("http", u.getProtocol(), "SSIS returns a wrong protocol");
        assertEquals("www.yahoo.com", u.getHost(), "SSIS returns a wrong host");
        assertEquals(8080, u.getPort(), "SSIS returns a wrong port");
        assertEquals("test.html", u.getFile(), "SSIS returns a wrong file");
        assertTrue(u.getRef().equals("foo"), "SSIS returns a wrong anchor: " + u.getRef());

        // Regression for HARMONY-83
        new URL("http", "apache.org", 123456789, "file");
        try {
            new URL("http", "apache.org", -123, "file");
            fail("Assert 0: Negative port should throw exception");
        } catch (MalformedURLException e) {
            // expected
        }

    }

    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_StringILjava_lang_StringLjava_net_URLStreamHandler()
            throws Exception {
        // Test for method java.net.URL(java.lang.String, java.lang.String, int,
        // java.lang.String, java.net.URLStreamHandler)
        u = new URL("http", "www.yahoo.com", 8080, "test.html#foo", null);
        assertEquals("http", u.getProtocol(), "SSISH1 returns a wrong protocol");
        assertEquals("www.yahoo.com", u.getHost(), "SSISH1 returns a wrong host");
        assertEquals(8080, u.getPort(), "SSISH1 returns a wrong port");
        assertEquals("test.html", u.getFile(), "SSISH1 returns a wrong file");
        assertTrue(u.getRef().equals("foo"), "SSISH1 returns a wrong anchor: " + u.getRef());
    }

    @Test
    public void test_equalsLjava_lang_Object() throws MalformedURLException {
        u = new URL("http://www.apache.org:8080/dir::23??????????test.html");
        u1 = new URL("http://www.apache.org:8080/dir::23??????????test.html");
        assertTrue(u.equals(u1), "A) equals returns false for two identical URLs");
        assertTrue(!u1.equals(null), "return true for null comparison");
        u = new URL("ftp://www.apache.org:8080/dir::23??????????test.html");
        assertTrue(!u.equals(u1), "Returned true for non-equal URLs");

        // Regression for HARMONY-6556
        u = new URL("file", null, 0, "/test.txt");
        u1 = new URL("file", null, 0, "/test.txt");
        assertEquals(u, u1);

        u = new URL("file", "first.invalid", 0, "/test.txt");
        u1 = new URL("file", "second.invalid", 0, "/test.txt");
        assertFalse(u.equals(u1));
    }

    @Test
    public void test_sameFileLjava_net_URL() throws Exception {
        // Test for method boolean java.net.URL.sameFile(java.net.URL)
        u = new URL("http://www.yahoo.com");
        u1 = new URL("http", "www.yahoo.com", "");
        assertTrue(u.sameFile(u1), "Should be the same1");
        u = new URL("http://www.yahoo.com/dir1/dir2/test.html#anchor1");
        u1 = new URL("http://www.yahoo.com/dir1/dir2/test.html#anchor2");
        assertTrue(u.sameFile(u1), "Should be the same ");

        // regression test for Harmony-1040
        u = new URL("file", null, -1, "/d:/somedir/");
        u1 = new URL("file:/d:/somedir/");
        assertFalse(u.sameFile(u1));

        // regression test for Harmony-2136
        URL url1 = new URL("file:///anyfile");
        URL url2 = new URL("file://localhost/anyfile");
        assertTrue(url1.sameFile(url2));

        url1 = new URL("http:///anyfile");
        url2 = new URL("http://localhost/anyfile");
        assertFalse(url1.sameFile(url2));

        url1 = new URL("ftp:///anyfile");
        url2 = new URL("ftp://localhost/anyfile");
        assertFalse(url1.sameFile(url2));

        url1 = new URL("jar:file:///anyfile.jar!/");
        url2 = new URL("jar:file://localhost/anyfile.jar!/");
        assertFalse(url1.sameFile(url2));
    }

    @Test
    public void test_toString() {
        // Test for method java.lang.String java.net.URL.toString()
        try {
            u1 = new URL("http://www.yahoo2.com:9999");
            u = new URL("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1");
            assertEquals("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1", u.toString(),
                    "a) Does not return the right url string");
            assertEquals("http://www.yahoo2.com:9999", u1.toString(), "b) Does not return the right url string");
            assertEquals(u, new URL(u.toString()), "c) Does not return the right url string");
        } catch (Exception e) {
            // Do nothing
        }
    }

    @Test
    public void test_toExternalForm() {
        try {
            u1 = new URL("http://www.yahoo2.com:9999");
            u = new URL("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1");
            assertEquals("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1", u.toString(),
                    "a) Does not return the right url string");
            assertEquals("http://www.yahoo2.com:9999", u1.toString(), "b) Does not return the right url string");
            assertTrue(u.equals(new URL(u.toString())), "c) Does not return the right url string");

            u = new URL("http:index");
            assertEquals("http:index", u.toExternalForm(), "2 wrong external form");

            u = new URL("http", null, "index");
            assertEquals("http:index", u.toExternalForm(), "2 wrong external form");
        } catch (Exception e) {
            // Do nothing
        }
    }

    @Test
    public void test_getFile() throws Exception {
        // Test for method java.lang.String java.net.URL.getFile()
        u = new URL("http", "www.yahoo.com", 1233, "test/!@$%^&*/test.html#foo");
        assertEquals("test/!@$%^&*/test.html", u.getFile(), "returns a wrong file");
        u = new URL("http", "www.yahoo.com", 1233, "");
        assertTrue(u.getFile().equals(""), "returns a wrong file");
    }

    @Test
    public void test_getPort() throws Exception {
        // Test for method int java.net.URL.getPort()
        u = new URL("http://member12.c++.com:9999");
        assertTrue(u.getPort() == 9999, "return wrong port number " + u.getPort());
        u = new URL("http://member12.c++.com:9999/");
        assertEquals(9999, u.getPort(), "return wrong port number");
    }

    @Test
    public void test_getDefaultPort() throws MalformedURLException {
        u = new URL("http://member12.c++.com:9999");
        assertEquals(80, u.getDefaultPort());
        u = new URL("ftp://member12.c++.com:9999/");
        assertEquals(21, u.getDefaultPort());
    }

    @Test
    public void test_getProtocol() throws Exception {
        // Test for method java.lang.String java.net.URL.getProtocol()
        u = new URL("http://www.yahoo2.com:9999");
        assertTrue(u.getProtocol().equals("http"), "u returns a wrong protocol: " + u.getProtocol());
    }

    @Test
    public void test_getRef() {
        // Test for method java.lang.String java.net.URL.getRef()
        try {
            u1 = new URL("http://www.yahoo2.com:9999");
            u = new URL("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1");
            assertEquals("anchor1", u.getRef(), "returns a wrong anchor1");
            assertNull(u1.getRef(), "returns a wrong anchor2");
            u1 = new URL("http://www.yahoo2.com#ref");
            assertEquals("ref", u1.getRef(), "returns a wrong anchor3");
            u1 = new URL("http://www.yahoo2.com/file#ref1#ref2");
            assertEquals("ref1#ref2", u1.getRef(), "returns a wrong anchor4");
        } catch (MalformedURLException e) {
            fail("Incorrect URL format : " + e.getMessage());
        }
    }

    @Test
    public void test_getAuthority() throws MalformedURLException {
        URL testURL = new URL("http", "hostname", 80, "/java?q1#ref");
        assertEquals("hostname:80", testURL.getAuthority());
        assertEquals("hostname", testURL.getHost());
        assertNull(testURL.getUserInfo());
        assertEquals("/java?q1", testURL.getFile());
        assertEquals("/java", testURL.getPath());
        assertEquals("q1", testURL.getQuery());
        assertEquals("ref", testURL.getRef());

        testURL = new URL("http", "home", -1, "/java");
        assertEquals("home", testURL.getAuthority(), "wrong authority2");
        assertNull(testURL.getUserInfo(), "wrong userInfo2");
        assertEquals("home", testURL.getHost(), "wrong host2");
        assertEquals("/java", testURL.getFile(), "wrong file2");
        assertEquals("/java", testURL.getPath(), "wrong path2");
        assertNull(testURL.getQuery(), "wrong query2");
        assertNull(testURL.getRef(), "wrong ref2");
    }

    @Test
    public void test_toURI() throws Exception {
        u = new URL("http://www.apache.org");
        URI uri = u.toURI();
        assertTrue(u.equals(uri.toURL()));
    }

    @Test
    public void test_ConstructorLnullLjava_lang_StringILjava_lang_String() throws Exception {
        // Regression for HARMONY-1131
        try {
            new URL(null, "1", 0, "file");
            fail("NullPointerException expected, but nothing was thrown!");
        } catch (NullPointerException e) {
            // Expected NullPointerException
        }
    }

    @Test
    public void test_ConstructorLnullLjava_lang_StringLjava_lang_String() throws Exception {
        // Regression for HARMONY-1131
        try {
            new URL(null, "1", "file");
            fail("NullPointerException expected, but nothing was thrown!");
        } catch (NullPointerException e) {
            // Expected NullPointerException
        }
    }

    @Test
    public void test_toExternalForm_Absolute() throws MalformedURLException {
        String strURL = "http://localhost?name=value";
        URL url = new URL(strURL);
        assertEquals(strURL, url.toExternalForm());

        strURL = "http://localhost?name=value/age=12";
        url = new URL(strURL);
        assertEquals(strURL, url.toExternalForm());
    }

    @Test
    public void test_toExternalForm_Relative() throws MalformedURLException {
        String strURL = "http://a/b/c/d;p?q";
        String ref = "?y";
        URL url = new URL(new URL(strURL), ref);
        assertEquals("http://a/b/c/?y", url.toExternalForm());
    }

    // Regression test for HARMONY-6254

    // Bogus handler forces file part of URL to be null
    static class MyHandler2 extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL arg0) throws IOException {
            return null;
        }

        @Override
        protected void setURL(URL u, String protocol, String host, int port,
                String authority, String userInfo, String file, String query,
                String ref) {
            super.setURL(u, protocol, host, port, authority, userInfo, null, query, ref);
        }
    }

    @Test
    public void test_toExternalForm_Null() throws IOException {
        URLStreamHandler myHandler = new MyHandler2();
        URL url = new URL(null, "foobar://example.com/foobar", myHandler);
        String s = url.toExternalForm();
        assertEquals("foobar://example.com", s, "Got wrong URL external form");
    }

    static class MyURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL arg0) throws IOException {
            return null;
        }
    }

    static class MyURLStreamHandlerFactory implements URLStreamHandlerFactory {

        public static MyURLStreamHandler handler = new MyURLStreamHandler();

        @Override
        public URLStreamHandler createURLStreamHandler(String arg0) {
            handler = new MyURLStreamHandler();
            return handler;
        }
    }
}
