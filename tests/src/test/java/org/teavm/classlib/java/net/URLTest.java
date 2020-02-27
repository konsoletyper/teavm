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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
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
        assertEquals("u returns a wrong protocol", "http", u.getProtocol());
        assertEquals("u returns a wrong host", "www.yahoo1.com", u.getHost());
        assertEquals("u returns a wrong port", 8080, u.getPort());
        assertEquals("u returns a wrong file", "/dir1/dir2/test.cgi?point1.html", u.getFile());
        assertEquals("u returns a wrong anchor", "anchor1", u.getRef());

        // test for no file
        u1 = new URL("http://www.yahoo2.com:9999");
        assertEquals("u1 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("u1 returns a wrong host", "www.yahoo2.com", u1.getHost());
        assertEquals("u1 returns a wrong port", 9999, u1.getPort());
        assertTrue("u1 returns a wrong file", u1.getFile().equals(""));
        assertNull("u1 returns a wrong anchor", u1.getRef());

        // test for no port
        u2 = new URL("http://www.yahoo3.com/dir1/dir2/test.cgi?point1.html#anchor1");
        assertEquals("u2 returns a wrong protocol", "http", u2.getProtocol());
        assertEquals("u2 returns a wrong host", "www.yahoo3.com", u2.getHost());
        assertEquals("u2 returns a wrong port", -1, u2.getPort());
        assertEquals("u2 returns a wrong file", "/dir1/dir2/test.cgi?point1.html", u2.getFile());
        assertEquals("u2 returns a wrong anchor", "anchor1", u2.getRef());

        // test for no port
        URL u2a = new URL("file://www.yahoo3.com/dir1/dir2/test.cgi#anchor1");
        assertEquals("u2a returns a wrong protocol", "file", u2a.getProtocol());
        assertEquals("u2a returns a wrong host", "www.yahoo3.com", u2a.getHost());
        assertEquals("u2a returns a wrong port", -1, u2a.getPort());
        assertEquals("u2a returns a wrong file", "/dir1/dir2/test.cgi", u2a.getFile());
        assertEquals("u2a returns a wrong anchor", "anchor1", u2a.getRef());

        // test for no file, no port
        u3 = new URL("http://www.yahoo4.com/");
        assertEquals("u3 returns a wrong protocol", "http", u3.getProtocol());
        assertEquals("u3 returns a wrong host", "www.yahoo4.com", u3.getHost());
        assertEquals("u3 returns a wrong port", -1, u3.getPort());
        assertEquals("u3 returns a wrong file", "/", u3.getFile());
        assertNull("u3 returns a wrong anchor", u3.getRef());

        // test for no file, no port
        URL u3a = new URL("file://www.yahoo4.com/");
        assertEquals("u3a returns a wrong protocol", "file", u3a.getProtocol());
        assertEquals("u3a returns a wrong host", "www.yahoo4.com", u3a.getHost());
        assertEquals("u3a returns a wrong port", -1, u3a.getPort());
        assertEquals("u3a returns a wrong file", "/", u3a.getFile());
        assertNull("u3a returns a wrong anchor", u3a.getRef());

        // test for no file, no port
        URL u3b = new URL("file://www.yahoo4.com");
        assertEquals("u3b returns a wrong protocol", "file", u3b.getProtocol());
        assertEquals("u3b returns a wrong host", "www.yahoo4.com", u3b.getHost());
        assertEquals("u3b returns a wrong port", -1, u3b.getPort());
        assertTrue("u3b returns a wrong file", u3b.getFile().equals(""));
        assertNull("u3b returns a wrong anchor", u3b.getRef());

        // test for non-port ":" and wierd characters occurrences
        u4 = new URL("http://www.yahoo5.com/di!@$%^&*()_+r1/di:::r2/test.cgi?point1.html#anchor1");
        assertEquals("u4 returns a wrong protocol", "http", u4.getProtocol());
        assertEquals("u4 returns a wrong host", "www.yahoo5.com", u4.getHost());
        assertEquals("u4 returns a wrong port", -1, u4.getPort());
        assertEquals("u4 returns a wrong file", "/di!@$%^&*()_+r1/di:::r2/test.cgi?point1.html", u4.getFile());
        assertEquals("u4 returns a wrong anchor", "anchor1", u4.getRef());

        u5 = new URL("file:/testing.tst");
        assertEquals("u5 returns a wrong protocol", "file", u5.getProtocol());
        assertTrue("u5 returns a wrong host", u5.getHost().equals(""));
        assertEquals("u5 returns a wrong port", -1, u5.getPort());
        assertEquals("u5 returns a wrong file", "/testing.tst", u5.getFile());
        assertNull("u5 returns a wrong anchor", u5.getRef());

        URL u5a = new URL("file:testing.tst");
        assertEquals("u5a returns a wrong protocol", "file", u5a.getProtocol());
        assertTrue("u5a returns a wrong host", u5a.getHost().equals(""));
        assertEquals("u5a returns a wrong port", -1, u5a.getPort());
        assertEquals("u5a returns a wrong file", "testing.tst", u5a.getFile());
        assertNull("u5a returns a wrong anchor", u5a.getRef());

        URL u6 = new URL("http://host:/file");
        assertEquals("u6 return a wrong port", -1, u6.getPort());

        URL u7 = new URL("file:../../file.txt");
        assertTrue("u7 returns a wrong file: " + u7.getFile(), u7.getFile().equals("../../file.txt"));

        URL u8 = new URL("http://[fec0::1:20d:60ff:fe24:7410]:35/file.txt");
        assertTrue("u8 returns a wrong protocol " + u8.getProtocol(), u8.getProtocol().equals("http"));
        assertTrue("u8 returns a wrong host " + u8.getHost(), u8.getHost().equals("[fec0::1:20d:60ff:fe24:7410]"));
        assertTrue("u8 returns a wrong port " + u8.getPort(), u8.getPort() == 35);
        assertTrue("u8 returns a wrong file " + u8.getFile(), u8.getFile().equals("/file.txt"));
        assertNull("u8 returns a wrong anchor " + u8.getRef(), u8.getRef());

        URL u9 = new URL("file://[fec0::1:20d:60ff:fe24:7410]/file.txt#sogood");
        assertTrue("u9 returns a wrong protocol " + u9.getProtocol(), u9.getProtocol().equals("file"));
        assertTrue("u9 returns a wrong host " + u9.getHost(), u9.getHost().equals("[fec0::1:20d:60ff:fe24:7410]"));
        assertTrue("u9 returns a wrong port " + u9.getPort(), u9.getPort() == -1);
        assertTrue("u9 returns a wrong file " + u9.getFile(), u9.getFile().equals("/file.txt"));
        assertTrue("u9 returns a wrong anchor " + u9.getRef(), u9.getRef().equals("sogood"));

        URL u10 = new URL("file://[fec0::1:20d:60ff:fe24:7410]");
        assertTrue("u10 returns a wrong protocol " + u10.getProtocol(), u10.getProtocol().equals("file"));
        assertTrue("u10 returns a wrong host " + u10.getHost(), u10.getHost()
                .equals("[fec0::1:20d:60ff:fe24:7410]"));
        assertTrue("u10 returns a wrong port " + u10.getPort(), u10.getPort() == -1);

        URL u11 = new URL("file:////file.txt");
        assertNull("u11 returns a wrong authority " + u11.getAuthority(), u11.getAuthority());
        assertTrue("u11 returns a wrong file " + u11.getFile(), u11.getFile().equals("////file.txt"));

        URL u12 = new URL("file:///file.txt");
        assertTrue("u12 returns a wrong authority", u12.getAuthority().equals(""));
        assertTrue("u12 returns a wrong file " + u12.getFile(), u12.getFile().equals("/file.txt"));


        // test for error catching

        // Bad HTTP format - no "//"
        u = new URL("http:www.yahoo5.com::22/dir1/di:::r2/test.cgi?point1.html#anchor1");

        caught = false;
        try {
            u = new URL("http://www.yahoo5.com::22/dir1/di:::r2/test.cgi?point1.html#anchor1");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue("Should have throw MalformedURLException", caught);

        // unknown protocol
        try {
            u = new URL("myProtocol://www.yahoo.com:22");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue("3 Failed to throw MalformedURLException", caught);

        caught = false;
        // no protocol
        try {
            u = new URL("www.yahoo.com");
        } catch (MalformedURLException e) {
            caught = true;
        }
        assertTrue("4 Failed to throw MalformedURLException", caught);

        caught = false;

        URL u1 = null;
        try {
            // No leading or trailing spaces.
            u1 = new URL("file:/some/path");
            assertEquals("5 got wrong file length1", 10, u1.getFile().length());

            // Leading spaces.
            u1 = new URL("  file:/some/path");
            assertEquals("5 got wrong file length2", 10, u1.getFile().length());

            // Trailing spaces.
            u1 = new URL("file:/some/path  ");
            assertEquals("5 got wrong file length3", 10, u1.getFile().length());

            // Leading and trailing.
            u1 = new URL("  file:/some/path ");
            assertEquals("5 got wrong file length4", 10, u1.getFile().length());

            // in-place spaces.
            u1 = new URL("  file:  /some/path ");
            assertEquals("5 got wrong file length5", 12, u1.getFile().length());

        } catch (MalformedURLException e) {
            fail("5 Did not expect the exception " + e);
        }

        // testing jar protocol with relative path
        // to make sure it's not canonicalized
        try {
            String file = "file:/a!/b/../d";

            u = new URL("jar:" + file);
            assertEquals("Wrong file (jar protocol, relative path)", file, u.getFile());
        } catch (MalformedURLException e) {
            fail("Unexpected exception (jar protocol, relative path)" + e);
        }
    }

    @Test
    public void test_ConstructorLjava_net_URLLjava_lang_String() throws Exception {
        // Test for method java.net.URL(java.net.URL, java.lang.String)
        u = new URL("http://www.yahoo.com");
        URL uf = new URL("file://www.yahoo.com");
        // basic ones
        u1 = new URL(u, "file.java");
        assertEquals("1 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("1 returns a wrong host", "www.yahoo.com", u1.getHost());
        assertEquals("1 returns a wrong port", -1, u1.getPort());
        assertEquals("1 returns a wrong file", "/file.java", u1.getFile());
        assertNull("1 returns a wrong anchor", u1.getRef());

        URL u1f = new URL(uf, "file.java");
        assertEquals("1f returns a wrong protocol", "file", u1f.getProtocol());
        assertEquals("1f returns a wrong host", "www.yahoo.com", u1f.getHost());
        assertEquals("1f returns a wrong port", -1, u1f.getPort());
        assertEquals("1f returns a wrong file", "/file.java", u1f.getFile());
        assertNull("1f returns a wrong anchor", u1f.getRef());

        u1 = new URL(u, "dir1/dir2/../file.java");
        assertEquals("3 returns a wrong protocol", "http", u1.getProtocol());
        assertTrue("3 returns a wrong host: " + u1.getHost(), u1.getHost().equals("www.yahoo.com"));
        assertEquals("3 returns a wrong port", -1, u1.getPort());
        assertEquals("3 returns a wrong file", "/dir1/dir2/../file.java", u1.getFile());
        assertNull("3 returns a wrong anchor", u1.getRef());

        u1 = new URL(u, "http:dir1/dir2/../file.java");
        assertEquals("3a returns a wrong protocol", "http", u1.getProtocol());
        assertTrue("3a returns a wrong host: " + u1.getHost(), u1.getHost().equals(""));
        assertEquals("3a returns a wrong port", -1, u1.getPort());
        assertEquals("3a returns a wrong file", "dir1/dir2/../file.java", u1.getFile());
        assertNull("3a returns a wrong anchor", u1.getRef());

        u = new URL("http://www.apache.org/testing/");
        u1 = new URL(u, "file.java");
        assertEquals("4 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("4 returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("4 returns a wrong port", -1, u1.getPort());
        assertEquals("4 returns a wrong file", "/testing/file.java", u1.getFile());
        assertNull("4 returns a wrong anchor", u1.getRef());

        uf = new URL("file://www.apache.org/testing/");
        u1f = new URL(uf, "file.java");
        assertEquals("4f returns a wrong protocol", "file", u1f.getProtocol());
        assertEquals("4f returns a wrong host", "www.apache.org", u1f.getHost());
        assertEquals("4f returns a wrong port", -1, u1f.getPort());
        assertEquals("4f returns a wrong file", "/testing/file.java", u1f.getFile());
        assertNull("4f returns a wrong anchor", u1f.getRef());

        uf = new URL("file:/testing/");
        u1f = new URL(uf, "file.java");
        assertEquals("4fa returns a wrong protocol", "file", u1f.getProtocol());
        assertTrue("4fa returns a wrong host", u1f.getHost().equals(""));
        assertEquals("4fa returns a wrong port", -1, u1f.getPort());
        assertEquals("4fa returns a wrong file", "/testing/file.java", u1f.getFile());
        assertNull("4fa returns a wrong anchor", u1f.getRef());

        uf = new URL("file:testing/");
        u1f = new URL(uf, "file.java");
        assertEquals("4fb returns a wrong protocol", "file", u1f.getProtocol());
        assertTrue("4fb returns a wrong host", u1f.getHost().equals(""));
        assertEquals("4fb returns a wrong port", -1, u1f.getPort());
        assertEquals("4fb returns a wrong file", "testing/file.java", u1f.getFile());
        assertNull("4fb returns a wrong anchor", u1f.getRef());

        u1f = new URL(uf, "file:file.java");
        assertEquals("4fc returns a wrong protocol", "file", u1f.getProtocol());
        assertTrue("4fc returns a wrong host", u1f.getHost().equals(""));
        assertEquals("4fc returns a wrong port", -1, u1f.getPort());
        assertEquals("4fc returns a wrong file", "file.java", u1f.getFile());
        assertNull("4fc returns a wrong anchor", u1f.getRef());

        u1f = new URL(uf, "file:");
        assertEquals("4fd returns a wrong protocol", "file", u1f.getProtocol());
        assertTrue("4fd returns a wrong host", u1f.getHost().equals(""));
        assertEquals("4fd returns a wrong port", -1, u1f.getPort());
        assertTrue("4fd returns a wrong file", u1f.getFile().equals(""));
        assertNull("4fd returns a wrong anchor", u1f.getRef());

        u = new URL("http://www.apache.org/testing");
        u1 = new URL(u, "file.java");
        assertEquals("5 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("5 returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("5 returns a wrong port", -1, u1.getPort());
        assertEquals("5 returns a wrong file", "/file.java", u1.getFile());
        assertNull("5 returns a wrong anchor", u1.getRef());

        uf = new URL("file://www.apache.org/testing");
        u1f = new URL(uf, "file.java");
        assertEquals("5f returns a wrong protocol", "file", u1f.getProtocol());
        assertEquals("5f returns a wrong host", "www.apache.org", u1f.getHost());
        assertEquals("5f returns a wrong port", -1, u1f.getPort());
        assertEquals("5f returns a wrong file", "/file.java", u1f.getFile());
        assertNull("5f returns a wrong anchor", u1f.getRef());

        uf = new URL("file:/testing");
        u1f = new URL(uf, "file.java");
        assertEquals("5fa returns a wrong protocol", "file", u1f.getProtocol());
        assertTrue("5fa returns a wrong host", u1f.getHost().equals(""));
        assertEquals("5fa returns a wrong port", -1, u1f.getPort());
        assertEquals("5fa returns a wrong file", "/file.java", u1f.getFile());
        assertNull("5fa returns a wrong anchor", u1f.getRef());

        uf = new URL("file:testing");
        u1f = new URL(uf, "file.java");
        assertEquals("5fb returns a wrong protocol", "file", u1f.getProtocol());
        assertTrue("5fb returns a wrong host", u1f.getHost().equals(""));
        assertEquals("5fb returns a wrong port", -1, u1f.getPort());
        assertEquals("5fb returns a wrong file", "file.java", u1f.getFile());
        assertNull("5fb returns a wrong anchor", u1f.getRef());

        u = new URL("http://www.apache.org/testing/foobaz");
        u1 = new URL(u, "/file.java");
        assertEquals("6 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("6 returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("6 returns a wrong port", -1, u1.getPort());
        assertEquals("6 returns a wrong file", "/file.java", u1.getFile());
        assertNull("6 returns a wrong anchor", u1.getRef());

        uf = new URL("file://www.apache.org/testing/foobaz");
        u1f = new URL(uf, "/file.java");
        assertEquals("6f returns a wrong protocol", "file", u1f.getProtocol());
        assertEquals("6f returns a wrong host", "www.apache.org", u1f.getHost());
        assertEquals("6f returns a wrong port", -1, u1f.getPort());
        assertEquals("6f returns a wrong file", "/file.java", u1f.getFile());
        assertNull("6f returns a wrong anchor", u1f.getRef());

        u = new URL("http://www.apache.org:8000/testing/foobaz");
        u1 = new URL(u, "/file.java");
        assertEquals("7 returns a wrong protocol", "http", u1.getProtocol());
        assertEquals("7 returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("7 returns a wrong port", 8000, u1.getPort());
        assertEquals("7 returns a wrong file", "/file.java", u1.getFile());
        assertNull("7 returns a wrong anchor", u1.getRef());

        u = new URL("http://www.apache.org/index.html");
        u1 = new URL(u, "#bar");
        assertEquals("8 returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("8 returns a wrong file", "/index.html", u1.getFile());
        assertEquals("8 returns a wrong anchor", "bar", u1.getRef());

        u = new URL("http://www.apache.org/index.html#foo");
        u1 = new URL(u, "http:#bar");
        assertEquals("9 returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("9 returns a wrong file", "/index.html", u1.getFile());
        assertEquals("9 returns a wrong anchor", "bar", u1.getRef());

        u = new URL("http://www.apache.org/index.html");
        u1 = new URL(u, "");
        assertEquals("10 returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("10 returns a wrong file", "/index.html", u1.getFile());
        assertNull("10 returns a wrong anchor", u1.getRef());

        uf = new URL("file://www.apache.org/index.html");
        u1f = new URL(uf, "");
        assertEquals("10f returns a wrong host", "www.apache.org", u1.getHost());
        assertEquals("10f returns a wrong file", "/index.html", u1.getFile());
        assertNull("10f returns a wrong anchor", u1.getRef());

        u = new URL("http://www.apache.org/index.html");
        u1 = new URL(u, "http://www.apache.org");
        assertEquals("11 returns a wrong host", "www.apache.org", u1.getHost());
        assertTrue("11 returns a wrong file", u1.getFile().equals(""));
        assertNull("11 returns a wrong anchor", u1.getRef());

        // test for question mark processing
        u = new URL("http://www.foo.com/d0/d1/d2/cgi-bin?foo=bar/baz");

        // test for relative file and out of bound "/../" processing
        u1 = new URL(u, "../dir1/./dir2/../file.java");
        assertTrue("A) returns a wrong file: " + u1.getFile(), u1.getFile().equals("/d0/d1/dir1/file.java"));

        // test for absolute and relative file processing
        u1 = new URL(u, "/../dir1/./dir2/../file.java");
        assertEquals("B) returns a wrong file", "/../dir1/./dir2/../file.java",  u1.getFile());

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

        // Strange behavior in reference, the hostname contains a ':' so it gets
        // wrapped in '[', ']'
        URL testURL = new URL("http", "www.apache.org:8080", "test.html#anch");
        assertEquals("wrong protocol", "http", testURL.getProtocol());
        assertEquals("wrong host", "[www.apache.org:8080]", testURL.getHost());
        assertEquals("wrong port", -1, testURL.getPort());
        assertEquals("wrong file", "test.html", testURL.getFile());
        assertEquals("wrong anchor", "anch", testURL.getRef());
    }

    @Test
    public void test_ConstructorLjava_lang_StringLjava_lang_StringILjava_lang_String() throws MalformedURLException {
        u = new URL("http", "www.yahoo.com", 8080, "test.html#foo");
        assertEquals("SSIS returns a wrong protocol", "http", u.getProtocol());
        assertEquals("SSIS returns a wrong host", "www.yahoo.com", u.getHost());
        assertEquals("SSIS returns a wrong port", 8080, u.getPort());
        assertEquals("SSIS returns a wrong file", "test.html", u.getFile());
        assertTrue("SSIS returns a wrong anchor: " + u.getRef(), u.getRef().equals("foo"));

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
        assertEquals("SSISH1 returns a wrong protocol", "http", u.getProtocol());
        assertEquals("SSISH1 returns a wrong host", "www.yahoo.com", u.getHost());
        assertEquals("SSISH1 returns a wrong port", 8080, u.getPort());
        assertEquals("SSISH1 returns a wrong file", "test.html", u.getFile());
        assertTrue("SSISH1 returns a wrong anchor: " + u.getRef(), u.getRef().equals("foo"));
    }

    @Test
    public void test_equalsLjava_lang_Object() throws MalformedURLException {
        u = new URL("http://www.apache.org:8080/dir::23??????????test.html");
        u1 = new URL("http://www.apache.org:8080/dir::23??????????test.html");
        assertTrue("A) equals returns false for two identical URLs", u.equals(u1));
        assertTrue("return true for null comparison", !u1.equals(null));
        u = new URL("ftp://www.apache.org:8080/dir::23??????????test.html");
        assertTrue("Returned true for non-equal URLs", !u.equals(u1));

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
        assertTrue("Should be the same1", u.sameFile(u1));
        u = new URL("http://www.yahoo.com/dir1/dir2/test.html#anchor1");
        u1 = new URL("http://www.yahoo.com/dir1/dir2/test.html#anchor2");
        assertTrue("Should be the same ", u.sameFile(u1));

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
            assertEquals(
                    "a) Does not return the right url string",
                    "http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1",
                    u.toString());
            assertEquals("b) Does not return the right url string",
                    "http://www.yahoo2.com:9999", u1.toString());
            assertTrue("c) Does not return the right url string", u
                    .equals(new URL(u.toString())));
        } catch (Exception e) {
            // Do nothing
        }
    }

    @Test
    public void test_toExternalForm() {
        try {
            u1 = new URL("http://www.yahoo2.com:9999");
            u = new URL("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1");
            assertEquals(
                    "a) Does not return the right url string",
                    "http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1",
                    u.toString());
            assertEquals("b) Does not return the right url string", "http://www.yahoo2.com:9999", u1.toString());
            assertTrue("c) Does not return the right url string", u.equals(new URL(u.toString())));

            u = new URL("http:index");
            assertEquals("2 wrong external form", "http:index", u.toExternalForm());

            u = new URL("http", null, "index");
            assertEquals("2 wrong external form", "http:index", u.toExternalForm());
        } catch (Exception e) {
            // Do nothing
        }
    }

    @Test
    public void test_getFile() throws Exception {
        // Test for method java.lang.String java.net.URL.getFile()
        u = new URL("http", "www.yahoo.com:8080", 1233, "test/!@$%^&*/test.html#foo");
        assertEquals("returns a wrong file", "test/!@$%^&*/test.html", u.getFile());
        u = new URL("http", "www.yahoo.com:8080", 1233, "");
        assertTrue("returns a wrong file", u.getFile().equals(""));
    }

    @Test
    public void test_getHost() throws MalformedURLException {
        // Regression for HARMONY-60
        String ipv6Host = "FEDC:BA98:7654:3210:FEDC:BA98:7654:3210";
        URL url = new URL("http", ipv6Host, -1, "myfile");
        assertEquals("[" + ipv6Host + "]", url.getHost());
    }

    @Test
    public void test_getPort() throws Exception {
        // Test for method int java.net.URL.getPort()
        u = new URL("http://member12.c++.com:9999");
        assertTrue("return wrong port number " + u.getPort(), u.getPort() == 9999);
        u = new URL("http://member12.c++.com:9999/");
        assertEquals("return wrong port number", 9999, u.getPort());
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
        assertTrue("u returns a wrong protocol: " + u.getProtocol(), u.getProtocol().equals("http"));
    }

    @Test
    public void test_getRef() {
        // Test for method java.lang.String java.net.URL.getRef()
        try {
            u1 = new URL("http://www.yahoo2.com:9999");
            u = new URL("http://www.yahoo1.com:8080/dir1/dir2/test.cgi?point1.html#anchor1");
            assertEquals("returns a wrong anchor1", "anchor1", u.getRef());
            assertNull("returns a wrong anchor2", u1.getRef());
            u1 = new URL("http://www.yahoo2.com#ref");
            assertEquals("returns a wrong anchor3", "ref", u1.getRef());
            u1 = new URL("http://www.yahoo2.com/file#ref1#ref2");
            assertEquals("returns a wrong anchor4", "ref1#ref2", u1.getRef());
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

        testURL = new URL("http", "u:p@home", 80, "/java?q1#ref");
        assertEquals("[u:p@home]:80", testURL.getAuthority());
        assertEquals("[u:p@home]", testURL.getHost());
        assertNull(testURL.getUserInfo());
        assertEquals("/java?q1", testURL.getFile());
        assertEquals("/java", testURL.getPath());
        assertEquals("q1", testURL.getQuery());
        assertEquals("ref", testURL.getRef());

        testURL = new URL("http", "home", -1, "/java");
        assertEquals("wrong authority2", "home", testURL.getAuthority());
        assertNull("wrong userInfo2", testURL.getUserInfo());
        assertEquals("wrong host2", "home", testURL.getHost());
        assertEquals("wrong file2", "/java", testURL.getFile());
        assertEquals("wrong path2", "/java", testURL.getPath());
        assertNull("wrong query2", testURL.getQuery());
        assertNull("wrong ref2", testURL.getRef());
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
        assertEquals("Got wrong URL external form", "foobar://example.com", s);
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
